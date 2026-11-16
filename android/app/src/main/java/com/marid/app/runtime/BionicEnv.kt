package com.marid.app.runtime

import java.io.File

/**
 * Environment contract for every Bionic process (provision probe, engine,
 * agent children). Fresh implementation for Marid, inheriting the CONTRACT
 * proven by Zcode-Android (see docs/architecture/OLD-ZCODE-RESEARCH.md §2.4):
 *
 * - Termux-built libcrypto has OPENSSLDIR compiled in as
 *   /data/data/com.termux/files/usr/etc/tls -> EACCES for this app -> fatal
 *   "OpenSSL configuration error" (node exit 13) unless OPENSSL_CONF,
 *   OPENSSL_MODULES and the CA variables point INSIDE our own runtime.
 * - TLS certificate verification is NEVER disabled (no NODE_TLS_REJECT_*
 *   tampering, no empty CA stores). The ca-certificates deb ships a real
 *   Mozilla bundle at usr/etc/tls/cert.pem.
 * - Every variable points inside the app-private runtime, never at Termux.
 *
 * Pure JVM (no android.* imports) so the exact contract stays unit-testable.
 */
object BionicEnv {

    const val OPENSSL_CONF_REL = "usr/etc/tls/openssl.cnf"
    const val CERT_PEM_REL = "usr/etc/tls/cert.pem"
    const val CERTS_DIR_REL = "usr/etc/tls/certs"
    const val OSSL_MODULES_REL = "usr/lib/ossl-modules"

    /**
     * Minimal fallback config written ONLY when the bundled openssl.cnf is
     * absent. It must exist and be readable — an UNREADABLE OPENSSL_CONF
     * target is fatal at node startup. Deliberately minimal: defaults apply
     * and TLS verification stays on.
     */
    val FALLBACK_OPENSSL_CNF = """
        # Marid embedded runtime — fallback OpenSSL configuration.
        # Written because the bundled openssl.cnf was missing. The Termux-built
        # libcrypto has OPENSSLDIR compiled in as /data/data/com.termux/...,
        # which is unreadable for this app (fatal at node startup), so
        # OPENSSL_CONF always points at a file inside the app runtime.
        # Intentionally minimal: defaults apply and TLS verification stays on.
        [openssl_init]
    """.trimIndent() + "\n"

    fun ensureOpenSslConfig(runtimeRoot: File): File {
        val cnf = File(runtimeRoot, OPENSSL_CONF_REL)
        cnf.parentFile?.mkdirs()
        if (!cnf.exists() || !cnf.canRead()) {
            cnf.writeText(FALLBACK_OPENSSL_CNF)
        }
        return cnf
    }

    /**
     * Apply the full contract to [env]. [runtimeRoot] is filesDir/runtime —
     * the parent of usr/ and dist/. [port] is the engine HTTP/WS port.
     */
    fun apply(
        env: MutableMap<String, String>,
        runtimeRoot: File,
        home: File,
        bashBin: File,
        port: Int,
    ) {
        val usr = File(runtimeRoot, "usr")
        env["PREFIX"] = usr.absolutePath
        env["HOME"] = home.absolutePath
        env["TMPDIR"] = File(home, "tmp").absolutePath
        env["PATH"] = "${usr.absolutePath}/bin:/system/bin:/system/xbin"
        env["LD_LIBRARY_PATH"] = "${usr.absolutePath}/lib"
        env["SHELL"] = bashBin.absolutePath
        env["LANG"] = "C.UTF-8"
        env["NODE_OPTIONS"] = ""
        env["PAGER"] = "cat"
        env["GIT_PAGER"] = "cat"
        env["GIT_CONFIG_NOSYSTEM"] = "1"

        // --- OpenSSL / TLS wiring (device failure 2 contract) ---------------
        val cnf = runCatching { ensureOpenSslConfig(runtimeRoot) }.getOrNull()
        if (cnf != null && cnf.isFile) {
            env["OPENSSL_CONF"] = cnf.absolutePath
        }
        val modules = File(runtimeRoot, OSSL_MODULES_REL)
        if (modules.isDirectory) {
            env["OPENSSL_MODULES"] = modules.absolutePath
        }
        val cert = File(runtimeRoot, CERT_PEM_REL)
        if (cert.isFile) {
            env["SSL_CERT_FILE"] = cert.absolutePath
            env["GIT_SSL_CAINFO"] = cert.absolutePath
            // Widens the trusted set with the real Mozilla bundle; never weakens.
            env["NODE_EXTRA_CA_CERTS"] = cert.absolutePath
        }
        val certsDir = File(runtimeRoot, CERTS_DIR_REL)
        if (certsDir.isDirectory) {
            env["SSL_CERT_DIR"] = certsDir.absolutePath
        }

        // --- Engine wiring ---------------------------------------------------
        env["PORT"] = port.toString()
        env["MARID_SERVER_HOST"] = "127.0.0.1"
        // Headless-server credential contract (no OS credential store on
        // Android; discovered by the sandbox engine boot test): the file-based
        // cipher secret lives inside app-private storage, generated at
        // provision time by Provisioner and never regenerated.
        env["MARID_CREDENTIAL_SECRET_FILE"] =
            File(runtimeRoot, "credential-secret").absolutePath
        // NOTE: verification is deliberately left enabled — this function
        // never sets NODE_TLS_REJECT_UNAUTHORIZED, never empties CA paths.
    }
}
