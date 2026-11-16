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
object FileLogger {
    private const val TAG = "QodexFileLog"
    private const val MAX_BYTES = 1L shl 20
    @Volatile private var dir: File? = null

    fun init(context: Context) {
        try {
            val base = context.getExternalFilesDir(null) ?: context.filesDir
            dir = File(base, "logs").apply { mkdirs() }
            append(TAG, "file logging ready at ${dir?.absolutePath}")
        } catch (t: Throwable) {
            Log.w(TAG, "init failed: ${t.message}")
        }
    }

    fun append(tag: String, message: String) {
        Log.i(tag, message)
        try {
            val f = File(dir ?: return, "boot.log")
            if (f.length() > MAX_BYTES) f.delete() // cap runaway growth from retry loops
            val ts = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            PrintWriter(FileWriter(f, true)).use { it.println("[$ts] [$tag] $message") }
        } catch (t: Throwable) {
            Log.w(TAG, "log write failed: ${t.message}") // logging must never crash the app
        }
    }
}
