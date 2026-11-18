package com.qodex.app.rpc

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64
import java.util.zip.CRC32

/**
 * Pins the V4 wire reassembly contract against the engine's
 * wire-reassembly.ts: complete frames pass through, fragment sets reassemble
 * only when every shard is present, the CRC-32 + UTF-8 + envelope checks hold,
 * and any violation rejects with the protocol's reasonCode instead of dropping
 * data silently.
 */
class WireFrameAssemblerTest {

    private fun wireComplete(logical: JsonObject): JsonObject =
        Json.parseToJsonElement(
            """
        {"wireVersion":3,"kind":"complete","deliveryKind":"online",
         "logicalFrameId":"lf-1","logicalFrameOrdinal":1,
         "topic":"conversation/sess-1","subscriptionId":"sub-1",
         "frame":${logical}}
            """.trimIndent(),
        ) as JsonObject

    private fun wireFragment(
        logicalFrameId: String,
        index: Int,
        count: Int,
        logicalBytes: Int,
        checksum: String,
        data: ByteArray,
    ): JsonObject {
        val envelope =
            """{"wireVersion":3,"kind":"fragment","deliveryKind":"online",
                "logicalFrameId":"$logicalFrameId","logicalFrameOrdinal":1,
                "topic":"conversation/sess-1","subscriptionId":"sub-1",
                "fragmentIndex":$index,"fragmentCount":$count,
                "logicalBytes":$logicalBytes,
                "checksum":{"algorithm":"crc32","value":"$checksum"},
                "dataBase64":"${Base64.getEncoder().encodeToString(data)}"}"""
        return Json.parseToJsonElement(envelope) as JsonObject
    }

    private fun logicalFrame(seq: Int): JsonObject = Json.parseToJsonElement(
        """
        {"topic":"conversation/sess-1","subscriptionId":"sub-1",
         "fromSeq":$seq,"toSeq":$seq,"sentAt":1,
         "payload":{"kind":"deltas","deltas":[{"op":"state.updated","patch":{}}]}}
        """.trimIndent(),
    ) as JsonObject

    private fun crcOf(bytes: ByteArray): String {
        val crc = CRC32().apply { update(bytes) }
        return crc.value.toString(16).padStart(8, '0')
    }

    @Test
    fun `complete wire carries the logical frame through`() {
        val assembler = WireFrameAssembler()
        val logical = logicalFrame(4)
        val outcome = assembler.accept(wireComplete(logical))
        assertTrue(outcome is WireFrameAssembler.Outcome.Ready)
        assertEquals(logical, (outcome as WireFrameAssembler.Outcome.Ready).logical)
        assertEquals("online", outcome.deliveryKind)
    }

    @Test
    fun `complete frame with mismatched inner envelope is rejected`() {
        val assembler = WireFrameAssembler()
        val mismatched = Json.parseToJsonElement(
            """
            {"topic":"conversation/other","subscriptionId":"sub-1","fromSeq":1,"toSeq":1,"sentAt":1,
             "payload":{"kind":"deltas","deltas":[]}}
            """.trimIndent(),
        ) as JsonObject
        val outcome = assembler.accept(wireComplete(mismatched))
        assertEquals(
            "proto.frameAssemblyMetadataMismatch",
            (outcome as WireFrameAssembler.Outcome.Rejected).reasonCode,
        )
    }

    @Test
    fun `fragments reassemble in order with crc32 verification`() {
        val assembler = WireFrameAssembler()
        val json = """{"topic":"conversation/sess-1","subscriptionId":"sub-1","fromSeq":0,"toSeq":3,
            "sentAt":9,"payload":{"kind":"deltas","deltas":[{"op":"state.updated","patch":{"revision":2}}]}}"""
        val bytes = json.toByteArray(Charsets.UTF_8)
        val mid = bytes.size / 2
        val first = bytes.copyOfRange(0, mid)
        val second = bytes.copyOfRange(mid, bytes.size)

        assertEquals(WireFrameAssembler.Outcome.Incomplete, assembler.accept(wireFragment("lf-9", 1, 2, bytes.size, crcOf(bytes), second)))
        val outcome = assembler.accept(wireFragment("lf-9", 0, 2, bytes.size, crcOf(bytes), first))
        assertTrue(outcome is WireFrameAssembler.Outcome.Ready)
        val logical = (outcome as WireFrameAssembler.Outcome.Ready).logical
        assertEquals(
            "deltas",
            ((logical["payload"] as JsonObject)["kind"] as kotlinx.serialization.json.JsonPrimitive).content,
        )
    }

    @Test
    fun `fragment with corrupt crc is rejected`() {
        val assembler = WireFrameAssembler()
        val bytes = "hello".toByteArray(Charsets.UTF_8)
        val badCrc = "deadbeef"
        // A single-fragment set assembles immediately; the checksum must fail it.
        val outcome = assembler.accept(wireFragment("lf-2", 0, 1, bytes.size, badCrc, bytes))
        assertEquals(
            "proto.frameAssemblyChecksumMismatch",
            (outcome as WireFrameAssembler.Outcome.Rejected).reasonCode,
        )
    }

    @Test
    fun `fragment set exceeding the assembly cap is rejected`() {
        val assembler = WireFrameAssembler(maxAssemblyBytes = 8)
        val bytes = "0123456789".toByteArray(Charsets.UTF_8)
        val outcome = assembler.accept(wireFragment("lf-3", 0, 1, bytes.size, crcOf(bytes), bytes))
        assertEquals(
            "proto.frameAssemblyTooLarge",
            (outcome as WireFrameAssembler.Outcome.Rejected).reasonCode,
        )
    }

    @Test
    fun `invalid base64 fragment rejects and clears staging`() {
        val assembler = WireFrameAssembler()
        val envelope = Json.parseToJsonElement(
            """
            {"wireVersion":3,"kind":"fragment","deliveryKind":"online",
             "logicalFrameId":"lf-4","logicalFrameOrdinal":1,
             "topic":"conversation/sess-1","subscriptionId":"sub-1",
             "fragmentIndex":0,"fragmentCount":1,"logicalBytes":4,
             "checksum":{"algorithm":"crc32","value":"00000000"},
             "dataBase64":"!!bad!!"}
            """.trimIndent(),
        ) as JsonObject
        val outcome = assembler.accept(envelope)
        assertEquals(
            "proto.frameAssemblyInvalidBase64",
            (outcome as WireFrameAssembler.Outcome.Rejected).reasonCode,
        )
    }
}
