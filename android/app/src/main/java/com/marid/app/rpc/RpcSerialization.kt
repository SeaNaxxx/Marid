package com.marid.app.rpc

import java.nio.charset.StandardCharsets
import java.util.Base64
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put

// VS Code-derived wire format: [1 type tag][VQL length][data]; pure JVM so golden vectors from the TS serializer can unit-test it.
sealed class RpcValue {
    object Undefined : RpcValue()
    data class Str(val value: String) : RpcValue()
    data class Binary(val bytes: ByteArray) : RpcValue() {
        override fun equals(other: Any?) = other is Binary && bytes.contentEquals(other.bytes)
        override fun hashCode() = bytes.contentHashCode()
    }
    data class IntValue(val value: Int) : RpcValue()
    data class Arr(val items: List<RpcValue>) : RpcValue()
    data class Obj(val json: JsonElement) : RpcValue()
}

object RpcSerialization {
    const val TYPE_UNDEFINED = 0
    const val TYPE_STRING = 1
    const val TYPE_BUFFER = 2
    const val TYPE_VSBUFFER = 3
    const val TYPE_ARRAY = 4
    const val TYPE_OBJECT = 5
    const val TYPE_INT = 6

    const val NESTED_UINT8_MARKER = "__marid_rpc_nested_uint8array_v1"
    const val NESTED_UINT8_BASE64_KEY = "base64"

    private val json = Json

    fun serialize(value: RpcValue): ByteArray {
        val writer = ByteWriter()
        serializeTo(writer, value)
        return writer.toByteArray()
    }

    fun serializeTo(writer: ByteWriter, value: RpcValue) {
        when (value) {
            is RpcValue.Undefined -> writer.writeByte(TYPE_UNDEFINED)
            is RpcValue.Str -> {
                val bytes = value.value.toByteArray(StandardCharsets.UTF_8)
                writer.writeByte(TYPE_STRING)
                writeIntVql(writer, bytes.size)
                writer.write(bytes)
            }
            is RpcValue.Binary -> {
                writer.writeByte(TYPE_BUFFER)
                writeIntVql(writer, value.bytes.size)
                writer.write(value.bytes)
            }
            is RpcValue.IntValue -> {
                writer.writeByte(TYPE_INT)
                writeIntVql(writer, value.value)
            }
            is RpcValue.Arr -> {
                writer.writeByte(TYPE_ARRAY)
                writeIntVql(writer, value.items.size)
                for (item in value.items) serializeTo(writer, item)
            }
            is RpcValue.Obj -> {
                val encoded = encodeNestedBinary(value.json)
                val bytes = json.encodeToString(JsonElement.serializer(), encoded)
                    .toByteArray(StandardCharsets.UTF_8)
                writer.writeByte(TYPE_OBJECT)
                writeIntVql(writer, bytes.size)
                writer.write(bytes)
            }
        }
    }

    fun serializeMessage(header: RpcValue, body: RpcValue): ByteArray {
        val writer = ByteWriter()
        serializeTo(writer, header)
        serializeTo(writer, body)
        return writer.toByteArray()
    }

    private fun encodeNestedBinary(element: JsonElement): JsonElement = when (element) {
        is JsonPrimitive -> element
        is JsonArray -> JsonArray(element.map { encodeNestedBinary(it) })
        is JsonObject -> {
            // {"__marid_rpc_nested_uint8array_v1": true, "base64": "..."} stays as-is.
            if (isNestedBinaryMarker(element)) {
                element
            } else {
                buildJsonObject {
                    element.forEach { (key, v) -> put(key, encodeNestedBinary(v)) }
                }
            }
        }
        else -> element
    }

    private fun isNestedBinaryMarker(obj: JsonObject): Boolean =
        obj.size == 2 &&
            obj[NESTED_UINT8_MARKER] == JsonPrimitive(true) &&
            obj[NESTED_UINT8_BASE64_KEY] is JsonPrimitive

    fun deserialize(bytes: ByteArray): RpcValue {
        val reader = ByteReader(bytes)
        return deserializeValue(reader)
    }

    fun deserializeValue(reader: ByteReader): RpcValue {
        val type = reader.readByte().toInt() and 0xFF
        return when (type) {
            TYPE_UNDEFINED -> RpcValue.Undefined
            TYPE_STRING -> RpcValue.Str(String(reader.read(readIntVql(reader)), StandardCharsets.UTF_8))
            TYPE_BUFFER, TYPE_VSBUFFER -> RpcValue.Binary(reader.read(readIntVql(reader)))
            TYPE_ARRAY -> {
                val length = readIntVql(reader)
                val items = ArrayList<RpcValue>(length)
                repeat(length) { items.add(deserializeValue(reader)) }
                RpcValue.Arr(items)
            }
            TYPE_OBJECT -> {
                val text = String(reader.read(readIntVql(reader)), StandardCharsets.UTF_8)
                RpcValue.Obj(decodeNestedBinary(json.parseToJsonElement(text)))
            }
            TYPE_INT -> RpcValue.IntValue(readIntVql(reader))
            else -> throw IllegalArgumentException("Unknown RPC data type tag: $type")
        }
    }

    private fun decodeNestedBinary(element: JsonElement): JsonElement = when (element) {
        is JsonObject ->
            if (isNestedBinaryMarker(element)) {
                val base64 = (element[NESTED_UINT8_BASE64_KEY] as? JsonPrimitive)?.contentOrNull ?: ""
                JsonPrimitive(base64) // keep payload as base64 string inside JSON objects
            } else {
                buildJsonObject {
                    element.forEach { (key, v) -> put(key, decodeNestedBinary(v)) }
                }
            }
        is JsonArray -> JsonArray(element.map { decodeNestedBinary(it) })
        else -> element
    }

    fun writeIntVql(writer: ByteWriter, value: Int) {
        if (value == 0) {
            writer.writeByte(0)
            return
        }
        var v = value
        val scratch = ByteArray(5)
        var i = 0
        while (v != 0) {
            scratch[i] = (v and 0x7F).toByte()
            v = v ushr 7
            if (v > 0) {
                scratch[i] = (scratch[i].toInt() or 0x80).toByte()
            }
            i++
        }
        writer.write(scratch, 0, i)
    }

    /** Mirrors readIntVQL in serialization.ts — JS `<<` signed-32 semantics. */
    fun readIntVql(reader: ByteReader): Int {
        var value = 0
        var n = 0
        while (true) {
            val next = reader.readByte().toInt() and 0xFF
            value = value or ((next and 0x7F) shl n)
            if (next and 0x80 == 0) {
                return value
            }
            n += 7
        }
    }
}

class ByteWriter {
    private val buffers = ArrayList<ByteArray>()
    private var pending = ByteArray(64)
    private var pendingLen = 0

    fun writeByte(b: Int) {
        if (pendingLen == pending.size) flushPending()
        pending[pendingLen++] = b.toByte()
    }

    fun write(bytes: ByteArray) = write(bytes, 0, bytes.size)

    fun write(bytes: ByteArray, offset: Int, length: Int) {
        if (length >= pending.size) {
            flushPending()
            buffers.add(bytes.copyOfRange(offset, offset + length))
        } else {
            if (pendingLen + length > pending.size) flushPending()
            System.arraycopy(bytes, offset, pending, pendingLen, length)
            pendingLen += length
        }
    }

    private fun flushPending() {
        if (pendingLen > 0) {
            buffers.add(pending.copyOf(pendingLen))
            pendingLen = 0
        }
    }

    fun toByteArray(): ByteArray {
        flushPending()
        val total = buffers.sumOf { it.size }
        val result = ByteArray(total)
        var pos = 0
        for (b in buffers) {
            System.arraycopy(b, 0, result, pos, b.size)
            pos += b.size
        }
        return result
    }
}

class ByteReader(private val buffer: ByteArray) {
    private var pos = 0

    val remaining: Int get() = buffer.size - pos

    fun readByte(): Byte {
        if (pos >= buffer.size) throw IllegalStateException("ByteReader underflow")
        return buffer[pos++]
    }

    fun read(count: Int): ByteArray {
        if (count < 0 || pos + count > buffer.size) {
            throw IllegalStateException("ByteReader underflow: need $count, have ${buffer.size - pos}")
        }
        val result = buffer.copyOfRange(pos, pos + count)
        pos += count
        return result
    }
}
