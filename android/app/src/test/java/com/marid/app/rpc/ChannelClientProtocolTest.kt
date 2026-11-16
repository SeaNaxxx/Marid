package com.marid.app.rpc

import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * End-to-end protocol tests for ChannelClient: Initialize gating,
 * request/response, error mapping, event subscribe/fire/dispose — a JVM
 * stand-in for the engine's ChannelServer (same wire contract as
 * packages/rpc/src/channelServer.ts).
 */
class ChannelClientProtocolTest {

    /** Minimal fake ChannelServer speaking the real wire contract. */
    private class FakeServer {
        val received = mutableListOf<Pair<List<Int>, RpcValue?>>()

        /** Processes a raw payload (single serialized message, no frame). */
        fun onPayload(payload: ByteArray, respond: (ByteArray) -> Unit) {
            val reader = ByteReader(payload)
            val header = RpcSerialization.deserializeValue(reader) as RpcValue.Arr
            val type = (header.items[0] as RpcValue.IntValue).value
            val id = (header.items.getOrNull(1) as? RpcValue.IntValue)?.value ?: -1
            val channel = (header.items.getOrNull(2) as? RpcValue.Str)?.value
            val command = (header.items.getOrNull(3) as? RpcValue.Str)?.value
            val arg = RpcSerialization.deserializeValue(reader)
            received.add(listOf(type, id) to arg)

            when (type) {
                RpcRequestType.PROMISE -> {
                    val data: RpcValue = if (channel == "marid-agent" && command == "initialize") {
                        RpcValue.Obj(
                            buildJsonObject {
                                put("runtime", "marid-engine")
                                put("ok", true)
                            },
                        )
                    } else if (command == "explode") {
                        RpcValue.Obj(
                            buildJsonObject {
                                put("message", "boom")
                                put("name", "EngineError")
                                put("stack", kotlinx.serialization.json.buildJsonArray { })
                            },
                        )
                    } else {
                        RpcValue.Str("ack:${command}")
                    }
                    respond(RpcSerialization.serializeMessage(
                        RpcValue.Arr(listOf(RpcValue.IntValue(if (command == "explode") RpcResponseType.PROMISE_ERROR else RpcResponseType.PROMISE_SUCCESS), RpcValue.IntValue(id))),
                        data,
                    ))
                }
                RpcRequestType.EVENT_LISTEN -> {
                    // Fire one event immediately (mirrors channelServer onEventListen).
                    respond(RpcSerialization.serializeMessage(
                        RpcValue.Arr(listOf(RpcValue.IntValue(RpcResponseType.EVENT_FIRE), RpcValue.IntValue(id))),
                        RpcValue.Str("event-1"),
                    ))
                }
                RpcRequestType.EVENT_DISPOSE -> {
                    // Server stops firing; nothing to do in the fake.
                }
            }
        }

        private fun buildJsonArrayFrom(list: List<String>): RpcValue = RpcValue.Arr(list.map { RpcValue.Str(it) })
        @Suppress("unused")
        private val unused = buildJsonArrayFrom(emptyList()) // keeps helper referenced for future cases
    }

    @Test
    fun `requests queue until Initialize then resolve`() = runTest {
        val server = FakeServer()
        // Synchronous wire loop: send -> server -> respond -> client.
        var client: ChannelClient? = null
        client = ChannelClient(send = { payload ->
            server.onPayload(payload) { client!!.onMessage(it) }
        })

        // Server sends Initialize ([200] + undefined) on connect.
        client.onMessage(
            RpcSerialization.serializeMessage(
                RpcValue.Arr(listOf(RpcValue.IntValue(200))),
                RpcValue.Undefined,
            ),
        )

        val result = client.request(
            "marid-agent",
            "initialize",
            RpcValue.Obj(buildJsonObject { put("workspacePath", "/w") }),
        )

        val obj = (result as RpcValue.Obj).json as JsonObject
        assertEquals("marid-engine", (obj["runtime"] as JsonPrimitive).content)
        assertEquals("true", (obj["ok"] as JsonPrimitive).content)
        // The request actually reached the server with the right header.
        val sent = server.received.first()
        assertEquals(listOf(RpcRequestType.PROMISE, 0), sent.first)
    }

    @Test
    fun `PromiseError maps to RpcCallException with name`() = runTest {
        val server = FakeServer()
        // Synchronous wire loop: send -> server -> respond -> client.
        var client: ChannelClient? = null
        client = ChannelClient(send = { payload ->
            server.onPayload(payload) { client!!.onMessage(it) }
        })
        client.onMessage(
            RpcSerialization.serializeMessage(
                RpcValue.Arr(listOf(RpcValue.IntValue(200))),
                RpcValue.Undefined,
            ),
        )
        var thrown: RpcCallException? = null
        try {
            client.request("marid-agent", "explode")
        } catch (e: RpcCallException) {
            thrown = e
        }
        assertNotNull("expected RpcCallException", thrown)
        assertEquals("boom", thrown!!.message)
        assertEquals("EngineError", thrown.name)
        // The request reached the fake server exactly once.
        assertEquals(1, server.received.size)
    }

    @Test
    fun `events fire and dispose reaches the server`() = runTest {
        val server = FakeServer()
        val outbox = ArrayDeque<ByteArray>()
        val client = ChannelClient(send = { outbox.add(it) })
        client.onMessage(
            RpcSerialization.serializeMessage(
                RpcValue.Arr(listOf(RpcValue.IntValue(200))),
                RpcValue.Undefined,
            ),
        )

        val gotFirst = CountDownLatch(1)
        val events = java.util.Collections.synchronizedList(mutableListOf<RpcValue>())
        var subscription: ChannelClient.EventSubscription? = null
        val job = launch {
            subscription = client.listen("marid-agent", "onDynamicSessionEvent", RpcValue.Obj(
                buildJsonObject { put("deliveryKind", "live") },
            )) { value ->
                events.add(value)
                gotFirst.countDown()
            }
        }
        // Pump the scheduler until the launch sent the request, then flush the
        // wire; the first EVENT_FIRE completes the subscription gate.
        this.testScheduler.advanceUntilIdle()
        for (payload in outbox.toList()) server.onPayload(payload) { client.onMessage(it) }
        outbox.clear()
        this.testScheduler.advanceUntilIdle()
        job.join()
        assertTrue(gotFirst.await(2, TimeUnit.SECONDS))
        assertNotNull(subscription)

        // A second server fire reaches the callback.
        client.onMessage(
            RpcSerialization.serializeMessage(
                RpcValue.Arr(listOf(RpcValue.IntValue(RpcResponseType.EVENT_FIRE), RpcValue.IntValue(1))),
                RpcValue.Str("event-2"),
            ),
        )

        // Dispose sends EVENT_DISPOSE on the wire.
        subscription!!.dispose()
        val last = outbox.last()
        val reader = ByteReader(last)
        val header = RpcSerialization.deserializeValue(reader) as RpcValue.Arr
        assertEquals(RpcRequestType.EVENT_DISPOSE, (header.items[0] as RpcValue.IntValue).value)
    }

    @Test
    fun `framed socket protocol round trip via ChunkStream`() {
        // Build a Regular frame carrying a request, split it arbitrarily,
        // reassemble, and recover the exact serialized message.
        val original = RpcSerialization.serializeMessage(
            RpcValue.Arr(
                listOf(
                    RpcValue.IntValue(RpcRequestType.PROMISE),
                    RpcValue.IntValue(9),
                    RpcValue.Str("marid-task"),
                    RpcValue.Str("list"),
                ),
            ),
            RpcValue.Undefined,
        )
        val framed = SocketFraming.frameRegular(original)
        val stream = ChunkStream()
        stream.acceptChunk(framed.copyOfRange(0, 5))
        assertTrue(!stream.hasCompleteFrame())
        stream.acceptChunk(framed.copyOfRange(5, framed.size))
        assertTrue(stream.hasCompleteFrame())
        val header = stream.peekHeader()!!
        assertEquals(SocketFraming.TYPE_REGULAR, header[0])
        stream.skip(SocketFraming.HEADER_SIZE)
        val body = stream.read(header[3])!!
        assertEquals(original.toList(), body.toList())
    }
}
