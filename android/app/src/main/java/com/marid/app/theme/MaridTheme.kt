package com.marid.app.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * Extended token palette (non-Material colors used by the ZCode design:
 * subtle text tiers, selected/hover surfaces, tool colors, terminal colors…).
 */
data class MaridTokens(
    val background: Color,
    val header: Color,
    val sidebar: Color,
    val surface: Color,
    val surfaceHover: Color,
    val hover: Color,
    val selected: Color,
    val border: Color,
    val borderHover: Color,
    val foreground: Color,
    val foregroundSubtle: Color,
    val foregroundSubtlest: Color,
    val foregroundInverse: Color,
    val brand: Color,
    val accent: Color,
    val card: Color,
    val input: Color,
    val inputFocused: Color,
    val popover: Color,
    val menuHover: Color,
    val success: Color,
    val destructive: Color,
    val warning: Color,
    val iconBlue: Color,
    val iconPurple: Color,
    val diffAdded: Color,
    val diffRemoved: Color,
    val userMessageBubble: Color,
    val secondary: Color,
    val terminal: MaridColor.Terminal,
)

val LocalMaridTokens = staticCompositionLocalOf {
    MaridTokens(
        background = MaridColor.Background,
        header = MaridColor.Header,
        sidebar = MaridColor.Sidebar,
        surface = MaridColor.Surface,
        surfaceHover = MaridColor.SurfaceHover,
        hover = MaridColor.Hover,
        selected = MaridColor.Selected,
        border = MaridColor.Border,
        borderHover = MaridColor.BorderHover,
        foreground = MaridColor.Foreground,
        foregroundSubtle = MaridColor.ForegroundSubtle,
        foregroundSubtlest = MaridColor.ForegroundSubtlest,
        foregroundInverse = MaridColor.ForegroundInverse,
        brand = MaridColor.Brand,
        accent = MaridColor.Accent,
        card = MaridColor.Card,
        input = MaridColor.Input,
        inputFocused = MaridColor.Input,
        popover = MaridColor.Popover,
        menuHover = MaridColor.MenuHover,
        success = MaridColor.Success,
        destructive = MaridColor.Destructive,
        warning = MaridColor.Warning,
        iconBlue = MaridColor.IconBlue,
        iconPurple = MaridColor.IconPurple,
        diffAdded = MaridColor.DiffAdded,
        diffRemoved = MaridColor.DiffRemoved,
        userMessageBubble = MaridColor.UserMessageBubble,
        secondary = MaridColor.Secondary,
        terminal = MaridColor.Terminal,
    )
}

/**
 * Material3 dark scheme mapped onto the zai-dark tokens (Material surfaces are
 * used by sheets/menus so they inherit the exact desktop look).
 */
private val ZaiDarkScheme = darkColorScheme(
    primary = MaridColor.Primary,
    onPrimary = MaridColor.OnPrimary,
    secondary = MaridColor.Secondary,
    onSecondary = MaridColor.Foreground,
    background = MaridColor.Background,
    onBackground = MaridColor.Foreground,
    surface = MaridColor.Background,
    onSurface = MaridColor.Foreground,
    surfaceVariant = MaridColor.Card,
    onSurfaceVariant = MaridColor.ForegroundSubtle,
    surfaceContainer = MaridColor.Header,
    surfaceContainerLow = MaridColor.Background,
    surfaceContainerLowest = MaridColor.Background,
    surfaceContainerHigh = MaridColor.Card,
    surfaceContainerHighest = MaridColor.Card,
    outline = MaridColor.Border,
    outlineVariant = MaridColor.Border,
    error = MaridColor.Destructive,
    onError = Color.White,
    scrim = Color.Black,
)

/** ZCode text scale — `--ui-font-size: 14px` based (see MaridTextSize). */
private val MaridTypography = Typography(
    displaySmall = TextStyle(fontSize = MaridTextSize.Xl.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = MaridTextSize.Base.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = TextStyle(fontSize = MaridTextSize.Base.sp, fontWeight = FontWeight.Medium),
    bodyMedium = TextStyle(
        fontSize = MaridTextSize.Base.sp,
        lineHeight = (MaridTextSize.Base * 1.75f).sp, // leading-[1.75] markdown body
        fontWeight = FontWeight.Normal,
    ),
    bodySmall = TextStyle(fontSize = MaridTextSize.Sm.sp),
    labelMedium = TextStyle(fontSize = MaridTextSize.Sm.sp, fontWeight = FontWeight.Medium),
    labelSmall = TextStyle(fontSize = MaridTextSize.Xs.sp),
)

object MaridThemeTokens {
    val current: MaridTokens
        @Composable get() = LocalMaridTokens.current
}

/** Convenience accessors mirroring Tailwind utility names used in source. */
@Composable
fun maridTokens(): MaridTokens = LocalMaridTokens.current

@Composable
fun MaridTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalMaridTokens provides defaultTokens()) {
        MaterialTheme(
            colorScheme = ZaiDarkScheme,
            typography = MaridTypography,
            content = content,
        )
    }
}

private fun defaultTokens(): MaridTokens = MaridTokens(
    background = MaridColor.Background,
    header = MaridColor.Header,
    sidebar = MaridColor.Sidebar,
    surface = MaridColor.Surface,
    surfaceHover = MaridColor.SurfaceHover,
    hover = MaridColor.Hover,
    selected = MaridColor.Selected,
    border = MaridColor.Border,
    borderHover = MaridColor.BorderHover,
    foreground = MaridColor.Foreground,
    foregroundSubtle = MaridColor.ForegroundSubtle,
    foregroundSubtlest = MaridColor.ForegroundSubtlest,
    foregroundInverse = MaridColor.ForegroundInverse,
    brand = MaridColor.Brand,
    accent = MaridColor.Accent,
    card = MaridColor.Card,
    input = MaridColor.Input,
    inputFocused = MaridColor.Input,
    popover = MaridColor.Popover,
    menuHover = MaridColor.MenuHover,
    success = MaridColor.Success,
    destructive = MaridColor.Destructive,
    warning = MaridColor.Warning,
    iconBlue = MaridColor.IconBlue,
    iconPurple = MaridColor.IconPurple,
    diffAdded = MaridColor.DiffAdded,
    diffRemoved = MaridColor.DiffRemoved,
    userMessageBubble = MaridColor.UserMessageBubble,
    secondary = MaridColor.Secondary,
    terminal = MaridColor.Terminal,
)

/** Monospace text style — `--font-mono` stack (system monospace on Android). */
val monoStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontSize = MaridTextSize.Caption.sp, // xterm.js default fontSize: 13
    lineHeight = 20.sp,
)
