package com.qodex.app.ui

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
import com.qodex.app.icons.QodexIcons
import com.qodex.app.rpc.QodexRpcClient
import com.qodex.app.rpc.asObjectOrNull
import com.qodex.app.rpc.asStringOrNull
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
    rpc: QodexRpcClient? = null,
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
            .background(QodexTokens.Background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState()),
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
                "Settings",
                color = QodexTokens.Foreground,
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
                color = QodexTokens.ForegroundSubtlest,
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
                color = QodexTokens.ForegroundSubtlest,
                fontSize = 12.sp,
            )
            SettingsValueRow(label = "Version", value = engineVersion ?: "Qodex 0.1.0")
            SettingsValueRow(label = "Engine", value = "Node.js (Bionic) + @qodex/server")
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
                QodexTokens.Surface,
                RoundedCornerShape(8.dp),
            )
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                color = QodexTokens.Foreground,
                fontSize = 14.sp,
            )
            Text(
                description,
                color = QodexTokens.ForegroundSubtle,
                fontSize = 12.sp,
            )
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
private fun SettingsValueRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(QodexTokens.Surface, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = QodexTokens.Foreground, fontSize = 14.sp)
        Spacer(Modifier.weight(1f))
        Text(value, color = QodexTokens.ForegroundSubtle, fontSize = 12.sp)
    }
}
