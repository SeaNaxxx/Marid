package com.qodex.app.engine

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
            fixture().use { raw ->
                GZIPInputStream(raw).use { gz ->
                    TarExtractor.extract(gz, dest)
                }
            }
            val node = File(dest, "bin/node")
            val lib = File(dest, "usr/lib/libtest.so")
            assertTrue("bin/node extracted", node.isFile)
            assertTrue("bin/node readable", node.canRead())
            assertTrue("bin/node executable (mode 0755 preserved)", node.canExecute())
            assertTrue("usr/lib/libtest.so extracted", lib.isFile)
            assertTrue("lib readable", lib.canRead())
            assertFalse("data file stays non-executable (mode 0644)", lib.canExecute())
        } finally {
            dest.deleteRecursively()
        }
    }

    @Test
    fun `setFilePermissions forces owner rx on any archived mode`() {
        // Fresh file per mode case: setFilePermissions only GRANTS bits (chmod +x
        // enforcement) — it never revokes, by design.
        val execFile = File.createTempFile("qodex-perm-x", ".bin")
        val dataFile = File.createTempFile("qodex-perm-d", ".bin")
        try {
            execFile.setReadable(false, true)
            execFile.setExecutable(false, true)
            TarExtractor.setFilePermissions(execFile, 0b111_101_101)
            assertTrue(execFile.canRead())
            assertTrue(execFile.canExecute())

            dataFile.setExecutable(true, true) // pre-grant to prove no revoke happens
            TarExtractor.setFilePermissions(dataFile, 0b110_100_100)
            assertTrue(dataFile.canRead())
            assertTrue(dataFile.canExecute()) // still executable — grant-only semantics
        } finally {
            execFile.delete()
            dataFile.delete()
        }
    }

    @Test
    fun `entry walker reports each file`() {
        val dest = File(System.getProperty("java.io.tmpdir"), "qodex-tar-walk-${System.nanoTime()}")
        try {
            val seen = mutableListOf<String>()
            fixture().use { raw ->
                GZIPInputStream(raw).use { gz ->
                    TarExtractor.extract(gz, dest) { entry, _ -> seen.add(entry.name) }
                }
            }
            assertTrue(seen.contains("bin"))
            assertTrue(seen.contains("bin/node"))
            assertTrue(seen.contains("bin/qodex-exec"))
            assertTrue(seen.contains("usr/lib/libtest.so"))
            assertEquals(6, seen.size) // bin/ bin/node bin/qodex-exec usr/ usr/lib/ usr/lib/libtest.so
        } finally {
            dest.deleteRecursively()
        }
    }
}
