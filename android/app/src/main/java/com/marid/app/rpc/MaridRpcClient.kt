package com.marid.app.rpc

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
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

// Speaks the engine's ChannelClient protocol over ws://127.0.0.1:<port>/ws; channel names from packages/shared/src/channels.ts.
object ServiceChannels {
    const val MARID_AGENT = "marid-agent"
    const val MARID_TASK = "marid-task"
    const val MODEL_SELECTION = "model-selection"
    const val SETTING = "setting"
    const val TERMINAL = "terminal"
    const val FILE = "file"
    const val GIT = "git"
    const val CLIENT_CONFIG = "client-config"
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

class MaridRpcClient(
    private val baseUrl: String = "ws://127.0.0.1:3030",
    private val events: RpcEvents,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val http = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(0, TimeUnit.MILLISECONDS) // WebSocket: no read timeout
        .pingInterval(20, TimeUnit.SECONDS)
        .build()

    private var webSocket: WebSocket? = null
    private var channelClient: ChannelClient? = null
    private val chunkStream = ChunkStream()
    private val reconnectEnabled = AtomicBoolean(true)
    private var reconnectAttempt = 0
    private var reconnectJob: Job? = null

    @Volatile
    var state: RpcConnectionState = RpcConnectionState.DISCONNECTED
        private set

    fun connect() {
        if (state == RpcConnectionState.CONNECTING || state == RpcConnectionState.CONNECTED) return
        reconnectEnabled.set(true)
        events.onState(RpcConnectionState.CONNECTING)
        openSocket()
    }

    private fun openSocket() {
        val url = "$baseUrl/ws"
        val request = Request.Builder().url(url).build()
        events.onLog("RPC connecting: $url")

        webSocket = http.newWebSocket(
            request,
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) {
                    events.onLog("RPC socket open (ws handshake ok); waiting for Initialize")
                }

                override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                    val client = channelClient ?: return
                    chunkStream.acceptChunk(bytes.toByteArray())
                    readFrames(client)
                }

                override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                    events.onLog("RPC failure: ${t.message}")
                    setState(RpcConnectionState.DISCONNECTED)
                    scheduleReconnect()
                }

                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                    events.onLog("RPC closed: $code $reason")
                    setState(RpcConnectionState.DISCONNECTED)
                    scheduleReconnect()
                }
            },
        )
    }

    private fun readFrames(client: ChannelClient) {
        while (chunkStream.hasCompleteFrame()) {
            val header = chunkStream.peekHeader() ?: break
            val type = header[0]
            val bodyLength = header[3]
            if (type == SocketFraming.TYPE_REGULAR) {
                chunkStream.skip(SocketFraming.HEADER_SIZE)
                val body = if (bodyLength == 0) ByteArray(0) else chunkStream.read(bodyLength) ?: break
                if (body.isNotEmpty()) {
                    try {
                        client.onMessage(body)
                    } catch (t: Throwable) {
                        events.onLog("RPC message error: ${t.message}")
                    }
                } else {
                    try {
                        client.onMessage(ByteArray(0))
                    } catch (t: Throwable) {
                        events.onLog("RPC message error: ${t.message}")
                    }
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
                openSocket()
            }
        }
    }

    fun disconnect() {
        reconnectEnabled.set(false)
        reconnectJob?.cancel()
        webSocket?.close(1000, "client shutdown")
        webSocket = null
        channelClient?.dispose()
        channelClient = null
        setState(RpcConnectionState.DISCONNECTED)
    }

    fun shutdown() {
        disconnect()
        scope.cancel()
    }

    // Server creates a fresh ChannelServer per connection, so a fresh client per socket is the matching shape.
    private fun ensureClient(): ChannelClient {
        if (channelClient == null) {
            val client = ChannelClient(
                send = { payload ->
                    webSocket?.send(SocketFraming.frameRegular(payload).toByteString())
                },
                log = { events.onLog(it) },
            )
            channelClient = client
            setState(RpcConnectionState.CONNECTED)
        }
        return channelClient!!
    }

    suspend fun call(channel: String, command: String, arg: JsonObject? = null): JsonElement? {
        val client = ensureClient()
        val response = client.request(
            channel,
            command,
            arg?.let { RpcValue.Obj(it) } ?: RpcValue.Undefined,
        )
        return when (response) {
            is RpcValue.Obj -> response.json
            else -> null
        }
    }

    suspend fun listen(
        channel: String,
        event: String,
        arg: JsonObject? = null,
        onEvent: (JsonElement?) -> Unit,
    ): ChannelClient.EventSubscription {
        val client = ensureClient()
        return client.listen(
            channel,
            event,
            arg?.let { RpcValue.Obj(it) } ?: RpcValue.Undefined,
        ) { value ->
            onEvent((value as? RpcValue.Obj)?.json)
        }
    }

    suspend fun agentInitialize(workspacePath: String): JsonElement? = call(
        ServiceChannels.MARID_AGENT,
        "initialize",
        buildJsonObject { put("workspacePath", workspacePath) },
    )

    suspend fun agentListSessions(workspacePath: String): JsonElement? = call(
        ServiceChannels.MARID_AGENT,
        "listSessions",
        buildJsonObject { put("workspacePath", workspacePath) },
    )

    suspend fun agentCreateSession(workspacePath: String, sessionId: String? = null): JsonElement? {
        val arg = buildJsonObject {
            put("workspacePath", workspacePath)
            sessionId?.let { put("sessionId", it) }
        }
        return call(ServiceChannels.MARID_AGENT, "createSession", arg)
    }

    suspend fun sendConversationCommandV4(params: JsonObject): JsonElement? =
        call(ServiceChannels.MARID_AGENT, "sendConversationCommandV4", params)

    suspend fun readSessionMessages(params: JsonObject): JsonElement? =
        call(ServiceChannels.MARID_AGENT, "readSessionMessages", params)

    suspend fun subscribeSessionEvents(
        params: JsonObject,
        onEvent: (JsonElement?) -> Unit,
    ): ChannelClient.EventSubscription = listen(
        ServiceChannels.MARID_AGENT,
        "onDynamicSessionEvent",
        params,
        onEvent,
    )

    suspend fun modelSelectionView(): JsonElement? =
        call(ServiceChannels.MODEL_SELECTION, "getView")

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
