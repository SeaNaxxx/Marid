package com.qodex.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qodex.app.rpc.RpcConnectionState
import com.qodex.app.state.QodexAppViewModel

// Screen switching is a tiny state machine — no navigation dependency.
@Composable
fun QodexRootScreen(vm: QodexAppViewModel) {
    val rpcState by vm.rpcState.collectAsState()
    val turns by vm.turns.collectAsState()
    val models by vm.models.collectAsState()
    val selectedModel by vm.selectedModel.collectAsState()
    val composerText by vm.composerText.collectAsState()
    val providerNotReady by vm.providerNotReady.collectAsState()
    val tasks by vm.sidebarTasks.collectAsState()
    val sidebarLoading by vm.sidebarLoading.collectAsState()
    val activeSessionId by vm.activeSessionId.collectAsState()
    val turnRunning by vm.turnRunning.collectAsState()

    var drawerOpen by rememberSaveable { mutableStateOf(false) }
    var screen by rememberSaveable { mutableStateOf(RootScreen.CHAT) }
    var terminalOpen by rememberSaveable { mutableStateOf(false) }
    var connectOpen by rememberSaveable { mutableStateOf(false) }
    // Settings sub-screens are keyed by section name; a plain string survives
    // rememberSaveable (a nullable enum would break Bundle serialization).
    var sectionName by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(rpcState) {
        if (rpcState == RpcConnectionState.CONNECTED) vm.loadModels()
    }
    // Returning from Model Settings after configuring a provider re-boots the
    // conversation so the engine's readiness gate clears instead of staying failed.
    LaunchedEffect(screen) {
        if (screen == RootScreen.CHAT && providerNotReady) vm.retryConversation()
    }

    QodexSidebarDrawerHost(
        isOpen = drawerOpen && screen == RootScreen.CHAT,
        onOpenChange = { drawerOpen = it },
        drawerContent = {
            QodexSidebarDrawer(
                profileName = "Qodex",
                profileInitial = "Q",
                looseTasks = tasks,
                loading = sidebarLoading,
                onNewTask = {
                    vm.startNewConversation()
                    screen = RootScreen.CHAT
                    drawerOpen = false
                },
                onSelectTask = { task ->
                    vm.openTask(task.key)
                    screen = RootScreen.CHAT
                    drawerOpen = false
                },
                onCreateTask = {
                    vm.startNewConversation()
                    screen = RootScreen.CHAT
                    drawerOpen = false
                },
                onOpenSearch = {
                    screen = RootScreen.TASKS
                    drawerOpen = false
                },
                onOpenSettings = { screen = RootScreen.SETTINGS; drawerOpen = false },
                onProfileClick = {
                    drawerOpen = false
                    connectOpen = true
                },
            )
        },
        content = {
            when (screen) {
                RootScreen.CHAT -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(QodexTokens.Background),
                    ) {
                        QodexHeaderBar(
                            title = tasks.firstOrNull { it.key == activeSessionId }?.title
                                ?: if (turns.isEmpty()) null else "Qodex",
                            onMenu = { drawerOpen = true },
                            isDraft = turns.isEmpty(),
                            onToggleTerminal = { terminalOpen = !terminalOpen },
                            terminalOpen = terminalOpen,
                        )
                        // Boot gates chat entry on CONNECTED, so DISCONNECTED here is always a live drop.
                        if (rpcState == RpcConnectionState.DISCONNECTED) {
                            EngineDisconnectedBanner(onReconnect = { vm.reconnectRpc() })
                        }
                        if (providerNotReady) {
                            ProviderNotReadyBanner(onOpenModelSettings = { screen = RootScreen.MODEL_SETTINGS })
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            if (turns.isEmpty()) {
                                WelcomeScreen(
                                    onNewChat = { vm.startNewConversation() },
                                    onQuickPrompt = { prompt ->
                                        vm.onComposerTextChange(prompt)
                                    },
                                    workspacePath = vm.workspacePath,
                                    selectedModel = selectedModel?.let { "${it.providerLabel} • ${it.modelId}" },
                                )
                            } else {
                                QodexChatArea(turns = turns, turnRunning = turnRunning)
                            }
                        }
                        QodexComposer(
                            text = composerText,
                            onTextChange = vm::onComposerTextChange,
                            models = models,
                            selectedModel = selectedModel,
                            onModelSelect = vm::selectModel,
                            onSend = vm::sendText,
                        )
                    }
                }
                RootScreen.SETTINGS -> SettingsHubScreen(
                    onOpenModelSettings = { screen = RootScreen.MODEL_SETTINGS },
                    onOpenConnectors = { screen = RootScreen.CONNECTORS },
                    onOpenSection = { sectionName = it.name; screen = RootScreen.SECTION },
                    onBack = { screen = RootScreen.CHAT },
                    engineVersion = null,
                )
                RootScreen.SECTION -> {
                    val current = EngineSectionId.entries.firstOrNull { it.name == sectionName }
                        ?: EngineSectionId.GENERAL
                    EngineSectionScreen(
                        section = current,
                        rpc = vm.rpcClient(),
                        workspacePath = vm.workspacePath,
                        onBack = { screen = RootScreen.SETTINGS },
                    )
                }
                RootScreen.CONNECTORS -> ConnectorsScreen(
                    rpc = vm.rpcClient(),
                    workspacePath = vm.workspacePath,
                    onBack = { screen = RootScreen.SETTINGS },
                    onOpenModelSettings = { screen = RootScreen.MODEL_SETTINGS },
                )
                RootScreen.MODEL_SETTINGS -> ModelSettingsScreen(
                    rpc = vm.rpcClient(),
                    workspacePath = vm.workspacePath,
                    onBack = { screen = RootScreen.SETTINGS },
                )
                RootScreen.TASKS -> TaskListScreen(
                    tasks = tasks.map { task ->
                        TaskRow2e(
                            id = task.key,
                            title = task.title,
                            status = when {
                                task.isRunning -> TaskRowStatus2e.RUNNING
                                task.isError -> TaskRowStatus2e.ERROR
                                else -> TaskRowStatus2e.COMPLETED
                            },
                            updatedAt = task.updatedAtMs,
                            isActive = task.isActive,
                        )
                    },
                    onOpenTask = { row ->
                        vm.openTask(row.id)
                        screen = RootScreen.CHAT
                    },
                    onNewTask = {
                        vm.startNewConversation()
                        screen = RootScreen.CHAT
                    },
                )
            }
            if (terminalOpen && screen == RootScreen.CHAT) {
                QodexTerminalSheet(rpc = vm.rpcClient(), onClose = { terminalOpen = false })
            }
        },
    )

    if (connectOpen) {
        QodexConnectDialog(rpc = vm.rpcClient(), onDismiss = { connectOpen = false })
    }
}

private enum class RootScreen { CHAT, SETTINGS, MODEL_SETTINGS, CONNECTORS, SECTION, TASKS }

// Provider gate: the engine refuses model execution until a provider + key is
// configured; this banner routes the user to Model Settings instead of a dead composer.
@Composable
private fun ProviderNotReadyBanner(onOpenModelSettings: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .background(
                QodexTokens.Warning.copy(alpha = 0.12f),
                RoundedCornerShape(8.dp),
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "No model provider configured",
            color = QodexTokens.Foreground,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "Open Model Settings",
            color = QodexTokens.Foreground,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .background(QodexTokens.Surface, RoundedCornerShape(6.dp))
                .clickable(onClick = onOpenModelSettings)
                .padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}

// ChatErrorBanner parity: a red state strip, never an infinite spinner, when the engine socket drops.
@Composable
private fun EngineDisconnectedBanner(onReconnect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .background(
                QodexTokens.Destructive.copy(alpha = 0.1f), // bg-destructive/10
                RoundedCornerShape(8.dp),
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "Engine disconnected",
            color = QodexTokens.Destructive,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "Reconnect",
            color = QodexTokens.Foreground,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .background(QodexTokens.Surface, RoundedCornerShape(6.dp))
                .clickable(onClick = onReconnect)
                .padding(horizontal = 10.dp, vertical = 6.dp),
        )
    }
}
