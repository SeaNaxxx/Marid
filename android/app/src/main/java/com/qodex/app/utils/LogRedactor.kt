package com.qodex.app.utils

/**
 * Central secret redaction for every diagnostic sink (logcat + boot.log).
 * Audit M-01: there was no guaranteed redaction boundary between exception
 * messages, engine stderr and the persistent log — one pass here protects all
 * writers. Rules target credential-shaped material only and keep the
 * structural prefix (header name / query key / JSON field) so logs stay
 * diagnosable.
 */
object LogRedactor {

    private const val REDACTED = "[REDACTED]"

    private data class Rule(val pattern: Regex, val replacement: String)

    private val rules = listOf(
        // Authorization / API-key style headers: keep the header name, drop the
        // value — including a leading Bearer/Basic scheme word so the token
        // after it cannot survive the header match.
        Rule(
            Regex("(?i)((?:authorization|proxy-authorization|x-api-key|api-key|api_token)\\s*[:=]\\s*)(?:bearer\\s+|basic\\s+|token\\s+)?(\\S+)"),
            "$1$REDACTED",
        ),
        // Standalone Bearer credentials without a header name.
        Rule(
            Regex("(?i)(bearer\\s+)([A-Za-z0-9._~+\\-=]{8,})"),
            "$1$REDACTED",
        ),
        // Common provider key shapes (OpenAI/Anthropic-style, GitHub, Google).
        Rule(Regex("\\b((?:sk|rk|pk)-)[A-Za-z0-9_-]{8,}\\b"), "$1$REDACTED"),
        Rule(Regex("\\b(gh[pousr]_)[A-Za-z0-9]{20,}\\b"), "$1$REDACTED"),
        Rule(Regex("\\b(github_pat_)[A-Za-z0-9_]{20,}\\b"), "$1$REDACTED"),
        Rule(Regex("\\b(AIza)[A-Za-z0-9_-]{20,}\\b"), "$1$REDACTED"),
        // Query-string secrets: keep the key, drop the value.
        Rule(
            Regex("(?i)([?&](?:api[_-]?key|access[_-]?token|refresh[_-]?token|client[_-]?secret|token|secret|password|key)=)([^&\\s\"']*)"),
            "$1$REDACTED",
        ),
        // JSON/YAML credential fields.
        Rule(
            Regex("(?i)(\"?(?:api[_-]?key|apikey|access[_-]?token|client[_-]?secret|secret[_-]?access[_-]?key|password)\"?\\s*[:=]\\s*)\"[^\"]+\""),
            "$1\"$REDACTED\"",
        ),
        // Credential-store environment values.
        Rule(
            Regex("(?i)(QODEX_CREDENTIAL_SECRET[A-Z_]*\\s*=\\s*)(\\S+)"),
            "$1$REDACTED",
        ),
    )

    fun redact(message: String): String {
        var out = message
        for (rule in rules) {
            out = rule.pattern.replace(out, rule.replacement)
        }
        return out
    }
}
