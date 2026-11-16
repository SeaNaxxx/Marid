package com.marid.app.ui
// marid-sync-1

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marid.app.icons.MaridIcons

/**
 * 1:1 Compose port of the ZCode desktop workspace header (dark theme tokens from
 * [MaridTokens]; Tailwind spacing 1 unit = 4dp; shadcn radius: rounded-md 6dp,
 * rounded-lg 8dp, rounded-2xl 16dp).
 *
 * React files ported:
 *   - packages/ui/src/WorkspaceHeader.tsx
 *     (header shell: h-12 = 48dp, border-b border-border/50, p-2 = 8dp,
 *      justify-between gap-2, variant "task" | "draft")
 *   - packages/ui/src/WorkspaceHeaderSections.tsx (WorkspaceHeaderTitleSection:
 *     workspace/context ghost button + h1 title text-ui-base font-semibold
 *     text-foreground min-w-12 max-w-100 truncate + Ellipsis more button
 *     + reload LoaderCircle size-4 animate-spin)
 *   - packages/ui/src/WorkspaceHeaderSections/WorkspaceHeaderActionSection.tsx
 *     (action row gap-0.5 = 2dp)
 *   - packages/ui/src/WorkspaceHeaderSections/WorkspaceContextPath.tsx
 *   - packages/ui/src/WorkspaceTerminalToggleButton.tsx
 *     (ghost icon-md, SquareTerminal size-4, isTerminalOpen -> !bg-selected)
 *   - packages/ui/src/WorkspaceSidePaneToggleButton.tsx
 *     (ghost icon-md, PanelRightOpen/PanelRightClose size-4, !bg-selected when open)
 *   - packages/ui/src/WorkspaceHelpMenuButton.tsx (ghost icon-md, CircleHelp size-4)
 *
 * Extracted exact values:
 *   height h-12 = 48dp; padding p-2 = 8dp; title/action gap-2 = 8dp; action gap-0.5 = 2dp
 *   ghost icon-md = size-7 28dp, rounded-lg 8dp, icon size-4 16dp, text-foreground,
 *     hover:bg-hover (dark: --color-hover = surface-hover 10% white = MaridTokens.SurfaceHover)
 *   active toggle: bg-selected (10% white, same value as hover token)
 *   remote context trigger: variant ghost size sm (h-6 24dp) + gap-1.5 px-1.5
 *     text-foreground-subtle max-w-40; device label text-ui-sm 12sp
 *   title h1: text-ui-base 14sp, font-semibold, text-foreground, min-w-12 48dp,
 *     max-w-100 400dp, truncate
 *   border-bottom: border-border/50 -> MaridTokens.Border at 50% alpha (draft: transparent)
 *   reload spinner: size-7 28dp circle, LoaderCircle size-4 16dp animate-spin
 *     (1s linear infinite), text-foreground-subtle
 *
 * Deviations (mobile adaptation, everything else is value-exact):
 *   - Menu (hamburger) trigger added as the first control: the desktop header sits
 *     next to the pinned sidebar; on phone the sidebar is a drawer and needs its own
 *     trigger. Styled with the exact ghost icon-md token set.
 *   - WorkspaceEditorButtonGroup and ConversationShareMenu are intentionally not
 *     rendered: in the React source both are conditional (editors list is empty and
 *     `isDesktop` is false on phone), so they would not appear.
 *   - Dropdown/popover menu contents (help menu, task more menu) are out of scope of
 *     this file; their triggers fire the provided callbacks.
 */
@Composable
fun MaridHeaderBar(
    title: String?,
    onMenu: () -> Unit,
    onOpenHelp: () -> Unit,
    modifier: Modifier = Modifier,
    isDraft: Boolean = false,
    workspaceDeviceName: String? = null,
    workspaceStatus: MaridRemoteStatus = MaridRemoteStatus.Online,
    onOpenWorkspaceContext: (() -> Unit)? = null,
    onOpenTaskMenu: (() -> Unit)? = null,
    reloadPending: Boolean = false,
    readOnly: Boolean = false,
    terminalOpen: Boolean = false,
    onToggleTerminal: (() -> Unit)? = null,
    sidePaneOpen: Boolean = false,
    onToggleSidePane: (() -> Unit)? = null,
) {
    val bottomBorderColor =
        if (isDraft) Color.Transparent else MaridTokens.Border.copy(alpha = 0.5f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(MaridTokens.Header)
            .drawBehind {
                // border-b border-border/50 (1px)
                if (bottomBorderColor != Color.Transparent) {
                    drawRect(
                        color = bottomBorderColor,
                        topLeft = Offset(0f, size.height - 1.dp.toPx()),
                        size = Size(size.width, 1.dp.toPx()),
                    )
                }
            }
            .clipToBounds(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 8.dp), // p-2
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween, // gap-2, justify-between
        ) {
            Row(
                modifier = Modifier
                    .weight(1f, fill = false) // min-w-0 flex-1 -> sized by content, capped
                    .clipToBounds(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp), // title section gap-2
            ) {
                // Mobile drawer trigger (documented deviation; exact ghost icon-md styling).
                MaridHeaderGhostIconButton(
                    icon = MaridIcons.Menu,
                    contentDescription = "Menu",
                    onClick = onMenu,
                )
                if (!isDraft && onOpenWorkspaceContext != null) {
                    MaridWorkspaceContextButton(
                        deviceName = workspaceDeviceName,
                        status = workspaceStatus,
                        onClick = onOpenWorkspaceContext,
                    )
                }
                if (!isDraft && title != null) {
                    // h1: flex min-w-12 max-w-100 shrink items-center gap-2 truncate
                    //     font-semibold text-foreground text-ui-base
                    Text(
                        text = title,
                        color = MaridTokens.Foreground,
                        fontSize = 14.sp, // text-ui-base
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .widthIn(min = 48.dp, max = 400.dp) // min-w-12 max-w-100
                            .weight(1f, fill = false),
                    )
                }
                if (!isDraft && onOpenTaskMenu != null) {
                    MaridHeaderGhostIconButton(
                        icon = MaridIcons.Ellipsis,
                        contentDescription = "More",
                        onClick = onOpenTaskMenu,
                    )
                }
                if (!isDraft && reloadPending) {
                    // reload spinner: inline-flex size-7 rounded-full text-foreground-subtle
                    Box(
                        modifier = Modifier.size(28.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        MaridSpinningLoader(
                            size = 16.dp, // size-4
                            tint = MaridTokens.ForegroundSubtle,
                        )
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp), // action section gap-0.5
            ) {
                if (onToggleTerminal != null) {
                    MaridHeaderGhostIconButton(
                        icon = MaridIcons.SquareTerminal,
                        contentDescription = "Toggle terminal",
                        onClick = onToggleTerminal,
                        active = terminalOpen, // !bg-selected
                        enabled = !readOnly, // disabled={Boolean(disabledReason)} + disabled:opacity-50
                    )
                }
                MaridHeaderGhostIconButton(
                    icon = MaridIcons.CircleHelp,
                    contentDescription = "Help",
                    onClick = onOpenHelp,
                )
                if (onToggleSidePane != null) {
                    MaridHeaderGhostIconButton(
                        icon = if (sidePaneOpen) MaridIcons.PanelRightClose else MaridIcons.PanelRightOpen,
                        contentDescription = "Toggle panel",
                        onClick = onToggleSidePane,
                        active = sidePaneOpen, // !bg-selected
                    )
                }
            }
        }
    }
}

/**
 * Ghost `icon-md` button: size-7 (28dp), rounded-lg (8dp), icon size-4 (16dp),
 * text-foreground, hover:bg-hover (dark = surface-hover 10% white).
 */
@Composable
private fun MaridHeaderGhostIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    boxSize: Dp = 28.dp,
    iconSize: Dp = 16.dp,
    cornerRadius: Dp = 8.dp,
    iconTint: Color = MaridTokens.Foreground,
    active: Boolean = false,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val background = if (active || pressed) MaridTokens.SurfaceHover else Color.Transparent
    Box(
        modifier = modifier
            .size(boxSize)
            .alpha(if (enabled) 1f else 0.5f) // disabled:opacity-50
            .clip(RoundedCornerShape(cornerRadius))
            .background(background)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled && onClick != null,
                onClickLabel = contentDescription,
            ) { onClick?.invoke() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize),
            tint = iconTint,
        )
    }
}

/**
 * Workspace context trigger. Remote variant: Button ghost size sm (h-6 24dp) +
 * "min-w-0 max-w-40 gap-1.5 px-1.5 text-foreground-subtle" with the project
 * connection state icon (size-4) and a text-ui-sm (12sp) device label.
 * Local variant: ghost icon-md with Folder size-4 text-foreground-subtle.
 */
@Composable
private fun MaridWorkspaceContextButton(
    deviceName: String?,
    status: MaridRemoteStatus,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    if (deviceName != null) {
        Row(
            modifier = Modifier
                .height(24.dp) // size sm -> h-6
                .widthIn(max = 160.dp) // max-w-40
                .clip(RoundedCornerShape(6.dp)) // rounded-md (button base)
                .background(if (pressed) MaridTokens.SurfaceHover else Color.Transparent)
                .clickable(interactionSource = interaction, indication = null) { onClick() }
                .padding(horizontal = 6.dp), // px-1.5
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp), // gap-1.5
        ) {
            MaridRemoteStateIcon(status = status, project = true)
            Text(
                text = deviceName,
                color = MaridTokens.ForegroundSubtle,
                fontSize = 12.sp, // span text-ui-sm
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
    } else {
        MaridHeaderGhostIconButton(
            icon = MaridIcons.Folder,
            contentDescription = "Workspace",
            onClick = onClick,
            iconTint = MaridTokens.ForegroundSubtle, // Folder size-4 text-foreground-subtle
        )
    }
}

/**
 * animate-spin: LoaderCircle rotating 1s linear infinite, shared by the header
 * reload spinner, project rows and task rows.
 */
@Composable
internal fun MaridSpinningLoader(
    size: Dp,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "marid-spin")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(durationMillis = 1000, easing = LinearEasing)),
        label = "marid-spin-angle",
    )
    Icon(
        imageVector = MaridIcons.LoaderCircle,
        contentDescription = null,
        modifier = modifier.size(size).rotate(angle),
        tint = tint,
    )
}
