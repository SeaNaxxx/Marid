package com.marid.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import com.marid.app.runtime.EngineProcess
import com.marid.app.runtime.Provisioner
import com.marid.app.runtime.RuntimeBus
import com.marid.app.runtime.RuntimePhase
import com.marid.app.state.MaridAppViewModel
import com.marid.app.ui.MaridRootScreen
import com.marid.app.ui.MaridTokens

/**
 * Entry point. Boot contract (owner spec — no buttons on the happy path):
 *
 *   open -> [provision if needed] -> [engine starts on a random loopback port]
 *        -> [RPC connects to ws://127.0.0.1:<port>/ws] -> Chat
 *
 * The three-dots screen covers provisioning AND engine startup. Failures are
 * the ONLY place a button appears (retry). No "Start" button, no
 * "Extracting..." text, no fake progress.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: MaridAppViewModel = ViewModelProvider(this, MaridAppViewModel.factory(applicationContext as android.app.Application))[MaridAppViewModel::class.java]
            val phase by RuntimeBus.phase.collectAsState()
            val rpcState by vm.rpcState.collectAsState()

            LaunchedEffect(Unit) { vm.boot(applicationContext) }

            MaridAppRootContent(phase, rpcState, vm)
        }
    }
}

@Composable
private fun MaridAppRootContent(
    phase: RuntimePhase,
    rpcState: com.marid.app.rpc.RpcConnectionState,
    vm: MaridAppViewModel,
) {
    val screen = remember(phase, rpcState) {
        when {
            phase is RuntimePhase.Failed -> BootScreen.Error(phase)
            phase is RuntimePhase.EngineReady &&
                rpcState == com.marid.app.rpc.RpcConnectionState.CONNECTED -> BootScreen.Chat
            else -> BootScreen.Dots
        }
    }
    when (screen) {
        is BootScreen.Error -> ErrorScreen(screen.phase, vm)
        BootScreen.Dots -> ProvisioningDotsScreen()
        BootScreen.Chat -> MaridRootScreen(vm)
    }
}

private sealed interface BootScreen {
    data object Dots : BootScreen
    data object Chat : BootScreen
    data class Error(val phase: RuntimePhase.Failed) : BootScreen
}

@Composable
private fun ErrorScreen(phase: RuntimePhase.Failed, vm: MaridAppViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaridTokens.Background)
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "Could not start: ${phase.message}",
            color = Color(0xFFF87171),
            fontSize = 14.sp,
            fontFamily = FontFamily.Monospace,
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { vm.retry() },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2B2B2B),
                contentColor = Color(0xFFE7E7E7),
            ),
            shape = RoundedCornerShape(8.dp),
        ) {
            Text("Retry", fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun ProvisioningDotsScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaridTokens.Background),
        contentAlignment = Alignment.Center,
    ) {
        val transition = rememberInfiniteTransition(label = "boot-dots")
        val dots = listOf(0, 1, 2).map { offset ->
            transition.animateFloat(
                initialValue = 0.25f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(900, delayMillis = offset * 180, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "dot-$offset",
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            dots.forEach { alpha ->
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(
                            Color(0xFF9E9E9E).copy(alpha = alpha.value),
                            RoundedCornerShape(5.dp),
                        ),
                )
            }
        }
    }
}
