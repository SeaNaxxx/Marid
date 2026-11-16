package com.marid.app.ui

import androidx.compose.ui.graphics.Color

/**
 * Design tokens ported 1:1 from the ZCode desktop UI source of truth
 * (packages/ui/src/styles.css @theme + .dark block).
 *
 * The Tailwind v4 palette values below are the EXACT sRGB conversions of the
 * oklch definitions in tailwindcss/theme.css (computed via the standard
 * OKLab -> linear sRGB matrix, not approximated). Semantic mappings follow
 * the `.dark` block of styles.css, which is the theme the owner's screenshots
 * use. Alpha-mix tokens mirror `color-mix(in oklab, X n%, transparent)`.
 *
 * Typography scale (styles.css @theme):
 *   text-ui-xl 18sp, ui-lg 16sp, ui-base 14sp (default), ui-caption 13sp,
 *   ui-sm 12sp, ui-xs 10sp, ui-2xs 9sp; tracking-wf-label 0.09em.
 */
object MaridTokens {

    // ------------------------------------------------- Tailwind v4 palette
    val Neutral50 = Color(0xFFFAFAFA)
    val Neutral100 = Color(0xFFF5F5F5)
    val Neutral200 = Color(0xFFE5E5E5)
    val Neutral300 = Color(0xFFD4D4D4)
    val Neutral400 = Color(0xFFA1A1A1)
    val Neutral500 = Color(0xFF737373)
    val Neutral600 = Color(0xFF525252)
    val Neutral700 = Color(0xFF404040)
    val Neutral800 = Color(0xFF262626)
    val Neutral900 = Color(0xFF171717)
    val Neutral950 = Color(0xFF0A0A0A)

    val Sky50 = Color(0xFFF0F9FF)
    val Sky100 = Color(0xFFDFF2FE)
    val Sky200 = Color(0xFFB8E6FE)
    val Sky300 = Color(0xFF74D4FF)
    val Sky400 = Color(0xFF00BCFF)
    val Sky500 = Color(0xFF00A6F4)
    val Sky600 = Color(0xFF0084D1)
    val Sky700 = Color(0xFF0069A8)
    val Sky800 = Color(0xFF00598A)
    val Sky900 = Color(0xFF024A70)
    val Sky950 = Color(0xFF052F4A)

    val Green300 = Color(0xFF7BF1A8)
    val Green400 = Color(0xFF05DF72)
    val Green500 = Color(0xFF00C950)
    val Green600 = Color(0xFF00A63E)
    val Green700 = Color(0xFF008236)
    val Red300 = Color(0xFFFFA2A2)
    val Red400 = Color(0xFFFF6467)
    val Red500 = Color(0xFFFB2C36)
    val Red600 = Color(0xFFE7000B)
    val Yellow300 = Color(0xFFFFDF20)
    val Yellow400 = Color(0xFFFDC700)
    val Yellow500 = Color(0xFFF0B100)
    val Amber300 = Color(0xFFFFD230)
    val Amber400 = Color(0xFFFFB900)
    val Amber600 = Color(0xFFE17100)
    val Violet50 = Color(0xFFF5F3FF)
    val Violet300 = Color(0xFFC4B4FF)
    val Violet400 = Color(0xFFA684FF)
    val Violet600 = Color(0xFF7F22FE)
    val Violet700 = Color(0xFF7008E7)
    val Teal300 = Color(0xFF46ECD5)
    val Teal400 = Color(0xFF00D5BE)
    val Teal600 = Color(0xFF009689)
    val Fuchsia500 = Color(0xFFE12AFB)
    val Cyan500 = Color(0xFF00B8DB)
    val Cyan600 = Color(0xFF0092B8)
    val Blue300 = Color(0xFF8EC5FF)
    val Blue700 = Color(0xFF1447E6)
    val Slate300 = Color(0xFFCAD5E2)
    val Slate600 = Color(0xFF45556C)
    val Rose500 = Color(0xFFFF2056)
    val White = Color(0xFFFFFFFF)

    // ---------------------------------------------- dark semantic surfaces
    val Background = Neutral900
    val Sidebar = Neutral950
    val Header = Neutral900
    val Panel = Neutral900
    val Card = Neutral800
    val CardSelected = Neutral700
    val Popover = Neutral800
    val PopoverHeader = Neutral700
    val Menu = Neutral950
    val MenuHover = Neutral900
    val Tooltip = Neutral900
    val Toast = Neutral900
    val Tab = Neutral800
    val TabActive = Neutral950
    val Tag = Neutral700

    /** --color-surface: color-mix(white 5%, transparent) */
    val Surface = Color(0x0DFFFFFF)
    /** --color-surface-hover / --color-hover / --color-selected: white 10% */
    val SurfaceHover = Color(0x1AFFFFFF)
    val Selected = Color(0x1AFFFFFF)
    /** --color-user-message-bubble: white 8% */
    val UserMessageBubble = Color(0x14FFFFFF)

    // -------------------------------------------------------------- border
    /** --color-border: white 10% */
    val Border = Color(0x1AFFFFFF)
    /** --color-border-hover: white 30% */
    val BorderHover = Color(0x4DFFFFFF)

    // ------------------------------------------------------------ text
    val Foreground = Neutral200
    /** color-mix(neutral-200 60%, transparent) */
    val ForegroundSubtle = Color(0x99E5E5E5)
    /** color-mix(neutral-200 30%, transparent) */
    val ForegroundSubtlest = Color(0x4DE5E5E5)
    val ForegroundInverse = White

    // ------------------------------------------------------------ accents
    val Brand = Sky500
    val Primary = Neutral50
    val PrimaryForeground = Neutral950
    val Secondary = Neutral700
    val Destructive = Red500
    val Success = Green500
    val Warning = Yellow500
    val Input = Neutral800
    val InputFocused = Neutral950
    val InputBorderFocused = Brand
    val TerminalBg = Neutral950
    val TerminalFg = Neutral50

    // ------------------------------------------- trajectory / chat accents
    val TrajectoryUser = Color(0xFF60A5FA)
    val TrajectoryAssistant = Color(0xFF2DD4BF)
    val TrajectoryReasoning = Color(0xFFA78BFA)
    val TrajectoryToolCall = Color(0xFFF59E0B)
    val TrajectoryToolResult = Color(0xFF38BDF8)

    // -------------------------------------------------------- git statuses
    val GitModified = Amber400
    val GitAdded = Teal400
    val GitDeleted = Red400
    val GitRenamed = Sky400
    val DiffAdded = Green500
    val DiffRemoved = Red500

    // ------------------------------------------------------------ metrics
    // Tailwind spacing: 1 unit = 4dp (p-2 = 8dp, p-4 = 16dp ...).
    const val SPACING_UNIT = 4
    // shadcn radius scale used by the desktop UI (rounded-md/lg/xl).
    const val RADIUS_MD = 6
    const val RADIUS_LG = 8
    const val RADIUS_XL = 12
    const val RADIUS_2XL = 16
}
