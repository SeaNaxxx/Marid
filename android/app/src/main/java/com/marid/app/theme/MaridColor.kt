package com.marid.app.theme

import androidx.compose.ui.graphics.Color

/**
 * Marid color tokens — 1:1 port of the `.theme-zai-dark` design tokens from
 * `packages/ui/src/styles.css` (lines 720-884). NO INVENTED COLORS.
 *
 * The React/Tailwind desktop UI resolves these via Tailwind v4 `@theme`
 * variables; every hex/rgba below is copied verbatim from that source.
 */
object MaridColor {
    // Surfaces
    val Background = Color(0xFF161616) // --color-background: #161616
    val BackgroundWinAlt = Color(0xFF2B2B2B) // --color-background-win-alt: #2b2b2b
    val Header = Color(0xFF202020) // --color-header: #202020
    val Panel = Color(0xFF202020) // --color-panel: #202020
    val Sidebar = Color(0xFF161616) // --color-sidebar: #161616

    // Interactive surfaces (white alphas — rgba(255,255,255,n) in source)
    val Surface = Color(0x0DFFFFFF) // rgba(255,255,255,0.05)
    val SurfaceHover = Color(0x1AFFFFFF) // rgba(255,255,255,0.10)
    val Hover = Color(0x0DFFFFFF) // rgba(255,255,255,0.05)
    val Selected = Color(0x1AFFFFFF) // rgba(255,255,255,0.10)
    val UserMessageBubble = Color(0x14FFFFFF) // white 8% (.dark user-message-bubble)

    // Borders
    val Border = Color(0x1AFFFFFF) // rgba(255,255,255,0.10)
    val BorderHover = Color(0x26FFFFFF) // rgba(255,255,255,0.15)

    // Foreground
    val Foreground = Color(0xFFF5F5F5) // neutral-100 oklch(0.97 0 0)
    val ForegroundSubtle = Color(0x99D4D4D4) // neutral-300 @ 60% (color-mix)
    val ForegroundSubtlest = Color(0x4DD4D4D4) // neutral-300 @ 30% (--lucide-subtlest-opacity: 0.3)
    val ForegroundInverse = Color(0xFF000000) // --color-foreground-inverse: #000000
    val RemoteForegroundMuted = Color(0xFFB3B3B3) // --color-remote-foreground-muted: #b3b3b3

    // Brand / primary
    val Brand = Color(0xFFFFFFFF) // .theme-zai-dark --color-brand: #ffffff
    val Primary = Color(0xFFFFFFFF) // --color-primary: #ffffff
    val OnPrimary = Color(0xFF000000) // --color-primary-foreground: #000000
    val Accent = Color(0xFF001D3D) // --color-accent: #001d3d
    val Secondary = Color(0xFF363636) // --color-secondary: #363636

    // Cards / inputs / popovers
    val Card = Color(0xFF2B2B2B) // --color-card: #2b2b2b
    val Popover = Color(0xFF2B2B2B) // --color-popover: #2b2b2b
    val Input = Color(0xFF2B2B2B) // --color-input: #2b2b2b
    val MenuHover = Color(0xFF363636) // --color-menu-hover: #363636

    // Semantic
    val Success = Color(0xFF46BF72) // --color-success: #46bf72
    val Destructive = Color(0xFFFF5C5C) // --color-destructive: #ff5c5c
    val Warning = Color(0xFFFF8A30) // --color-warning: #ff8a30
    val IconBlue = Color(0xFF80BEFF) // --color-icon-blue = terminal-bright-blue #80beff
    val IconPurple = Color(0xFFA888F2) // --color-icon-purple = terminal-bright-magenta #a888f2
    val IdleTask = Color(0xFF7B5CE5) // --color-idle-task: #7b5ce5
    val DiffAdded = Color(0xFF46BF72) // --color-diff-added: green-500 (#46bf72 dark)
    val DiffRemoved = Color(0xFFFF5C5C) // --color-diff-removed: red-500 (#ff5c5c dark)

    // Composer focus (dark fallback uses sky-500 as input-border-focused/brand;
    // zai-dark brand is white — the focused border follows --color-brand)
    val InputBorderFocused = Brand

    // Terminal palette (packages/ui/src/terminal/terminalTheme.ts, zai-dark)
    object Terminal {
        val Bg = Color(0xFF161616)
        val Fg = Color(0xFFF5F5F5)
        val Cursor = Color(0xFFF8F8F8)
        val Selection = Color(0x474099FF) // rgba(64,153,255,0.28)
        val Black = Color(0xFF363636)
        val Red = Color(0xFFFF5C5C)
        val Green = Color(0xFF46BF72)
        val Yellow = Color(0xFFFF8A30)
        val Blue = Color(0xFF4099FF)
        val Magenta = Color(0xFF7B5CE5)
        val Cyan = Color(0xFF42C8C8)
        val White = Color(0xFFADADAD)
        val BrightBlack = Color(0xFF747474)
        val BrightRed = Color(0xFFFF9999)
        val BrightGreen = Color(0xFF87D9A4)
        val BrightYellow = Color(0xFFFFB26B)
        val BrightBlue = Color(0xFF80BEFF)
        val BrightMagenta = Color(0xFFA888F2)
        val BrightCyan = Color(0xFF8EE5E5)
        val BrightWhite = Color(0xFFF8F8F8)
    }
}

/**
 * Corner radius scale — Tailwind v4 defaults used throughout the desktop UI:
 * rounded-md 6px, rounded-lg 8px, rounded-xl 12px, rounded-2xl 16px.
 */
object MaridRadius {
    const val Md = 6f
    const val Lg = 8f
    const val Xl = 12f
    const val Xl2 = 16f
}

/**
 * Font size scale — packages/ui `@theme` (`--ui-font-size: 14px`):
 * text-ui-xs 10, text-ui-sm 12, text-ui-caption 13, text-ui-base 14,
 * text-ui-lg 16, text-ui-xl 18.
 */
object MaridTextSize {
    const val Xs = 10f
    const val Sm = 12f
    const val Caption = 13f
    const val Base = 14f
    const val Lg = 16f
    const val Xl = 18f
}
