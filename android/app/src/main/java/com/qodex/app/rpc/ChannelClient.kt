package com.qodex.app.rpc

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.JsonObject

// Server sends Initialize (200) right after socket open; requests queue until then (port of packages/rpc/src/channelClient.ts).
object RpcRequestType {
    const val PROMISE = 100
    const val PROMISE_CANCEL = 101
    const val EVENT_LISTEN = 102
    const val EVENT_DISPOSE = 103
}

object RpcResponseType {
    const val INITIALIZE = 200
    const val PROMISE_SUCCESS = 201
    const val PROMISE_ERROR = 202
    const val PROMISE_ERROR_OBJ = 203
    const val EVENT_FIRE = 204
    const val EVENT_ERROR = 205
}

/** Error carrying the passthrough fields the TS client lifts onto Error. */
class RpcCallException(
    message: String,
    val name: String = "Error",
) : Exception(message)

private sealed class PendingReply {
    class Promise(val deferred: CompletableDeferred<RpcValue>) : PendingReply()
    class Event(val onEvent: (RpcValue) -> Unit) : PendingReply()
}

class ChannelClient(
    private val send: (ByteArray) -> Unit,
    private val log: (String) -> Unit = {},
    // Fires when the engine's Initialize (200) arrives — the server-side handshake anchor for boot.log.
    private val onReady: () -> Unit = {},
) {
    @Volatile
    private var initialized = false

    @Volatile
    private var disposed = false
    private var lastRequestId = 0
    private val mutex = Mutex()
    private val pending = HashMap<Int, PendingReply>()
    private val initWaiters = ArrayList<CompletableDeferred<Unit>>()
    private val establishedEvents = HashMap<Int, ChannelClient.EventSubscription>()

    fun onMessage(payload: ByteArray) {
        val reader = ByteReader(payload)
        val header = RpcSerialization.deserializeValue(reader)
        val headerArr = (header as? RpcValue.Arr)?.items
            ?: throw IllegalStateException("RPC message header is not an array")
        val type = (headerArr.getOrNull(0) as? RpcValue.IntValue)?.value ?: -1

        when (type) {
            RpcResponseType.INITIALIZE -> onInitialized()
            RpcResponseType.PROMISE_SUCCESS,
            RpcResponseType.PROMISE_ERROR,
            RpcResponseType.PROMISE_ERROR_OBJ,
            -> {
                val id = (headerArr.getOrNull(1) as? RpcValue.IntValue)?.value ?: return
                val data = RpcSerialization.deserializeValue(reader)
                val reply = synchronized(this) { pending.remove(id) }
                (reply as? PendingReply.Promise)?.let { p ->
                    when (type) {
                        RpcResponseType.PROMISE_SUCCESS, RpcResponseType.PROMISE_ERROR_OBJ ->
                            p.deferred.complete(data)
                        else -> p.deferred.completeExceptionally(toError(data))
                    }
                }
            }
            RpcResponseType.EVENT_FIRE -> {
                val id = (headerArr.getOrNull(1) as? RpcValue.IntValue)?.value ?: return
                val data = RpcSerialization.deserializeValue(reader)
                val reply = synchronized(this) { pending[id] }
                (reply as? PendingReply.Event)?.let { p ->
                    fireFirst(id)
                    p.onEvent(data)
                }
            }
            RpcResponseType.EVENT_ERROR -> {
                val id = (headerArr.getOrNull(1) as? RpcValue.IntValue)?.value ?: return
                val data = RpcSerialization.deserializeValue(reader)
                val reply = synchronized(this) { pending.remove(id) }
                (reply as? PendingReply.Event)?.let { p ->
                    fireFirst(id)
                    log("RPC event subscription failed: ${describeError(data)}")
                }
            }
        }
    }

    private val firstFireWaiters = HashMap<Int, CompletableDeferred<Unit>>()

    private fun fireFirst(id: Int) {
        val w = synchronized(this) { firstFireWaiters.remove(id) }
        w?.complete(Unit)
    }

    private fun onInitialized() {
        onReady()
        val waiters: List<CompletableDeferred<Unit>>
        synchronized(this) {
            initialized = true
            waiters = ArrayList(initWaiters)
            initWaiters.clear()
        }
        for (w in waiters) w.complete(Unit)
    }

    private suspend fun awaitInitialized() {
        val waiter = synchronized(this) {
            if (initialized || disposed) return
            CompletableDeferred<Unit>().also { initWaiters.add(it) }
        }
        waiter.await()
    }

    suspend fun request(
        channelName: String,
        command: String,
        arg: RpcValue = RpcValue.Undefined,
    ): RpcValue {
        check(!disposed) { "ChannelClient is disposed" }
        awaitInitialized()
        val id = mutex.withLock { lastRequestId++ }

        val deferred = CompletableDeferred<RpcValue>()
        synchronized(this) { pending[id] = PendingReply.Promise(deferred) }

        val header = RpcValue.Arr(
            listOf(
                RpcValue.IntValue(RpcRequestType.PROMISE),
                RpcValue.IntValue(id),
                RpcValue.Str(channelName),
                RpcValue.Str(command),
            ),
        )
        send(RpcSerialization.serializeMessage(header, arg))
        return try {
            deferred.await()
        } finally {
            synchronized(this) { pending.remove(id) }
        }
    }

    // Subscription stays active until dispose() (sends EventDispose).
    suspend fun listen(
        channelName: String,
        event: String,
        arg: RpcValue = RpcValue.Undefined,
        onEvent: (RpcValue) -> Unit,
    ): EventSubscription {
        check(!disposed) { "ChannelClient is disposed" }
        awaitInitialized()
        val id = mutex.withLock { lastRequestId++ }

        val firstFire = CompletableDeferred<Unit>()
        synchronized(this) {
            pending[id] = PendingReply.Event(onEvent)
            firstFireWaiters[id] = firstFire
        }

        val header = RpcValue.Arr(
            listOf(
                RpcValue.IntValue(RpcRequestType.EVENT_LISTEN),
                RpcValue.IntValue(id),
                RpcValue.Str(channelName),
                RpcValue.Str(event),
            ),
        )
        send(RpcSerialization.serializeMessage(header, arg))

        val subscription = EventSubscription(id)
        // Wait for the first event fire or EventError so callers know the subscription is registered server-side.
        firstFire.await()
        synchronized(this) { establishedEvents[id] = subscription }
        return subscription
    }

    fun dispose(reason: String = "ChannelClient disposed") {
        synchronized(this) {
            if (disposed) return
            disposed = true
        }
        val waiters: List<CompletableDeferred<Unit>>
        synchronized(this) {
            waiters = ArrayList(initWaiters)
            initWaiters.clear()
        }
        for (w in waiters) w.complete(Unit)
        synchronized(this) {
            pending.clear()
            firstFireWaiters.forEach { (_, w) -> w.complete(Unit) }
            firstFireWaiters.clear()
        }
        log("ChannelClient disposed: $reason")
    }

    private fun describeError(data: RpcValue): String = when (data) {
        is RpcValue.Obj -> {
            val obj = data.json as? JsonObject
            val message = obj?.get("message") as? kotlinx.serialization.json.JsonPrimitive
            message?.content ?: "unknown"
        }
        else -> "unknown"
    }

    private fun toError(data: RpcValue): Exception = when (data) {
        is RpcValue.Obj -> {
            val obj = data.json as? JsonObject
            val message = (obj?.get("message") as? kotlinx.serialization.json.JsonPrimitive)?.content ?: "RPC error"
            val name = (obj?.get("name") as? kotlinx.serialization.json.JsonPrimitive)?.content ?: "Error"
            RpcCallException(message, name)
        }
        else -> RpcCallException("RPC error")
    }

    inner class EventSubscription(private val id: Int) {
        @Volatile
        private var active = true

        fun dispose() {
            if (!active) return
            active = false
            synchronized(this@ChannelClient) {
                pending.remove(id)
                establishedEvents.remove(id)
            }
            val header = RpcValue.Arr(
                listOf(
                    RpcValue.IntValue(RpcRequestType.EVENT_DISPOSE),
                    RpcValue.IntValue(id),
                ),
            )
            send(RpcSerialization.serializeMessage(header, RpcValue.Undefined))
        }
    }
}
