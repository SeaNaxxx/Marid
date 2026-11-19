package com.qodex.app.state

import android.app.Application
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.qodex.app.rpc.ChannelClient
import com.qodex.app.rpc.QodexRpcClient
import com.qodex.app.rpc.RpcCallException
import com.qodex.app.rpc.RpcConnectionState
import com.qodex.app.rpc.RpcEvents
import com.qodex.app.rpc.WireFrameAssembler
import com.qodex.app.rpc.asArrayOrNull
import com.qodex.app.rpc.asObjectOrNull
import com.qodex.app.rpc.asStringOrNull
import com.qodex.app.runtime.AgentKeepAliveService
import com.qodex.app.runtime.EngineProcess
import com.qodex.app.runtime.Provisioner
import com.qodex.app.runtime.RuntimeBus
import com.qodex.app.runtime.RuntimePhase
import com.qodex.app.ui.ChatTurn
import com.qodex.app.ui.ModelEntry2c
import com.qodex.app.ui.fetchModelEntries
import com.qodex.app.utils.FileLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID

// Boot orchestration: provision -> engine spawn -> RPC connect -> V4 conversation
// subscription. The UI renders RuntimeBus.phase plus the projection below.
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
        if (current is RuntimePhase.Provisioning || current is RuntimePhase.StartingEngine) {
            // Single-flight boot: Activity recreation re-fires boot() while a
            // provision/engine-start is already running; a duplicate would race it.
            return
        }
        if (current is RuntimePhase.EngineReady) return
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
                watchHandshakeTimeout()
            } catch (t: Throwable) {
                RuntimeBus.append("engine start failed: ${t.message}")
                RuntimeBus.setPhase(RuntimePhase.Failed("engine", "${t.message}"))
            }
        }
    }

    // An alive-but-silent engine used to sit on the dots screen forever; the
    // Initialize handshake must arrive within a bounded window or boot fails visibly.
    private suspend fun watchHandshakeTimeout() {
        delay(45_000L)
        if (_rpcState.value != RpcConnectionState.CONNECTED && EngineProcess.isRunning) {
            RuntimeBus.append("engine handshake timeout (45s) — RPC Initialize never arrived")
            FileLogger.append("RPC", "engine handshake timeout (45s)")
            RuntimeBus.setPhase(
                RuntimePhase.Failed("rpc", "engine did not complete its RPC handshake (45s) — tap Retry"),
            )
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
                    onEngineConnected()
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
        conversation?.dispose()
        conversation = null
        v4HandshakeDone = false
        _activeSessionId.value = null
        scope.launch {
            RuntimeBus.append("engine restart: $reason")
            EngineProcess.stop()
            startEngine(app)
        }
    }

    fun retry() {
        val app = this.app
        _rpcState.value = RpcConnectionState.DISCONNECTED
        rpc = null
        conversation?.dispose()
        conversation = null
        v4HandshakeDone = false
        _activeSessionId.value = null
        RuntimeBus.setPhase(RuntimePhase.Idle)
        scope.launch {
            // stop() destroys, sleeps and sweeps /proc — keep it off the main thread.
            EngineProcess.stop()
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

    // True while the last turnHeader row reports a running turn; drives the composer busy state.
    private val _turnRunning = MutableStateFlow(false)
    val turnRunning: StateFlow<Boolean> = _turnRunning.asStateFlow()

    // Set when the engine refuses model execution because no provider/model is
    // configured; the UI routes the user to Model Settings instead of failing silently.
    private val _providerNotReady = MutableStateFlow(false)
    val providerNotReady: StateFlow<Boolean> = _providerNotReady.asStateFlow()

    fun onComposerTextChange(text: String) {
        _composerText.value = text
    }

    private var currentSessionId: String? = null
    private var currentSubscriptionId: String? = null
    private var sending = false

    private val _activeSessionId = MutableStateFlow<String?>(null)
    val activeSessionId: StateFlow<String?> = _activeSessionId.asStateFlow()

    private val _sidebarLoading = MutableStateFlow(false)
    val sidebarLoading: StateFlow<Boolean> = _sidebarLoading.asStateFlow()

    // The V4 hello/clientHello handshake happens once per engine connection;
    // re-running it for a later session attach would double-initialize.
    @Volatile
    private var v4HandshakeDone = false

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
        val previous = _selectedModel.value
        _selectedModel.value = entry
        val client = rpc ?: return
        val sessionId = currentSessionId ?: return
        scope.launch {
            runCatching {
                client.agentSetModel(workspacePath, sessionId, entry.providerId, entry.modelId)
            }.onFailure {
                // Roll the picker back so the UI never shows a selection the engine rejected.
                _selectedModel.value = previous
                RuntimeBus.append("model switch failed: ${it.message}")
                FileLogger.append("RPC", "Error on setModel: ${it.message}")
            }.onSuccess {
                RuntimeBus.append("model switched: ${entry.providerId}/${entry.modelId}")
            }
        }
    }

    // ------------------------------------------------------------------
    // V4 conversation wiring (validated end-to-end against entry-stdio.js
    // by scripts/qodex-android/conversation-smoke.mts):
    // project registration -> createSession(deferred) -> hello/clientHello
    // handshake -> subscribeConversationV4 -> onDynamicConversationFrame.
    // ------------------------------------------------------------------

    private class Conversation(
        val subscription: ChannelClient.EventSubscription,
        val assembler: WireFrameAssembler,
        val projection: ConversationProjection,
    ) {
        fun dispose() {
            subscription.dispose()
            assembler.reset()
            projection.clear()
        }
    }

    private var conversation: Conversation? = null

    private fun onEngineConnected() {
        loadModels()
        loadSidebarTasks()
        startConversation()
    }

    private fun startConversation() {
        val client = rpc ?: return
        scope.launch {
            try {
                // Detach the previous stream first: frames from a stale session
                // must never bleed into the new one.
                conversation?.dispose()
                conversation = null
                _turns.value = emptyList()
                val projectId = ensureWorkspaceProject(client)
                val prepared = client.projectPrepareSession(projectId)?.asObjectOrNull()
                val sessionWorkspacePath = prepared?.get("workspacePath").asStringOrNull() ?: workspacePath
                val created = client.agentCreateSession(
                    workspacePath = sessionWorkspacePath,
                    projectId = projectId,
                )?.asObjectOrNull()
                val sessionId = created?.get("session")?.asObjectOrNull()?.get("sessionId").asStringOrNull()
                    ?: created?.get("sessionId").asStringOrNull()
                if (sessionId == null) {
                    RuntimeBus.append("conversation boot failed: createSession returned no sessionId")
                    return@launch
                }
                attachConversation(client, sessionWorkspacePath, sessionId)
            } catch (t: Throwable) {
                RuntimeBus.append("conversation boot failed: ${t.message}")
                FileLogger.append("RPC", "conversation boot failed: ${t.message}")
                surfaceConversationError(t, booting = true)
            }
        }
    }

    // New task: a fresh engine session with a clean transcript. The composer
    // draft is preserved on purpose — starting a task is not a text deletion.
    fun startNewConversation() {
        _providerNotReady.value = false
        startConversation()
    }

    // Open a task from the sidebar: subscribe to its existing session. The
    // engine's subscribe path sends a full snapshot (persisted messages
    // included), so history re-renders through the normal frame stream.
    fun openTask(sessionId: String) {
        val client = rpc ?: run {
            RuntimeBus.append("open task: engine RPC not connected")
            return
        }
        if (sessionId == _activeSessionId.value) return
        _providerNotReady.value = false
        scope.launch {
            try {
                conversation?.dispose()
                conversation = null
                _turns.value = emptyList()
                attachConversation(client, workspacePath, sessionId)
            } catch (t: Throwable) {
                RuntimeBus.append("open task failed: ${t.message}")
                FileLogger.append("RPC", "open task failed: ${t.message}")
                surfaceConversationError(t, booting = true)
            }
        }
    }

    private suspend fun attachConversation(
        client: QodexRpcClient,
        sessionWorkspacePath: String,
        sessionId: String,
    ) {
        currentSessionId = sessionId
        currentSubscriptionId = null
        _activeSessionId.value = sessionId

        ensureHandshake(client)

        val subscribed = client.subscribeConversationV4(sessionWorkspacePath, sessionId)
        val subscriptionId = subscribed?.asObjectOrNull()
            ?.get("ack")?.asObjectOrNull()?.get("subscriptionId").asStringOrNull()
            ?: subscribed?.asObjectOrNull()?.get("subscriptionId").asStringOrNull()
        if (subscriptionId == null) {
            RuntimeBus.append("conversation attach failed: no subscriptionId in ack")
            return
        }
        currentSubscriptionId = subscriptionId
        RuntimeBus.append("conversation subscribed: session=$sessionId sub=$subscriptionId")

        val assembler = WireFrameAssembler()
        val projection = ConversationProjection()
        val frames = client.conversationFrames(sessionWorkspacePath) { wire ->
            handleWireFrame(assembler, projection, wire)
        }
        conversation = Conversation(frames, assembler, projection)
        loadSidebarTasks()
    }

    // Standalone-server entries refuse project-less session creation
    // (project.remote-project-required); reuse the project bound to the
    // workspace root, creating it once on first boot.
    private suspend fun ensureWorkspaceProject(client: QodexRpcClient): String {
        val existing = client.projectReadProjects()?.asObjectOrNull()?.get("projects").asArrayOrNull()
        val match = existing?.firstNotNullOfOrNull { entry ->
            val project = entry.asObjectOrNull() ?: return@firstNotNullOfOrNull null
            val roots = project.get("roots").asArrayOrNull().orEmpty()
            val hasRoot = roots.any {
                it.asObjectOrNull()?.get("rootPath").asStringOrNull() == workspacePath
            }
            if (hasRoot) project.get("id").asStringOrNull() else null
        }
        if (match != null) return match
        val created = client.projectSaveProject("Qodex Workspace", workspacePath)?.asObjectOrNull()
        val id = created?.get("id").asStringOrNull()
            ?: throw IllegalStateException("saveProject returned no project id")
        RuntimeBus.append("workspace project created: $id")
        return id
    }

    private suspend fun ensureHandshake(client: QodexRpcClient) {
        if (v4HandshakeDone) return
        val hello = client.helloConversationV4()?.asObjectOrNull()
        val protocolVersion = (hello?.get("protocolVersion") as? JsonPrimitive)?.content?.toIntOrNull() ?: 3
        val clientKind = if (hello?.get("clientMode").asStringOrNull() == "desktop-continuous") {
            "desktop"
        } else {
            "mobileApp"
        }
        client.initializeConversationV4(
            buildJsonObject {
                put("kind", "clientHello")
                put("protocolVersion", protocolVersion)
                put("clientId", "qodex-android")
                put("clientKind", clientKind)
                put("appVersion", "qodex-android")
                put("capabilities", buildJsonObject { put("workspaceHookReviewUi", true) })
            },
        )
        v4HandshakeDone = true
    }

    private fun handleWireFrame(
        assembler: WireFrameAssembler,
        projection: ConversationProjection,
        wire: kotlinx.serialization.json.JsonElement?,
    ) {
        val wireObj = wire as? JsonObject ?: return
        when (val outcome = assembler.accept(wireObj)) {
            is WireFrameAssembler.Outcome.Ready -> {
                if (projection.applyLogicalFrame(outcome.logical)) {
                    renderProjection(projection)
                }
            }
            is WireFrameAssembler.Outcome.Rejected ->
                RuntimeBus.append("conversation frame rejected: ${outcome.reasonCode}")
            WireFrameAssembler.Outcome.Incomplete -> Unit
        }
    }

    private fun renderProjection(projection: ConversationProjection) {
        val view = projection.view()
        val running = view.rows
            .lastOrNull { it.string("kind") == "turnHeader" }
            ?.string("state") == "running"
        _turnRunning.value = running
        _turns.value = mapRowsToTurns(view.rows)
    }

    // sendText envelope over the qodex-agent channel; the engine echoes the
    // userInput row back through the frame stream, so no local bubble is appended.
    fun sendText(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty() || sending) return
        val client = rpc ?: run {
            // Silence was the defect: a dead send must be visible in the transcript.
            surfaceConversationError(
                IllegalStateException("engine RPC is not connected — reconnect and try again"),
                booting = true,
            )
            return
        }
        if (currentSessionId == null || currentSubscriptionId == null) {
            RuntimeBus.append("send: conversation not subscribed yet; retrying boot")
            surfaceConversationError(
                IllegalStateException("conversation is still connecting — try again in a moment"),
                booting = true,
            )
            startConversation()
            return
        }
        sending = true
        scope.launch {
            try {
                val envelope = buildJsonObject {
                    put("commandId", UUID.randomUUID().toString())
                    put("clientId", "qodex-android")
                    put("sessionId", currentSessionId)
                    put("type", "sendText")
                    put("payload", buildJsonObject { put("text", trimmed) })
                    put("issuedAt", System.currentTimeMillis())
                }
                val ack = client.sendConversationCommandV4(
                    buildJsonObject {
                        put("workspacePath", workspacePath)
                        put("envelope", envelope)
                    },
                )?.asObjectOrNull()
                val status = ack?.get("status").asStringOrNull()
                if (status == "accepted") {
                    // The accepted ack means the engine owns this turn: clear
                    // the composer and refresh the sidebar so the new task
                    // title appears without a manual refresh.
                    _composerText.value = ""
                    loadSidebarTasks()
                } else if (status != null) {
                    val reason = ack?.get("message").asStringOrNull()
                        ?: ack?.get("reasonCode").asStringOrNull()
                        ?: "command rejected"
                    surfaceConversationError(IllegalStateException(reason), booting = false)
                }
                RuntimeBus.append("send ack: ${status ?: "null"}")
            } catch (t: Throwable) {
                RuntimeBus.append("send failed: ${t.message}")
                FileLogger.append("RPC", "Error on sendText: ${t.message}")
                surfaceConversationError(t, booting = false)
            } finally {
                sending = false
            }
        }
    }

    // Engine failures belong in the transcript — silence is the defect this
    // replaces. Provider-not-ready additionally flips the settings gate so the
    // composer can route the user to Model Settings.
    private fun surfaceConversationError(t: Throwable, booting: Boolean) {
        val notReady = (t as? RpcCallException)?.code == "QODEX_AGENT_PROVIDER_NOT_READY"
        if (notReady) _providerNotReady.value = true
        val prefix = if (booting) "[engine]" else "[turn error]"
        val message = t.message?.take(300) ?: "unknown engine error"
        appendSystemBlock("$prefix $message")
    }

    // User-facing retry after configuring providers in Model Settings.
    fun retryConversation() {
        _providerNotReady.value = false
        val client = rpc ?: return
        if (_rpcState.value != RpcConnectionState.CONNECTED) return
        scope.launch {
            runCatching { loadModels() }
            startConversation()
        }
    }

    // ------------------------------------------------------------------
    // Sidebar task list: REAL engine sessions via qodex-agent.listSessions.
    // ------------------------------------------------------------------

    private val _sidebarTasks = MutableStateFlow<List<com.qodex.app.ui.QodexSidebarTask>>(emptyList())
    val sidebarTasks: StateFlow<List<com.qodex.app.ui.QodexSidebarTask>> = _sidebarTasks.asStateFlow()

    fun loadSidebarTasks() {
        val client = rpc ?: return
        _sidebarLoading.value = true
        scope.launch {
            val sessions = runCatching {
                client.agentListSessions(workspacePath)
                    ?.asObjectOrNull()?.get("sessions").asArrayOrNull().orEmpty()
            }.getOrElse {
                RuntimeBus.append("sidebar sessions fetch failed: ${it.message}")
                emptyList()
            }
            val tasks = sessions.mapNotNull { entry ->
                val session = entry.asObjectOrNull() ?: return@mapNotNull null
                val id = session.get("sessionId").asStringOrNull() ?: return@mapNotNull null
                val status = session.get("status").asStringOrNull().orEmpty()
                val updatedAt = session.get("updatedAt")
                    .let { (it as? JsonPrimitive)?.content?.toLongOrNull() }
                com.qodex.app.ui.QodexSidebarTask(
                    key = id,
                    title = session.get("title").asStringOrNull()?.ifEmpty { null } ?: "Untitled task",
                    timeLabel = updatedAt?.let { formatRelativeTime(it) },
                    updatedAtMs = updatedAt ?: 0L,
                    isRunning = status == "running" || status == "waiting",
                    isError = status == "error",
                    isActive = id == _activeSessionId.value,
                )
            }
            _sidebarTasks.value = tasks
            _sidebarLoading.value = false
        }
    }

    private fun formatRelativeTime(epochMs: Long): String {
        val delta = System.currentTimeMillis() - epochMs
        val minutes = delta / 60_000
        return when {
            minutes < 1 -> "now"
            minutes < 60 -> "${minutes}m"
            minutes < 60 * 24 -> "${minutes / 60}h"
            else -> "${minutes / (60 * 24)}d"
        }
    }

    private fun appendSystemBlock(text: String) {
        _turns.value = _turns.value + ChatTurn.AssistantTurn(listOf(com.qodex.app.ui.AssistantBlock.MessageText(text)))
    }

    class factory(private val app: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = QodexAppViewModel(app) as T
    }
}
