package com.qodex.app.ui

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qodex.app.icons.QodexIcons
import kotlinx.coroutines.launch

private object ChatAreaPalette {
    val Surface40 = Color(0x05FFFFFF)

    val Background50 = Color(0x80171717)

    val MutedBg15 = Color(0x26262626)

    val MutedForeground = QodexTokens.Neutral400

    val DestructiveBg10 = Color(0x1AFB2C36)

    val ShimmerSoft = Color(0x33FFFFFF)

    /** White 8% bubble composited over #171717. */
    val BubbleFadeBase = Color(0xFF2A2A2A)
}

object QodexIcons2b {

    val ArrowDown: ImageVector by lazy {
        ImageVector.Builder(
            name = "Qodex2b.ArrowDown",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            strokePath {
                moveTo(12f, 5f)
                lineTo(12f, 19f)
            }
            strokePath {
                moveTo(19f, 12f)
                lineTo(12f, 19f)
                lineTo(5f, 12f)
            }
        }.build()
    }

    val Wrench: ImageVector by lazy {
        ImageVector.Builder(
            name = "Qodex2b.Wrench",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            strokePath {
                moveTo(14.7f, 6.3f)
                arcToRelative(1f, 1f, 0f, false, false, 0f, 1.4f)
                lineToRelative(1.6f, 1.6f)
                arcToRelative(1f, 1f, 0f, false, false, 1.4f, 0f)
                lineToRelative(3.77f, -3.77f)
                arcToRelative(6f, 6f, 0f, false, true, -7.94f, 7.94f)
                lineToRelative(-6.91f, 6.91f)
                arcToRelative(2.12f, 2.12f, 0f, false, true, -3f, -3f)
                lineToRelative(6.91f, -6.91f)
                arcToRelative(6f, 6f, 0f, false, true, 7.94f, -7.94f)
                lineToRelative(-3.76f, 3.76f)
                close()
            }
        }.build()
    }

    private fun ImageVector.Builder.strokePath(block: androidx.compose.ui.graphics.vector.PathBuilder.() -> Unit) {
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = androidx.compose.ui.graphics.PathFillType.NonZero,
            pathBuilder = block,
        )
    }
}

private val UiBase = TextStyle(fontSize = 14.sp, lineHeight = 21.sp)
private val UiBaseMarkdown = TextStyle(
    fontSize = 14.sp,
    lineHeight = 24.5.sp, // leading-[1.75]
    letterSpacing = 0.35.sp, // tracking-wide (0.025em @ 14sp)
)
private val UiSm = TextStyle(fontSize = 12.sp, lineHeight = 18.sp)
private val UiCode = TextStyle(
    fontSize = 12.sp, // CodeViewer fontSizePx = 12
    lineHeight = 16.sp,
    fontFamily = FontFamily.Monospace, // --font-mono
)
private val TabularNums = TextStyle(fontFeatureSettings = "tnum") // .tabular-nums

private val Radius12 = RoundedCornerShape(12.dp) // rounded-xl
private val Radius6 = RoundedCornerShape(6.dp) // rounded-md

private val CssEaseInOut = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)

// reverseLayout keeps the latest message bottom-anchored; auto-follows while pinned.
@Composable
fun QodexChatArea(
    turns: List<ChatTurn>,
    modifier: Modifier = Modifier,
    onRetryLast: (() -> Unit)? = null,
    turnRunning: Boolean = false,
) {
    // Product UI renders no retry entry; callback retained for host API symmetry.
    onRetryLast?.let { /* intentionally unused; see header deviation note 2 */ }

    val listState = rememberLazyListState()
    var pinnedToBottom by remember { mutableStateOf(true) }

    LaunchedEffect(listState) {
        snapshotFlow {
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
        }.collect { pinnedToBottom = it }
    }

    // While pinned, new content keeps the view anchored to the latest message.
    LaunchedEffect(turns.size) {
        if (turns.isNotEmpty() && pinnedToBottom) listState.scrollToItem(0)
    }

    val showBackToBottom by remember {
        derivedStateOf {
            listState.firstVisibleItemIndex != 0 ||
                listState.firstVisibleItemScrollOffset > 24
        }
    }
    val scope = rememberCoroutineScope()

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            reverseLayout = true,
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 20.dp),
        ) {
            val count = turns.size
            items(
                count = count,
                key = { index -> "turn-${count - 1 - index}" },
            ) { index ->
                when (val turn = turns[count - 1 - index]) {
                    is ChatTurn.UserTurn -> UserTurnItem(turn)
                    is ChatTurn.AssistantTurn -> AssistantTurnItem(turn)
                    is ChatTurn.ToolCallTurn -> ToolCallCard(
                        name = turn.name,
                        status = turn.status,
                        summary = turn.summary,
                        detail = turn.detail,
                        progressFraction = turn.progressFraction,
                        progressMessage = turn.progressMessage,
                    )
                }
            }
        }

        if (showBackToBottom) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp) // bottom-3
                    .size(28.dp) // size-7 (Button size="icon")
                    .clip(CircleShape) // rounded-full
                    .background(QodexTokens.Card) // bg-card
                    .border(1.dp, QodexTokens.Border, CircleShape) // variant outline border-border
                    .clickable { scope.launch { listState.animateScrollToItem(0) } },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = QodexIcons2b.ArrowDown,
                    contentDescription = "Scroll to bottom", // chat.scrollToBottom
                    modifier = Modifier.size(16.dp), // ArrowDownIcon size-4
                    tint = QodexTokens.Foreground, // variant outline text-foreground
                )
            }
        }

        // Live turn indicator: a running turn must never look like a silent hang.
        if (turnRunning) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(start = 20.dp, bottom = 10.dp)
                    .clip(Radius12)
                    .background(QodexTokens.Card)
                    .border(1.dp, QodexTokens.Border, Radius12)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val pulse = rememberInfiniteTransition(label = "working-pulse")
                val dotAlpha = pulse.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(700, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse,
                    ),
                    label = "working-dot",
                )
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .background(QodexTokens.ForegroundSubtle.copy(alpha = dotAlpha.value), CircleShape),
                )
                Text(
                    text = "Working",
                    color = QodexTokens.ForegroundSubtle,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
private fun UserTurnItem(turn: ChatTurn.UserTurn) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End,
    ) {
        UserBubble(text = turn.text)
    }
}

// Upstream bubble border utility is overridden to width 0 — rendered borderless.
@Composable
private fun UserBubble(text: String) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var overflowing by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .clip(Radius12) // rounded-xl (+ styles.css 12px top-right override)
            .background(QodexTokens.UserMessageBubble) // --color-user-message-bubble
            .padding(horizontal = 16.dp, vertical = 12.dp), // px-4 py-3
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { // gap-2
            Box {
                Box(
                    modifier = when {
                        expanded || !overflowing -> Modifier
                        else -> Modifier.heightIn(max = 120.dp) // COLLAPSED_USER_INPUT_CONTENT_MAX_HEIGHT_PX
                    },
                ) {
                    Text(
                        text = text,
                        style = UiBase.copy(color = QodexTokens.Foreground), // text-ui-base text-foreground
                        softWrap = true, // whitespace-pre-wrap break-words
                        onTextLayout = { result -> overflowing = result.didOverflowHeight },
                        modifier = Modifier.clipToBounds(),
                    )
                    if (!expanded && overflowing) {
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(
                                    Brush.verticalGradient(
                                        0f to Color.Transparent,
                                        0.7f to Color.Transparent,
                                        1f to ChatAreaPalette.BubbleFadeBase,
                                    ),
                                ),
                        )
                    }
                }
                if (!expanded && overflowing) {
                    ExpandChevronButton(
                        expanded = false,
                        onClick = { expanded = true },
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }
            if (expanded && overflowing) {
                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                    ExpandChevronButton(
                        expanded = true,
                        onClick = { expanded = false },
                        modifier = Modifier.align(Alignment.CenterVertically),
                    )
                }
            }
        }
    }
}

@Composable
private fun ExpandChevronButton(expanded: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(24.dp) // size sm h-6
            .clip(CircleShape) // rounded-full
            .background(QodexTokens.Background) // bg-background
            .border(1.dp, QodexTokens.Border, CircleShape) // variant outline border-border
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp), // px-2
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = QodexIcons.ChevronDown,
            contentDescription = null,
            modifier = Modifier
                .size(16.dp) // size-4
                .rotate(if (expanded) 180f else 0f), // ChevronUp == ChevronDown flipped
            tint = QodexTokens.Foreground, // variant outline text-foreground
        )
    }
}

private sealed interface TurnUnit {
    data class Single(val block: AssistantBlock) : TurnUnit
    data class WorkRun(val blocks: List<AssistantBlock>) : TurnUnit
}

// Consecutive tool/reasoning rows group into one 4dp-gap run; text and change-summary panels break it at 20dp.
@Composable
private fun AssistantTurnItem(turn: ChatTurn.AssistantTurn) {
    val units = remember(turn.blocks) { buildTurnUnits(turn.blocks) }
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        units.forEach { unit ->
            when (unit) {
                is TurnUnit.Single -> AssistantBlockItem(unit.block)
                is TurnUnit.WorkRun -> Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp), // gap-1
                ) {
                    unit.blocks.forEach { AssistantBlockItem(it) }
                }
            }
        }
    }
}

private fun buildTurnUnits(blocks: List<AssistantBlock>): List<TurnUnit> {
    val units = mutableListOf<TurnUnit>()
    val run = mutableListOf<AssistantBlock>()
    fun flush() {
        if (run.isNotEmpty()) units += TurnUnit.WorkRun(run.toList())
        run.clear()
    }
    for (block in blocks) {
        when (block) {
            is AssistantBlock.ToolCall, is AssistantBlock.Reasoning -> run += block
            else -> {
                flush()
                units += TurnUnit.Single(block)
            }
        }
    }
    flush()
    return units
}

@Composable
private fun AssistantBlockItem(block: AssistantBlock) {
    when (block) {
        is AssistantBlock.MessageText -> MessageTextBlock(block.text)
        is AssistantBlock.ToolCall -> ToolCallCard(
            name = block.name,
            status = block.status,
            summary = block.summary,
            detail = block.detail,
            progressFraction = block.progressFraction,
            progressMessage = block.progressMessage,
        )
        is AssistantBlock.Reasoning -> ReasoningCard(block)
        is AssistantBlock.ChangeSummary -> ChangeSummaryCard(block)
    }
}

// Markdown rendering simplified: plain text + fenced code blocks only.
// Engine status lines ([turn error]/[engine]) render in destructive red: a
// failure typed as plain prose reads as assistant content, which hides it.
@Composable
private fun MessageTextBlock(text: String) {
    val isErrorLine = text.startsWith("[turn error]") || text.startsWith("[engine]")
    Column(modifier = Modifier.fillMaxWidth()) {
        splitFencedCode(text).forEach { piece ->
            when (piece) {
                is TextPiece.Prose -> Text(
                    text = piece.text,
                    style = UiBaseMarkdown.copy(
                        color = if (isErrorLine) QodexTokens.Destructive else QodexTokens.Foreground,
                    ),
                    softWrap = true,
                )
                is TextPiece.Code -> CodeFrame(piece)
            }
        }
    }
}

@Composable
private fun CodeFrame(piece: TextPiece.Code) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Radius12) // rounded-xl
            .background(ChatAreaPalette.MutedBg15) // bg-muted/15
            .padding(start = 8.dp, end = 8.dp, bottom = 12.dp), // p-2 pt-0 pb-3
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 240.dp) // max-h-60 overflow-auto
                .verticalScroll(rememberScrollState())
                .clip(Radius12) // rounded-xl
                .background(QodexTokens.Background) // bg-background
                .padding(8.dp),
        ) {
            Text(
                text = piece.code.trimEnd('\n'),
                style = UiCode.copy(color = QodexTokens.Foreground), // text-foreground
                softWrap = true,
            )
        }
    }
}

private sealed interface TextPiece {
    data class Prose(val text: String) : TextPiece
    data class Code(val language: String?, val code: String) : TextPiece
}

// An unterminated fence (streaming) keeps the tail as code.
private fun splitFencedCode(text: String): List<TextPiece> {
    val pieces = mutableListOf<TextPiece>()
    var cursor = 0
    while (cursor <= text.length) {
        val open = text.indexOf("```", cursor)
        if (open < 0) {
            if (cursor < text.length) pieces += TextPiece.Prose(text.substring(cursor))
            break
        }
        if (open > cursor) pieces += TextPiece.Prose(text.substring(cursor, open))
        val lineEnd = text.indexOf('\n', open)
        if (lineEnd < 0) {
            pieces += TextPiece.Code(language = null, code = text.substring(open + 3))
            break
        }
        val language = text.substring(open + 3, lineEnd).trim().takeIf { it.isNotEmpty() }
        var close = text.indexOf("\n```", lineEnd)
        if (close < 0) {
            pieces += TextPiece.Code(language = language, code = text.substring(lineEnd + 1))
            break
        }
        pieces += TextPiece.Code(language = language, code = text.substring(lineEnd + 1, close))
        cursor = close + 4
        if (cursor < text.length && text[cursor] == '\r') cursor++
        if (cursor < text.length && text[cursor] == '\n') cursor++
        if (cursor == text.length) break
    }
    return pieces
}

@Composable
private fun ToolCallCard(
    name: String,
    status: ToolCallStatus,
    summary: String,
    detail: String?,
    progressFraction: Float?,
    progressMessage: String?,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val isRunning = status == ToolCallStatus.RUNNING || status == ToolCallStatus.PENDING
    val failed = status == ToolCallStatus.FAILED

    Column(modifier = Modifier.fillMaxWidth()) { // Collapsible w-full flex flex-col
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(Radius6)
                .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp), // gap-2
        ) {
            if (isRunning) {
                SpinningLoader()
            }
            Icon(
                imageVector = QodexIcons2b.Wrench,
                contentDescription = null,
                modifier = Modifier.size(16.dp), // size-4
                tint = QodexTokens.ForegroundSubtle,
            )
            val kindLabel = name.replaceFirstChar { it.uppercase() }
            if (isRunning) {
                Text(
                    text = kindLabel,
                    style = TextStyle(brush = rememberGradientFlowBrush(), fontSize = 14.sp),
                    maxLines = 1,
                )
            } else {
                Text(
                    text = kindLabel,
                    style = UiBase.copy(color = QodexTokens.ForegroundSubtle),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = summary,
                style = UiBase.copy(color = QodexTokens.ForegroundSubtle),
                softWrap = true,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (isRunning && (progressFraction != null || progressMessage != null)) {
                ProgressIndicator(
                    fraction = progressFraction,
                    message = progressMessage,
                    modifier = Modifier.weight(1f, fill = false),
                )
            }
            Text(
                text = status.label,
                style = UiBase.copy(color = QodexTokens.ForegroundSubtle),
                maxLines = 1,
            )
            // Collapsed chevron is hover-revealed upstream — hidden on touch, shown only when expanded.
            if (expanded) {
                Icon(
                    imageVector = QodexIcons.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier
                        .size(16.dp)
                        .rotate(animateFloatAsState(90f, tween(200), label = "tool-chevron").value),
                    tint = QodexTokens.ForegroundSubtlest,
                )
            }
        }

        if (expanded) {
            Column(modifier = Modifier.padding(top = 8.dp)) { // pt-2
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(Radius12)
                        .border(1.dp, QodexTokens.Border, Radius12)
                        .background(ChatAreaPalette.Surface40)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp), // space-y-3
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { // space-y-2
                        Row(
                            modifier = Modifier.height(24.dp), // min-h-6
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = if (failed) "Error" else "Result", // fallback.error / result
                                style = UiSm.copy(color = QodexTokens.ForegroundSubtle),
                            )
                        }
                        val body = detail
                        if (body == null) {
                            Text(
                                text = if (isRunning) "Waiting for result" else "No result available",
                                style = UiBase.copy(color = QodexTokens.ForegroundSubtlest),
                                softWrap = true,
                            )
                        } else if (failed) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 240.dp)
                                    .verticalScroll(rememberScrollState())
                                    .clip(Radius6)
                                    .background(ChatAreaPalette.DestructiveBg10)
                                    .padding(12.dp),
                            ) {
                                Text(
                                    text = body,
                                    style = UiBase.copy(color = QodexTokens.Destructive),
                                    softWrap = true,
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 240.dp) // max-h-60 overflow-auto
                                    .verticalScroll(rememberScrollState()),
                            ) {
                                Text(
                                    text = body,
                                    style = UiBase.copy(color = QodexTokens.ForegroundSubtle),
                                    softWrap = true,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SpinningLoader() {
    val transition = rememberInfiniteTransition(label = "animate-spin")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing)),
        label = "spin",
    )
    Icon(
        imageVector = QodexIcons.LoaderCircle,
        contentDescription = null,
        modifier = Modifier.size(12.dp).rotate(angle),
        tint = QodexTokens.ForegroundSubtle,
    )
}

@Composable
private fun ProgressIndicator(fraction: Float?, message: String?, modifier: Modifier = Modifier) {
    val animatedFraction by animateFloatAsState(
        targetValue = fraction ?: 0f,
        animationSpec = tween(200),
        label = "progress-width",
    )
    val transition = rememberInfiniteTransition(label = "tool-call-progress-indeterminate")
    val sweep by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = CssEaseInOut)),
        label = "sweep",
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp), // gap-2
    ) {
        Text(
            text = "·",
            style = UiBase.copy(color = QodexTokens.ForegroundSubtlest), // text-foreground-subtlest
        )
        val trackHeight = 4.dp // h-1
        Box(
            modifier = Modifier
                .width(64.dp) // w-16
                .height(trackHeight)
                .clip(CircleShape) // rounded-full
                .background(QodexTokens.SurfaceHover) // bg-surface-hover
                .drawBehind {
                    val w = size.width
                    val h = size.height
                    val corner = androidx.compose.ui.geometry.CornerRadius(h / 2f, h / 2f)
                    if (fraction != null) {
                        drawRoundRect(
                            color = QodexTokens.Brand, // bg-brand
                            topLeft = Offset.Zero,
                            size = Size(w * animatedFraction, h),
                            cornerRadius = corner,
                        )
                    } else {
                        val x = (-1f / 3f + sweep * (4f / 3f)) * w
                        drawRoundRect(
                            color = QodexTokens.Brand,
                            topLeft = Offset(x, 0f),
                            size = Size(w / 3f, h),
                            cornerRadius = corner,
                        )
                    }
                },
        )
        if (message != null) {
            Text(
                text = message,
                style = UiSm.copy(color = QodexTokens.ForegroundSubtle),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis, // truncate
                modifier = Modifier.weight(1f, fill = false),
            )
        }
    }
}

@Composable
private fun ReasoningCard(block: AssistantBlock.Reasoning) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) { // not-prose min-w-0 flex flex-col
        Row(
            modifier = Modifier
                .clip(Radius6)
                .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp), // gap-2
        ) {
            Icon(
                imageVector = QodexIcons.Brain,
                contentDescription = null,
                modifier = Modifier.size(16.dp), // size-4
                tint = QodexTokens.ForegroundSubtle, // group-hover:text-foreground omitted
            )
            if (block.streaming && !expanded) {
                Text(
                    text = "Thinking", // chat.reasoning.thinking
                    style = TextStyle(brush = rememberGradientFlowBrush(), fontSize = 14.sp),
                    maxLines = 1,
                )
            } else {
                Text(
                    text = "Thought", // chat.reasoning.thought
                    style = UiBase.copy(color = QodexTokens.ForegroundSubtle),
                    maxLines = 1,
                )
                Text(
                    text = block.durationSeconds?.let { "$it seconds" } ?: "a few seconds",
                    style = UiBase.copy(color = QodexTokens.ForegroundSubtlest),
                    maxLines = 1,
                )
            }
            if (expanded) {
                Icon(
                    imageVector = QodexIcons.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier
                        .size(16.dp)
                        .rotate(
                            animateFloatAsState(90f, tween(200), label = "reasoning-chevron").value,
                        ),
                    tint = QodexTokens.ForegroundSubtlest,
                )
            }
        }

        if (expanded) {
            Column(modifier = Modifier.padding(top = 12.dp)) { // pt-3
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp) // max-h-60 overflow-auto
                        .verticalScroll(rememberScrollState())
                        .padding(start = 8.dp) // ml-2
                        .drawBehind {
                            drawRect(
                                color = QodexTokens.Border,
                                topLeft = Offset.Zero,
                                size = Size(1.dp.toPx(), size.height),
                            )
                        }
                        .padding(start = 15.dp), // pl-3.5 (14dp) measured after the 1dp border
                ) {
                    Text(
                        text = block.text,
                        style = UiBase.copy(color = QodexTokens.ForegroundSubtlest),
                        softWrap = true,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChangeSummaryCard(block: AssistantBlock.ChangeSummary) {
    var open by rememberSaveable { mutableStateOf(false) }

    val totalAdded = block.files.sumOf { it.added }
    val totalRemoved = block.files.sumOf { it.removed }
    val fileCount = block.files.size
    val header = if (fileCount == 1) "$fileCount file changed" else "$fileCount files changed"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Radius12) // rounded-xl overflow-hidden
            .border(1.dp, QodexTokens.Border, Radius12) // border border-border
            .background(QodexTokens.Card), // bg-card (dark neutral-800)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp) // h-10
                .clickable { open = !open }
                .padding(horizontal = 8.dp), // px-2
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp), // gap-2
            ) {
                Icon(
                    imageVector = QodexIcons.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier
                        .size(14.dp) // size-3.5
                        .rotate(
                            animateFloatAsState(
                                targetValue = if (open) 90f else 0f,
                                animationSpec = tween(200),
                                label = "panel-chevron",
                            ).value,
                        ),
                    tint = QodexTokens.ForegroundSubtlest, // text-foreground-subtlest
                )
                Text(
                    text = header, // chat.changeSummary.filesChanged.one/other
                    style = UiBase.copy(
                        color = QodexTokens.Foreground,
                        fontWeight = FontWeight.Medium, // font-medium
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis, // truncate
                    modifier = Modifier.weight(1f, fill = false),
                )
                DiffCounts(added = totalAdded, removed = totalRemoved) // tabular-nums
            }
        }

        if (open) {
            block.files.forEach { file ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ChatAreaPalette.Background50)
                        .padding(horizontal = 8.dp, vertical = 8.dp), // px-2 py-2
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp), // gap-1
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp), // gap-2
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp), // gap-1.5
                            modifier = Modifier.weight(1f, fill = false),
                        ) {
                            Icon(
                                imageVector = QodexIcons.File,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp), // iconSize 16
                                tint = QodexTokens.ForegroundSubtle,
                            )
                            val name = file.path.substringAfterLast('/')
                            val dir = file.path.removeSuffix(name).removeSuffix("/")
                            Text(
                                text = name, // fileNameClassName font-medium text-foreground
                                style = UiBase.copy(
                                    color = QodexTokens.Foreground,
                                    fontWeight = FontWeight.Medium,
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis, // truncate
                            )
                            if (dir.isNotEmpty()) {
                                Text(
                                    text = dir, // filePathClassName text-foreground-subtlest
                                    style = UiBase.copy(color = QodexTokens.ForegroundSubtlest),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis, // truncate
                                )
                            }
                        }
                        DiffCounts(added = file.added, removed = file.removed)
                    }
                }
            }
        }
    }
}

@Composable
private fun DiffCounts(added: Int, removed: Int) {
    if (added <= 0 && removed <= 0) return
    Text(
        text = buildAnnotatedString {
            if (added > 0) {
                withStyle(SpanStyle(color = QodexTokens.DiffAdded)) { append("+$added") }
            }
            if (added > 0 && removed > 0) append(" ")
            if (removed > 0) {
                withStyle(SpanStyle(color = QodexTokens.DiffRemoved)) { append("-$removed") }
            }
        },
        style = TabularNums.copy(fontSize = 14.sp, lineHeight = 21.sp), // text-ui-base tabular-nums
    )
}

// CSS gradient-flow is a 4s cycle held half the time; only the 2s sweep is animated here.
@Composable
internal fun rememberGradientFlowBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "gradientFlow")
    val position by transition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
        ),
        label = "gradientFlowPosition",
    )
    return GradientFlowBrush(position)
}

private fun GradientFlowBrush(position: Float): Brush {
    val soft = QodexTokens.ForegroundSubtle
    val strong = QodexTokens.Foreground
    return Brush.linearGradient(
        0f to strong,
        (position - 0.16f).coerceIn(0f, 1f) to strong,
        position.coerceIn(0f, 1f) to soft,
        (position + 0.16f).coerceIn(0f, 1f) to strong,
        1f to strong,
    )
}
