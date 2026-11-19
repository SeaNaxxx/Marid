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
 * - The consolidated qodex-usr archive (plain usr/ paths) extracts node/bash to
 *   <stagingRoot>/usr/bin and TarExtractor preserves the tar exec bits.
 * - The legacy Termux re-rooting (./data/data/com.termux/files/ prefix) still
 *   lands binaries at <dest>/usr/bin for archives built that way.
 * - A missing node or bash binary must fail Provisioner.requireUsrBinaries —
 *   the same gate provisionTransactional runs before activating live trees
 *   (the previous version of this test exited early and could never fail).
 */
class RuntimeLayoutVerificationTest {

    /** Tar entries under an arbitrary root prefix; binaries 0755, data files 0644. */
    private fun tarGzOf(paths: List<String>, prefix: String = ""): File {
        val out = File.createTempFile("runtime-fixture", ".tgz")
        TarArchiveOutputStream(
            GzipCompressorOutputStream(BufferedOutputStream(FileOutputStream(out))),
        ).use { tar ->
            for (p in paths) {
                val isDir = p.endsWith("/")
                val entryPath = if (prefix.isEmpty()) p else "./data/data/com.termux/files/$p"
                val entry = TarArchiveEntry(entryPath)
                val data = "mock-binary-content\n".toByteArray()
                if (!isDir) {
                    entry.size = data.size.toLong()
                    entry.mode = if (p.startsWith("usr/bin/")) 0b111101101 else 0b110100100
                }
                tar.putArchiveEntry(entry)
                if (!isDir) tar.write(data)
                tar.closeArchiveEntry()
            }
            tar.finish()
        }
        return out
    }

    private fun tempDir(name: String): File =
        File.createTempFile(name, "").let {
            it.delete()
            it.parentFile.resolve("$name-${System.nanoTime()}").also { d -> d.mkdirs() }
        }

    private fun extractTo(archive: File, dest: File): TarExtractor.ExtractReport {
        val report = TarExtractor.ExtractReport()
        GZIPInputStream(archive.inputStream()).use { gz ->
            TarExtractor(dest, report).extract(gz)
        }
        return report
    }

    @Test
    fun `consolidated usr archive extracts binaries under staging usr bin`() {
        val stagingRoot = tempDir("qodex-staging")
        // This is the layout scripts/qodex-android/pack-runtime-usr.mjs produces:
        // plain usr/ paths at the archive root, one streaming pass.
        val archive = tarGzOf(
            listOf("usr/", "usr/bin/", "usr/bin/node", "usr/bin/bash", "usr/lib/", "usr/lib/libtest.so"),
        )
        try {
            val report = extractTo(archive, stagingRoot)
            val usrStaging = File(stagingRoot, "usr")
            val stagedNode = File(usrStaging, "bin/node")
            val stagedBash = File(usrStaging, "bin/bash")
            val stagedLib = File(usrStaging, "lib/libtest.so")

            assertTrue("node binary must exist at stagingRoot/usr/bin/node", stagedNode.isFile)
            assertTrue("bash binary must exist at stagingRoot/usr/bin/bash", stagedBash.isFile)
            assertTrue("libtest.so must exist at stagingRoot/usr/lib/libtest.so", stagedLib.isFile)
            assertTrue("node must not be empty", stagedNode.length() > 0)
            // Tar entry mode 0755 must survive extraction — no chmod needed.
            assertTrue(
                "tar exec bits must be preserved by extraction (node)",
                stagedNode.canExecute(),
            )
            assertTrue(
                "tar exec bits must be preserved by extraction (bash)",
                stagedBash.canExecute(),
            )
            assertFalse(
                "data files must stay non-executable",
                stagedLib.canExecute(),
            )
            assertEquals("No entries skipped for regular files", 0, report.skippedEntries)

            // The real provisioning gate must pass on this layout.
            Provisioner.requireUsrBinaries(usrStaging)
        } finally {
            stagingRoot.deleteRecursively()
            archive.delete()
        }
    }

    @Test
    fun `legacy termux re-rooted archive still lands binaries under usr bin`() {
        val stagingRoot = tempDir("qodex-staging-legacy")
        val archive = tarGzOf(
            listOf("usr/bin/node", "usr/bin/bash", "usr/lib/libtest.so"),
            prefix = "termux",
        )
        try {
            extractTo(archive, stagingRoot)
            assertTrue(File(stagingRoot, "usr/bin/node").isFile)
            assertTrue(File(stagingRoot, "usr/bin/bash").isFile)
            Provisioner.requireUsrBinaries(File(stagingRoot, "usr"))
        } finally {
            stagingRoot.deleteRecursively()
            archive.delete()
        }
    }

    @Test
    fun `missing node binary fails the usr binaries gate`() {
        val stagingRoot = tempDir("qodex-incomplete")
        // Archive missing usr/bin/node on purpose.
        val archive = tarGzOf(listOf("usr/", "usr/bin/", "usr/bin/bash"))
        try {
            extractTo(archive, stagingRoot)
            val usrStaging = File(stagingRoot, "usr")
            assertTrue("bash extracted", File(usrStaging, "bin/bash").isFile)
            assertFalse("node is missing", File(usrStaging, "bin/node").isFile)
            try {
                Provisioner.requireUsrBinaries(usrStaging)
                fail("Expected requireUsrBinaries to fail when usr/bin/node is missing")
            } catch (e: IOException) {
                assertTrue(
                    "gate message must name the missing binary: ${e.message}",
                    e.message!!.contains("bionic runtime incomplete"),
                )
            }
        } finally {
            stagingRoot.deleteRecursively()
            archive.delete()
        }
    }

    @Test
    fun `missing bash binary fails the usr binaries gate`() {
        val stagingRoot = tempDir("qodex-incomplete-bash")
        val archive = tarGzOf(listOf("usr/", "usr/bin/", "usr/bin/node"))
        try {
            extractTo(archive, stagingRoot)
            try {
                Provisioner.requireUsrBinaries(File(stagingRoot, "usr"))
                fail("Expected requireUsrBinaries to fail when usr/bin/bash is missing")
            } catch (e: IOException) {
                assertTrue(
                    "gate message must name the missing binary: ${e.message}",
                    e.message!!.contains("bionic runtime incomplete"),
                )
            }
        } finally {
            stagingRoot.deleteRecursively()
            archive.delete()
        }
    }
}
