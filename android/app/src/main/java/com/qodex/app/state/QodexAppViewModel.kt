package com.qodex.app.state

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.qodex.app.rpc.QodexRpcClient
import com.qodex.app.rpc.RpcEvents
import com.qodex.app.rpc.RpcConnectionState
import com.qodex.app.rpc.asArrayOrNull
import com.qodex.app.rpc.asObjectOrNull
import com.qodex.app.rpc.asStringOrNull
import com.qodex.app.runtime.AgentKeepAliveService
import com.qodex.app.runtime.EngineProcess
import com.qodex.app.runtime.Provisioner
import com.qodex.app.runtime.RuntimeBus
import com.qodex.app.runtime.RuntimePhase
import com.qodex.app.ui.AssistantBlock
import com.qodex.app.ui.ChatTurn
import com.qodex.app.ui.ModelEntry2c
import com.qodex.app.ui.ToolCallStatus
import com.qodex.app.ui.fetchModelEntries
import com.qodex.app.utils.FileLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.put
import java.util.UUID

// Boot orchestration: provision -> engine spawn -> RPC connect; the UI renders RuntimeBus.phase.
class QodexAppViewModel(private val app: Application) : ViewModel() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _rpcState = MutableStateFlow(RpcConnectionState.DISCONNECTED)
    val rpcState: StateFlow<RpcConnectionState> = _rpcState.asStateFlow()

    private var rpc: QodexRpcClient? = null

    fun rpcClient(): QodexRpcClient? = rpc

    // User-facing recovery: with stdio there is no socket to re-dial — a fresh
    // engine process is the only path back to a clean Initialize handshake.
    fun reconnectRpc() {
        FileLogger.append("RPC", "reconnect requested after disconnect")
        restartEngine("user reconnect")
    }

    fun boot(ctx: Context) {
        val current = RuntimeBus.phase.value
        if (current is RuntimePhase.Failed || current is RuntimePhase.EngineReady) {
            // Fall through only when idle; otherwise keep the owner-visible state untouched.
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
            try {
                EngineProcess.start(ctx)
                AgentKeepAliveService.start(ctx)
                connectRpc()
            } catch (t: Throwable) {
                RuntimeBus.append("engine start failed: ${t.message}")
                RuntimeBus.setPhase(RuntimePhase.Failed("engine", "${t.message}"))
            }
        }
    }

    private fun connectRpc() {
        val streams = EngineProcess.streamsOrNull()
        if (streams == null) {
            startEngine(app)
            return
        }
        val client = QodexRpcClient(
            streams.stdout,
            streams.stdin,
            RpcEvents({ state ->
                _rpcState.value = state
                if (state == RpcConnectionState.CONNECTED) {
                    RuntimeBus.append("RPC connected via stdio handshake")
                }
            }, { line -> RuntimeBus.append(line) }),
            onTransportDown = { reason -> restartEngine(reason) },
        )
        rpc = client
        client.connect()
    }

    // One ChannelClient per engine process: Initialize is sent once at process
    // start, so any transport loss ends in a full engine respawn, never a
    // mid-process client recreation that would hang waiting for a second Initialize.
    private fun restartEngine(reason: String) {
        val old = rpc
        rpc = null
        old?.shutdown()
        scope.launch {
            RuntimeBus.append("engine restart: $reason")
            EngineProcess.stop()
            startEngine(app)
        }
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

    fun loadModels() {
        val client = rpc ?: return
        scope.launch {
            val entries = runCatching { fetchModelEntries(client) }
                .getOrElse {
                    RuntimeBus.append("model list fetch failed: ${it.message}")
                    FileLogger.append("RPC", "Error on model-selection.getView: ${it.message}")
                    emptyList()
                }
            _models.value = entries
            if (_selectedModel.value == null) _selectedModel.value = entries.firstOrNull()
            RuntimeBus.append("model catalog: ${entries.size} entries")
            FileLogger.append("RPC", "model catalog loaded: ${entries.size} entries")
        }
    }

    fun selectModel(entry: ModelEntry2c) {
        _selectedModel.value = entry
    }

    // sendText envelope over the qodex-agent channel; unknown event shapes are logged, never crash the UI.
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
                    put("clientId", "qodex-android")
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
                FileLogger.append("RPC", "Error on sendText: ${t.message}")
            } finally {
                sending = false
            }
        }
    }

    private suspend fun ensureSession(client: QodexRpcClient) {
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

    private fun subscribeSessionEvents(client: QodexRpcClient) {
        val sessionId = currentSessionId ?: return
        scope.launch {
            runCatching {
                client.subscribeSessionEvents(
                    kotlinx.serialization.json.buildJsonObject {
                        put("workspacePath", workspacePath)
                        put("sessionId", sessionId)
                        // QodexAgentSessionSubscribeParams requires a deliveryKind; desktop-continuous
                        // keeps live deltas flowing the way the desktop renderer consumes them.
                        put("deliveryKind", "desktop-continuous")
                    },
                ) { event ->
                    handleSessionEvent(event)
                }
            }.onFailure {
                RuntimeBus.append("session event subscribe failed: ${it.message}")
                FileLogger.append("RPC", "Error on onDynamicSessionEvent subscribe: ${it.message}")
            }
        }
    }

    // Engine event contract: packages/shared/src/qodex-protocol/index.ts qodexSessionEventSchema —
    // envelopes { type, payload } with model.streaming / part.delta / tool.updated / turn.* shapes.
    private fun handleSessionEvent(event: kotlinx.serialization.json.JsonElement?) {
        val obj = event.asObjectOrNull() ?: return
        val type = obj["type"].asStringOrNull() ?: obj["event"]?.asStringOrNull()
        val payload = obj["payload"].asObjectOrNull() ?: obj
        when (type) {
            "model.streaming" -> handleModelStreaming(payload)
            "part.delta" -> {
                when (payload["field"].asStringOrNull()) {
                    "text" -> appendToLastTextBlock(payload["delta"].asStringOrNull() ?: "")
                    "reasoning" -> appendToLastReasoningBlock(payload["delta"].asStringOrNull() ?: "")
                    else -> RuntimeBus.append("session event (unmapped field): $payload")
                }
            }
            "part.upserted" -> {
                val part = payload["part"].asObjectOrNull() ?: return
                when (part["type"].asStringOrNull()) {
                    "text" -> {
                        val text = part["text"].asStringOrNull() ?: return
                        if (streamingTextBuilder == null) appendAssistantBlock(AssistantBlock.MessageText(text))
                    }
                    "reasoning" -> {
                        val text = part["text"].asStringOrNull() ?: return
                        appendAssistantBlock(AssistantBlock.Reasoning(text = text, streaming = false))
                    }
                    else -> RuntimeBus.append("session event (unmapped part): ${part["type"]}")
                }
            }
            "tool.updated" -> handleToolUpdated(payload)
            "turn.failed" -> {
                val error = payload["error"].asObjectOrNull()
                val message = error?.get("message").asStringOrNull() ?: "turn failed"
                appendAssistantBlock(AssistantBlock.MessageText("[error] $message"))
                FileLogger.append("RPC", "turn.failed: $message")
            }
            "assistantText", "assistant_text", "text" -> {
                // Legacy shapes kept as fallback for older engine builds.
                val text = payload["text"].asStringOrNull() ?: return
                appendAssistantBlock(AssistantBlock.MessageText(text))
            }
            "toolCall", "tool_call" -> {
                val name = payload["name"].asStringOrNull()
                    ?: payload["toolName"].asStringOrNull() ?: "tool"
                val statusRaw = payload["status"].asStringOrNull()
                val status = when (statusRaw) {
                    "completed", "success" -> ToolCallStatus.COMPLETED
                    "failed", "error" -> ToolCallStatus.FAILED
                    else -> ToolCallStatus.RUNNING
                }
                appendAssistantBlock(
                    AssistantBlock.ToolCall(
                        name = name,
                        status = status,
                        summary = payload["summary"].asStringOrNull().orEmpty(),
                    ),
                )
            }
            else -> RuntimeBus.append("session event (unmapped): $type ${payload.toString().take(160)}")
        }
    }

    private var streamingTextBuilder: StringBuilder? = null

    // model.streaming kinds from qodexModelStreamingKindSchema: text_start/delta/end, reasoning_*, tool_input_*, tool_call.
    private fun handleModelStreaming(payload: kotlinx.serialization.json.JsonObject) {
        when (payload["kind"].asStringOrNull()) {
            "text_start" -> {
                streamingTextBuilder = StringBuilder()
                appendAssistantBlock(AssistantBlock.MessageText(""))
            }
            "text_delta" -> {
                if (streamingTextBuilder == null) {
                    streamingTextBuilder = StringBuilder()
                    appendAssistantBlock(AssistantBlock.MessageText(""))
                }
                appendToLastTextBlock(payload["delta"].asStringOrNull() ?: "")
            }
            "text_end", "finish" -> streamingTextBuilder = null
            "reasoning_delta" -> appendToLastReasoningBlock(payload["delta"].asStringOrNull() ?: "")
            "tool_call" -> {
                val name = payload["toolName"].asStringOrNull() ?: "tool"
                appendAssistantBlock(AssistantBlock.ToolCall(name = name, status = ToolCallStatus.RUNNING, summary = ""))
            }
            else -> Unit
        }
    }

    private fun handleToolUpdated(payload: kotlinx.serialization.json.JsonObject) {
        val kind = payload["kind"].asStringOrNull() ?: return
        val toolCallId = payload["toolCallId"].asStringOrNull()
        val name = payload["toolName"].asStringOrNull() ?: "tool"
        val status = when (kind) {
            "scheduled", "started" -> ToolCallStatus.RUNNING
            "result" -> ToolCallStatus.COMPLETED
            "error" -> ToolCallStatus.FAILED
            else -> return // progress/batch/raw don't change block state
        }
        val existingIndex = toolCallId?.let { toolCallBlocks[it] }
        if (existingIndex != null) {
            updateAssistantBlock(existingIndex) { block ->
                (block as? AssistantBlock.ToolCall)?.copy(status = status) ?: block
            }
        } else {
            val summary = payload["description"].asStringOrNull().orEmpty()
            appendAssistantBlock(AssistantBlock.ToolCall(name = name, status = status, summary = summary))?.let { blockIndex ->
                toolCallId?.let { toolCallBlocks[it] = blockIndex }
            }
        }
    }

    // toolCallId -> index of its ToolCall block inside the last assistant turn, so result/error events update in place.
    private val toolCallBlocks = LinkedHashMap<String, Int>()

    private fun appendToLastTextBlock(delta: String) {
        if (delta.isEmpty()) return
        _turns.value = _turns.value.toMutableList().also { list ->
            for (i in list.indices.reversed()) {
                val turn = list[i]
                if (turn !is ChatTurn.AssistantTurn) continue
                for (b in turn.blocks.indices.reversed()) {
                    val block = turn.blocks[b]
                    if (block is AssistantBlock.MessageText) {
                        list[i] = turn.copy(blocks = turn.blocks.subList(0, b) +
                            block.copy(text = block.text + delta) +
                            turn.blocks.subList(b + 1, turn.blocks.size))
                        return
                    }
                }
            }
        }
    }

    private fun appendToLastReasoningBlock(delta: String) {
        if (delta.isEmpty()) return
        _turns.value = _turns.value.toMutableList().also { list ->
            for (i in list.indices.reversed()) {
                val turn = list[i]
                if (turn !is ChatTurn.AssistantTurn) continue
                for (b in turn.blocks.indices.reversed()) {
                    val block = turn.blocks[b]
                    if (block is AssistantBlock.Reasoning) {
                        list[i] = turn.copy(blocks = turn.blocks.subList(0, b) +
                            block.copy(text = block.text + delta) +
                            turn.blocks.subList(b + 1, turn.blocks.size))
                        return
                    }
                    if (block is AssistantBlock.MessageText) return
                }
            }
            list.add(ChatTurn.AssistantTurn(listOf(AssistantBlock.Reasoning(text = delta, streaming = true))))
        }
    }

    private fun updateAssistantBlock(index: Int, transform: (AssistantBlock) -> AssistantBlock) {
        _turns.value = _turns.value.toMutableList().also { list ->
            val last = list.lastOrNull() as? ChatTurn.AssistantTurn ?: return
            if (index < last.blocks.size) {
                list[list.size - 1] = last.copy(blocks = last.blocks.subList(0, index) +
                    transform(last.blocks[index]) +
                    last.blocks.subList(index + 1, last.blocks.size))
            }
        }
    }

    // Returns the block's index inside the (new) last assistant turn for later in-place updates.
    private fun appendAssistantBlock(block: AssistantBlock): Int? {
        var insertedIndex: Int? = null
        _turns.value = _turns.value.toMutableList().also { list ->
            val last = list.lastOrNull()
            if (last is ChatTurn.AssistantTurn) {
                list[list.size - 1] = last.copy(blocks = last.blocks + block)
                insertedIndex = last.blocks.size
            } else {
                list.add(ChatTurn.AssistantTurn(listOf(block)))
                insertedIndex = 0
            }
        }
        if (block is AssistantBlock.MessageText) {
            streamingTextBuilder = streamingTextBuilder ?: StringBuilder()
        }
        return insertedIndex
    }

    class factory(private val app: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = QodexAppViewModel(app) as T
    }
}
