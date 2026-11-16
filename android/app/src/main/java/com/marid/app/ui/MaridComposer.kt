package com.marid.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marid.app.icons.MaridIcons
import com.marid.app.state.ComposerMode
import com.marid.app.state.MaridUiState
import com.marid.app.state.ModelEntry
import com.marid.app.theme.MaridRadius
import com.marid.app.theme.MaridTextSize
import com.marid.app.theme.maridTokens

/**
 * Composer — 1:1 port of ChatPromptEditor.tsx (shell) + the bottom toolbar:
 *   shell: "rounded-2xl border-[0.5px] border-input-border bg-input p-3"
 *   toolbar DOM order: [+] → mode → … → [model cluster] → send
 */
@Composable
fun MaridComposer(
    state: MaridUiState,
    onTextChange: (String) -> Unit,
    onSend: () -> Unit,
    onToggleModeMenu: () -> Unit,
    onToggleModelMenu: () -> Unit,
    onSelectModel: (ModelEntry) -> Unit,
    onSelectMode: (ComposerMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val t = maridTokens()
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(t.background)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        // Model dropdown opens ABOVE the composer shell (DropdownMenu side=top)
        if (state.modelMenuOpen) {
            ModelMenu(
                models = state.models,
                selected = state.selectedModel,
                onSelect = onSelectModel,
            )
        }
        if (state.modeMenuOpen) {
            ModeMenu(selected = state.composerMode, onSelect = onSelectMode)
        }

        // Composer shell — rounded-2xl border-[0.5px] bg-input p-3
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(MaridRadius.Xl2.dp)) // rounded-2xl 16px
                .border(0.5.dp, t.border, RoundedCornerShape(MaridRadius.Xl2.dp))
                .background(t.input) // bg-input #2b2b2b
                .padding(12.dp), // p-3
        ) {
            // LexicalChatInput: min-h-10 max-h-40 text-foreground
            BasicTextField(
                value = state.composerText,
                onValueChange = onTextChange,
                textStyle = TextStyle(
                    fontSize = MaridTextSize.Base.sp,
                    lineHeight = 20.sp,
                    color = t.foreground,
                ),
                cursorBrush = SolidColor(t.foreground),
                maxLines = 6,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 40.dp) // min-h-10
                    .semantics { contentDescription = "Ask for follow-up changes" },
                decorationBox = { inner ->
                    Box {
                        if (state.composerText.isEmpty()) {
                            Text(
                                text = "Ask for follow-up changes", // chat.placeholder.followUpAsk
                                fontSize = MaridTextSize.Base.sp,
                                color = t.foregroundSubtlest, // placeholder text-foreground-subtlest
                                maxLines = 2,
                            )
                        }
                        inner()
                    }
                },
            )

            // Toolbar: "group/toolbar flex items-end gap-3" — leading [+][mode], trailing [model][send]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // (a) "+" attachments button — ghost icon-md rounded-lg
                ComposerIconButton(
                    icon = MaridIcons.Plus,
                    contentDescription = "Add attachment",
                    onClick = { /* attachment menu — parity planned */ },
                )

                Spacer(modifier = Modifier.width(8.dp))

                // (b) Mode control — HandIcon + "Ask before changes" + ChevronDown
                // V4ComposerModeControls.tsx:127-147 (mode.label.glm.build)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(MaridRadius.Lg.dp))
                        .clickable { onToggleModeMenu() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .semantics { contentDescription = "Switch mode" },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        MaridIcons.Hand,
                        contentDescription = null, // resolveModeOptionIcon default → HandIcon
                        tint = t.foreground,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = state.composerMode.label,
                        fontSize = MaridTextSize.Base.sp,
                        color = t.foreground,
                    )
                    Icon(
                        MaridIcons.ChevronDown,
                        contentDescription = null,
                        tint = t.foreground,
                        modifier = Modifier.size(14.dp),
                    )
                }

                // Leading flex-1 spacer ("flex min-w-0 flex-1 items-center")
                Box(modifier = Modifier.weight(1f))

                // (c) Model selector — ModelConfigSelect.tsx trigger:
                // PackageIcon + model label + thought level ("Off") + ChevronDown
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(MaridRadius.Lg.dp))
                        .clickable { onToggleModelMenu() }
                        .padding(start = 8.dp, end = 6.dp, top = 4.dp, bottom = 4.dp)
                        .semantics { contentDescription = "Model selector" },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        MaridIcons.Package,
                        contentDescription = null,
                        tint = t.foreground,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = state.selectedModel.label,
                        fontSize = MaridTextSize.Base.sp,
                        color = t.foreground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = state.thoughtLevel, // ComposerThoughtIndicator "Off"
                        fontSize = MaridTextSize.Base.sp,
                        color = t.foregroundSubtle,
                    )
                    Icon(
                        MaridIcons.ChevronDown,
                        contentDescription = null,
                        tint = t.foregroundSubtle,
                        modifier = Modifier.size(14.dp),
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // (d) Send — circular bg-brand + ArrowUp (text-foreground-inverse)
                // ConversationComposer.tsx:2734-2745
                Box(
                    modifier = Modifier
                        .size(28.dp) // size="icon-md"
                        .clip(CircleShape) // rounded-full
                        .background(t.brand) // bg-brand (zai-dark: #ffffff)
                        .clickable(enabled = state.sendEnabled && !state.sending) { onSend() }
                        .semantics { contentDescription = "Send" },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (state.sending) MaridIcons.LoaderCircle else MaridIcons.ArrowUp,
                        contentDescription = null,
                        tint = t.foregroundInverse, // text-foreground-inverse #000
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
        }
    }
}

/** Ghost icon button — icon-md: 28dp face, 16dp icon, rounded-lg. */
@Composable
private fun ComposerIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val t = maridTokens()
    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(MaridRadius.Lg.dp))
            .clickable { onClick() }
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = t.foreground,
            modifier = Modifier.size(16.dp),
        )
    }
}

/**
 * Model menu — dropdown fed by model-selection.getView() (engine-fetched,
 * replacing the static "mock-model Off" text per task STEP 3).
 */
@Composable
private fun ModelMenu(
    models: List<ModelEntry>,
    selected: ModelEntry,
    onSelect: (ModelEntry) -> Unit,
) {
    val t = maridTokens()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MaridRadius.Xl.dp))
            .border(0.5.dp, t.border, RoundedCornerShape(MaridRadius.Xl.dp))
            .background(t.popover) // bg-popover #2b2b2b
            .heightIn(max = 240.dp)
            .padding(4.dp),
    ) {
        LazyColumn {
            items(models) { model ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(MaridRadius.Lg.dp))
                        .background(if (model == selected) t.selected else androidx.compose.ui.graphics.Color.Transparent)
                        .clickable { onSelect(model) }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = model.label,
                        fontSize = MaridTextSize.Base.sp,
                        color = t.foreground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (model == selected) {
                        Icon(
                            MaridIcons.Check,
                            contentDescription = "Selected",
                            tint = t.success,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
        }
    }
}

/** Mode menu — V4ComposerModeControls DropdownMenuContent w-64: Plan + permission modes. */
@Composable
private fun ModeMenu(
    selected: ComposerMode,
    onSelect: (ComposerMode) -> Unit,
) {
    val t = maridTokens()
    Column(
        modifier = Modifier
            .width(256.dp) // w-64
            .clip(RoundedCornerShape(MaridRadius.Lg.dp))
            .border(0.5.dp, t.border, RoundedCornerShape(MaridRadius.Lg.dp))
            .background(t.popover)
            .padding(4.dp),
    ) {
        // Plan checkbox item (Lightbulb icon + description)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(MaridRadius.Lg.dp))
                .clickable { }
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                MaridIcons.Lightbulb,
                contentDescription = null,
                tint = t.foreground,
                modifier = Modifier.size(18.dp),
            )
            Column {
                Text(text = "Plan", fontSize = MaridTextSize.Base.sp, color = t.foreground)
                Text(
                    text = "Explore and propose a plan first",
                    fontSize = MaridTextSize.Sm.sp,
                    color = t.foregroundSubtle, // description text-ui-sm text-foreground-subtle
                )
            }
        }
        // Permission modes radio group
        ComposerMode.entries.forEach { mode ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(MaridRadius.Lg.dp))
                    .clickable { onSelect(mode) }
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    MaridIcons.Hand,
                    contentDescription = null,
                    tint = if (mode == selected) t.foreground else t.foregroundSubtle,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = mode.label,
                    fontSize = MaridTextSize.Base.sp,
                    color = t.foreground,
                    fontWeight = if (mode == selected) FontWeight.Medium else FontWeight.Normal,
                )
            }
        }
    }
}
