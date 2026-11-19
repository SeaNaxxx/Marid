package com.qodex.app.runtime

import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream

/**
 * Extraction contract for qodex-dist.tgz: the archive root must carry the
 * built engine entries directly — entry-stdio.js beside node_modules/ and the
 * bundled agent CLI qodex.cjs — so TarExtractor lands them at
 * filesDir/runtime/dist/ and distEntry() finds runtime/dist/entry-stdio.js
 * while BionicEnv wires the agent child spawn to runtime/dist/qodex.cjs.
 * A nested dist/ (or a stale bundle built before the stdio entry existed)
 * would unpack to runtime/dist/dist/entry-stdio.js and the engine would never
 * boot; without qodex.cjs the engine boots but every agent spawn fails with
 * "Qodex agent server command is not configured". The contract is asserted
 * BEFORE extraction with the archive's real entries in the failure message
 * instead of a post-extraction guess.
 */
object DistArchiveContract {

    const val ENTRY_STDIO = "entry-stdio.js"
    const val NODE_MODULES = "node_modules"
    const val AGENT_CLI = "qodex.cjs"

    /** Root-relative name: strips the tar ./ prefix and trailing slash. */
    fun normalize(name: String): String = name.removePrefix("./").trimEnd('/')

    /**
     * Asserts the archive root layout before extraction. Throws IOException
     * naming what is missing plus the first 10 actual entries.
     */
    fun requireValid(tgz: File) {
        val names = ArrayList<String>(256)
        GzipCompressorInputStream(tgz.inputStream().buffered(1 shl 16)).use { gz ->
            TarArchiveInputStream(gz).use { tis ->
                while (true) {
                    val entry = tis.nextTarEntry ?: break
                    names.add(entry.name)
                }
            }
        }
        requireValid(names)
    }

    /** Pure-list overload — injectable and unit-testable without Android. */
    fun requireValid(names: List<String>) {
        val normalized = names.map(::normalize)
        val hasEntry = normalized.any { it == ENTRY_STDIO }
        val hasNodeModules = normalized.any {
            it == NODE_MODULES || it.startsWith("$NODE_MODULES/")
        }
        val hasAgentCli = normalized.any { it == AGENT_CLI }
        if (hasEntry && hasNodeModules && hasAgentCli) return
        val missing = buildList {
            if (!hasEntry) add("$ENTRY_STDIO missing at archive root")
            if (!hasNodeModules) add("$NODE_MODULES/ missing at archive root")
            if (!hasAgentCli) add("$AGENT_CLI missing at archive root (agent CLI bundle)")
        }
        val head = normalized.take(10).joinToString(", ")
        throw IOException(
            "qodex-dist archive contract violation: ${missing.joinToString("; ")} " +
                "— first entries: [$head]",
        )
    }

    /** Streams an uncompressed tar for diagnostics without keeping entries. */
    fun entryNames(tar: InputStream): List<String> {
        val names = ArrayList<String>(256)
        TarArchiveInputStream(tar.buffered(1 shl 16)).use { tis ->
            while (true) {
                val entry = tis.nextTarEntry ?: break
                names.add(entry.name)
            }
        }
        return names
    }
}
