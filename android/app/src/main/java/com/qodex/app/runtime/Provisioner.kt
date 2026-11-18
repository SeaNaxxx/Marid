package com.qodex.app.runtime

import android.content.Context
import android.os.SystemClock
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

    // Marker content is the source of truth: the dist sha it records must match
    // the dist shipped in THIS apk, otherwise the runtime is stale and a
    // re-provision is required (apk upgrade / corrupted tree paths).
    fun isProvisioned(ctx: Context): Boolean = runCatching {
        val marker = markerFile(ctx)
        if (!marker.isFile) return@runCatching false
        if (!nodeBin(ctx).isFile || !distEntry(ctx).isFile) return@runCatching false
        // The marker JSON contains exactly one 64-hex digest (distSha256); the
        // probe text and timestamp never match a 64-hex pattern.
        val recorded = Regex("[0-9a-fA-F]{64}").find(marker.readText())?.value
        recorded != null && recorded.equals(expectedDistSha(ctx), ignoreCase = true)
    }.getOrDefault(false)

    private fun expectedDistSha(ctx: Context): String = runCatching {
        readAssetText(ctx, DIST_SHA_ASSET).trim()
    }.getOrDefault("")

    // stdio entry: the RPC channel rides process pipes (no HTTP/WS listener).
    fun distEntry(ctx: Context): File = File(distDir(ctx), "entry-stdio.js")

    // Single-flight: Activity recreation fires boot() again while a provision
    // is running; a second concurrent provision() used to deleteRecursively()
    // the tree under the first extraction, leaving no marker and forcing a full
    // re-provision on every launch. A duplicate request is now ignored.
    @Volatile
    private var provisionInFlight = false
    private val provisionLock = Any()

    suspend fun provision(ctx: Context): Unit = withContext(Dispatchers.IO) {
        val app = ctx.applicationContext
        val started = synchronized(provisionLock) {
            if (provisionInFlight) {
                RuntimeBus.append("provision already in flight — duplicate request ignored")
                false
            } else {
                provisionInFlight = true
                true
            }
        }
        if (!started) return@withContext
        try {
            provisionTransactional(app)
        } finally {
            provisionInFlight = false
        }
    }

    // Transactional install: everything extracts into staging trees and is
    // swapped in only after every gate passed; a kill mid-provision leaves the
    // previous tree (or nothing) intact — never a partial usr/ or dist/.
    private suspend fun provisionTransactional(app: Context): Unit = withContext(Dispatchers.IO) {
        val rt = runtimeRoot(app)
        val t0 = SystemClock.elapsedRealtime()
        val usrStaging = File(rt, "usr.staging")
        val distStaging = File(rt, "dist.staging")
        try {
            FileLogger.append("Provisioner", "provision started (transactional)")
            RuntimeBus.setPhase(RuntimePhase.Provisioning("preparing directories"))
            usrStaging.deleteRecursively()
            distStaging.deleteRecursively()
            rt.mkdirs()
            File(homeDir(app), "tmp").mkdirs()

            RuntimeBus.setPhase(RuntimePhase.Provisioning("extracting Bionic runtime"))
            val tDeb = SystemClock.elapsedRealtime()
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
            val debList = debNames.sorted().filter { it.endsWith(".deb") }
            var debs = 0
            for (name in debList) {
                val expected = shaByName[name.removeSuffix(".deb")]
                    ?: throw IOException("deb not in manifest: $name")
                val staged = File(rt, "downloads/$name")
                staged.parentFile?.mkdirs()
                copyAsset(app, "$DEB_DIR_ASSET/$name", staged)
                verifySha256(staged, expected, name)
                DebExtractor.extract(staged, usrStaging, report)
                staged.delete()
                debs++
                if (debs % 10 == 0) {
                    RuntimeBus.setPhase(
                        RuntimePhase.Provisioning("extracting Bionic runtime ($debs/${debList.size})"),
                    )
                }
            }
            RuntimeBus.append(
                "bionic extraction: $debs debs, ${report.regularFiles} files, " +
                    "${report.symlinksCreated} symlinks, ${report.symlinkCopies} " +
                    "symlink copies, ${report.hardlinksCreated} hardlinks, " +
                    "${report.skippedEntries} skipped" +
                    (report.firstError?.let { " (first fs error: $it)" } ?: ""),
            )
            val stagedNode = File(usrStaging, "bin/node")
            val stagedBash = File(usrStaging, "bin/bash")
            if (!stagedNode.isFile || !stagedBash.isFile) {
                throw IOException(
                    "bionic runtime incomplete (usr/bin/node or usr/bin/bash missing) " +
                        "— ${report.skippedEntries} entries skipped, first: ${report.firstError}",
                )
            }
            FileLogger.append(
                "Provisioner",
                "bionic extracted: $debs debs, ${report.regularFiles} files, " +
                    "${report.symlinksCreated} symlinks, ${report.skippedEntries} skipped " +
                    "in ${SystemClock.elapsedRealtime() - tDeb}ms",
            )
            File(usrStaging, "bin").listFiles()?.forEach { f ->
                if (f.isFile && !TarExtractor.DefaultLinkOps.isSymbolicLink(f)) {
                    f.setExecutable(true, false)
                }
            }
            File(usrStaging, "lib").listFiles()?.forEach { f ->
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
            val tDist = SystemClock.elapsedRealtime()
            val distSha = readAssetText(app, DIST_SHA_ASSET).trim()
            val distTar = File(rt, "downloads/qodex-dist.tgz")
            distTar.parentFile?.mkdirs()
            copyAsset(app, DIST_ASSET, distTar)
            verifySha256(distTar, distSha, "qodex-dist")
            // Assert the archive layout BEFORE extraction — a nested or stale
            // bundle must fail with its real root entries, not a post-hoc guess.
            DistArchiveContract.requireValid(distTar)
            distStaging.mkdirs()
            TarExtractor(distStaging).extract(
                distTar.inputStream().let { GzipCompressorInputStream(it) },
            )
            distTar.delete()
            if (!File(distStaging, "entry-stdio.js").isFile) {
                throw IOException(
                    "engine dist layout unexpected (entry-stdio.js missing at archive root)",
                )
            }
            FileLogger.append(
                "Provisioner",
                "engine dist extracted (sha256 verified) in ${SystemClock.elapsedRealtime() - tDist}ms",
            )

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

            // Activation: staged trees replace the live ones only here — after
            // every extraction, hash and layout gate succeeded.
            RuntimeBus.setPhase(RuntimePhase.Provisioning("activating runtime"))
            val tSwap = SystemClock.elapsedRealtime()
            swapTree(usrDir(app), File(usrStaging, "usr"), "usr")
            swapTree(distDir(app), distStaging, "dist")
            FileLogger.append(
                "Provisioner",
                "runtime activated in ${SystemClock.elapsedRealtime() - tSwap}ms",
            )

            // ARM64/Bionic compatibility gate before provisioning is declared complete.
            RuntimeBus.setPhase(RuntimePhase.Provisioning("checking Node.js on this device"))
            val tProbe = SystemClock.elapsedRealtime()
            val probe = runNodeProbe(app)
            RuntimeBus.append("device-side node probe: $probe")
            FileLogger.append(
                "Provisioner",
                "node probe passed in ${SystemClock.elapsedRealtime() - tProbe}ms: $probe",
            )

            markerFile(app).writeText(
                """{"node":"$probe","debs":$debs,""" +
                    """"distSha256":"$distSha","provisionedAt":${System.currentTimeMillis()}}""",
            )
            File(rt, "downloads").deleteRecursively()
            usrStaging.deleteRecursively()
            RuntimeBus.setPhase(RuntimePhase.Provisioned)
            FileLogger.append(
                "Provisioner",
                "provision complete in ${SystemClock.elapsedRealtime() - t0}ms",
            )
        } catch (t: Throwable) {
            // Staging trees are disposable; the live runtime is never touched on failure.
            usrStaging.deleteRecursively()
            distStaging.deleteRecursively()
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

    // Same-volume rename swap with backup-restore rollback: the previous tree
    // only disappears after the new tree is fully in place.
    private fun swapTree(target: File, staged: File, label: String) {
        if (!staged.isDirectory) throw IOException("$label swap: staged tree missing (${staged.absolutePath})")
        val backup = File(target.parentFile, "$label.previous")
        backup.deleteRecursively()
        if (target.exists()) {
            if (!target.renameTo(backup)) {
                throw IOException("$label swap: could not move the previous tree aside")
            }
        }
        if (!staged.renameTo(target)) {
            if (backup.exists()) {
                runCatching { backup.renameTo(target) }
                    .onFailure {
                        throw IOException("$label swap failed and backup restore failed: ${it.message}")
                    }
            }
            throw IOException("$label swap: rename failed")
        }
        backup.deleteRecursively()
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
