package com.marid.app.engine

import android.content.Context
import android.os.Build
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File
import java.io.IOException
import java.util.zip.GZIPInputStream

/**
 * Provisioner — extracts the embedded Bionic runtime from APK assets into
 * the Marid sandbox prefix.
 *
 * FIXES the "Permission denied" defect chain:
 *  1. $PREFIX is `context.filesDir/usr` (com.marid.app sandbox) — never the
 *     old `com.termux` path.
 *  2. Every extracted binary gets chmod u+rx via [TarExtractor.setFilePermissions]
 *     plus an explicit [BionicEnv.ensureExecutablePermissions] pass afterwards.
 *  3. `provision-complete.json` is written only after both extraction and the
 *     permission pass succeed.
 *
 * APK assets contract (bundled by CI):
 *   assets/bionic/node-<abi>.tar.gz        → usr (bin/node, lib, tmp, bin/applets)
 *   assets/bionic/rootfs.tar.gz            → usr (coreutils, bash, git …)
 *   assets/engine/dist.tar.gz              → runtime/dist/marid (entry-http.js …)
 */
class Provisioner(private val context: Context, private val bionic: BionicEnv) {

    data class ProvisionResult(
        val ok: Boolean,
        val filesExtracted: Int,
        val executablesFixed: Int,
        val error: String? = null,
    )

    fun provisionIfNeeded(onLog: (String) -> Unit = {}): ProvisionResult {
        if (bionic.isProvisioned()) {
            onLog("provision: already complete, ensuring permissions")
            val fixed = bionic.ensureExecutablePermissions()
            return ProvisionResult(ok = true, filesExtracted = 0, executablesFixed = fixed)
        }
        return provision(onLog)
    }

    fun provision(onLog: (String) -> Unit = {}): ProvisionResult {
        return try {
            onLog("provision: extracting bionic runtime to ${bionic.prefixPath}")
            var files = 0

            // 1) node + runtime libs for this ABI
            val nodeTar = "bionic/node-${Build.SUPPORTED_ABIS.firstOrNull() ?: "arm64-v8a"}.tar.gz"
            files += extractAsset(nodeTar, bionic.prefix, onLog)

            // 2) rootfs (coreutils/bash/git)
            files += extractAsset("bionic/rootfs.tar.gz", bionic.prefix, onLog)

            // 3) engine dist bundle
            files += extractAsset("engine/dist.tar.gz", File(context.filesDir, "runtime/dist"), onLog)

            // 4) required dirs
            for (dir in listOf("tmp", "bin", "lib", "var/log")) {
                File(bionic.prefix, dir).mkdirs()
            }
            File(context.filesDir, "home").mkdirs()
            File(context.filesDir, "workspace").mkdirs()

            // 5) THE FIX: explicit executable/readable pass over everything
            val fixed = bionic.ensureExecutablePermissions()
            onLog("provision: fixed permissions on $fixed binaries")

            // 6) marker
            bionic.provisionMarker.parentFile?.mkdirs()
            val now = System.currentTimeMillis()
            val markerObject: JsonObject = buildJsonObject {
                put("ts", now)
                put("prefix", bionic.prefixPath)
                put("abi", Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown")
                put("files", files)
                put("executables", fixed)
            }
            bionic.provisionMarker.writeText(
                Json.encodeToString(JsonObject.serializer(), markerObject),
            )
            onLog("provision: complete (${bionic.provisionMarker.absolutePath})")
            ProvisionResult(ok = true, filesExtracted = files, executablesFixed = fixed)
        } catch (t: Throwable) {
            onLog("provision: FAILED ${t.message}")
            ProvisionResult(ok = false, filesExtracted = 0, executablesFixed = 0, error = t.message)
        }
    }

    /** Extract a gzip tarball from assets into dest; returns file count. */
    private fun extractAsset(assetPath: String, dest: File, onLog: (String) -> Unit): Int {
        var count = 0
        context.assets.open(assetPath).use { raw ->
            GZIPInputStream(raw.buffered(64 * 1024)).use { gz ->
                TarExtractor.extract(gz, dest) { _, _ -> count++ }
            }
        }
        onLog("provision: $assetPath -> ${dest.absolutePath} ($count files)")
        return count
    }

    /**
     * Offline verification of the permission contract used by unit tests and
     * the terminal sheet "doctor" command: every file under bin/ (excluding
     * *.so) must be readable + executable.
     */
    fun verifyExecutableContract(): List<String> {
        val problems = mutableListOf<String>()
        val bin = File(bionic.prefix, "bin")
        if (!bin.isDirectory) {
            problems.add("missing ${bin.absolutePath}")
            return problems
        }
        bin.listFiles()?.forEach { f ->
            if (f.isFile && !f.name.endsWith(".so")) {
                if (!f.canRead()) problems.add("not readable: ${f.name}")
                if (!f.canExecute()) problems.add("not executable: ${f.name}")
            }
        }
        val node = File(bin, "node")
        if (!node.isFile) problems.add("missing node binary")
        return problems
    }
}
