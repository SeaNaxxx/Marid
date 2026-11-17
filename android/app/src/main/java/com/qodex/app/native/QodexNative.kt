package com.qodex.app.native

// Phase 2 baseline binding for libqodex_native.so (crates/qodex-native).
// The library is OPTIONAL by design: when the .so is not shipped for this
// build/device every accessor returns null/false and callers fall back to the
// Node engine path. Nothing in the UI depends on it yet.
object QodexNative {

    @Volatile
    private var loadState: LoadState = LoadState.NotAttempted

    private enum class LoadState { NotAttempted, Loaded, Unavailable }

    val available: Boolean
        get() = ensureLoaded() == LoadState.Loaded

    private fun ensureLoaded(): LoadState {
        val current = loadState
        if (current != LoadState.NotAttempted) return current
        synchronized(this) {
            if (loadState == LoadState.NotAttempted) {
                loadState = try {
                    System.loadLibrary("qodex_native")
                    LoadState.Loaded
                } catch (_: UnsatisfiedLinkError) {
                    LoadState.Unavailable
                }
            }
            return loadState
        }
    }

    /** Newline-delimited workspace file paths, or null when native is absent. */
    fun walkFilesSafe(root: String, maxEntries: Int): String? =
        if (available) walkFiles(root, maxEntries) else null

    /** JSON array of "path:line:text" regex matches, or null when native is absent. */
    fun searchFilesSafe(root: String, pattern: String, maxResults: Int): String? =
        if (available) searchFiles(root, pattern, maxResults) else null

    @JvmStatic
    private external fun walkFiles(root: String, maxEntries: Int): String

    @JvmStatic
    private external fun searchFiles(root: String, pattern: String, maxResults: Int): String
}
