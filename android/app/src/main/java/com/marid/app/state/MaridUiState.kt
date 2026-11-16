package com.marid.app.state

import kotlinx.serialization.json.JsonElement

/**
 * UI models mirroring the desktop conversation renderers
 * (packages/ui/src/v4/ConversationTurnGroup.tsx + ToolCallBlocks) so the
 * Compose chat area is a 1:1 translation of the React UI.
 */

/** Tool call kinds — lucide icons per ToolSummaryRow renderers. */
enum class ToolKind(val label: String) {
    READ("Read"),
    EDIT("Edit"),
    BASH("Bash"),
    WRITE("Write"),
}

enum class ToolStatus { COMPLETED, RUNNING, FAILED }

/** One tool row: icon + kind label + detail (file path / command) + status. */
data class ToolRow(
    val kind: ToolKind,
    val detail: String,
    val status: ToolStatus,
    val isCommand: Boolean = false, // Bash rows render monospace
)

/** "1 file changed +1 -0" summary card (MessageChangeSummaryPanel.tsx). */
data class ChangeSummary(
    val files: Int,
    val added: Int,
    val removed: Int,
    val undonable: Boolean = true,
)

/** An agent turn group: "Worked for {duration}" header + rows + markdown text. */
data class AgentTurn(
    val workedForLabel: String, // "Worked for 8s" (chat.history.workedFor)
    val toolRows: List<ToolRow> = emptyList(),
    val agentText: String = "",
    val changeSummary: ChangeSummary? = null,
    val live: Boolean = false, // true while streaming from the engine
)

/** User input bubble (ConversationUserInputBubble.tsx). */
data class UserTurn(val text: String)

sealed class ChatTurn {
    data class User(val turn: UserTurn) : ChatTurn()
    data class Agent(val turn: AgentTurn) : ChatTurn()
}

/** Model selector entry — from model-selection.getView(). */
data class ModelEntry(val provider: String, val model: String) {
    val label: String get() = "$provider/$model"
}

/** Sidebar task list entry (ProjectSidebar task rows). */
data class TaskEntry(
    val title: String,
    val status: String = "IDLE", // RUNNING | COMPLETED | IDLE
    val active: Boolean = false,
)

/** Composer permission mode — V4ComposerModeControls.tsx. */
enum class ComposerMode(val label: String) {
    ASK_BEFORE_CHANGES("Ask before changes"),
}

/** Everything the Compose UI renders. */
data class MaridUiState(
    val connectionState: RpcConnectionStateCompat = RpcConnectionStateCompat.DISCONNECTED,
    val engineHealthy: Boolean = false,
    val provisionStatus: String = "",
    val taskTitle: String = "هلا",
    val workspacePath: String = "/data/data/com.marid.app/files/workspace",
    val turns: List<ChatTurn> = seedTurns(),
    val composerMode: ComposerMode = ComposerMode.ASK_BEFORE_CHANGES,
    val models: List<ModelEntry> = listOf(ModelEntry("mock-provider", "mock-model")),
    val selectedModel: ModelEntry = ModelEntry("mock-provider", "mock-model"),
    val thoughtLevel: String = "Off",
    val modelMenuOpen: Boolean = false,
    val modeMenuOpen: Boolean = false,
    val composerText: String = "",
    val sendEnabled: Boolean = false,
    val sending: Boolean = false,
    val terminalOpen: Boolean = false,
    val terminalLines: List<String> = listOf("Marid terminal — engine diagnostics"),
    val drawerOpen: Boolean = false,
    val tasks: List<TaskEntry> = listOf(
        TaskEntry(title = "هلا", status = "COMPLETED", active = true),
    ),
) {
    companion object {
        /**
         * Seed conversation mirroring the reference screenshot: task "هلا",
         * "Worked for 8s", tool rows, "1 file changed +1 -0" + Undo.
         * Replaced/augmented by live engine events once connected.
         */
        fun seedTurns(): List<ChatTurn> = listOf(
            ChatTurn.User(UserTurn("هلا")),
            ChatTurn.Agent(
                AgentTurn(
                    workedForLabel = "Worked for 8s",
                    toolRows = listOf(
                        ToolRow(kind = ToolKind.EDIT, detail = "MainActivity.kt", status = ToolStatus.COMPLETED),
                    ),
                    agentText = "Marid is ready. The embedded engine will answer here once it is provisioned and running.",
                    changeSummary = ChangeSummary(files = 1, added = 1, removed = 0),
                ),
            ),
        )
    }
}

/** Local alias so the UI layer does not import the rpc package directly. */
typealias RpcConnectionStateCompat = com.marid.app.rpc.RpcConnectionState
