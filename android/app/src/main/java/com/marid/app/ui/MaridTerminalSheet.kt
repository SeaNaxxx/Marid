package com.marid.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marid.app.icons.MaridIcons
import com.marid.app.state.MaridUiState
import com.marid.app.theme.MaridRadius
import com.marid.app.theme.MaridTextSize
import com.marid.app.theme.monoStyle
import com.marid.app.theme.maridTokens

/**
 * Terminal bottom sheet — port of Terminal.tsx:278-373 into a
 * ModalBottomSheet (the desktop terminal is a bottom ResizablePanel of the
 * conversation column; on phones a bottom sheet is the layout adaptation):
 *   section "flex flex-col gap-2 bg-background p-3 pb-2"
 *   title "Terminal" font-medium + shell label text-foreground-subtle
 *   monospace fontSize 13 (xterm.js default).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MaridTerminalSheet(
    state: MaridUiState,
    onDismiss: () -> Unit,
    onRunCommand: (String) -> Unit,
) {
    val t = maridTokens()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scroll = rememberScrollState()

    LaunchedEffect(state.terminalLines.size) {
        scroll.animateScrollTo(scroll.maxValue)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = t.background, // bg-background #161616
        scrimColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp) // p-3 pb-2
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Title row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Terminal", // terminal.title
                    fontSize = MaridTextSize.Base.sp,
                    fontWeight = FontWeight.Medium,
                    color = t.foreground,
                )
                Text(
                    text = "bash — bionic ($PREFIX_LABEL)",
                    fontSize = MaridTextSize.Base.sp,
                    color = t.foregroundSubtle,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Box(modifier = Modifier.weight(1f))
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(RoundedCornerShape(MaridRadius.Lg.dp))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center,
                ) {
                    androidx.compose.material3.Icon(
                        MaridIcons.X,
                        contentDescription = "Close terminal", // TID_TERMINAL_CLOSE_BUTTON
                        tint = t.foreground,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }

            // Output — max height, monospace 13
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 320.dp)
                    .clip(RoundedCornerShape(MaridRadius.Lg.dp))
                    .border(0.5.dp, t.border, RoundedCornerShape(MaridRadius.Lg.dp))
                    .background(t.background)
                    .verticalScroll(scroll)
                    .padding(8.dp),
            ) {
                state.terminalLines.forEach { line ->
                    Text(
                        text = line,
                        style = monoStyle,
                        color = t.foreground,
                    )
                }
            }

            // Quick diagnostics row — the permission doctor (STEP 4 fix)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TerminalQuickButton("doctor", onRunCommand)
                TerminalQuickButton("ls", onRunCommand)
                TerminalQuickButton("uname -a", onRunCommand)
            }
        }
    }
}

private const val PREFIX_LABEL = "com.marid.app/files/usr"

@Composable
private fun TerminalQuickButton(label: String, onRunCommand: (String) -> Unit) {
    val t = maridTokens()
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(MaridRadius.Lg.dp))
            .background(t.surface)
            .clickable { onRunCommand(label) }
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            text = label,
            style = monoStyle,
            color = t.foreground,
        )
    }
}
