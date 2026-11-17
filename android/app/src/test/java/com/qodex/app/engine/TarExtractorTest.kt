package com.qodex.app.engine

import com.qodex.app.runtime.TarExtractor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.zip.GZIPInputStream

/**
 * TarExtractor tests — proves the STEP 4 permission fix: binaries extracted
 * from the provisioned rootfs come out readable/executable (the exact defect
 * chain behind the old "Permission denied" screenshots).
 */
class TarExtractorTest {

    private fun fixture(): java.io.InputStream =
        javaClass.getResourceAsStream("/tar-fixture.tar.gz")
            ?: error("tar-fixture.tar.gz missing from test resources")

    @Test
    fun `extracts ustar gz with executable permissions preserved`() {
        val dest = File.createTempFile("qodex-tar-test", "").let {
            it.delete()
            it.parentFile.resolve("qodex-tar-${System.nanoTime()}")
        }
        try {
            val report = TarExtractor.ExtractReport()
            fixture().use { raw ->
                GZIPInputStream(raw).use { gz ->
                    TarExtractor(dest, report).extract(gz)
                }
            }
            val node = File(dest, "bin/node")
            val exec = File(dest, "bin/qodex-exec")
            val lib = File(dest, "usr/lib/libtest.so")
            assertTrue("bin/node extracted", node.isFile)
            assertTrue("bin/node readable", node.canRead())
            assertTrue("bin/node executable (mode 0755 preserved)", node.canExecute())
            assertTrue("bin/qodex-exec extracted", exec.isFile)
            assertTrue("bin/qodex-exec executable", exec.canExecute())
            assertTrue("usr/lib/libtest.so extracted", lib.isFile)
            assertTrue("lib readable", lib.canRead())
            assertFalse("data file stays non-executable (mode 0644)", lib.canExecute())
            assertEquals("regular files extracted", 3, report.regularFiles)
            assertEquals("directories created", 3, report.directories)
            assertEquals("no entries skipped", 0, report.skippedEntries)
        } finally {
            dest.deleteRecursively()
        }
    }
}
