package com.qodex.app.runtime

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import java.io.File

// Termux's compiled-in OPENSSLDIR is EACCES under this app, so every TLS path must live inside the runtime; TLS verification is never weakened.
object BionicEnv {

    const val OPENSSL_CONF_REL = "usr/etc/tls/openssl.cnf"
    const val CERT_PEM_REL = "usr/etc/tls/cert.pem"
    const val CERTS_DIR_REL = "usr/etc/tls/certs"
    const val OSSL_MODULES_REL = "usr/lib/ossl-modules"

    // Written only when the bundled openssl.cnf is absent — an unreadable OPENSSL_CONF target is fatal at node startup.
    val FALLBACK_OPENSSL_CNF = """
        # Qodex embedded runtime — fallback OpenSSL configuration.
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

    fun apply(
        env: MutableMap<String, String>,
        runtimeRoot: File,
        home: File,
        bashBin: File,
    ) {
        val usr = File(runtimeRoot, "usr")
        // TMPDIR must exist before node/bash run: it is only guaranteed by
        // provisioning, and a wiped runtime/home would otherwise leave node
        // pointing at a dangling tmp directory.
        File(home, "tmp").mkdirs()
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

        // Stdio transport: the RPC channel rides the process pipes; no PORT, no
        // listener, no loopback TCP — other apps cannot reach the engine at all.
        env["QODEX_TRANSPORT"] = "stdio"
        // No OS credential store on Android: the file-based cipher secret lives in app-private storage, generated at provision time.
        env["QODEX_CREDENTIAL_SECRET_FILE"] =
            File(runtimeRoot, "credential-secret").absolutePath
        // Verification deliberately stays enabled: no NODE_TLS_REJECT_UNAUTHORIZED, no emptied CA paths.

        // Agent CLI child-process wiring. The engine spawns its agent client as
        // a separate node process; QodexAgentProcessManager's default resolver
        // checks QODEX_AGENT_SERVER_COMMAND FIRST (packages/services/src/qodex-agent/
        // qodexAgentProcessManager.ts). Without these vars the fallback chain
        // (monorepo dev → Electron → deployed native binary) has no Android
        // match and the first agent spawn fails with "Qodex agent server
        // command is not configured. Set QODEX_AGENT_SERVER_COMMAND before
        // integration." Same shape as the desktop release wiring
        // (packages/qodex-server-cli/src/runtime/agentWiring.ts).
        val nodeBin = File(usr, "bin/node")
        val agentCli = File(runtimeRoot, "dist/qodex.cjs")
        if (nodeBin.isFile && agentCli.isFile) {
            env["QODEX_AGENT_SERVER_COMMAND"] = nodeBin.absolutePath
            env["QODEX_AGENT_SERVER_ARGS_JSON"] = buildJsonArray {
                add(JsonPrimitive(agentCli.absolutePath))
                add(JsonPrimitive("app-server"))
                add(JsonPrimitive("--stdio"))
            }.toString()
            RuntimeBus.append(
                "agent CLI wiring: QODEX_AGENT_SERVER_COMMAND=${nodeBin.absolutePath} " +
                    "args=[${agentCli.absolutePath}, app-server, --stdio]",
            )
        } else {
            // Explicit failure signal instead of a mid-session "not configured"
            // surprise: provisioning verified both files, so a miss here means
            // the runtime tree was tampered with after provisioning.
            RuntimeBus.append(
                "agent CLI wiring unavailable (node=${nodeBin.isFile}, qodex.cjs=${agentCli.isFile})",
            )
        }
    }
}
