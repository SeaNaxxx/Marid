package com.qodex.app.rpc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Audit M-02 regression: RPC framing must reject oversized / hostile length
 * headers deterministically BEFORE allocation instead of buffering unbounded
 * or desynchronizing via Int overflow of the unsigned 32-bit length field.
 */
class FrameLimitTest {

    private fun header(type: Int, length: Long): ByteArray {
        val h = ByteArray(SocketFraming.HEADER_SIZE)
        h[0] = type.toByte()
        writeUInt32BE(h, 1, 0L)
        writeUInt32BE(h, 5, 0L)
        writeUInt32BE(h, 9, length)
        return h
    }

    @Test
    fun `normal frames still parse end to end`() {
        val stream = ChunkStream()
        val payload = "hello frame".toByteArray()
        stream.acceptChunk(SocketFraming.frameRegular(payload))
        assertTrue(stream.hasCompleteFrame())
        val header = stream.peekHeader()!!
        assertEquals(SocketFraming.TYPE_REGULAR, header[0])
        assertEquals(payload.size, header[3])
        stream.skip(SocketFraming.HEADER_SIZE)
        assertEquals(payload.contentToString(), stream.read(payload.size)!!.contentToString())
        assertFalse(stream.hasCompleteFrame())
    }

    @Test
    fun `oversized declared frame length is rejected`() {
        val stream = ChunkStream()
        // 0xFFFFFFFF = 4 GiB — Int conversion would be negative (desync) and
        // buffering it would exhaust memory; must throw at header inspection.
        stream.acceptChunk(header(SocketFraming.TYPE_REGULAR, 0xFFFF_FFFFL))
        assertThrows(FrameTooLargeException::class.java) { stream.hasCompleteFrame() }
        assertThrows(FrameTooLargeException::class.java) { stream.peekHeader() }
    }

    @Test
    fun `declared frame just above the ceiling is rejected`() {
        val stream = ChunkStream()
        stream.acceptChunk(header(SocketFraming.TYPE_REGULAR, SocketFraming.MAX_FRAME_BYTES + 1))
        assertThrows(FrameTooLargeException::class.java) { stream.hasCompleteFrame() }
    }

    @Test
    fun `buffered bytes beyond one maximal frame are rejected before allocation`() {
        val stream = ChunkStream()
        val chunk = ByteArray(1024 * 1024)
        val ceiling = SocketFraming.HEADER_SIZE + SocketFraming.MAX_FRAME_BYTES
        // Feed the largest whole-chunk multiple that still fits under the
        // ceiling; the next chunk crosses it and must be rejected in
        // acceptChunk BEFORE the bytes are buffered.
        var fed = 0L
        while (fed + chunk.size <= ceiling) {
            stream.acceptChunk(chunk)
            fed += chunk.size
        }
        assertThrows(FrameTooLargeException::class.java) {
            stream.acceptChunk(chunk)
        }
    }

    @Test
    fun `a frame exactly at the ceiling still completes`() {
        val stream = ChunkStream()
        val body = ByteArray(SocketFraming.MAX_FRAME_BYTES.toInt() - 1)
        val frame = SocketFraming.frameRegular(body)
        // Feed in small chunks to exercise the bounded-buffer path.
        var offset = 0
        val step = 1024 * 1024
        while (offset < frame.size) {
            val end = minOf(offset + step, frame.size)
            stream.acceptChunk(frame.copyOfRange(offset, end))
            offset = end
        }
        assertTrue(stream.hasCompleteFrame())
        stream.skip(SocketFraming.HEADER_SIZE)
        assertEquals(body.size, stream.read(body.size)!!.size)
    }

    @Test
    fun `negative read length is rejected instead of corrupting the stream`() {
        val stream = ChunkStream()
        stream.acceptChunk(header(SocketFraming.TYPE_REGULAR, 10L))
        assertThrows(IllegalStateException::class.java) { stream.read(-1) }
    }

    @Test
    fun `frameRegular rejects oversized outbound payloads`() {
        assertThrows(IllegalArgumentException::class.java) {
            SocketFraming.frameRegular(ByteArray(SocketFraming.MAX_FRAME_BYTES.toInt() + 1))
        }
    }
}
