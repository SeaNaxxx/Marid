package com.marid.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.marid.app.icons.MaridIcons

/**
 * 1:1 port of the ZCode desktop terminal pane chrome — dark theme only — with an HONEST
 * unavailability message instead of a terminal engine.
 *
 * WHY PLACEHOLDER: the desktop terminal is xterm.js over a node-pty stream owned by the engine
 * process (packages/ui/src/terminal/TerminalSession.tsx). The PTY-PROBE-VERDICT research
 * document concluded NEGATIVE for Android: node-pty cannot run in the Android app sandbox, and
 * the engine RPC does not expose a PTY byte stream to third-party clients. Rendering a fake
 * shell (simulated prompt, dead input line) would lie to the user, so the sheet renders the
 * exact desktop chrome + one centered line stating the terminal is unavailable. No fake output,
 * no dead input line, no dead "new terminal" button.
 *
 * FILES PORTED
 *   - packages/ui/src/Terminal.tsx               (section chrome: p-3 pb-2 gap-2 bg-background;
 *                                                 header: title + TabsList + action buttons)
 *   - packages/ui/src/terminal/TerminalTabTrigger.tsx (tab visuals: h-7 max-w-36 rounded-lg
 *                                                 pl-2 pr-1 gap-1 text-ui-base font-medium,
 *                                                 active = bg-selected + text-foreground,
 *                                                 ghost icon-xs close button)
 *   - packages/ui/src/terminal/terminalTheme.ts  (dark theme values: --color-terminal-bg =
 *                                                 neutral-950, --color-terminal-fg =
 *                                                 neutral-50 → MaridTokens.TerminalBg /
 *                                                 TerminalFg; xterm fontSize 13 +
 *                                                 DEFAULT_TERMINAL_FONT_FAMILY = monospace)
 *   - packages/ui/src/terminal/TerminalSession.tsx (font config only; engine not portable)
 *   - packages/ui/src/i18n/locales/en-US.ts      (terminal.title = "Terminal",
 *                                                 terminal.close = "Close terminal pane",
 *                                                 terminal.closeTab = "Close {title}")
 *
 * EXTRACTION NOTES (source class → Compose value; 1 spacing unit = 4dp; radius md=6 lg=8)
 *   section "flex h-full min-h-0 flex-col gap-2 overflow-hidden bg-background p-3 pb-2"
 *        → Column(spacedBy 8dp), padding(start/top/end 12dp, bottom 8dp), bg = MaridTokens
 *          .Background (neutral-900), overflow-hidden → clipToBounds. The sheet is
 *          bottom-anchored by the caller; height is caller-controlled via `modifier`
 *          (content area = weight(1f), so a bounded sheet expands the TerminalBg fill;
 *          without a height the sheet wraps the chrome).
 *   header "flex shrink-0 items-center gap-2"        → Row(spacedBy 8dp, centerVertically).
 *   pane title "truncate text-ui-base font-medium text-foreground" → "Terminal", 14sp,
 *          FontWeight.Medium(500), Foreground.
 *   TabsList "flex !h-7 w-max justify-start gap-1 rounded-none bg-transparent p-0"
 *                                                    → Row(height 28dp, spacedBy 4dp).
 *   tab (data-active branch) "!h-7 max-w-36 rounded-lg pl-2 pr-1 gap-1 text-ui-base font-medium
 *    data-active:!bg-selected data-active:text-foreground"
 *                                                    → 28dp height, max width 144dp, radius
 *                                                      8dp, padding(start 8dp, end 4dp), gap
 *                                                      4dp, bg = MaridTokens.Selected (white
 *                                                      10%), text Foreground Medium 14sp.
 *   tab close "Button variant=ghost size=icon-xs" with "X size-3"
 *                                                    → 20dp box (icon-xs = size-5), radius
 *                                                      4dp (rounded-sm = 0.25rem), X icon 12dp;
 *                                                      live: single-session close action =
 *                                                      "close-panel" (getTerminalSessionClose
 *                                                      Action) → onClose.
 *   actions "flex shrink-0 items-center gap-1"; close "Button variant=ghost size=icon-md"
 *    with "X h-4 w-4" (ghost: "hover:bg-hover", hover = --color-hover = white 10%)
 *                                                    → Row(spacedBy 4dp); 28dp box (icon-md =
 *                                                      size-7), radius 8dp, pressed bg =
 *                                                      MaridTokens.SurfaceHover, X icon 16dp,
 *                                                      tint Foreground; testTag
 *                                                      "terminal-close-button" (TID_TERMINAL_
 *                                                      CLOSE_BUTTON); data-testid TID_TERMINAL
 *                                                      = "terminal" on the root.
 *   TabsContent "min-h-0 flex-1" + TerminalSession canvas → Box(bg = MaridTokens.TerminalBg
 *          [dark --color-terminal-bg = neutral-950], centered) with the unavailability line:
 *          "Terminal unavailable on Android (no PTY)", 13sp (xterm fontSize: 13), monospace
 *          (DEFAULT_TERMINAL_FONT_FAMILY), color MaridTokens.TerminalFg [dark
 *          --color-terminal-fg = neutral-50].
 *
 * DEVIATIONS (none invented):
 *   1. The "New terminal" Plus button (Terminal.tsx, rendered when !isOfficeMode) is omitted:
 *      creating a PTY session is impossible on Android; a dead button is forbidden.
 *   2. shellLabel sub-label (activeSession?.shellLabel) is session-runtime data; no session
 *      exists, so it is not rendered (source renders it only when non-null).
 *   3. Only one tab is shown ("Terminal" — the source's fallback when cwd has no leaf:
 *      projectName = getPathLeaf(cwd) || terminal.title, formatTerminalTabTitle(project, 1) =
 *      project for index 1). Multi-tab/session state does not exist without a PTY.
 *   4. Tab inactive/hover styles (hover:!bg-hover hover:text-foreground on inactive tabs) are
 *      unreachable: the single placeholder tab is always data-active.
 *   5. Font: SFMono/Menlo/Consolas stack → Android FontFamily.Monospace.
 */
@Composable
public fun MaridTerminalSheet(onClose: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(MaridTokens.Background) // bg-background
            .clipToBounds() // overflow-hidden
            .testTag("terminal") // TID_TERMINAL
            // section padding "p-3 pb-2"
            .padding(start = 12.dp, top = 12.dp, end = 12.dp, bottom = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp), // gap-2
    ) {
        // header: "flex shrink-0 items-center gap-2"
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp), // gap-2
        ) {
            // "truncate text-ui-base font-medium text-foreground"
            Text(
                text = "Terminal", // terminal.title (en-US)
                color = MaridTokens.Foreground,
                fontSize = 14.sp, // text-ui-base
                fontWeight = FontWeight.Medium, // font-medium
                maxLines = 1,
                overflow = TextOverflow.Ellipsis, // truncate
            )

            // TabsList: "flex !h-7 w-max justify-start gap-1 rounded-none bg-transparent p-0"
            Row(
                modifier = Modifier.height(28.dp), // !h-7
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp), // gap-1
            ) {
                TerminalTab2e(onClose = onClose)
            }

            Spacer(modifier = Modifier.weight(1f))

            // actions: "flex shrink-0 items-center gap-1"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp), // gap-1
            ) {
                // DEVIATION 1: "New terminal" Plus button omitted (no PTY).
                GhostIconButton2e(
                    icon = MaridIcons.X,
                    iconSize = 16.dp, // "X h-4 w-4"
                    contentDescription = "Close terminal pane", // terminal.close (en-US)
                    testTag = "terminal-close-button", // TID_TERMINAL_CLOSE_BUTTON
                    onClick = onClose,
                )
            }
        }

        // TabsContent area: "min-h-0 flex-1 overflow-hidden" → TerminalBg fill + honest message
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f) // flex-1 — fills a caller-bounded sheet, wraps when unbounded
                .background(MaridTokens.TerminalBg), // --color-terminal-bg (dark = neutral-950)
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Terminal unavailable on Android (no PTY)",
                color = MaridTokens.TerminalFg, // --color-terminal-fg (dark = neutral-50)
                fontSize = 13.sp, // xterm fontSize: 13
                fontFamily = FontFamily.Monospace, // DEFAULT_TERMINAL_FONT_FAMILY
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
        }
    }
}

/**
 * TerminalTabTrigger (data-active state): "!h-7 max-w-36 rounded-lg pl-2 pr-1 gap-1
 * text-ui-base font-medium data-active:!bg-selected data-active:text-foreground" + ghost
 * icon-xs close ("X size-3").
 */
@Composable
private fun TerminalTab2e(onClose: () -> Unit) {
    Row(
        modifier = Modifier
            .widthIn(max = 144.dp) // max-w-36 = 36 * 4dp
            .height(28.dp) // !h-7
            .clip(RoundedCornerShape(8.dp)) // rounded-lg
            .background(MaridTokens.Selected), // data-active:!bg-selected (white 10%)
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp), // gap-1
    ) {
        Text(
            text = "Terminal", // projectName fallback = terminal.title; formatTerminalTabTitle(_, 1)
            color = MaridTokens.Foreground, // data-active:text-foreground
            fontSize = 14.sp, // text-ui-base
            fontWeight = FontWeight.Medium, // font-medium
            maxLines = 1,
            modifier = Modifier
                .padding(start = 8.dp) // pl-2
                .weight(1f, fill = false),
        )
        // tab close: Button ghost size="icon-xs" (size-5 rounded-sm) + "X size-3"; the active
        // tab always shows it. Single-session close action = "close-panel" → onClose.
        val interactionSource = remember { MutableInteractionSource() }
        val pressed by interactionSource.collectIsPressedAsState()
        Box(
            modifier = Modifier
                .size(20.dp) // icon-xs = size-5
                .clip(RoundedCornerShape(4.dp)) // rounded-sm = 0.25rem
                .background(if (pressed) MaridTokens.SurfaceHover else Color.Transparent) // ghost hover:bg-hover
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClose,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = MaridIcons.X,
                contentDescription = "Close Terminal", // terminal.closeTab "{title}" = "Close Terminal"
                modifier = Modifier.size(12.dp), // size-3
                tint = MaridTokens.Foreground,
            )
        }
    }
}

/** Button variant=ghost size=icon-md ("size-7 rounded-lg", hover:bg-hover). */
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
            .size(28.dp) // icon-md = size-7
            .testTag(testTag)
            .clip(RoundedCornerShape(8.dp)) // rounded-lg
            .background(if (pressed) MaridTokens.SurfaceHover else Color.Transparent) // hover:bg-hover
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
            tint = MaridTokens.Foreground, // ghost: text-foreground
        )
    }
}
