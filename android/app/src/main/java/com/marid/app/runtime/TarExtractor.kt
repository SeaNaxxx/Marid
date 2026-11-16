package com.marid.app.runtime

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Paths

// Two-pass extraction (dirs/files, then links) so archive order can't break link targets; Termux entries re-root, absolute symlinks rewritten onto this runtime.
class TarExtractor(
    private val dest: File,
    private val report: ExtractReport = ExtractReport(),
    private val linkOps: LinkOps = DefaultLinkOps,
    private val copyBudgetBytes: Long = DEFAULT_COPY_BUDGET_BYTES,
) {

    /** Link primitives injectable for JVM tests. */
    interface LinkOps {
        fun createSymbolicLink(link: File, target: String)
        fun createLink(link: File, existing: File)
        fun isSymbolicLink(f: File): Boolean
    }

    object DefaultLinkOps : LinkOps {
        override fun createSymbolicLink(link: File, target: String) {
            Files.createSymbolicLink(link.toPath(), Paths.get(target))
        }

        override fun createLink(link: File, existing: File) {
            Files.createLink(link.toPath(), existing.toPath())
        }

        override fun isSymbolicLink(f: File): Boolean = Files.isSymbolicLink(f.toPath())
    }

    class ExtractReport {
        var regularFiles = 0
        var directories = 0
        var symlinksCreated = 0
        var symlinkCopies = 0
        var hardlinksCreated = 0
        var hardlinkCopies = 0
        var skippedEntries = 0
        var fallbackBytes = 0L
        var firstError: String? = null
    }

    class AppApiBugException(e: Throwable) :
        IOException(
            "APP DEFECT (not a device refusal): archive extraction used an API " +
                "missing on this Android level: ${e.javaClass.name}: ${e.message}", e,
        )

    fun extract(raw: InputStream) {
        val entries = mutableListOf<TarArchiveEntry>()
        TarArchiveInputStream(raw.buffered(1 shl 16)).use { tis ->
            while (true) {
                val entry = tis.nextTarEntry ?: break
                val target = relabeled(entry)
                if (target == null) {
                    report.skippedEntries++
                    continue
                }
                entries.add(entry)
                when {
                    entry.isDirectory -> {
                        target.mkdirs()
                        report.directories++
                    }
                    entry.isSymbolicLink || entry.isLink -> Unit // pass 2
                    else -> {
                        target.parentFile?.mkdirs()
                        FileOutputStream(target).use { tis.copyTo(it) }
                        applyMode(target, entry.mode)
                        report.regularFiles++
                    }
                }
            }
        }
        for (entry in entries) {
            val target = relabeled(entry) ?: continue
            if (entry.isSymbolicLink) extractSymlink(entry, target)
            else if (entry.isLink) extractHardlink(entry, target)
        }
    }

    // Re-roots ./data/data/com.termux/files/usr/... under dest; entries without the marker (DEBIAN control files) are skipped; traversal refused.
    private fun relabeled(entry: TarArchiveEntry): File? {
        val name = entry.name.removePrefix("./")
        val marker = "files/"
        val idx = name.indexOf(marker)
        val rooted = if (idx >= 0) name.substring(idx + marker.length) else name
        if (rooted.isEmpty()) return null
        val target = File(dest, rooted)
        if (!isInside(dest, target)) {
            throw IOException("archive entry escapes destination: ${entry.name}")
        }
        return target
    }

    private fun extractSymlink(entry: TarArchiveEntry, target: File) {
        target.parentFile?.mkdirs()
        if (target.exists() && !linkOps.isSymbolicLink(target)) target.delete()
        val rewritten = rewriteAbsoluteLink(entry.linkName)
        try {
            linkOps.createSymbolicLink(target, rewritten)
            report.symlinksCreated++
            return
        } catch (e: Throwable) {
            if (isAppApiBug(e)) throw AppApiBugException(e)
            if (report.firstError == null) {
                report.firstError = "${e.javaClass.simpleName}: ${e.message}"
            }
        }
        // Devices may refuse link() and old APIs lack createSymbolicLink — fall back to budget-capped copies.
        val resolved = resolveLinkTarget(entry, rewritten)
        if (resolved == null || !resolved.exists()) {
            report.skippedEntries++
            return
        }
        if (report.fallbackBytes + resolved.length() <= copyBudgetBytes) {
            if (runCatching { resolved.copyTo(target, overwrite = true) }.isSuccess) {
                applyMode(target, entry.mode)
                report.symlinkCopies++
                report.fallbackBytes += resolved.length()
                return
            }
        }
        report.skippedEntries++
    }

    private fun extractHardlink(entry: TarArchiveEntry, target: File) {
        target.parentFile?.mkdirs()
        val rewritten = rewriteAbsoluteLink(entry.linkName)
        val resolved = resolveLinkTarget(entry, rewritten)
        if (resolved == null || !resolved.isFile) {
            report.skippedEntries++
            return
        }
        try {
            linkOps.createLink(target, resolved)
            report.hardlinksCreated++
            return
        } catch (e: Throwable) {
            if (isAppApiBug(e)) throw AppApiBugException(e)
            if (report.firstError == null) {
                report.firstError = "${e.javaClass.simpleName}: ${e.message}"
            }
        }
        if (report.fallbackBytes + resolved.length() <= copyBudgetBytes) {
            if (runCatching { resolved.copyTo(target, overwrite = true) }.isSuccess) {
                applyMode(target, entry.mode)
                report.hardlinkCopies++
                report.fallbackBytes += resolved.length()
                return
            }
        }
        report.skippedEntries++
    }

    // Absolute symlinks into the Termux prefix are unreadable here; rewritten onto this runtime's root.
    private fun rewriteAbsoluteLink(linkName: String): String {
        val termuxPrefix = "/data/data/com.termux/files"
        if (linkName.startsWith(termuxPrefix)) {
            return File(dest, linkName.removePrefix("$termuxPrefix/")).absolutePath
        }
        return linkName
    }

    // Returns null when resolution escapes the destination — never fabricate paths outside the runtime.
    private fun resolveLinkTarget(entry: TarArchiveEntry, linkName: String): File? {
        if (linkName.isBlank()) return null
        if (linkName.startsWith("/")) {
            val c = runCatching { File(linkName).canonicalFile }.getOrNull() ?: return null
            val destCanon = dest.canonicalPath + File.separator
            return if (c.canonicalPath.startsWith(destCanon)) c else null
        }
        val asMember = canonicalUnderDest(File(dest, linkName)) ?: return null
        if (asMember.exists()) return asMember
        val parent = File(dest, entry.name.removePrefix("./")).parentFile ?: return null
        val asRelative = canonicalUnderDest(File(parent, linkName)) ?: return null
        return if (asRelative.exists()) asRelative else null
    }

    private fun canonicalUnderDest(f: File): File? {
        val c = try {
            f.canonicalFile
        } catch (_: IOException) {
            return null
        }
        val destCanon = dest.canonicalPath + File.separator
        return if (c.canonicalPath == dest.canonicalPath || c.canonicalPath.startsWith(destCanon)) c else null
    }

    private fun isInside(dest: File, target: File): Boolean {
        val c = try {
            target.canonicalPath
        } catch (_: IOException) {
            return false
        }
        val destCanon = dest.canonicalPath + File.separator
        return c.startsWith(destCanon) || c == dest.canonicalPath
    }

    /** App-API bugs must fail loudly, never be reported as device refusals. */
    private fun isAppApiBug(e: Throwable): Boolean {
        if (e is NoSuchMethodError || e is NoSuchMethodException) return true
        if (e is UnsupportedOperationException &&
            (e.cause is NoSuchMethodError || e.cause is NoSuchMethodException)
        ) return true
        return false
    }

    private fun applyMode(f: File, mode: Int) {
        val user = (mode shr 6) and 7
        f.setReadable(true)
        if (user and 4 != 0) f.setReadable(true, false) else f.setReadable(true, true)
        if (user and 2 != 0) f.setWritable(true)
        if (user and 1 != 0) f.setExecutable(true, false)
    }

    companion object {
        const val DEFAULT_COPY_BUDGET_BYTES: Long = 200L * 1024 * 1024
    }
}
