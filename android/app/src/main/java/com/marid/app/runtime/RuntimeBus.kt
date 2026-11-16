package com.marid.app.runtime

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import android.util.Log

// Every state is real — FAILED carries the exact stage + message for on-device diagnosis.
sealed interface RuntimePhase {
    data object Idle : RuntimePhase
    data class Provisioning(val step: String) : RuntimePhase
    data object Provisioned : RuntimePhase
    data class StartingEngine(val port: Int) : RuntimePhase
    data class EngineReady(val port: Int) : RuntimePhase
    data class Failed(val stage: String, val message: String) : RuntimePhase
}

// Process-wide singleton; the log ring mirrors to logcat (MaridRuntime) as the debugging fallback.
object RuntimeBus {
    const val TAG = "MaridRuntime"
    private const val LOG_LINES = 400

    val phase = MutableStateFlow<RuntimePhase>(RuntimePhase.Idle)
    val log = MutableStateFlow<List<String>>(emptyList())

    fun setPhase(p: RuntimePhase) {
        phase.value = p
        append("phase -> $p")
    }

    fun append(line: String) {
        Log.i(TAG, line)
        val ts = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US)
            .format(java.util.Date())
        log.update { (it + "[$ts] $line").takeLast(LOG_LINES) }
    }
}
