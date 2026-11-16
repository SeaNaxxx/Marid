package com.marid.app.runtime

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import android.util.Log

/**
 * Provisioning/runtime state machine surfaced to the UI. Every state is real:
 * nothing is faked. FAILED carries the exact stage + message so failures are
 * diagnosable on-device.
 */
sealed interface RuntimePhase {
    data object Idle : RuntimePhase
    data class Provisioning(val step: String) : RuntimePhase
    data object Provisioned : RuntimePhase
    data class StartingEngine(val port: Int) : RuntimePhase
    data class EngineReady(val port: Int) : RuntimePhase
    data class Failed(val stage: String, val message: String) : RuntimePhase
}

/**
 * Single source of runtime state for the UI. Process-wide singleton object.
 * Log ring mirrors to logcat (tag MaridRuntime) so adb logcat is always a
 * full-evidence fallback for device debugging.
 */
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
