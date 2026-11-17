# Qodex Design System — Jetpack Compose (Material 3)

This document is the single source of truth for all visual and interaction rules in
Qodex's Android UI. It is written for coding agents (GLM, Claude, Qwen, Codex) and
human contributors.

**This file supersedes and replaces the previous desktop/web design system.**
The old Tailwind CSS rules (`text-ui-*` scales), `packages/ui/src/styles.css`
references, and Windows/Linux desktop window-control guidelines are deleted and
MUST NOT be reintroduced. Qodex's UI is 100% native Jetpack Compose running on
Android phones — there is no web surface, no desktop window chrome, and no
browser to style.

`packages/ui` (React) remains a *behavioral* reference for desktop feature parity
only. Never copy its CSS, class names, or DOM structure into Compose code.

## 1. Highest-priority constraints

These are design-system defects, not stylistic preferences:

1. **Compose only.** No WebView for any first-party UI surface, no HTML/CSS, no
   Jetpack XML layouts, no React Native.
2. **Material 3 via `MaterialTheme`.** All colors, typography, and shapes resolve
   through `MaterialTheme.colorScheme`, `MaterialTheme.typography`, and
   `MaterialTheme.shapes` — or through the explicit `QodexTokens` palette when a
   token has no Material slot.
3. **Never hardcode font sizes or colors inline** (`fontSize = 15.sp` scattered at
   call sites, `Color(0xFF...)` duplicated in screens). Add or use a token first.
4. **48dp minimum touch target** for every interactive element (see Section 4).
5. **Edge-to-edge is mandatory.** Screens draw under system bars; insets are
   consumed explicitly (see Section 5). No `fitsSystemWindows` hacks.
6. **Both dark and light themes must remain usable.** A change that only looks
   right in one mode is incomplete.

## 2. Product character

Qodex is a native Android coding agent. The interface should feel calm, dense,
and operational — a workbench, not a marketing site.

Design for:

- one-handed phone use (Android 14+ ARM64 devices)
- long agent sessions with streaming output
- high information density without cramped touch targets
- readable chat, diff, and terminal content
- internationalization and variable text length

Avoid:

- oversized marketing-style spacing or hero sections
- playful gradients as the default UI language
- bright full-surface brand fills
- ambiguous hierarchy between background, card, and popover surfaces
- ALL-CAPS button labels and robotic copy ("Supercharge your coding!")

## 3. Color system

### 3.1 Palette source of truth

`android/app/src/main/java/com/qodex/app/ui/QodexTokens.kt` is the only place raw
color values live. It holds exact sRGB conversions of Tailwind v4's oklch palette
(`Neutral`, `Sky`, `Green`, `Red`, `Yellow`, `Teal`, `Violet`, ...). Do not
duplicate numeric values in documentation or in screen code — reference the token.

### 3.2 Dark theme (current baseline)

Semantic dark tokens already defined in `QodexTokens`:

| Slot | Token | Value |
| --- | --- | --- |
| Background | `QodexTokens.Background` | `Neutral900` `#171717` |
| Sidebar | `QodexTokens.Sidebar` | `Neutral950` `#0A0A0A` |
| Card | `QodexTokens.Card` | `Neutral800` `#262626` |
| Card selected | `QodexTokens.CardSelected` | `Neutral700` |
| Popover / menu | `QodexTokens.Popover` / `Menu` | `Neutral800` / `Neutral950` |
| Foreground | `QodexTokens.Foreground` | `Neutral200` `#E5E5E5` |
| Foreground subtle | `QodexTokens.ForegroundSubtle` | 60% `#E5E5E5` |
| Border | `QodexTokens.Border` | 10% white |
| Brand / links | `QodexTokens.Brand` | `Sky500` `#00A6F4` |
| Primary button | `QodexTokens.Primary` / `PrimaryForeground` | `Neutral50` / `Neutral950` |
| Destructive | `QodexTokens.Destructive` | `Red500` |
| Success / warning | `QodexTokens.Success` / `Warning` | `Green500` / `Yellow500` |
| Input | `QodexTokens.Input` | `Neutral800` |
| Terminal | `TerminalBg` / `TerminalFg` | `Neutral950` / `Neutral50` |

Overlay washes (`Surface`, `SurfaceHover`, `Selected`, `UserMessageBubble`) are
alpha whites and must stay alpha-based so they invert cleanly in light mode.

### 3.3 Light theme (target mapping)

Light mode maps the same neutral scale to inverted surfaces. When implementing
`lightColorScheme`, use:

| Slot | Value |
| --- | --- |
| Background / surfaces | `Neutral50` `#FAFAFA` |
| Sidebar / terminal | `Neutral100` `#F5F5F5` |
| Card / popover | `White` |
| Card selected | `Neutral200` `#E5E5E5` |
| Foreground | `Neutral900` `#171717` |
| Foreground subtle | `Neutral500` `#737373` |
| Border | `Neutral200` `#E5E5E5` (solid) |
| Overlay washes | alpha `Neutral900` at the same opacities (e.g. `UserMessageBubble` = 8% `Neutral900`) |

Brand, Destructive, Success, Warning, diff, and trajectory colors are identical
in both modes (they already pass contrast on light backgrounds).

### 3.4 Wiring rule

Screens must not read `QodexTokens` directly for themable slots. Introduce a
`QodexTheme` composable (dark = current tokens, light = Section 3.3) that wraps
the app in `MaterialTheme(colorScheme = ..., typography = ...)` and expose
themed slots through `MaterialTheme.colorScheme`. `QodexTokens` constants are
then referenced only inside theme construction and for non-themable, role-fixed
colors (trajectory lanes, git status colors, terminal).

### 3.5 Color usage rules

- Semantic feedback only: red = destructive/error, green = success/added lines,
  amber = warning/modified files. Never use brand color for errors.
- One accent per screen region. Do not mix `Sky`, `Violet`, and `Teal` as
  competing highlights inside a single card.
- Selected states change the *surface* (token `Selected` / `CardSelected`), not
  the text color alone.

## 4. Typography

### 4.1 Material 3 typography slots

All text styles resolve through `MaterialTheme.typography`. Configure the slots
once in `QodexTheme` with these values:

| Slot | Size / line height | Weight | Used for |
| --- | --- | --- | --- |
| `displaySmall` | 36 / 44 | 600 | Welcome screen title |
| `headlineSmall` | 24 / 32 | 600 | Screen titles |
| `titleLarge` | 20 / 28 | 600 | Dialog titles, section headers |
| `titleMedium` | 16 / 24 | 600 | Card titles, list item titles |
| `titleSmall` | 14 / 20 | 600 | Inline headings, metadata labels |
| `bodyLarge` | 16 / 24 | 400 | Composer input text |
| `bodyMedium` | 14 / 20 | 400 | Default chat and body text |
| `bodySmall` | 12 / 16 | 400 | Secondary metadata, timestamps |
| `labelLarge` | 14 / 20 | 500 | Button labels |
| `labelMedium` | 12 / 16 | 500 | Chips, tags, badges |
| `labelSmall` | 11 / 16 | 500 | Fine-print status labels |

Rules:

- Never introduce arbitrary sizes (`fontSize = 13.sp` at a call site) or scale
  text by tweaking density; adjust the slot table instead.
- The only monospace exception is code-adjacent rendering: chat code fences,
  **Diff Viewer**, and **TerminalStream** use `FontFamily.Monospace` at 13sp /
  20sp line height. Their surrounding controls still use the slots above.

## 5. Spacing, touch targets, and window insets

### 5.1 Spacing grid

- Base unit: 4dp (`QodexTokens.SPACING_UNIT`). Use multiples: 4, 8, 12, 16, 24.
- Screen horizontal padding: 16dp. Card inner padding: 12–16dp. Gaps between
  list items: 8dp.
- Do not exceed 24dp vertical rhythm in dense lists (settings, model lists).

### 5.2 Touch targets (mandatory)

- Every clickable element — buttons, list rows, icons, chips, send button,
  approval actions — exposes at least **48dp x 48dp** of touch area.
- Icons rendered at 20–24dp visual size must sit in a ≥48dp clickable container
  (`Modifier.size(48.dp)` box or `minimumInteractiveComponentSize`).
- Adjacent destructive actions (e.g. "Deny" next to "Approve") keep ≥8dp gap so
  a missed tap cannot trigger the wrong action.
- List rows: minimum height 56dp for settings/model entries; chat rows may be
  shorter if the whole row is not clickable.

### 5.3 Window insets and edge-to-edge

`MainActivity` calls `enableEdgeToEdge()`; all screens draw full-bleed. Insets
are consumed explicitly, top-down through the hierarchy:

1. **Status bar:** header bar applies `WindowInsets.statusBarsPadding()` (or
   `windowInsetsPadding(WindowInsets.statusBars)`).
2. **Navigation bar / gesture area:** the root screen bottom container applies
   `navigationBarsPadding()`.
3. **Keyboard (IME):** the composer container applies
   `Modifier.imePadding()` and the chat list behind it stays scrollable;
   `windowSoftInputMode="adjustResize"` stays declared in the manifest. Never
   let the keyboard cover the send button or the text field.
4. **RTL:** layouts use start/end (`Arrangement`, `padding(start = …)`), never
   raw left/right, so Arabic/Hebrew locales mirror correctly.

## 6. Shapes and elevation

| Token | Radius | Used for |
| --- | --- | --- |
| `RADIUS_MD` (6dp) | small | chips, tags, inline badges |
| `RADIUS_LG` (8dp) | medium | buttons, inputs, small cards |
| `RADIUS_XL` (12dp) | large | cards, popovers, chat bubbles |
| `RADIUS_2XL` (16dp) | sheet | bottom sheets, side drawer |

- Elevation is restrained: cards rest at 0dp (surface tone separation via
  tokens), popovers/menus at 3dp, dragging elements at 8dp. No colored shadows.

## 7. Core components (native composables)

These components mirror the desktop feature set. Implement them as Composables
under `com.qodex.app.ui`, stateless where possible, driven by the ViewModel and
the engine RPC stream.

### 7.1 Chat Bubble (part of MessageTimeline / QodexChatArea)

- **User message:** right-aligned bubble, background `UserMessageBubble` (8–10%
  wash), radius `RADIUS_XL`, max width 85% of the row; text uses
  `bodyLarge`.
- **Agent message:** full-width row (no bubble) with a role label
  (`labelMedium`, `ForegroundSubtle`) above `bodyMedium` content; markdown is
  rendered with code fences in monospace.
- **Streaming:** the in-flight assistant block shows an animated caret (opacity
  pulse, 900ms) and grows live; completed blocks stop animating. Never block
  input or scroll while streaming.
- **Tool call block:** each tool invocation renders as a bordered card
  (`Border` token, `RADIUS_LG`) with the tool name in `titleSmall`, collapsible
  input/result sections; running tools show a small linear progress indicator,
  failed tools render the real error message in `Destructive` color.
- Timestamps and model tags use `bodySmall` / `labelMedium`.

### 7.2 ToolApprovalCard

- Shown whenever the engine emits a tool approval request; pinned above the
  composer so it is never scrolled away.
- Contents: command/path payload in monospace (`bodySmall`), the requesting
  tool name, and two buttons — "Approve" (primary) and "Deny" (outlined,
  `Destructive` text) — each ≥48dp tall, ≥8dp apart.
- While a request is pending, the composer stays enabled but submission is
  queued; nothing is auto-approved.

### 7.3 Diff Viewer (UnifiedDiffViewer)

- Monospace 13sp / 20sp line height, `TerminalBg` surface, horizontal scroll
  for long lines (no wrap inside diff lines).
- Added lines: `DiffAdded` text at full opacity on a 15% `DiffAdded`
  background; removed lines: `DiffRemoved` on 15% `DiffRemoved`; context lines
  keep `Foreground`.
- Hunk headers (`@@ … @@`) render in `labelMedium` with `ForegroundSubtlest`.
- Long diffs render through `LazyColumn` (virtualized) with stable keys; never
  build a non-lazy `Column` of thousands of lines.
- No syntax-highlighting dependency in Phase 1; keep coloring strictly
  add/remove semantics.
- Actions attached to the diff (Apply / Copy / Share) sit in a header row with
  ≥48dp targets and use the Sharesheet via the Kotlin host when sharing.

### 7.4 Composer (QodexComposer)

- Input field: `Input` background, `InputBorderFocused` border when focused,
  `bodyLarge` text, min height 48dp, grows to `heightIn(max = 160.dp)` then
  scrolls internally.
- Send button: 48dp, `Primary` colors; becomes a Stop control while a task is
  running (same position — the primary action never moves).
- Attachment chip row (future) uses `labelMedium` chips with `RADIUS_MD`.
- The whole composer sits above `imePadding()` + `navigationBarsPadding()`.

### 7.5 Model Settings rows (ModelSettingsScreen / ProviderModelEditor)

- Provider and model entries are ≥56dp rows: name in `titleMedium`, metadata
  (context window, capabilities) in `bodySmall` with `ForegroundSubtle`.
- Destructive row actions (delete model) use `Destructive` color and are
  separated from primary actions by ≥8dp.
- Secret fields (API keys) render with `PasswordVisualTransformation` and a
  visible show/hide toggle ≥48dp; values are never logged or echoed into the
  terminal stream.

### 7.6 TerminalStream (QodexTerminalSheet)

- Bottom sheet with `RADIUS_2XL` top corners, `TerminalBg` background,
  `TerminalFg` monospace 13sp text.
- Auto-scrolls only when already at the bottom; a "jump to live" affordance
  appears when the user scrolls up.
- Height is draggable between 40% and 90% of screen; dismiss via downward drag
  or a ≥48dp close button.

## 8. Iconography and motion

- Icons: Material Icons (filled/outlined set) plus the repo's `icons/` package.
  Every icon-only control carries `contentDescription` (or `null` when
  decorative and paired with a text label).
- Motion: durations 150–250ms with `FastOutSlowInEasing`; streaming and progress
  animations use linear easing. No page-transition theatrics — screens switch
  without shared-element choreography.
- Loading states: shimmer/skeleton only on cold boot; runtime operations show
  inline spinners or progress in place. Never fake progress.

## 9. Accessibility and anti-slop rules

1. **Functional copy.** Buttons say what they do: "Apply Diff", "Run Test",
   "Save Settings". No marketing headers, no ALL-CAPS shouting.
2. **Contrast.** Text passes WCAG AA (4.5:1) against its surface in both themes;
   `ForegroundSubtlest` is reserved for non-essential metadata only.
3. **Touch, focus, and IME:** every interactive element is reachable by
   keyboard/external input and respects the 48dp rule and IME insets.
4. **TalkBack:** meaningful `semantics { contentDescription }` on icon buttons,
   tool call blocks, and diff actions; chat timeline exposes each turn as one
   semantic unit with the role in its label.
5. **No AI slop:** no emoji in UI copy, no invented feature claims, no
   placeholder screens that pretend data loaded when it did not — error states
   show the real error and a retry path.

## 10. Do / Don't quick reference

| Do | Don't |
| --- | --- |
| Add tokens to `QodexTokens` / theme first | Inline `Color(0xFF...)` in screens |
| `MaterialTheme.typography` slots | Arbitrary `fontSize` at call sites |
| 48dp touch targets, 56dp list rows | 24dp icon buttons crammed in rows |
| `imePadding()` + insets top-down | `fitsSystemWindows`, hardcoded paddings for bars |
| Real error text in `Destructive` | Infinite spinners hiding failures |
| Desktop feature parity via composables | Copying `packages/ui` CSS/HTML into Compose |
| Both dark and light verified | Dark-only screenshots as proof |
