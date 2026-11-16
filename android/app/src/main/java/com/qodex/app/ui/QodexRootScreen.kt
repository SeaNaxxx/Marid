package com.qodex.app.ui

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

    var drawerOpen by rememberSaveable { mutableStateOf(false) }
    var screen by rememberSaveable { mutableStateOf(RootScreen.CHAT) }
    var terminalOpen by rememberSaveable { mutableStateOf(false) }
    var connectOpen by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(rpcState) {
        if (rpcState == RpcConnectionState.CONNECTED) vm.loadModels()
    }

    QodexSidebarDrawerHost(
        isOpen = drawerOpen && screen == RootScreen.CHAT,
        onOpenChange = { drawerOpen = it },
        drawerContent = {
            QodexSidebarDrawer(
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
                            .background(QodexTokens.Background),
                    ) {
                        QodexHeaderBar(
                            title = if (turns.isEmpty()) null else "Qodex",
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
                                QodexChatArea(turns = turns)
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
                QodexTerminalSheet(onClose = { terminalOpen = false })
            }
        },
    )

    if (connectOpen) {
        QodexConnectDialog(onDismiss = { connectOpen = false })
    }
}

private enum class RootScreen { CHAT, SETTINGS, MODEL_SETTINGS, TASKS }
