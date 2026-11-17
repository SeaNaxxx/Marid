package com.qodex.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.qodex.app.icons.QodexIcons
import com.qodex.app.rpc.QodexRpcClient
import com.qodex.app.rpc.asArrayOrNull
import com.qodex.app.rpc.asObjectOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// Real engine facts only: whatever the connection reports renders here; OAuth login stays desktop-only by design.
@Composable
fun QodexConnectDialog(
    rpc: QodexRpcClient?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var providerCount by remember { mutableStateOf<Int?>(null) }
    var engineOk by remember { mutableStateOf<Boolean?>(null) }

    LaunchedEffect(rpc) {
        if (rpc == null) {
            engineOk = false
            return@LaunchedEffect
        }
        val view = withContext(Dispatchers.IO) {
            runCatching { rpc.providerSettingsView() }.getOrNull()
        }
        val obj = view.asObjectOrNull()
        engineOk = obj != null
        providerCount = obj?.get("providers").asArrayOrNull()?.size ?: 0
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(onClick = onDismiss, interactionSource = null, indication = null),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .fillMaxWidth()
                    .clickable(interactionSource = null, indication = null) { /* swallow */ }
                    .background(QodexTokens.Popover, RoundedCornerShape(12.dp))
                    .border(1.dp, QodexTokens.Border, RoundedCornerShape(12.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Connection",
                        color = QodexTokens.Foreground,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Icon(
                        imageVector = QodexIcons.X,
                        contentDescription = "Close",
                        tint = QodexTokens.ForegroundSubtle,
                        modifier = Modifier
                            .size(32.dp)
                            .clickable(onClick = onDismiss)
                            .padding(6.dp),
                    )
                }
                ConnectStatusRow(
                    label = "Engine",
                    value = when (engineOk) {
                        true -> "Connected"
                        false -> "Unreachable"
                        null -> "Checking..."
                    },
                    ok = engineOk == true,
                )
                ConnectStatusRow(
                    label = "Providers configured",
                    value = providerCount?.toString() ?: "-",
                    ok = (providerCount ?: 0) > 0,
                )
            }
        }
    }
}

@Composable
private fun ConnectStatusRow(label: String, value: String, ok: Boolean) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp) // touch target >= 44dp
            .border(1.dp, QodexTokens.Border, RoundedCornerShape(8.dp))
            .background(QodexTokens.Input)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    if (ok) QodexTokens.Success else QodexTokens.Warning,
                    RoundedCornerShape(50),
                ),
        )
        Text(
            text = label,
            color = QodexTokens.Foreground,
            fontSize = 14.sp,
            modifier = Modifier.weight(1f),
        )
        Text(
            text = value,
            color = QodexTokens.ForegroundSubtle,
            fontSize = 12.sp,
        )
    }
}
