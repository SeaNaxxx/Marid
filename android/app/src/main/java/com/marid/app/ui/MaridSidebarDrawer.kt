package com.marid.app.ui

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

enum class MaridRemoteStatus { Online, Connecting, Offline, Pending, Failed }

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

internal val MaridRemoteForegroundMuted = Color(0xFFB3B3B3)

internal val MaridIconBlue = MaridTokens.Sky600

internal val MaridWarningSurface = Color(0x29F0B100)

internal val MaridProgressForeground = MaridTokens.Blue300

internal val MaridWarningForeground = MaridTokens.Yellow400

internal val MaridSuccessForeground = MaridTokens.Green300

internal val MaridDangerForeground = MaridTokens.Red300

internal val MaridBorderHalf = MaridTokens.Border.copy(alpha = 0.5f)

// projectIcon=true: online intentionally renders the muted neutral instead of success green.
internal fun maridRemoteStatusForeground(status: MaridRemoteStatus, projectIcon: Boolean): Color =
    when (status) {
        MaridRemoteStatus.Online ->
            if (projectIcon) MaridRemoteForegroundMuted else MaridSuccessForeground
        MaridRemoteStatus.Connecting -> MaridProgressForeground
        MaridRemoteStatus.Offline -> MaridRemoteForegroundMuted // neutral tone
        MaridRemoteStatus.Pending -> MaridWarningForeground
        MaridRemoteStatus.Failed -> MaridDangerForeground
    }

// Icons missing from MaridIcons, drawn with exact lucide geometry.
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

    internal val Cloud: ImageVector by lazy {
        builder("Cloud").apply {
            stroke {
                moveTo(17.5f, 19f)
                lineTo(9f, 19f)
                arcTo(7f, 7f, 0f, true, true, 15.71f, 10f)
                lineTo(17.5f, 10f)
                arcTo(4.5f, 4.5f, 0f, true, true, 17.5f, 19f)
                close()
            }
        }.build()
    }

    internal val CloudOff: ImageVector by lazy {
        builder("CloudOff").apply {
            stroke {
                moveTo(10.94f, 5.274f)
                arcTo(7f, 7f, 0f, false, true, 15.71f, 10f)
                lineTo(17.5f, 10f)
                arcTo(4.5f, 4.5f, 0f, false, true, 21.722f, 16.057f)
            }
            stroke {
                moveTo(18.796f, 18.81f)
                arcTo(4.5f, 4.5f, 0f, false, true, 17.5f, 19f)
                lineTo(9f, 19f)
                arcTo(7f, 7f, 0f, false, true, 5.79f, 5.78f)
            }
            stroke {
                moveTo(2f, 2f)
                lineTo(22f, 22f)
            }
        }.build()
    }

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
                arcTo(7f, 7f, 0f, true, true, 15.71f, 8f)
                lineTo(17.5f, 8f)
                arcTo(4.5f, 4.5f, 0f, true, true, 17.5f, 17f)
                lineTo(15.858f, 17f)
            }
        }.build()
    }

    internal val FolderOpen: ImageVector by lazy {
        builder("FolderOpen").apply {
            stroke {
                moveTo(6f, 14f)
                lineTo(7.5f, 11.1f)
                arcTo(2f, 2f, 0f, false, true, 9.24f, 10f)
                lineTo(20f, 10f)
                arcTo(2f, 2f, 0f, false, true, 21.94f, 12.5f)
                lineTo(20.4f, 18.5f)
                arcTo(2f, 2f, 0f, false, true, 18.45f, 20f)
                lineTo(4f, 20f)
                arcTo(2f, 2f, 0f, false, true, 2f, 18f)
                lineTo(2f, 5f)
                arcTo(2f, 2f, 0f, false, true, 4f, 3f)
                lineTo(7.9f, 3f)
                arcTo(2f, 2f, 0f, false, true, 9.59f, 3.9f)
                lineTo(10.4f, 5.1f)
                arcTo(2f, 2f, 0f, false, false, 12.07f, 6f)
                lineTo(18f, 6f)
                arcTo(2f, 2f, 0f, false, true, 20f, 8f)
                lineTo(20f, 10f)
            }
        }.build()
    }

    internal val Folders: ImageVector by lazy {
        builder("Folders").apply {
            stroke {
                moveTo(20f, 5f)
                arcTo(2f, 2f, 0f, false, true, 22f, 7f)
                lineTo(22f, 14f)
                arcTo(2f, 2f, 0f, false, true, 20f, 16f)
                lineTo(9f, 16f)
                arcTo(2f, 2f, 0f, false, true, 7f, 14f)
                lineTo(7f, 5f)
                arcTo(2f, 2f, 0f, false, true, 9f, 3f)
                lineTo(11.5f, 3f)
                arcTo(1.5f, 1.5f, 0f, false, true, 12.7f, 3.6f)
                lineTo(13.3f, 4.4f)
                arcTo(1.5f, 1.5f, 0f, false, false, 14.5f, 5f)
                close()
            }
            stroke {
                moveTo(3f, 8.268f)
                arcTo(2f, 2f, 0f, false, false, 2f, 10.006f)
                lineTo(2f, 19f)
                arcTo(2f, 2f, 0f, false, false, 4f, 21f)
                lineTo(15f, 21f)
                arcTo(2f, 2f, 0f, false, false, 16.732f, 20f)
            }
        }.build()
    }

    internal val FoldersOpen: ImageVector by lazy {
        builder("FoldersOpen").apply {
            stroke {
                moveTo(10f, 12f)
                lineTo(11.2f, 10f)
                arcTo(2f, 2f, 0f, false, true, 12.92f, 9f)
                lineTo(21f, 9f)
                arcTo(1f, 1f, 0f, false, true, 21.97f, 10.25f)
                lineTo(20.87f, 14.5f)
                arcTo(2f, 2f, 0f, false, true, 18.94f, 16f)
                lineTo(9f, 16f)
                arcTo(2f, 2f, 0f, false, true, 7f, 14f)
                lineTo(7f, 5f)
                arcTo(2f, 2f, 0f, false, true, 9f, 3f)
                lineTo(11.5f, 3f)
                arcTo(1.5f, 1.5f, 0f, false, true, 12.7f, 3.6f)
                lineTo(13.3f, 4.4f)
                arcTo(1.5f, 1.5f, 0f, false, false, 14.5f, 5f)
                lineTo(20f, 5f)
                arcTo(2f, 2f, 0f, false, true, 22f, 7f)
                lineTo(22f, 9f)
            }
            stroke {
                moveTo(3f, 8.268f)
                arcTo(2f, 2f, 0f, false, false, 2f, 10.006f)
                lineTo(2f, 19f)
                arcTo(2f, 2f, 0f, false, false, 4f, 21f)
                lineTo(15f, 21f)
                arcTo(2f, 2f, 0f, false, false, 16.732f, 20f)
            }
        }.build()
    }

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
                arcTo(2f, 2f, 0f, false, false, 5f, 12f)
                lineTo(8f, 12f)
            }
            stroke {
                moveTo(3f, 5f)
                lineTo(3f, 17f)
                arcTo(2f, 2f, 0f, false, false, 5f, 19f)
                lineTo(8f, 19f)
            }
        }.build()
    }

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

    internal val Pin: ImageVector by lazy {
        builder("Pin").apply {
            stroke {
                moveTo(12f, 17f)
                lineTo(12f, 22f)
            }
            stroke {
                moveTo(9f, 10.76f)
                arcTo(2f, 2f, 0f, false, true, 7.89f, 12.55f)
                lineTo(6.11f, 13.45f)
                arcTo(2f, 2f, 0f, false, false, 5f, 15.24f)
                lineTo(5f, 16f)
                arcTo(1f, 1f, 0f, false, false, 6f, 17f)
                lineTo(18f, 17f)
                arcTo(1f, 1f, 0f, false, false, 19f, 16f)
                lineTo(19f, 15.24f)
                arcTo(2f, 2f, 0f, false, false, 17.89f, 13.45f)
                lineTo(16.11f, 12.55f)
                arcTo(2f, 2f, 0f, false, true, 15f, 10.76f)
                lineTo(15f, 7f)
                arcTo(1f, 1f, 0f, false, true, 16f, 6f)
                arcTo(2f, 2f, 0f, false, false, 16f, 2f)
                lineTo(8f, 2f)
                arcTo(2f, 2f, 0f, false, false, 8f, 6f)
                arcTo(1f, 1f, 0f, false, true, 9f, 7f)
                close()
            }
        }.build()
    }

    internal val Trash2: ImageVector by lazy {
        builder("Trash2").apply {
            stroke { moveTo(10f, 11f); lineTo(10f, 17f) }
            stroke { moveTo(14f, 11f); lineTo(14f, 17f) }
            stroke {
                moveTo(19f, 6f)
                lineTo(19f, 20f)
                arcTo(2f, 2f, 0f, false, true, 17f, 22f)
                lineTo(7f, 22f)
                arcTo(2f, 2f, 0f, false, true, 5f, 20f)
                lineTo(5f, 6f)
            }
            stroke { moveTo(3f, 6f); lineTo(21f, 6f) }
            stroke {
                moveTo(8f, 6f)
                lineTo(8f, 4f)
                arcTo(2f, 2f, 0f, false, true, 10f, 2f)
                lineTo(14f, 2f)
                arcTo(2f, 2f, 0f, false, true, 16f, 4f)
                lineTo(16f, 6f)
            }
        }.build()
    }
}

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

@Composable
private fun MaridProjectFolderIcon(rootCount: Int, open: Boolean, size: Dp, tint: Color) {
    val icon = when {
        rootCount > 1 -> if (open) MaridIcons2a.FoldersOpen else MaridIcons2a.Folders
        open -> MaridIcons2a.FolderOpen
        else -> MaridIcons.Folder
    }
    Icon(icon, contentDescription = null, modifier = Modifier.size(size), tint = tint)
}

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
        Spacer(Modifier.height(48.dp))
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
            Text(
                text = shortcutLabel,
                color = MaridTokens.ForegroundSubtlest,
                fontSize = 10.sp,
                maxLines = 1,
            )
        }
    }
}

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

// Title button hover bg stays transparent; the ROW carries the hover surface.
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
                if (!project.expanded && project.online && project.hasUnread) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(MaridIconBlue),
                    )
                }
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
