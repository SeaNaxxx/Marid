package com.marid.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marid.app.icons.MaridIcons
import com.marid.app.rpc.MaridRpcClient
import com.marid.app.rpc.asObjectOrNull
import com.marid.app.rpc.asStringOrNull
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun SettingsScreen(
    onOpenModelSettings: () -> Unit,
    onBack: () -> Unit,
    rpc: MaridRpcClient? = null,
    modifier: Modifier = Modifier,
) {
    var engineVersion by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(rpc) {
        if (rpc != null) {
            engineVersion = withContext(Dispatchers.IO) {
                runCatching {
                    rpc.call("setting", "getView")
                        ?.asObjectOrNull()?.get("version").asStringOrNull()
                }.getOrNull()
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaridTokens.Background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState()),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .background(MaridTokens.Header),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clickable { onBack() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = MaridIcons.ChevronLeft,
                    contentDescription = "Back",
                    tint = MaridTokens.Foreground,
                    modifier = Modifier.size(16.dp),
                )
            }
            Text(
                "Settings",
                color = MaridTokens.Foreground,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
        }

        Spacer(Modifier.height(12.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                "General",
                color = MaridTokens.ForegroundSubtlest,
                fontSize = 12.sp,
            )
            SettingsNavRow(
                label = "Model Settings",
                description = "Providers and API keys",
                onClick = onOpenModelSettings,
            )
        }

        Spacer(Modifier.height(16.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                "About",
                color = MaridTokens.ForegroundSubtlest,
                fontSize = 12.sp,
            )
            SettingsValueRow(label = "Version", value = engineVersion ?: "Marid 0.1.0")
            SettingsValueRow(label = "Engine", value = "Node.js (Bionic) + @marid/server")
        }
    }
}

@Composable
private fun SettingsNavRow(
    label: String,
    description: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaridTokens.Surface,
                RoundedCornerShape(8.dp),
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                color = MaridTokens.Foreground,
                fontSize = 14.sp,
            )
            Text(
                description,
                color = MaridTokens.ForegroundSubtle,
                fontSize = 12.sp,
            )
        }
        Icon(
            imageVector = MaridIcons.ChevronRight,
            contentDescription = null,
            tint = MaridTokens.ForegroundSubtlest,
            modifier = Modifier.size(14.dp),
        )
    }
}

@Composable
private fun SettingsValueRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaridTokens.Surface, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = MaridTokens.Foreground, fontSize = 14.sp)
        Spacer(Modifier.weight(1f))
        Text(value, color = MaridTokens.ForegroundSubtle, fontSize = 12.sp)
    }
}
