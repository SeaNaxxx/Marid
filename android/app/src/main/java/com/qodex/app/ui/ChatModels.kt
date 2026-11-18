package com.qodex.app.ui

/**
 * Chat surface model shared by the renderer and the V4 row mapper
 * (state/ConversationRows.kt). Pure Kotlin on purpose: keeping these types out
 * of Compose files lets the projection mapping stay JVM-unit-testable.
 */
enum class ToolCallStatus(val label: String) {
    PENDING("Pending"), // chat.toolCall.status.pending   / "input-streaming"
    RUNNING("Running"), // chat.toolCall.status.running    / "input-available"
    COMPLETED("Completed"), // chat.toolCall.status.completed / "output-available"
    FAILED("Failed"), // chat.toolCall.status.failed     / "output-error"
    DENIED("Denied"), // chat.toolCall.status.denied     / "output-denied"
}

sealed class ChatTurn {
    data class UserTurn(val text: String) : ChatTurn()

    data class AssistantTurn(val blocks: List<AssistantBlock>) : ChatTurn()

    data class ToolCallTurn(
        val name: String,
        val status: ToolCallStatus,
        val summary: String,
        val detail: String? = null,
        val progressFraction: Float? = null,
        val progressMessage: String? = null,
    ) : ChatTurn()
}

sealed class AssistantBlock {
    data class MessageText(val text: String) : AssistantBlock()

    data class ToolCall(
        val name: String,
        val status: ToolCallStatus,
        val summary: String,
        val detail: String? = null,
        val progressFraction: Float? = null,
        val progressMessage: String? = null,
    ) : AssistantBlock()

    data class Reasoning(
        val text: String,
        val streaming: Boolean = false,
        val durationSeconds: Int? = null,
    ) : AssistantBlock()

    data class ChangeSummary(val files: List<ChangedFile>) : AssistantBlock()
}

data class ChangedFile(val path: String, val added: Int, val removed: Int)
