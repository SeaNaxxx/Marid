package com.marid.app.runtime

import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Pure-JVM two-pass tar extraction for the embedded Bionic runtime and the
 * engine dist. Fresh Marid implementation inheriting the CONTRACT from
 * Zcode-Android's device failures (docs/architecture/OLD-ZCODE-RESEARCH.md §2.5):
 *
 * - TWO-PASS design: pass 1 extracts directories + regular files, pass 2
 *   creates symlinks/hardlinks once their targets exist. This removes the
 *   archive-ordering fragility of fallback copies entirely.
 * - Termux debs store paths as data/data/com.termux/files/usr/... — every
 *   entry is re-rooted at "files/" so usr/... lands under [dest].
 * - Devices may refuse link() (Zcode-Android: AccessDeniedException x135) and
 *   API 24/25 lack Files.createSymbolicLink: symlinks fall back to
 *   budget-capped copies of the resolved target instead of failing.
 * - App-API bugs must FAIL LOUDLY, never masquerade as device refusals
 *   (NoSuchMethodError on an API level that has the API = app defect).
 * - Path-traversal guard on every entry and every link resolution.
 * - Tar mode bits are applied (chmod) for regular files.
 */
class TarExtractor(
    private val dest: File,
    private val report: ExtractReport = ExtractReport(),
    private val linkOps: LinkOps = DefaultLinkOps,
    private val copyBudgetBytes: Long = DEFAULT_COPY_BUDGET_BYTES,
) {

    /** Link primitives, injectable for JVM tests. */
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
            // Pass 1: directories + regular files (streaming; bodies read in order).
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
        // Pass 2: links (targets now exist on disk).
        for (entry in entries) {
            val target = relabeled(entry) ?: continue
            if (entry.isSymbolicLink) extractSymlink(entry, target)
            else if (entry.isLink) extractHardlink(entry, target)
        }
    }

    /**
     * Re-roots a Termux deb entry (./data/data/com.termux/files/usr/...) at
     * [dest]. Entries without the marker (DEBIAN control files, debian-binary)
     * return null and are skipped. Traversal outside [dest] is refused.
     */
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

    // ------------------------------------------------------------- symlinks

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
        // Fallback: copy the resolved target (budget-capped).
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

    // ------------------------------------------------------------ hardlinks

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

    /**
     * Termux debs may contain ABSOLUTE symlinks into the Termux prefix
     * (/data/data/com.termux/files/...). Those are unreadable for this app,
     * so they are rewritten onto THIS runtime's root before creation.
     */
    private fun rewriteAbsoluteLink(linkName: String): String {
        val termuxPrefix = "/data/data/com.termux/files"
        if (linkName.startsWith(termuxPrefix)) {
            return File(dest, linkName.removePrefix("$termuxPrefix/")).absolutePath
        }
        return linkName
    }

    /**
     * Resolves a link target under [dest]. Accepts absolute (already
     * rewritten) targets and tar-member / parent-relative ones; returns null
     * when the resolution escapes the destination (never fabricate paths
     * outside the runtime).
     */
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
        /** Worst-case on-device copy fallback budget (failure-1/2 evidence). */
        const val DEFAULT_COPY_BUDGET_BYTES: Long = 200L * 1024 * 1024
    }
}
