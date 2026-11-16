package com.marid.app.ui

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.marid.app.icons.MaridIcons

// Placeholders by design; no real OAuth flow.
@Composable
fun MaridConnectDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                    .background(MaridTokens.Popover, RoundedCornerShape(12.dp))
                    .border(1.dp, MaridTokens.Border, RoundedCornerShape(12.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Connect",
                        color = MaridTokens.Foreground,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Icon(
                        imageVector = MaridIcons.X,
                        contentDescription = "Close",
                        tint = MaridTokens.ForegroundSubtle,
                        modifier = Modifier
                            .size(32.dp)
                            .clickable(onClick = onDismiss)
                            .padding(6.dp),
                    )
                }
                Text(
                    text = "OAuth providers are placeholders in this build.",
                    color = MaridTokens.ForegroundSubtle,
                    fontSize = 12.sp,
                )
                ConnectPlaceholderRow(
                    icon = MaridIcons.GitBranch,
                    label = "Connect to GitHub",
                )
                ConnectPlaceholderRow(
                    icon = MaridIcons.User,
                    label = "Connect to Google",
                )
            }
        }
    }
}

/** Disabled placeholder — the OAuth flow behind it does not exist yet. */
@Composable
private fun ConnectPlaceholderRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp) // touch target >= 44dp
            .border(1.dp, MaridTokens.Border, RoundedCornerShape(8.dp))
            .background(MaridTokens.Input)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaridTokens.ForegroundSubtlest,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = label,
            color = MaridTokens.ForegroundSubtlest,
            fontSize = 14.sp,
        )
    }
}
