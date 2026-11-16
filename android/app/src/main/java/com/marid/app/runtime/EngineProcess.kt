package com.marid.app.runtime

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.URL

/**
 * Owns the on-device engine process: spawns the embedded Bionic node on
 * packages/server's HTTP entry with a random loopback port, waits for health,
 * and exposes the WS URL the RPC client connects to (ws://127.0.0.1:<port>/ws).
 *
 * Lifecycle contract (Zcode-Android lessons): the UI never spawns Node
 * directly — it asks this object; a healthy leftover from a killed app
 * process is adopted, never duplicated; explicit stop kills owned processes
 * plus same-uid leftovers via pidof.
 */
object EngineProcess {

    @Volatile private var process: Process? = null
    @Volatile private var port: Int = -1

    val isRunning: Boolean get() = process?.isAlive == true
    fun wsUrl(): String = "ws://127.0.0.1:$port/ws"
    fun httpUrl(): String = "http://127.0.0.1:$port"
    fun currentPort(): Int = port

    /** Picks a free loopback port at random (owner spec: --port <random>). */
    fun pickFreePort(): Int =
        ServerSocket().use { s ->
            s.bind(InetSocketAddress("127.0.0.1", 0))
            s.localPort
        }

    suspend fun start(ctx: Context): Int = withContext(Dispatchers.IO) {
        val app = ctx.applicationContext
        if (isRunning) return@withContext port
        val chosen = pickFreePort()
        RuntimeBus.setPhase(RuntimePhase.StartingEngine(chosen))

        val command = listOf(
            Provisioner.nodeBin(app).absolutePath,
            Provisioner.distEntry(app).absolutePath,
        )
        RuntimeBus.append("spawning engine on :$chosen — ${command.joinToString(" ")}")
        val pb = ProcessBuilder(command)
        pb.redirectErrorStream(true)
        val env = pb.environment()
        BionicEnv.apply(
            env,
            Provisioner.runtimeRoot(app),
            Provisioner.homeDir(app),
            Provisioner.bashBin(app),
            chosen,
        )
        val proc = pb.start()
        process = proc
        port = chosen

        Thread {
            try {
                proc.inputStream.bufferedReader().use { r: BufferedReader ->
                    while (true) {
                        val line = r.readLine() ?: break
                        RuntimeBus.append("engine| $line")
                    }
                }
            } catch (_: Exception) {
            }
            if (proc === process) {
                RuntimeBus.append(
                    "engine process exited (code=${runCatching { proc.exitValue() }.getOrDefault(-1)})",
                )
            }
        }.apply { isDaemon = true }.start()

        val deadline = System.currentTimeMillis() + 120_000L
        var healthy = false
        while (System.currentTimeMillis() < deadline) {
            if (!proc.isAlive) {
                RuntimeBus.append(
                    "engine died during startup (exit=${runCatching { proc.exitValue() }.getOrDefault(-1)})",
                )
                break
            }
            if (httpAnswering(chosen)) {
                healthy = true
                break
            }
            Thread.sleep(1_500)
        }
        if (!healthy) {
            stop()
            val msg = if (proc.isAlive) {
                "engine did not answer within 120s"
            } else {
                "engine exited during startup — see MaridRuntime logcat"
            }
            RuntimeBus.setPhase(RuntimePhase.Failed("engine", msg))
            throw IOException(msg)
        }
        RuntimeBus.append("engine healthy on :$chosen — WS at ${wsUrl()}")
        RuntimeBus.setPhase(RuntimePhase.EngineReady(chosen))
        chosen
    }

    /**
     * True when the port answers HTTP at all. Without MARID_WEB_STATIC_ROOT
     * the engine serves only /api plus /ws — the root path may 404, which is
     * still proof the hono server is alive.
     */
    private fun httpAnswering(port: Int): Boolean = runCatching {
        val c = URL("http://127.0.0.1:$port/").openConnection() as HttpURLConnection
        c.connectTimeout = 1_500
        c.readTimeout = 1_500
        val ok = c.responseCode in 200..499
        c.disconnect()
        ok
    }.getOrDefault(false)

    /** Best-effort adoption of a runtime survivor we hold no handle for. */
    fun adoptIfHealthy(ctx: Context, wantedPort: Int): Boolean {
        if (process?.isAlive == true) return false
        if (!httpAnswering(wantedPort)) return false
        port = wantedPort
        RuntimeBus.append(
            "adopt: port $wantedPort already answering — reusing the existing engine " +
                "(no duplicate node spawned)",
        )
        RuntimeBus.setPhase(RuntimePhase.EngineReady(wantedPort))
        return true
    }

    fun stop() {
        runCatching { process?.destroy() }
        Thread.sleep(150)
        runCatching { process?.destroyForcibly() }
        process = null
        if (port > 0) {
            runCatching {
                val p = ProcessBuilder("sh", "-c", "pidof node").start()
                val out = p.inputStream.bufferedReader().readText().trim()
                p.waitFor()
                out.split(Regex("\\s+")).mapNotNull { it.toIntOrNull() }.forEach { pid ->
                    runCatching { android.os.Process.killProcess(pid) }
                }
            }
        }
        RuntimeBus.append("engine stopped")
    }

    /** The persisted marker carries the port of the last successful start. */
    fun readLastPort(ctx: Context): Int = runCatching {
        val marker = File(Provisioner.runtimeRoot(ctx), "engine-port.txt")
        if (marker.isFile) marker.readText().trim().toInt() else -1
    }.getOrDefault(-1)

    fun persistPort(ctx: Context, p: Int) {
        runCatching {
            File(Provisioner.runtimeRoot(ctx), "engine-port.txt").writeText(p.toString())
        }
    }
}
