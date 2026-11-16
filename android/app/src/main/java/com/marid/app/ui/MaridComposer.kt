package com.marid.app.ui
// marid-sync-1

import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.marid.app.icons.MaridIcons
import com.marid.app.rpc.MaridRpcClient
import com.marid.app.rpc.asArrayOrNull
import com.marid.app.rpc.asObjectOrNull
import com.marid.app.rpc.asStringOrNull
import kotlin.math.roundToInt

/*
 * ============================================================================
 * MaridComposer — 1:1 port of the ZCode desktop conversation composer.
 * ============================================================================
 *
 * React files ported (source of truth packages/ui/src):
 *   - prompt-editor/ChatPromptEditor.tsx          composer shell + toolbar row + send button
 *   - LexicalChatInput.tsx (contenteditable part) editor field visuals (min-h-10/max-h-40,
 *                                                 text-ui-base leading-5, placeholder)
 *   - prompt-editor/ChatPromptActionMenu.tsx      "+" action affordance (trigger only)
 *   - v4/composer/V4ComposerModeControls.tsx      mode switch trigger + mode dropdown
 *   - chat-input-toolbar/ChatModeSwitchControl.tsx (trigger class reference)
 *   - chat-input-toolbar/RollingToolbarLabel.tsx  rolling label (0.75em slide, 200ms)
 *   - chat-input-toolbar/ThoughtLevelCycleControl.tsx  thought level trigger + select menu
 *   - chat-input-toolbar/composer-thought-slider.css   effort slider colors/keyframes
 *   - chat-input-toolbar/ComposerEffortHeading.tsx     effort heading
 *   - chat-input-toolbar/ComposerModelMenu.tsx    model menu body (provider/models/effort)
 *   - chat-input-toolbar/ComposerProviderMenu.tsx provider sub-menu
 *   - ModelConfigSelect.tsx                       model select trigger + dropdown chrome
 *   - components/ui/dropdown-menu.tsx, select.tsx, button.tsx (shadcn primitives the
 *     above compose with — exact paddings/radii/colors extracted from them)
 *
 * EXTRACTION TABLE (dark theme, packages/ui/src/styles.css `.dark` block) —
 * 1 Tailwind spacing unit = 4dp. rounded-md=6dp, lg=8dp, xl=12dp, 2xl=16dp, full=50%, sm=4dp.
 *
 *  TOKEN                          | TAILWIND / CSS SOURCE                       | VALUE
 *  -------------------------------|---------------------------------------------|------------------
 *  shell bg                       | bg-input                                    | #262626
 *  shell bg (focus-within)        | focus-within:bg-input-focused               | #0A0A0A
 *  shell border                   | border-[0.5px] border-input-border          | white 10% @0.5dp
 *  shell border (hover)           | hover:border-input-border-hover             | white 30%
 *  shell border (focus-within)    | focus-within:!border-input-border-focused   | #00A6F4 (brand)
 *  shell radius                   | rounded-2xl                                 | 16dp
 *  shell padding                  | p-3                                         | 12dp
 *  shell gap                      | gap-3                                       | 12dp
 *  shell shadow                   | shadow-(--shadow-composer-soft)             | `:root` = 0 0 #0000
 *                                 | (.dark does not override)                   | → none in dark
 *  editor min/max height          | min-h-10 max-h-40 overflow-y-auto           | 40dp / 160dp
 *  editor text                    | text-ui-base leading-5 text-foreground      | 14sp / 20sp #E5E5E5
 *  editor placeholder             | text-foreground-subtlest                    | #E5E5E5 @30%
 *  caret                          | contenteditable caret = currentColor        | #E5E5E5
 *  ghost hover                    | hover:bg-hover (= --color-surface-hover)    | white 10%
 *  toolbar row                    | flex items-end gap-3                        | gap 12dp, bottom-align
 *  leading group                  | flex shrink-0 items-center gap-1            | gap 4dp
 *  trailing group                 | ml-auto items-center gap-1.5                | gap 6dp
 *  model cluster                  | flex items-center gap-1                     | gap 4dp
 *  "+ action" trigger             | Button ghost size=icon-md + rounded-lg      | 28dp, r8, Plus 16dp
 *  mode trigger                   | h-7 gap-1 rounded-lg px-2 text-ui-base      | 28dp, r8, px8, gap4, 14sp
 *  mode trigger chevron           | size-3.5 (ChevronDown)                      | 14dp
 *  mode colors                    | modeTriggerColor: guarded=text-icon-blue    | #0084D1
 *                                 | yolo=text-warning                           | #F0B100
 *  mode menu                      | w-64, DropdownMenuContent chrome            | 256dp
 *  menu chrome (all popovers)     | rounded-lg border border-popover-border     | r8, border white 10%,
 *                                 | bg-menu p-1 gap-0.5 shadow-md               | #0A0A0A, p4, gap2
 *  menu item hover                | data-[highlighted]:bg-menu-hover            | #171717
 *  provider row hover/open        | data-[…]:bg-menu-hover/50                   | #171717 @50%
 *  mode item                      | min-h-13 items-start gap-3 py-2 pl-2 pr-8   | min52, gap12, py8 pl8 pr32
 *  mode item icon                 | size-4.5 mt-0.5                             | 18dp, top offset 2dp
 *  mode item desc                 | text-ui-sm text-foreground-subtle           | 12sp, #E5E5E5 @60%
 *  plan checkbox item             | DropdownMenuCheckboxItem min-h-13 …         | Lightbulb 18dp
 *  plan marker separator          | h-3 w-px bg-border                          | 12dp x 1dp white10%
 *  menu radio check               | CheckIcon size-4 text-foreground-subtle     | 16dp, right 8dp
 *  menu separator                 | h-px bg-border (composer: bg-border/50)     | 1dp white 5%
 *  menu label "Models"            | DropdownMenuLabel text-ui-sm text-…-subtle  | px8 py6, 12sp @60%
 *  model menu width               | w-72                                        | 288dp
 *  model list viewport            | ScrollFadeViewport max-h-60                 | max 240dp scroll
 *  model/provider item            | min-h-8 gap-2 pl-2 pr-8 text-ui-base        | min32, gap8, pl8 pr32
 *  provider logo                  | ProviderLogo size-4 rounded-sm (Package     | 16dp r4 fallback icon
 *                                 | fallback when no builtin asset)             |
 *  access badge                   | rounded-full bg-surface px-1 py-px          | r-full white5%,
 *                                 | text-ui-xs font-medium                      | 10sp medium, maxW 192
 *  model trigger                  | w-fit justify-between gap-1 rounded-lg      | 28dp r8, pl8 pr6, gap4
 *                                 | pl-2 pr-1.5 text-ui-base                    |
 *  model trigger chevron          | ChevronDownIcon size-3.5 text-…-subtle      | 14dp #E5E5E5@60%
 *  open-state trigger label       | data-composer-open-label inset-x-1.5        | centered 14sp @60%,
 *                                 | text-center text-foreground-subtle          | width max(prev,128dp)
 *  thought trigger                | gap-1 rounded-lg px-1.5 py-1.5 text-ui-base | 28dp r8 px6, Brain 16dp
 *  thought meter bar              | w-1 bg-current/10 rounded-full, fill        | 4dp wide, #E5E5E5@10%,
 *                                 | bg-success (bottom, height %, min-h-1)      | green #00C950
 *  effort slider container        | composer-thought-slider h-8                 | 32dp
 *  effort track                   | inset-x-0 top-1 h-6 rounded-full            | top4, h24, #E5E5E5@10%,
 *                                 | bg-foreground/10 + border border-…/10       | border #E5E5E5@10%
 *  effort fill                    | bg-success width 14px+(100%-28px)*p         | #00C950; at max:
 *                                 | ::before 270deg blue-500→success            | #316FF6→#00C950
 *  effort stops                   | inset-x-3.5 dots size-1 rounded-full        | 4dp dots at 14dp inset,
 *                                 | bg-foreground-subtlest / filled white .3    | #E5E5E5@30% / white@30%
 *  effort thumb                   | size-7 rounded-full shadow-sm               | 28dp white @top 2dp
 *  effort heading                 | mb-1 gap-2 text-ui-base; label text-ui-sm   | gap8, 12sp @60% label
 *  send button                    | size icon-md rounded-full bg-brand          | 28dp r-full #00A6F4,
 *                                 | text-foreground-inverse hover:bg-brand/80   | #FFFFFF icon 16dp,
 *                                 | disabled:opacity-50                         | hover #00A6F4@80%
 *  send icon                      | ArrowUpIcon size-4                          | 16dp
 *  rolling label                  | h-[1.3em] leading-[1.25] 0.2s ease          | 1.3em box, 200ms roll
 *  placeholder (en-US)            | chat.placeholder.newTask                    | "Ask Marid anything,
 *                                 |                                             | @ to add context, / for
 *                                 |                                             | commands or capabilities"
 *
 * RPC WIRING (verified against source):
 *   Desktop: modelSelectionService.getView() (packages/services/src/model-provider/
 *   providerFacadeServices.ts:221) → channel "model-selection", command "getView"
 *   (packages/shared/src/channels.ts:152; wire asserted in
 *   packages/desktop/test/e2e/.../conversation-session-v4-composer-toolbar.test.ts:1467).
 *   Response = ModelSelectionView (packages/provider/src/facades.ts:201):
 *     { revision, providers: [{ providerId, providerName?, templateId, config,
 *       models: [{ modelId, config }] }], preferredSelection?, effectiveSelection? }
 *   Group label derivation (packages/ui/src/lib/modelSelectionGroups.ts:59):
 *     label = providerName?.trim() || providerId; model display name = modelId.
 *   Android side: MaridRpcClient.modelSelectionView() already issues the identical
 *   call(ServiceChannels.MODEL_SELECTION, "getView"); fetchModelEntries() below parses
 *   the same response shape defensively (empty list + Log.w on any mismatch).
 *
 * DEVIATIONS (kept minimal; every one is forced by the fixed MaridComposer API or
 * platform, none is an invented visual value):
 *   1. Lexical rich editor → single styled BasicTextField with exact shell visuals
 *      (per task; no markdown/mention rendering).
 *   2. ChatPromptActionMenu: only the "+" trigger is ported; the MentionPanel needs
 *      the desktop mention/file/plugin providers which do not exist on Android.
 *   3. ChatContextUsage ring + VoiceInputButton + stop button are not rendered:
 *      MaridComposer's fixed signature carries no usage snapshot / voice / pending
 *      state. Send button always renders ArrowUp (no submitting spinner).
 *   4. Container-query collapse (@sm/@xl composer widths) not ported: the port
 *      always renders the desktop wide-layout state (labels visible, thought meter
 *      bar hidden, model Package icon hidden) — values for the collapsed states are
 *      in the extraction table where relevant.
 *   5. Thought-level catalog comes from model reasoning specs over RPC; the port
 *      uses internal defaults ["low","medium","high"] (current "medium") until that
 *      catalog is wired — labels/colors/states are the exact desktop ones.
 *   6. Mode = internal state ("build"); no onSwitchMode callback exists in the
 *      fixed signature. YOLO confirmation dialog not ported.
 *   7. shadow-md on menus approximated with a Compose elevation shadow
 *      (black 10%); shadow-composer-soft is a no-op in dark (0 0 #0000) → none.
 *   8. Menu open state keeps the shell in its focus-within visuals (desktop keeps
 *      :focus-within while the menu is open); text-field focus itself is lost to
 *      the focusable popup window on Android.
 *   9. Selection handle/highlight colors are Compose-required; brand-based values
 *      are used (not specified in source). Text selection itself unspecified.
 *  10. Radix scroll fades on the model list (ScrollFadeViewport) and stop-dot
 *      hover scaling are pointer-hover micro-interactions; not ported.
 */

// ----------------------------------------------------------------------------
// Extra palette values (exact hex; MaridTokens covers all semantic tokens).
// ----------------------------------------------------------------------------

private object Composer2cPalette {
    /** --color-menu (dark) = neutral-950. */
    val MenuBg = Color(0xFF0A0A0A)

    /** data-[highlighted]:bg-menu-hover/50 = neutral-900 @ 50%. */
    val MenuHover50 = Color(0x80171717)

    /** bg-foreground/10 = neutral-200 (#E5E5E5) @ 10% (track, meter, borders). */
    val Foreground10 = Color(0x1AE5E5E5)

    /** --color-icon-blue (dark) → terminal-bright-blue → sky-600. */
    val IconBlue = Color(0xFF0084D1)

    /** hover:bg-brand/80 = #00A6F4 @ 80%. */
    val Brand80 = Color(0xCC00A6F4)

    /** Tailwind v4 blue-500 — composer-thought-slider --composer-thought-max-accent. */
    val Blue500 = Color(0xFF316FF6)

    /** shadow-md rgb(0 0 0 / 10%). */
    val ShadowBlack10 = Color(0x1A000000)

    /** shadow-sm rgb(0 0 0 / 5%) (effort thumb). */
    val ShadowBlack5 = Color(0x0D000000)
}

// ----------------------------------------------------------------------------
// RPC — model catalog (channel/command verified; see header).
// ----------------------------------------------------------------------------

/** One selectable model of the composer model menu (ModelSelectGroupItem). */
data class ModelEntry2c(
    val providerId: String,
    val providerLabel: String,
    val modelId: String,
)

private const val TAG_2C = "MaridComposer"

/**
 * Fetches the model catalog exactly like the desktop composer does:
 * channel "model-selection", command "getView" (providerFacadeServices.ts:221),
 * parsing ModelSelectionView { providers: [{ providerId, providerName?, models: [{ modelId }] }] }.
 * Defensive: any shape deviation → empty list + Log.w("MaridComposer").
 */
suspend fun fetchModelEntries(rpc: MaridRpcClient): List<ModelEntry2c> {
    return try {
        val view = rpc.modelSelectionView()?.asObjectOrNull()
        if (view == null) {
            Log.w(TAG_2C, "model-selection/getView returned no object; empty catalog")
            return emptyList()
        }
        val providers = view["providers"].asArrayOrNull()
        if (providers == null) {
            Log.w(TAG_2C, "model-selection/getView response missing providers[]; empty catalog")
            return emptyList()
        }
        val entries = mutableListOf<ModelEntry2c>()
        for (providerElement in providers) {
            val provider = providerElement.asObjectOrNull() ?: continue
            val providerId =
                (provider["providerId"] ?: provider["id"] ?: provider["name"]).asStringOrNull()
                    ?: continue
            // modelSelectionGroups.ts:59 — label = providerName?.trim() || providerId.
            val providerLabel = provider["providerName"].asStringOrNull()
                ?.trim()?.takeIf { it.isNotEmpty() } ?: providerId
            val models = provider["models"].asArrayOrNull() ?: continue
            for (modelElement in models) {
                val model = modelElement.asObjectOrNull() ?: continue
                val modelId = (model["modelId"] ?: model["id"] ?: model["name"]).asStringOrNull()
                    ?: continue
                entries.add(ModelEntry2c(providerId, providerLabel, modelId))
            }
        }
        entries
    } catch (t: Throwable) {
        Log.w(TAG_2C, "model-selection/getView failed: ${t.message}", t)
        emptyList()
    }
}

// ----------------------------------------------------------------------------
// Entry point.
// ----------------------------------------------------------------------------

@Composable
fun MaridComposer(
    text: String,
    onTextChange: (String) -> Unit,
    models: List<ModelEntry2c>,
    selectedModel: ModelEntry2c?,
    onModelSelect: (ModelEntry2c) -> Unit,
    onSend: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Internal state (mode, menus) — per task, never hoisted.
    val shellInteraction = remember { MutableInteractionSource() }
    val fieldFocused by shellInteraction.collectIsFocusedAsState()
    val shellHovered by shellInteraction.collectIsHoveredAsState()

    var modelMenuOpen by remember { mutableStateOf(false) }
    var providerMenuOpen by remember { mutableStateOf(false) }
    var modeMenuOpen by remember { mutableStateOf(false) }
    var thoughtMenuOpen by remember { mutableStateOf(false) }

    var mode by remember { mutableStateOf("build") }
    var planEnabled by remember { mutableStateOf(false) }
    val thoughtLevels = remember { listOf("low", "medium", "high") }
    var thoughtValue by remember { mutableStateOf("medium") }

    val rootOrigin = remember { mutableStateOf(IntOffset.Zero) }
    val modeAnchor = remember { mutableStateOf(Anchor2c()) }
    val thoughtAnchor = remember { mutableStateOf(Anchor2c()) }
    val modelAnchor = remember { mutableStateOf(Anchor2c()) }

    // focus-within stays true while a menu is open (desktop behavior, deviation 8).
    val focusVisual = fieldFocused || modelMenuOpen || providerMenuOpen ||
        modeMenuOpen || thoughtMenuOpen

    // transition-colors (150ms) on border + shell background.
    val borderColor by animateColorAsState(
        when {
            focusVisual -> MaridTokens.InputBorderFocused // focus-within:!border-input-border-focused
            shellHovered -> MaridTokens.BorderHover       // hover:border-input-border-hover
            else -> MaridTokens.Border                    // border-input-border
        },
        tween(150),
        label = "composer2cBorder",
    )
    val shellColor by animateColorAsState(
        if (focusVisual) MaridTokens.InputFocused else MaridTokens.Input,
        tween(150),
        label = "composer2cShell",
    )

    Box(
        modifier = modifier.onGloballyPositioned { coords ->
            val pos = coords.positionInRoot()
            rootOrigin.value = IntOffset(pos.x.roundToInt(), pos.y.roundToInt())
        },
    ) {
        // ChatPromptEditor shell: flex flex-col gap-3 rounded-2xl border-[0.5px]
        // border-input-border bg-input p-3 overflow-hidden (overflow visible here —
        // popovers are separate windows and must escape the shell).
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .hoverable(shellInteraction)
                .clip(RoundedCornerShape(16.dp)) // rounded-2xl
                .border(0.5.dp, borderColor, RoundedCornerShape(16.dp)) // border-[0.5px]
                .background(shellColor) // bg-input / focus-within:bg-input-focused
                .padding(12.dp), // p-3
            verticalArrangement = Arrangement.spacedBy(12.dp), // gap-3
        ) {
            ComposerPromptField2c(
                text = text,
                onTextChange = onTextChange,
                onSend = onSend,
                interactionSource = shellInteraction,
            )

            // Toolbar row: "group/toolbar flex items-end gap-3".
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Leading: "flex min-w-0 flex-1 items-center" →
                // inner "flex shrink-0 items-center gap-1".
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp), // gap-1
                    ) {
                        // ChatPromptActionMenu trigger: Button ghost size="icon-md"
                        // className "gap-1 rounded-lg text-ui-base" with PlusIcon size-4.
                        ActionMenuButton2c()
                        ModeSwitchControl2c(
                            mode = mode,
                            open = modeMenuOpen,
                            onOpenChange = { modeMenuOpen = it },
                            onModeSelect = { mode = it },
                            planEnabled = planEnabled,
                            onPlanToggle = { planEnabled = it },
                            anchor = modeAnchor,
                            rootOrigin = rootOrigin,
                        )
                        if (planEnabled) {
                            PlanMarker2c(onRemove = { planEnabled = false })
                        }
                    }
                }

                // Trailing: "ml-auto flex shrink-0 items-center justify-end gap-1.5".
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp), // gap-1.5
                ) {
                    // V4ComposerModelControls cluster: "flex min-w-0 shrink items-center gap-1".
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp), // gap-1
                    ) {
                        ModelSelectControl2c(
                            models = models,
                            selectedModel = selectedModel,
                            onModelSelect = onModelSelect,
                            menuOpen = modelMenuOpen,
                            onMenuOpenChange = { open ->
                                modelMenuOpen = open
                                if (!open) providerMenuOpen = false
                            },
                            providerMenuOpen = providerMenuOpen,
                            onProviderMenuOpenChange = { providerMenuOpen = it },
                            thoughtLevels = thoughtLevels,
                            thoughtValue = thoughtValue,
                            onThoughtValueChange = { thoughtValue = it },
                            anchor = modelAnchor,
                            rootOrigin = rootOrigin,
                        )
                        ThoughtLevelControl2c(
                            levels = thoughtLevels,
                            value = thoughtValue,
                            onValueChange = { thoughtValue = it },
                            menuOpen = thoughtMenuOpen,
                            onMenuOpenChange = { thoughtMenuOpen = it },
                            anchor = thoughtAnchor,
                            rootOrigin = rootOrigin,
                        )
                    }

                    SendButton2c(
                        enabled = text.isNotBlank(),
                        onClick = { onSend(text) },
                    )
                }
            }
        }

    }
}

// ----------------------------------------------------------------------------
// Prompt field — LexicalChatInput contenteditable visuals.
// ----------------------------------------------------------------------------

@Composable
private fun ComposerPromptField2c(
    text: String,
    onTextChange: (String) -> Unit,
    onSend: (String) -> Unit,
    interactionSource: MutableInteractionSource,
) {
    val selectionColors = remember {
        TextSelectionColors(
            handleColor = MaridTokens.InputBorderFocused,
            backgroundColor = MaridTokens.InputBorderFocused.copy(alpha = 0.30f),
        )
    }
    Box(modifier = Modifier.fillMaxWidth()) {
        if (text.isEmpty()) {
            // Placeholder: "pointer-events-none absolute left-0 top-0 …
            // text-foreground-subtlest" (chat.placeholder.newTask).
            Text(
                text = "Ask Marid anything, @ to add context, / for commands or capabilities",
                style = TextStyle(
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = MaridTokens.ForegroundSubtlest,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        CompositionLocalProvider(LocalTextSelectionColors provides selectionColors) {
            BasicTextField(
                value = text,
                onValueChange = onTextChange,
                // min-h-10 max-h-40 overflow-y-auto text-ui-base leading-5 text-foreground
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 40.dp, max = 160.dp),
                textStyle = TextStyle(
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = MaridTokens.Foreground,
                ),
                cursorBrush = SolidColor(MaridTokens.Foreground), // caret = currentColor
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(
                    onSend = { if (text.isNotBlank()) onSend(text) },
                ),
                interactionSource = interactionSource,
            )
        }
    }
}

// ----------------------------------------------------------------------------
// Ghost trigger (Button variant="ghost": text-foreground hover:bg-hover).
// ----------------------------------------------------------------------------

@Composable
private fun GhostTrigger2c(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentColor: Color = MaridTokens.Foreground,
    content: @Composable RowScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Row(
        modifier = modifier
            .hoverable(interaction)
            .background(
                if (hovered) MaridTokens.SurfaceHover else Color.Transparent,
                RoundedCornerShape(8.dp), // rounded-lg
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp), // gap-1
    ) {
        CompositionLocalProvider(LocalContentColor2c provides contentColor) {
            content()
        }
    }
}

/** Minimal replacement for Material LocalContentColor — no Material theme here. */
private val LocalContentColor2c = compositionLocalOf { MaridTokens.Foreground }

@Composable
private fun ActionMenuButton2c() {
    // ChatPromptActionMenu trigger: ghost icon-md (28dp, rounded-lg) + PlusIcon size-4.
    GhostTrigger2c(
        onClick = { /* MentionPanel requires desktop mention providers — deviation 2. */ },
        modifier = Modifier.size(28.dp), // icon-md size-7
        contentColor = LocalContentColor2c.current,
    ) {
        Icon(
            imageVector = MaridIcons.Plus,
            contentDescription = "Add context", // chat.composer.actionMenu
            modifier = Modifier.size(16.dp),
            tint = LocalContentColor2c.current,
        )
    }
}

// ----------------------------------------------------------------------------
// Mode switch — V4ComposerModeControls.tsx.
// ----------------------------------------------------------------------------

private data class ModeOption2c(
    val id: String,
    val label: String,   // mode.label.glm.*
    val description: String, // mode.description.glm.*
    val highPermission: Boolean = false,
)

// MARID_AGENT_PROVIDER = "glm" (marid-agent-policy.ts:5) → glm i18n strings.
private val MODE_PERMISSIONS_2C = listOf(
    ModeOption2c("build", "Ask before changes", "Ask before file changes."),
    ModeOption2c("guarded", "Autonomous mode", "Ask when there’s risk"),
    ModeOption2c("yolo", "Full access", "Run with fewer confirmations.", highPermission = true),
)

private const val MODE_PLAN_LABEL_2C = "Plan mode" // mode.label.glm.plan
private const val MODE_PLAN_DESCRIPTION_2C = "Plan before editing." // mode.description.glm.plan

/** resolveModeOptionIcon (display.tsx:264). */
private fun modeIcon2c(modeId: String) = when {
    modeId == "yolo" -> MaridIcons.ShieldAlert
    modeId == "guarded" -> MaridIcons.ShieldCheck
    else -> MaridIcons.Hand // build/default fallback
}

@Composable
private fun ModeSwitchControl2c(
    mode: String,
    open: Boolean,
    onOpenChange: (Boolean) -> Unit,
    onModeSelect: (String) -> Unit,
    planEnabled: Boolean,
    onPlanToggle: (Boolean) -> Unit,
    anchor: MutableState<Anchor2c>,
    rootOrigin: State<IntOffset>,
) {
    val selected = MODE_PERMISSIONS_2C.firstOrNull { it.id == mode } ?: MODE_PERMISSIONS_2C.first()
    // modeTriggerColor (display-help.ts:26): guarded → icon-blue, yolo → warning.
    val triggerColor = when (selected.id) {
        "guarded" -> Composer2cPalette.IconBlue
        "yolo" -> MaridTokens.Warning
        else -> MaridTokens.Foreground
    }
    GhostTrigger2c(
        onClick = { onOpenChange(true) },
        modifier = Modifier
            .height(28.dp) // h-7
            .captureAnchor2c(anchor, rootOrigin)
            .padding(horizontal = 8.dp), // px-2
        contentColor = triggerColor,
    ) {
        Icon(
            imageVector = modeIcon2c(selected.id),
            contentDescription = null,
            modifier = Modifier.size(16.dp), // Icon size-4
            tint = LocalContentColor2c.current,
        )
        RollingLabel2c(label = selected.label, color = LocalContentColor2c.current)
        Icon(
            imageVector = MaridIcons.ChevronDown,
            contentDescription = null,
            modifier = Modifier.size(14.dp), // size-3.5
            tint = MaridTokens.ForegroundSubtle,
        )
    }

    if (open) {
        MenuPopup2c(
            anchor = anchor.value,
            alignCenter = false,
            sideOffset = 4.dp, // sideOffset={4}
            collisionPadding = 8.dp, // collisionPadding={8}
            onDismissRequest = { onOpenChange(false) },
        ) {
            MenuSurface2c(modifier = Modifier.width(256.dp)) { // w-64
                // Plan: DropdownMenuCheckboxItem min-h-13 items-start gap-3 py-2.
                ModeMenuItem2c(
                    icon = MaridIcons.Lightbulb,
                    title = MODE_PLAN_LABEL_2C,
                    description = MODE_PLAN_DESCRIPTION_2C,
                    checked = planEnabled,
                    onClick = { onPlanToggle(!planEnabled) },
                )
                MenuSeparator2c(fullBleed = true) // default DropdownMenuSeparator bg-border
                for (option in MODE_PERMISSIONS_2C) {
                    ModeMenuItem2c(
                        icon = modeIcon2c(option.id),
                        title = option.label,
                        description = option.description,
                        checked = option.id == selected.id,
                        onClick = { onModeSelect(option.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ModeMenuItem2c(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onClick: () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Box(modifier = Modifier.padding(horizontal = 4.dp)) { // item inset inside p-1 chrome
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp) // min-h-13
                .hoverable(interaction)
                .background(
                    if (hovered) MaridTokens.MenuHover else Color.Transparent,
                    RoundedCornerShape(6.dp), // rounded-md
                )
                .clickable(interactionSource = interaction, indication = null, onClick = onClick)
                .padding(start = 8.dp, end = 32.dp, top = 8.dp, bottom = 8.dp), // py-2 pl-2 pr-8
            verticalAlignment = Alignment.Top, // items-start
            horizontalArrangement = Arrangement.spacedBy(12.dp), // gap-3
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier
                    .size(18.dp) // size-4.5
                    .offset(y = 2.dp), // mt-0.5
                tint = MaridTokens.Foreground,
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) { // gap-0.5
                Text(
                    text = title,
                    style = TextStyle(fontSize = 14.sp, color = MaridTokens.Foreground),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = description,
                    style = TextStyle(fontSize = 12.sp, color = MaridTokens.ForegroundSubtle),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (checked) {
            Icon(
                imageVector = MaridIcons.Check,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp) // right-2
                    .size(16.dp),
                tint = MaridTokens.ForegroundSubtle,
            )
        }
    }
}

@Composable
private fun PlanMarker2c(onRemove: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Spacer(
            modifier = Modifier
                .width(1.dp)
                .height(12.dp) // h-3 w-px
                .background(MaridTokens.Border), // bg-border
        )
        Row(
            modifier = Modifier
                .padding(start = 4.dp) // gap-1
                .height(28.dp) // h-7
                .hoverable(interaction)
                .background(
                    if (hovered) MaridTokens.SurfaceHover else Color.Transparent,
                    RoundedCornerShape(8.dp),
                )
                .clickable(interactionSource = interaction, indication = null, onClick = onRemove)
                .padding(horizontal = 8.dp), // px-2
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            // Lightbulb swaps to X on hover (group-hover/plan).
            Icon(
                imageVector = if (hovered) MaridIcons.X else MaridIcons.Lightbulb,
                contentDescription = "Remove Plan from the next submission", // chat.plan.removeMarker
                modifier = Modifier.size(16.dp),
                tint = MaridTokens.ForegroundSubtle,
            )
            Text(
                text = "Plan", // mode.plan
                style = TextStyle(fontSize = 14.sp, color = MaridTokens.ForegroundSubtle),
                maxLines = 1,
            )
        }
    }
}

// ----------------------------------------------------------------------------
// Thought level — ThoughtLevelCycleControl.tsx (interactionMode = "select").
// ----------------------------------------------------------------------------

/** thoughtLevelLabels.ts — value → en-US label. */
private fun thoughtLevelLabel2c(value: String): String = when (value.trim().lowercase()) {
    "disabled", "false", "no", "none", "nothink", "no-think", "no_think", "off" -> "Off"
    "enable", "enabled", "on", "true" -> "On"
    "minimal" -> "Minimal"
    "low" -> "Low"
    "medium" -> "Medium"
    "high" -> "High"
    "extra-high", "extra_high", "xhigh" -> "Extra high"
    "max" -> "Max"
    "ultra" -> "Ultra"
    else -> value
}

@Composable
private fun ThoughtLevelControl2c(
    levels: List<String>,
    value: String,
    onValueChange: (String) -> Unit,
    menuOpen: Boolean,
    onMenuOpenChange: (Boolean) -> Unit,
    anchor: MutableState<Anchor2c>,
    rootOrigin: State<IntOffset>,
) {
    GhostTrigger2c(
        onClick = { onMenuOpenChange(true) },
        modifier = Modifier
            .height(28.dp) // SelectTrigger default h-7
            .captureAnchor2c(anchor, rootOrigin)
            .padding(horizontal = 6.dp), // px-1.5
        contentColor = MaridTokens.Foreground,
    ) {
        Icon(
            imageVector = MaridIcons.Brain,
            contentDescription = null,
            modifier = Modifier.size(16.dp), // BrainIcon size-4
            tint = LocalContentColor2c.current,
        )
        // Thought meter bar: "w-1 self-stretch bg-current/10 rounded-full" with
        // bg-success fill — hidden at the desktop @xl/composer width (deviation 4);
        // kept implemented for parity.
        ThoughtMeterBar2c(levels = levels, value = value, visible = false)
        RollingLabel2c(
            label = thoughtLevelLabel2c(value),
            color = LocalContentColor2c.current,
        )
        Icon(
            imageVector = MaridIcons.ChevronDown,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaridTokens.ForegroundSubtle, // text-foreground-subtle
        )
    }

    if (menuOpen) {
        MenuPopup2c(
            anchor = anchor.value,
            alignCenter = false,
            sideOffset = 4.dp,
            collisionPadding = 8.dp,
            onDismissRequest = { onMenuOpenChange(false) },
        ) {
            MenuSurface2c(modifier = Modifier.widthIn(min = 128.dp)) { // SelectContent min-w-32
                for (level in levels) {
                    SelectItem2c(
                        label = thoughtLevelLabel2c(level).replaceFirstChar { it.uppercase() },
                        checked = level == value,
                        onClick = { onValueChange(level) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ThoughtMeterBar2c(levels: List<String>, value: String, visible: Boolean) {
    if (!visible) return
    val thinking = levels.filterNot { isNoThoughtLevel2c(it) }
    val activeDots =
        if (isNoThoughtLevel2c(value)) 0 else (thinking.indexOf(value) + 1).coerceAtLeast(0)
    val dotCount = thinking.size.coerceAtLeast(1)
    val progress = (activeDots.toFloat() / dotCount).coerceIn(0f, 1f)
    Box(
        modifier = Modifier
            .width(4.dp) // w-1
            .height(16.dp) // self-stretch of the h-7 content box
            .clip(RoundedCornerShape(999.dp))
            .background(MaridTokens.Foreground.copy(alpha = 0.10f)), // bg-current/10
    ) {
        if (progress > 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height((16.dp * progress).coerceAtLeast(4.dp)) // min-h-1
                    .background(MaridTokens.Success), // bg-success
            )
        }
    }
}

private fun isNoThoughtLevel2c(value: String): Boolean =
    value.trim().lowercase() in setOf(
        "disabled", "false", "no", "none", "nothink", "no-think", "no_think", "off",
    )

@Composable
private fun SelectItem2c(label: String, checked: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Box(modifier = Modifier.padding(horizontal = 4.dp)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 28.dp) // min-h-7
                .hoverable(interaction)
                .background(
                    if (hovered) MaridTokens.MenuHover else Color.Transparent,
                    RoundedCornerShape(6.dp),
                )
                .clickable(interactionSource = interaction, indication = null, onClick = onClick)
                .padding(start = 8.dp, end = 32.dp, top = 4.dp, bottom = 4.dp), // px-2 py-1
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                text = label,
                style = TextStyle(
                    fontSize = 14.sp,
                    lineHeight = 22.75.sp, // text-ui-base/relaxed
                    color = MaridTokens.Foreground,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (checked) {
                Icon(
                    imageVector = MaridIcons.Check,
                    contentDescription = null,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 8.dp)
                        .size(16.dp),
                    tint = MaridTokens.ForegroundSubtle,
                )
            }
        }
    }
}

// ----------------------------------------------------------------------------
// Model select — ModelConfigSelect.tsx + ComposerModelMenu/ComposerProviderMenu.
// ----------------------------------------------------------------------------

@Composable
private fun ModelSelectControl2c(
    models: List<ModelEntry2c>,
    selectedModel: ModelEntry2c?,
    onModelSelect: (ModelEntry2c) -> Unit,
    menuOpen: Boolean,
    onMenuOpenChange: (Boolean) -> Unit,
    providerMenuOpen: Boolean,
    onProviderMenuOpenChange: (Boolean) -> Unit,
    thoughtLevels: List<String>,
    thoughtValue: String,
    onThoughtValueChange: (String) -> Unit,
    anchor: MutableState<Anchor2c>,
    rootOrigin: State<IntOffset>,
) {
    val providerIds = models.map { it.providerId }.distinct()
    val currentProviderId =
        providerIds.firstOrNull { it == selectedModel?.providerId } ?: providerIds.firstOrNull()
    val currentProviderLabel =
        models.firstOrNull { it.providerId == currentProviderId }?.providerLabel ?: "—"
    val hasSelection = selectedModel != null

    // Trigger width lock for the open state: max(previous width, 8rem).
    val density = LocalDensity.current
    var triggerWidthPx by remember { mutableStateOf(0) }
    val openStateWidth = maxOf(
        triggerWidthPx,
        with(density) { 128.dp.roundToPx() }, // min-w-32 (8rem)
    )

    GhostTrigger2c(
        onClick = { onMenuOpenChange(true) },
        modifier = Modifier
            .height(28.dp) // h-7
            .then(
                if (menuOpen && hasSelection) {
                    Modifier.width(with(density) { openStateWidth.toDp() })
                } else {
                    Modifier.onSizeChanged { triggerWidthPx = it.width }
                },
            )
            .captureAnchor2c(anchor, rootOrigin)
            .padding(start = 8.dp, end = 6.dp), // pl-2 pr-1.5
        contentColor = if (hasSelection) MaridTokens.Foreground else MaridTokens.ForegroundSubtle,
    ) {
        // ComposerTriggerTransition frames — same 0.75em roll (deviation-free swap).
        // No fillMaxWidth here: the trigger row must wrap its content when closed.
        AnimatedContent(
            targetState = menuOpen && hasSelection,
            transitionSpec = {
                (slideInVertically(tween(200, easing = FastOutSlowInEasing)) { h ->
                    (h * 0.75f).roundToInt()
                } + fadeIn(tween(200))) togetherWith
                    (slideOutVertically(tween(200, easing = FastOutSlowInEasing)) { h ->
                        -(h * 0.75f).roundToInt()
                    } + fadeOut(tween(200)))
            },
            label = "modelTrigger2c",
        ) { openFrame ->
            if (openFrame) {
                // data-composer-open-label: centered "Select model" (fills the locked
                // trigger width from the modifier above).
                Text(
                    text = "Select model", // chat.toolbar.combined.selectModel
                    style = TextStyle(fontSize = 14.sp, color = MaridTokens.ForegroundSubtle),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp), // gap-1
                ) {
                    RollingLabel2c(
                        label = selectedModel?.modelId ?: "",
                        color = LocalContentColor2c.current,
                        modifier = Modifier.widthIn(max = 220.dp),
                    )
                    if (hasSelection) {
                        // ComposerThoughtIndicator label (meter hidden at desktop width).
                        Text(
                            text = thoughtLevelLabel2c("medium"),
                            style = TextStyle(
                                fontSize = 14.sp,
                                color = MaridTokens.ForegroundSubtle,
                            ),
                            maxLines = 1,
                        )
                    }
                    Icon(
                        imageVector = MaridIcons.ChevronDown,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaridTokens.ForegroundSubtle,
                    )
                }
            }
        }
    }

    if (menuOpen) {
        val browseKey = remember { mutableStateOf<String?>(null) }
        MenuPopup2c(
            anchor = anchor.value,
            alignCenter = true, // contentAlign="center"
            sideOffset = 2.dp, // DropdownMenuContent default sideOffset=2
            collisionPadding = 12.dp, // collisionPadding={12} for composerOptions
            onDismissRequest = { onMenuOpenChange(false) },
        ) {
            MenuSurface2c(modifier = Modifier.width(288.dp)) { // w-72
                // ComposerProviderMenu sub-trigger: min-h-8 gap-3, highlighted/open
                // bg-menu-hover/50.
                val providerInteraction = remember { MutableInteractionSource() }
                val providerHovered by providerInteraction.collectIsHoveredAsState()
                Box(modifier = Modifier.padding(horizontal = 4.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 32.dp) // min-h-8
                            .hoverable(providerInteraction)
                            .background(
                                if (providerHovered || providerMenuOpen) {
                                    Composer2cPalette.MenuHover50
                                } else {
                                    Color.Transparent
                                },
                                RoundedCornerShape(6.dp),
                            )
                            .clickable(
                                interactionSource = providerInteraction,
                                indication = null,
                            ) { onProviderMenuOpenChange(!providerMenuOpen) }
                            .padding(horizontal = 8.dp, vertical = 4.dp), // px-2 py-1
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp), // gap-3
                    ) {
                        Row(
                            modifier = Modifier.weight(1f),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp), // gap-1.5
                        ) {
                            Icon(
                                imageVector = MaridIcons.Package, // ProviderLogo fallback
                                contentDescription = null,
                                modifier = Modifier.size(16.dp), // size-4
                                tint = MaridTokens.Foreground,
                            )
                            Text(
                                text = currentProviderLabel,
                                style = TextStyle(
                                    fontSize = 14.sp,
                                    color = MaridTokens.Foreground,
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Icon(
                            imageVector = MaridIcons.ChevronRight,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaridTokens.ForegroundSubtle,
                        )
                    }
                }

                MenuSeparator2c(fullBleed = true, soft = true) // bg-border/50
                // DropdownMenuLabel className "text-ui-sm text-foreground-subtle".
                Box(modifier = Modifier.padding(horizontal = 4.dp)) {
                    Text(
                        text = "Models", // chat.toolbar.combined.models
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = MaridTokens.ForegroundSubtle,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 6.dp), // px-2 py-1.5
                        maxLines = 1,
                    )
                }

                // ScrollFadeViewport max-h-60 — model radio items.
                Column(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .heightIn(max = 240.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    val visibleModels = models.filter {
                        it.providerId == (browseKey.value ?: currentProviderId)
                    }
                    for (model in visibleModels) {
                        ModelItem2c(
                            name = model.modelId,
                            checked = model.modelId == selectedModel?.modelId &&
                                model.providerId == selectedModel?.providerId,
                            onClick = {
                                // Composer menu keeps the menu open after model select
                                // (onSelect preventDefault) so effort can be adjusted.
                                onModelSelect(model)
                                browseKey.value = null
                            },
                        )
                    }
                }

                // Effort section (ComposerModelMenu showEffort): separator + block.
                MenuSeparator2c(fullBleed = true, soft = true)
                EffortBlock2c(
                    levels = thoughtLevels,
                    value = thoughtValue,
                    onValueChange = onThoughtValueChange,
                    hasSelection = hasSelection,
                )
            }

            // Provider sub-menu — DropdownMenuSubContent: opens flush to the menu's
            // right edge, top-aligned with the menu frame (ComposerProviderMenu offset).
            if (providerMenuOpen) {
                val density2 = LocalDensity.current
                Popup(
                    offset = IntOffset(with(density2) { (288.dp + 4.dp).roundToPx() }, 0),
                    onDismissRequest = { onProviderMenuOpenChange(false) },
                    properties = PopupProperties(focusable = true),
                ) {
                    MenuSurface2c(modifier = Modifier.widthIn(min = 160.dp)) {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            for (providerId in providerIds) {
                                val first = models.first { it.providerId == providerId }
                                ProviderItem2c(
                                    label = first.providerLabel,
                                    checked = providerId == (browseKey.value ?: currentProviderId),
                                    onClick = {
                                        browseKey.value = providerId
                                        // Desktop: keep current selection if it belongs to
                                        // the browsed provider, else select its first model.
                                        if (selectedModel?.providerId != providerId) {
                                            models.firstOrNull { it.providerId == providerId }
                                                ?.let(onModelSelect)
                                        }
                                        onProviderMenuOpenChange(false)
                                    },
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
private fun ModelItem2c(name: String, checked: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 32.dp) // min-h-8
            .hoverable(interaction)
            .background(
                if (hovered) MaridTokens.MenuHover else Color.Transparent,
                RoundedCornerShape(6.dp),
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(start = 8.dp, end = 32.dp, top = 6.dp, bottom = 6.dp), // pl-2 pr-8 py-1.5
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp), // gap-2
        ) {
            Text(
                text = name,
                style = TextStyle(fontSize = 14.sp, color = MaridTokens.Foreground),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            // badgeLabel / vision badge: no catalog data in the port API — omitted.
        }
        if (checked) {
            Icon(
                imageVector = MaridIcons.Check,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp)
                    .size(16.dp),
                tint = MaridTokens.ForegroundSubtle,
            )
        }
    }
}

@Composable
private fun ProviderItem2c(label: String, checked: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 32.dp) // min-h-8
            .hoverable(interaction)
            .background(
                if (hovered) MaridTokens.MenuHover else Color.Transparent,
                RoundedCornerShape(6.dp),
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(start = 8.dp, end = 32.dp, top = 6.dp, bottom = 6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                imageVector = MaridIcons.Package,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaridTokens.Foreground,
            )
            Text(
                text = label,
                style = TextStyle(fontSize = 14.sp, color = MaridTokens.Foreground),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
        }
        if (checked) {
            Icon(
                imageVector = MaridIcons.Check,
                contentDescription = null,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 8.dp)
                    .size(16.dp),
                tint = MaridTokens.ForegroundSubtle,
            )
        }
    }
}

// ----------------------------------------------------------------------------
// Effort slider — ComposerModelMenu + composer-thought-slider.css.
// ----------------------------------------------------------------------------

@Composable
private fun EffortBlock2c(
    levels: List<String>,
    value: String,
    onValueChange: (String) -> Unit,
    hasSelection: Boolean,
) {
    val currentIndex = levels.indexOf(value)
    val currentLabel = if (currentIndex >= 0) thoughtLevelLabel2c(value) else "—"
    val single = levels.size == 1
    Column(modifier = Modifier.padding(horizontal = 4.dp)) {
        Column(modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)) { // px-2 py-2
            Row(
                modifier = Modifier.padding(bottom = 4.dp), // mb-1
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp), // gap-2
            ) {
                Text(
                    text = "Effort", // chat.toolbar.combined.effort
                    style = TextStyle(fontSize = 12.sp, color = MaridTokens.ForegroundSubtle),
                    maxLines = 1,
                )
                Text(
                    text = if (hasSelection) currentLabel else "—",
                    style = TextStyle(fontSize = 14.sp, color = MaridTokens.Foreground),
                    maxLines = 1,
                )
            }
            if (!single && levels.size > 1) {
                ThoughtSlider2c(
                    levels = levels,
                    currentIndex = if (currentIndex >= 0) currentIndex else 0,
                    onIndexChange = { index ->
                        levels.getOrNull(index)?.let(onValueChange)
                    },
                )
            }
        }
    }
}

@Composable
private fun ThoughtSlider2c(
    levels: List<String>,
    currentIndex: Int,
    onIndexChange: (Int) -> Unit,
) {
    val density = LocalDensity.current
    val thumbPx = with(density) { 28.dp.roundToPx() } // thumb size-7
    val insetPx = with(density) { 14.dp.roundToPx() } // thumb travel inset (28/2)
    var containerPx by remember { mutableStateOf(0) }
    val currentContainer by rememberUpdatedState(containerPx)
    val currentIndexState by rememberUpdatedState(currentIndex)
    val levelsState by rememberUpdatedState(levels)

    fun indexFromX(x: Float): Int {
        val trackWidth = (currentContainer - thumbPx).coerceAtLeast(1)
        val fraction = ((x - insetPx) / trackWidth).coerceIn(0f, 1f)
        return (fraction * (levelsState.size - 1)).roundToInt()
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp) // h-8
            .onSizeChanged { containerPx = it.width }
            .pointerInput(levels.size) {
                detectTapGestures { offset -> onIndexChange(indexFromX(offset.x)) }
            }
            .pointerInput(levels.size) {
                detectHorizontalDragGestures { change, _ ->
                    onIndexChange(indexFromX(change.position.x))
                }
            },
    ) {
        val progress = if (levels.size > 1) {
            currentIndex.toFloat() / (levels.size - 1)
        } else {
            0f
        }
        val isMax = currentIndex == levels.size - 1

        // Track: absolute inset-x-0 top-1 h-6 rounded-full bg-foreground/10
        // + inner border border-foreground/10.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = 4.dp) // top-1
                .height(24.dp) // h-6
                .clip(RoundedCornerShape(12.dp))
                .background(Composer2cPalette.Foreground10)
                .border(1.dp, Composer2cPalette.Foreground10, RoundedCornerShape(12.dp)),
        )

        // Fill: absolute inset-y-0 left-0 rounded-full; width 14px + (100%-28px)*p.
        val targetFill = with(density) {
            (insetPx + (containerPx - thumbPx) * progress).toDp()
        }
        val fillWidth by animateDpAsState(
            targetValue = targetFill,
            animationSpec = tween(120, easing = LinearOutSlowInEasing),
            label = "effortFill2c",
        )
        Box(
            modifier = Modifier
                .offset(y = 4.dp)
                .height(24.dp)
                .width(fillWidth)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    if (isMax) {
                        // data-max=true: 270deg blue-500 → success (left green, right blue).
                        Brush.horizontalGradient(
                            listOf(MaridTokens.Success, Composer2cPalette.Blue500),
                        )
                    } else {
                        SolidColor(MaridTokens.Success) // bg-success
                    },
                ),
        ) {
        if (isMax) {
            EffortBubbles2c(
                fillWidthPx = with(density) { fillWidth.roundToPx() },
                heightPx = with(density) { 24.dp.roundToPx() },
            )
        }
        }

        // Stops: inset-x-3.5 size-1 rounded-full, subtle → filled white @ 30%.
        Row(
            modifier = Modifier
                .matchParentSize()
                .padding(horizontal = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            for ((index, _) in levels.withIndex()) {
                Box(
                    modifier = Modifier
                        .size(4.dp) // size-1
                        .background(
                            if (index <= currentIndex) {
                                MaridTokens.White.copy(alpha = 0.30f)
                            } else {
                                MaridTokens.ForegroundSubtlest
                            },
                            CircleShape,
                        ),
                )
            }
        }

        // Thumb: composer-thought-slider-thumb size-7 white shadow-sm, top-0.5.
        val targetThumbX = with(density) {
            ((containerPx - thumbPx) * progress).toDp()
        }
        val thumbX by animateDpAsState(
            targetValue = targetThumbX,
            animationSpec = tween(120, easing = LinearOutSlowInEasing),
            label = "effortThumb2c",
        )
        Box(
            modifier = Modifier
                .offset(x = thumbX, y = 2.dp) // top-0.5
                .size(28.dp)
                .shadow(
                    1.dp,
                    CircleShape,
                    ambientColor = Composer2cPalette.ShadowBlack5,
                    spotColor = Composer2cPalette.ShadowBlack5,
                    clip = false,
                )
                .background(MaridTokens.White, CircleShape),
        )
    }
}

/** EFFORT_BUBBLES (ComposerModelMenu.tsx:16) — top/size/duration/delay, fixed seed. */
private data class EffortBubble2c(
    val topPx: Int,
    val sizeDp: Int,
    val durationMs: Int,
    val delayMs: Int,
)

private val EFFORT_BUBBLES_2C = listOf(
    EffortBubble2c(4, 2, 2600, -400),
    EffortBubble2c(14, 3, 3200, -2100),
    EffortBubble2c(8, 4, 2800, -1100),
    EffortBubble2c(18, 2, 3600, -800),
    EffortBubble2c(3, 3, 3000, -2500),
    EffortBubble2c(12, 2, 3400, -1700),
    EffortBubble2c(6, 3, 3100, -900),
    EffortBubble2c(17, 4, 2900, -2300),
    EffortBubble2c(10, 2, 3500, -200),
    EffortBubble2c(2, 2, 2700, -1800),
    EffortBubble2c(15, 3, 3300, -1200),
)

/** composer-thought-bubble-flow keyframes: x 104% → -4%, opacity 0/.4/.4/0. */
@Composable
private fun EffortBubbles2c(fillWidthPx: Int, heightPx: Int) {
    for (bubble in EFFORT_BUBBLES_2C) {
        val transition = rememberInfiniteTransition(label = "bubble2c")
        val raw by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                tween(durationMillis = bubble.durationMs, easing = LinearEasing),
            ),
            label = "bubble2c$bubble",
        )
        val phase = -bubble.delayMs.toFloat() / bubble.durationMs
        val p = ((raw + phase) % 1f).let { if (it < 0) it + 1f else it }
        val alpha = when {
            p < 0.15f -> 0.4f * (p / 0.15f)
            p < 0.8f -> 0.4f
            else -> 0.4f * ((1f - p) / 0.2f)
        }
        val x = ((1.04f - 1.08f * p) * fillWidthPx).roundToInt()
        Box(
            modifier = Modifier
                .offset { IntOffset(x, bubble.topPx) }
                .alpha(alpha),
        ) {
            Box(
                modifier = Modifier
                    .size(bubble.sizeDp.dp)
                    .background(MaridTokens.White, CircleShape),
            )
        }
    }
    // heightPx intentionally unused beyond documentation: tops are px within the 24px fill.
}

// ----------------------------------------------------------------------------
// Send — ChatPromptEditor submit button.
// ----------------------------------------------------------------------------

@Composable
private fun SendButton2c(enabled: Boolean, onClick: () -> Unit) {
    val interaction = remember { MutableInteractionSource() }
    val hovered by interaction.collectIsHoveredAsState()
    Box(
        modifier = Modifier
            .size(28.dp) // icon-md size-7
            .alpha(if (enabled) 1f else 0.5f) // disabled:opacity-50
            .hoverable(interaction)
            .background(
                when {
                    enabled && hovered -> Composer2cPalette.Brand80 // hover:bg-brand/80
                    else -> MaridTokens.Brand // bg-brand
                },
                CircleShape, // rounded-full
            )
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = MaridIcons.ArrowUp,
            contentDescription = "Send",
            modifier = Modifier.size(16.dp), // ArrowUpIcon size-4
            tint = MaridTokens.ForegroundInverse, // text-foreground-inverse
        )
    }
}

// ----------------------------------------------------------------------------
// RollingToolbarLabel.
// ----------------------------------------------------------------------------

@Composable
private fun RollingLabel2c(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 14.sp,
) {
    val density = LocalDensity.current
    val boxHeight = with(density) { (fontSize * 1.3f).toDp() } // h-[1.3em]
    AnimatedContent(
        targetState = label,
        transitionSpec = {
            (slideInVertically(tween(200, easing = FastOutSlowInEasing)) { h ->
                (h * 0.75f).roundToInt()
            } + fadeIn(tween(200))) togetherWith
                (slideOutVertically(tween(200, easing = FastOutSlowInEasing)) { h ->
                    -(h * 0.75f).roundToInt()
                } + fadeOut(tween(200)))
        },
        modifier = modifier
            .height(boxHeight)
            .clipToBounds(), // overflow-hidden
        label = "rolling2c",
    ) { value ->
        Text(
            text = value,
            style = TextStyle(
                fontSize = fontSize,
                lineHeight = fontSize * 1.25f, // leading-[1.25]
                color = color,
            ),
            softWrap = false, // whitespace-nowrap
            maxLines = 1,
        )
    }
}

// ----------------------------------------------------------------------------
// Menu chrome — DropdownMenuContent / SelectContent / DropdownMenuSubContent:
// "rounded-lg border border-popover-border bg-menu p-1 gap-0.5 shadow-md".
// ----------------------------------------------------------------------------

@Composable
private fun MenuSurface2c(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(8.dp) // rounded-lg
    Box(modifier = modifier) {
        // shadow-md (deviation 7): elevation shadow, black 10%.
        Box(
            modifier = Modifier
                .matchParentSize()
                .shadow(
                    6.dp,
                    shape,
                    ambientColor = Composer2cPalette.ShadowBlack10,
                    spotColor = Composer2cPalette.ShadowBlack10,
                    clip = false,
                ),
        )
        Column(
            modifier = Modifier
                .background(Composer2cPalette.MenuBg, shape) // bg-menu
                .border(1.dp, MaridTokens.Border, shape) // border-popover-border
                .padding(vertical = 4.dp), // p-1 (horizontal inset lives on items/separators)
            verticalArrangement = Arrangement.spacedBy(2.dp), // gap-0.5
            content = content,
        )
    }
}

/**
 * DropdownMenuSeparator "-mx-1 my-1 h-px bg-border" — full-bleed by living outside
 * the item inset; `soft` renders the composer override bg-border/50 (white 5%).
 */
@Composable
private fun MenuSeparator2c(fullBleed: Boolean, soft: Boolean = false) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = if (fullBleed) 0.dp else 4.dp, vertical = 4.dp) // my-1
            .height(1.dp)
            .background(if (soft) MaridTokens.Surface else MaridTokens.Border),
    )
}

// ----------------------------------------------------------------------------
// Popup plumbing.
// ----------------------------------------------------------------------------

/** Trigger bounds relative to the composer root. */
private class Anchor2c(
    val offset: IntOffset = IntOffset.Zero,
    val size: IntSize = IntSize.Zero,
)

private fun Modifier.captureAnchor2c(
    anchor: MutableState<Anchor2c>,
    rootOrigin: State<IntOffset>,
): Modifier = onGloballyPositioned { coords ->
    val pos = coords.positionInRoot()
    val origin = rootOrigin.value
    anchor.value = Anchor2c(
        offset = IntOffset(
            pos.x.roundToInt() - origin.x,
            pos.y.roundToInt() - origin.y,
        ),
        size = coords.size,
    )
}

/** Radix side="top" popper: sideOffset gap, align start/center, collision padding. */
private class TopMenuPositionProvider2c(
    private val anchor: Anchor2c,
    private val alignCenter: Boolean,
    private val sideOffsetPx: Int,
    private val collisionPadPx: Int,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val trigger = anchorBounds.topLeft + anchor.offset
        val rawX = if (alignCenter) {
            trigger.x + anchor.size.width / 2 - popupContentSize.width / 2
        } else {
            trigger.x
        }
        val maxX = (windowSize.width - collisionPadPx - popupContentSize.width)
            .coerceAtLeast(collisionPadPx)
        val x = rawX.coerceIn(collisionPadPx, maxX)
        val aboveY = trigger.y - sideOffsetPx - popupContentSize.height
        val y = if (aboveY >= collisionPadPx) {
            aboveY
        } else {
            // Radix flip: not enough space above → open below the trigger.
            (trigger.y + anchor.size.height + sideOffsetPx)
                .coerceAtMost((windowSize.height - collisionPadPx - popupContentSize.height).coerceAtLeast(0))
        }
        return IntOffset(x, y)
    }
}

@Composable
private fun MenuPopup2c(
    anchor: Anchor2c,
    alignCenter: Boolean,
    sideOffset: Dp,
    collisionPadding: Dp,
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val provider = remember(anchor, alignCenter) {
        TopMenuPositionProvider2c(
            anchor = anchor,
            alignCenter = alignCenter,
            sideOffsetPx = with(density) { sideOffset.roundToPx() },
            collisionPadPx = with(density) { collisionPadding.roundToPx() },
        )
    }
    Popup(
        popupPositionProvider = provider,
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true),
        content = content,
    )
}


