package com.qodex.app.state

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the V4 conversation projection delta rules (delta.ts): snapshot window
 * replacement, row append/upsert/remove semantics, streamable-path appends and
 * state patches — the exact behavior the engine guarantees on the wire.
 */
class ConversationProjectionTest {

    private fun row(
        rowId: Int,
        kind: String,
        text: String = "",
        state: String? = null,
    ): JsonObject {
        val base = "\"rowId\":$rowId,\"turnId\":\"t1\",\"createdAt\":1,\"createdAtSeq\":1,\"kind\":\"$kind\""
        val extra = when (kind) {
            "userInput" -> ",\"text\":\"$text\",\"origin\":\"realUser\""
            "assistantText" -> ",\"text\":\"$text\"" + (state?.let { ",\"state\":\"$it\"" } ?: "")
            "toolCall" -> ",\"toolCallId\":\"tc-$rowId\",\"toolName\":\"bash\",\"status\":\"running\",\"inputText\":\"$text\""
            else -> ""
        }
        return Json.parseToJsonElement("{$base$extra}") as JsonObject
    }

    private fun logicalFrame(payloadJson: String, toSeq: Long): JsonObject =
        Json.parseToJsonElement(
            """
            {"topic":"conversation/s","subscriptionId":"sub","fromSeq":0,"toSeq":$toSeq,"sentAt":1,
             "payload":$payloadJson}
            """.trimIndent(),
        ) as JsonObject

    @Test
    fun `snapshot replaces the visible window`() {
        val projection = ConversationProjection()
        val applied = projection.applyLogicalFrame(
            logicalFrame(
                """
                {"kind":"snapshot","snapshot":{"protocolVersion":1,"sessionId":"s","logEpoch":"e","seq":3,"revision":1,
                 "rows":{"window":[${row(1, "userInput", text = "hi")}],"firstRowId":1},
                 "meta":{"title":"Hi"}}}
                """.trimIndent(),
                toSeq = 3,
            ),
        )
        assertTrue(applied)
        val view = projection.view()
        assertEquals(1, view.rows.size)
        assertEquals("userInput", view.rows[0].string("kind"))
        assertEquals("Hi", (view.state["meta"] as JsonObject).string("title"))
        assertEquals(3L, projection.lastSeq)
    }

    @Test
    fun `row appended then delta streams text into the same row`() {
        val projection = ConversationProjection()
        projection.applyLogicalFrame(
            logicalFrame(
                """
                {"kind":"deltas","deltas":[{"op":"row.appended","row":${row(2, "assistantText", state = "streaming")}}]}
                """.trimIndent(),
                toSeq = 5,
            ),
        )
        projection.applyLogicalFrame(
            logicalFrame(
                """
                {"kind":"deltas","deltas":[{"op":"row.delta","rowId":2,"path":"text","append":"Hel"},{"op":"row.delta","rowId":2,"path":"text","append":"lo"}]}
                """.trimIndent(),
                toSeq = 6,
            ),
        )
        val rows = projection.view().rows
        assertEquals(1, rows.size)
        assertEquals("Hello", rows[0].string("text"))
        assertEquals("assistantText", rows[0].string("kind"))
    }

    @Test
    fun `row upserted replaces the row by id`() {
        val projection = ConversationProjection()
        projection.applyLogicalFrame(
            logicalFrame(
                """
                {"kind":"deltas","deltas":[{"op":"row.appended","row":${row(7, "toolCall", text = "ls")}}]}
                """.trimIndent(),
                toSeq = 8,
            ),
        )
        projection.applyLogicalFrame(
            logicalFrame(
                """
                {"kind":"deltas","deltas":[{"op":"row.upserted","row":${row(7, "toolCall", text = "ls -la")}}]}
                """.trimIndent(),
                toSeq = 9,
            ),
        )
        val rows = projection.view().rows
        assertEquals(1, rows.size)
        assertEquals("ls -la", rows[0].string("inputText"))
    }

    @Test
    fun `row removed drops the row and everything after`() {
        val projection = ConversationProjection()
        projection.applyLogicalFrame(
            logicalFrame(
                """
                {"kind":"deltas","deltas":[
                  {"op":"row.appended","row":${row(1, "userInput", text = "a")}},
                  {"op":"row.appended","row":${row(2, "assistantText")}},
                  {"op":"row.appended","row":${row(3, "assistantText")}}]}
                """.trimIndent(),
                toSeq = 10,
            ),
        )
        projection.applyLogicalFrame(
            logicalFrame(
                """{"kind":"deltas","deltas":[{"op":"row.removed","fromRowId":2}]}""",
                toSeq = 11,
            ),
        )
        val rows = projection.view().rows
        assertEquals(1, rows.size)
        assertEquals(1, rows[0].int("rowId"))
    }

    @Test
    fun `state patches merge key-wise and output dot-path appends land inside output`() {
        val projection = ConversationProjection()
        projection.applyLogicalFrame(
            logicalFrame(
                """
                {"kind":"deltas","deltas":[
                  {"op":"row.appended","row":{"rowId":9,"turnId":"t","createdAt":1,"createdAtSeq":1,"kind":"toolCall","toolCallId":"c1","toolName":"bash","status":"inputStreaming","inputText":"","output":{"text":""}}},
                  {"op":"state.updated","patch":{"revision":1}},
                  {"op":"row.delta","rowId":9,"path":"output.text","append":"done"},
                  {"op":"state.updated","patch":{"revision":2,"usage":{"outputTokens":42}}}]}
                """.trimIndent(),
                toSeq = 20,
            ),
        )
        val view = projection.view()
        assertEquals("done", ((view.rows[0]["output"] as JsonObject)["text"] as JsonPrimitive).content)
        assertEquals(2, (view.state["revision"] as JsonPrimitive).content.toInt())
        assertEquals(
            42,
            ((view.state["usage"] as JsonObject)["outputTokens"] as JsonPrimitive).content.toInt(),
        )
    }

    @Test
    fun `plugin and workflow deltas are ignored without breaking the projection`() {
        val projection = ConversationProjection()
        val applied = projection.applyLogicalFrame(
            logicalFrame(
                """
                {"kind":"deltas","deltas":[{"op":"pluginUi.resourceUpdated","pluginId":"p","serverName":"s","uri":"u","subscribers":[]}]}
                """.trimIndent(),
                toSeq = 30,
            ),
        )
        assertTrue(!applied)
        assertEquals(0, projection.view().rows.size)
        assertNull(projection.view().rows.firstOrNull())
    }

    @Test
    fun `clear resets rows state and seq`() {
        val projection = ConversationProjection()
        projection.applyLogicalFrame(
            logicalFrame(
                """
                {"kind":"snapshot","snapshot":{"protocolVersion":1,"sessionId":"s","logEpoch":"e","seq":3,"revision":1,"rows":{"window":[],"firstRowId":null}}}
                """.trimIndent(),
                toSeq = 3,
            ),
        )
        projection.clear()
        val view = projection.view()
        assertEquals(0, view.rows.size)
        assertNull(projection.lastSeq)
    }
}
