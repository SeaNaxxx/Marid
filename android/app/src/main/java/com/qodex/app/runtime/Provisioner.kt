package com.qodex.app.runtime

import android.content.Context
import com.qodex.app.utils.FileLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.security.MessageDigest

// Fully offline provision from APK assets — must succeed in airplane mode; usr/ + dist/ land under filesDir/runtime.
object Provisioner {

    const val DEB_DIR_ASSET = "runtime/termux-debs"
    const val DIST_ASSET = "runtime/qodex-dist.tgz"
    const val DIST_SHA_ASSET = "runtime/qodex-dist.sha256"

    fun runtimeRoot(ctx: Context): File = File(ctx.filesDir, "runtime")
    fun usrDir(ctx: Context): File = File(runtimeRoot(ctx), "usr")
    fun distDir(ctx: Context): File = File(runtimeRoot(ctx), "dist")
    fun homeDir(ctx: Context): File = File(runtimeRoot(ctx), "home").apply { mkdirs() }
    fun workspaceDir(ctx: Context): File =
        File(ctx.filesDir, "workspace").apply { mkdirs() }
    fun nodeBin(ctx: Context): File = File(usrDir(ctx), "bin/node")
    fun bashBin(ctx: Context): File = File(usrDir(ctx), "bin/bash")
    // Engine credential contract: 32-4096 bytes, mode 600, persisted once — existing encrypted credentials require their original key.
    fun credentialSecret(ctx: Context): File =
        File(runtimeRoot(ctx), "credential-secret")
    fun markerFile(ctx: Context): File =
        File(runtimeRoot(ctx), "provision-complete.json")

    fun isProvisioned(ctx: Context): Boolean = runCatching {
        markerFile(ctx).isFile &&
            nodeBin(ctx).isFile &&
            distEntry(ctx).isFile
    }.getOrDefault(false)

    // stdio entry: the RPC channel rides process pipes (no HTTP/WS listener).
    fun distEntry(ctx: Context): File = File(distDir(ctx), "entry-stdio.js")

    suspend fun provision(ctx: Context): Unit = withContext(Dispatchers.IO) {
        val app = ctx.applicationContext
        try {
            FileLogger.append("Provisioner", "provision started")
            val rt = runtimeRoot(app)
            RuntimeBus.setPhase(RuntimePhase.Provisioning("preparing directories"))
            usrDir(app).deleteRecursively()
            distDir(app).deleteRecursively()
            rt.mkdirs()
            File(homeDir(app), "tmp").mkdirs()

            RuntimeBus.setPhase(RuntimePhase.Provisioning("extracting Bionic runtime"))
            val manifest = readAssetText(app, "$DEB_DIR_ASSET/manifest.txt")
            val debNames = app.assets.list(DEB_DIR_ASSET)
                ?: throw IOException("assets missing: $DEB_DIR_ASSET")
            val shaByName = manifest.lineSequence()
                .filter { it.isNotBlank() }
                .associate {
                    val parts = it.split("\t")
                    parts[0] to parts.getOrElse(2) { "" }
                }
            if (shaByName.isEmpty()) {
                throw IOException("termux-debs manifest empty or unparsable")
            }
            val report = TarExtractor.ExtractReport()
            var debs = 0
            for (name in debNames.sorted()) {
                if (!name.endsWith(".deb")) continue
                val expected = shaByName[name.removeSuffix(".deb")]
                    ?: throw IOException("deb not in manifest: $name")
                val staged = File(rt, "downloads/$name")
                staged.parentFile?.mkdirs()
                copyAsset(app, "$DEB_DIR_ASSET/$name", staged)
                verifySha256(staged, expected, name)
                DebExtractor.extract(staged, rt, report)
                staged.delete()
                debs++
            }
            RuntimeBus.append(
                "bionic extraction: $debs debs, ${report.regularFiles} files, " +
                    "${report.symlinksCreated} symlinks, ${report.symlinkCopies} " +
                    "symlink copies, ${report.hardlinksCreated} hardlinks, " +
                    "${report.skippedEntries} skipped" +
                    (report.firstError?.let { " (first fs error: $it)" } ?: ""),
            )
            if (!nodeBin(app).isFile || !bashBin(app).isFile) {
                throw IOException(
                    "bionic runtime incomplete (usr/bin/node or usr/bin/bash missing) " +
                        "— ${report.skippedEntries} entries skipped, first: ${report.firstError}",
                )
            }
            FileLogger.append(
                "Provisioner",
                "bionic extracted: $debs debs, ${report.regularFiles} files, " +
                    "${report.symlinksCreated} symlinks, ${report.skippedEntries} skipped",
            )
            File(usrDir(app), "bin").listFiles()?.forEach { f ->
                if (f.isFile && !TarExtractor.DefaultLinkOps.isSymbolicLink(f)) {
                    f.setExecutable(true, false)
                }
            }
            File(usrDir(app), "lib").listFiles()?.forEach { f ->
                if (f.isFile) {
                    f.setReadable(true, false)
                    if (f.extension == "so" || f.name.contains(".so.")) {
                        f.setExecutable(true, false)
                    }
                }
            }
            // Compiled-in Termux OPENSSLDIR is EACCES here; the runtime's own openssl.cnf/cert.pem are fatal-if-missing.
            val cnf = BionicEnv.ensureOpenSslConfig(rt)
            if (!cnf.isFile || !cnf.canRead()) {
                throw IOException("openssl.cnf unreadable at ${cnf.absolutePath}")
            }
            RuntimeBus.append(
                "tls: openssl.cnf=${cnf.length()}B cert.pem=" +
                    "${File(rt, BionicEnv.CERT_PEM_REL).isFile} " +
                    "ossl-modules=${File(rt, BionicEnv.OSSL_MODULES_REL).isDirectory}",
            )

            RuntimeBus.setPhase(RuntimePhase.Provisioning("extracting engine"))
            val distSha = readAssetText(app, DIST_SHA_ASSET).trim()
            val distTar = File(rt, "downloads/qodex-dist.tgz")
            distTar.parentFile?.mkdirs()
            copyAsset(app, DIST_ASSET, distTar)
            verifySha256(distTar, distSha, "qodex-dist")
            distDir(app).mkdirs()
            TarExtractor(distDir(app)).extract(
                distTar.inputStream().let { GzipCompressorInputStream(it) },
            )
            distTar.delete()
            if (!distEntry(app).isFile) {
                throw IOException(
                    "engine dist layout unexpected (dist/entry-http.js missing)",
                )
            }
            FileLogger.append("Provisioner", "engine dist extracted (sha256 verified)")

            val secret = credentialSecret(app)
            if (!secret.isFile) {
                secret.parentFile?.mkdirs()
                val bytes = ByteArray(48)
                java.security.SecureRandom().nextBytes(bytes)
                secret.writeBytes(bytes)
                secret.setReadable(false, false)
                secret.setWritable(false, false)
                secret.setReadable(true, true) // owner-only
                secret.setWritable(true, true)
                RuntimeBus.append("credential secret generated (48B, 0600)")
                FileLogger.append("Provisioner", "credential secret generated (48B, 0600)")
            }

            // ARM64/Bionic compatibility gate before provisioning is declared complete.
            RuntimeBus.setPhase(RuntimePhase.Provisioning("checking Node.js on this device"))
            val probe = runNodeProbe(app)
            RuntimeBus.append("device-side node probe: $probe")
            FileLogger.append("Provisioner", "node probe passed: $probe")

            markerFile(app).writeText(
                """{"node":"$probe","debs":$debs,""" +
                    """"distSha256":"$distSha","provisionedAt":${System.currentTimeMillis()}}""",
            )
            File(rt, "downloads").deleteRecursively()
            RuntimeBus.setPhase(RuntimePhase.Provisioned)
            FileLogger.append("Provisioner", "provision complete")
        } catch (t: Throwable) {
            FileLogger.append("Provisioner", "provision failed: ${t.message ?: t.javaClass.simpleName}")
            RuntimeBus.append("provisioning failed: ${t.javaClass.simpleName}: ${t.message}")
            RuntimeBus.setPhase(
                RuntimePhase.Failed(
                    "provision",
                    "${t.message ?: t.javaClass.simpleName}",
                ),
            )
        }
    }

    // Failure classification: CANNOT LINK = linker closure defect; OpenSSL configuration error = env defect; com.termux in output = env leak.
    fun runNodeProbe(ctx: Context): String {
        val pb = ProcessBuilder(
            nodeBin(ctx).absolutePath, "-e",
            "console.log('Qodex Node OK ' + process.version + ' ' + " +
                "process.platform + ' ' + process.arch)",
        )
        pb.redirectErrorStream(true)
        BionicEnv.apply(pb.environment(), runtimeRoot(ctx), homeDir(ctx), bashBin(ctx))
        val proc = pb.start()
        val out = proc.inputStream.bufferedReader().readText().trim()
        val code = proc.waitFor()
        if (code != 0) {
            val detail = out.take(300)
            val cls = when {
                out.contains("CANNOT LINK") ->
                    " [classification: linker closure defect — a bundled library " +
                        "is missing or was not extracted]"
                out.contains("OpenSSL configuration error") ->
                    " [classification: env defect — OPENSSL_CONF/OPENSSL_MODULES " +
                        "must point inside the app runtime]"
                out.contains("com.termux") ->
                    " [classification: env defect — runtime leaked a Termux path]"
                else -> ""
            }
            throw IOException("embedded node failed on this device (exit $code): $detail$cls")
        }
        return out.take(200)
    }

    // ------------------------------------------------------------------ util

    fun sha256(file: File): String {
        val md = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buf = ByteArray(1 shl 16)
            var r: Int
            while (input.read(buf).also { r = it } != -1) md.update(buf, 0, r)
        }
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    private fun verifySha256(file: File, expected: String, label: String) {
        val got = sha256(file)
        if (!expected.equals(got, ignoreCase = true)) {
            throw IOException("$label sha256 mismatch: expected=$expected got=$got")
        }
    }

    fun copyAsset(ctx: Context, name: String, dest: File) {
        ctx.assets.open(name).use { input ->
            dest.outputStream().use { input.copyTo(it) }
        }
    }

    private fun readAssetText(ctx: Context, name: String): String =
        ctx.assets.open(name).bufferedReader().use { it.readText() }
}
