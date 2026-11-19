package com.qodex.app.runtime

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.GZIPInputStream

/**
 * P0 extraction contract — the committed defect shipped a nested dist/ (and a
 * bundle built before entry-stdio.js existed), so the provisioner unpacked to
 * runtime/dist/dist/entry-stdio.js while distEntry() looked at
 * runtime/dist/entry-stdio.js: the engine never booted once. These tests pin
 * the flat archive root (entry-stdio.js beside node_modules/) and prove the
 * pre-extraction assertion rejects the defective layouts with real entries.
 */
class DistArchiveContractTest {

    private fun tgzOf(vararg paths: String): File {
        val out = File.createTempFile("qodex-contract", ".tgz")
        TarArchiveOutputStream(
            GzipCompressorOutputStream(BufferedOutputStream(FileOutputStream(out))),
        ).use { tar ->
            for (path in paths) {
                val isDir = path.endsWith("/")
                val data = "$path\n".toByteArray()
                val entry = TarArchiveEntry(path)
                if (!isDir) entry.size = data.size.toLong()
                tar.putArchiveEntry(entry)
                if (!isDir) tar.write(data)
                tar.closeArchiveEntry()
            }
            tar.finish()
        }
        return out
    }

    private fun withTgz(vararg paths: String, block: (File) -> Unit) {
        val tgz = tgzOf(*paths)
        try {
            block(tgz)
        } finally {
            tgz.delete()
        }
    }

    @Test
    fun `flat archive root passes the contract`() {
        withTgz(
            "package.json", "entry-stdio.js", "qodex.cjs", "entry-http.js",
            "remote/", "remote/qodex-server.cjs",
            "node_modules/", "node_modules/package.json",
        ) { tgz ->
            DistArchiveContract.requireValid(tgz) // must not throw
        }
    }

    @Test
    fun `archive without the agent CLI bundle is rejected`() {
        withTgz(
            "package.json", "entry-stdio.js",
            "node_modules/", "node_modules/a/index.js",
        ) { tgz ->
            try {
                DistArchiveContract.requireValid(tgz)
                throw AssertionError("archive without qodex.cjs must be rejected")
            } catch (e: IOException) {
                assertTrue(
                    "message names the missing agent CLI bundle: ${e.message}",
                    e.message!!.contains("qodex.cjs missing at archive root"),
                )
            }
        }
    }

    @Test
    fun `nested dist root is rejected with the archive's real entries`() {
        withTgz(
            "package.json", "dist/", "dist/entry-stdio.js",
            "dist/remote/", "dist/remote/qodex-server.cjs",
            "node_modules/", "node_modules/a/index.js",
        ) { tgz ->
            try {
                DistArchiveContract.requireValid(tgz)
                throw AssertionError("nested dist/ archive must be rejected")
            } catch (e: IOException) {
                assertTrue(
                    "message names the missing entry: ${e.message}",
                    e.message!!.contains("entry-stdio.js missing at archive root"),
                )
                assertTrue(
                    "message lists first 10 entries: ${e.message}",
                    e.message!!.contains("first entries: [package.json, dist, dist/entry-stdio.js"),
                )
            }
        }
    }

    @Test
    fun `archive without node_modules is rejected`() {
        withTgz("package.json", "entry-stdio.js") { tgz ->
            try {
                DistArchiveContract.requireValid(tgz)
                throw AssertionError("archive without node_modules must be rejected")
            } catch (e: IOException) {
                assertTrue(
                    "message names the missing node_modules: ${e.message}",
                    e.message!!.contains("node_modules/ missing at archive root"),
                )
            }
        }
    }

    @Test
    fun `flat archive extracts the engine entry to the destination root`() {
        withTgz(
            "package.json", "entry-stdio.js",
            "remote/", "remote/qodex-server.cjs",
            "node_modules/", "node_modules/package.json",
        ) { tgz ->
            val dest = File.createTempFile("qodex-flat", "").let {
                it.delete()
                it.parentFile.resolve("qodex-flat-${System.nanoTime()}")
            }
            try {
                GZIPInputStream(tgz.inputStream()).use { gz ->
                    TarExtractor(dest).extract(gz)
                }
                assertTrue(
                    "entry-stdio.js lands at destination root (distEntry contract)",
                    File(dest, DistArchiveContract.ENTRY_STDIO).isFile,
                )
                assertTrue(
                    "remote payload lands under destination root",
                    File(dest, "remote/qodex-server.cjs").isFile,
                )
            } finally {
                dest.deleteRecursively()
            }
        }
    }

    @Test
    fun `normalize strips tar prefixes and trailing slashes`() {
        assertTrue(DistArchiveContract.normalize("./entry-stdio.js") == "entry-stdio.js")
        assertTrue(DistArchiveContract.normalize("node_modules/") == "node_modules")
        assertTrue(DistArchiveContract.normalize("entry-stdio.js") == "entry-stdio.js")
        assertFalse(
            DistArchiveContract.normalize("./dist/entry-stdio.js") ==
                DistArchiveContract.ENTRY_STDIO,
        )
    }
}
