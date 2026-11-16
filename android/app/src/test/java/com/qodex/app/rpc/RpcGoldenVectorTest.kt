package com.qodex.app.rpc

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Golden-vector conformance tests — the vectors were produced by running the
 * REAL TypeScript serializer (packages/rpc/src/serialization.ts) under Bun
 * (scripts/lucide-to-compose sibling workflow; generator:
 * android/app/src/test/gen-golden-vectors.mjs).
 *
 * These prove the Kotlin port is byte-identical on the wire to the
 * React/desktop client: a Kotlin-built request must be accepted by the
 * unmodified Qodex engine, and engine replies must parse identically.
 */
class RpcGoldenVectorTest {

    private val vectors: Map<String, String> by lazy {
        val stream = javaClass.getResourceAsStream("/rpc-golden-vectors.json")
            ?: error("rpc-golden-vectors.json missing from test resources")
        val text = stream.readBytes().decodeToString()
        val root = Json.parseToJsonElement(text) as JsonObject
        val cases = (root["cases"] as kotlinx.serialization.json.JsonArray).map {
            val obj = it as JsonObject
            (obj["name"] as JsonPrimitive).content to (obj["hex"] as JsonPrimitive).content
        }
        val frames = (root["frames"] as kotlinx.serialization.json.JsonArray).map {
            val obj = it as JsonObject
            (obj["name"] as JsonPrimitive).content to (obj["hex"] as JsonPrimitive).content
        }
        (cases + frames).toMap()
    }

    private fun serialize(header: RpcValue, body: RpcValue): ByteArray =
        RpcSerialization.serializeMessage(header, body)

    private fun assertGolden(name: String, actual: ByteArray) {
        val expectedHex = vectors[name] ?: error("missing golden case '$name'")
        val actualHex = actual.joinToString("") { "%02x".format(it) }
        assertEquals("case $name", expectedHex, actualHex)
    }

    private fun hexToBytes(hex: String): ByteArray =
        hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

    // ------------------------------------------------------------------
    // Serialize == golden
    // ------------------------------------------------------------------

    @Test
    fun `request-initialize is byte-identical to TS serializer`() {
        val header = RpcValue.Arr(
            listOf(
                RpcValue.IntValue(100),
                RpcValue.IntValue(0),
                RpcValue.Str("qodex-agent"),
                RpcValue.Str("initialize"),
            ),
        )
        val body = RpcValue.Obj(
            buildJsonObject { put("workspacePath", "/data/data/com.qodex.app/files/workspace") },
        )
        assertGolden("request-initialize", serialize(header, body))
    }

    @Test
    fun `request-send-text envelope is byte-identical to TS serializer`() {
        val header = RpcValue.Arr(
            listOf(
                RpcValue.IntValue(100),
                RpcValue.IntValue(1),
                RpcValue.Str("qodex-agent"),
                RpcValue.Str("sendConversationCommandV4"),
            ),
        )
        val body = RpcValue.Obj(
            buildJsonObject {
                put("workspacePath", "/data/data/com.qodex.app/files/workspace")
                put(
                    "envelope",
                    buildJsonObject {
                        put("commandId", "0194a2b3-c4d5-7e89-f012-3456789abcde")
                        put("clientId", "qodex-android")
                        put("sessionId", JsonNull)
                        put("type", "sendText")
                        put(
                            "payload",
                            buildJsonObject { put("text", "هلا") },
                        )
                        put("issuedAt", 1763240000000L)
                        put("inputOrigin", "mobile")
                    },
                )
            },
        )
        assertGolden("request-send-text", serialize(header, body))
    }

    @Test
    fun `request-event-listen is byte-identical to TS serializer`() {
        val header = RpcValue.Arr(
            listOf(
                RpcValue.IntValue(102),
                RpcValue.IntValue(2),
                RpcValue.Str("qodex-agent"),
                RpcValue.Str("onDynamicSessionEvent"),
            ),
        )
        val body = RpcValue.Obj(
            buildJsonObject {
                put("workspacePath", "/data/data/com.qodex.app/files/workspace")
                put("deliveryKind", "live")
            },
        )
        assertGolden("request-event-listen", serialize(header, body))
    }

    @Test
    fun `request-event-dispose is byte-identical to TS serializer`() {
        val header = RpcValue.Arr(
            listOf(RpcValue.IntValue(103), RpcValue.IntValue(2)),
        )
        assertGolden("request-event-dispose", serialize(header, RpcValue.Undefined))
    }

    @Test
    fun `response-initialize is byte-identical to TS serializer`() {
        assertGolden("response-initialize", serialize(RpcValue.Arr(listOf(RpcValue.IntValue(200))), RpcValue.Undefined))
    }

    @Test
    fun `response-success-string is byte-identical to TS serializer`() {
        val header = RpcValue.Arr(listOf(RpcValue.IntValue(201), RpcValue.IntValue(42)))
        assertGolden("response-success-string", serialize(header, RpcValue.Str("ok")))
    }

    @Test
    fun `response-success-view is byte-identical to TS serializer`() {
        val header = RpcValue.Arr(listOf(RpcValue.IntValue(201), RpcValue.IntValue(43)))
        val body = RpcValue.Obj(
            buildJsonObject {
                put("revision", 7)
                put(
                    "providers",
                    buildJsonArray {
                        add(
                            buildJsonObject {
                                put("id", "z-ai")
                                put(
                                    "models",
                                    buildJsonArray {
                                        add(buildJsonObject { put("id", "glm-5.3") })
                                        add(buildJsonObject { put("id", "glm-4.7") })
                                    },
                                )
                            },
                        )
                        add(
                            buildJsonObject {
                                put("id", "mock-provider")
                                put("models", buildJsonArray { add(buildJsonObject { put("id", "mock-model") }) })
                            },
                        )
                    },
                )
                put(
                    "preferredSelection",
                    buildJsonObject {
                        put("providerId", "z-ai")
                        put("modelId", "glm-5.3")
                    },
                )
            },
        )
        assertGolden("response-success-view", serialize(header, body))
    }

    @Test
    fun `response-error is byte-identical to TS serializer`() {
        val header = RpcValue.Arr(listOf(RpcValue.IntValue(202), RpcValue.IntValue(44)))
        val body = RpcValue.Obj(
            buildJsonObject {
                put("message", "Provider Registry 中不存在 Model")
                put("name", "Error")
                put("stack", buildJsonArray { add(JsonPrimitive("at f ()")) })
            },
        )
        assertGolden("response-error", serialize(header, body))
    }

    @Test
    fun `response-event-fire is byte-identical to TS serializer`() {
        val header = RpcValue.Arr(listOf(RpcValue.IntValue(204), RpcValue.IntValue(7)))
        val body = RpcValue.Obj(
            buildJsonObject {
                put("kind", "session.event")
                put("seq", 12)
                put(
                    "parts",
                    buildJsonArray {
                        add(
                            buildJsonObject {
                                put("type", "text")
                                put("value", "هلا")
                            },
                        )
                    },
                )
                put("ratio", 0.75)
            },
        )
        assertGolden("response-event-fire", serialize(header, body))
    }

    @Test
    fun `value-int edge cases are byte-identical to TS serializer`() {
        assertGolden(
            "value-int-zero",
            serialize(RpcValue.Arr(listOf(RpcValue.IntValue(0))), RpcValue.IntValue(0)),
        )
        assertGolden(
            "value-int-maxish",
            serialize(RpcValue.Arr(listOf(RpcValue.IntValue(6))), RpcValue.IntValue(268435455)),
        )
        assertGolden(
            "value-string-unicode",
            serialize(RpcValue.Arr(listOf(RpcValue.IntValue(1))), RpcValue.Str("مرحبا Qodex")),
        )
    }

    @Test
    fun `value-array-mixed is byte-identical to TS serializer`() {
        val header = RpcValue.Arr(listOf(RpcValue.IntValue(4)))
        val body = RpcValue.Arr(
            listOf(
                RpcValue.IntValue(1),
                RpcValue.Str("two"),
                RpcValue.Obj(JsonPrimitive(true)),
                RpcValue.Obj(JsonNull),
                RpcValue.Obj(buildJsonObject { put("three", 3) }),
            ),
        )
        assertGolden("value-array-mixed", serialize(header, body))
    }

    // ------------------------------------------------------------------
    // Deserialize == golden
    // ------------------------------------------------------------------

    @Test
    fun `deserializing golden request-send-text round-trips`() {
        val bytes = hexToBytes(vectors["request-send-text"] ?: error("missing"))
        val reader = ByteReader(bytes)
        val header = RpcSerialization.deserializeValue(reader) as RpcValue.Arr
        assertEquals(100, (header.items[0] as RpcValue.IntValue).value)
        assertEquals(1, (header.items[1] as RpcValue.IntValue).value)
        assertEquals("qodex-agent", (header.items[2] as RpcValue.Str).value)
        assertEquals("sendConversationCommandV4", (header.items[3] as RpcValue.Str).value)
        val body = RpcSerialization.deserializeValue(reader) as RpcValue.Obj
        val obj = body.json as JsonObject
        assertEquals("sendText", (obj["envelope"] as JsonObject)["type"]?.let { (it as JsonPrimitive).content })
        assertEquals(
            "هلا",
            (((obj["envelope"] as JsonObject)["payload"] as JsonObject)["text"] as JsonPrimitive).content,
        )
        assertEquals(1763240000000L, ((obj["envelope"] as JsonObject)["issuedAt"] as JsonPrimitive).content.toLong())
        assertFalse(reader.remaining > 0)
    }

    // ------------------------------------------------------------------
    // Socket framing == golden
    // ------------------------------------------------------------------

    @Test
    fun `framing golden frames are byte-identical`() {
        val payloadHex = vectors["request-initialize"] ?: error("missing case")
        val framed = SocketFraming.frameRegular(hexToBytes(payloadHex))
        assertGolden("frame-of-request-initialize", framed)
    }

    @Test
    fun `chunked framing reassembles across fragments`() {
        val payload = hexToBytes(vectors["request-initialize"]!!)
        val framed = SocketFraming.frameRegular(payload)
        val stream = ChunkStream()
        // Feed in awkward fragments.
        var offset = 0
        val sizes = listOf(3, 7, 2, 11, framed.size)
        var produced = 0
        for (size in sizes) {
            if (offset >= framed.size) break
            val end = (offset + size).coerceAtMost(framed.size)
            stream.acceptChunk(framed.copyOfRange(offset, end))
            offset = end
            if (stream.hasCompleteFrame()) {
                val header = stream.peekHeader()!!
                assertEquals(SocketFraming.TYPE_REGULAR, header[0])
                assertEquals(payload.size, header[3])
                stream.skip(SocketFraming.HEADER_SIZE)
                val body = stream.read(header[3])!!
                assertArrayEquals(payload, body)
                produced++
            }
        }
        assertEquals(1, produced)
    }

    @Test
    fun `vql encodes 128 the same as the TS writer`() {
        // TS: writeInt32VQL(writer, 128) -> [0x80, 0x01]
        val writer = ByteWriter()
        RpcSerialization.writeIntVql(writer, 128)
        assertArrayEquals(byteArrayOf(0x80.toByte(), 0x01), writer.toByteArray())
    }
}
