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

// Screen switching is a tiny state machine — no navigation dependency.
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
    var connectOpen by rememberSaveable { mutableStateOf(false) }

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
