package com.marid.app.engine

import android.content.Context
import java.io.File

/**
 * Bionic environment — manages the embedded userland prefix.
 *
 * THE PERMISSION-BUG FIX (Defect Log "Permission denied", termux paths):
 * the old ZCode-Android build pointed $PREFIX at
 * `/data/data/com.termux/files/usr`, which the Marid app sandbox cannot
 * read or execute (different uid). Marid owns its own prefix under the app
 * files dir: `/data/data/com.marid.app/files/usr`.
 */
class BionicEnv(private val context: Context) {

    /** $PREFIX — inside OUR app sandbox; no cross-app termux paths. */
    val prefix: File
        get() = File(context.filesDir, "usr")

    val prefixPath: String get() = prefix.absolutePath

    /** Dist layout for the engine bundle: files/runtime/dist/marid */
    val runtimeDist: File
        get() = File(context.filesDir, "runtime/dist/marid")

    val logsDir: File
        get() = File(context.filesDir, "logs")

    /** provision-complete.json — the marker the UI/engine both check. */
    val provisionMarker: File
        get() = File(logsDir, "provision-complete.json")

    fun isProvisioned(): Boolean = provisionMarker.isFile && prefix.isDirectory

    /**
     * Environment for child processes (node engine, bash, git), mirroring the
     * Termux-style env contract the runtime expects but bound to the Marid
     * sandbox paths.
     */
    fun buildEnvironment(): List<String> {
        val files = context.filesDir.absolutePath
        return listOf(
            "PREFIX=$prefixPath",
            "PATH=$prefixPath/bin:$prefixPath/bin/applets/:/system/bin:/system/xbin:/vendor/bin",
            "LD_LIBRARY_PATH=$prefixPath/lib",
            "TMPDIR=$prefixPath/tmp",
            "HOME=$files/home",
            "PWD=$files/workspace",
            "TMP=$prefixPath/tmp",
            "TEMP=$prefixPath/tmp",
            "SHELL=$prefixPath/bin/bash",
            "LANG=en_US.UTF-8",
            "NODE_PATH=$prefixPath/lib/node_modules",
            "ANDROID_DATA=/data",
            "ANDROID_ROOT=/system",
            "BOOTCLASSPATH=/system/framework/core-oj.jar",
        )
    }

    /**
     * chmod +x / readable pass over the extracted prefix — the fix for the
     * "Permission denied" screenshots: exec() fails unless the file has the
     * owner-exec bit, and adb-installed archives can lose mode bits.
     * Returns the number of files made executable.
     */
    fun ensureExecutablePermissions(): Int {
        var count = 0
        val bin = File(prefix, "bin")
        val libexec = File(prefix, "libexec")
        val applets = File(bin, "applets")

        val dirs = sequenceOf(bin, applets, libexec).filter { it.isDirectory }
        for (dir in dirs) {
            dir.listFiles()?.forEach { f ->
                if (f.isFile && !f.name.endsWith(".so")) {
                    if (f.setReadable(true, true) && f.setExecutable(true, true)) count++
                }
            }
        }
        // Shared libraries must be readable for dlopen.
        val lib = File(prefix, "lib")
        if (lib.isDirectory) {
            lib.listFiles()?.forEach { f ->
                if (f.isFile && f.name.endsWith(".so")) {
                    if (f.setReadable(true, true)) count++
                }
            }
        }
        // The node binary itself.
        val node = File(prefix, "bin/node")
        if (node.isFile && node.setExecutable(true, true)) count++
        return count
    }

    /** Diagnostic dump used by the terminal sheet and the in-app agent. */
    fun diagnosticSummary(): String = buildString {
        appendLine("PREFIX=$prefixPath")
        appendLine("provisioned=${isProvisioned()}")
        appendLine("prefixExists=${prefix.isDirectory}")
        val node = File(prefix, "bin/node")
        appendLine("nodeBinary=${node.isFile} canExec=${node.canExecute()}")
        appendLine("distBundle=${File(runtimeDist, "entry-http.js").isFile}")
    }
}
