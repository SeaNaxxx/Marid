package com.marid.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marid.app.icons.MaridIcons
import com.marid.app.state.AgentTurn
import com.marid.app.state.ChatTurn
import com.marid.app.state.ChangeSummary
import com.marid.app.state.ComposerMode
import com.marid.app.state.MaridUiState
import com.marid.app.state.ToolKind
import com.marid.app.state.ToolRow
import com.marid.app.state.ToolStatus
import com.marid.app.theme.MaridRadius
import com.marid.app.theme.MaridTextSize
import com.marid.app.theme.monoStyle
import com.marid.app.theme.maridTokens

/**
 * Chat area — 1:1 port of the conversation renderers:
 * - ConversationTurnGroup.tsx: turn sections "relative mx-auto flex w-full
 *   flex-col gap-5 px-4 pb-5", AssistantHistoryStatus "Worked for {duration}"
 * - ConversationUserInputBubble.tsx: right-aligned bubble
 *   "rounded-xl rounded-tr-xs border border-border bg-surface px-4 py-3"
 * - ToolSummaryRow.tsx: icon + kind + detail rows (12px apart)
 * - MessageChangeSummaryPanel.tsx: "{n} file changed +N -N" card with Undo
 */
@Composable
fun MaridChatArea(
    state: MaridUiState,
    onUndoChanges: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val t = maridTokens()
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(t.background), // bg-background #161616
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 16.dp, // px-4
            vertical = 16.dp, // pb-5 + top breathing room
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp), // gap-5 between rows
        reverseLayout = false,
    ) {
        items(state.turns) { turn ->
            when (turn) {
                is ChatTurn.User -> UserBubble(turn.turn.text)
                is ChatTurn.Agent -> AgentTurnGroup(
                    turn = turn.turn,
                    onUndoChanges = onUndoChanges,
                )
            }
        }
    }
}

/** ConversationUserInputBubble.tsx:27-31 — right aligned bubble. */
@Composable
private fun UserBubble(text: String) {
    val t = maridTokens()
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.End, // "flex flex-col items-end"
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = 480.dp) // max-w-xl desktop; phone-fitted
                .clip(
                    RoundedCornerShape(
                        topStart = MaridRadius.Xl.dp,
                        topEnd = 4.dp, // rounded-tr-xs
                        bottomStart = MaridRadius.Xl.dp,
                        bottomEnd = MaridRadius.Xl.dp,
                    ),
                )
                .border(0.5.dp, t.border, RoundedCornerShape(MaridRadius.Xl.dp))
                .background(t.userMessageBubble) // bg-surface (user-message-bubble white 8%)
                .padding(horizontal = 16.dp, vertical = 12.dp), // px-4 py-3
        ) {
            Text(
                text = text,
                fontSize = MaridTextSize.Base.sp,
                color = t.foreground,
                lineHeight = (MaridTextSize.Base * 1.5f).sp,
            )
        }
    }
}

/** ConversationTurnGroup.tsx agent turn: header + work items + text + summary. */
@Composable
private fun AgentTurnGroup(
    turn: AgentTurn,
    onUndoChanges: () -> Unit,
) {
    val t = maridTokens()
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) { // gap-5 → 16dp group spacing
        // AssistantHistoryStatus: "Worked for {duration}"
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    0.5.dp,
                    t.border.copy(alpha = 0.5f),
                    RoundedCornerShape(0.dp),
                )
                .padding(bottom = 8.dp), // border-b pb-2
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = turn.workedForLabel,
                    fontSize = MaridTextSize.Base.sp,
                    color = t.foregroundSubtle, // text-ui-base text-foreground-subtle
                )
                Icon(
                    MaridIcons.ChevronRight,
                    contentDescription = "Collapse turn",
                    tint = t.foregroundSubtlest,
                    modifier = Modifier.size(16.dp),
                )
            }
        }

        // Work items — "flex flex-col gap-1" + rows 12px apart (styles.css:2440)
        if (turn.toolRows.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                turn.toolRows.forEach { row -> ToolRowView(row) }
            }
        }

        // Assistant markdown text — "w-full text-ui-base" leading-[1.75]
        if (turn.agentText.isNotEmpty()) {
            Text(
                text = turn.agentText,
                fontSize = MaridTextSize.Base.sp,
                lineHeight = (MaridTextSize.Base * 1.75f).sp, // leading-[1.75]
                color = t.foreground,
            )
        }

        // Change summary card (MessageChangeSummaryPanel.tsx)
        turn.changeSummary?.let { summary -> ChangeSummaryCard(summary, onUndoChanges) }

        // "Completed (end_turn)" — caption under the turn
        if (!turn.live) {
            Text(
                text = "Completed (end_turn)",
                fontSize = MaridTextSize.Caption.sp,
                color = t.foregroundSubtlest,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** ToolSummaryRow.tsx — icon + kind label + detail + status. */
@Composable
private fun ToolRowView(row: ToolRow) {
    val t = maridTokens()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = toolIcon(row.kind),
            contentDescription = row.kind.label,
            tint = t.foregroundSubtle, // monochrome design — all icons foreground-subtle
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = toolKindLabel(row),
            fontSize = MaridTextSize.Base.sp,
            color = t.foregroundSubtle, // kind label text-foreground-subtle
            fontWeight = FontWeight.Normal,
        )
        if (row.isCommand) {
            Text(
                text = row.detail,
                style = monoStyle.copy(fontSize = MaridTextSize.Base.sp), // font-mono for commands
                color = t.foreground,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
        } else {
            Text(
                text = row.detail,
                fontSize = MaridTextSize.Base.sp,
                color = t.foreground, // file chip text-foreground
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
        }
        if (row.status == ToolStatus.COMPLETED) {
            Icon(
                MaridIcons.Check,
                contentDescription = "Completed",
                tint = t.success,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

private fun toolKindLabel(row: ToolRow): String = when (row.status) {
    ToolStatus.COMPLETED -> when (row.kind) {
        ToolKind.EDIT -> "Edited"
        ToolKind.READ -> "Read"
        ToolKind.WRITE -> "Wrote"
        ToolKind.BASH -> "Ran"
    }
    ToolStatus.RUNNING -> when (row.kind) {
        ToolKind.EDIT -> "Editing"
        ToolKind.READ -> "Reading"
        ToolKind.WRITE -> "Writing"
        ToolKind.BASH -> "Running"
    }
    ToolStatus.FAILED -> "Failed"
}

/** Per-tool icons — ToolCallBlocks renderers (read: Search, edit: Pencil, bash: SquareTerminal). */
private fun toolIcon(kind: ToolKind): ImageVector = when (kind) {
    ToolKind.READ -> MaridIcons.Search
    ToolKind.EDIT -> MaridIcons.Pencil
    ToolKind.WRITE -> MaridIcons.Pencil
    ToolKind.BASH -> MaridIcons.SquareTerminal
}

/** MessageChangeSummaryPanel.tsx:103-164 — "1 file changed +1 -0" + Undo. */
@Composable
private fun ChangeSummaryCard(summary: ChangeSummary, onUndoChanges: () -> Unit) {
    val t = maridTokens()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MaridRadius.Xl.dp)) // rounded-xl
            .border(0.5.dp, t.border, RoundedCornerShape(MaridRadius.Xl.dp))
            .background(t.card), // bg-card #2b2b2b
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp) // h-10
                .padding(horizontal = 8.dp), // px-2
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                MaridIcons.ChevronRight,
                contentDescription = "Expand changed files", // chat.changeSummary.expand
                tint = t.foregroundSubtlest,
                modifier = Modifier.size(14.dp),
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (summary.files == 1) "$summary.files file changed" else "${summary.files} files changed",
                fontSize = MaridTextSize.Base.sp,
                fontWeight = FontWeight.Medium, // font-medium
                color = t.foreground,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = "+${summary.added}",
                fontSize = MaridTextSize.Base.sp,
                color = t.diffAdded, // text-diff-added
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "-${summary.removed}",
                fontSize = MaridTextSize.Base.sp,
                color = t.diffRemoved, // text-diff-removed
            )
            Spacer(modifier = Modifier.width(8.dp))
            // Undo button: "h-7 gap-1.5 px-2 text-foreground-subtle" + Undo2 size-3.5
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(MaridRadius.Lg.dp))
                    .clickable(enabled = summary.undonable) { onUndoChanges() }
                    .padding(horizontal = 8.dp)
                    .semantics { contentDescription = "Undo" },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "Undo",
                    fontSize = MaridTextSize.Base.sp,
                    color = t.foregroundSubtle,
                )
                Icon(
                    MaridIcons.Undo2,
                    contentDescription = null,
                    tint = t.foregroundSubtle,
                    modifier = Modifier.size(14.dp),
                )
            }
        }
    }
}

/** ComposerMode import kept for API stability with composer previews. */
private val composerModeDefault = ComposerMode.ASK_BEFORE_CHANGES
