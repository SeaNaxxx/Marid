package com.qodex.app.rpc

import java.io.ByteArrayOutputStream

// 13-byte frame header [type(1) id(4) ack(4) length(4)]; only Regular frames carry payloads here (port of packages/rpc/src/protocol.ts).
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

    // Protocol ceiling (audit M-02): the header carries an unsigned 32-bit
    // length, so a corrupt or hostile stream can declare a 4 GiB frame whose
    // Int conversion would go negative and desynchronize the parser. Legitimate
    // NDJSON protocol payloads stay far below this bound; anything larger (or
    // more buffered bytes than one maximal frame needs) is rejected BEFORE
    // allocation so a stream error can never exhaust app memory. The RPC
    // client accepts one chunk then drains complete frames, so the buffer can
    // never legitimately exceed header + one maximal frame.
    const val MAX_FRAME_BYTES: Long = 64L * 1024 * 1024

    fun frameRegular(payload: ByteArray): ByteArray {
        require(payload.size.toLong() <= MAX_FRAME_BYTES) {
            "payload exceeds MAX_FRAME_BYTES: ${payload.size}"
        }
        val out = ByteArray(HEADER_SIZE + payload.size)
        out[0] = TYPE_REGULAR.toByte()
        writeUInt32BE(out, 1, 0L)
        writeUInt32BE(out, 5, 0L)
        writeUInt32BE(out, 9, payload.size.toLong())
        System.arraycopy(payload, 0, out, HEADER_SIZE, payload.size)
        return out
    }
}

// Deterministic protocol failure for oversized or over-buffered frames; the
// RPC read loop routes it through transportDown (clean disconnect + reconnect).
class FrameTooLargeException(val frameLength: Long) :
    IllegalStateException(
        "RPC frame length $frameLength exceeds the ${SocketFraming.MAX_FRAME_BYTES} byte protocol ceiling",
    )

// Reassembles fragmented stream bytes into framed messages (port of ChunkStream): a frame is consumed only once fully arrived.
class ChunkStream {
    private val out = ByteArrayOutputStream()
    private var totalLength = 0

    val byteLength: Int get() = totalLength

    fun acceptChunk(chunk: ByteArray) {
        // Bound the pending buffer before writing: without a declared complete
        // frame, buffered bytes may never exceed header + one maximal frame.
        if (totalLength + chunk.size > SocketFraming.HEADER_SIZE + SocketFraming.MAX_FRAME_BYTES) {
            throw FrameTooLargeException(totalLength + chunk.size.toLong())
        }
        out.write(chunk)
        totalLength += chunk.size
    }

    fun hasCompleteFrame(): Boolean {
        if (totalLength < SocketFraming.HEADER_SIZE) return false
        val header = peek(SocketFraming.HEADER_SIZE) ?: return false
        val length = readUInt32BE(header, 9) // unsigned magnitude stays Long
        if (length > SocketFraming.MAX_FRAME_BYTES) {
            throw FrameTooLargeException(length)
        }
        return totalLength >= SocketFraming.HEADER_SIZE + length.toInt()
    }

    fun peekHeader(): IntArray? {
        val header = peek(SocketFraming.HEADER_SIZE) ?: return null
        val length = readUInt32BE(header, 9)
        if (length > SocketFraming.MAX_FRAME_BYTES) {
            throw FrameTooLargeException(length)
        }
        return intArrayOf(
            header[0].toInt() and 0xFF,
            readUInt32BE(header, 1).toInt(),
            readUInt32BE(header, 5).toInt(),
            length.toInt(), // safe: bounded by MAX_FRAME_BYTES (< Int.MAX_VALUE)
        )
    }

    fun skip(count: Int) {
        read(count) ?: throw IllegalStateException("ChunkStream.skip($count) beyond readable range")
    }

    fun read(count: Int): ByteArray? {
        if (count < 0) {
            throw IllegalStateException("ChunkStream.read($count): negative length")
        }
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

// Long keeps the unsigned magnitude.
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
