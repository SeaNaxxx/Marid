package com.marid.app.runtime

import com.github.luben.zstd.ZstdInputStream
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.ar.ArArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream

// A .deb is an ar archive: debian-binary + control.tar.* (skipped) + data.tar.* payload, re-rooted by TarExtractor.
// ar member names carry no leading slash; the data member is always last, so the ar stream itself owns the lifecycle.
object DebExtractor {

    fun extract(deb: File, dest: File, report: TarExtractor.ExtractReport) {
        ArArchiveInputStream(deb.inputStream().buffered(1 shl 16)).use { ar ->
            var sawData = false
            while (true) {
                val entry: ArchiveEntry = ar.nextEntry ?: break
                val name = entry.name.trim().trimStart('.', '/')
                if (!name.startsWith("data.tar")) continue
                sawData = true
                val decompressed: InputStream = when {
                    name.startsWith("data.tar.zst") ->
                        ZstdInputStream(ar.buffered(1 shl 16))
                    name.startsWith("data.tar.xz") ->
                        XZCompressorInputStream(ar.buffered(1 shl 16))
                    name.startsWith("data.tar.gz") ->
                        GzipCompressorInputStream(ar.buffered(1 shl 16))
                    else -> throw IOException(
                        "unsupported deb payload compression: $name (expected data.tar.zst|data.tar.xz|data.tar.gz)",
                    )
                }
                // TarExtractor closes the stream it is given; shield the ar stream so the loop survives.
                TarExtractor(dest, report).extract(NonClosing(decompressed))
            }
            if (!sawData) {
                throw IOException("deb has no data.tar member: ${deb.name}")
            }
        }
    }

    private class NonClosing(private val inner: InputStream) : InputStream() {
        override fun read(): Int = inner.read()
        override fun read(b: ByteArray, off: Int, len: Int): Int = inner.read(b, off, len)
        override fun skip(n: Long): Long = inner.skip(n)
        override fun available(): Int = inner.available()
        override fun close() = Unit
    }
}
