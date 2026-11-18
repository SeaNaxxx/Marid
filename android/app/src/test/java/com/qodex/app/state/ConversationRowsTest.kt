package com.qodex.app.state

import com.qodex.app.ui.ChatTurn
import com.qodex.app.ui.ToolCallStatus
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the rows-to-turns rendering contract: user rows become user turns,
 * assistant/reasoning/tool rows group into assistant turns in row order, and
 * rows without a mobile card (turnHeader, markers, hook runs) never fabricate
 * visible content.
 */
class ConversationRowsTest {

    private fun rows(vararg json: String): List<JsonObject> =
        json.map { Json.parseToJsonElement(it) as JsonObject }

    private fun row(rowId: Int, kind: String, body: String = ""): String {
        val base = """"rowId":$rowId,"turnId":"t","createdAt":1,"createdAtSeq":1,"kind":"$kind""""
        return "{$base${if (body.isEmpty()) "" else ",$body"}}"
    }

    @Test
    fun `user and assistant rows map to turns in order`() {
        val turns = mapRowsToTurns(
            rows(
                row(1, "turnHeader", """"origin":"userInput","state":"running""""),
                row(2, "userInput", """"text":"hi","origin":"realUser""""),
                row(3, "reasoning", """"text":"thinking","state":"streaming""""),
                row(4, "assistantText", """"text":"Hello there","state":"complete""""),
            ),
        )
        assertEquals(2, turns.size)
        assertTrue(turns[0] is ChatTurn.UserTurn)
        assertEquals("hi", (turns[0] as ChatTurn.UserTurn).text)
        val assistant = turns[1] as ChatTurn.AssistantTurn
        assertEquals(2, assistant.blocks.size)
        assertTrue(assistant.blocks[0] is com.qodex.app.ui.AssistantBlock.Reasoning)
        assertTrue(assistant.blocks[1] is com.qodex.app.ui.AssistantBlock.MessageText)
    }

    @Test
    fun `tool status maps onto card states and error detail surfaces`() {
        val turns = mapRowsToTurns(
            rows(
                row(2, "userInput", """"text":"run ls","origin":"realUser""""),
                row(
                    3,
                    "toolCall",
                    """"toolCallId":"c1","toolName":"bash","status":"error","inputText":"ls -la","error":{"code":"E","message":"boom"}""",
                ),
                row(
                    4,
                    "toolCall",
                    """"toolCallId":"c2","toolName":"bash","status":"success","inputText":"git status","output":{"text":"clean"}""",
                ),
            ),
        )
        val assistant = turns[1] as ChatTurn.AssistantTurn
        val failed = assistant.blocks[0] as com.qodex.app.ui.AssistantBlock.ToolCall
        assertEquals(ToolCallStatus.FAILED, failed.status)
        assertEquals("boom", failed.detail)
        val completed = assistant.blocks[1] as com.qodex.app.ui.AssistantBlock.ToolCall
        assertEquals(ToolCallStatus.COMPLETED, completed.status)
        assertEquals("clean", completed.detail)
    }

    @Test
    fun `markers and hook rows render nothing instead of fake content`() {
        val marker =
            "\"marker\":{\"type\":\"modelChange\",\"toProvider\":\"p\",\"toModel\":\"m\",\"toThought\":\"high\"}"
        val hook =
            "\"hookInvocationId\":\"h1\",\"hookEventName\":\"Stop\",\"hookCount\":1,\"state\":\"completed\",\"startedAt\":1,\"lane\":\"assistantWork\",\"executions\":[]"
        val turns = mapRowsToTurns(
            rows(
                row(1, "turnHeader", "\"origin\":\"userInput\",\"state\":\"running\""),
                row(2, "timelineMarker", marker),
                row(3, "hookInvocation", hook),
            ),
        )
        assertEquals(0, turns.size)
    }

    @Test
    fun `subagent and artifact rows surface as completed tool cards`() {
        val turns = mapRowsToTurns(
            rows(
                row(1, "subagent", """"subagentType":"explore","status":"success","summaryText":"found 2 files""""),
                row(2, "artifact", """"artifactVersionId":"v1","logicalArtifactKey":"k","displayName":"report.md","artifactType":"md","mimeType":"text/markdown","sizeBytes":3,"sha256":"${"0".repeat(64)}","ref":"r","state":"current""""),
            ),
        )
        val assistant = turns[0] as ChatTurn.AssistantTurn
        assertEquals(2, assistant.blocks.size)
        val subagent = assistant.blocks[0] as com.qodex.app.ui.AssistantBlock.ToolCall
        assertEquals(ToolCallStatus.COMPLETED, subagent.status)
        assertEquals("explore", subagent.name)
    }
}
