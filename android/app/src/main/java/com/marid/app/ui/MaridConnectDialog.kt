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

/**
 * Connect dialog — OAuth placeholder surface.
 *
 * Directive (owner, 2026-11-16): the sidebar profile button previously
 * presented a Z.ai login. Every Z.ai login trace is REMOVED; tapping
 * "Connect" now opens THIS dialog with two explicit provider placeholders —
 * "Connect to GitHub" / "Connect to Google". The real OAuth flows are NOT
 * implemented yet, so both rows render as disabled placeholders (muted, not
 * clickable — dead-tap buttons are forbidden) and the caption states it.
 *
 * Visual language = the ported design system (MaridTokens dark palette):
 * scrim black 50%, card bg-popover (neutral-800), 1dp border (white 10%),
 * 12dp card radius, 8dp row radius, 44dp touch rows, 14sp ui-base labels.
 */
@Composable
fun MaridConnectDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        // scrim "fixed inset-0 bg-black/50" + tap-to-dismiss
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(onClick = onDismiss, interactionSource = null, indication = null),
            contentAlignment = Alignment.Center,
        ) {
            // card "w-full max-w-xs rounded-xl border border-border bg-popover p-5"
            Column(
                modifier = Modifier
                    .widthIn(max = 320.dp)
                    .fillMaxWidth()
                    .clickable(interactionSource = null, indication = null) { /* swallow */ }
                    .background(MaridTokens.Popover, RoundedCornerShape(12.dp)) // rounded-xl
                    .border(1.dp, MaridTokens.Border, RoundedCornerShape(12.dp))
                    .padding(20.dp), // p-5
                verticalArrangement = Arrangement.spacedBy(16.dp), // gap-4
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Connect",
                        color = MaridTokens.Foreground,
                        fontSize = 16.sp, // text-ui-lg
                        fontWeight = FontWeight.SemiBold,
                    )
                    // close: ghost icon button, real interactive element
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
                    fontSize = 12.sp, // text-ui-sm
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

/**
 * Placeholder provider row — rendered disabled (muted, non-clickable):
 * the OAuth flow behind it does not exist yet, and dead-tap buttons are
 * forbidden by the UI rules.
 */
@Composable
private fun ConnectPlaceholderRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 44.dp) // touch target >= 44dp
            .border(1.dp, MaridTokens.Border, RoundedCornerShape(8.dp)) // rounded-lg
            .background(MaridTokens.Input) // bg-input
            .padding(horizontal = 12.dp), // px-3
        horizontalArrangement = Arrangement.spacedBy(8.dp), // gap-2
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaridTokens.ForegroundSubtlest,
            modifier = Modifier.size(16.dp), // size-4
        )
        Text(
            text = label,
            color = MaridTokens.ForegroundSubtlest,
            fontSize = 14.sp, // text-ui-base
        )
    }
}
