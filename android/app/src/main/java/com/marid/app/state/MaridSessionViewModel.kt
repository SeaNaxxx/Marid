package com.marid.app.state

import android.content.Context
import com.marid.app.engine.BionicEnv
import com.marid.app.engine.EngineController
import com.marid.app.engine.Provisioner
import com.marid.app.rpc.MaridRpcClient
import com.marid.app.rpc.RpcConnectionState
import com.marid.app.rpc.asArrayOrNull
import com.marid.app.rpc.asObjectOrNull
import com.marid.app.rpc.asStringOrNull
import com.marid.app.rpc.modelSelectionModelEntries
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID

/**
 * Marid session view model — glues Provisioner → EngineController →
 * MaridRpcClient to the Compose UI. All engine work runs on IO.
 */
class MaridSessionViewModel(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val bionic = BionicEnv(context)
    private val provisioner = Provisioner(context, bionic)
    private val engine = EngineController(context, bionic) { log -> appendEngineLog(log) }

    private val rpc = MaridRpcClient(
        baseUrl = "ws://127.0.0.1:3030",
        events = com.marid.app.rpc.RpcEvents(
            onState = { state -> updateState { copy(connectionState = state) } },
            onLog = { log -> appendEngineLog(log) },
        ),
    )

    private val _state = MutableStateFlow(MaridUiState())
    val state: StateFlow<MaridUiState> = _state.asStateFlow()

    private val terminalId = MutableStateFlow<String?>(null)

    init {
        updateState { copy(workspacePath = context.filesDir.resolve("workspace").absolutePath) }
    }

    private fun updateState(transform: MaridUiState.() -> MaridUiState) {
        _state.update(transform)
    }

    private fun appendEngineLog(line: String) {
        updateState { copy(terminalLines = (terminalLines + line).takeLast(500)) }
    }

    // ------------------------------------------------------------------
    // Boot: provision → engine → RPC → models → session events
    // ------------------------------------------------------------------

    fun boot() {
        scope.launch {
            val result = provisioner.provisionIfNeeded { log -> appendEngineLog(log) }
            updateState {
                copy(
                    provisionStatus = if (result.ok) {
                        "provisioned (${result.filesExtracted} files, ${result.executablesFixed} executables fixed)"
                    } else {
                        "provision failed: ${result.error ?: "assets missing (CI must bundle bionic/ engine tars)"}"
                    },
                )
            }

            if (result.ok) {
                val started = engine.start()
                updateState { copy(engineHealthy = started) }
            }

            rpc.connect()

            // Model selector — "Replace 'mock-model Off' with a real Model
            // Selector dropdown that fetches from the engine" (task STEP 3).
            refreshModels()
        }
    }

    private suspend fun refreshModels() {
        try {
            val view = rpc.modelSelectionView()
            val entries = view?.asObjectOrNull()?.modelSelectionModelEntries().orEmpty()
            if (entries.isNotEmpty()) {
                val models = entries.map { ModelEntry(it.first, it.second) }
                updateState {
                    copy(
                        models = models,
                        selectedModel = if (models.none { it == selectedModel }) models.first() else selectedModel,
                    )
                }
            }
        } catch (t: Throwable) {
            appendEngineLog("models: unavailable (${t.message})")
        }
    }

    // ------------------------------------------------------------------
    // Composer actions
    // ------------------------------------------------------------------

    fun onComposerTextChange(text: String) {
        updateState { copy(composerText = text, sendEnabled = text.isNotBlank()) }
    }

    fun selectModel(entry: ModelEntry) {
        updateState { copy(selectedModel = entry, modelMenuOpen = false) }
    }

    fun toggleModelMenu() = updateState { copy(modelMenuOpen = !modelMenuOpen) }

    fun toggleModeMenu() = updateState { copy(modeMenuOpen = !modeMenuOpen) }

    fun toggleTerminal() = updateState { copy(terminalOpen = !terminalOpen) }

    fun toggleDrawer() = updateState { copy(drawerOpen = !drawerOpen) }

    /**
     * Send — STEP 5: "typing a message and pressing Send creates a real
     * request and receives a response." With the engine live this sends a
     * v4 sendText envelope over the marid-agent channel; without an engine
     * the turn is still appended locally and the connection state makes the
     * offline condition explicit.
     */
    fun send() {
        val current = _state.value
        val text = current.composerText.trim()
        if (text.isEmpty() || current.sending) return

        updateState { copy(sending = true, composerText = "", sendEnabled = false) }
        updateState { copy(turns = turns + ChatTurn.User(UserTurn(text))) }

        scope.launch {
            try {
                if (_state.value.connectionState == RpcConnectionState.CONNECTED) {
                    ensureSession()
                    val envelope = buildJsonObject {
                        put("commandId", UUID.randomUUID().toString())
                        put("clientId", "marid-android")
                        put("sessionId", currentSessionId)
                        put("type", "sendText")
                        put(
                            "payload",
                            buildJsonObject { put("text", text) },
                        )
                        put("issuedAt", System.currentTimeMillis())
                        put("inputOrigin", "mobile")
                    }
                    val ack = rpc.sendConversationCommandV4(
                        buildJsonObject {
                            put("workspacePath", _state.value.workspacePath)
                            put("envelope", envelope)
                        },
                    )
                    appendEngineLog("rpc: sendConversationCommandV4 ack=${ack != null}")
                } else {
                    appendEngineLog("rpc: engine offline — turn kept locally")
                }
            } catch (t: Throwable) {
                appendEngineLog("rpc: send failed (${t.message})")
            } finally {
                updateState { copy(sending = false) }
            }
        }
    }

    private var currentSessionId: String? = null

    private suspend fun ensureSession() {
        if (currentSessionId != null) return
        try {
            val created = rpc.agentCreateSession(_state.value.workspacePath)
            currentSessionId = created?.asObjectOrNull()
                ?.get("sessionId").let { (it as? JsonPrimitive)?.content }
                ?: created?.asObjectOrNull()?.get("id").let { (it as? JsonPrimitive)?.content }
            appendEngineLog("rpc: session ready ${currentSessionId ?: "(inline)"}")
        } catch (t: Throwable) {
            appendEngineLog("rpc: createSession failed (${t.message})")
        }
    }

    // ------------------------------------------------------------------
    // Terminal (STEP 4 — bottom sheet)
    // ------------------------------------------------------------------

    fun runTerminalCommand(command: String) {
        updateState { copy(terminalLines = terminalLines + "$ $command") }
        scope.launch {
            val output = try {
                when {
                    command == "doctor" -> provisioner.verifyExecutableContract()
                        .ifEmpty { listOf("permission contract OK") } + bionic.diagnosticSummary()
                    command.startsWith("ls") -> runShell("ls " + command.removePrefix("ls").trim())
                    else -> runShell(command)
                }
            } catch (t: Throwable) {
                listOf("error: ${t.message}")
            }
            updateState { copy(terminalLines = (terminalLines + output).takeLast(500)) }
        }
    }

    private suspend fun runShell(command: String): List<String> {
        // Prefer the engine terminal service when connected.
        if (_state.value.connectionState == RpcConnectionState.CONNECTED) {
            try {
                val id = terminalId.value ?: rpc.terminalCreate(80, 24).let { created ->
                    val tid = created?.asObjectOrNull()?.get("id").let { (it as? JsonPrimitive)?.content }
                    terminalId.value = tid
                    tid
                }
                if (id != null) {
                    rpc.terminalWrite(id, command + "\n")
                    return listOf("(streamed to engine terminal $id)")
                }
            } catch (_: Throwable) {
                // fall through to local shell
            }
        }
        // Local Bionic shell fallback — runs inside the app sandbox.
        return kotlinx.coroutines.withContext(Dispatchers.IO) {
            val pb = ProcessBuilder("/system/bin/sh", "-c", command)
                .directory(context.filesDir)
            bionic.buildEnvironment().forEach {
                val idx = it.indexOf('=')
                pb.environment()[it.substring(0, idx)] = it.substring(idx + 1)
            }
            val proc = pb.start()
            val out = proc.inputStream.bufferedReader().readLines()
            val err = proc.errorStream.bufferedReader().readLines()
            proc.waitFor()
            (out + err).ifEmpty { listOf("(no output)") }
        }
    }

    fun shutdown() {
        engine.stop()
        rpc.shutdown()
    }
}
