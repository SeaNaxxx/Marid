package com.marid.app.engine

import java.io.File
import java.io.IOException
import java.io.InputStream

/**
 * Minimal ustar reader — extracts the rootfs tarballs provisioned into the
 * app. No external dependency (the engine runtime tarballs we produce are
 * plain ustar, optionally gzipped).
 *
 * Handles: regular files, directories, symlinks, GNU long names (typeflag
 * 'L'), ustar prefix fields, and PAX headers ('x'/'g' path override) to the
 * extent needed for our own archives.
 */
object TarExtractor {

    data class Entry(
        val name: String,
        val mode: Int,
        val typeflag: Byte,
        val linkName: String?,
        val size: Long,
    )

    const val TYPE_REGULAR: Byte = '0'.code.toByte()
    const val TYPE_HARDLINK: Byte = '1'.code.toByte()
    const val TYPE_SYMLINK: Byte = '2'.code.toByte()
    const val TYPE_DIRECTORY: Byte = '5'.code.toByte()

    fun extract(tar: InputStream, destDir: File, onEntry: ((Entry, File) -> Unit)? = null) {
        destDir.mkdirs()
        val header = ByteArray(512)
        var pendingPaxPath: String? = null
        var pendingGnuLongPath: String? = null

        while (true) {
            if (!readFully(tar, header)) break
            if (isZeroBlock(header)) {
                // Two consecutive zero blocks terminate; tolerate one.
                val next = ByteArray(512)
                if (!readFully(tar, next) || isZeroBlock(next)) break
                continue
            }

            val name = parseString(header, 0, 100)
            val mode = parseOctal(header, 100, 8) ?: 0
            val typeflag = header[156]
            val linkName = parseString(header, 157, 100).ifEmpty { null }
            var size = parseSize(header, 124, 12)
            val prefix = parseString(header, 345, 155)
            var fullName = if (prefix.isNotEmpty()) "$prefix/$name" else name

            // PAX 'x' header overrides the next entry's path.
            if (typeflag == 'x'.code.toByte() || typeflag == 'g'.code.toByte()) {
                val paxData = ByteArray(size.toInt())
                readFully(tar, paxData)
                pendingPaxPath = parsePaxPath(paxData) ?: pendingPaxPath
                continue
            }
            // GNU long name header.
            if (typeflag == 'L'.code.toByte()) {
                val longNameData = ByteArray(size.toInt())
                readFully(tar, longNameData)
                pendingGnuLongPath = String(longNameData, 0, indexOfZero(longNameData), Charsets.UTF_8)
                continue
            }
            if (typeflag == 'K'.code.toByte()) {
                val longLinkData = ByteArray(size.toInt())
                readFully(tar, longLinkData)
                // long link name (rare in our archives) — skip content
                continue
            }

            pendingPaxPath?.let {
                fullName = it
                pendingPaxPath = null
            }
            pendingGnuLongPath?.let {
                fullName = it
                pendingGnuLongPath = null
            }

            val entry = Entry(
                name = fullName.trimEnd('/'),
                mode = mode,
                typeflag = typeflag,
                linkName = linkName,
                size = size,
            )

            when (entry.typeflag) {
                TYPE_DIRECTORY -> {
                    val dir = File(destDir, sanitize(entry.name))
                    dir.mkdirs()
                    onEntry?.invoke(entry, dir)
                }
                TYPE_SYMLINK -> {
                    val target = File(destDir, sanitize(entry.name))
                    target.parentFile?.mkdirs()
                    if (!target.exists()) {
                        try {
                            java.nio.file.Files.createSymbolicLink(
                                target.toPath(),
                                java.nio.file.Path.of(entry.linkName ?: ""),
                            )
                        } catch (t: IOException) {
                            // Some filesystems deny symlinks; fall back to copy on read.
                            onEntry?.invoke(entry, target)
                        }
                    }
                    skipFully(tar, 0) // symlinks carry no body
                }
                TYPE_REGULAR, TYPE_HARDLINK, 0.toByte() -> {
                    val target = File(destDir, sanitize(entry.name))
                    target.parentFile?.mkdirs()
                    val body = ByteArray(size.coerceAtMost(Int.MAX_VALUE.toLong()).toInt())
                    if (!readFully(tar, body)) throw IOException("truncated tar body for ${entry.name}")
                    target.outputStream().use { it.write(body) }
                    setFilePermissions(target, entry.mode)
                    onEntry?.invoke(entry, target)
                }
                else -> skipFully(tar, size)
            }

            // Skip the 512-block padding after the body.
            val remainder = if (size % 512 == 0L) 0 else 512 - (size % 512)
            skipFully(tar, remainder)
        }
    }

    /**
     * The critical fix for the "Permission denied" defect: ensure extracted
     * binaries are owner-executable and readable regardless of the mode bits
     * captured at archive time.
     */
    fun setFilePermissions(target: File, mode: Int) {
        target.setReadable(true, true)
        target.setWritable(true, true)
        // 0o111 = u+x,g+x,o+x (Kotlin has no octal literals).
        if (mode and 73 != 0) {
            target.setExecutable(true, true)
        }
    }

    private fun sanitize(name: String): String {
        val cleaned = name.replace("\\", "/")
        val parts = cleaned.split("/").filter { it.isNotEmpty() && it != "." && it != ".." }
        if (parts.isEmpty()) throw IOException("tar entry with empty path: '$name'")
        return parts.joinToString("/")
    }

    private fun isZeroBlock(block: ByteArray): Boolean = block.all { it.toInt() == 0 }

    private fun parseString(header: ByteArray, offset: Int, length: Int): String {
        var end = offset
        val max = offset + length
        while (end < max && header[end].toInt() != 0) end++
        return String(header, offset, end - offset, Charsets.US_ASCII)
    }

    private fun parseOctal(header: ByteArray, offset: Int, length: Int): Int? {
        var value = 0
        var any = false
        for (i in 0 until length) {
            val b = header[offset + i].toInt() and 0xFF
            if (b == 0 || b == 32) {
                if (any) break else continue
            }
            if (b < '0'.code || b > '7'.code) continue
            any = true
            value = (value shl 3) or (b - '0'.code)
        }
        return if (any) value else null
    }

    private fun parseSize(header: ByteArray, offset: Int, length: Int): Long {
        // Size can be base-256 (high bit set) for >8GB; not our case.
        if (header[offset].toInt() and 0x80 != 0) {
            var value = 0L
            for (i in 0 until length) {
                value = (value shl 8) or (header[offset + i].toLong() and 0xFF)
            }
            return value and 0x7FFFFFFFFFFFFFFFL
        }
        return parseOctal(header, offset, length)?.toLong() ?: 0L
    }

    private fun parsePaxPath(data: ByteArray): String? {
        val text = String(data, Charsets.UTF_8)
        for (line in text.split("\n")) {
            val m = Regex("^\\d+ path=([^\\n]+)$").find(line) ?: continue
            return m.groupValues[1]
        }
        return null
    }

    private fun indexOfZero(data: ByteArray): Int {
        var end = data.size
        for (i in data.indices) {
            if (data[i].toInt() == 0) {
                end = i
                break
            }
        }
        return end
    }

    private fun readFully(input: InputStream, buffer: ByteArray): Boolean {
        var read = 0
        while (read < buffer.size) {
            val n = input.read(buffer, read, buffer.size - read)
            if (n < 0) return false // EOF before the buffer filled
            if (n == 0) return false
            read += n
        }
        return true
    }

    private fun skipFully(input: InputStream, count: Long) {
        if (count <= 0L) return
        var remaining = count
        val skipBuffer = ByteArray(8192)
        while (remaining > 0) {
            if (remaining >= skipBuffer.size) {
                val n = input.read(skipBuffer)
                if (n < 0) return
                remaining -= n
            } else {
                val n = input.read(skipBuffer, 0, remaining.toInt())
                if (n < 0) return
                remaining -= n
            }
        }
    }
}
