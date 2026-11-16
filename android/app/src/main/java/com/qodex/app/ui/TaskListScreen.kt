package com.qodex.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.qodex.app.icons.QodexIcons

public enum class TaskRowStatus2e {
    RUNNING,
    COMPLETED,
    ERROR,
}

/** updatedAt is epoch millis. */
public data class TaskRow2e(
    val id: String,
    val title: String,
    val status: TaskRowStatus2e,
    val updatedAt: Long,
    val isActive: Boolean = false,
)

public fun taskRowStatusAccentColor2e(status: TaskRowStatus2e): Color = when (status) {
    TaskRowStatus2e.RUNNING -> QodexTokens.ForegroundSubtle
    TaskRowStatus2e.COMPLETED -> QodexTokens.Violet400
    TaskRowStatus2e.ERROR -> QodexTokens.Destructive
}

public fun formatTaskRelativeTime2e(updatedAt: Long, nowMs: Long): String {
    val minutes = (nowMs - updatedAt) / 60_000L
    if (minutes < 1) return "now"
    if (minutes < 60) return "${minutes}m"
    val hours = minutes / 60
    if (hours < 24) return "${hours}h"
    return "${hours / 24}d"
}

@Composable
public fun TaskListScreen(
    tasks: List<TaskRow2e>,
    onOpenTask: (TaskRow2e) -> Unit,
    onNewTask: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(QodexTokens.Sidebar),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (onNewTask != null) {
            NewTaskButton2e(onNewTask = onNewTask)
        }

        if (tasks.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 34.dp, vertical = 8.dp)) {
                Text(
                    text = "No tasks yet",
                    color = QodexTokens.ForegroundSubtlest,
                    fontSize = 14.sp,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth().weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                items(items = tasks, key = { it.id }) { row ->
                    TaskListItemRow2e(row = row, onOpenTask = onOpenTask)
                }
            }
        }
    }
}

@Composable
private fun NewTaskButton2e(onNewTask: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            val interactionSource = remember { MutableInteractionSource() }
            val pressed by interactionSource.collectIsPressedAsState()
            // Touch has no hover; pressed replaces hover:bg-surface-hover.
            val bg by animateColorAsState(
                targetValue = if (pressed) QodexTokens.SurfaceHover else Color.Transparent,
                animationSpec = tween(durationMillis = 150),
                label = "newTaskBg",
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(bg)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null, // pressed bg handled above (no Material ripple)
                        onClick = onNewTask,
                    )
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = QodexIcons.MessageCirclePlus,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = QodexTokens.Foreground,
                )
                Text(
                    text = "New task",
                    color = QodexTokens.Foreground,
                    fontSize = 14.sp,
                )
            }
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(QodexTokens.Border))
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TaskListItemRow2e(row: TaskRow2e, onOpenTask: (TaskRow2e) -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    val nowMs = remember { System.currentTimeMillis() }
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val bg by animateColorAsState(
        targetValue = when {
            row.isActive -> QodexTokens.Selected
            pressed -> QodexTokens.SurfaceHover
            else -> Color.Transparent
        },
        animationSpec = tween(durationMillis = 150),
        label = "taskRowBg",
    )

    Box { // popup anchor
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(bg)
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null, // pressed bg handled via animateColorAsState (no ripple)
                    onClick = { onOpenTask(row) },
                    onLongClick = { menuOpen = true },
                )
                .padding(start = 10.dp, end = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(modifier = Modifier.size(16.dp), contentAlignment = Alignment.Center) {
                LeadingIndicator2e(status = row.status)
            }
            TaskTitleText2e(title = row.title, modifier = Modifier.weight(1f))
            Text(
                text = formatTaskRelativeTime2e(updatedAt = row.updatedAt, nowMs = nowMs),
                modifier = Modifier.padding(end = 2.dp),
                color = QodexTokens.ForegroundSubtle,
                fontSize = 12.sp,
            )
        }
        if (menuOpen) {
            TaskRowContextMenu2e(row = row, onDismiss = { menuOpen = false })
        }
    }
}

@Composable
private fun LeadingIndicator2e(status: TaskRowStatus2e) {
    when (status) {
        TaskRowStatus2e.ERROR -> Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(taskRowStatusAccentColor2e(status)),
        )

        TaskRowStatus2e.COMPLETED -> Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(taskRowStatusAccentColor2e(status)),
        )

        TaskRowStatus2e.RUNNING -> {
            val spin = rememberInfiniteTransition(label = "taskRowSpin")
            val angle by spin.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1000, easing = LinearEasing),
                ),
                label = "taskRowSpinAngle",
            )
            Icon(
                imageVector = QodexIcons.LoaderCircle,
                contentDescription = null,
                modifier = Modifier
                    .size(16.dp)
                    .graphicsLayer { rotationZ = angle },
                tint = taskRowStatusAccentColor2e(status),
            )
        }
    }
}

/** Right-edge fade mask applied only while overflowing; ellipsis intentionally not used. */
@Composable
private fun TaskTitleText2e(title: String, modifier: Modifier = Modifier) {
    var overflowing by remember(title) { mutableStateOf(false) }
    Box(modifier = modifier.then(if (overflowing) Modifier.taskTitleFadeMask2e() else Modifier)) {
        Text(
            text = title,
            modifier = Modifier.fillMaxWidth(),
            color = QodexTokens.Foreground,
            fontSize = 14.sp,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            onTextLayout = { overflowing = it.hasVisualOverflow },
        )
    }
}

private fun Modifier.taskTitleFadeMask2e(): Modifier =
    this.graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
        .drawWithContent {
            drawContent()
            val fadePx = 24.dp.toPx()
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color.Black, Color.Transparent),
                    startX = size.width - fadePx,
                    endX = size.width,
                ),
                topLeft = Offset(size.width - fadePx, 0f),
                size = Size(fadePx, size.height),
                blendMode = BlendMode.DstIn,
            )
        }

/** Long-press context menu; items are disabled when no Android capability backs them. */
@Composable
private fun TaskRowContextMenu2e(row: TaskRow2e, onDismiss: () -> Unit) {
    val clipboard = LocalClipboardManager.current
    Popup(
        popupPositionProvider = TaskMenuPopupPosition2e(),
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        Column(
            modifier = Modifier
                .width(208.dp)
                .shadow(elevation = 4.dp, shape = RoundedCornerShape(8.dp), clip = false)
                .clip(RoundedCornerShape(8.dp))
                .background(QodexTokens.Menu)
                .border(1.dp, QodexTokens.Border, RoundedCornerShape(8.dp))
                .padding(vertical = 4.dp), // horizontal padding lives on items so separators run full-bleed
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            ContextMenuItem2e("Pin task", enabled = false)
            ContextMenuItem2e("Rename task", enabled = false)
            ContextMenuItem2e("Archive task", enabled = false)
            ContextMenuItem2e("Mark as unread", enabled = false)
            MenuSeparator2e()
            ContextMenuItem2e("Open in File Manager", enabled = false)
            ContextMenuItem2e("Copy path", enabled = false)
            ContextMenuItem2e("Copy task path", enabled = false)
            ContextMenuItem2e("Copy log path", enabled = false)
            ContextMenuItem2e("Copy session ID", enabled = true) {
                clipboard.setText(AnnotatedString(row.id))
                onDismiss()
            }
            ContextMenuItem2e("Go to config", enabled = false)
            MenuSeparator2e()
            ContextMenuItem2e("View model trajectory", enabled = false)
            MenuSeparator2e()
            ContextMenuItem2e("Report issue", enabled = false)
        }
    }
}

@Composable
private fun ContextMenuItem2e(label: String, enabled: Boolean, onClick: (() -> Unit)? = null) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Box(modifier = Modifier.padding(horizontal = 4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 28.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(if (enabled && pressed) QodexTokens.MenuHover else Color.Transparent)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null, // pressed bg handled above (no Material ripple)
                    enabled = enabled,
                    onClick = { onClick?.invoke() },
                )
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                color = if (enabled) QodexTokens.Foreground else QodexTokens.ForegroundSubtlest,
                fontSize = 14.sp,
                lineHeight = 22.75f.sp,
            )
        }
    }
}

@Composable
private fun MenuSeparator2e() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .height(1.dp)
            .background(QodexTokens.Border),
    )
}

private class TaskMenuPopupPosition2e : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val margin = 8
        var x = anchorBounds.left
        var y = anchorBounds.bottom + 4
        if (x + popupContentSize.width > windowSize.width - margin) {
            x = windowSize.width - margin - popupContentSize.width
        }
        if (y + popupContentSize.height > windowSize.height - margin) {
            y = anchorBounds.top - popupContentSize.height - 4
        }
        val maxX = (windowSize.width - popupContentSize.width).coerceAtLeast(0)
        val maxY = (windowSize.height - popupContentSize.height).coerceAtLeast(0)
        return IntOffset(x.coerceIn(0, maxX), y.coerceIn(0, maxY))
    }
}
