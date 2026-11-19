package com.qodex.app.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Audit M-01 regression: every diagnostic sink (logcat + boot.log) passes
 * through LogRedactor — credential-shaped material must never survive.
 */
class LogRedactorTest {

    @Test
    fun `authorization header value is redacted but header kept`() {
        val out = LogRedactor.redact("request failed: Authorization: Bearer abc.def.ghi token rejected")
        assertFalse(out.contains("abc.def.ghi"))
        assertEquals(
            "request failed: Authorization: [REDACTED] token rejected",
            out,
        )
    }

    @Test
    fun `provider api keys are redacted`() {
        assertFalse(
            LogRedactor.redact("key sk-proj-abcdef1234567890xyz in message")
                .contains("abcdef1234567890xyz"),
        )
        assertFalse(
            LogRedactor.redact("ghp_1234567890abcdefghijklmnopqrstuv leaked")
                .contains("1234567890abcdefghijklmnopqrstuv"),
        )
        assertFalse(
            LogRedactor.redact("token github_pat_11B5NZWTI0abcdef_abcdefabcdefabcdef")
                .contains("11B5NZWTI0abcdef"),
        )
        assertFalse(
            LogRedactor.redact("google AIzaSyA1234567890abcdefghijklmnopqrstu")
                .contains("AIzaSyA1234567890"),
        )
    }

    @Test
    fun `query string secrets are redacted with key preserved`() {
        val out = LogRedactor.redact("GET https://api.example.com/v1?key=supersecret123&lang=en")
        assertFalse(out.contains("supersecret123"))
        assertEquals("GET https://api.example.com/v1?key=[REDACTED]&lang=en", out)
    }

    @Test
    fun `json credential fields are redacted`() {
        val out = LogRedactor.redact("""{"apiKey": "sk-verysecretvalue", "model": "x"}""")
        assertFalse(out.contains("sk-verysecretvalue"))
        assertTrue(out.contains(""""apiKey": "[REDACTED]""""))
        assertTrue(out.contains(""""model": "x""""))
    }

    @Test
    fun `credential secret env assignments are redacted`() {
        val out = LogRedactor.redact("env QODEX_CREDENTIAL_SECRET_FILE=/data/user/0/com.qodex.app/files/runtime/credential-secret")
        // The FILE PATH stays (it is a location, not a secret); only secret
        // VALUES like QODEX_CREDENTIAL_SECRET=hex lose their value.
        assertFalse(
            LogRedactor.redact("QODEX_CREDENTIAL_SECRET=aabbccdd00112233").contains("aabbccdd00112233"),
        )
        assertTrue(out.contains("QODEX_CREDENTIAL_SECRET_FILE="))
    }

    @Test
    fun `plain diagnostics pass through unchanged`() {
        val line = "engine| [qodex-server:stdio] provider-settings.getView OK (262.6ms)"
        assertEquals(line, LogRedactor.redact(line))
    }

    @Test
    fun `redaction never crashes on odd input`() {
        assertEquals("", LogRedactor.redact(""))
        assertEquals("Authorization:", LogRedactor.redact("Authorization:"))
        LogRedactor.redact("bearer " + "x".repeat(500))
        LogRedactor.redact("key sk-\nbad\tvalue ok")
    }
}
