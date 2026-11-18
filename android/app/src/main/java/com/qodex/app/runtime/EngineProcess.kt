package com.qodex.app.runtime

import android.content.Context
import com.qodex.app.utils.FileLogger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

// Engine lifecycle over pure stdio: no TCP port exists anymore. Kotlin owns the
// spawned process; stdout is the RPC frame channel, stderr is diagnostics, and
// closing the app process kills the engine via pipe EOF (no adopt logic needed).
object EngineProcess {

    class Streams(
        val stdout: InputStream,
        val stdin: OutputStream,
        val stderr: InputStream,
    )

    @Volatile private var process: Process? = null
    @Volatile private var streams: Streams? = null

    val isRunning: Boolean get() = process?.isAlive == true

    fun streamsOrNull(): Streams? = streams?.takeIf { isRunning }

    suspend fun start(ctx: Context): Unit = withContext(Dispatchers.IO) {
        if (isRunning) return@withContext
        RuntimeBus.setPhase(RuntimePhase.StartingEngine)

        val app = ctx.applicationContext
        val command = listOf(
            Provisioner.nodeBin(app).absolutePath,
            Provisioner.distEntry(app).absolutePath,
        )
        RuntimeBus.append("spawning engine (stdio) — ${command.joinToString(" ")}")
        val pb = ProcessBuilder(command)
        // redirectErrorStream stays false: mixing stderr into stdout would corrupt the frame stream.
        pb.redirectErrorStream(false)
        val env = pb.environment()
        BionicEnv.apply(
            env,
            Provisioner.runtimeRoot(app),
            Provisioner.homeDir(app),
            Provisioner.bashBin(app),
        )
        val proc = pb.start()
        process = proc
        streams = Streams(proc.inputStream, proc.outputStream, proc.errorStream)
        FileLogger.append("EngineProcess", "engine spawn (stdio): node ${command[1]}")

        Thread {
            try {
                proc.errorStream.bufferedReader().forEachLine { line ->
                    RuntimeBus.append("engine| $line")
                }
            } catch (_: Exception) {
            }
        }.apply { isDaemon = true }.start()

        Thread {
            val code = runCatching { proc.waitFor() }.getOrDefault(-1)
            FileLogger.append("EngineProcess", "engine process exited (code=$code)")
            RuntimeBus.append("engine process exited (code=$code)")
            if (proc === process) {
                streams = null
                process = null
            }
        }.apply { isDaemon = true }.start()

        // Early-death gate only: real readiness is the RPC Initialize handshake,
        // which the RPC client surfaces as CONNECTED and the boot UI gates on.
        val deadline = System.currentTimeMillis() + 15_000L
        while (System.currentTimeMillis() < deadline) {
            if (!proc.isAlive) {
                val msg = "engine exited during startup (exit=${runCatching { proc.exitValue() }.getOrDefault(-1)}) — see QodexRuntime log"
                FileLogger.append("EngineProcess", "engine start failed: $msg")
                RuntimeBus.setPhase(RuntimePhase.Failed("engine", msg))
                throw IOException(msg)
            }
            Thread.sleep(500)
        }
        RuntimeBus.append("engine alive on stdio — waiting for RPC handshake")
        RuntimeBus.setPhase(RuntimePhase.EngineReady)
    }

    fun stop() {
        runCatching { process?.destroy() }
        Thread.sleep(150)
        runCatching { process?.destroyForcibly() }
        process = null
        streams = null
        // Sweep fallback for detached engine children: kill only node processes
        // whose /proc cmdline points INTO this app's runtime tree — never every
        // node on the device (Termux or a debugger session must survive).
        runCatching {
            val p = ProcessBuilder("sh", "-c", "pidof node").start()
            val out = p.inputStream.bufferedReader().readText().trim()
            p.waitFor()
            out.split(Regex("\\s+")).mapNotNull { it.toIntOrNull() }.forEach { pid ->
                runCatching {
                    val cmdline = File("/proc/$pid/cmdline")
                        .inputStream()
                        .bufferedReader()
                        .readText()
                        .replace('\u0000', ' ')
                    if (cmdline.contains("/runtime/usr/bin/node") ||
                        cmdline.contains("/runtime/dist/entry-stdio.js")
                    ) {
                        android.os.Process.killProcess(pid)
                    }
                }
            }
        }
        RuntimeBus.append("engine stopped")
        FileLogger.append("EngineProcess", "engine stopped")
    }
}
