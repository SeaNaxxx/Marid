package com.qodex.app.rpc

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import java.util.Base64
import java.util.TreeMap
import java.util.zip.CRC32

/**
 * V4 physical wire reassembly (port of packages/shared/src/qodex-protocol-v4/wire-reassembly.ts).
 *
 * Conversation/sessions-index events arrive as wire envelopes on
 * onDynamicConversationFrame / onDynamicSessionsIndexFrame:
 *  - kind "complete": the logical topic frame is carried atomically under `frame`.
 *  - kind "fragment": base64 shards of one UTF-8 JSON logical frame; only a full
 *    shard set passing the CRC-32 check and envelope match may be applied.
 *
 * A logical frame that fails any check is rejected with its reasonCode so the
 * caller can log it — silently dropping frames would desync the projection.
 */
class WireFrameAssembler(private val maxAssemblyBytes: Int = 16 * 1024 * 1024) {

    sealed interface Outcome {
        /** A logical topic frame ready to apply. */
        data class Ready(val logical: JsonObject, val deliveryKind: String?) : Outcome
        data object Incomplete : Outcome
        data class Rejected(val reasonCode: String) : Outcome
    }

    private class Staging(val first: JsonObject) {
        val shards = TreeMap<Int, ByteArray>()
        var bytes = 0
    }

    // logicalFrameId -> staging; keyed per logical frame, ordinals only index inside it.
    private val staging = HashMap<String, Staging>()

    fun accept(wire: JsonObject): Outcome {
        val kind = wire.string("kind") ?: return Outcome.Rejected("proto.wireEnvelopeInvalid")
        val logicalFrameId = wire.string("logicalFrameId")
            ?: return Outcome.Rejected("proto.wireEnvelopeInvalid")
        return if (kind == "complete") {
            val frame = wire["frame"] as? JsonObject
                ?: return Outcome.Rejected("proto.wireEnvelopeInvalid")
            if (frame.string("topic") != wire.string("topic") ||
                frame.string("subscriptionId") != wire.string("subscriptionId")
            ) {
                return Outcome.Rejected("proto.frameAssemblyMetadataMismatch")
            }
            staging.remove(logicalFrameId)
            Outcome.Ready(frame, wire.string("deliveryKind"))
        } else if (kind == "fragment") {
            acceptFragment(wire, logicalFrameId)
        } else {
            Outcome.Rejected("proto.wireEnvelopeInvalid")
        }
    }

    private fun acceptFragment(wire: JsonObject, logicalFrameId: String): Outcome {
        val fragmentCount = wire.int("fragmentCount")
            ?: return Outcome.Rejected("proto.wireEnvelopeInvalid")
        val fragmentIndex = wire.int("fragmentIndex")
            ?: return Outcome.Rejected("proto.wireEnvelopeInvalid")
        val logicalBytes = wire.int("logicalBytes")
            ?: return Outcome.Rejected("proto.wireEnvelopeInvalid")
        val dataBase64 = wire.string("dataBase64")
            ?: return Outcome.Rejected("proto.wireEnvelopeInvalid")
        val checksum = (wire["checksum"] as? JsonObject)?.string("value")
            ?: return Outcome.Rejected("proto.wireEnvelopeInvalid")

        if (fragmentCount > 1024) return Outcome.Rejected("proto.frameFragmentCountExceeded")
        if (logicalBytes > maxAssemblyBytes) return Outcome.Rejected("proto.frameAssemblyTooLarge")
        if (fragmentIndex < 0 || fragmentIndex >= fragmentCount || fragmentCount > logicalBytes) {
            return Outcome.Rejected("proto.frameAssemblyMetadataMismatch")
        }

        val slot = staging.getOrPut(logicalFrameId) {
            // Fragments carry identical envelope metadata; the first one seeds the slot.
            Staging(wire)
        }
        val first = slot.first
        val metaMismatch = listOf(
            wire.int("logicalFrameOrdinal") != first.int("logicalFrameOrdinal"),
            wire.string("deliveryKind") != first.string("deliveryKind"),
            wire.string("topic") != first.string("topic"),
            wire.string("subscriptionId") != first.string("subscriptionId"),
            fragmentCount != first.int("fragmentCount"),
            logicalBytes != first.int("logicalBytes"),
            checksum != (first["checksum"] as? JsonObject)?.string("value"),
        ).any { it }
        if (metaMismatch) {
            staging.remove(logicalFrameId)
            return Outcome.Rejected("proto.frameAssemblyMetadataMismatch")
        }

        val decoded = try {
            Base64.getDecoder().decode(dataBase64)
        } catch (_: IllegalArgumentException) {
            staging.remove(logicalFrameId)
            return Outcome.Rejected("proto.frameAssemblyInvalidBase64")
        }
        val previous = slot.shards[fragmentIndex]
        if (previous != null) {
            if (!previous.contentEquals(decoded)) {
                staging.remove(logicalFrameId)
                return Outcome.Rejected("proto.frameAssemblyFragmentConflict")
            }
            return Outcome.Incomplete
        }
        if (slot.bytes + decoded.size > maxAssemblyBytes) {
            staging.remove(logicalFrameId)
            return Outcome.Rejected("proto.frameAssemblyTooLarge")
        }
        if (slot.bytes + decoded.size > logicalBytes) {
            staging.remove(logicalFrameId)
            return Outcome.Rejected("proto.frameAssemblyLengthMismatch")
        }
        slot.shards[fragmentIndex] = decoded
        slot.bytes += decoded.size

        for (index in 0 until fragmentCount) {
            if (!slot.shards.containsKey(index)) return Outcome.Incomplete
        }
        if (slot.bytes != logicalBytes) {
            staging.remove(logicalFrameId)
            return Outcome.Rejected("proto.frameAssemblyLengthMismatch")
        }

        val logical = ByteArray(slot.bytes)
        var offset = 0
        for (index in 0 until fragmentCount) {
            val shard = slot.shards.getValue(index)
            System.arraycopy(shard, 0, logical, offset, shard.size)
            offset += shard.size
        }
        staging.remove(logicalFrameId)

        val crc = CRC32().apply { update(logical) }
        if (crc.value.toString(16).padStart(8, '0') != checksum) {
            return Outcome.Rejected("proto.frameAssemblyChecksumMismatch")
        }
        val json = runCatching {
            kotlinx.serialization.json.Json.parseToJsonElement(String(logical, Charsets.UTF_8))
        }.getOrNull() as? JsonObject ?: return Outcome.Rejected("proto.frameAssemblyInvalidJson")
        if (json.string("topic") != first.string("topic") ||
            json.string("subscriptionId") != first.string("subscriptionId")
        ) {
            return Outcome.Rejected("proto.frameAssemblyMetadataMismatch")
        }
        return Outcome.Ready(json, first.string("deliveryKind"))
    }

    fun reset() {
        staging.clear()
    }
}

private fun JsonObject.string(key: String): String? =
    (this[key] as? JsonPrimitive)?.takeIf { it !is kotlinx.serialization.json.JsonNull }?.content

private fun JsonObject.int(key: String): Int? = (this[key] as? JsonPrimitive)?.content?.toIntOrNull()
