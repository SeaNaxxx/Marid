package com.marid.app.runtime

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.security.MessageDigest

/**
 * Provisions the embedded Linux userland + engine dist from APK assets into
 * app-private storage. Fully OFFLINE (must succeed in airplane mode).
 *
 * Assets layout (embedded at build time by android/scripts/fetch-termux-debs.py
 * and the engine build step):
 *   runtime/termux-debs/<package>.deb   Termux Bionic deb closure (index
 *                                       SHA256-verified at fetch time)
 *   runtime/termux-debs/manifest.txt    "name<TAB>version<TAB>sha256" lines
 *   runtime/marid-dist.tgz              @marid/server engine dist + prod deps
 *   runtime/marid-dist.sha256           dist tarball sha256
 *
 * Runtime layout produced here:
 *   <filesDir>/runtime/usr/    Bionic prefix (node, bash, libs, TLS store)
 *   <filesDir>/runtime/dist/   engine dist (entry-http.js + node_modules)
 *   <filesDir>/runtime/home/   HOME for the engine (tmp/ inside)
 *   <filesDir>/runtime/workspace/   agent workspace root
 *   <filesDir>/runtime/provision-complete.json
 *
 * Inherited device-failure contracts (see BionicEnv / TarExtractor):
 * per-deb sha256 (G4), sonames as real files + relative/rewritten symlinks
 * (G1/G2), usr/etc/tls/{openssl.cnf,cert.pem} wired via env (G3).
 */
object Provisioner {

    const val DEB_DIR_ASSET = "runtime/termux-debs"
    const val DIST_ASSET = "runtime/marid-dist.tgz"
    const val DIST_SHA_ASSET = "runtime/marid-dist.sha256"

    fun runtimeRoot(ctx: Context): File = File(ctx.filesDir, "runtime")
    fun usrDir(ctx: Context): File = File(runtimeRoot(ctx), "usr")
    fun distDir(ctx: Context): File = File(runtimeRoot(ctx), "dist")
    fun homeDir(ctx: Context): File = File(runtimeRoot(ctx), "home").apply { mkdirs() }
    fun workspaceDir(ctx: Context): File =
        File(ctx.filesDir, "workspace").apply { mkdirs() }
    fun nodeBin(ctx: Context): File = File(usrDir(ctx), "bin/node")
    fun bashBin(ctx: Context): File = File(usrDir(ctx), "bin/bash")
    /**
     * Engine credential secret (MARID_CREDENTIAL_SECRET_FILE contract,
     * discovered by the sandbox engine boot test): 32-4096 bytes, mode 600.
     * The server's file-based credential cipher requires it when no OS
     * credential store exists (always the case on Android). Persisted once;
     * NEVER regenerated on re-provision — existing encrypted credentials
     * require their original key.
     */
    fun credentialSecret(ctx: Context): File =
        File(runtimeRoot(ctx), "credential-secret")
    fun markerFile(ctx: Context): File =
        File(runtimeRoot(ctx), "provision-complete.json")

    fun isProvisioned(ctx: Context): Boolean = runCatching {
        markerFile(ctx).isFile &&
            nodeBin(ctx).isFile &&
            distEntry(ctx).isFile
    }.getOrDefault(false)

    /** Engine entry: dist/entry-http.js (tsup output of @marid/server). */
    fun distEntry(ctx: Context): File = File(distDir(ctx), "entry-http.js")

    suspend fun provision(ctx: Context): Unit = withContext(Dispatchers.IO) {
        val app = ctx.applicationContext
        try {
            val rt = runtimeRoot(app)
            RuntimeBus.setPhase(RuntimePhase.Provisioning("preparing directories"))
            usrDir(app).deleteRecursively()
            distDir(app).deleteRecursively()
            rt.mkdirs()
            File(homeDir(app), "tmp").mkdirs()

            // 1. Termux Bionic deb closure --------------------------------
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
            // chmod: bins + shared libs executable (device-proven policy).
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
            // OpenSSL config must be present + readable INSIDE the runtime
            // (compiled-in Termux OPENSSLDIR is EACCES and fatal at startup).
            val cnf = BionicEnv.ensureOpenSslConfig(rt)
            if (!cnf.isFile || !cnf.canRead()) {
                throw IOException("openssl.cnf unreadable at ${cnf.absolutePath}")
            }
            RuntimeBus.append(
                "tls: openssl.cnf=${cnf.length()}B cert.pem=" +
                    "${File(rt, BionicEnv.CERT_PEM_REL).isFile} " +
                    "ossl-modules=${File(rt, BionicEnv.OSSL_MODULES_REL).isDirectory}",
            )

            // 2. Engine dist -----------------------------------------------
            RuntimeBus.setPhase(RuntimePhase.Provisioning("extracting engine"))
            val distSha = readAssetText(app, DIST_SHA_ASSET).trim()
            val distTar = File(rt, "downloads/marid-dist.tgz")
            distTar.parentFile?.mkdirs()
            copyAsset(app, DIST_ASSET, distTar)
            verifySha256(distTar, distSha, "marid-dist")
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

            // 3. Credential secret (engine contract; generate ONCE) ---------
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
            }

            // 4. On-device gate: the embedded node must run HERE -----------
            RuntimeBus.setPhase(RuntimePhase.Provisioning("checking Node.js on this device"))
            val probe = runNodeProbe(app)
            RuntimeBus.append("device-side node probe: $probe")

            // 4. Marker ------------------------------------------------------
            markerFile(app).writeText(
                """{"node":"$probe","debs":$debs,""" +
                    """"distSha256":"$distSha","provisionedAt":${System.currentTimeMillis()}}""",
            )
            File(rt, "downloads").deleteRecursively()
            RuntimeBus.setPhase(RuntimePhase.Provisioned)
        } catch (t: Throwable) {
            RuntimeBus.append("provisioning failed: ${t.javaClass.simpleName}: ${t.message}")
            RuntimeBus.setPhase(
                RuntimePhase.Failed(
                    "provision",
                    "${t.message ?: t.javaClass.simpleName}",
                ),
            )
        }
    }

    /**
     * Runs the embedded node ON THIS DEVICE — the ARM64/Bionic compatibility
     * gate. Failure classification follows the proven taxonomy:
     * CANNOT LINK → linker closure defect; OpenSSL configuration error →
     * env defect; com.termux in output → env leaked a Termux path.
     */
    fun runNodeProbe(ctx: Context): String {
        val pb = ProcessBuilder(
            nodeBin(ctx).absolutePath, "-e",
            "console.log('Marid Node OK ' + process.version + ' ' + " +
                "process.platform + ' ' + process.arch)",
        )
        pb.redirectErrorStream(true)
        BionicEnv.apply(pb.environment(), runtimeRoot(ctx), homeDir(ctx), bashBin(ctx), 0)
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
