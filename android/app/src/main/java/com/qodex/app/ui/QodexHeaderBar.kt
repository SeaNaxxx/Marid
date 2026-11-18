package com.qodex.app.ui

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
import com.qodex.app.icons.QodexIcons

@Composable
fun QodexHeaderBar(
    title: String?,
    onMenu: () -> Unit,
    // A dead help button is worse than none: the entry renders only when a
    // handler exists.
    onOpenHelp: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    isDraft: Boolean = false,
    workspaceDeviceName: String? = null,
    workspaceStatus: QodexRemoteStatus = QodexRemoteStatus.Online,
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
        if (isDraft) Color.Transparent else QodexTokens.Border.copy(alpha = 0.5f)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(QodexTokens.Header)
            .drawBehind {
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
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .clipToBounds(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // Mobile drawer trigger — on phone the sidebar is a drawer, so the header carries it.
                QodexHeaderGhostIconButton(
                    icon = QodexIcons.Menu,
                    contentDescription = "Menu",
                    onClick = onMenu,
                )
                if (!isDraft && onOpenWorkspaceContext != null) {
                    QodexWorkspaceContextButton(
                        deviceName = workspaceDeviceName,
                        status = workspaceStatus,
                        onClick = onOpenWorkspaceContext,
                    )
                }
                if (!isDraft && title != null) {
                    Text(
                        text = title,
                        color = QodexTokens.Foreground,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .widthIn(min = 48.dp, max = 400.dp)
                            .weight(1f, fill = false),
                    )
                }
                if (!isDraft && onOpenTaskMenu != null) {
                    QodexHeaderGhostIconButton(
                        icon = QodexIcons.Ellipsis,
                        contentDescription = "More",
                        onClick = onOpenTaskMenu,
                    )
                }
                if (!isDraft && reloadPending) {
                    Box(
                        modifier = Modifier.size(28.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        QodexSpinningLoader(
                            size = 16.dp,
                            tint = QodexTokens.ForegroundSubtle,
                        )
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                if (onToggleTerminal != null) {
                    QodexHeaderGhostIconButton(
                        icon = QodexIcons.SquareTerminal,
                        contentDescription = "Toggle terminal",
                        onClick = onToggleTerminal,
                        active = terminalOpen,
                        enabled = !readOnly,
                    )
                }
                if (onOpenHelp != null) {
                    QodexHeaderGhostIconButton(
                        icon = QodexIcons.CircleHelp,
                        contentDescription = "Help",
                        onClick = onOpenHelp,
                    )
                }
                if (onToggleSidePane != null) {
                    QodexHeaderGhostIconButton(
                        icon = if (sidePaneOpen) QodexIcons.PanelRightClose else QodexIcons.PanelRightOpen,
                        contentDescription = "Toggle panel",
                        onClick = onToggleSidePane,
                        active = sidePaneOpen,
                    )
                }
            }
        }
    }
}

@Composable
private fun QodexHeaderGhostIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    boxSize: Dp = 28.dp,
    iconSize: Dp = 16.dp,
    cornerRadius: Dp = 8.dp,
    iconTint: Color = QodexTokens.Foreground,
    active: Boolean = false,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val background = if (active || pressed) QodexTokens.SurfaceHover else Color.Transparent
    Box(
        modifier = modifier
            .size(boxSize)
            .alpha(if (enabled) 1f else 0.5f)
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

@Composable
private fun QodexWorkspaceContextButton(
    deviceName: String?,
    status: QodexRemoteStatus,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    if (deviceName != null) {
        Row(
            modifier = Modifier
                .height(24.dp)
                .widthIn(max = 160.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (pressed) QodexTokens.SurfaceHover else Color.Transparent)
                .clickable(interactionSource = interaction, indication = null) { onClick() }
                .padding(horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            QodexRemoteStateIcon(status = status, project = true)
            Text(
                text = deviceName,
                color = QodexTokens.ForegroundSubtle,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
    } else {
        QodexHeaderGhostIconButton(
            icon = QodexIcons.Folder,
            contentDescription = "Workspace",
            onClick = onClick,
            iconTint = QodexTokens.ForegroundSubtle,
        )
    }
}

@Composable
internal fun QodexSpinningLoader(
    size: Dp,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "qodex-spin")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(animation = tween(durationMillis = 1000, easing = LinearEasing)),
        label = "qodex-spin-angle",
    )
    Icon(
        imageVector = QodexIcons.LoaderCircle,
        contentDescription = null,
        modifier = modifier.size(size).rotate(angle),
        tint = tint,
    )
}
