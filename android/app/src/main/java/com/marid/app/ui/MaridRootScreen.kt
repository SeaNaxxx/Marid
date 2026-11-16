package com.marid.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.marid.app.rpc.RpcConnectionState
import com.marid.app.state.MaridAppViewModel

/**
 * Root product composition — the desktop layout adapted to phone:
 * HeaderBar on top, Sidebar as a slide-in drawer (MaridSidebarDrawerHost),
 * ChatArea + Composer filling the body. Screen switching (Model Settings /
 * Settings / Task list / Terminal) is a tiny state machine, mirroring the
 * desktop surfaces without a navigation dependency.
 *
 * Sources: App.tsx / WorkspaceHeader.tsx / WorkspaceSidebar.tsx /
 * ToolCallBlocks / prompt-editor / settings pages. Deviation: sidebar is a
 * drawer on phone (desktop fixed panel); settings becomes Model Settings
 * entry (mobile single-column adaptation).
 */
@Composable
fun MaridRootScreen(vm: MaridAppViewModel) {
    val rpcState by vm.rpcState.collectAsState()
    val turns by vm.turns.collectAsState()
    val models by vm.models.collectAsState()
    val selectedModel by vm.selectedModel.collectAsState()
    val composerText by vm.composerText.collectAsState()

    var drawerOpen by rememberSaveable { mutableStateOf(false) }
    var screen by rememberSaveable { mutableStateOf(RootScreen.CHAT) }
    var terminalOpen by rememberSaveable { mutableStateOf(false) }
    // Connect dialog (OAuth placeholders) — opened from the sidebar profile
    // button ("Connect"); replaces the removed Z.ai login surface.
    var connectOpen by rememberSaveable { mutableStateOf(false) }

    // Load the model catalog when the RPC becomes CONNECTED (same trigger as
    // the desktop composer; model-selection.getView channel).
    LaunchedEffect(rpcState) {
        if (rpcState == RpcConnectionState.CONNECTED) vm.loadModels()
    }

    MaridSidebarDrawerHost(
        isOpen = drawerOpen && screen == RootScreen.CHAT,
        onOpenChange = { drawerOpen = it },
        drawerContent = {
            MaridSidebarDrawer(
                profileName = "SeaNaxxx",
                profileInitial = "S",
                onNewTask = { drawerOpen = false },
                onSelectTask = { drawerOpen = false },
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
                            .background(MaridTokens.Background),
                    ) {
                        MaridHeaderBar(
                            title = if (turns.isEmpty()) null else "Marid",
                            onMenu = { drawerOpen = true },
                            onOpenHelp = {},
                            isDraft = turns.isEmpty(),
                            onToggleTerminal = { terminalOpen = !terminalOpen },
                            terminalOpen = terminalOpen,
                        )
                        Box(modifier = Modifier.weight(1f)) {
                            if (turns.isEmpty()) {
                                WelcomeScreen()
                            } else {
                                MaridChatArea(turns = turns)
                            }
                        }
                        MaridComposer(
                            text = composerText,
                            onTextChange = vm::onComposerTextChange,
                            models = models,
                            selectedModel = selectedModel,
                            onModelSelect = vm::selectModel,
                            onSend = vm::sendText,
                        )
                    }
                }
                RootScreen.SETTINGS -> SettingsScreen(
                    onOpenModelSettings = { screen = RootScreen.MODEL_SETTINGS },
                    onBack = { screen = RootScreen.CHAT },
                    rpc = vm.rpcClient(),
                )
                RootScreen.MODEL_SETTINGS -> ModelSettingsScreen(
                    rpc = vm.rpcClient(),
                    onBack = { screen = RootScreen.SETTINGS },
                )
                RootScreen.TASKS -> TaskListScreen(
                    tasks = emptyList(),
                    onOpenTask = { screen = RootScreen.CHAT },
                    onNewTask = { screen = RootScreen.CHAT },
                )
            }
            if (terminalOpen && screen == RootScreen.CHAT) {
                MaridTerminalSheet(onClose = { terminalOpen = false })
            }
        },
    )

    if (connectOpen) {
        MaridConnectDialog(onDismiss = { connectOpen = false })
    }
}

private enum class RootScreen { CHAT, SETTINGS, MODEL_SETTINGS, TASKS }
