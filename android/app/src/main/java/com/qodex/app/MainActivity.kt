package com.qodex.app

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
import com.qodex.app.runtime.EngineProcess
import com.qodex.app.runtime.Provisioner
import com.qodex.app.runtime.RuntimeBus
import com.qodex.app.runtime.RuntimePhase
import com.qodex.app.state.QodexAppViewModel
import com.qodex.app.ui.QodexRootScreen
import com.qodex.app.ui.QodexTokens
import com.qodex.app.utils.FileLogger

// Boot contract: provision -> engine subprocess on stdio pipes -> RPC handshake; buttons appear only on failure (no fake progress).
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        FileLogger.init(applicationContext)
        FileLogger.append("MainActivity", "onCreate: provisioned=${Provisioner.isProvisioned(applicationContext)}")
        setContent {
            val vm: QodexAppViewModel = ViewModelProvider(this, QodexAppViewModel.factory(applicationContext as android.app.Application))[QodexAppViewModel::class.java]
            val phase by RuntimeBus.phase.collectAsState()
            val rpcState by vm.rpcState.collectAsState()

            LaunchedEffect(Unit) { vm.boot(applicationContext) }

            QodexAppRootContent(phase, rpcState, vm)
        }
    }
}

@Composable
private fun QodexAppRootContent(
    phase: RuntimePhase,
    rpcState: com.qodex.app.rpc.RpcConnectionState,
    vm: QodexAppViewModel,
) {
    val screen = remember(phase, rpcState) {
        when {
            phase is RuntimePhase.Failed -> BootScreen.Error(phase)
            phase is RuntimePhase.EngineReady &&
                rpcState == com.qodex.app.rpc.RpcConnectionState.CONNECTED -> BootScreen.Chat
            else -> BootScreen.Dots
        }
    }
    when (screen) {
        is BootScreen.Error -> ErrorScreen(screen.phase, vm)
        BootScreen.Dots -> ProvisioningDotsScreen()
        BootScreen.Chat -> QodexRootScreen(vm)
    }
}

private sealed interface BootScreen {
    data object Dots : BootScreen
    data object Chat : BootScreen
    data class Error(val phase: RuntimePhase.Failed) : BootScreen
}

@Composable
private fun ErrorScreen(phase: RuntimePhase.Failed, vm: QodexAppViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(QodexTokens.Background)
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
            onClick = {
                FileLogger.append("MainActivity", "retry requested by user")
                vm.retry()
            },
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
            .background(QodexTokens.Background),
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
