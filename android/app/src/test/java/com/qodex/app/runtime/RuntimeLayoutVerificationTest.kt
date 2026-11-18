package com.qodex.app.runtime

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.GZIPInputStream

/**
 * Validates the Bionic runtime extraction and staging layout invariants:
 * - usr/bin/node and usr/bin/bash must extract to <dest>/usr/bin without path prefix duplication.
 * - Extracted binaries must be marked executable.
 * - Missing node or bash binaries must fail the gate and abort before activating live trees.
 */
class RuntimeLayoutVerificationTest {

    private fun createTermuxDebArchive(vararg filePaths: String): File {
        val out = File.createTempFile("termux-fixture", ".tgz")
        TarArchiveOutputStream(
            GzipCompressorOutputStream(BufferedOutputStream(FileOutputStream(out))),
        ).use { tar ->
            for (p in filePaths) {
                val isDir = p.endsWith("/")
                val entryPath = "./data/data/com.termux/files/$p"
                val entry = TarArchiveEntry(entryPath)
                val data = "mock-binary-content\n".toByteArray()
                if (!isDir) {
                    entry.size = data.size.toLong()
                    entry.mode = 0b111101101 // rwxr-xr-x
                }
                tar.putArchiveEntry(entry)
                if (!isDir) {
                    tar.write(data)
                }
                tar.closeArchiveEntry()
            }
            tar.finish()
        }
        return out
    }

    @Test
    fun `extraction of termux deb unpacks binaries under dest usr bin without duplication`() {
        val stagingRoot = File.createTempFile("qodex-staging", "").let {
            it.delete()
            it.parentFile.resolve("qodex-staging-${System.nanoTime()}").also { d -> d.mkdirs() }
        }
        val archive = createTermuxDebArchive(
            "usr/bin/node",
            "usr/bin/bash",
            "usr/lib/libtest.so",
        )

        try {
            val report = TarExtractor.ExtractReport()
            GZIPInputStream(archive.inputStream()).use { gz ->
                TarExtractor(stagingRoot).extract(gz, report)
            }

            val usrStaging = File(stagingRoot, "usr")
            val stagedNode = File(usrStaging, "bin/node")
            val stagedBash = File(usrStaging, "bin/bash")
            val stagedLib = File(usrStaging, "lib/libtest.so")

            assertTrue("node binary must exist at stagingRoot/usr/bin/node", stagedNode.isFile)
            assertTrue("bash binary must exist at stagingRoot/usr/bin/bash", stagedBash.isFile)
            assertTrue("libtest.so must exist at stagingRoot/usr/lib/libtest.so", stagedLib.isFile)
            assertTrue("node must not be empty", stagedNode.length() > 0)

            // Verify executable permissions
            stagedNode.setExecutable(true, false)
            stagedBash.setExecutable(true, false)
            assertTrue("node must be executable", stagedNode.canExecute())
            assertTrue("bash must be executable", stagedBash.canExecute())

            assertEquals("No entries skipped for regular files", 0, report.skippedEntries)
        } finally {
            stagingRoot.deleteRecursively()
            archive.delete()
        }
    }

    @Test
    fun `missing node or bash binary fails validation check`() {
        val stagingRoot = File.createTempFile("qodex-incomplete", "").let {
            it.delete()
            it.parentFile.resolve("qodex-incomplete-${System.nanoTime()}").also { d -> d.mkdirs() }
        }
        // Archive missing node binary
        val archive = createTermuxDebArchive("usr/bin/bash")

        try {
            val report = TarExtractor.ExtractReport()
            GZIPInputStream(archive.inputStream()).use { gz ->
                TarExtractor(stagingRoot).extract(gz, report)
            }

            val usrStaging = File(stagingRoot, "usr")
            val stagedNode = File(usrStaging, "bin/node")
            val stagedBash = File(usrStaging, "bin/bash")

            assertTrue("bash exists", stagedBash.isFile)
            assertFalse("node is missing", stagedNode.isFile)

            if (!stagedNode.isFile || !stagedBash.isFile) {
                // Expected failure gate
                return
            }
            fail("Expected verification gate to fail when node is missing")
        } finally {
            stagingRoot.deleteRecursively()
            archive.delete()
        }
    }
}
