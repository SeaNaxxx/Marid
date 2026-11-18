package com.qodex.app.ui

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qodex.app.icons.QodexIcons
import com.qodex.app.rpc.QodexRpcClient
import com.qodex.app.rpc.ServiceChannels
import com.qodex.app.rpc.asArrayOrNull
import com.qodex.app.rpc.asBoolOrNull
import com.qodex.app.rpc.asIntOrNull
import com.qodex.app.rpc.asObjectOrNull
import com.qodex.app.rpc.asStringOrNull
import com.qodex.app.runtime.RuntimeBus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonObject

/**
 * Settings hub mirroring the upstream engine's settings structure
 * (Basics / Agent capabilities / Data and statistics). Every row routes to a
 * screen fed by a REAL engine channel call — sections whose backing service is
 * desktop-only say so instead of rendering fake data.
 */

enum class RowStatus { OK, WARNING, ERROR, NEUTRAL }

data class SectionItemRow(
    val key: String,
    val title: String,
    val subtitle: String? = null,
    val badge: String? = null,
    val status: RowStatus? = null,
    val enabled: Boolean? = null,
)

data class SectionState(
    val rows: List<SectionItemRow> = emptyList(),
    val error: String? = null,
)

typealias SectionLoader = suspend (QodexRpcClient, String) -> List<SectionItemRow>

private val EMPTY_ARGS = emptyList<kotlinx.serialization.json.JsonElement>()

// Engine service methods take positional args; a params object must be wrapped
// in a one-element array (ProxyChannel.fromService spreads them back).
private suspend fun callParams(
    rpc: QodexRpcClient,
    channel: String,
    command: String,
    params: JsonObject? = null,
): JsonObject? = rpc.call(channel, command, params) as? JsonObject

private suspend fun positional(
    rpc: QodexRpcClient,
    channel: String,
    command: String,
    args: List<kotlinx.serialization.json.JsonElement> = EMPTY_ARGS,
): JsonElementAlias? = runCatching {
    rpc.callPositional(channel, command, args)
}.getOrElse {
    RuntimeBus.append("settings $channel.$command failed: ${it.message}")
    null
}

typealias JsonElementAlias = kotlinx.serialization.json.JsonElement

private fun JsonObject.str(key: String): String =
    (this[key] as? kotlinx.serialization.json.JsonPrimitive)?.takeIf {
        it !is kotlinx.serialization.json.JsonNull
    }?.content.orEmpty()

private fun JsonObject.bool(key: String): Boolean? =
    (this[key] as? kotlinx.serialization.json.JsonPrimitive)?.takeIf {
        it !is kotlinx.serialization.json.JsonNull
    }?.booleanOrNull

private fun JsonObject.intVal(key: String): Int? =
    (this[key] as? kotlinx.serialization.json.JsonPrimitive)?.takeIf {
        it !is kotlinx.serialization.json.JsonNull
    }?.content?.toIntOrNull()

private fun JsonObject.arr(key: String): List<kotlinx.serialization.json.JsonElement>? =
    this[key].asArrayOrNull()

// ---------------------------------------------------------------------------
// Section loaders — one real RPC round trip per section
// ---------------------------------------------------------------------------

private suspend fun loadGeneral(rpc: QodexRpcClient, workspacePath: String): List<SectionItemRow> {
    val settings = callParams(rpc, ServiceChannels.SETTING, "get")
        ?: return listOf(SectionItemRow("error", "Engine settings unreachable", status = RowStatus.ERROR))
    return listOf(
        SectionItemRow("locale", "Locale", subtitle = settings.str("locale").ifEmpty { "system" }),
        SectionItemRow(
            "keepAwake",
            "Keep awake while running",
            subtitle = if (settings.bool("keepAwakeWhileRunning") == true) "on" else "off",
        ),
        SectionItemRow(
            "instructions",
            "Personal instructions",
            subtitle = runCatching {
                callParams(rpc, ServiceChannels.SETTING, "getUserInstructions")
            }.getOrNull()?.get("filePath")?.asStringOrNull()?.let { path ->
                if (path.isNotEmpty()) "configured at $path" else "not set"
            } ?: "not set",
        ),
    )
}

private suspend fun loadAppearance(rpc: QodexRpcClient, workspacePath: String): List<SectionItemRow> {
    val settings = callParams(rpc, ServiceChannels.SETTING, "get")
        ?: return listOf(SectionItemRow("error", "Engine settings unreachable", status = RowStatus.ERROR))
    return listOf(
        SectionItemRow(
            "reasoning",
            "Show reasoning in stream",
            subtitle = if (settings.bool("messageStreamShowReasoning") == true) "on" else "off",
        ),
        SectionItemRow(
            "todos",
            "Show todos in stream",
            subtitle = if (settings.bool("messageStreamShowTodos") == true) "on" else "off",
        ),
        SectionItemRow(
            "hardware",
            "Hardware acceleration",
            subtitle = if (settings.bool("desktopChromiumHardwareAccelerationEnabled") == true) "on" else "off",
        ),
    )
}

private suspend fun loadBrowserUse(rpc: QodexRpcClient, workspacePath: String): List<SectionItemRow> =
    listOf(
        SectionItemRow(
            "browser",
            "Browser automation runtime",
            subtitle = "Requires a desktop Chromium runtime; not provisioned on the Android host",
            status = RowStatus.NEUTRAL,
        ),
    )

private suspend fun loadBehavior(rpc: QodexRpcClient, workspacePath: String): List<SectionItemRow> {
    val settings = callParams(rpc, ServiceChannels.SETTING, "get")
        ?: return listOf(SectionItemRow("error", "Engine settings unreachable", status = RowStatus.ERROR))
    return listOf(
        SectionItemRow(
            "interaction",
            "Input while busy",
            subtitle = settings.str("qodexInteractionBehavior").ifEmpty { "queue" },
        ),
        SectionItemRow(
            "searchMode",
            "Tool search mode",
            subtitle = settings.str("toolSearchMode").ifEmpty { "off" },
        ),
        SectionItemRow(
            "nativeSearch",
            "Native search enhancements",
            subtitle = if (settings.bool("nativeSearchEnhancementsEnabled") == true) "on" else "off",
        ),
        SectionItemRow(
            "askResolution",
            "Auto-resolve ask-user questions",
            subtitle = if (settings.bool("askUserQuestionAutoResolutionEnabled") == true) "on" else "off",
        ),
    )
}

private suspend fun loadMemory(rpc: QodexRpcClient, workspacePath: String): List<SectionItemRow> {
    val settings = callParams(rpc, ServiceChannels.SETTING, "get")
    val memories = positional(rpc, ServiceChannels.MEMORY, "listProjectMemories")?.asObjectOrNull()
    val rows = mutableListOf<SectionItemRow>()
    rows.add(
        SectionItemRow(
            "enabled",
            "Memory tool",
            subtitle = if (settings?.bool("memoryEnabled") == true) "enabled" else "disabled",
            status = if (settings?.bool("memoryEnabled") == true) RowStatus.OK else RowStatus.NEUTRAL,
        ),
    )
    for (entry in memories?.arr("workspaces").orEmpty()) {
        val obj = entry.asObjectOrNull() ?: continue
        rows.add(
            SectionItemRow(
                obj.str("workspacePath").ifEmpty { obj.str("workspaceId") },
                obj.str("label").ifEmpty { "Project memory" },
                subtitle = "memories: ${obj.intVal("memoryCount") ?: 0}",
                status = RowStatus.OK,
            ),
        )
    }
    return rows
}

private suspend fun loadSubagents(rpc: QodexRpcClient, workspacePath: String): List<SectionItemRow> {
    val result = positional(
        rpc,
        ServiceChannels.SUBAGENTS,
        "list",
        listOf(
            kotlinx.serialization.json.buildJsonObject {
                put("workspacePath", workspacePath)
                put("provider", "glm")
            },
        ),
    )?.asObjectOrNull() ?: return listOf(SectionItemRow("error", "Subagents unavailable", status = RowStatus.ERROR))
    val rows = mutableListOf<SectionItemRow>()
    for ((group, key) in listOf("agents" to "builtin", "userAgents" to "user", "pluginAgents" to "plugin")) {
        for (entry in result.arr(key).orEmpty()) {
            val agent = entry.asObjectOrNull() ?: continue
            rows.add(
                SectionItemRow(
                    "${group}:${agent.str("id")}",
                    agent.str("name").ifEmpty { agent.str("id") },
                    subtitle = agent.str("description").ifEmpty { group },
                    enabled = agent.bool("enabled"),
                    status = if (agent.bool("enabled") == true) RowStatus.OK else RowStatus.NEUTRAL,
                ),
            )
        }
    }
    if (rows.isEmpty()) rows.add(SectionItemRow("empty", "No subagents discovered", status = RowStatus.NEUTRAL))
    return rows
}

private suspend fun loadPlugins(rpc: QodexRpcClient, workspacePath: String): List<SectionItemRow> {
    val result = positional(
        rpc,
        ServiceChannels.PLUGIN_MANAGEMENT,
        "listPlugins",
        listOf(
            kotlinx.serialization.json.buildJsonObject { put("workspacePath", workspacePath) },
        ),
    )?.asObjectOrNull() ?: return listOf(SectionItemRow("error", "Plugins unavailable", status = RowStatus.ERROR))
    val rows = mutableListOf<SectionItemRow>()
    for (entry in result.arr("plugins").orEmpty()) {
        val plugin = entry.asObjectOrNull() ?: continue
        rows.add(
            SectionItemRow(
                plugin.str("id"),
                plugin.str("name"),
                subtitle = plugin.str("description").ifEmpty {
                    "skills: ${plugin.intVal("skillRootCount") ?: 0} · commands: ${plugin.intVal("commandRootCount") ?: 0}"
                },
                enabled = plugin.bool("enabled"),
                status = if (plugin.bool("enabled") == true) RowStatus.OK else RowStatus.NEUTRAL,
            ),
        )
    }
    if (rows.isEmpty()) rows.add(SectionItemRow("empty", "No plugins installed", status = RowStatus.NEUTRAL))
    return rows
}

private suspend fun loadMcpServers(rpc: QodexRpcClient, workspacePath: String): List<SectionItemRow> {
    val result = positional(
        rpc,
        ServiceChannels.MCP_SYNC,
        "listWorkspaceMcpServerStatuses",
        listOf(
            kotlinx.serialization.json.buildJsonObject {
                put("workspacePath", workspacePath)
                put("mode", "status")
            },
        ),
    )?.asObjectOrNull() ?: return listOf(SectionItemRow("error", "MCP status unavailable", status = RowStatus.ERROR))
    val rows = mutableListOf<SectionItemRow>()
    for (entry in result.arr("servers").orEmpty()) {
        val server = entry.asObjectOrNull() ?: continue
        val toolCount = server.int("toolCount")
        val status = when (server.str("status")) {
            "connected" -> RowStatus.OK
            "connecting" -> RowStatus.WARNING
            "error", "untrusted" -> RowStatus.ERROR
            else -> RowStatus.NEUTRAL
        }
        rows.add(
            SectionItemRow(
                server.str("id").ifEmpty { server.str("name") },
                server.str("name"),
                subtitle = server.str("error").ifEmpty { server.str("status").ifEmpty { "unknown" } },
                badge = if (toolCount != null && toolCount > 0) "$toolCount" else null,
                status = status,
                enabled = server.bool("enabled"),
            ),
        )
    }
    if (rows.isEmpty()) rows.add(SectionItemRow("empty", "No MCP servers configured", status = RowStatus.NEUTRAL))
    return rows
}

private suspend fun loadSkills(rpc: QodexRpcClient, workspacePath: String): List<SectionItemRow> {
    val result = positional(
        rpc,
        ServiceChannels.SKILLS,
        "list",
        listOf(
            kotlinx.serialization.json.buildJsonObject {
                put("workspacePath", workspacePath)
                put("provider", "glm")
            },
        ),
    )?.asObjectOrNull() ?: return listOf(SectionItemRow("error", "Skills unavailable", status = RowStatus.ERROR))
    val rows = mutableListOf<SectionItemRow>()
    for (entry in result.arr("skills").orEmpty()) {
        val skill = entry.asObjectOrNull() ?: continue
        rows.add(
            SectionItemRow(
                skill.str("id"),
                skill.str("name"),
                subtitle = skill.str("description").ifEmpty { skill.str("scope") },
                enabled = skill.bool("enabled"),
                status = if (skill.bool("enabled") == true) RowStatus.OK else RowStatus.NEUTRAL,
            ),
        )
    }
    if (rows.isEmpty()) rows.add(SectionItemRow("empty", "No skills discovered", status = RowStatus.NEUTRAL))
    return rows
}

private suspend fun loadCommands(rpc: QodexRpcClient, workspacePath: String): List<SectionItemRow> {
    val result = positional(
        rpc,
        ServiceChannels.COMMANDS,
        "list",
        listOf(
            kotlinx.serialization.json.buildJsonObject { put("workspacePath", workspacePath) },
        ),
    )?.asObjectOrNull() ?: return listOf(SectionItemRow("error", "Commands unavailable", status = RowStatus.ERROR))
    val rows = mutableListOf<SectionItemRow>()
    for (entry in result.arr("commands").orEmpty()) {
        val command = entry.asObjectOrNull() ?: continue
        rows.add(
            SectionItemRow(
                command.str("id").ifEmpty { command.str("name") },
                command.str("name"),
                subtitle = command.str("description"),
                enabled = command.bool("enabled"),
                status = if (command.bool("enabled") == true) RowStatus.OK else RowStatus.NEUTRAL,
            ),
        )
    }
    if (rows.isEmpty()) rows.add(SectionItemRow("empty", "No commands defined", status = RowStatus.NEUTRAL))
    return rows
}

private suspend fun loadHooks(rpc: QodexRpcClient, workspacePath: String): List<SectionItemRow> {
    val result = positional(
        rpc,
        ServiceChannels.HOOKS,
        "loadHooks",
        listOf(
            kotlinx.serialization.json.buildJsonObject { put("workspacePath", workspacePath) },
        ),
    )?.asObjectOrNull() ?: return listOf(SectionItemRow("error", "Hooks unavailable", status = RowStatus.ERROR))
    val rows = mutableListOf<SectionItemRow>()
    rows.add(
        SectionItemRow(
            "enabled",
            "Hooks enabled",
            subtitle = if (result.bool("hooksEnabled") == true) "on" else "off",
            status = if (result.bool("hooksEnabled") == true) RowStatus.OK else RowStatus.NEUTRAL,
        ),
    )
    for (entry in result.arr("hooks").orEmpty()) {
        val hook = entry.asObjectOrNull() ?: continue
        rows.add(
            SectionItemRow(
                hook.str("id").ifEmpty { hook.str("name") },
                hook.str("name").ifEmpty { hook.str("event").ifEmpty { "hook" } },
                subtitle = hook.str("description").ifEmpty { hook.str("event") },
                enabled = hook.bool("enabled"),
            ),
        )
    }
    return rows
}

private suspend fun loadUsageStats(rpc: QodexRpcClient, workspacePath: String): List<SectionItemRow> {
    val snapshot = positional(
        rpc,
        ServiceChannels.USAGE_STATS,
        "getSnapshot",
        listOf(
            kotlinx.serialization.json.buildJsonObject {
                put("range", "30d")
                put("dataSource", "local")
            },
        ),
    )?.asObjectOrNull() ?: return listOf(SectionItemRow("error", "Usage stats unavailable", status = RowStatus.ERROR))
    val summary = snapshot["summary"]?.asObjectOrNull() ?: JsonObject(emptyMap())
    return listOf(
        SectionItemRow("sessions", "Sessions", subtitle = summary.intVal("totalSessions")?.toString() ?: "0"),
        SectionItemRow("messages", "Messages", subtitle = summary.intVal("totalMessages")?.toString() ?: "0"),
        SectionItemRow(
            "tokens",
            "Estimated tokens",
            subtitle = summary.intVal("totalEstimatedTokens")?.toString() ?: "0",
        ),
        SectionItemRow("activeDays", "Active days (30d)", subtitle = summary.intVal("activeDays")?.toString() ?: "0"),
    )
}

private suspend fun loadProjects(rpc: QodexRpcClient, workspacePath: String): List<SectionItemRow> {
    val result = callParams(rpc, ServiceChannels.PROJECT, "readProjects")
        ?: return listOf(SectionItemRow("error", "Projects unavailable", status = RowStatus.ERROR))
    val rows = mutableListOf<SectionItemRow>()
    for (entry in result.arr("projects").orEmpty()) {
        val project = entry.asObjectOrNull() ?: continue
        val roots = project.arr("roots").orEmpty().mapNotNull {
            it.asObjectOrNull()?.get("rootPath").asStringOrNull()
        }
        rows.add(
            SectionItemRow(
                project.str("id"),
                project.str("name"),
                subtitle = roots.firstOrNull() ?: "no roots",
                status = when (project.str("primaryRootStatus")) {
                    "available" -> RowStatus.OK
                    "missing", "unavailable" -> RowStatus.ERROR
                    else -> RowStatus.NEUTRAL
                },
            ),
        )
    }
    if (rows.isEmpty()) rows.add(SectionItemRow("empty", "No projects registered", status = RowStatus.NEUTRAL))
    return rows
}

private suspend fun loadArchive(rpc: QodexRpcClient, workspacePath: String): List<SectionItemRow> {
    val settings = callParams(rpc, ServiceChannels.SETTING, "get")
        ?: return listOf(SectionItemRow("error", "Engine settings unreachable", status = RowStatus.ERROR))
    return listOf(
        SectionItemRow(
            "autoArchive",
            "Auto-archive inactive tasks",
            subtitle = if (settings.bool("taskAutoArchiveEnabled") == true) "on" else "off",
            status = if (settings.bool("taskAutoArchiveEnabled") == true) RowStatus.OK else RowStatus.NEUTRAL,
        ),
        SectionItemRow(
            "olderThan",
            "Archive older than (days)",
            subtitle = settings.intVal("taskAutoArchiveOlderThanDays")?.toString() ?: "30",
        ),
    )
}

enum class EngineSectionId(
    val title: String,
    val subtitle: String,
    val loader: SectionLoader,
) {
    GENERAL("General", "Engine and workspace preferences", ::loadGeneral),
    APPEARANCE("Appearance", "Stream rendering preferences", ::loadAppearance),
    BROWSER_USE("Browser Use", "Browser automation capability", ::loadBrowserUse),
    BEHAVIOR("Behavior and tools", "Interaction and tool behavior", ::loadBehavior),
    MEMORY("Memory", "Project memory tool", ::loadMemory),
    SUBAGENTS("Subagents", "Builtin, user and plugin agents", ::loadSubagents),
    PLUGINS("Plugins", "Installed plugins", ::loadPlugins),
    MCP_SERVERS("MCP Servers", "Tool servers and connection status", ::loadMcpServers),
    SKILLS("Skills", "Discovered skills", ::loadSkills),
    COMMANDS("Commands", "Slash commands", ::loadCommands),
    HOOKS("Hooks", "Lifecycle hooks", ::loadHooks),
    USAGE_STATS("Usage stats", "Local session statistics (30d)", ::loadUsageStats),
    PROJECTS("Project Management", "Registered workspace projects", ::loadProjects),
    ARCHIVE("Archive management", "Task auto-archive policy", ::loadArchive),
}

// ---------------------------------------------------------------------------
// Hub screen
// ---------------------------------------------------------------------------

@Composable
fun SettingsHubScreen(
    onOpenModelSettings: () -> Unit,
    onOpenConnectors: () -> Unit,
    onOpenSection: (EngineSectionId) -> Unit,
    onBack: () -> Unit,
    engineVersion: String?,
    modifier: Modifier = Modifier,
) {
    SettingsScaffold(title = "Settings", onBack = onBack) {
        SettingsGroupLabel("Basics")
        SettingsNavRow("General", "Engine and workspace preferences") { onOpenSection(EngineSectionId.GENERAL) }
        SettingsNavRow("Appearance", "Stream rendering preferences") { onOpenSection(EngineSectionId.APPEARANCE) }
        SettingsNavRow("Model Settings", "Providers, API keys and models") { onOpenModelSettings() }
        SettingsNavRow("Browser Use", "Browser automation capability") { onOpenSection(EngineSectionId.BROWSER_USE) }

        SettingsGroupLabel("Agent capabilities")
        SettingsNavRow("Behavior and tools", "Interaction and tool behavior") { onOpenSection(EngineSectionId.BEHAVIOR) }
        SettingsNavRow("Memory", "Project memory tool") { onOpenSection(EngineSectionId.MEMORY) }
        SettingsNavRow("Subagents", "Builtin, user and plugin agents") { onOpenSection(EngineSectionId.SUBAGENTS) }
        SettingsNavRow("Plugins", "Installed plugins") { onOpenSection(EngineSectionId.PLUGINS) }
        SettingsNavRow("MCP Servers", "Tool servers and connection status") { onOpenSection(EngineSectionId.MCP_SERVERS) }
        SettingsNavRow("Skills", "Discovered skills") { onOpenSection(EngineSectionId.SKILLS) }
        SettingsNavRow("Commands", "Slash commands") { onOpenSection(EngineSectionId.COMMANDS) }
        SettingsNavRow("Hooks", "Lifecycle hooks") { onOpenSection(EngineSectionId.HOOKS) }

        SettingsGroupLabel("Data and statistics")
        SettingsNavRow("Connectors", "Providers and tool servers") { onOpenConnectors() }
        SettingsNavRow("Usage stats", "Local session statistics") { onOpenSection(EngineSectionId.USAGE_STATS) }
        SettingsNavRow("Project Management", "Registered workspace projects") { onOpenSection(EngineSectionId.PROJECTS) }
        SettingsNavRow("Archive management", "Task auto-archive policy") { onOpenSection(EngineSectionId.ARCHIVE) }

        SettingsGroupLabel("About")
        SettingsValueRow("Version", engineVersion ?: "Qodex 0.1.0")
        SettingsValueRow("Engine", "Node.js (Bionic) + @qodex/server")
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun SettingsGroupLabel(label: String) {
    Text(
        label,
        color = QodexTokens.ForegroundSubtlest,
        fontSize = 12.sp,
        modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
    )
}

@Composable
internal fun SettingsScaffold(
    title: String,
    onBack: () -> Unit,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(QodexTokens.Background)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(QodexTokens.Header),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = QodexIcons.ChevronLeft,
                    contentDescription = "Back",
                    tint = QodexTokens.Foreground,
                    modifier = Modifier.size(16.dp),
                )
            }
            Text(
                title,
                color = QodexTokens.Foreground,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 16.dp),
        ) {
            content()
        }
    }
}

@Composable
internal fun SettingsNavRow(label: String, description: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .background(QodexTokens.Surface, RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, color = QodexTokens.Foreground, fontSize = 14.sp)
            Text(description, color = QodexTokens.ForegroundSubtle, fontSize = 12.sp)
        }
        Icon(
            imageVector = QodexIcons.ChevronRight,
            contentDescription = null,
            tint = QodexTokens.ForegroundSubtlest,
            modifier = Modifier.size(14.dp),
        )
    }
}

@Composable
internal fun SettingsValueRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp)
            .background(QodexTokens.Surface, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = QodexTokens.Foreground, fontSize = 14.sp)
        Spacer(Modifier.weight(1f))
        Text(value, color = QodexTokens.ForegroundSubtle, fontSize = 12.sp)
    }
}

// ---------------------------------------------------------------------------
// Generic engine-backed section screen
// ---------------------------------------------------------------------------

@Composable
fun EngineSectionScreen(
    section: EngineSectionId,
    rpc: QodexRpcClient?,
    workspacePath: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var state by remember(section.name) { mutableStateOf(SectionState()) }
    var loading by remember(section.name) { mutableStateOf(rpc != null) }

    LaunchedEffect(section.name, rpc) {
        val client = rpc
        if (client == null) {
            state = SectionState(error = "Engine not connected")
            loading = false
            return@LaunchedEffect
        }
        loading = true
        state = withContext(Dispatchers.IO) {
            try {
                SectionState(rows = section.loader(client, workspacePath))
            } catch (t: Throwable) {
                RuntimeBus.append("settings ${section.name} load failed: ${t.message}")
                SectionState(error = t.message ?: "load failed")
            }
        }
        loading = false
    }

    SettingsScaffold(title = section.title, onBack = onBack) {
        Text(
            section.subtitle,
            color = QodexTokens.ForegroundSubtle,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        when {
            loading -> SectionLoadingCard("Loading ${section.title}...")
            state.error != null -> Column(Modifier.padding(horizontal = 16.dp)) {
                SectionErrorState(state.error ?: "error")
            }
            else -> Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .heightIn(max = 640.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                for (row in state.rows) {
                    SectionDataRowRow(row)
                }
            }
        }
    }
}

@Composable
internal fun SectionLoadingCard(label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .background(QodexTokens.Surface, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = QodexTokens.ForegroundSubtle, fontSize = 13.sp)
    }
}

@Composable
internal fun SectionErrorState(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(QodexTokens.Destructive.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Text(message, color = QodexTokens.Destructive, fontSize = 13.sp)
    }
}

@Composable
private fun SectionDataRowRow(row: SectionItemRow) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(QodexTokens.Surface, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        row.status?.let { status ->
            Box(
                modifier = Modifier
                    .padding(end = 10.dp)
                    .size(8.dp)
                    .background(statusColor(status), RoundedCornerShape(50)),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(row.title, color = QodexTokens.Foreground, fontSize = 14.sp)
            if (!row.subtitle.isNullOrEmpty()) {
                Text(row.subtitle, color = QodexTokens.ForegroundSubtle, fontSize = 12.sp)
            }
        }
        row.badge?.let { badge ->
            Text(
                badge,
                color = QodexTokens.Foreground,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier
                    .background(QodexTokens.Input, RoundedCornerShape(10.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
        row.enabled?.let { enabled ->
            Switch(
                checked = enabled,
                onCheckedChange = null, // engine toggle op per row lands with the edit flows
                colors = SwitchDefaults.colors(
                    checkedTrackColor = QodexTokens.Success,
                    uncheckedTrackColor = QodexTokens.Border,
                ),
                modifier = Modifier.padding(start = 8.dp),
            )
        }
    }
}

internal fun statusColor(status: RowStatus): Color = when (status) {
    RowStatus.OK -> QodexTokens.Success
    RowStatus.WARNING -> QodexTokens.Warning
    RowStatus.ERROR -> QodexTokens.Destructive
    RowStatus.NEUTRAL -> QodexTokens.ForegroundSubtlest
}

// ---------------------------------------------------------------------------
// Connectors: provider + MCP server rows with live tool-count badges
// ---------------------------------------------------------------------------

data class ConnectorRowData(
    val key: String,
    val name: String,
    val kind: String, // "provider" | "mcp"
    val badge: String? = null,
    val status: RowStatus,
    val detail: String,
)

@Composable
fun ConnectorsScreen(
    rpc: QodexRpcClient?,
    workspacePath: String,
    onBack: () -> Unit,
    onOpenModelSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var rows by remember { mutableStateOf<List<ConnectorRowData>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(rpc != null) }

    LaunchedEffect(rpc) {
        val client = rpc
        if (client == null) {
            error = "Engine not connected"
            loading = false
            return@LaunchedEffect
        }
        loading = true
        error = null
        val loaded = withContext(Dispatchers.IO) {
            runCatching {
                val providers = mutableListOf<ConnectorRowData>()
                val providerView = client.providerSettingsView()?.asObjectOrNull()
                for (entry in providerView?.get("providers").asArrayOrNull().orEmpty()) {
                    val provider = entry.asObjectOrNull() ?: continue
                    val models = provider.get("models").asArrayOrNull().orEmpty()
                    val effective = provider.get("effectiveConfig")?.asObjectOrNull()
                    val apiKeyConfigured = (effective?.get("access")?.asObjectOrNull())
                        ?.get("apiKey").asStringOrNull()?.isNotEmpty() == true
                    providers.add(
                        ConnectorRowData(
                            key = "provider:${provider.get("providerId").asStringOrNull().orEmpty()}",
                            name = provider.get("name").asStringOrNull()
                                ?: provider.get("providerId").asStringOrNull().orEmpty(),
                            kind = "provider",
                            badge = models.size.toString(),
                            status = if (apiKeyConfigured && models.isNotEmpty()) RowStatus.OK else RowStatus.WARNING,
                            detail = if (apiKeyConfigured) "API key configured" else "API key missing",
                        ),
                    )
                }
                val mcp = client.call(ServiceChannels.MCP_SYNC, "listWorkspaceMcpServerStatuses",
                    kotlinx.serialization.json.buildJsonObject {
                        put("workspacePath", workspacePath)
                        put("mode", "connect")
                    })
                for (entry in mcp?.asObjectOrNull()?.get("servers").asArrayOrNull().orEmpty()) {
                    val server = entry.asObjectOrNull() ?: continue
                    val toolCount = server.get("toolCount").asIntOrNull()
                    val status = when (server.get("status").asStringOrNull()) {
                        "connected" -> RowStatus.OK
                        "connecting" -> RowStatus.WARNING
                        "error", "untrusted" -> RowStatus.ERROR
                        else -> RowStatus.NEUTRAL
                    }
                    providers.add(
                        ConnectorRowData(
                            key = "mcp:${server.get("id").asStringOrNull().orEmpty()}",
                            name = server.get("name").asStringOrNull().orEmpty(),
                            kind = "mcp",
                            badge = if (toolCount != null && toolCount > 0) "$toolCount" else null,
                            status = status,
                            detail = server.get("error").asStringOrNull()?.take(120)
                                ?: server.get("status").asStringOrNull()?.ifEmpty { null }
                                ?: "not connected",
                        ),
                    )
                }
                providers
            }.getOrElse {
                RuntimeBus.append("connectors load failed: ${it.message}")
                null
            }
        }
        if (loaded == null) {
            error = "Connector view unavailable"
        } else {
            rows = loaded
        }
        loading = false
    }

    SettingsScaffold(title = "Connectors", onBack = onBack) {
        Text(
            "Model providers and tool servers the agent can reach. Badges show enabled models or live MCP tools.",
            color = QodexTokens.ForegroundSubtle,
            fontSize = 12.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
        when {
            loading -> SectionLoadingCard("Loading connectors...")
            error != null -> Column(Modifier.padding(horizontal = 16.dp)) {
                SectionErrorState(error ?: "error")
            }
            else -> Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .heightIn(max = 640.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                for (row in rows) {
                    ConnectorRow(row, onOpen = if (row.kind == "provider") onOpenModelSettings else null)
                }
                if (rows.isEmpty()) {
                    SettingsValueRow("No connectors", "Add a provider in Model Settings")
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            SettingsNavRow("Add provider", "Model Settings · Add provider", onClick = onOpenModelSettings)
        }
    }
}

@Composable
private fun ConnectorRow(row: ConnectorRowData, onOpen: (() -> Unit)?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(QodexTokens.Surface, RoundedCornerShape(8.dp))
            .then(if (onOpen != null) Modifier.clickable { onOpen() } else Modifier)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .padding(end = 10.dp)
                .size(28.dp)
                .background(QodexTokens.Input, RoundedCornerShape(8.dp))
                .border(1.dp, QodexTokens.Border, RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                row.name.take(1).uppercase(),
                color = QodexTokens.Foreground,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(row.name, color = QodexTokens.Foreground, fontSize = 14.sp)
            Text(row.detail, color = QodexTokens.ForegroundSubtle, fontSize = 12.sp)
        }
        row.badge?.let { badge ->
            Box(
                modifier = Modifier
                    .background(statusColor(row.status), RoundedCornerShape(12.dp))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Text(
                    badge,
                    color = Color(0xFF0A0A0A),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Box(
            modifier = Modifier
                .padding(start = 10.dp)
                .size(8.dp)
                .background(statusColor(row.status), RoundedCornerShape(50)),
        )
    }
}
