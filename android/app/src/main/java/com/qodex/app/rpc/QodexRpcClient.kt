package com.qodex.app.rpc

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import com.qodex.app.utils.FileLogger
import java.io.InputStream
import java.io.OutputStream
import java.util.concurrent.atomic.AtomicBoolean

// Speaks the engine's ChannelClient protocol over the engine process's stdio
// pipes (entry-stdio.ts); channel names from packages/shared/src/channels.ts.
// No socket, no OkHttp: frames are the same 13-byte SocketProtocol headers the
// TS side writes directly onto the raw pipe bytes.
object ServiceChannels {
    const val QODEX_AGENT = "qodex-agent"
    const val QODEX_TASK = "qodex-task"
    const val MODEL_SELECTION = "model-selection"
    const val PROVIDER_SETTINGS = "provider-settings"
    const val SETTING = "setting"
    const val TERMINAL = "terminal"
    const val FILE = "file"
    const val GIT = "git"
    const val CLIENT_CONFIG = "client-config"
    const val PROJECT = "project"
    const val SKILLS = "skills"
    const val SUBAGENTS = "subagents"
    const val COMMANDS = "commands"
    const val HOOKS = "hooks"
    const val MEMORY = "memory"
    const val PLUGINS = "plugins"
    const val PLUGIN_MANAGEMENT = "plugin-management"
    const val USAGE_STATS = "usage-stats"
    const val MCP_SYNC = "mcp-sync"
}

enum class RpcConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
}

data class RpcEvents(
    val onState: (RpcConnectionState) -> Unit,
    val onLog: (String) -> Unit = {},
)

class QodexRpcClient(
    private val stdout: InputStream,
    private val stdin: OutputStream,
    private val events: RpcEvents,
    // stdio cannot re-dial: transport loss means the engine process must be
    // respawned by the host before frames can flow again.
    private val onTransportDown: (reason: String) -> Unit = {},
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var channelClient: ChannelClient? = null
    private val chunkStream = ChunkStream()
    private val reconnectEnabled = AtomicBoolean(true)
    private var reconnectAttempt = 0
    private var reconnectJob: Job? = null
    private val sendLock = Any()

    @Volatile
    var state: RpcConnectionState = RpcConnectionState.DISCONNECTED
        private set

    fun connect() {
        if (state == RpcConnectionState.CONNECTING || state == RpcConnectionState.CONNECTED) return
        reconnectEnabled.set(true)
        events.onState(RpcConnectionState.CONNECTING)
        // Client exists before the first frame so Initialize is never dropped;
        // CONNECTED flips only when the engine's Initialize handshake arrives.
        ensureClient()
        startReadLoop()
    }

    private fun startReadLoop() {
        val client = channelClient ?: return
        val buffer = ByteArray(64 * 1024)
        Thread {
            try {
                while (true) {
                    val n = stdout.read(buffer)
                    if (n < 0) {
                        transportDown("engine stdout closed")
                        return@Thread
                    }
                    if (n > 0) {
                        chunkStream.acceptChunk(buffer.copyOf(n))
                        readFrames(client)
                    }
                }
            } catch (t: Exception) {
                if (reconnectEnabled.get()) {
                    transportDown("engine stream error: ${t.message}")
                }
            }
        }.apply {
            isDaemon = true
            name = "qodex-rpc-read"
            start()
        }
    }

    private fun transportDown(reason: String) {
        events.onLog("RPC down: $reason")
        setState(RpcConnectionState.DISCONNECTED)
        scheduleReconnect()
        onTransportDown(reason)
    }

    private fun readFrames(client: ChannelClient) {
        while (chunkStream.hasCompleteFrame()) {
            val header = chunkStream.peekHeader() ?: break
            val type = header[0]
            val bodyLength = header[3]
            if (type == SocketFraming.TYPE_REGULAR) {
                chunkStream.skip(SocketFraming.HEADER_SIZE)
                val body = if (bodyLength == 0) ByteArray(0) else chunkStream.read(bodyLength) ?: break
                try {
                    client.onMessage(body)
                } catch (t: Throwable) {
                    events.onLog("RPC message error: ${t.message}")
                }
            } else {
                // Ack/KeepAlive/Control frames are not used by the basic SocketProtocol path; drop them.
                chunkStream.skip(SocketFraming.HEADER_SIZE + bodyLength)
            }
        }
    }

    private fun setState(next: RpcConnectionState) {
        if (state == next) return
        state = next
        events.onState(next)
    }

    private fun scheduleReconnect() {
        if (!reconnectEnabled.get()) return
        if (reconnectJob?.isActive == true) return
        reconnectJob = scope.launch {
            val delayMs = (1000L shl reconnectAttempt.coerceAtMost(3)).coerceAtMost(10_000L)
            reconnectAttempt++
            delay(delayMs)
            if (reconnectEnabled.get() && state == RpcConnectionState.DISCONNECTED) {
                onTransportDown("reconnect requested")
            }
        }
    }

    fun disconnect() {
        reconnectEnabled.set(false)
        reconnectJob?.cancel()
        channelClient?.dispose()
        channelClient = null
        setState(RpcConnectionState.DISCONNECTED)
    }

    fun shutdown() {
        disconnect()
        scope.cancel()
    }

    // One ChannelClient per engine process: the server sends Initialize once at
    // process start, so recreating the client mid-process would hang on the handshake.
    private fun ensureClient(): ChannelClient {
        if (channelClient == null) {
            val client = ChannelClient(
                send = { payload -> sendFrame(payload) },
                log = { events.onLog(it) },
                onReady = {
                    reconnectAttempt = 0
                    FileLogger.append("RPC", "Handshake complete: Initialize received -> Ready")
                    events.onLog("RPC handshake: Initialize received -> Ready")
                    setState(RpcConnectionState.CONNECTED)
                },
            )
            channelClient = client
        }
        return channelClient!!
    }

    private fun sendFrame(payload: ByteArray) {
        synchronized(sendLock) {
            stdin.write(SocketFraming.frameRegular(payload))
            stdin.flush()
        }
    }

    // Desktop ProxyChannel.toService packs method args as a single-element array
    // (channel.call(propKey, [params])) and ProxyChannel.fromService applies them
    // (target.apply(handler, args)) — a bare object arg is spread as an array-like
    // with no length, so every params-taking method would receive undefined and
    // fail server-side while the UI shows nothing. Service methods therefore must
    // be called with the params object wrapped in a one-element JSON array.
    suspend fun call(channel: String, command: String, arg: JsonObject? = null): JsonElement? {
        FileLogger.append("RPC", "Sending $channel.$command")
        val client = ensureClient()
        val response = try {
            client.request(
                channel,
                command,
                RpcValue.Arr(arg?.let { listOf(RpcValue.Obj(it)) } ?: emptyList()),
            )
        } catch (t: Throwable) {
            FileLogger.append("RPC", "Error on $channel.$command: ${t.message}")
            throw t
        }
        FileLogger.append("RPC", "Received response: $channel.$command")
        return when (response) {
            is RpcValue.Obj -> response.json
            else -> null
        }
    }

    // Engine service methods take positional args; the TS client proxy packs them as a JSON array (ProxyChannel.fromService spreads it back).
    suspend fun callPositional(
        channel: String,
        command: String,
        args: List<JsonElement>,
    ): JsonElement? {
        FileLogger.append("RPC", "Sending $channel.$command (${args.size} args)")
        val client = ensureClient()
        val response = try {
            client.request(channel, command, RpcValue.Obj(JsonArray(args)))
        } catch (t: Throwable) {
            FileLogger.append("RPC", "Error on $channel.$command: ${t.message}")
            throw t
        }
        FileLogger.append("RPC", "Received response: $channel.$command")
        return (response as? RpcValue.Obj)?.json
    }

    suspend fun listen(
        channel: String,
        event: String,
        arg: JsonObject? = null,
        onEvent: (JsonElement?) -> Unit,
    ): ChannelClient.EventSubscription {
        FileLogger.append("RPC", "Subscribing $channel.$event")
        val client = ensureClient()
        return try {
            client.listen(
                channel,
                event,
                arg?.let { RpcValue.Obj(it) } ?: RpcValue.Undefined,
            ) { value ->
                onEvent((value as? RpcValue.Obj)?.json)
            }
        } catch (t: Throwable) {
            FileLogger.append("RPC", "Error subscribing $channel.$event: ${t.message}")
            throw t
        }
    }

    suspend fun agentInitialize(workspacePath: String): JsonElement? = call(
        ServiceChannels.QODEX_AGENT,
        "initialize",
        buildJsonObject { put("workspacePath", workspacePath) },
    )

    suspend fun agentListSessions(workspacePath: String): JsonElement? = call(
        ServiceChannels.QODEX_AGENT,
        "listSessions",
        buildJsonObject { put("workspacePath", workspacePath) },
    )

    // Composer model switching applies to the live session; the engine persists
    // the selection into the canonical intent on the next sendText.
    suspend fun agentSetModel(
        workspacePath: String,
        sessionId: String,
        providerId: String,
        modelId: String,
    ): JsonElement? = call(
        ServiceChannels.QODEX_AGENT,
        "setModel",
        buildJsonObject {
            put("workspacePath", workspacePath)
            put("sessionId", sessionId)
            put(
                "model",
                buildJsonObject {
                    put("providerId", providerId)
                    put("modelId", modelId)
                },
            )
            put("persistAsWorkspaceLastUsed", true)
        },
    )

    suspend fun agentCreateSession(
        workspacePath: String,
        sessionId: String? = null,
        projectId: String? = null,
    ): JsonElement? {
        val arg = buildJsonObject {
            put("workspacePath", workspacePath)
            sessionId?.let { put("sessionId", it) }
            projectId?.let { put("projectId", it) }
            // session_input carries a session foreign key; a deferred draft is
            // promoted (master row persisted) by first-prompt admission. An
            // immediate create without a persisted master row makes the very
            // first sendText fail with FOREIGN KEY constraint failed.
            put("persistence", "deferred")
        }
        return call(ServiceChannels.QODEX_AGENT, "createSession", arg)
    }

    // V4 connection handshake: hello -> clientHello, once per connection before
    // any conversation/sessions-index subscription.
    suspend fun helloConversationV4(): JsonElement? =
        call(ServiceChannels.QODEX_AGENT, "helloConversationV4")

    suspend fun initializeConversationV4(clientHello: JsonObject): JsonElement? =
        call(ServiceChannels.QODEX_AGENT, "initializeConversationV4", clientHello)

    suspend fun subscribeConversationV4(
        workspacePath: String,
        sessionId: String,
    ): JsonElement? = call(
        ServiceChannels.QODEX_AGENT,
        "subscribeConversationV4",
        buildJsonObject {
            put("workspacePath", workspacePath)
            put("sessionId", sessionId)
        },
    )

    suspend fun unsubscribeConversationV4(
        workspacePath: String,
        subscriptionId: String,
    ): JsonElement? = call(
        ServiceChannels.QODEX_AGENT,
        "unsubscribeConversationV4",
        buildJsonObject {
            put("workspacePath", workspacePath)
            put("subscriptionId", subscriptionId)
        },
    )

    // Standalone-server entries route session creation through the project
    // system: without a project the create fails with project.remote-project-required.
    suspend fun projectReadProjects(): JsonElement? =
        call(ServiceChannels.PROJECT, "readProjects")

    suspend fun projectSaveProject(name: String, root: String): JsonElement? = call(
        ServiceChannels.PROJECT,
        "saveProject",
        buildJsonObject {
            put("name", name)
            put("roots", JsonArray(listOf(JsonPrimitive(root))))
        },
    )

    suspend fun projectPrepareSession(projectId: String): JsonElement? = call(
        ServiceChannels.PROJECT,
        "prepareSession",
        buildJsonObject { put("projectId", projectId) },
    )

    // V4 conversation frames: wire envelopes (complete / fragment) carrying the
    // logical topic frames; reassembly happens in WireFrameAssembler.
    suspend fun conversationFrames(
        workspacePath: String,
        onWireFrame: (JsonElement) -> Unit,
    ): ChannelClient.EventSubscription = listen(
        ServiceChannels.QODEX_AGENT,
        "onDynamicConversationFrame",
        buildJsonObject { put("workspacePath", workspacePath) },
        onWireFrame,
    )

    // sessions-index: workspace-level task list feed for the sidebar.
    suspend fun sessionsIndexFrames(
        workspacePath: String,
        onWireFrame: (JsonElement) -> Unit,
    ): ChannelClient.EventSubscription = listen(
        ServiceChannels.QODEX_AGENT,
        "onDynamicSessionsIndexFrame",
        buildJsonObject { put("workspacePath", workspacePath) },
        onWireFrame,
    )

    suspend fun sendConversationCommandV4(params: JsonObject): JsonElement? =
        call(ServiceChannels.QODEX_AGENT, "sendConversationCommandV4", params)

    suspend fun readSessionMessages(params: JsonObject): JsonElement? =
        call(ServiceChannels.QODEX_AGENT, "readSessionMessages", params)

    suspend fun subscribeSessionEvents(
        params: JsonObject,
        onEvent: (JsonElement?) -> Unit,
    ): ChannelClient.EventSubscription = listen(
        ServiceChannels.QODEX_AGENT,
        "onDynamicSessionEvent",
        params,
        onEvent,
    )

    suspend fun modelSelectionView(): JsonElement? =
        call(ServiceChannels.MODEL_SELECTION, "getView")

    suspend fun providerSettingsView(): JsonElement? =
        call(ServiceChannels.PROVIDER_SETTINGS, "getView")

    // QodexAgentService.testModelConnectivity takes one params object; the result is {success:true} or a thrown error.
    suspend fun testModelConnectivity(
        workspacePath: String,
        providerId: String,
        modelId: String,
    ): JsonElement? = call(
        ServiceChannels.QODEX_AGENT,
        "testModelConnectivity",
        buildJsonObject {
            put("workspacePath", workspacePath)
            put(
                "selection",
                buildJsonObject {
                    put("providerId", providerId)
                    put("modelId", modelId)
                },
            )
        },
    )

    suspend fun terminalCreate(cols: Int, rows: Int): JsonElement? = call(
        ServiceChannels.TERMINAL,
        "create",
        buildJsonObject {
            put("cols", cols)
            put("rows", rows)
        },
    )

    suspend fun terminalWrite(terminalId: String, data: String): JsonElement? = call(
        ServiceChannels.TERMINAL,
        "write",
        buildJsonObject {
            put("id", terminalId)
            put("data", data)
        },
    )

    suspend fun terminalDispose(terminalId: String): JsonElement? = call(
        ServiceChannels.TERMINAL,
        "dispose",
        buildJsonObject { put("id", terminalId) },
    )

    suspend fun terminalData(terminalId: String, onEvent: (JsonElement?) -> Unit): ChannelClient.EventSubscription =
        listen(
            ServiceChannels.TERMINAL,
            "onDynamicData",
            buildJsonObject { put("id", terminalId) },
            onEvent,
        )
}

// Engine payloads are read defensively — shapes follow the TS interfaces but must never crash the UI.

fun JsonElement?.asStringOrNull(): String? = (this as? JsonPrimitive)?.takeIf { it !is JsonNull }?.content

fun JsonElement?.asIntOrNull(): Int? = (this as? JsonPrimitive)?.content?.toIntOrNull()

fun JsonElement?.asBoolOrNull(): Boolean? = (this as? JsonPrimitive)?.booleanOrNull

fun JsonElement?.asArrayOrNull(): List<JsonElement>? = (this as? JsonArray)?.toList()

fun JsonElement?.asObjectOrNull(): JsonObject? = this as? JsonObject

fun JsonObject.modelSelectionModelEntries(): List<Pair<String, String>> {
    val result = mutableListOf<Pair<String, String>>()
    val providers = this["providers"].asArrayOrNull() ?: return result
    for (provider in providers) {
        val obj = provider.asObjectOrNull() ?: continue
        val providerId = (obj["id"] ?: obj["name"]).asStringOrNull() ?: continue
        val models = obj["models"].asArrayOrNull() ?: continue
        for (model in models) {
            val m = model.asObjectOrNull() ?: continue
            val modelId = (m["id"] ?: m["name"]).asStringOrNull() ?: continue
            result.add(providerId to modelId)
        }
    }
    return result
}
