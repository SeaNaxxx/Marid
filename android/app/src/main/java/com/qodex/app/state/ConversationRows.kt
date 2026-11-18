package com.qodex.app.state

import com.qodex.app.ui.AssistantBlock
import com.qodex.app.ui.ChatTurn
import com.qodex.app.ui.ToolCallStatus
import kotlinx.serialization.json.JsonObject

/**
 * Maps V4 conversation rows (rows.ts schema kinds) into the chat surface model.
 * The engine owns row semantics; this mapping only renders — turnHeader carries
 * turn state (drives the working indicator), userInput renders as the user
 * bubble, assistantText/reasoning stream as assistant blocks, toolCall renders
 * as a tool card, and remaining row kinds surface as honest system lines
 * (never fabricated content).
 */
fun mapRowsToTurns(rows: List<JsonObject>): List<ChatTurn> {
    val turns = mutableListOf<ChatTurn>()
    var assistantBlocks = mutableListOf<AssistantBlock>()
    var pendingRunBlocks = mutableListOf<AssistantBlock>()

    fun flushAssistant() {
        if (assistantBlocks.isNotEmpty()) {
            turns.add(ChatTurn.AssistantTurn(assistantBlocks.toList()))
            assistantBlocks = mutableListOf()
        }
        if (pendingRunBlocks.isNotEmpty()) {
            turns.add(ChatTurn.AssistantTurn(pendingRunBlocks.toList()))
            pendingRunBlocks = mutableListOf()
        }
    }

    for (row in rows) {
        when (row.string("kind")) {
            "userInput" -> {
                flushAssistant()
                val text = row.string("text")
                if (text.isNotEmpty()) turns.add(ChatTurn.UserTurn(text))
            }
            "assistantText" -> {
                val text = row.string("text")
                if (text.isEmpty()) continue
                assistantBlocks.add(AssistantBlock.MessageText(text))
            }
            "reasoning" -> {
                val text = row.string("text")
                if (text.isEmpty()) continue
                val streaming = row.string("state") == "streaming"
                assistantBlocks.add(AssistantBlock.Reasoning(text = text, streaming = streaming))
            }
            "toolCall" -> {
                val name = row.string("toolName").ifEmpty { "tool" }
                val status = when (row.string("status")) {
                    "success" -> ToolCallStatus.COMPLETED
                    "error" -> ToolCallStatus.FAILED
                    "cancelled" -> ToolCallStatus.DENIED
                    else -> ToolCallStatus.RUNNING
                }
                val summary = row.string("inputText")
                val detail = toolOutputDetail(row)
                assistantBlocks.add(
                    AssistantBlock.ToolCall(name = name, status = status, summary = summary, detail = detail),
                )
            }
            "subagent" -> {
                val status = when (row.string("status")) {
                    "success" -> ToolCallStatus.COMPLETED
                    "failed" -> ToolCallStatus.FAILED
                    else -> ToolCallStatus.RUNNING
                }
                assistantBlocks.add(
                    AssistantBlock.ToolCall(
                        name = row.string("subagentType").ifEmpty { "subagent" },
                        status = status,
                        summary = row.string("summaryText"),
                        detail = null,
                    ),
                )
            }
            "artifact" -> {
                val name = row.string("displayName")
                if (name.isNotEmpty()) {
                    assistantBlocks.add(
                        AssistantBlock.ToolCall(
                            name = "artifact",
                            status = ToolCallStatus.COMPLETED,
                            summary = name,
                            detail = row.string("artifactType"),
                        ),
                    )
                }
            }
            "hookInvocation", "timelineMarker", "turnHeader", "unreadable" -> {
                // turnHeader only toggles turn state; markers and hook runs have
                // no dedicated mobile card yet — skipping keeps the transcript honest.
            }
            else -> Unit
        }
    }
    flushAssistant()
    return turns
}

private fun toolOutputDetail(row: JsonObject): String? {
    val error = row["error"] as? JsonObject
    if (error != null) {
        val message = error.string("message")
        if (message.isNotEmpty()) return message
    }
    val output = row["output"] as? JsonObject ?: return null
    return outputPreview(output)
}

// Output objects carry one of several text-bearing fields; the first non-empty
// preview wins. Bounded to keep the card readable.
private fun outputPreview(output: JsonObject): String? {
    val candidates = listOf("text", "content", "output", "result", "summary", "message")
    for (key in candidates) {
        val value = (output[key] as? kotlinx.serialization.json.JsonPrimitive)?.content
        if (!value.isNullOrEmpty()) return value.take(2000)
    }
    return null
}
