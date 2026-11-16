package com.marid.app.ui
// marid-sync-1

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marid.app.icons.MaridIcons

/**
 * 1:1 Compose port of the ZCode desktop workspace sidebar as a MOBILE DRAWER
 * (dark theme tokens from [MaridTokens]; Tailwind spacing 1 unit = 4dp;
 * rounded-md 6dp, rounded-lg 8dp, rounded-xl 12dp, rounded-2xl 16dp).
 *
 * React files ported:
 *   - packages/ui/src/WorkspaceSidebar.tsx -> project-sidebar/index.tsx
 *     (aside flex h-full flex-col; h-12 top strip; navigation px-2 py-3 gap-1;
 *      task list region border-t border-border/50 pt-3; scroll column gap-3 pb-3;
 *      loading px-4 text-ui-sm text-foreground-subtle / error text-destructive)
 *   - packages/ui/src/NewTaskButtonGroup.tsx
 *     (h-8 32dp rounded-lg 8dp pl/pr 10dp, gap-2, hover:bg-surface-hover,
 *      MessageCirclePlus h-4 w-4 16dp, text-ui-base 14sp,
 *      shortcut text-ui-xs 10sp text-foreground-subtlest ml-auto)
 *   - packages/ui/src/project-sidebar/ProjectSidebarNavigation.tsx
 *     (Automations/Plugin Marketplace: Button ghost size lg h-8 32dp px-2.5 10dp
 *      gap-2 justify-start, CalendarClock / Blocks size-4 16dp, active bg-selected)
 *   - packages/ui/src/project-sidebar/ProjectSidebarListHeader.tsx
 *     (px-2 pb-2 gap-1; title px-2.5 text-ui-base font-medium text-foreground-subtle;
 *      Search/ListFilter ghost icon-sm 24dp, icon size-3.5 14dp text-foreground-subtle)
 *   - packages/ui/src/WorkspaceSidebar/WorkspacePurposeSection.tsx
 *     (section header ml-2 mr-1 h-7 28dp; title px-2.5 text-ui-base font-medium
 *      text-foreground-subtlest; chevron size-3.5 visible on touch;
 *      action buttons ghost icon-sm, Plus/ListChevrons* size-3.5)
 *   - packages/ui/src/project-sidebar/ProjectNode.tsx + ProjectNodeContent.tsx
 *     (section rounded-xl 12dp; row ml-2 h-8 32dp gap-1 rounded-lg pr-1
 *      hover:bg-surface-hover; title button h-8 gap-2 pl-2.5 pr-1 text-foreground;
 *      icon slot size-4 16dp ProjectFolderIcon text-foreground-subtle; name
 *      text-ui-base text-foreground-subtle truncate; device label text-ui-sm
 *      text-remote-foreground-muted max-w-24 96dp; unread dot size-1.5 6dp
 *      rounded-full bg-icon-blue; trailing ghost icon-sm buttons Ellipsis/ListTree/
 *      MessageCirclePlus size-3.5, always visible on touch;
 *      expanded tasks container pl-2 pt-1)
 *   - packages/ui/src/project-sidebar/ProjectFolderIcon.tsx
 *     (rootCount>1 -> Folders/FoldersOpen else Folder/FolderOpen; FoldersOpen
 *      geometry copied verbatim from the source)
 *   - packages/ui/src/TaskListItem.tsx + components/TaskTitleOverflowText.tsx
 *     (row gap-2 rounded-lg pl-2.5 pr-1 py-1, active bg-selected / hover bg-surface-hover;
 *      leading slot size-4: error dot h-1.5 w-1.5 bg-destructive, unread dot
 *      size-[7px] bg-icon-blue, LoaderCircle size-4 text-foreground-subtle animate-spin;
 *      title text-ui-base text-foreground with right 1.5rem fade mask;
 *      change summary +N text-diff-added / -N text-diff-removed;
 *      timeline second line text-ui-base text-foreground-subtle h-6;
 *      meta text-ui-sm text-foreground-subtle, Clock size-3.5;
 *      trailing actions gap-0.5 ghost icon-sm, visible on touch)
 *   - packages/ui/src/project-sidebar/ProjectTaskRows.tsx
 *     (pinned wrapper px-2; "Pinned" h3 px-2.5 py-1 text-ui-base font-medium
 *      text-foreground-subtlest; empty state px-8.5 py-2 text-foreground-subtlest;
 *      show-more ghost size sm ml-8.5 text-foreground-subtlest;
 *      timeline date headings px-2.5 pt-2 pb-1 text-foreground-subtle)
 *   - packages/ui/src/project-sidebar/ArchiveGroupCard.tsx
 *     (header row gap-2; toggle ghost default h-7 px-0 font-medium, Folder size-4
 *      text-foreground-subtle, ChevronRight size-3.5 rotate-90 when open;
 *      count text-ui-sm text-foreground-subtle; menu ghost icon-sm Ellipsis size-4;
 *      open content pt-3 card rounded-xl border border-border bg-surface px-4
 *      divide-y divide-border; rows py-3 gap-x-3; title text-ui-base font-medium;
 *      date text-ui-sm text-foreground-subtle; Trash2 ghost icon-sm text-foreground-subtle;
 *      unarchive secondary h-7 px-2 bg-secondary text-foreground rounded-md;
 *      show-more ghost my-2 w-full)
 *   - packages/ui/src/project-sidebar/HomeOnlyWorkspaceRows.tsx
 *     (section px-2; h3 px-2.5 py-1 text-foreground-subtlest; rows h-8 gap-2
 *      rounded-lg px-2.5 text-ui-base text-foreground-subtle hover:bg-surface-hover;
 *      status span text-ui-sm tone-colored)
 *   - packages/ui/src/project-sidebar/OfflineRemoteProjectRows.tsx
 *     (rows rounded-lg px-3 py-2 text-ui-sm hover:bg-surface-hover tone-colored)
 *   - packages/ui/src/WorkspaceSidebarFooter.tsx
 *     (+ WorkspaceSidebarFooterUsageSummary.tsx / WorkspaceSidebarFooterPlanBadgeHelpers.ts)
 *     (footer px-4 pt-2 pb-4 gap-2.5 + pr-3 -> left 16 / right 12 / top 8 / bottom 16,
 *      gap 10dp; profile button ghost lg h-8 flex-1 justify-start gap-2 rounded-tl-2xl
 *      rounded-bl-2xl 16dp border-0 pl-0; Avatar size-8 32dp rounded-full ring
 *      border-border, fallback bg-background text-foreground text-ui-base;
 *      name text-ui-base font-semibold text-foreground truncate; plan badge chip
 *      rounded-full border border-border bg-surface px-1 py-px text-ui-xs font-medium
 *      text-foreground-subtle max-w-20 80dp; settings ghost icon-lg 32dp Settings size-4)
 *
 * Exact extra colors (computed from styles.css .dark):
 *   --color-remote-foreground-muted: #B3B3B3
 *   --color-icon-blue: terminal-bright-blue -> sky-600 (#0084D1, MaridTokens.Sky600)
 *   --color-warning-surface: color-mix(in srgb, yellow-500 16%, transparent) -> 0x29F0B100
 *   --color-warning-surface-foreground: yellow-400 (MaridTokens.Yellow400)
 *   --color-progress-surface-foreground: blue-300 (MaridTokens.Blue300)
 *   --color-success-surface-foreground: green-300 (MaridTokens.Green300)
 *   --color-destructive-surface-foreground: red-300 (MaridTokens.Red300)
 *
 * Deviations (mobile adaptation):
 *   - The fixed-width desktop panel becomes a slide-in drawer: sheet width is the
 *     exact desktop sidebar width (WORKSPACE_SIDEBAR_DEFAULT_WIDTH_PX = 264px)
 *     when it fits (screen width - 64dp), else full width minus 64dp.
 *   - Row menu/context-menu/dropdown CONTENTS are not ported (separate overlay layer
 *     in React); their trigger buttons fire the provided callbacks.
 *   - Profile dropdown menu is not ported; its plan-badge chip is shown next to the
 *     profile name with the exact chip styles (the desktop menu is the only place
 *     the badge lives in the source).
 *   - ProjectNode "session drop target"/drag indicators and dnd-kit reorder
 *     affordances are desktop drag interactions and are not ported.
 */
enum class MaridRemoteStatus { Online, Connecting, Offline, Pending, Failed }

// ------------------------------------------------------------------ data models

data class MaridSidebarTask(
    val key: String,
    val title: String,
    val timeLabel: String? = null,
    val isActive: Boolean = false,
    val isUnread: Boolean = false,
    val isError: Boolean = false,
    val isRunning: Boolean = false,
    val isCron: Boolean = false,
    val timeline: Boolean = false,
    val workspaceLabel: String? = null,
    val deviceLabel: String? = null,
    val addedLines: Int = 0,
    val removedLines: Int = 0,
)

data class MaridSidebarProject(
    val id: String,
    val name: String,
    val pinned: Boolean = false,
    val rootCount: Int = 1,
    val expanded: Boolean = false,
    val isRemote: Boolean = false,
    val status: MaridRemoteStatus = MaridRemoteStatus.Online,
    val online: Boolean = true,
    val hasUnread: Boolean = false,
    val deviceLabel: String? = null,
    val statusLabel: String? = null,
    val titleDimmed: Boolean = false, // primaryRootStatus missing/unavailable -> opacity-50
    val tasks: List<MaridSidebarTask> = emptyList(),
)

data class MaridArchiveTask(val key: String, val title: String, val dateLabel: String)

data class MaridArchiveGroup(val name: String, val tasks: List<MaridArchiveTask>)

data class MaridOfflineWorkspaceRow(
    val key: String,
    val label: String,
    val status: MaridRemoteStatus,
    val statusLabel: String,
)

data class MaridHomeOnlyWorkspace(
    val key: String,
    val label: String,
    val isRemote: Boolean,
    val status: MaridRemoteStatus,
    val statusLabel: String?,
)

/** English defaults extracted from packages/ui/src/i18n/locales/en-US.ts. */
data class MaridSidebarStrings(
    val newTask: String = "New task", // taskList.newThread
    val automations: String = "Automations", // workspace.openScheduledSettings
    val pluginMarketplace: String = "Plugin Marketplace", // workspace.openPluginsSettings
    val listTitle: String = "Task list", // taskList.title
    val search: String = "Search", // commandCenter.open
    val filterView: String = "Filter view", // project.view
    val pinnedSection: String = "Pinned", // taskList.pinnedSection
    val projectsSection: String = "Projects", // project.projects
    val tasksSection: String = "Tasks", // project.tasks
    val newProject: String = "New project", // project.new
    val newTaskInProject: String = "New task", // project.createTask
    val showFiles: String = "Show files", // workspaceSidebar.showFileTree
    val more: String = "More", // common.more
    val noTasks: String = "No tasks yet", // taskList.noTasks
    val noUnreadTasks: String = "No unread tasks", // taskList.noUnreadTasks
    val showMore: String = "Show more", // taskList.showMore
    val showLess: String = "Show less", // taskList.showLess
    val loading: String = "Loading...", // common.loading
    val offline: String = "Offline", // project.offline
    val selectProject: String = "Select a project", // project.unresolved
    val thisComputer: String = "This computer", // project.thisComputer
    val homeOnlyWorkspaces: String = "Workspaces", // webRemoteControl.homeOnlyWorkspaces
    val noSwitchableWorkspaces: String = "No switchable workspaces",
    val settings: String = "Settings", // settings.title
    val notLoggedIn: String = "Connect", // sidebar.profile.notLoggedIn
    val unarchive: String = "Unarchive", // archives.unarchive
    val archiveCount: (Int) -> String = { count -> "$count sessions" }, // archives.count
    val archiveGroupActions: (String) -> String = { name -> "Archive actions for $name" },
    val archiveDeleteTask: (String) -> String = { title -> "Permanently delete \"$title\"" },
)

// ------------------------------------------------------------- exact dark colors

/** --color-remote-foreground-muted (dark): #B3B3B3 */
internal val MaridRemoteForegroundMuted = Color(0xFFB3B3B3)

/** --color-icon-blue (dark): terminal-bright-blue -> sky-600 (MaridTokens.Sky600) */
internal val MaridIconBlue = MaridTokens.Sky600

/** --color-warning-surface (dark): color-mix(in srgb, yellow-500 16%, transparent) */
internal val MaridWarningSurface = Color(0x29F0B100)

/** --color-progress-surface-foreground (dark): blue-300 */
internal val MaridProgressForeground = MaridTokens.Blue300

/** --color-warning-surface-foreground (dark): yellow-400 */
internal val MaridWarningForeground = MaridTokens.Yellow400

/** --color-success-surface-foreground (dark): green-300 */
internal val MaridSuccessForeground = MaridTokens.Green300

/** --color-destructive-surface-foreground (dark): red-300 */
internal val MaridDangerForeground = MaridTokens.Red300

/** --color-border/50 (border-t border-border/50 -> border token at 50% alpha) */
internal val MaridBorderHalf = MaridTokens.Border.copy(alpha = 0.5f)

/**
 * RemoteConnectionPresentation TONES (lib/remoteConnectionPresentation.ts):
 * status -> foreground color class. `projectIcon` mirrors
 * RemoteConnectionStateIcon(project=true), where online uses the muted neutral.
 */
internal fun maridRemoteStatusForeground(status: MaridRemoteStatus, projectIcon: Boolean): Color =
    when (status) {
        MaridRemoteStatus.Online ->
            if (projectIcon) MaridRemoteForegroundMuted else MaridSuccessForeground
        MaridRemoteStatus.Connecting -> MaridProgressForeground
        MaridRemoteStatus.Offline -> MaridRemoteForegroundMuted // neutral tone
        MaridRemoteStatus.Pending -> MaridWarningForeground
        MaridRemoteStatus.Failed -> MaridDangerForeground
    }

// ----------------------------------------------------------- lucide additions
// lucide-static v1.73.0 geometry (same version as MaridIcons.kt): exact 24x24
// paths, stroke-width 2, round caps/joins, fetched for icons missing from
// com.marid.app.icons.MaridIcons.

internal object MaridIcons2a {

    private fun builder(name: String) = ImageVector.Builder(
        name = "Marid2a.$name",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    )

    private fun ImageVector.Builder.stroke(block: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit) =
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) { block() }

    /** lucide "cloud" v1.73.0: M17.5 19H9a7 7 0 1 1 6.71-9h1.79a4.5 4.5 0 1 1 0 9Z */
    internal val Cloud: ImageVector by lazy {
        builder("Cloud").apply {
            stroke {
                moveTo(17.5f, 19f)
                lineTo(9f, 19f)
                // a7 7 0 1 1 6.71 -9
                arcTo(7f, 7f, 0f, true, true, 15.71f, 10f)
                lineTo(17.5f, 10f)
                // a4.5 4.5 0 1 1 0 9
                arcTo(4.5f, 4.5f, 0f, true, true, 17.5f, 19f)
                close()
            }
        }.build()
    }

    /** lucide "cloud-off" v1.73.0 */
    internal val CloudOff: ImageVector by lazy {
        builder("CloudOff").apply {
            stroke {
                moveTo(10.94f, 5.274f)
                // a7 7 0 0 1 4.77 4.726
                arcTo(7f, 7f, 0f, false, true, 15.71f, 10f)
                lineTo(17.5f, 10f)
                // a4.5 4.5 0 0 1 4.222 6.057
                arcTo(4.5f, 4.5f, 0f, false, true, 21.722f, 16.057f)
            }
            stroke {
                moveTo(18.796f, 18.81f)
                // a4.5 4.5 0 0 1 -1.296 .19
                arcTo(4.5f, 4.5f, 0f, false, true, 17.5f, 19f)
                lineTo(9f, 19f)
                // A7 7 0 0 1 5.79 5.78
                arcTo(7f, 7f, 0f, false, true, 5.79f, 5.78f)
            }
            stroke {
                moveTo(2f, 2f)
                lineTo(22f, 22f)
            }
        }.build()
    }

    /**
     * lucide "cloud-alert" v1.73.0
     * (M12 12v4 / M12 20h.01 / M8.128 16.949A7 7 0 1 1 15.71 8h1.79a1 1 0 0 1 0 9h-1.642)
     */
    internal val CloudAlert: ImageVector by lazy {
        builder("CloudAlert").apply {
            stroke {
                moveTo(12f, 12f)
                lineTo(12f, 16f)
            }
            stroke {
                moveTo(12f, 20f)
                lineTo(12.01f, 20f)
            }
            stroke {
                moveTo(8.128f, 16.949f)
                // A7 7 0 1 1 15.71 8
                arcTo(7f, 7f, 0f, true, true, 15.71f, 8f)
                lineTo(17.5f, 8f)
                // a1 1 0 0 1 0 9 — SVG scales radii up to a half-circle (r=4.5)
                arcTo(4.5f, 4.5f, 0f, true, true, 17.5f, 17f)
                lineTo(15.858f, 17f)
            }
        }.build()
    }

    /** lucide "folder-open" v1.73.0 */
    internal val FolderOpen: ImageVector by lazy {
        builder("FolderOpen").apply {
            stroke {
                moveTo(6f, 14f)
                lineTo(7.5f, 11.1f)
                // A2 2 0 0 1 9.24 10
                arcTo(2f, 2f, 0f, false, true, 9.24f, 10f)
                lineTo(20f, 10f)
                // a2 2 0 0 1 1.94 2.5
                arcTo(2f, 2f, 0f, false, true, 21.94f, 12.5f)
                lineTo(20.4f, 18.5f)
                // a2 2 0 0 1 -1.95 1.5
                arcTo(2f, 2f, 0f, false, true, 18.45f, 20f)
                lineTo(4f, 20f)
                // a2 2 0 0 1 -2 -2
                arcTo(2f, 2f, 0f, false, true, 2f, 18f)
                lineTo(2f, 5f)
                // a2 2 0 0 1 2 -2
                arcTo(2f, 2f, 0f, false, true, 4f, 3f)
                lineTo(7.9f, 3f)
                // a2 2 0 0 1 1.69 .9
                arcTo(2f, 2f, 0f, false, true, 9.59f, 3.9f)
                lineTo(10.4f, 5.1f)
                // a2 2 0 0 0 1.67 .9
                arcTo(2f, 2f, 0f, false, false, 12.07f, 6f)
                lineTo(18f, 6f)
                // a2 2 0 0 1 2 2
                arcTo(2f, 2f, 0f, false, true, 20f, 8f)
                lineTo(20f, 10f)
            }
        }.build()
    }

    /** lucide "folders" v1.73.0 (front + back outline) */
    internal val Folders: ImageVector by lazy {
        builder("Folders").apply {
            stroke {
                moveTo(20f, 5f)
                // a2 2 0 0 1 2 2
                arcTo(2f, 2f, 0f, false, true, 22f, 7f)
                lineTo(22f, 14f)
                // a2 2 0 0 1 -2 2
                arcTo(2f, 2f, 0f, false, true, 20f, 16f)
                lineTo(9f, 16f)
                // a2 2 0 0 1 -2 -2
                arcTo(2f, 2f, 0f, false, true, 7f, 14f)
                lineTo(7f, 5f)
                // a2 2 0 0 1 2 -2
                arcTo(2f, 2f, 0f, false, true, 9f, 3f)
                lineTo(11.5f, 3f)
                // a1.5 1.5 0 0 1 1.2 .6
                arcTo(1.5f, 1.5f, 0f, false, true, 12.7f, 3.6f)
                lineTo(13.3f, 4.4f)
                // a1.5 1.5 0 0 0 1.2 .6
                arcTo(1.5f, 1.5f, 0f, false, false, 14.5f, 5f)
                close()
            }
            stroke {
                moveTo(3f, 8.268f)
                // a2 2 0 0 0 -1 1.738
                arcTo(2f, 2f, 0f, false, false, 2f, 10.006f)
                lineTo(2f, 19f)
                // a2 2 0 0 0 2 2
                arcTo(2f, 2f, 0f, false, false, 4f, 21f)
                lineTo(15f, 21f)
                // a2 2 0 0 0 1.732 -1
                arcTo(2f, 2f, 0f, false, false, 16.732f, 20f)
            }
        }.build()
    }

    /**
     * FoldersOpen — copied VERBATIM from project-sidebar/ProjectFolderIcon.tsx
     * ("folders-open" custom lucide icon): front open-flap folder + back outline.
     */
    internal val FoldersOpen: ImageVector by lazy {
        builder("FoldersOpen").apply {
            stroke {
                moveTo(10f, 12f)
                lineTo(11.2f, 10f)
                // a2 2 0 0 1 1.72 -1
                arcTo(2f, 2f, 0f, false, true, 12.92f, 9f)
                lineTo(21f, 9f)
                // a1 1 0 0 1 .97 1.25
                arcTo(1f, 1f, 0f, false, true, 21.97f, 10.25f)
                lineTo(20.87f, 14.5f)
                // A2 2 0 0 1 18.94 16
                arcTo(2f, 2f, 0f, false, true, 18.94f, 16f)
                lineTo(9f, 16f)
                // a2 2 0 0 1 -2 -2
                arcTo(2f, 2f, 0f, false, true, 7f, 14f)
                lineTo(7f, 5f)
                // a2 2 0 0 1 2 -2
                arcTo(2f, 2f, 0f, false, true, 9f, 3f)
                lineTo(11.5f, 3f)
                // a1.5 1.5 0 0 1 1.2 .6
                arcTo(1.5f, 1.5f, 0f, false, true, 12.7f, 3.6f)
                lineTo(13.3f, 4.4f)
                // a1.5 1.5 0 0 0 1.2 .6
                arcTo(1.5f, 1.5f, 0f, false, false, 14.5f, 5f)
                lineTo(20f, 5f)
                // a2 2 0 0 1 2 2
                arcTo(2f, 2f, 0f, false, true, 22f, 7f)
                lineTo(22f, 9f)
            }
            stroke {
                moveTo(3f, 8.268f)
                // a2 2 0 0 0 -1 1.738
                arcTo(2f, 2f, 0f, false, false, 2f, 10.006f)
                lineTo(2f, 19f)
                // a2 2 0 0 0 2 2
                arcTo(2f, 2f, 0f, false, false, 4f, 21f)
                lineTo(15f, 21f)
                // a2 2 0 0 0 1.732 -1
                arcTo(2f, 2f, 0f, false, false, 16.732f, 20f)
            }
        }.build()
    }

    /** lucide "list-tree" v1.73.0 */
    internal val ListTree: ImageVector by lazy {
        builder("ListTree").apply {
            stroke {
                moveTo(8f, 5f)
                lineTo(21f, 5f)
            }
            stroke {
                moveTo(13f, 12f)
                lineTo(21f, 12f)
            }
            stroke {
                moveTo(13f, 19f)
                lineTo(21f, 19f)
            }
            stroke {
                moveTo(3f, 10f)
                // a2 2 0 0 0 2 2
                arcTo(2f, 2f, 0f, false, false, 5f, 12f)
                lineTo(8f, 12f)
            }
            stroke {
                moveTo(3f, 5f)
                lineTo(3f, 17f)
                // a2 2 0 0 0 2 2
                arcTo(2f, 2f, 0f, false, false, 5f, 19f)
                lineTo(8f, 19f)
            }
        }.build()
    }

    /** lucide "list-chevrons-down-up" v1.73.0 */
    internal val ListChevronsDownUp: ImageVector by lazy {
        builder("ListChevronsDownUp").apply {
            stroke { moveTo(3f, 5f); lineTo(11f, 5f) }
            stroke { moveTo(3f, 12f); lineTo(11f, 12f) }
            stroke { moveTo(3f, 19f); lineTo(11f, 19f) }
            stroke {
                moveTo(15f, 5f)
                lineTo(18f, 8f)
                lineTo(21f, 5f)
            }
            stroke {
                moveTo(15f, 19f)
                lineTo(18f, 16f)
                lineTo(21f, 19f)
            }
        }.build()
    }

    /** lucide "list-chevrons-up-down" v1.73.0 */
    internal val ListChevronsUpDown: ImageVector by lazy {
        builder("ListChevronsUpDown").apply {
            stroke { moveTo(3f, 5f); lineTo(11f, 5f) }
            stroke { moveTo(3f, 12f); lineTo(11f, 12f) }
            stroke { moveTo(3f, 19f); lineTo(11f, 19f) }
            stroke {
                moveTo(15f, 8f)
                lineTo(18f, 5f)
                lineTo(21f, 8f)
            }
            stroke {
                moveTo(15f, 16f)
                lineTo(18f, 19f)
                lineTo(21f, 16f)
            }
        }.build()
    }

    /** lucide "pin" v1.73.0 */
    internal val Pin: ImageVector by lazy {
        builder("Pin").apply {
            stroke {
                moveTo(12f, 17f)
                lineTo(12f, 22f)
            }
            stroke {
                moveTo(9f, 10.76f)
                // a2 2 0 0 1 -1.11 1.79
                arcTo(2f, 2f, 0f, false, true, 7.89f, 12.55f)
                lineTo(6.11f, 13.45f)
                // A2 2 0 0 0 5 15.24
                arcTo(2f, 2f, 0f, false, false, 5f, 15.24f)
                lineTo(5f, 16f)
                // a1 1 0 0 0 1 1
                arcTo(1f, 1f, 0f, false, false, 6f, 17f)
                lineTo(18f, 17f)
                // a1 1 0 0 0 1 -1
                arcTo(1f, 1f, 0f, false, false, 19f, 16f)
                lineTo(19f, 15.24f)
                // a2 2 0 0 0 -1.11 -1.79
                arcTo(2f, 2f, 0f, false, false, 17.89f, 13.45f)
                lineTo(16.11f, 12.55f)
                // A2 2 0 0 1 15 10.76
                arcTo(2f, 2f, 0f, false, true, 15f, 10.76f)
                lineTo(15f, 7f)
                // a1 1 0 0 1 1 -1
                arcTo(1f, 1f, 0f, false, true, 16f, 6f)
                // a2 2 0 0 0 0 -4
                arcTo(2f, 2f, 0f, false, false, 16f, 2f)
                lineTo(8f, 2f)
                // a2 2 0 0 0 0 4
                arcTo(2f, 2f, 0f, false, false, 8f, 6f)
                // a1 1 0 0 1 1 1
                arcTo(1f, 1f, 0f, false, true, 9f, 7f)
                close()
            }
        }.build()
    }

    /** lucide "trash-2" v1.73.0 */
    internal val Trash2: ImageVector by lazy {
        builder("Trash2").apply {
            stroke { moveTo(10f, 11f); lineTo(10f, 17f) }
            stroke { moveTo(14f, 11f); lineTo(14f, 17f) }
            stroke {
                moveTo(19f, 6f)
                lineTo(19f, 20f)
                // a2 2 0 0 1 -2 2
                arcTo(2f, 2f, 0f, false, true, 17f, 22f)
                lineTo(7f, 22f)
                // a2 2 0 0 1 -2 -2
                arcTo(2f, 2f, 0f, false, true, 5f, 20f)
                lineTo(5f, 6f)
            }
            stroke { moveTo(3f, 6f); lineTo(21f, 6f) }
            stroke {
                moveTo(8f, 6f)
                lineTo(8f, 4f)
                // a2 2 0 0 1 2 -2
                arcTo(2f, 2f, 0f, false, true, 10f, 2f)
                lineTo(14f, 2f)
                // a2 2 0 0 1 2 2
                arcTo(2f, 2f, 0f, false, true, 16f, 4f)
                lineTo(16f, 6f)
            }
        }.build()
    }
}

// ------------------------------------------------------------ shared primitives

/**
 * RemoteConnectionStateIcon (remote-connection/RemoteConnectionStateIcon.tsx):
 * 16dp slot; online -> Cloud, connecting -> LoaderCircle spin, offline -> CloudOff,
 * pending -> CloudAlert, failed -> Cloud + X overlay (size-3, -bottom-1 -right-1).
 */
@Composable
internal fun MaridRemoteStateIcon(
    status: MaridRemoteStatus,
    project: Boolean,
    modifier: Modifier = Modifier,
    size: Dp = 16.dp,
) {
    val tint = maridRemoteStatusForeground(status, projectIcon = project)
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        when (status) {
            MaridRemoteStatus.Online ->
                Icon(MaridIcons2a.Cloud, null, Modifier.size(size), tint = tint)
            MaridRemoteStatus.Connecting ->
                MaridSpinningLoader(size = size, tint = tint)
            MaridRemoteStatus.Offline ->
                Icon(MaridIcons2a.CloudOff, null, Modifier.size(size), tint = tint)
            MaridRemoteStatus.Pending ->
                Icon(MaridIcons2a.CloudAlert, null, Modifier.size(size), tint = tint)
            MaridRemoteStatus.Failed -> {
                Icon(MaridIcons2a.Cloud, null, Modifier.size(size), tint = tint)
                // cloud-x: <X className="absolute -bottom-1 -right-1 size-3 bg-inherit" />
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(12.dp)
                        .background(MaridTokens.Sidebar)
                        .padding(1.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(MaridIcons.X, null, Modifier.size(10.dp), tint = tint)
                }
            }
        }
    }
}

/** ProjectFolderIcon: rootCount > 1 -> Folders/FoldersOpen else Folder/FolderOpen. */
@Composable
private fun MaridProjectFolderIcon(rootCount: Int, open: Boolean, size: Dp, tint: Color) {
    val icon = when {
        rootCount > 1 -> if (open) MaridIcons2a.FoldersOpen else MaridIcons2a.Folders
        open -> MaridIcons2a.FolderOpen
        else -> MaridIcons.Folder
    }
    Icon(icon, contentDescription = null, modifier = Modifier.size(size), tint = tint)
}

/**
 * Ghost `icon-sm` row button: size-6 (24dp), rounded-md (6dp), icon size-3.5 (14dp),
 * text-foreground-subtle, hover:text-foreground (pressed brightens the tint).
 */
@Composable
private fun MaridRowGhostIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconSize: Dp = 14.dp,
    enabled: Boolean = true,
    tint: Color = MaridTokens.ForegroundSubtle,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier = modifier
            .size(24.dp)
            .alpha(if (enabled) 1f else 0.5f)
            .clip(RoundedCornerShape(6.dp))
            .background(if (pressed) MaridTokens.SurfaceHover else Color.Transparent)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClickLabel = contentDescription,
            ) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize),
            tint = if (pressed) MaridTokens.Foreground else tint,
        )
    }
}

/**
 * TaskTitleOverflowText fade: [mask-image:linear-gradient(to_right, black
 * calc(100% - 1.5rem), transparent)] — 24dp right-edge fade instead of an ellipsis.
 */
private fun Modifier.maridTitleRightFade(): Modifier =
    graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
        .drawWithContent {
            drawContent()
            val fadeWidth = 24.dp.toPx() // 1.5rem
            if (size.width > fadeWidth) {
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.Black, Color.Black, Color.Transparent),
                        startX = size.width - fadeWidth,
                        endX = size.width,
                    ),
                    blendMode = BlendMode.DstIn,
                )
            }
        }

// ------------------------------------------------------------------- drawer

/**
 * Behavioral container: ModalNavigationDrawer (content fully custom-styled).
 * Sheet width = exact desktop sidebar width (264px,
 * WORKSPACE_SIDEBAR_DEFAULT_WIDTH_PX in app-shell/WorkspaceShellLayout.tsx)
 * when it fits on screen minus 64dp, else full width minus 64dp.
 */
@Composable
fun MaridSidebarDrawerHost(
    isOpen: Boolean,
    onOpenChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    drawerContent: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    val drawerWidth =
        if (screenWidth >= 264.dp + 64.dp) 264.dp else screenWidth - 64.dp
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    LaunchedEffect(isOpen) {
        if (isOpen) drawerState.open() else drawerState.close()
    }
    LaunchedEffect(drawerState.targetValue) {
        if (drawerState.targetValue == DrawerValue.Closed && isOpen) onOpenChange(false)
    }
    ModalNavigationDrawer(
        modifier = modifier,
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerShape = RectangleShape,
                drawerContainerColor = MaridTokens.Sidebar,
                windowInsets = WindowInsets(0, 0, 0, 0),
                modifier = Modifier
                    .width(drawerWidth)
                    .fillMaxHeight()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
            ) {
                drawerContent()
            }
        },
        content = content,
    )
}

/**
 * The sidebar sheet content (ProjectSidebar). Data comes in via params; every
 * interactive control fires a callback, nothing is wired to RPC here.
 */
@Composable
fun MaridSidebarDrawer(
    modifier: Modifier = Modifier,
    strings: MaridSidebarStrings = MaridSidebarStrings(),
    newTaskShortcutLabel: String? = null,
    newTaskEnabled: Boolean = true,
    automationsActive: Boolean = false,
    pluginStoreActive: Boolean = false,
    projects: List<MaridSidebarProject> = emptyList(),
    pinnedTasks: List<MaridSidebarTask> = emptyList(),
    looseTasks: List<MaridSidebarTask> = emptyList(),
    archiveGroups: List<MaridArchiveGroup> = emptyList(),
    offlineWorkspaces: List<MaridOfflineWorkspaceRow> = emptyList(),
    homeOnlyWorkspaces: List<MaridHomeOnlyWorkspace>? = null,
    loading: Boolean = false,
    error: String? = null,
    hasExpandedProjects: Boolean = false,
    profileName: String,
    profileInitial: String,
    profileLoggedIn: Boolean = false,
    planBadgeLabel: String? = null,
    onNewTask: () -> Unit = {},
    onOpenAutomations: (() -> Unit)? = null,
    onOpenPluginStore: (() -> Unit)? = null,
    onOpenSearch: () -> Unit = {},
    onOpenViewFilter: () -> Unit = {},
    onToggleAllProjects: (() -> Unit)? = null,
    onAddProject: () -> Unit = {},
    onCreateTask: () -> Unit = {},
    onSelectTask: (MaridSidebarTask) -> Unit = {},
    onTaskFiles: (MaridSidebarTask) -> Unit = {},
    onTaskArchive: (MaridSidebarTask) -> Unit = {},
    onToggleProject: (MaridSidebarProject, Boolean) -> Unit = { _, _ -> },
    onProjectMore: (MaridSidebarProject) -> Unit = {},
    onProjectFiles: (MaridSidebarProject) -> Unit = {},
    onProjectCreateTask: (MaridSidebarProject) -> Unit = {},
    onArchiveGroupMenu: (MaridArchiveGroup) -> Unit = {},
    onArchiveTaskDelete: (MaridArchiveTask) -> Unit = {},
    onArchiveTaskUnarchive: (MaridArchiveTask) -> Unit = {},
    onOfflineWorkspaceClick: (MaridOfflineWorkspaceRow) -> Unit = {},
    onHomeWorkspaceClick: (MaridHomeOnlyWorkspace) -> Unit = {},
    onProfileClick: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaridTokens.Sidebar),
    ) {
        // h-12 drag strip above the task list region (desktop parity).
        Spacer(Modifier.height(48.dp))
        // ProjectSidebarNavigation: flex flex-col gap-1 px-2 py-3
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            MaridNewTaskRow(
                label = strings.newTask,
                shortcutLabel = newTaskShortcutLabel,
                enabled = newTaskEnabled,
                onClick = onNewTask,
            )
            if (onOpenAutomations != null) {
                MaridNavRow(
                    icon = MaridIcons.CalendarClock,
                    label = strings.automations,
                    active = automationsActive,
                    onClick = onOpenAutomations,
                )
            }
            if (onOpenPluginStore != null) {
                MaridNavRow(
                    icon = MaridIcons.Blocks,
                    label = strings.pluginMarketplace,
                    active = pluginStoreActive,
                    onClick = onOpenPluginStore,
                )
            }
        }
        // Task list region: border-t border-border/50 pt-3
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(MaridBorderHalf),
            )
            Spacer(Modifier.height(12.dp)) // pt-3
            // ProjectSidebarListHeader: px-2 pb-2 gap-1
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = strings.listTitle,
                    color = MaridTokens.ForegroundSubtle,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .padding(horizontal = 10.dp), // px-2.5
                )
                MaridRowGhostIconButton(
                    icon = MaridIcons.Search,
                    contentDescription = strings.search,
                    onClick = onOpenSearch,
                    iconSize = 14.dp, // Search size-3.5
                )
                MaridRowGhostIconButton(
                    icon = MaridIcons.ListFilter,
                    contentDescription = strings.filterView,
                    onClick = onOpenViewFilter,
                    iconSize = 14.dp, // ListFilter size-3.5
                )
            }
            // Scroll list: flex flex-col gap-3 pb-3
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (homeOnlyWorkspaces != null) {
                    MaridHomeOnlySection(
                        title = strings.homeOnlyWorkspaces,
                        workspaces = homeOnlyWorkspaces,
                        emptyLabel = strings.noSwitchableWorkspaces,
                        loadingLabel = strings.loading,
                        onClick = onHomeWorkspaceClick,
                    )
                } else {
                    if (pinnedTasks.isNotEmpty()) {
                        // Pinned block: px-2 + h3 px-2.5 py-1
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                        ) {
                            Text(
                                text = strings.pinnedSection,
                                color = MaridTokens.ForegroundSubtlest,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                            pinnedTasks.forEach { task ->
                                // ProjectTaskRow wrapper: py-px
                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 1.dp),
                                ) {
                                    MaridTaskRow(
                                        task = task,
                                        showFilesButton = true, // pinned rows only
                                        onSelect = { onSelectTask(task) },
                                        onFiles = { onTaskFiles(task) },
                                        onArchive = { onTaskArchive(task) },
                                    )
                                }
                            }
                        }
                    }
                    projects.filter { it.pinned }.forEach { project ->
                        MaridProjectRow(
                            project = project,
                            strings = strings,
                            onToggle = onToggleProject,
                            onMore = onProjectMore,
                            onFiles = onProjectFiles,
                            onCreateTask = onProjectCreateTask,
                            onSelectTask = onSelectTask,
                            onTaskFiles = onTaskFiles,
                            onTaskArchive = onTaskArchive,
                        )
                    }
                    // Purpose sections (WorkspacePurposeSection): Projects + Tasks
                    MaridPurposeSection(
                        title = strings.projectsSection,
                        expanded = true,
                        onToggleExpand = null, // collapse preference stays with the host
                        collapseAction = onToggleAllProjects?.let {
                            {
                                MaridRowGhostIconButton(
                                    icon = if (hasExpandedProjects) {
                                        MaridIcons2a.ListChevronsDownUp
                                    } else {
                                        MaridIcons2a.ListChevronsUpDown
                                    },
                                    contentDescription = strings.filterView,
                                    onClick = it,
                                )
                            }
                        },
                        addAction = {
                            MaridRowGhostIconButton(
                                icon = MaridIcons.Plus,
                                contentDescription = strings.newProject,
                                onClick = onAddProject,
                            )
                        },
                        content = {
                            projects.filter { !it.pinned }.forEach { project ->
                                MaridProjectRow(
                                    project = project,
                                    strings = strings,
                                    onToggle = onToggleProject,
                                    onMore = onProjectMore,
                                    onFiles = onProjectFiles,
                                    onCreateTask = onProjectCreateTask,
                                    onSelectTask = onSelectTask,
                                    onTaskFiles = onTaskFiles,
                                    onTaskArchive = onTaskArchive,
                                )
                            }
                        },
                    )
                    MaridPurposeSection(
                        title = strings.tasksSection,
                        expanded = true,
                        onToggleExpand = null,
                        collapseAction = null,
                        addAction = {
                            MaridRowGhostIconButton(
                                icon = MaridIcons.MessageCirclePlus,
                                contentDescription = strings.newTaskInProject,
                                onClick = onCreateTask,
                            )
                        },
                        content = {
                            // 任务分区：pl-2 内缩
                            Column(Modifier.padding(start = 8.dp)) {
                                if (looseTasks.isEmpty()) {
                                    Text(
                                        text = strings.noTasks,
                                        color = MaridTokens.ForegroundSubtlest,
                                        fontSize = 14.sp,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 34.dp, vertical = 8.dp), // px-8.5 py-2
                                    )
                                } else {
                                    looseTasks.forEach { task ->
                                        Column(
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 1.dp), // py-px
                                        ) {
                                            MaridTaskRow(
                                                task = task,
                                                showFilesButton = false,
                                                onSelect = { onSelectTask(task) },
                                                onFiles = { onTaskFiles(task) },
                                                onArchive = { onTaskArchive(task) },
                                            )
                                        }
                                    }
                                }
                            }
                        },
                    )
                    offlineWorkspaces.forEach { row ->
                        MaridOfflineWorkspaceRow(row = row, onClick = { onOfflineWorkspaceClick(row) })
                    }
                }
                archiveGroups.forEach { group ->
                    MaridArchiveGroupCard(
                        group = group,
                        strings = strings,
                        onMenu = { onArchiveGroupMenu(group) },
                        onDelete = onArchiveTaskDelete,
                        onUnarchive = onArchiveTaskUnarchive,
                    )
                }
                if (loading && homeOnlyWorkspaces == null) {
                    // p.px-4 text-ui-sm text-foreground-subtle
                    Text(
                        text = strings.loading,
                        color = MaridTokens.ForegroundSubtle,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    )
                }
                if (error != null && homeOnlyWorkspaces == null) {
                    Text(
                        text = error,
                        color = MaridTokens.Destructive,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    )
                }
            }
        }
        // WorkspaceSidebarFooter className="pr-3": px-4 pt-2 pb-4 + pr-3
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 12.dp, top = 8.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp), // gap-2.5
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp), // gap-2
            ) {
                MaridFooterProfileButton(
                    name = if (profileLoggedIn) profileName else strings.notLoggedIn,
                    initial = profileInitial,
                    loggedIn = profileLoggedIn,
                    planBadgeLabel = planBadgeLabel,
                    onClick = onProfileClick,
                )
                MaridFooterIconButton(
                    icon = MaridIcons.Settings,
                    contentDescription = strings.settings,
                    onClick = onOpenSettings,
                )
            }
        }
    }
}

// --------------------------------------------------------------- nav rows

/** NewTaskButtonGroup: h-8 rounded-lg pl/pr-2.5 gap-2 hover:bg-surface-hover. */
@Composable
private fun MaridNewTaskRow(
    label: String,
    shortcutLabel: String?,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val contentColor =
        if (enabled) MaridTokens.Foreground else MaridTokens.ForegroundSubtlest
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp) // h-8
            .clip(RoundedCornerShape(8.dp)) // rounded-lg
            .background(if (enabled && pressed) MaridTokens.SurfaceHover else Color.Transparent)
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
            ) { onClick() }
            .padding(horizontal = 10.dp), // pl-2.5 pr-2.5
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp), // gap-2
        ) {
            Icon(
                imageVector = MaridIcons.MessageCirclePlus,
                contentDescription = null,
                modifier = Modifier.size(16.dp), // h-4 w-4
                tint = contentColor,
            )
            Text(
                text = label,
                color = contentColor,
                fontSize = 14.sp, // text-ui-base
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (shortcutLabel != null) {
            // ml-auto text-ui-xs font-normal text-foreground-subtlest
            Text(
                text = shortcutLabel,
                color = MaridTokens.ForegroundSubtlest,
                fontSize = 10.sp,
                maxLines = 1,
            )
        }
    }
}

/**
 * ProjectSidebarNavigation secondary rows: Button ghost size lg (h-8 px-2.5) with
 * "w-full justify-start gap-2", active -> bg-selected text-foreground.
 */
@Composable
private fun MaridNavRow(
    icon: ImageVector,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val background = when {
        active -> MaridTokens.Selected
        pressed -> MaridTokens.SurfaceHover
        else -> Color.Transparent
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp) // size lg -> h-8
            .clip(RoundedCornerShape(8.dp)) // rounded-lg
            .background(background)
            .clickable(interactionSource = interaction, indication = null) { onClick() }
            .padding(horizontal = 10.dp), // px-2.5
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp), // gap-2 override
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(16.dp), // size-4
            tint = MaridTokens.Foreground,
        )
        Text(
            text = label,
            color = MaridTokens.Foreground,
            fontSize = 14.sp, // text-ui-base
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

// ------------------------------------------------------------ purpose section

/**
 * WorkspacePurposeSection header: ml-2 mr-1 h-7; trigger px-2.5 gap-1 text-ui-base
 * font-medium text-foreground-subtlest hover:text-foreground; chevron size-3.5
 * visible on touch ([@media(hover:none)]:opacity-100); action buttons always
 * visible on touch.
 */
@Composable
private fun MaridPurposeSection(
    title: String,
    expanded: Boolean,
    onToggleExpand: (() -> Unit)?,
    collapseAction: (@Composable () -> Unit)?,
    addAction: (@Composable () -> Unit)?,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 4.dp) // ml-2 mr-1
                .height(28.dp), // h-7
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(28.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .clickable(
                        interactionSource = interaction,
                        indication = null,
                        enabled = onToggleExpand != null,
                    ) { onToggleExpand?.invoke() }
                    .padding(horizontal = 10.dp), // px-2.5
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp), // gap-1
            ) {
                Text(
                    text = title,
                    color = if (pressed) MaridTokens.Foreground else MaridTokens.ForegroundSubtlest,
                    fontSize = 14.sp, // text-ui-base
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                // chevron: size-3.5 shrink-0, visible on touch
                Icon(
                    imageVector = if (expanded) MaridIcons.ChevronDown else MaridIcons.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = if (pressed) MaridTokens.Foreground else MaridTokens.ForegroundSubtlest,
                )
            }
            if (collapseAction != null || addAction != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    collapseAction?.invoke()
                    addAction?.invoke()
                }
            }
        }
        content()
    }
}

// ---------------------------------------------------------------- project row

/**
 * ProjectNode + ProjectNodeContent.
 * Container: min-w-0 rounded-xl (12dp).
 * Row: ml-2 h-8 gap-1 rounded-lg pr-1 hover:bg-surface-hover.
 * Title button: h-8 gap-2 pl-2.5 pr-1 text-left text-foreground (hover bg stays
 * transparent; the ROW carries the hover surface).
 */
@Composable
private fun MaridProjectRow(
    project: MaridSidebarProject,
    strings: MaridSidebarStrings,
    onToggle: (MaridSidebarProject, Boolean) -> Unit,
    onMore: (MaridSidebarProject) -> Unit,
    onFiles: (MaridSidebarProject) -> Unit,
    onCreateTask: (MaridSidebarProject) -> Unit,
    onSelectTask: (MaridSidebarTask) -> Unit,
    onTaskFiles: (MaridSidebarTask) -> Unit,
    onTaskArchive: (MaridSidebarTask) -> Unit,
) {
    val titleInteraction = remember { MutableInteractionSource() }
    val titlePressed by titleInteraction.collectIsPressedAsState()
    val rowBackground =
        if (titlePressed) MaridTokens.SurfaceHover else Color.Transparent
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)), // rounded-xl project section container
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp) // ml-2 (margin, outside the hover surface)
                .height(32.dp) // h-8
                .clip(RoundedCornerShape(8.dp)) // rounded-lg
                .background(rowBackground)
                .padding(end = 4.dp), // pr-1
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp), // gap-1
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(
                        interactionSource = titleInteraction,
                        indication = null,
                        onClickLabel = project.name,
                    ) { onToggle(project, !project.expanded) }
                    .alpha(if (project.titleDimmed) 0.5f else 1f)
                    .padding(start = 10.dp, end = 4.dp), // pl-2.5 pr-1
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp), // gap-2
            ) {
                // Icon slot: flex size-4 items-center justify-center
                Box(
                    modifier = Modifier.size(16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    if (project.isRemote) {
                        MaridRemoteStateIcon(status = project.status, project = true)
                    } else {
                        MaridProjectFolderIcon(
                            rootCount = project.rootCount,
                            open = project.expanded,
                            size = 16.dp,
                            tint = MaridTokens.ForegroundSubtle,
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // name: truncate text-ui-base text-foreground-subtle
                    Text(
                        text = project.name,
                        color = MaridTokens.ForegroundSubtle,
                        fontSize = 14.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .alignByBaseline(),
                    )
                    // device label: max-w-24 truncate text-ui-sm text-remote-foreground-muted
                    if (project.deviceLabel != null) {
                        Text(
                            text = project.deviceLabel,
                            color = MaridRemoteForegroundMuted,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .widthIn(max = 96.dp)
                                .alignByBaseline(),
                        )
                    }
                }
                // unread dot: size-1.5 rounded-full bg-icon-blue (collapsed + online)
                if (!project.expanded && project.online && project.hasUnread) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(MaridIconBlue),
                    )
                }
                // status label (offline/unresolved): text-ui-sm tone-colored
                if (!project.online && project.statusLabel != null) {
                    Text(
                        text = project.statusLabel,
                        color = maridRemoteStatusForeground(project.status, projectIcon = true),
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            // Actions: flex shrink-0 items-center gap-1, opacity-100 on touch
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (project.online) {
                    MaridRowGhostIconButton(
                        icon = MaridIcons2a.ListTree,
                        contentDescription = strings.showFiles,
                        onClick = { onFiles(project) },
                    )
                    MaridRowGhostIconButton(
                        icon = MaridIcons.MessageCirclePlus,
                        contentDescription = strings.newTaskInProject,
                        onClick = { onCreateTask(project) },
                    )
                }
                MaridRowGhostIconButton(
                    icon = MaridIcons.Ellipsis,
                    contentDescription = strings.more,
                    onClick = { onMore(project) },
                )
            }
        }
        if (project.expanded) {
            // CollapsibleContent: pl-2 pt-1
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 8.dp, top = 4.dp),
            ) {
                project.tasks.forEach { task ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 1.dp), // py-px
                    ) {
                        MaridTaskRow(
                            task = task,
                            showFilesButton = false,
                            onSelect = { onSelectTask(task) },
                            onFiles = { onTaskFiles(task) },
                            onArchive = { onTaskArchive(task) },
                        )
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ task row

/**
 * TaskListItem (default + timeline variants):
 * li: flex gap-2 rounded-lg pl-2.5 pr-1 py-1 items-center,
 * isActive -> bg-selected, hover -> bg-surface-hover.
 */
@Composable
private fun MaridTaskRow(
    task: MaridSidebarTask,
    showFilesButton: Boolean,
    onSelect: () -> Unit,
    onFiles: () -> Unit,
    onArchive: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val background = when {
        task.isActive -> MaridTokens.Selected
        pressed -> MaridTokens.SurfaceHover
        else -> Color.Transparent
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp)) // rounded-lg
            .background(background)
            .clickable(interactionSource = interaction, indication = null) { onSelect() }
            .padding(start = 10.dp, end = 4.dp, top = 4.dp, bottom = 4.dp), // pl-2.5 pr-1 py-1
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp), // gap-2
    ) {
        // Leading slot: relative flex size-4 items-center justify-center
        Box(
            modifier = Modifier.size(16.dp),
            contentAlignment = Alignment.Center,
        ) {
            when {
                task.isError ->
                    Box(
                        Modifier
                            .size(6.dp) // h-1.5 w-1.5
                            .clip(CircleShape)
                            .background(MaridTokens.Destructive),
                    )
                task.isUnread ->
                    Box(
                        Modifier
                            .size(7.dp) // size-[7px]
                            .clip(CircleShape)
                            .background(MaridIconBlue),
                    )
                task.isRunning -> MaridSpinningLoader(16.dp, MaridTokens.ForegroundSubtle)
                else -> Unit // 无状态只保留对齐槽
            }
        }
        if (task.timeline) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp), // gap-0.5
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp), // gap-1.5
                ) {
                    Text(
                        text = task.title,
                        color = MaridTokens.Foreground,
                        fontSize = 14.sp, // text-ui-base text-foreground
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier
                            .weight(1f)
                            .maridTitleRightFade(),
                    )
                    MaridChangeSummary(task)
                }
                Row(
                    modifier = Modifier.height(24.dp), // h-6
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = task.workspaceLabel.orEmpty(),
                        color = MaridTokens.ForegroundSubtle,
                        fontSize = 14.sp, // text-ui-base text-foreground-subtle
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (task.deviceLabel != null) {
                        Text(
                            text = "·",
                            color = MaridTokens.ForegroundSubtlest,
                            fontSize = 14.sp,
                        )
                        Text(
                            text = task.deviceLabel,
                            color = MaridTokens.ForegroundSubtle,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (task.timeLabel != null) {
                        Text(
                            text = task.timeLabel,
                            color = MaridTokens.ForegroundSubtle,
                            fontSize = 14.sp,
                            maxLines = 1,
                            modifier = Modifier.padding(end = 4.dp), // mr-1
                        )
                    }
                }
            }
        } else {
            Text(
                text = task.title,
                color = MaridTokens.Foreground,
                fontSize = 14.sp, // text-ui-base text-foreground
                maxLines = 1,
                softWrap = false,
                modifier = Modifier
                    .weight(1f)
                    .maridTitleRightFade(),
            )
            MaridChangeSummary(task)
            if (showFilesButton) {
                MaridRowGhostIconButton(
                    icon = MaridIcons2a.ListTree,
                    contentDescription = "Show files",
                    onClick = onFiles,
                    tint = MaridTokens.Foreground,
                )
            }
            MaridRowGhostIconButton(
                icon = MaridIcons2a.Trash2,
                contentDescription = "Archive",
                onClick = onArchive,
                tint = MaridTokens.Foreground,
            )
            if (task.timeLabel != null) {
                Row(
                    modifier = Modifier.padding(end = 2.dp), // mr-0.5
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp), // gap-1
                ) {
                    if (task.isCron) {
                        Icon(
                            imageVector = MaridIcons.Clock,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp), // size-3.5
                            tint = MaridTokens.ForegroundSubtle,
                        )
                    }
                    Text(
                        text = task.timeLabel,
                        color = MaridTokens.ForegroundSubtle,
                        fontSize = 12.sp, // text-ui-sm text-foreground-subtle
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

/** changeSummaryNode: +N text-diff-added / -N text-diff-removed (text-ui-base). */
@Composable
private fun MaridChangeSummary(task: MaridSidebarTask) {
    if (task.addedLines > 0) {
        Text(
            text = "+${task.addedLines}",
            color = MaridTokens.DiffAdded,
            fontSize = 14.sp,
            maxLines = 1,
        )
    }
    if (task.removedLines > 0) {
        Text(
            text = "-${task.removedLines}",
            color = MaridTokens.DiffRemoved,
            fontSize = 14.sp,
            maxLines = 1,
            modifier = Modifier.padding(start = 4.dp), // ml-1
        )
    }
}

// -------------------------------------------------------- offline/home rows

/**
 * OfflineRemoteProjectRow: flex w-full items-center gap-2 rounded-lg px-3 py-2
 * text-left text-ui-sm hover:bg-surface-hover (tone-colored text).
 */
@Composable
private fun MaridOfflineWorkspaceRow(row: MaridOfflineWorkspaceRow, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val tint = maridRemoteStatusForeground(row.status, projectIcon = false)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(if (pressed) MaridTokens.SurfaceHover else Color.Transparent)
            .clickable(interactionSource = interaction, indication = null) { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MaridRemoteStateIcon(status = row.status, project = false)
        Text(
            text = row.label,
            color = tint,
            fontSize = 12.sp, // text-ui-sm
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = row.statusLabel,
            color = tint,
            fontSize = 12.sp,
            maxLines = 1,
        )
    }
}

/**
 * HomeOnlyWorkspaceRows section: px-2; h3 px-2.5 py-1 text-foreground-subtlest;
 * rows h-8 w-full gap-2 rounded-lg px-2.5 text-ui-base text-foreground-subtle
 * hover:bg-surface-hover; remote status span text-ui-sm tone-colored.
 */
@Composable
private fun MaridHomeOnlySection(
    title: String,
    workspaces: List<MaridHomeOnlyWorkspace>,
    emptyLabel: String,
    loadingLabel: String,
    onClick: (MaridHomeOnlyWorkspace) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp), // section px-2
    ) {
        Text(
            text = title,
            color = MaridTokens.ForegroundSubtlest,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 4.dp), // px-2.5 py-1
        )
        if (workspaces.isEmpty()) {
            Text(
                text = emptyLabel,
                color = MaridTokens.ForegroundSubtlest,
                fontSize = 14.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 8.dp), // px-2.5 py-2
            )
            return@Column
        }
        workspaces.forEach { workspace ->
            val interaction = remember { MutableInteractionSource() }
            val pressed by interaction.collectIsPressedAsState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(32.dp) // h-8
                    .clip(RoundedCornerShape(8.dp)) // rounded-lg
                    .background(if (pressed) MaridTokens.SurfaceHover else Color.Transparent)
                    .clickable(interactionSource = interaction, indication = null) {
                        onClick(workspace)
                    }
                    .padding(horizontal = 10.dp), // px-2.5
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp), // gap-2
            ) {
                if (workspace.isRemote) {
                    MaridRemoteStateIcon(status = workspace.status, project = true)
                } else {
                    Icon(
                        imageVector = MaridIcons.Folder,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaridTokens.ForegroundSubtle,
                    )
                }
                Text(
                    text = workspace.label,
                    color = MaridTokens.ForegroundSubtle,
                    fontSize = 14.sp, // text-ui-base
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (workspace.isRemote && workspace.statusLabel != null) {
                    Text(
                        text = workspace.statusLabel,
                        color = maridRemoteStatusForeground(workspace.status, projectIcon = false),
                        fontSize = 12.sp, // text-ui-sm
                        maxLines = 1,
                    )
                }
            }
        }
    }
}

// ----------------------------------------------------------- archive card

/**
 * ArchiveGroupCard: header gap-2; toggle ghost default (h-7 px-0) font-medium,
 * Folder size-4 text-foreground-subtle, ChevronRight size-3.5 rotate-90 open;
 * count text-ui-sm text-foreground-subtle; menu ghost icon-sm Ellipsis size-4.
 * Open content: pt-3; card rounded-xl border border-border bg-surface px-4,
 * rows divide-y divide-border py-3 gap-x-3.
 */
@Composable
private fun MaridArchiveGroupCard(
    group: MaridArchiveGroup,
    strings: MaridSidebarStrings,
    onMenu: () -> Unit,
    onDelete: (MaridArchiveTask) -> Unit,
    onUnarchive: (MaridArchiveTask) -> Unit,
) {
    var open by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp), // gap-2
        ) {
            val toggleInteraction = remember { MutableInteractionSource() }
            val togglePressed by toggleInteraction.collectIsPressedAsState()
            Row(
                modifier = Modifier
                    .weight(1f)
                    .height(28.dp) // Button default h-7
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (togglePressed) MaridTokens.SurfaceHover else Color.Transparent)
                    .clickable(
                        interactionSource = toggleInteraction,
                        indication = null,
                    ) { open = !open },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp), // gap-2 override
            ) {
                Icon(
                    imageVector = MaridIcons.Folder,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp), // size-4
                    tint = MaridTokens.ForegroundSubtle,
                )
                Text(
                    text = group.name,
                    color = MaridTokens.Foreground,
                    fontSize = 14.sp, // text-ui-base
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Icon(
                    imageVector = MaridIcons.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier
                        .size(14.dp) // size-3.5
                        .rotate(if (open) 90f else 0f),
                    tint = MaridTokens.ForegroundSubtle,
                )
            }
            Text(
                text = strings.archiveCount(group.tasks.size),
                color = MaridTokens.ForegroundSubtle,
                fontSize = 12.sp, // text-ui-sm
                maxLines = 1,
            )
            MaridRowGhostIconButton(
                icon = MaridIcons.Ellipsis,
                contentDescription = strings.archiveGroupActions(group.name),
                onClick = onMenu,
                iconSize = 16.dp, // Ellipsis size-4 here
            )
        }
        if (open) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp) // data-[state=open]:pt-3
                    .clip(RoundedCornerShape(12.dp)) // rounded-xl
                    .border(1.dp, MaridTokens.Border, RoundedCornerShape(12.dp))
                    .background(MaridTokens.Surface)
                    .padding(horizontal = 16.dp), // px-4
            ) {
                group.tasks.forEachIndexed { index, task ->
                    if (index > 0) {
                        // divide-y divide-border
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(MaridTokens.Border),
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp), // py-3
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp), // gap-x-3
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = task.title,
                                color = MaridTokens.Foreground,
                                fontSize = 14.sp, // text-ui-base font-medium
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = task.dateLabel,
                                color = MaridTokens.ForegroundSubtle,
                                fontSize = 12.sp, // text-ui-sm text-foreground-subtle
                                maxLines = 1,
                            )
                        }
                        MaridRowGhostIconButton(
                            icon = MaridIcons2a.Trash2,
                            contentDescription = strings.archiveDeleteTask(task.title),
                            onClick = { onDelete(task) },
                            iconSize = 16.dp, // Trash2 size-4 text-foreground-subtle
                        )
                        // Unarchive: variant secondary size default (h-7 px-2) rounded-md
                        val unarchiveInteraction = remember { MutableInteractionSource() }
                        val unarchivePressed by unarchiveInteraction.collectIsPressedAsState()
                        Box(
                            modifier = Modifier
                                .height(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    if (unarchivePressed) {
                                        MaridTokens.Secondary.copy(alpha = 0.8f)
                                    } else {
                                        MaridTokens.Secondary
                                    },
                                )
                                .clickable(
                                    interactionSource = unarchiveInteraction,
                                    indication = null,
                                ) { onUnarchive(task) }
                                .padding(horizontal = 8.dp), // px-2
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = strings.unarchive,
                                color = MaridTokens.Foreground,
                                fontSize = 14.sp, // text-ui-base
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------ footer

/**
 * Footer profile trigger: Button ghost size lg with "min-w-0 flex-1 justify-start
 * gap-2 overflow-hidden rounded-tl-2xl rounded-bl-2xl border-0 pl-0".
 * Avatar size-8 (32dp) ring border-border; fallback bg-background text-foreground.
 * Name text-ui-base font-semibold text-foreground.
 * Plan badge (WorkspaceSidebarFooterPlanBadge): rounded-full border border-border
 * bg-surface px-1 py-px text-ui-xs font-medium text-foreground-subtle max-w-20.
 */
@Composable
private fun MaridFooterProfileButton(
    name: String,
    initial: String,
    loggedIn: Boolean,
    planBadgeLabel: String?,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp) // size lg -> h-8
            .clip(
                RoundedCornerShape(
                    topStart = 16.dp, // rounded-tl-2xl
                    bottomStart = 16.dp, // rounded-bl-2xl
                    topEnd = 8.dp,
                    bottomEnd = 8.dp,
                ),
            )
            .background(if (pressed) MaridTokens.SurfaceHover else Color.Transparent)
            .clickable(interactionSource = interaction, indication = null) { onClick() }
            .padding(end = 10.dp), // px-2.5 with pl-0
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp), // gap-2
    ) {
        // Avatar: relative flex size-8 rounded-full + after:border-border ring
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaridTokens.Background) // fallback bg-background
                .border(1.dp, MaridTokens.Border, CircleShape), // after:border-border
            contentAlignment = Alignment.Center,
        ) {
            if (loggedIn && initial.isNotBlank()) {
                Text(
                    text = initial,
                    color = MaridTokens.Foreground, // text-foreground
                    fontSize = 14.sp, // text-ui-base
                    fontWeight = FontWeight.Medium,
                )
            } else {
                Icon(
                    imageVector = MaridIcons.User,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp), // size-4
                    tint = MaridTokens.Foreground,
                )
            }
        }
        Text(
            text = name,
            color = MaridTokens.Foreground,
            fontSize = 14.sp, // text-ui-base font-semibold
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (planBadgeLabel != null) {
            Row(
                modifier = Modifier
                    .widthIn(max = 80.dp) // max-w-20
                    .clip(RoundedCornerShape(50)) // rounded-full
                    .border(1.dp, MaridTokens.Border, RoundedCornerShape(50))
                    .background(MaridTokens.Surface)
                    .padding(horizontal = 4.dp, vertical = 1.dp), // px-1 py-px
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = planBadgeLabel,
                    color = MaridTokens.ForegroundSubtle,
                    fontSize = 10.sp, // text-ui-xs font-medium
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** Footer settings trigger: ghost icon-lg (size-8 32dp rounded-lg), Settings size-4. */
@Composable
private fun MaridFooterIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier = Modifier
            .size(32.dp) // icon-lg -> size-8
            .clip(RoundedCornerShape(8.dp)) // rounded-lg
            .background(if (pressed) MaridTokens.SurfaceHover else Color.Transparent)
            .clickable(interactionSource = interaction, indication = null) { onClick() },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(16.dp), // size-4
            tint = MaridTokens.Foreground,
        )
    }
}
