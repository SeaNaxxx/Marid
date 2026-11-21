package com.qodex.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qodex.app.icons.QodexIcons
import com.qodex.app.rpc.QodexRpcClient
import com.qodex.app.rpc.asObjectOrNull
import com.qodex.app.rpc.asStringOrNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

// Real engine PTY attempt: terminal.create → onDynamicData stream + write/resize.
// The shipped engine bundles node-pty as an x86-64 native module, so on the
// Bionic ARM64 runtime create() fails — the sheet then surfaces the ENGINE'S
// OWN error verbatim under the honest unavailable state (never a simulated
// shell, never a silent black box). If a Bionic node-pty build ships later,
// this same path comes alive with the touch accessory bar and input line.
@Composable
public fun QodexTerminalSheet(
    rpc: QodexRpcClient?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var sessionId by remember { mutableStateOf<String?>(null) }
    var engineError by remember { mutableStateOf<String?>(null) }
    var attemptMessage by remember { mutableStateOf("Connecting to engine PTY...") }
    var output by remember { mutableStateOf("") }
    var ctrlPending by remember { mutableStateOf(false) }
    var altPending by remember { mutableStateOf(false) }
    var input by remember { mutableStateOf(TextFieldValue("")) }
    var cols by remember { mutableIntStateOf(80) }
    var rows by remember { mutableIntStateOf(24) }
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()

    DisposableEffect(rpc) {
        val client = rpc
        var dataSub: com.qodex.app.rpc.ChannelClient.EventSubscription? = null
        var exitSub: com.qodex.app.rpc.ChannelClient.EventSubscription? = null
        val createdId = java.util.concurrent.atomic.AtomicReference<String?>(null)
        if (client == null) {
            engineError = "Engine not connected — terminal needs the engine RPC channel."
            attemptMessage = ""
        } else {
            scope.launch {
                runCatching { client.terminalCreate(cols, rows) }
                    .onFailure { t ->
                        // The engine's own failure text (missing/broken node-pty
                        // native module on this runtime) — shown verbatim.
                        engineError = t.message ?: t.javaClass.simpleName
                        attemptMessage = ""
                    }
                    .onSuccess { result ->
                        val id = result.asObjectOrNull()?.get("id").asStringOrNull()
                        if (id.isNullOrEmpty()) {
                            engineError = "terminal.create returned no session id"
                            attemptMessage = ""
                        } else {
                            createdId.set(id)
                            sessionId = id
                            output = ""
                            dataSub = client.terminalData(id) { payload ->
                                payload.asStringOrNull()?.let { data ->
                                    output = (output + data).takeLast(200_000)
                                    // Keep the latest PTY output in view.
                                    scope.launch { scroll.scrollTo(Int.MAX_VALUE) }
                                }
                            }
                            exitSub = client.terminalExit(id) { code ->
                                val exit = code.asStringOrNull() ?: ""
                                output += "\r\n[process exited ${if (exit.isEmpty()) "" else "(code $exit)"}]"
                                sessionId = null
                            }
                        }
                    }
            }
        }
        // onDispose must be the block's final expression (DisposableEffectResult).
        onDispose {
            runCatching { dataSub?.dispose() }
            runCatching { exitSub?.dispose() }
            createdId.get()?.let { id ->
                // rememberCoroutineScope() is already cancelled by the time
                // onDispose runs: launching terminalDispose into it was a silent
                // no-op, so the engine-side shell leaked on every sheet close.
                // A dedicated scope outlives the composition just long enough to
                // deliver terminal.dispose, then cancels itself.
                val outlivesScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
                outlivesScope.launch {
                    runCatching { client?.terminalDispose(id) }
                    outlivesScope.cancel()
                }
            }
        }
    }

    // Approximate PTY geometry from the terminal box size (monospace 12sp:
    // ~7.2dp per glyph column, ~17dp per line) and keep the engine's pty winsize in sync.
    LaunchedEffect(sessionId, cols, rows) {
        val id = sessionId ?: return@LaunchedEffect
        if (cols in 2..500 && rows in 2..200) {
            runCatching { rpc?.terminalResize(id, cols, rows) }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(QodexTokens.Background)
            .clipToBounds()
            .testTag("terminal")
            .padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 8.dp)
            .imePadding(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Terminal",
                color = QodexTokens.Foreground,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Row(
                modifier = Modifier.height(28.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                TerminalTab2e(onClose = onClose)
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                GhostIconButton2e(
                    icon = QodexIcons.X,
                    iconSize = 16.dp,
                    contentDescription = "Close terminal pane",
                    testTag = "terminal-close-button",
                    onClick = onClose,
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f) // fills a caller-bounded sheet; wraps when unbounded
                .background(QodexTokens.TerminalBg)
                .clipToBounds(),
            contentAlignment = Alignment.Center,
        ) {
            when {
                engineError != null -> Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Text(
                        text = "Terminal unavailable on Android",
                        color = QodexTokens.TerminalFg,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        // The engine's real failure reason — no invented blame.
                        text = engineError.orEmpty().take(400),
                        color = QodexTokens.ForegroundSubtle,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
                sessionId == null -> Text(
                    text = attemptMessage,
                    color = QodexTokens.TerminalFg,
                    fontSize = 13.sp,
                    fontFamily = FontFamily.Monospace,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                else -> Text(
                    text = output.ifEmpty { "" },
                    color = QodexTokens.TerminalFg,
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(scroll)
                        .padding(8.dp),
                )
            }
        }

        if (sessionId != null) {
            TerminalAccessoryBar2e(
                ctrlPending = ctrlPending,
                altPending = altPending,
                onCtrlToggle = { ctrlPending = !ctrlPending },
                onAltToggle = { altPending = !altPending },
                onRaw = { data -> sendToPty(scope, rpc, sessionId, data) },
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BasicTextField(
                    value = input,
                    onValueChange = { value ->
                        // Sticky CTRL applies to the next typed character:
                        // 'c' with CTRL pending sends the ETX control char (0x03).
                        val grew = value.text.length > input.text.length
                        if (ctrlPending && grew && value.text.length == input.text.length + 1) {
                            val typed = value.text.last().code
                            if (typed in 0x20..0x7e) {
                                sendToPty(
                                    scope, rpc, sessionId,
                                    (if (altPending) "\u001b" else "") +
                                        (typed and 0x1f).toChar().toString(),
                                )
                                ctrlPending = false
                                altPending = false
                            }
                        } else {
                            input = value
                        }
                    },
                    singleLine = true,
                    textStyle = TextStyle(
                        color = QodexTokens.Foreground,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                    ),
                    cursorBrush = SolidColor(QodexTokens.Foreground),
                    decorationBox = { inner -> inner() },
                    modifier = Modifier
                        .weight(1f)
                        .background(QodexTokens.TerminalBg, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                )
                TerminalKey2e(label = "↵") {
                    val text = input.text
                    input = TextFieldValue("")
                    sendToPty(
                        scope, rpc, sessionId,
                        (if (altPending) "\u001b" else "") + text + "\r",
                    )
                    altPending = false
                }
            }
        }
    }
}

private fun sendToPty(
    scope: kotlinx.coroutines.CoroutineScope,
    rpc: QodexRpcClient?,
    sessionId: String?,
    data: String,
) {
    val id = sessionId ?: return
    val client = rpc ?: return
    scope.launch { runCatching { client.terminalWrite(id, data) } }
}

// Touch accessory row for the PTY: sends real ANSI sequences into the engine's
// terminal stdio stream (task 2.1). CTRL is a sticky modifier applied to the
// next typed character; ALT prefixes ESC.
@Composable
private fun TerminalAccessoryBar2e(
    ctrlPending: Boolean,
    altPending: Boolean,
    onCtrlToggle: () -> Unit,
    onAltToggle: () -> Unit,
    onRaw: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TerminalKey2e(label = "CTRL", active = ctrlPending, onKey = onCtrlToggle)
        TerminalKey2e(label = "ALT", active = altPending, onKey = onAltToggle)
        TerminalKey2e(label = "ESC") { onRaw("\u001b"); }
        TerminalKey2e(label = "TAB") { onRaw("\t") }
        TerminalKey2e(label = "|") { onRaw("|") }
        TerminalKey2e(label = "-") { onRaw("-") }
        TerminalKey2e(label = "~") { onRaw("~") }
        TerminalKey2e(label = "/") { onRaw("/") }
        TerminalKey2e(label = "↑") { onRaw("\u001b[A") }
        TerminalKey2e(label = "↓") { onRaw("\u001b[B") }
        TerminalKey2e(label = "←") { onRaw("\u001b[D") }
        TerminalKey2e(label = "→") { onRaw("\u001b[C") }
    }
}

@Composable
private fun TerminalKey2e(
    label: String,
    active: Boolean = false,
    onKey: () -> Unit,
) {
    Box(
        modifier = Modifier
            .widthIn(min = 36.dp)
            .height(32.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                when {
                    active -> QodexTokens.Selected
                    else -> QodexTokens.Input
                },
            )
            .clickable { onKey() }
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            color = QodexTokens.Foreground,
            fontSize = 12.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
            fontFamily = FontFamily.Monospace,
        )
    }
}

@Composable
private fun TerminalTab2e(onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .widthIn(max = 144.dp)
            .height(28.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(QodexTokens.Selected),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "Terminal",
            color = QodexTokens.Foreground,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            modifier = Modifier
                .padding(start = 8.dp)
                .weight(1f, fill = false),
        )
        val interactionSource = remember { MutableInteractionSource() }
        val pressed by interactionSource.collectIsPressedAsState()
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (pressed) QodexTokens.SurfaceHover else Color.Transparent)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClose,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = QodexIcons.X,
                contentDescription = "Close Terminal",
                modifier = Modifier.size(12.dp),
                tint = QodexTokens.Foreground,
            )
        }
    }
}

@Composable
private fun GhostIconButton2e(
    icon: ImageVector,
    iconSize: Dp,
    contentDescription: String,
    testTag: String,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    Box(
        modifier = Modifier
            .size(28.dp)
            .testTag(testTag)
            .clip(RoundedCornerShape(8.dp))
            .background(if (pressed) QodexTokens.SurfaceHover else Color.Transparent)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(iconSize),
            tint = QodexTokens.Foreground,
        )
    }
}
