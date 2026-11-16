package com.marid.app.engine

import android.content.Context
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

/**
 * EngineController — runs the on-device Marid engine (Node.js over Bionic)
 * inside the app sandbox and waits for its HTTP surface.
 *
 * Engine layout (from the ZCode-Android runtime contract, paths moved out of
 * the termux sandbox into com.marid.app):
 *   $PREFIX = /data/data/com.marid.app/files/usr        (node, bash, git)
 *   $DIST   = /data/data/com.marid.app/files/runtime/dist/marid/entry-http.js
 *   engine log: /data/data/com.marid.app/files/logs/server.log
 */
class EngineController(
    private val context: Context,
    private val bionic: BionicEnv,
    private val onLog: (String) -> Unit = {},
) {
    private val processRef = AtomicReference<Process?>(null)
    private val running = AtomicBoolean(false)
    private val healthy = CompletableDeferred<Boolean>()
    private var logReaderThread: Thread? = null

    val isEngineRunning: Boolean get() = running.get()
    val engineUrl: String = "http://127.0.0.1:3030"

    /**
     * Starts `node entry-http.js` with the Bionic env. Returns once the
     * server answers /api/server-info (or after [timeoutMs]).
     */
    fun start(timeoutMs: Long = 20_000): Boolean {
        if (running.get()) return healthy.isCompleted && healthy.getCompleted()

        val node = File(bionic.prefix, "bin/node")
        val entry = File(bionic.runtimeDist, "entry-http.js")
        if (!node.isFile) {
            onLog("engine: node binary missing at ${node.absolutePath}")
            return false
        }
        if (!node.canExecute()) {
            // Belt and braces — the Provisioner already did this; enforce again.
            node.setExecutable(true, true)
        }
        if (!entry.isFile) {
            onLog("engine: dist bundle missing at ${entry.absolutePath} (CI must bundle engine/dist.tar.gz)")
            return false
        }

        val logsDir = bionic.logsDir.apply { mkdirs() }
        val serverLog = File(logsDir, "server.log")

        val argv = mutableListOf<String>().apply {
            add(node.absolutePath)
            add(entry.absolutePath)
        }
        val pb = ProcessBuilder(argv).apply {
            directory(context.filesDir)
            environment().clear()
            bionic.buildEnvironment().forEach {
                val idx = it.indexOf('=')
                environment()[it.substring(0, idx)] = it.substring(idx + 1)
            }
            environment()["PORT"] = "3030"
            redirectErrorStream(true)
        }

        onLog("engine: starting ${argv.joinToString(" ")}")
        val process = try {
            pb.start()
        } catch (t: Throwable) {
            onLog("engine: FAILED to spawn (${t.message}) — permission contract broken?")
            return false
        }
        processRef.set(process)
        running.set(true)

        logReaderThread = Thread {
            try {
                BufferedReader(InputStreamReader(process.inputStream)).useLines { lines ->
                    serverLog.appendText("")
                    lines.forEach { line ->
                        onLog("engine: $line")
                        serverLog.appendText(line + "\n")
                    }
                }
            } catch (_: Exception) {
                // stream closed on shutdown
            }
        }.apply { isDaemon = true; start() }

        // Watchdog: wait for health.
        Thread {
            var ok = false
            val deadline = System.currentTimeMillis() + timeoutMs
            while (System.currentTimeMillis() < deadline) {
                if (!process.isAlive) break
                if (httpHealthy()) { ok = true; break }
                try {
                    Thread.sleep(500)
                } catch (_: InterruptedException) {
                    break
                }
            }
            if (!ok && !process.isAlive) {
                onLog("engine: process exited with ${process.exitValue()}")
            }
            running.set(ok)
            healthy.complete(ok)
            onLog("engine: ${if (ok) "healthy at $engineUrl" else "unhealthy after ${timeoutMs}ms"}")
        }.apply { isDaemon = true; start() }

        return true
    }

    suspend fun awaitHealthy(): Boolean {
        return healthy.await()
    }

    private fun httpHealthy(): Boolean = try {
        val url = java.net.URL("$engineUrl/api/server-info")
        val conn = url.openConnection() as java.net.HttpURLConnection
        conn.connectTimeout = 2000
        conn.readTimeout = 2000
        val code = conn.responseCode
        conn.disconnect()
        code in 200..299
    } catch (_: Exception) {
        false
    }

    fun stop() {
        val process = processRef.getAndSet(null) ?: return
        running.set(false)
        process.destroy()
        try {
            if (!process.waitFor(3, TimeUnit.SECONDS)) process.destroyForcibly()
        } catch (_: InterruptedException) {
            process.destroyForcibly()
        }
        logReaderThread?.interrupt()
        onLog("engine: stopped")
    }
}
