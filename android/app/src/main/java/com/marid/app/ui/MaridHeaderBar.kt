package com.marid.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marid.app.icons.MaridIcons
import com.marid.app.theme.MaridRadius
import com.marid.app.theme.MaridTextSize
import com.marid.app.theme.maridTokens

/**
 * Workspace header — 1:1 port of `WorkspaceHeader.tsx`:
 * h-12 (48px), border-b border-border/50, p-2; left = title section,
 * right = action section (Help, Terminal toggle, Side pane toggle).
 *
 * Mobile adaptation (layout-only): the always-visible desktop sidebar becomes
 * a drawer, so a Menu button leads the left section — everything else matches
 * the desktop header exactly.
 */
@Composable
fun MaridHeaderBar(
    taskTitle: String,
    terminalOpen: Boolean,
    sidePaneOpen: Boolean,
    onMenuClick: () -> Unit,
    onHelpClick: () -> Unit,
    onTerminalToggle: () -> Unit,
    onSidePaneToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val t = maridTokens()
    // header h-12 = 48dp; inner padding p-2 = 8dp; border-b border-border/50
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(t.header)
            .border(0.5.dp, t.border.copy(alpha = 0.5f), RoundedCornerShape(0.dp))
            .padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // LEFT — WorkspaceHeaderTitleSection: [Menu(mobile)] [Folder] [Title] [Ellipsis]
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f),
        ) {
            HeaderIconButton(
                icon = MaridIcons.Menu,
                contentDescription = "Open sidebar",
                onClick = onMenuClick,
            )
            HeaderIconButton(
                icon = MaridIcons.Folder,
                contentDescription = "Workspace path",
                iconColor = t.foregroundSubtle,
            )
            Text(
                text = taskTitle,
                fontSize = MaridTextSize.Base.sp,
                fontWeight = FontWeight.SemiBold, // font-semibold text-foreground
                color = t.foreground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            HeaderIconButton(
                icon = MaridIcons.Ellipsis,
                contentDescription = "Task menu",
            )
        }

        // RIGHT — WorkspaceHeaderActionSection: [Help] [Terminal] [Split]
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp), // gap-0.5
        ) {
            HeaderIconButton(
                icon = MaridIcons.CircleHelp,
                contentDescription = "Help",
                onClick = onHelpClick,
            )
            HeaderIconButton(
                icon = MaridIcons.SquareTerminal,
                contentDescription = "Toggle terminal", // terminal.toggle
                selected = terminalOpen, // isTerminalOpen && "!bg-selected"
                onClick = onTerminalToggle,
            )
            HeaderIconButton(
                icon = if (sidePaneOpen) MaridIcons.PanelRightClose else MaridIcons.PanelRightOpen,
                contentDescription = "Toggle panel", // sidePane.togglePanel
                selected = sidePaneOpen,
                onClick = onSidePaneToggle,
            )
        }
    }
}

/**
 * Ghost icon button — `variant="ghost" size="icon-md"`:
 * size-7 (28px hover face in desktop rows; we use a 32dp touch target per
 * Android a11y minimum while keeping the 16dp icon), rounded-lg, hover
 * bg-hover, active pane bg-selected.
 */
@Composable
private fun HeaderIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: (() -> Unit)? = null,
    iconColor: Color? = null,
    selected: Boolean = false,
) {
    val t = maridTokens()
    val bg = when {
        selected -> t.selected // "!bg-selected text-foreground"
        else -> Color.Transparent
    }
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(MaridRadius.Lg.dp))
            .background(bg)
            .semantics { this.contentDescription = contentDescription }
            .then(
                if (onClick != null) {
                    Modifier.clickable { onClick() }
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor ?: t.foreground,
            modifier = Modifier.size(16.dp), // size-4 icons in the header
        )
    }
}
