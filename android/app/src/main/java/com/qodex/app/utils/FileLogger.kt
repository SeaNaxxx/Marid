package com.qodex.app.utils

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// logcat needs ADB; boot.log survives for the user to pull and share when the app fails to start.
// Audit M-01: diagnostics live in noBackupFilesDir (internal, excluded from
// Auto Backup / device transfer even if backup policy ever changes — never
// external storage), and every message passes the central LogRedactor before
// reaching logcat or disk so credential material cannot leak via exception
// messages or engine stderr.
object FileLogger {
    private const val TAG = "QodexFileLog"
    private const val MAX_BYTES = 1L shl 20
    @Volatile private var dir: File? = null

    fun init(context: Context) {
        try {
            // Internal-only and excluded from backups by the platform; the
            // manifest additionally sets allowBackup=false for the whole app.
            dir = File(context.noBackupFilesDir, "logs").apply { mkdirs() }
            append(TAG, "file logging ready at ${dir?.absolutePath}")
        } catch (t: Throwable) {
            Log.w(TAG, "init failed: ${t.message}")
        }
    }

    fun append(tag: String, message: String) {
        val safe = LogRedactor.redact(message)
        Log.i(tag, safe)
        try {
            val f = File(dir ?: return, "boot.log")
            if (f.length() > MAX_BYTES) f.delete() // cap runaway growth from retry loops
            val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            PrintWriter(FileWriter(f, true)).use { it.println("[$ts] [$tag] $safe") }
        } catch (t: Throwable) {
            Log.w(TAG, "log write failed: ${t.message}") // logging must never crash the app
        }
    }
}
