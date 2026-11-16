package com.marid.app.state

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.marid.app.rpc.MaridRpcClient
import com.marid.app.rpc.RpcEvents
import com.marid.app.rpc.RpcConnectionState
import com.marid.app.rpc.asArrayOrNull
import com.marid.app.rpc.asObjectOrNull
import com.marid.app.rpc.asStringOrNull
import com.marid.app.runtime.EngineProcess
import com.marid.app.runtime.Provisioner
import com.marid.app.runtime.RuntimeBus
import com.marid.app.runtime.RuntimePhase
import com.marid.app.ui.ChatTurn
import com.marid.app.ui.ModelEntry2c
import com.marid.app.ui.fetchModelEntries
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.put
import java.util.UUID

/**
 * Boot + session orchestration. Owns: provision -> engine spawn -> RPC
 * connect. The UI renders RuntimeBus.phase; chat/session features call the
 * RPC client through this VM (channels per packages/shared/src/channels.ts).
 */
class MaridAppViewModel(private val app: Application) : ViewModel() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _rpcState = MutableStateFlow(RpcConnectionState.DISCONNECTED)
    val rpcState: StateFlow<RpcConnectionState> = _rpcState.asStateFlow()

    private var rpc: MaridRpcClient? = null

    fun rpcClient(): MaridRpcClient? = rpc

    fun boot(ctx: Context) {
        val current = RuntimeBus.phase.value
        if (current is RuntimePhase.Failed || current is RuntimePhase.EngineReady) {
            // retry() or already-booted process recreation: fall through only
            // when idle, otherwise keep the owner-visible state untouched.
            if (current is RuntimePhase.EngineReady) return
        }
        if (EngineProcess.isRunning) {
            RuntimeBus.setPhase(RuntimePhase.Provisioned)
            startEngine(ctx)
            return
        }
        scope.launch {
            if (Provisioner.isProvisioned(ctx)) {
                RuntimeBus.setPhase(RuntimePhase.Provisioned)
                startEngine(ctx)
            } else {
                Provisioner.provision(ctx)
            }
        }
    }

    fun startEngine(ctx: Context) {
        scope.launch {
            // Adopt a healthy survivor first (killed app process, redelivered
            // service) — never spawn a duplicate node on a served port.
            val last = EngineProcess.readLastPort(ctx)
            if (last > 0 && EngineProcess.adoptIfHealthy(ctx, last)) {
                connectRpc()
                return@launch
            }
            try {
                EngineProcess.start(ctx)
                EngineProcess.persistPort(ctx, EngineProcess.currentPort())
                connectRpc()
            } catch (t: Throwable) {
                RuntimeBus.append("engine start failed: ${t.message}")
                RuntimeBus.setPhase(RuntimePhase.Failed("engine", "${t.message}"))
            }
        }
    }

    private fun connectRpc() {
        val url = EngineProcess.wsUrl().removeSuffix("/ws")
        val client = MaridRpcClient(url, RpcEvents({ state ->
            _rpcState.value = state
            if (state == RpcConnectionState.CONNECTED) {
                RuntimeBus.append("RPC connected: ${EngineProcess.wsUrl()}")
            }
        }, { line -> RuntimeBus.append(line) }))
        rpc = client
        client.connect()
    }

    fun retry() {
        val app = this.app
        EngineProcess.stop()
        _rpcState.value = RpcConnectionState.DISCONNECTED
        rpc = null
        RuntimeBus.setPhase(RuntimePhase.Idle)
        scope.launch {
            if (Provisioner.isProvisioned(app)) {
                RuntimeBus.setPhase(RuntimePhase.Provisioned)
                startEngine(app)
            } else {
                Provisioner.provision(app)
            }
        }
    }

    // =====================================================================
    // Chat + model orchestration (channels per packages/shared/src/channels.ts)
    // =====================================================================

    val workspacePath: String get() = Provisioner.workspaceDir(app).absolutePath

    private val _turns = MutableStateFlow<List<ChatTurn>>(emptyList())
    val turns: StateFlow<List<ChatTurn>> = _turns.asStateFlow()

    private val _models = MutableStateFlow<List<ModelEntry2c>>(emptyList())
    val models: StateFlow<List<ModelEntry2c>> = _models.asStateFlow()

    private val _selectedModel = MutableStateFlow<ModelEntry2c?>(null)
    val selectedModel: StateFlow<ModelEntry2c?> = _selectedModel.asStateFlow()

    private val _composerText = MutableStateFlow("")
    val composerText: StateFlow<String> = _composerText.asStateFlow()

    fun onComposerTextChange(text: String) {
        _composerText.value = text
    }

    private var currentSessionId: String? = null
    private var sending = false

    /** Loads the model catalog exactly like the desktop composer (model-selection.getView). */
    fun loadModels() {
        val client = rpc ?: return
        scope.launch {
            val entries = runCatching { fetchModelEntries(client) }
                .getOrElse {
                    RuntimeBus.append("model list fetch failed: ${it.message}")
                    emptyList()
                }
            _models.value = entries
            if (_selectedModel.value == null) _selectedModel.value = entries.firstOrNull()
            RuntimeBus.append("model catalog: ${entries.size} entries")
        }
    }

    fun selectModel(entry: ModelEntry2c) {
        _selectedModel.value = entry
    }

    /**
     * Send — the task-5 chat flow: sendText envelope over the marid-agent
     * channel (sendConversationCommandV4), session events streamed back via
     * onDynamicSessionEvent and rendered defensively (unknown shapes are
     * logged, never crash the UI).
     */
    fun sendText(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || sending) return
        val client = rpc ?: run {
            RuntimeBus.append("send: engine RPC not connected")
            return
        }
        sending = true
        _turns.value = _turns.value + ChatTurn.UserTurn(trimmed)
        scope.launch {
            try {
                ensureSession(client)
                val envelope = kotlinx.serialization.json.buildJsonObject {
                    put("commandId", UUID.randomUUID().toString())
                    put("clientId", "marid-android")
                    currentSessionId?.let { put("sessionId", kotlinx.serialization.json.JsonPrimitive(it)) }
                    put("type", "sendText")
                    put(
                        "payload",
                        kotlinx.serialization.json.buildJsonObject {
                            put("text", trimmed)
                        },
                    )
                    put("issuedAt", System.currentTimeMillis())
                    put("inputOrigin", "mobile")
                }
                val ack = client.sendConversationCommandV4(
                    kotlinx.serialization.json.buildJsonObject {
                        put("workspacePath", workspacePath)
                        put("envelope", envelope)
                    },
                )
                RuntimeBus.append("send ack received: ${ack != null}")
            } catch (t: Throwable) {
                RuntimeBus.append("send failed: ${t.message}")
            } finally {
                sending = false
            }
        }
    }

    private suspend fun ensureSession(client: MaridRpcClient) {
        if (currentSessionId != null) return
        runCatching {
            val created = client.agentCreateSession(workspacePath)
            currentSessionId = created?.asObjectOrNull()
                ?.get("sessionId").asStringOrNull()
                ?: created?.asObjectOrNull()?.get("id").asStringOrNull()
            RuntimeBus.append("session ready ${currentSessionId ?: "(inline)"}")
            subscribeSessionEvents(client)
        }.onFailure { RuntimeBus.append("createSession failed: ${it.message}") }
    }

    private fun subscribeSessionEvents(client: MaridRpcClient) {
        val sessionId = currentSessionId ?: return
        scope.launch {
            runCatching {
                client.subscribeSessionEvents(
                    kotlinx.serialization.json.buildJsonObject {
                        put("workspacePath", workspacePath)
                        put("sessionId", sessionId)
                    },
                ) { event ->
                    handleSessionEvent(event)
                }
            }.onFailure { RuntimeBus.append("session event subscribe failed: ${it.message}") }
        }
    }

    /**
     * Defensive mapping of engine session events to ChatTurns. Unknown event
     * shapes are logged (logcat MaridRuntime) so on-device evidence reveals
     * the exact contract without crashing the transcript.
     */
    private fun handleSessionEvent(event: kotlinx.serialization.json.JsonElement?) {
        val obj = event.asObjectOrNull() ?: return
        val type = obj["type"].asStringOrNull() ?: obj["event"]?.asStringOrNull()
        val payload = obj["payload"].asObjectOrNull() ?: obj
        when (type) {
            "assistantText", "assistant_text", "text" -> {
                val text = payload["text"].asStringOrNull() ?: return
                appendAssistantBlock(com.marid.app.ui.AssistantBlock.MessageText(text))
            }
            "toolCall", "tool_call" -> {
                val name = payload["name"].asStringOrNull()
                    ?: payload["toolName"].asStringOrNull() ?: "tool"
                val statusRaw = payload["status"].asStringOrNull()
                val status = when (statusRaw) {
                    "completed", "success" -> com.marid.app.ui.ToolCallStatus.COMPLETED
                    "running", "in_progress" -> com.marid.app.ui.ToolCallStatus.RUNNING
                    else -> com.marid.app.ui.ToolCallStatus.RUNNING
                }
                appendAssistantBlock(
                    com.marid.app.ui.AssistantBlock.ToolCall(
                        name = name,
                        status = status,
                        summary = payload["summary"].asStringOrNull().orEmpty(),
                    ),
                )
            }
            else -> RuntimeBus.append("session event (unmapped): $type ${payload.toString().take(160)}")
        }
    }

    private fun appendAssistantBlock(block: com.marid.app.ui.AssistantBlock) {
        _turns.value = _turns.value.toMutableList().also { list ->
            val last = list.lastOrNull()
            if (last is ChatTurn.AssistantTurn) {
                list[list.size - 1] = last.copy(blocks = last.blocks + block)
            } else {
                list.add(ChatTurn.AssistantTurn(listOf(block)))
            }
        }
    }

    class factory(private val app: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = MaridAppViewModel(app) as T
    }
}
