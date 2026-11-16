package com.marid.app.ui
// marid-sync-1

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
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
import com.marid.app.icons.MaridIcons
import kotlinx.coroutines.launch

/*
 * =============================================================================
 * MaridChatArea — 1:1 Compose port of the ZCode Desktop v4 chat transcript.
 * =============================================================================
 *
 * REACT FILES PORTED (source of truth: packages/ui/src):
 *   - v4/ConversationUserInputBubble.tsx        user message bubble
 *   - v4/ConversationUserInputBody.tsx          bubble collapse + expand affordance
 *   - v4/ConversationUserInputContent.tsx       bubble text (plain, pre-wrap)
 *   - v4/ConversationRowView.tsx                RowShell paddings, assistant text
 *                                               row, reasoning row, tool row shell
 *   - v4/ConversationTurnGroup.tsx              conversation column + turn spacing
 *   - v4/ConversationTimeline.tsx               back-to-bottom button
 *   - ToolCallBlocks.tsx + ToolCallBlocks/ToolLayout.tsx, ToolSummaryRow.tsx,
 *     ToolCallBody.tsx, ToolCallBlocks/renderers/fallback.tsx,
 *     ToolCallBlocks/session/ToolDetailSection.tsx,
 *     ToolCallBlocks/ToolSnapshotFieldNotice.tsx    tool call block frame/summary
 *   - ToolCallBlocks/ToolCallProgressIndicator.tsx  running progress bar
 *   - components/ai-elements/reasoning.tsx      reasoning collapse affordance
 *   - components/ai-elements/tool.tsx           ToolOutput error frame values
 *   - components/ai-elements/code-block.tsx     code block frame values
 *   - components/ai-elements/message.tsx        markdown container typography
 *   - MessageChangeSummaryPanel.tsx             change summary panel
 *   - lib/fileDisplay.tsx                       file row layout
 *   - lib/toolCallSummary.ts + shared tool-call-summary.ts   status model
 *   - i18n/locales/en-US.ts                     all user-facing strings
 *
 * -----------------------------------------------------------------------------
 * EXACT VALUE EXTRACTION (Tailwind class -> dp/sp; 1 unit = 4dp)
 * -----------------------------------------------------------------------------
 * Conversation column (ConversationTurnGroup.tsx:1487):
 *   px-4 -> 16dp horizontal padding;  pb-5 -> 20dp bottom;  gap-5 -> 20dp
 *   between turns/segments.  Work-item container (:317) flex flex-col gap-1 ->
 *   4dp between consecutive tool/reasoning rows.
 *
 * User bubble (ConversationUserInputBubble.tsx:30 + styles.css:2359):
 *   bg-surface  overridden by [data-v4-user-input-bubble] background-color
 *     var(--color-user-message-bubble) = white 8%  -> MaridTokens.UserMessageBubble
 *   border border-border -> KILLED by styles.css:2360 border-width:0 (unlayered
 *     rule wins over layered utilities) -> no border
 *   rounded-xl -> 12dp; rounded-tr-xs (2dp) overridden by styles.css:2361
 *     border-top-right-radius:12px -> 12dp on all corners
 *   px-4 py-3 -> 16dp / 12dp;  gap-2 -> 8dp;  text-ui-base text-foreground ->
 *     14sp, Foreground; bubble text line-height inherits 1.5 -> 21sp
 *     (confirmed by ConversationUserInputContent.tsx:72 "气泡正文（21px）")
 *   max-w-xl @min-[624px]/conversation -> 576dp cap only on wide containers;
 *     mobile width < 624px -> max-w-full (no cap).
 *   Collapse affordance (ConversationUserInputBody.tsx): content max-height
 *     120px; mask fade black 0% -> 70% -> transparent 100%; expand button:
 *     size sm variant outline -> h-6 (24dp) px-2 (8dp) rounded-full bg-background
 *     border-border text-foreground, chevron size-4 (16dp) (ChevronUp is
 *     ChevronDown rotated 180deg — identical Lucide geometry); expanded state
 *     drops below content with pt-2 (8dp).
 *
 * Assistant text (ConversationRowView.tsx:1613 + message.tsx:2003):
 *   w-full text-ui-base -> 14sp on Background; markdown container
 *   leading-[1.75] -> 24.5sp line-height; tracking-wide -> 0.025em -> 0.35sp
 *   letter spacing. No bubble/chrome (plain text on Background).
 *   Code blocks (code-block.tsx:157,342 + InlineCodeContent ToolCallBody.tsx:129):
 *   rounded-xl (12dp) bg-muted/15 wrapper (shadcn dark default --muted
 *   oklch(0.269 0 0) = #262626 at 15%), inner rounded-xl bg-background plate
 *   (#171717), code font-mono fontSizePx 12 (code-viewer.tsx:452), wrapper
 *   padding p-2 pt-0 pb-3 -> 8dp sides / 12dp bottom, max-h-60 -> 240dp scroll.
 *
 * Tool call block (ToolLayout.tsx + ToolSummaryRow.tsx + fallback.tsx):
 *   Row: w-full flex flex-col; header inline-flex items-center gap-2 (8dp)
 *   text-ui-base (14sp); icon slot size-4 (16dp) text-foreground-subtle;
 *   kind label font-normal whitespace-nowrap text-foreground-subtle, running:
 *   .animated-gradient-text.animated-gradient-text-foreground (styles.css:957,
 *   gradient strong=Foreground #E5E5E5, soft=rgba(255,255,255,.2) at 50%, ramp
 *   spans tile 34%..66% = +/-0.48 element widths, background-size 300%,
 *   gradient-flow 4s linear: position 100%->0% over 2s then held 2s);
 *   summary content gap-2 (8dp) font-normal text-foreground-subtle;
 *   chevron size-4 (16dp) text-foreground-subtlest rotate-90 when expanded
 *   (duration-200), collapsed opacity-0 group-hover:opacity-100 -> mobile:
 *   hidden when collapsed (no hover; expanded shows rotated chevron exactly).
 *   Collapsible body: CollapsibleContent text-popover-foreground + inner
 *   pt-2 (8dp); generic detail frame (fallback.tsx:79):
 *   rounded-xl (12dp) border border-border (white 10%) bg-surface/40
 *   (white 5% x 40% = white 2%) p-3 (12dp) space-y-3 (12dp);
 *   ToolDetailSection: header text-ui-sm (12sp) text-foreground-subtle
 *   min-h-6; content max-h-60 (240dp) text-ui-base text-foreground-subtle;
 *   empty content strings: chat.toolCall.fallback.waiting "Waiting for result"
 *   / chat.toolCall.fallback.noResult "No result available" (text-foreground
 *   -subtlest). Failed detail body (ai-elements/tool.tsx:157 ToolOutput error
 *   branch): rounded-md (6dp) bg-destructive/10 (Red500 10%) text-destructive
 *   p-3 (12dp).
 *   Status word (ToolLayout.tsx:292): plain whitespace-nowrap span, color =
 *   SummaryContent text-foreground-subtle (exact source classes — the source
 *   does not tint status words per state).
 *   Running spinner: LoaderCircle size-3 (12dp) animate-spin (1s linear
 *   infinite) — exact classes from ToolSnapshotFieldNotice.tsx:65, the only
 *   animate-spin spinner in the tool-call-blocks area (see deviations).
 *   Progress (ToolCallProgressIndicator.tsx): leading "." dot
 *   text-foreground-subtlest; track h-1 (4dp) w-16 (64dp) rounded-full
 *   bg-surface-hover (white 10%); determinate fill bg-brand (dark sky-500 =
 *   MaridTokens.Brand) width transition 200ms; indeterminate segment w-1/3
 *   sweeping left -33% -> 100% over 1.2s ease-in-out (CSS cubic-bezier
 *   (0.42,0,0.58,1)) infinite; message text-ui-sm (12sp)
 *   text-foreground-subtle truncate.
 *
 * Reasoning (ai-elements/reasoning.tsx + ConversationRowView.tsx:1706):
 *   trigger: inline-flex items-center gap-2 (8dp) text-ui-base; BrainIcon
 *   size-4 (16dp) text-foreground-subtle; label font-normal
 *   text-foreground-subtle ("Thought"/"Thinking"), duration font-normal
 *   text-foreground-subtlest; chevron ChevronRight size-4
 *   text-foreground-subtlest rotate-90 when open; streaming label =
 *   animated-gradient-text animated-gradient-text-foreground.
 *   content: pt-3 (12dp) -> max-h-60 (240dp) overflow-auto text-ui-base
 *   text-foreground-subtlest; ml-2 (8dp) border-border border-l (1dp white
 *   10%) pl-3.5 (14dp) left guide line; whitespace-pre-wrap break-words.
 *
 * Change summary panel (MessageChangeSummaryPanel.tsx):
 *   container: rounded-xl (12dp) border border-border bg-card (dark neutral-800
 *   #262626) overflow-hidden shadow-none; header h-10 (40dp) px-2 (8dp) gap-3
 *   (12dp) justify-between (hover:bg-hover omitted — no hover on touch);
 *   trigger px-1 (4dp) gap-2 (8dp); chevron size-3.5 (14dp)
 *   text-foreground-subtlest rotate-90 when open; title truncate text-ui-base
 *   font-medium text-foreground (14sp); counts tabular-nums text-ui-base with
 *   text-diff-added (green-500) / text-diff-removed (red-500);
 *   file rows: bg-background/50 (neutral-900 50%) px-2 py-2 (8dp/8dp) gap-1
 *   (4dp); file display gap-1.5 (6dp), icon 16dp, file name truncate
 *   text-ui-base font-medium text-foreground, file path truncate text-ui-base
 *   text-foreground-subtlest; per-file counts gap-2 (8dp) tabular-nums
 *   text-ui-base, +N text-diff-added only when added>0, -N text-diff-removed
 *   only when removed>0.
 *
 * Back-to-bottom (ConversationTimeline.tsx:150-179, 2256-2262):
 *   Button size icon variant outline -> size-7 (28dp) rounded-md(6dp) overridden
 *   by rounded-full; border-border text-foreground; bg-card (#262626)
 *   hover:bg-card-selected (hover omitted); ArrowDownIcon size-4 (16dp);
 *   positioned absolute bottom-3 (12dp) left-1/2 centered; label
 *   "chat.scrollToBottom" = "Scroll to bottom".
 *
 * Strings (en-US locale): Pending/Running/Completed/Failed/Denied
 *   (chat.toolCall.status.*), Result/Error (chat.toolCall.result,
 *   chat.toolCall.fallback.error), Thinking/Thought/{s} seconds/a few seconds
 *   (chat.reasoning.*), {n} file(s) changed (chat.changeSummary.filesChanged.*),
 *   Scroll to bottom (chat.scrollToBottom), Waiting for result /
 *   No result available (chat.toolCall.fallback.*).
 *
 * -----------------------------------------------------------------------------
 * DEVIATIONS (desktop-only features without a mobile visual; all documented,
 * none invented):
 *   1. Hover-only affordances omitted (touch has no hover): tool summary
 *      chevron fade-in on hover, summary-hover-text, hover:bg-hover surfaces,
 *      card-selected hover states. Expanded state keeps its chevron visible
 *      (matches the hover-free behavior).
 *   2. Desktop hover MessageActions (copy / fork / feedback / retry / edit /
 *      timestamps) omitted — the source renders them only on hover
 *      (opacity-0 group-hover:opacity-100). onRetryLast is accepted for API
 *      compatibility; the product UI intentionally renders no retry entry
 *      (ConversationRowView.tsx:355 "产品 UI 不渲染普通重试入口").
 *   3. ConsecutiveSummaryGroup rollup ("2 explored · 1 ran") omitted — mobile
 *      renders each work row individually (4dp gaps), preserving content.
 *   4. Tool-specific renderers (renderers/*) out of scope per task; the generic
 *      ToolLayout frame + fallback detail body is ported. Tool icon is the
 *      generic Wrench (fallback.tsx FALLBACK_TOOL_ICON).
 *   5. Sub-agent source label chip (bg-background-alt badge) omitted — no
 *      source data in the ChatTurn model.
 *   6. Failure copy-to-clipboard tooltip (statusTooltip + icon-md button)
 *      omitted — desktop tooltip interaction.
 *   7. Radix 300ms height collapse animation + delayed unmount omitted —
 *      content toggles instantly (mobile perf parity with the
 *      disableSummaryContentAnimation path); chevron rotation (200ms), shimmer
 *      and progress animations kept.
 *   8. prefers-reduced-motion variants omitted (mobile port always animates).
 *   9. shadow-sm on the back-to-bottom button and bubble expand button omitted
 *      (CSS 1px/2px box shadows have no equal Compose primitive; the outline
 *      border carries the affordance).
 *  10. Markdown rich rendering (headings/lists/links/inline styles via
 *      streamdown) simplified per task to plain text + fenced code blocks;
 *      inline `code`, tables and citations not ported. Streaming text-roll
 *      (QueuedSummaryContent) rendered as static text.
 *  11. ChatLoading orbs (chat-loading.tsx) omitted — 105-dot animated orb
 *      render; not part of the ChatTurn model (turn slots only).
 *  12. squircle corner-shape (24px top-right on supporting browsers,
 *      styles.css:2347) not portable — 12dp standard radii used (the source's
 *      non-squircle fallback is exactly 12px).
 *  13. File-type icon images (lib/fileDisplay) mapped to the neutral
 *      MaridIcons.File glyph tinted foreground-subtle.
 */

/** Private palette additions — exact conversions, each named by its Tailwind class. */
private object ChatAreaPalette {
    /** bg-surface/40 — fallback.tsx:79 detail frame. */
    val Surface40 = Color(0x05FFFFFF)

    /** bg-background/50 — change summary file rows. */
    val Background50 = Color(0x80171717)

    /** bg-muted/15 — shadcn dark `--muted` oklch(0.269 0 0) = #262626 at 15%. */
    val MutedBg15 = Color(0x26262626)

    /** text-muted-foreground — shadcn dark `--muted-foreground` oklch(0.708 0 0) = #A1A1A1. */
    val MutedForeground = MaridTokens.Neutral400

    /** bg-destructive/10 — ToolOutput error plate (Red500 #FB2C36 at 10%). */
    val DestructiveBg10 = Color(0x1AFB2C36)

    /** --animated-gradient-text-soft (dark): rgba(255, 255, 255, 0.2). */
    val ShimmerSoft = Color(0x33FFFFFF)

    /** User bubble backdrop = UserMessageBubble (white 8%) composited over #171717. */
    val BubbleFadeBase = Color(0xFF2A2A2A)
}

// ---------------------------------------------------------------------------
// animated-gradient-text (styles.css:957-1004, dark foreground variant)
// ---------------------------------------------------------------------------
*/

/** Missing Lucide glyphs (24dp viewport, stroke 2, round caps) — exact Lucide geometry. */
object MaridIcons2b {

    /** Lucide "arrow-down" — back-to-bottom button (ConversationTimeline). */
    val ArrowDown: ImageVector by lazy {
        ImageVector.Builder(
            name = "Marid2b.ArrowDown",
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

    /** Lucide "wrench" — generic tool icon (fallback.tsx FALLBACK_TOOL_ICON). */
    val Wrench: ImageVector by lazy {
        ImageVector.Builder(
            name = "Marid2b.Wrench",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
            strokePath {
                // M14.7 6.3a1 1 0 0 0 0 1.4l1.6 1.6a1 1 0 0 0 1.4 0l3.77-3.77
                // a6 6 0 0 1-7.94 7.94l-6.91 6.91a2.12 2.12 0 0 1-3-3l6.91-6.91
                // a6 6 0 0 1 7.94-7.94l-3.76 3.76z
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

// ---------------------------------------------------------------------------
// View model types (in-file contract for MaridChatArea)
// ---------------------------------------------------------------------------

/**
 * Compact tool call state — port of mapToolStatus (lib/mapToolStatus.ts) +
 * CompactToolCallState (shared tool-call-summary.ts) + en-US status strings.
 */
enum class ToolCallStatus(val label: String) {
    PENDING("Pending"), // chat.toolCall.status.pending   / "input-streaming"
    RUNNING("Running"), // chat.toolCall.status.running    / "input-available"
    COMPLETED("Completed"), // chat.toolCall.status.completed / "output-available"
    FAILED("Failed"), // chat.toolCall.status.failed     / "output-error"
    DENIED("Denied"), // chat.toolCall.status.denied     / "output-denied"
}

/** One transcript row group. */
sealed class ChatTurn {
    /** UserInputRowView -> ConversationUserInputBubble. */
    data class UserTurn(val text: String) : ChatTurn()

    /** Assistant segments: text, tool calls, reasoning and change summary. */
    data class AssistantTurn(val blocks: List<AssistantBlock>) : ChatTurn()

    /** Standalone tool call turn (ToolCallRowView -> ToolCallBlock). */
    data class ToolCallTurn(
        val name: String,
        val status: ToolCallStatus,
        val summary: String,
        val detail: String? = null,
        /** ToolProgress.fraction (0f..1f) when determinate, null otherwise. */
        val progressFraction: Float? = null,
        /** ToolProgress.message. */
        val progressMessage: String? = null,
    ) : ChatTurn()
}

/** Blocks of a [ChatTurn.AssistantTurn], rendered in source order. */
sealed class AssistantBlock {
    /** AssistantTextRowView plain text (markdown simplified per task scope). */
    data class MessageText(val text: String) : AssistantBlock()

    data class ToolCall(
        val name: String,
        val status: ToolCallStatus,
        val summary: String,
        val detail: String? = null,
        val progressFraction: Float? = null,
        val progressMessage: String? = null,
    ) : AssistantBlock()

    /** ReasoningRowView -> ai-elements Reasoning collapse. */
    data class Reasoning(
        val text: String,
        val streaming: Boolean = false,
        val durationSeconds: Int? = null,
    ) : AssistantBlock()

    /** MessageChangeSummaryPanel. */
    data class ChangeSummary(val files: List<ChangedFile>) : AssistantBlock()
}

/** One changed file row of [AssistantBlock.ChangeSummary]. */
data class ChangedFile(val path: String, val added: Int, val removed: Int)

// ---------------------------------------------------------------------------
// Typography (styles.css @theme: ui-base 14sp, ui-sm 12sp; html leading 1.5)
// ---------------------------------------------------------------------------

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

/** CSS ease-in-out = cubic-bezier(0.42, 0, 0.58, 1) (progress sweep). */
private val CssEaseInOut = CubicBezierEasing(0.42f, 0f, 0.58f, 1f)

// ---------------------------------------------------------------------------
// Public entry point
// ---------------------------------------------------------------------------

/**
 * Chat transcript surface. LazyColumn with `reverseLayout` so the latest
 * message is bottom-anchored (ConversationTimeline pins to scroll bottom and
 * auto-follows while pinned) + the exact ConversationBackToBottomButton
 * affordance.
 */
@Composable
fun MaridChatArea(
    turns: List<ChatTurn>,
    modifier: Modifier = Modifier,
    onRetryLast: (() -> Unit)? = null,
) {
    // The React product UI renders no retry entry (ConversationRowView.tsx
    // comment on the onRetry prop) — callback retained for host API symmetry.
    onRetryLast?.let { /* intentionally unused; see header deviation note 2 */ }

    val listState = rememberLazyListState()
    var pinnedToBottom by remember { mutableStateOf(true) }

    LaunchedEffect(listState) {
        snapshotFlow {
            listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
        }.collect { pinnedToBottom = it }
    }

    // While pinned, new content keeps the view anchored to the latest message
    // (ConversationTimeline wasPinnedToBottom behavior).
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
            // gap-5 between turns (ConversationTurnGroup.tsx:1487); px-4 / pb-5.
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

        // ConversationBackToBottomButton — absolute bottom-3 left-1/2 centered.
        if (showBackToBottom) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp) // bottom-3
                    .size(28.dp) // size-7 (Button size="icon")
                    .clip(CircleShape) // rounded-full
                    .background(MaridTokens.Card) // bg-card
                    .border(1.dp, MaridTokens.Border, CircleShape) // variant outline border-border
                    .clickable { scope.launch { listState.animateScrollToItem(0) } },
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = MaridIcons2b.ArrowDown,
                    contentDescription = "Scroll to bottom", // chat.scrollToBottom
                    modifier = Modifier.size(16.dp), // ArrowDownIcon size-4
                    tint = MaridTokens.Foreground, // variant outline text-foreground
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// User turn
// ---------------------------------------------------------------------------

/** UserInputRowView: right-aligned column (flex flex-col items-end). */
@Composable
private fun UserTurnItem(turn: ChatTurn.UserTurn) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.End,
    ) {
        UserBubble(text = turn.text)
    }
}

/**
 * ConversationUserInputBubble + ConversationUserInputBody collapse affordance.
 * Bubble: bg user-message-bubble (white 8%), NO border (styles.css border-width
 * :0 override), 12dp radius, px-4 py-3, text-ui-base text-foreground.
 */
@Composable
private fun UserBubble(text: String) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    var overflowing by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .clip(Radius12) // rounded-xl (+ styles.css 12px top-right override)
            .background(MaridTokens.UserMessageBubble) // --color-user-message-bubble
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
                        style = UiBase.copy(color = MaridTokens.Foreground), // text-ui-base text-foreground
                        softWrap = true, // whitespace-pre-wrap break-words
                        onTextLayout = { result -> overflowing = result.didOverflowHeight },
                        modifier = Modifier.clipToBounds(),
                    )
                    // [mask-image:linear-gradient(to_bottom,black 0%,black 70%,transparent 100%)]
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
                    // absolute inset-x-0 bottom-0 mx-auto
                    ExpandChevronButton(
                        expanded = false,
                        onClick = { expanded = true },
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }
            if (expanded && overflowing) {
                // expanded ? "w-full pt-2" centered
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

/** Button size="sm" variant="outline" rounded-full bg-background (h-6 px-2). */
@Composable
private fun ExpandChevronButton(expanded: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .height(24.dp) // size sm h-6
            .clip(CircleShape) // rounded-full
            .background(MaridTokens.Background) // bg-background
            .border(1.dp, MaridTokens.Border, CircleShape) // variant outline border-border
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp), // px-2
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = MaridIcons.ChevronDown,
            contentDescription = null,
            modifier = Modifier
                .size(16.dp) // size-4
                .rotate(if (expanded) 180f else 0f), // ChevronUp == ChevronDown flipped
            tint = MaridTokens.Foreground, // variant outline text-foreground
        )
    }
}

// ---------------------------------------------------------------------------
// Assistant turn
// ---------------------------------------------------------------------------

private sealed interface TurnUnit {
    data class Single(val block: AssistantBlock) : TurnUnit
    data class WorkRun(val blocks: List<AssistantBlock>) : TurnUnit
}

/**
 * Assistant turn layout: flex flex-col gap-5 (20dp) between segments; all
 * work rows (tool calls + reasoning) share the gap-1 (4dp)
 * data-conversation-work-items container; text and change summary panels
 * break the container (20dp gaps).
 */
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

/**
 * AssistantTextRowView text: w-full text-ui-base with markdown container
 * leading-[1.75] tracking-wide; fenced code blocks framed as
 * rounded-xl bg-muted/15 > rounded-xl bg-background with 12sp mono text.
 */
@Composable
private fun MessageTextBlock(text: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        splitFencedCode(text).forEach { piece ->
            when (piece) {
                is TextPiece.Prose -> Text(
                    text = piece.text,
                    style = UiBaseMarkdown.copy(color = MaridTokens.Foreground),
                    softWrap = true,
                )
                is TextPiece.Code -> CodeFrame(piece)
            }
        }
    }
}

/** max-h-60 rounded-xl bg-muted/15 > rounded-xl bg-background, mono 12sp. */
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
                .background(MaridTokens.Background) // bg-background
                .padding(8.dp),
        ) {
            Text(
                text = piece.code.trimEnd('\n'),
                style = UiCode.copy(color = MaridTokens.Foreground), // text-foreground
                softWrap = true,
            )
        }
    }
}

private sealed interface TextPiece {
    data class Prose(val text: String) : TextPiece
    data class Code(val language: String?, val code: String) : TextPiece
}

/**
 * Splits fenced markdown code blocks (```lang\n...\n```). An unterminated
 * fence (streaming) keeps the tail as code, mirroring the source's
 * streaming-tolerant markdown mode.
 */
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

// ---------------------------------------------------------------------------
// Tool call block (ToolLayout + ToolSummaryRow + fallback body)
// ---------------------------------------------------------------------------

/**
 * ToolCallBlock: frameless summary row (ToolLayout Collapsible w-full
 * flex flex-col) with collapsible body framed by the generic detail container
 * (fallback.tsx rounded-xl border-border bg-surface/40 p-3).
 */
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
        // ToolSummaryRow: inline-flex items-center gap-2 text-ui-base
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(Radius6)
                .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp), // gap-2
        ) {
            if (isRunning) {
                // LoaderCircle size-3 animate-spin (ToolSnapshotFieldNotice.tsx:65)
                SpinningLoader()
            }
            // SummaryLeadingContent icon slot: shrink-0 text-foreground-subtle
            Icon(
                imageVector = MaridIcons2b.Wrench,
                contentDescription = null,
                modifier = Modifier.size(16.dp), // size-4
                tint = MaridTokens.ForegroundSubtle,
            )
            // kindLabelClassName: font-normal whitespace-nowrap shrink-0 +
            // running ? animated-gradient-text-foreground : text-foreground-subtle
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
                    style = UiBase.copy(color = MaridTokens.ForegroundSubtle),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            // SummaryContent: min-w-0 flex items-center gap-2 font-normal
            // text-foreground-subtle (primaryText)
            Text(
                text = summary,
                style = UiBase.copy(color = MaridTokens.ForegroundSubtle),
                softWrap = true,
                modifier = Modifier.weight(1f, fill = false),
            )
            // ToolCallProgressIndicator (running with progress payload)
            if (isRunning && (progressFraction != null || progressMessage != null)) {
                ProgressIndicator(
                    fraction = progressFraction,
                    message = progressMessage,
                    modifier = Modifier.weight(1f, fill = false),
                )
            }
            // statusNode: whitespace-nowrap (color = SummaryContent
            // text-foreground-subtle — exact source classes, see header note)
            Text(
                text = status.label,
                style = UiBase.copy(color = MaridTokens.ForegroundSubtle),
                maxLines = 1,
            )
            // ChevronRightIcon size-4 text-foreground-subtlest, rotate-90 open;
            // collapsed opacity-0 group-hover:opacity-100 -> hidden without hover
            if (expanded) {
                Icon(
                    imageVector = MaridIcons.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier
                        .size(16.dp)
                        .rotate(animateFloatAsState(90f, tween(200), label = "tool-chevron").value),
                    tint = MaridTokens.ForegroundSubtlest,
                )
            }
        }

        // CollapsibleContent + TOOL_CONTENT_SPACING_CLASSNAME pt-2
        if (expanded) {
            Column(modifier = Modifier.padding(top = 8.dp)) { // pt-2
                // fallback.tsx:79 rounded-xl border border-border bg-surface/40 p-3
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(Radius12)
                        .border(1.dp, MaridTokens.Border, Radius12)
                        .background(ChatAreaPalette.Surface40)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp), // space-y-3
                ) {
                    // ToolDetailSection: space-y-2, header text-ui-sm
                    // text-foreground-subtle min-h-6, content max-h-60
                    // text-ui-base text-foreground-subtle
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { // space-y-2
                        Row(
                            modifier = Modifier.height(24.dp), // min-h-6
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = if (failed) "Error" else "Result", // fallback.error / result
                                style = UiSm.copy(color = MaridTokens.ForegroundSubtle),
                            )
                        }
                        val body = detail
                        if (body == null) {
                            // chat.toolCall.fallback.waiting / noResult (p
                            // className="text-foreground-subtlest")
                            Text(
                                text = if (isRunning) "Waiting for result" else "No result available",
                                style = UiBase.copy(color = MaridTokens.ForegroundSubtlest),
                                softWrap = true,
                            )
                        } else if (failed) {
                            // ai-elements/tool.tsx ToolOutput error branch:
                            // rounded-md bg-destructive/10 text-destructive p-3
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
                                    style = UiBase.copy(color = MaridTokens.Destructive),
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
                                    style = UiBase.copy(color = MaridTokens.ForegroundSubtle),
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

/** LoaderCircle size-3 (12dp) animate-spin — 1s linear infinite. */
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
        imageVector = MaridIcons.LoaderCircle,
        contentDescription = null,
        modifier = Modifier.size(12.dp).rotate(angle),
        tint = MaridTokens.ForegroundSubtle,
    )
}

/**
 * ToolCallProgressIndicator: "." + h-1 w-16 rounded-full bg-surface-hover
 * track with bg-brand fill (determinate width % / indeterminate 1/3 sweep) +
 * optional truncated message.
 */
@Composable
private fun ProgressIndicator(fraction: Float?, message: String?, modifier: Modifier = Modifier) {
    // transition-[width] duration-200 (determinate)
    val animatedFraction by animateFloatAsState(
        targetValue = fraction ?: 0f,
        animationSpec = tween(200),
        label = "progress-width",
    )
    // left: -33% -> 100%, w-1/3, 1.2s ease-in-out infinite (indeterminate)
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
            style = UiBase.copy(color = MaridTokens.ForegroundSubtlest), // text-foreground-subtlest
        )
        val trackHeight = 4.dp // h-1
        Box(
            modifier = Modifier
                .width(64.dp) // w-16
                .height(trackHeight)
                .clip(CircleShape) // rounded-full
                .background(MaridTokens.SurfaceHover) // bg-surface-hover
                .drawBehind {
                    val w = size.width
                    val h = size.height
                    val corner = androidx.compose.ui.geometry.CornerRadius(h / 2f, h / 2f)
                    if (fraction != null) {
                        drawRoundRect(
                            color = MaridTokens.Brand, // bg-brand
                            topLeft = Offset.Zero,
                            size = Size(w * animatedFraction, h),
                            cornerRadius = corner,
                        )
                    } else {
                        val x = (-1f / 3f + sweep * (4f / 3f)) * w
                        drawRoundRect(
                            color = MaridTokens.Brand,
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
                style = UiSm.copy(color = MaridTokens.ForegroundSubtle),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis, // truncate
                modifier = Modifier.weight(1f, fill = false),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Reasoning block (ai-elements Reasoning)
// ---------------------------------------------------------------------------

@Composable
private fun ReasoningCard(block: AssistantBlock.Reasoning) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) { // not-prose min-w-0 flex flex-col
        // ReasoningTrigger: inline-flex items-center gap-2 text-ui-base
        Row(
            modifier = Modifier
                .clip(Radius6)
                .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp), // gap-2
        ) {
            Icon(
                imageVector = MaridIcons.Brain,
                contentDescription = null,
                modifier = Modifier.size(16.dp), // size-4
                tint = MaridTokens.ForegroundSubtle, // group-hover:text-foreground omitted
            )
            if (block.streaming && !expanded) {
                // animated-gradient-text animated-gradient-text-foreground
                Text(
                    text = "Thinking", // chat.reasoning.thinking
                    style = TextStyle(brush = rememberGradientFlowBrush(), fontSize = 14.sp),
                    maxLines = 1,
                )
            } else {
                Text(
                    text = "Thought", // chat.reasoning.thought
                    style = UiBase.copy(color = MaridTokens.ForegroundSubtle),
                    maxLines = 1,
                )
                Text(
                    text = block.durationSeconds?.let { "$it seconds" } ?: "a few seconds",
                    // chat.reasoning.durationSeconds / chat.reasoning.durationFewSeconds
                    style = UiBase.copy(color = MaridTokens.ForegroundSubtlest),
                    maxLines = 1,
                )
            }
            if (expanded) {
                Icon(
                    imageVector = MaridIcons.ChevronRight,
                    contentDescription = null,
                    modifier = Modifier
                        .size(16.dp)
                        .rotate(
                            animateFloatAsState(90f, tween(200), label = "reasoning-chevron").value,
                        ),
                    tint = MaridTokens.ForegroundSubtlest,
                )
            }
        }

        // ReasoningContent: pt-3 > max-h-60 overflow-auto text-ui-base
        // text-foreground-subtlest + ml-2 border-l border-border pl-3.5
        if (expanded) {
            Column(modifier = Modifier.padding(top = 12.dp)) { // pt-3
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 240.dp) // max-h-60 overflow-auto
                        .verticalScroll(rememberScrollState())
                        .padding(start = 8.dp) // ml-2
                        .drawBehind {
                            // border-l border-border (1px)
                            drawRect(
                                color = MaridTokens.Border,
                                topLeft = Offset.Zero,
                                size = Size(1.dp.toPx(), size.height),
                            )
                        }
                        .padding(start = 15.dp), // pl-3.5 (14dp) measured after the 1dp border
                ) {
                    Text(
                        text = block.text,
                        style = UiBase.copy(color = MaridTokens.ForegroundSubtlest),
                        softWrap = true,
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Change summary panel (MessageChangeSummaryPanel)
// ---------------------------------------------------------------------------

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
            .border(1.dp, MaridTokens.Border, Radius12) // border border-border
            .background(MaridTokens.Card), // bg-card (dark neutral-800)
    ) {
        // Header: h-10 px-2 gap-3; trigger px-1 gap-2
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
                    imageVector = MaridIcons.ChevronRight,
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
                    tint = MaridTokens.ForegroundSubtlest, // text-foreground-subtlest
                )
                Text(
                    text = header, // chat.changeSummary.filesChanged.one/other
                    style = UiBase.copy(
                        color = MaridTokens.Foreground,
                        fontWeight = FontWeight.Medium, // font-medium
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis, // truncate
                    modifier = Modifier.weight(1f, fill = false),
                )
                DiffCounts(added = totalAdded, removed = totalRemoved) // tabular-nums
            }
        }

        // CollapsibleContent file rows: bg-background/50, px-2 py-2, gap-1
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
                                imageVector = MaridIcons.File,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp), // iconSize 16
                                tint = MaridTokens.ForegroundSubtle,
                            )
                            val name = file.path.substringAfterLast('/')
                            val dir = file.path.removeSuffix(name).removeSuffix("/")
                            Text(
                                text = name, // fileNameClassName font-medium text-foreground
                                style = UiBase.copy(
                                    color = MaridTokens.Foreground,
                                    fontWeight = FontWeight.Medium,
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis, // truncate
                            )
                            if (dir.isNotEmpty()) {
                                Text(
                                    text = dir, // filePathClassName text-foreground-subtlest
                                    style = UiBase.copy(color = MaridTokens.ForegroundSubtlest),
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

/** +N text-diff-added / -N text-diff-removed, tabular-nums, text-ui-base. */
@Composable
private fun DiffCounts(added: Int, removed: Int) {
    if (added <= 0 && removed <= 0) return
    Text(
        text = buildAnnotatedString {
            if (added > 0) {
                withStyle(SpanStyle(color = MaridTokens.DiffAdded)) { append("+$added") }
            }
            if (added > 0 && removed > 0) append(" ")
            if (removed > 0) {
                withStyle(SpanStyle(color = MaridTokens.DiffRemoved)) { append("-$removed") }
            }
        },
        style = TabularNums.copy(fontSize = 14.sp, lineHeight = 21.sp), // text-ui-base tabular-nums
    )
}

// ---------------------------------------------------------------------------
// animated-gradient-text (styles.css:957-1004, dark foreground variant)
// ---------------------------------------------------------------------------

// ---------------------------------------------------------------------------
// ChatArea palette + animated gradient (animated-gradient-text, styles.css
// 957-1004 dark variant) — restored after the truncated-tail repair.
// ---------------------------------------------------------------------------

/** Local chat-surface colors used by the transcript frame pieces. */
object ChatAreaPalette {
    /** bg-muted/15 (dark muted = white ~15% over surface). */
    val MutedBg15 = Color(0x26FFFFFF)

    /** surface at 40% (decorative frame wash). */
    val Surface40 = Color(0x66FFFFFF)

    /** destructive 10% surface tint. */
    val DestructiveBg10 = Color(0x1AFB2C36)

    /** background at 50% (header fade). */
    val Background50 = Color(0x80171717)

    /** Bubble fade base composited over Background (#171717). */
    val BubbleFadeBase = Color(0xFF2A2A2A)
}

/**
 * animated-gradient-text-foreground: strong 34% / soft 50% / strong 66% band
 * over a 300% tile, background-position 100% -> 0% over 2s (linear), then
 * held (gradient-flow 4s). Compose adaptation: the soft band's center is
 * animated 1f -> 0f every 2s (LinearEasing) via Brush position stops.
 */
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
    val soft = MaridTokens.ForegroundSubtle
    val strong = MaridTokens.Foreground
    return Brush.linearGradient(
        0f to strong,
        (position - 0.16f).coerceIn(0f, 1f) to strong,
        position.coerceIn(0f, 1f) to soft,
        (position + 0.16f).coerceIn(0f, 1f) to strong,
        1f to strong,
    )
}
