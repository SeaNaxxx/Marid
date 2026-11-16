package com.marid.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.marid.app.state.ComposerMode
import com.marid.app.state.MaridSessionViewModel
import com.marid.app.state.ModelEntry
import com.marid.app.theme.MaridColor
import com.marid.app.theme.MaridTheme
import com.marid.app.ui.MaridChatArea
import com.marid.app.ui.MaridComposer
import com.marid.app.ui.MaridHeaderBar
import com.marid.app.ui.MaridSidebarDrawer
import com.marid.app.ui.MaridTerminalSheet
import kotlinx.coroutines.launch

/**
 * Marid — native Android shell porting the ZCode desktop React/Tailwind UI
 * (packages/ui) to Jetpack Compose. Layout adaptation only: the desktop
 * always-visible sidebar becomes a ModalNavigationDrawer; the desktop bottom
 * terminal panel becomes a ModalBottomSheet. Colors, icons, spacing and
 * component shapes match packages/ui exactly (see theme/MaridColor.kt).
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val viewModel = MaridSessionViewModel(applicationContext)
        setContent {
            MaridTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaridColor.Background,
                ) {
                    MaridApp(viewModel)
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
fun MaridApp(viewModel: MaridSessionViewModel) {
    val state by viewModel.state.collectAsState()
    val drawerState = rememberDrawerState(initialValue = androidx.compose.material3.DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    var terminalSheetVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.boot() }
    LaunchedEffect(state.terminalOpen) {
        terminalSheetVisible = state.terminalOpen
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                drawerContainerColor = MaridColor.Sidebar, // bg-sidebar #161616
                windowInsets = WindowInsets(0),
            ) {
                MaridSidebarDrawer(
                    state = state,
                    onNewTask = { scope.launch { drawerState.close() } },
                    onAutomations = { scope.launch { drawerState.close() } },
                    onPluginMarketplace = { scope.launch { drawerState.close() } },
                    onConnect = { scope.launch { drawerState.close() } },
                    onSettings = { scope.launch { drawerState.close() } },
                )
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaridColor.Background)
                .padding(
                    top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
                ),
        ) {
            // Header — 48dp, menu/folder/title left; help/terminal/split right
            MaridHeaderBar(
                taskTitle = state.taskTitle,
                terminalOpen = state.terminalOpen,
                sidePaneOpen = false,
                onMenuClick = { scope.launch { drawerState.open() } },
                onHelpClick = { },
                onTerminalToggle = { viewModel.toggleTerminal() },
                onSidePaneToggle = { viewModel.toggleTerminal() },
            )

            // Chat area — agent turns, tool rows, change summary cards
            MaridChatArea(
                state = state,
                onUndoChanges = { },
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
            )

            // Composer — rounded-2xl shell, [+][Ask before changes][model][send]
            val bottomPadding = WindowInsets.systemBars.asPaddingValues().calculateBottomPadding()
            MaridComposer(
                state = state,
                onTextChange = { viewModel.onComposerTextChange(it) },
                onSend = { viewModel.send() },
                onToggleModeMenu = { viewModel.toggleModeMenu() },
                onToggleModelMenu = { viewModel.toggleModelMenu() },
                onSelectModel = { viewModel.selectModel(it) },
                onSelectMode = { /* single mode in this pass */ },
                modifier = Modifier.padding(bottom = bottomPadding),
            )
        }
    }

    // Terminal bottom sheet (STEP 4)
    if (terminalSheetVisible) {
        MaridTerminalSheet(
            state = state,
            onDismiss = { viewModel.toggleTerminal() },
            onRunCommand = { viewModel.runTerminalCommand(it) },
        )
    }
}
