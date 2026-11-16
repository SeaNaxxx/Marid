package com.marid.app.rpc

import java.io.ByteArrayOutputStream

/**
 * Marid RPC socket framing — Kotlin port of `packages/rpc/src/protocol.ts`.
 *
 * Message frame format (13-byte header):
 * ┌─────────┬──────────┬──────────┬──────────────┐
 * │ type(1) │  id(4)   │  ack(4)  │  length(4)   │
 * └─────────┴──────────┴──────────┴──────────────┘
 *
 * Only frames with type=Regular carry RPC payloads; Ack/Control/KeepAlive are
 * handled by PersistentProtocol upstream and are ignored by the basic
 * SocketProtocol used for the browser WebSocket client (and by us).
 */
object SocketFraming {
    const val TYPE_NONE = 0
    const val TYPE_REGULAR = 1
    const val TYPE_CONTROL = 2
    const val TYPE_ACK = 3
    const val TYPE_DISCONNECT = 5
    const val TYPE_REPLAY_REQUEST = 6
    const val TYPE_PAUSE = 7
    const val TYPE_RESUME = 8
    const val TYPE_KEEP_ALIVE = 9

    const val HEADER_SIZE = 13 // 1 + 4 + 4 + 4

    /** Wraps an RPC payload into a Regular frame (id=0, ack=0, like SocketProtocol.send). */
    fun frameRegular(payload: ByteArray): ByteArray {
        val out = ByteArray(HEADER_SIZE + payload.size)
        out[0] = TYPE_REGULAR.toByte()
        writeUInt32BE(out, 1, 0L)
        writeUInt32BE(out, 5, 0L)
        writeUInt32BE(out, 9, payload.size.toLong())
        System.arraycopy(payload, 0, out, HEADER_SIZE, payload.size)
        return out
    }
}

/**
 * Reassembles fragmented stream bytes into framed messages — port of
 * ChunkStream: buffers incoming chunks, lets the protocol peek 13-byte
 * headers and only consume a frame once its full body has arrived.
 */
class ChunkStream {
    private val out = ByteArrayOutputStream()
    private var totalLength = 0

    val byteLength: Int get() = totalLength

    fun acceptChunk(chunk: ByteArray) {
        out.write(chunk)
        totalLength += chunk.size
    }

    /** Returns true when a complete frame (header + body) is buffered. */
    fun hasCompleteFrame(): Boolean {
        if (totalLength < SocketFraming.HEADER_SIZE) return false
        val header = peek(SocketFraming.HEADER_SIZE) ?: return false
        val length = readUInt32BE(header, 9).toInt()
        return totalLength >= SocketFraming.HEADER_SIZE + length
    }

    /** Parsed header: [type, id, ack, bodyLength]. */
    fun peekHeader(): IntArray? {
        val header = peek(SocketFraming.HEADER_SIZE) ?: return null
        return intArrayOf(
            header[0].toInt() and 0xFF,
            readUInt32BE(header, 1).toInt(),
            readUInt32BE(header, 5).toInt(),
            readUInt32BE(header, 9).toInt(),
        )
    }

    fun skip(count: Int) {
        read(count) ?: throw IllegalStateException("ChunkStream.skip($count) beyond readable range")
    }

    fun read(count: Int): ByteArray? {
        if (totalLength < count) return null
        val all = out.toByteArray()
        val result = all.copyOfRange(0, count)
        out.reset()
        out.write(all, count, all.size - count)
        totalLength -= count
        return result
    }

    private fun peek(byteCount: Int): ByteArray? {
        if (totalLength < byteCount) return null
        val all = out.toByteArray()
        return all.copyOfRange(0, byteCount)
    }
}

/** unsigned 32-bit BE helpers (Long keeps the unsigned magnitude). */
fun writeUInt32BE(target: ByteArray, offset: Int, value: Long) {
    target[offset] = ((value ushr 24) and 0xFF).toByte()
    target[offset + 1] = ((value ushr 16) and 0xFF).toByte()
    target[offset + 2] = ((value ushr 8) and 0xFF).toByte()
    target[offset + 3] = (value and 0xFF).toByte()
}

fun readUInt32BE(source: ByteArray, offset: Int): Long =
    ((source[offset].toLong() and 0xFF) shl 24) or
        ((source[offset + 1].toLong() and 0xFF) shl 16) or
        ((source[offset + 2].toLong() and 0xFF) shl 8) or
        (source[offset + 3].toLong() and 0xFF)
