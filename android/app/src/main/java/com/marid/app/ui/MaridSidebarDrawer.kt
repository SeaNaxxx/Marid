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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.marid.app.state.MaridUiState
import com.marid.app.state.TaskEntry
import com.marid.app.theme.MaridRadius
import com.marid.app.theme.MaridTextSize
import com.marid.app.theme.maridTokens

/**
 * Sidebar — 1:1 port of `project-sidebar/index.tsx` + ProjectSidebarNavigation
 * + ProjectSidebarListHeader + WorkspaceSidebarFooter.
 *
 * Mobile adaptation: rendered as ModalNavigationDrawer content (264dp — the
 * desktop `WORKSPACE_SIDEBAR_DEFAULT_WIDTH_PX = 264`).
 */
@Composable
fun MaridSidebarDrawer(
    state: MaridUiState,
    onNewTask: () -> Unit,
    onAutomations: () -> Unit,
    onPluginMarketplace: () -> Unit,
    onConnect: () -> Unit,
    onSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val t = maridTokens()
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(t.sidebar) // bg-sidebar #161616
            .verticalScroll(rememberScrollState()),
    ) {
        Column(
            // "flex flex-col gap-1 px-2 py-3" (ProjectSidebarNavigation.tsx:34)
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            SidebarNavItem(
                icon = MaridIcons.MessageCirclePlus, // MessageCirclePlus h-4 w-4
                label = "New task", // taskList.newThread
                onClick = onNewTask,
            )
            SidebarNavItem(
                icon = MaridIcons.CalendarClock, // CalendarClock size-4
                label = "Automations", // workspace.openScheduledSettings
                onClick = onAutomations,
            )
            SidebarNavItem(
                icon = MaridIcons.Blocks, // Blocks size-4
                label = "Plugin Marketplace", // workspace.openPluginsSettings
                onClick = onPluginMarketplace,
            )
        }

        // "flex min-h-0 flex-1 flex-col border-t border-border/50 pt-3"
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .border(0.5.dp, t.border.copy(alpha = 0.5f), RoundedCornerShape(0.dp))
                .padding(top = 12.dp),
        ) {
            // ProjectSidebarListHeader: "flex items-center gap-1 px-2 pb-2"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Task list", // taskList.title
                    fontSize = MaridTextSize.Base.sp,
                    fontWeight = FontWeight.Medium,
                    color = t.foregroundSubtle,
                    modifier = Modifier
                        .padding(horizontal = 10.dp)
                        .weight(1f),
                )
                HeaderIconButton(MaridIcons.Search, "Search tasks", iconColor = t.foregroundSubtle)
                HeaderIconButton(MaridIcons.ListFilter, "Filter tasks", iconColor = t.foregroundSubtle)
            }

            // PurposeSections grid — Projects section
            SectionHeader(title = "Projects")
            TaskRow(entry = TaskEntry(title = "ZCODE-ANDROID · WORKSPACE", status = "PROJECT"))

            // Tasks section — active task rows
            SectionHeader(title = "Tasks")
            state.tasks.forEach { task ->
                TaskRow(entry = task)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // WorkspaceSidebarFooter — avatar + Connect + Settings
        SidebarFooter(
            connectionLabel = if (state.engineHealthy) "Connected" else "Connect",
            connected = state.engineHealthy,
            onConnect = onConnect,
            onSettings = onSettings,
        )
    }
}

@Composable
private fun SidebarNavItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    val t = maridTokens()
    Row(
        // "group w-full h-8 rounded-lg … pl-2.5 pr-2.5 hover:bg-surface-hover"
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
            .clip(RoundedCornerShape(MaridRadius.Lg.dp))
            .clickable { onClick() }
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, contentDescription = null, tint = t.foreground, modifier = Modifier.size(16.dp))
        Text(
            text = label,
            fontSize = MaridTextSize.Base.sp,
            color = t.foreground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SectionHeader(title: String) {
    val t = maridTokens()
    Row(
        // "ml-2 mr-1 flex h-7 items-center" + "px-2.5" trigger
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 4.dp)
            .height(28.dp)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = title,
            fontSize = MaridTextSize.Base.sp,
            fontWeight = FontWeight.Medium,
            color = t.foregroundSubtlest, // text-foreground-subtlest
            modifier = Modifier.weight(1f),
        )
        Icon(
            MaridIcons.ChevronDown,
            contentDescription = "Collapse $title",
            tint = t.foregroundSubtlest,
            modifier = Modifier.size(14.dp),
        )
        Icon(
            MaridIcons.Plus,
            contentDescription = "New $title entry",
            tint = t.foregroundSubtlest,
            modifier = Modifier.size(14.dp),
        )
    }
}

@Composable
private fun TaskRow(entry: TaskEntry) {
    val t = maridTokens()
    Row(
        // "group/task-item flex gap-2 rounded-lg pl-2.5 pr-1 py-1"
        // active: "bg-selected"; hover: "hover:bg-surface-hover"
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 4.dp)
            .clip(RoundedCornerShape(MaridRadius.Lg.dp))
            .background(if (entry.active) t.selected else Color.Transparent)
            .clickable { }
            .padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        // Leading indicator dot: unread bg-icon-blue size 7px / idle gray
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(
                    when (entry.status) {
                        "RUNNING" -> MaridColorWarnings() // warning #ff8a30
                        "COMPLETED" -> t.success
                        else -> t.foregroundSubtlest
                    },
                ),
        )
        Text(
            text = entry.title,
            fontSize = MaridTextSize.Base.sp,
            color = t.foreground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (entry.status != "PROJECT") {
            Text(
                text = entry.status,
                fontSize = MaridTextSize.Sm.sp,
                fontWeight = FontWeight.Medium,
                color = t.foregroundSubtle,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(t.surface)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
private fun MaridColorWarnings(): Color {
    val t = maridTokens()
    return t.warning
}

@Composable
private fun SidebarFooter(
    connectionLabel: String,
    connected: Boolean,
    onConnect: () -> Unit,
    onSettings: () -> Unit,
) {
    val t = maridTokens()
    Column(
        // "flex shrink-0 flex-col gap-2.5 px-4 pt-2 pb-4"
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            // Avatar: size-8 rounded-full bg-background
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(t.background)
                    .border(0.5.dp, t.border, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    MaridIcons.User,
                    contentDescription = null,
                    tint = t.foreground,
                    modifier = Modifier.size(16.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = connectionLabel,
                    fontSize = MaridTextSize.Base.sp,
                    fontWeight = FontWeight.SemiBold, // font-semibold
                    color = t.foreground,
                )
                if (connected) {
                    Text(
                        text = "LOCAL RUNTIME",
                        fontSize = MaridTextSize.Xs.sp,
                        color = t.foregroundSubtlest,
                    )
                }
            }
            HeaderIconButton(
                icon = MaridIcons.Smartphone,
                contentDescription = "Remote control",
                iconColor = if (connected) t.success else t.foregroundSubtle,
            )
            HeaderIconButton(
                icon = MaridIcons.Settings,
                contentDescription = "Settings",
                onClick = onSettings,
            )
        }
        // Connect button (TID_LOGIN_MENU_ITEM "Connect" when logged out)
        if (!connected) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(MaridRadius.Lg.dp))
                    .background(t.surface)
                    .clickable { onConnect() }
                    .padding(vertical = 8.dp, horizontal = 10.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        MaridIcons.Plug,
                        contentDescription = null, // project.connect uses Plug icon
                        tint = t.foreground,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(text = "Connect", fontSize = MaridTextSize.Base.sp, color = t.foreground)
                }
            }
        }
    }
}

@Composable
private fun HeaderIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: (() -> Unit)? = null,
    iconColor: Color? = null,
) {
    val t = maridTokens()
    Box(
        modifier = Modifier
            .size(24.dp) // size="icon-sm" → size-6
            .clip(RoundedCornerShape(MaridRadius.Lg.dp))
            .semantics { this.contentDescription = contentDescription }
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor ?: t.foregroundSubtle, // "text-foreground-subtle hover:text-foreground"
            modifier = Modifier.size(14.dp), // size-3.5
        )
    }
}
