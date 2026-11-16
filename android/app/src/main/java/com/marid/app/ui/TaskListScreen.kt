package com.marid.app.ui

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
import com.marid.app.icons.MaridIcons

/**
 * 1:1 port of the ZCode desktop task list — dark theme only.
 *
 * FILES PORTED
 *   - packages/ui/src/TaskList.tsx                (container: flex flex-col gap-2, create section,
 *                                                  empty state, ul space-y-0.5, show-more, footer)
 *   - packages/ui/src/TaskListItem.tsx            (default-variant row: MemoTaskItem JSX, lines 629-882)
 *   - packages/ui/src/TaskListItemContextMenu.tsx + TaskActionMenuContent.tsx
 *                                                 (long-press menu: exact item list + order)
 *   - packages/ui/src/TaskListLoadingHint.tsx     (reviewed; see deviation 3)
 *   - packages/ui/src/TaskSearchDialog.tsx        (reviewed; see deviation 4)
 *   - packages/ui/src/lib/taskListItemPresentation.ts (deriveTaskLeadingIndicator,
 *                                                  formatTaskRelativeTime)
 *   - packages/ui/src/NewTaskButtonGroup.tsx      (header "New task" row)
 *   - packages/ui/src/i18n/locales/en-US.ts       (exact strings)
 *
 * EXTRACTION NOTES (source class → Compose value; 1 spacing unit = 4dp; radius md=6 lg=8)
 * ── container ────────────────────────────────────────────────────────────────
 *   TaskList "flex flex-col gap-2"                     → Column(spacedBy 8dp) on bg-sidebar
 *                                                      (--color-sidebar = neutral-950; rows are
 *                                                      rounded cards on the dark sidebar per the
 *                                                      Bugfix comment in TaskListItem.tsx).
 *   create section "border-b p-3"                      → padding 12dp + 1dp bottom rule in
 *                                                      MaridTokens.Border (--color-border).
 *   list "ul.space-y-0.5"                              → LazyColumn(spacedBy 2dp).
 *   empty state "px-8.5 py-2 text-ui-base text-foreground-subtlest"
 *                                                      → padding(h 34dp, v 8dp), 14sp,
 *                                                        ForegroundSubtlest; "No tasks yet".
 * ── row (TaskListItem default variant) ───────────────────────────────────────
 *   li "group/task-item flex cursor-pointer gap-2 rounded-lg pl-2.5 pr-1 py-1 items-center"
 *      + "isActive ? bg-selected : hover:bg-surface-hover"
 *                                                      → 32dp total row height (py-1 8dp +
 *                                                        24dp h-6 title row; "默认行 32px"
 *                                                        comment in source), radius 8dp,
 *                                                        padding start 10dp / end 4dp, gap 8dp;
 *                                                        bg-selected = white 10%
 *                                                        (MaridTokens.Selected),
 *                                                        hover → pressed = bg-surface-hover
 *                                                        white 10% (MaridTokens.SurfaceHover),
 *                                                        transition-colors 150ms default.
 *   leading slot "relative flex size-4 items-center justify-center" → 16dp Box.
 *     error   "h-1.5 w-1.5 rounded-full bg-destructive" → 6dp dot, Destructive (red-500).
 *     unread  "size-[7px] rounded-full bg-icon-blue"    → 7dp dot geometry (dark --color-icon-blue
 *                                                         = sky-600) — not reachable from the
 *                                                         mandated status enum (no unread field).
 *     loading "LoaderCircle size-4 text-foreground-subtle" + animate-spin (1s linear infinite)
 *                                                       → 16dp icon, ForegroundSubtle, rotating.
 *     none    "无状态只保留对齐槽" (no dot for terminal statuses).
 *   title container "relative min-w-0 flex h-6 flex-1 ... gap-1.5"; title
 *   "text-ui-base text-foreground" via TaskTitleOverflowText
 *   "[mask-image:linear-gradient(to_right,black_calc(100%_-_1.5rem),transparent)]"
 *                                                       → 14sp Foreground, single line, right
 *                                                         24dp (1.5rem) alpha fade mask
 *                                                         (BlendMode.DstIn) applied only when the
 *                                                         text overflows — NOT ellipsis (the
 *                                                         source explicitly removed ellipsis).
 *   metadata "mr-0.5 flex shrink-0 items-center gap-1 text-ui-sm text-foreground-subtle"
 *                                                       → relative time 12sp ForegroundSubtle,
 *                                                         end margin 2dp; label per
 *                                                         formatTaskRelativeTime:
 *                                                         <1min "now", <60min "{m}m", <24h "{h}h",
 *                                                         else "{d}d" (en-US).
 * ── NewTaskButtonGroup ───────────────────────────────────────────────────────
 *   "w-full h-8 rounded-lg pl-2.5 pr-2.5 hover:bg-surface-hover gap-2" → 32dp row, radius 8dp,
 *      padding-h 10dp, pressed bg SurfaceHover; "MessageCirclePlus h-4 w-4" 16dp; label
 *      "New task" 14sp [taskList.newThread].
 * ── context menu (TaskListItemContextMenuContent) ────────────────────────────
 *   ContextMenuContent "min-w-44 ... w-52 rounded-lg border border-popover-border bg-menu p-1
 *   text-foreground shadow-md flex flex-col gap-0.5"
 *                                                       → 208dp width, radius 8dp, Menu
 *                                                         (neutral-950) bg, 1dp Border outline
 *                                                         (--color-popover-border = --color-border),
 *                                                         4dp padding, 2dp item gap.
 *   ContextMenuItem "min-h-7 rounded-md px-2 py-1 text-ui-base/relaxed data-[highlighted]:
 *   bg-menu-hover data-disabled:text-foreground-subtlest"
 *                                                       → 28dp min height, radius 6dp, padding
 *                                                         8dp/4dp, 14sp (lh 22.75sp), pressed bg
 *                                                         MenuHover (neutral-900), disabled text
 *                                                         ForegroundSubtlest.
 *   ContextMenuSeparator "-mx-1 my-1 h-px bg-border"    → full-bleed 1dp Border rule with 4dp
 *                                                         vertical margins.
 *   Item list + order = TaskActionMenuContent as invoked from TaskListItemContextMenuContent:
 *     Pin task · Rename task · Archive task · Mark as unread | Open in File Manager · Copy path ·
 *     Copy task path · Copy log path · Copy session ID · Go to config | View model trajectory |
 *     Report issue  (en-US labels; "Open in File Manager" = appHeader.openInFileManager, the
 *     label getFileManagerLabel() resolves for non-mac/non-windows platforms).
 *
 * DEVIATIONS (none invented):
 *   1. isActive/selected: the mandated TaskRow2e carries an `isActive: Boolean = false` flag
 *      ported from TaskListItem's isActive prop; when true the row paints bg-selected.
 *   2. Idle/completed accent: the desktop row renders NO dot for terminal statuses; per task
 *      mandate "idle-task accent = violet per tokens" completed rows use the source token
 *      --color-idle-task (dark block: violet-400 = MaridTokens.Violet400) with the source's
 *      7dp dot geometry (the unread-dot "size-[7px]" rule).
 *   3. TaskListLoadingHint is not wired: the mandated TaskListScreen API has no `loading`
 *      flag, so the loading branch (Spinner + "Loading tasks...", gap-2 px-2.5 py-1 14sp
 *      ForegroundSubtlest) can never be truthy; no dead UI is rendered.
 *   4. TaskSearchDialog is a self-contained command palette (own trigger row, Cmd dialog,
 *      shortcut pills). It is not mounted inside TaskList, and the mandated API exposes no
 *      search callback — a non-functional "Search" trigger would violate the no-dead-buttons
 *      rule, so it is omitted.
 *   5. Context-menu items whose desktop backing capabilities do not exist on Android (file
 *      manager / task-log paths / provider config / trajectory pane / feedback center) and
 *      actions with no callback in the mandated API (pin/rename/archive/mark-unread) are
 *      rendered in exact source order but DISABLED (source data-disabled styling), except
 *      "Copy session ID" which is fully live (clipboard = task id).
 *   6. show-more button (hasMore/onShowMore) and footer settings button (openSettingsTab,
 *      "border-t border-outline/40 p-3" + outline sm "Settings") are not ported: no
 *      pagination/settings callbacks exist in the mandated API; both would be dead buttons.
 *   7. Row hover marquee of TaskTitleOverflowText is a desktop hover interaction; the static
 *      right-fade mask is kept, the marquee is not.
 *   8. shadow-md (menu) → Compose elevation 4dp approximation.
 *   9. Desktop-only row extras not present in TaskRow2e (pinned indicator, pendingInteraction
 *      badge, change summary +N/-N, worktree/cron/moon icons, remote-device labels) are not
 *      rendered — the mandated row model carries only id/title/status/updatedAt/isActive.
 */
public enum class TaskRowStatus2e {
    RUNNING,
    COMPLETED,
    ERROR,
}

/**
 * Row model ported from MaridTaskMeta as consumed by TaskListItem (default variant).
 * `updatedAt` is epoch millis (MaridTaskMeta.updatedAt), `isActive` mirrors the isActive prop.
 */
public data class TaskRow2e(
    val id: String,
    val title: String,
    val status: TaskRowStatus2e,
    val updatedAt: Long,
    val isActive: Boolean = false,
)

/**
 * Status → accent mapping (see deviation 2 / header notes).
 *  RUNNING   → loader tint: --color-foreground-subtle (LoaderCircle text-foreground-subtle)
 *  COMPLETED → idle accent: --color-idle-task (dark = violet-400)
 *  ERROR     → dot fill: --color-destructive (red-500)
 */
public fun taskRowStatusAccentColor2e(status: TaskRowStatus2e): Color = when (status) {
    TaskRowStatus2e.RUNNING -> MaridTokens.ForegroundSubtle
    TaskRowStatus2e.COMPLETED -> MaridTokens.Violet400
    TaskRowStatus2e.ERROR -> MaridTokens.Destructive
}

/** formatTaskRelativeTime (lib/taskListItemPresentation.ts) — en-US strings. */
public fun formatTaskRelativeTime2e(updatedAt: Long, nowMs: Long): String {
    val minutes = (nowMs - updatedAt) / 60_000L
    if (minutes < 1) return "now" // taskList.justNow
    if (minutes < 60) return "${minutes}m" // taskList.minutesAgo "{minutes}m"
    val hours = minutes / 60
    if (hours < 24) return "${hours}h" // taskList.hoursAgo "{hours}h"
    return "${hours / 24}d" // taskList.daysAgo "{days}d"
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
            .background(MaridTokens.Sidebar), // --color-sidebar (neutral-950)
        verticalArrangement = Arrangement.spacedBy(8.dp), // container "flex flex-col gap-2"
    ) {
        // showCreateButton branch: "border-b p-3" with NewTaskButtonGroup
        if (onNewTask != null) {
            NewTaskButton2e(onNewTask = onNewTask)
        }

        if (tasks.isEmpty()) {
            // TID_TASK_EMPTY: "px-8.5 py-2 text-ui-base text-foreground-subtlest"
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 34.dp, vertical = 8.dp)) {
                Text(
                    text = "No tasks yet", // taskList.noTasks (en-US)
                    color = MaridTokens.ForegroundSubtlest,
                    fontSize = 14.sp, // text-ui-base
                )
            }
        } else {
            // "ul space-y-0.5"
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

/** NewTaskButtonGroup (NewTaskButtonGroup.tsx) inside TaskList's "border-b p-3" section. */
@Composable
private fun NewTaskButton2e(onNewTask: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Box(modifier = Modifier.fillMaxWidth().padding(12.dp)) { // p-3
            val interactionSource = remember { MutableInteractionSource() }
            val pressed by interactionSource.collectIsPressedAsState()
            // hover:bg-surface-hover → pressed state; transition-colors 150ms
            val bg by animateColorAsState(
                targetValue = if (pressed) MaridTokens.SurfaceHover else Color.Transparent,
                animationSpec = tween(durationMillis = 150),
                label = "newTaskBg",
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp) // h-8
                    .clip(RoundedCornerShape(8.dp)) // rounded-lg
                    .background(bg)
                    .clickable(
                        interactionSource = interactionSource,
                        indication = null, // pressed bg handled above (no Material ripple)
                        onClick = onNewTask,
                    )
                    .padding(horizontal = 10.dp), // pl-2.5 pr-2.5
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp), // gap-2
            ) {
                Icon(
                    imageVector = MaridIcons.MessageCirclePlus,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp), // h-4 w-4
                    tint = MaridTokens.Foreground,
                )
                Text(
                    text = "New task", // taskList.newThread (en-US)
                    color = MaridTokens.Foreground,
                    fontSize = 14.sp, // text-ui-base
                )
                // DEVIATION 6: source renders the keyboard-shortcut pill (ml-auto text-ui-xs
                // text-foreground-subtlest); no shortcut binding exists on touch devices.
            }
        }
        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(MaridTokens.Border)) // border-b
    }
}

/** Default-variant row (TaskListItem.tsx MemoTaskItem JSX, li line 629-638 + content 788-881). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TaskListItemRow2e(row: TaskRow2e, onOpenTask: (TaskRow2e) -> Unit) {
    var menuOpen by remember { mutableStateOf(false) }
    val nowMs = remember { System.currentTimeMillis() }
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    // isActive ? "bg-selected" : "hover:bg-surface-hover" — transition-colors 150ms
    val bg by animateColorAsState(
        targetValue = when {
            row.isActive -> MaridTokens.Selected // white 10%
            pressed -> MaridTokens.SurfaceHover // white 10%
            else -> Color.Transparent
        },
        animationSpec = tween(durationMillis = 150),
        label = "taskRowBg",
    )

    Box { // popup anchor
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp) // py-1 (4+4dp) + h-6 (24dp) title row = 32dp
                .clip(RoundedCornerShape(8.dp)) // rounded-lg
                .background(bg)
                .combinedClickable(
                    interactionSource = interactionSource,
                    indication = null, // pressed bg handled via animateColorAsState (no ripple)
                    onClick = { onOpenTask(row) },
                    onLongClick = { menuOpen = true }, // context menu trigger (source: right click)
                )
                .padding(start = 10.dp, end = 4.dp), // pl-2.5 pr-1
            verticalAlignment = Alignment.CenterVertically, // items-center (default variant)
            horizontalArrangement = Arrangement.spacedBy(8.dp), // gap-2
        ) {
            // "relative flex size-4 shrink-0 items-center justify-center"
            Box(modifier = Modifier.size(16.dp), contentAlignment = Alignment.Center) {
                LeadingIndicator2e(status = row.status)
            }
            TaskTitleText2e(title = row.title, modifier = Modifier.weight(1f))
            // "mr-0.5 ... text-ui-sm text-foreground-subtle"
            Text(
                text = formatTaskRelativeTime2e(updatedAt = row.updatedAt, nowMs = nowMs),
                modifier = Modifier.padding(end = 2.dp),
                color = MaridTokens.ForegroundSubtle,
                fontSize = 12.sp, // text-ui-sm
            )
        }
        if (menuOpen) {
            TaskRowContextMenu2e(row = row, onDismiss = { menuOpen = false })
        }
    }
}

/** Leading status slot — deriveTaskLeadingIndicator mapped onto the mandated status enum. */
@Composable
private fun LeadingIndicator2e(status: TaskRowStatus2e) {
    when (status) {
        // "h-1.5 w-1.5 rounded-full bg-destructive" (TaskListItem.tsx:663)
        TaskRowStatus2e.ERROR -> Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(taskRowStatusAccentColor2e(status)),
        )

        // "size-[7px] rounded-full" geometry with the --color-idle-task accent (deviation 2)
        TaskRowStatus2e.COMPLETED -> Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(taskRowStatusAccentColor2e(status)),
        )

        // "LoaderCircle size-4 text-foreground-subtle" + animate-spin (TaskListItem.tsx:667)
        TaskRowStatus2e.RUNNING -> {
            val spin = rememberInfiniteTransition(label = "taskRowSpin")
            val angle by spin.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 1000, easing = LinearEasing), // 1s linear infinite
                ),
                label = "taskRowSpinAngle",
            )
            Icon(
                imageVector = MaridIcons.LoaderCircle,
                contentDescription = null,
                modifier = Modifier
                    .size(16.dp) // size-4
                    .graphicsLayer { rotationZ = angle },
                tint = taskRowStatusAccentColor2e(status),
            )
        }
    }
}

/**
 * TaskTitleOverflowText — single-line title with the source's right-edge fade mask
 * ("[mask-image:linear-gradient(to_right,black_calc(100%_-_1.5rem),transparent)]"), applied
 * only while overflowing. Ellipsis is intentionally NOT used (source Bugfix comment).
 */
@Composable
private fun TaskTitleText2e(title: String, modifier: Modifier = Modifier) {
    var overflowing by remember(title) { mutableStateOf(false) }
    Box(modifier = modifier.then(if (overflowing) Modifier.taskTitleFadeMask2e() else Modifier)) {
        Text(
            text = title,
            modifier = Modifier.fillMaxWidth(),
            color = MaridTokens.Foreground,
            fontSize = 14.sp, // text-ui-base
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Clip,
            onTextLayout = { overflowing = it.hasVisualOverflow },
        )
    }
}

/** Right 1.5rem (24dp) alpha fade, exactly the source mask gradient, via DstIn blending. */
private fun Modifier.taskTitleFadeMask2e(): Modifier =
    this.graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
        .drawWithContent {
            drawContent()
            val fadePx = 24.dp.toPx() // mask fade = 1.5rem
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

/**
 * Long-press context menu — TaskListItemContextMenuContent with the exact item list/order of
 * TaskActionMenuContent (see header deviation 5 for the enabled/disabled rationale).
 */
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
                .width(208.dp) // w-52 (ContextMenuContent className)
                .shadow(elevation = 4.dp, shape = RoundedCornerShape(8.dp), clip = false) // shadow-md approx
                .clip(RoundedCornerShape(8.dp)) // rounded-lg
                .background(MaridTokens.Menu) // bg-menu (neutral-950)
                .border(1.dp, MaridTokens.Border, RoundedCornerShape(8.dp)) // border-popover-border
                .padding(vertical = 4.dp), // p-1 vertical half (horizontal lives on items so
            // separators can run full-bleed like "-mx-1")
            verticalArrangement = Arrangement.spacedBy(2.dp), // gap-0.5
        ) {
            ContextMenuItem2e("Pin task", enabled = false) // taskList.pin
            ContextMenuItem2e("Rename task", enabled = false) // taskList.rename
            ContextMenuItem2e("Archive task", enabled = false) // taskList.archive
            ContextMenuItem2e("Mark as unread", enabled = false) // taskList.markAsUnread
            MenuSeparator2e()
            ContextMenuItem2e("Open in File Manager", enabled = false) // appHeader.openInFileManager
            ContextMenuItem2e("Copy path", enabled = false) // appHeader.copyPath
            ContextMenuItem2e("Copy task path", enabled = false) // appHeader.copyTaskPath
            ContextMenuItem2e("Copy log path", enabled = false) // appHeader.copyLogPath
            ContextMenuItem2e("Copy session ID", enabled = true) { // appHeader.copySessionId
                clipboard.setText(AnnotatedString(row.id))
                onDismiss()
            }
            ContextMenuItem2e("Go to config", enabled = false) // appHeader.goToProviderConfig
            MenuSeparator2e()
            ContextMenuItem2e("View model trajectory", enabled = false) // taskList.viewModelTrajectory
            MenuSeparator2e()
            ContextMenuItem2e("Report issue", enabled = false) // taskList.feedback
        }
    }
}

/** ContextMenuItem: "min-h-7 rounded-md px-2 py-1 text-ui-base/relaxed" + disabled styling. */
@Composable
private fun ContextMenuItem2e(label: String, enabled: Boolean, onClick: (() -> Unit)? = null) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Box(modifier = Modifier.padding(horizontal = 4.dp)) { // p-1 horizontal half
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 28.dp) // min-h-7
                .clip(RoundedCornerShape(6.dp)) // rounded-md
                // data-[highlighted]:bg-menu-hover (menu-hover = neutral-900)
                .background(if (enabled && pressed) MaridTokens.MenuHover else Color.Transparent)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null, // pressed bg handled above (no Material ripple)
                    enabled = enabled,
                    onClick = { onClick?.invoke() },
                )
                .padding(horizontal = 8.dp, vertical = 4.dp), // px-2 py-1
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                // data-disabled:text-foreground-subtlest
                color = if (enabled) MaridTokens.Foreground else MaridTokens.ForegroundSubtlest,
                fontSize = 14.sp, // text-ui-base
                lineHeight = 22.75f.sp, // text-ui-base/relaxed = 1.625 * 14sp
            )
        }
    }
}

/** ContextMenuSeparator: "-mx-1 my-1 h-px bg-border" (full-bleed inside the p-1 shell). */
@Composable
private fun MenuSeparator2e() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp) // my-1
            .height(1.dp) // h-px
            .background(MaridTokens.Border), // bg-border
    )
}

/** Radix-style placement: side=bottom align=start, flips above when out of room. */
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
            y = anchorBounds.top - popupContentSize.height - 4 // flip above the row
        }
        val maxX = (windowSize.width - popupContentSize.width).coerceAtLeast(0)
        val maxY = (windowSize.height - popupContentSize.height).coerceAtLeast(0)
        return IntOffset(x.coerceIn(0, maxX), y.coerceIn(0, maxY))
    }
}
