package com.marid.app.runtime

import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.ar.ArArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.compress.compressors.xz.XZCompressorInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream

/**
 * Extracts a Termux .deb (Debian binary package) into the runtime root.
 *
 * A .deb is an `ar` archive whose members are:
 *   debian-binary   — version marker (skipped)
 *   control.tar.*   — package metadata (skipped)
 *   data.tar.xz|gz  — the payload; entries are re-rooted by [TarExtractor]
 *
 * Termux payloads are data.tar.xz (dpkg default); zst is refused loudly
 * rather than silently skipped.
 */
object DebExtractor {

    fun extract(deb: File, dest: File, report: TarExtractor.ExtractReport) {
        ArArchiveInputStream(deb.inputStream().buffered(1 shl 16)).use { ar ->
            var sawData = false
            while (true) {
                val entry: ArchiveEntry = ar.nextEntry ?: break
                val name = entry.name.trimStart('.')
                if (!name.startsWith("/data.tar")) continue
                sawData = true
                val decompressed: InputStream = when {
                    name.startsWith("/data.tar.xz") ->
                        XZCompressorInputStream(ar.buffered(1 shl 16))
                    name.startsWith("/data.tar.gz") ->
                        GzipCompressorInputStream(ar.buffered(1 shl 16))
                    else -> throw IOException(
                        "unsupported deb payload compression: $name " +
                            "(commons-compress zstd needs zstd-jni; repackage the deb)",
                    )
                }
                TarExtractor(dest, report).extract(decompressed)
                decompressed.close()
            }
            if (!sawData) {
                throw IOException("deb has no data.tar member: ${deb.name}")
            }
        }
    }
}
