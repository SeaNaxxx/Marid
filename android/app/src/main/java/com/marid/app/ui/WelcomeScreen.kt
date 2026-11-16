package com.marid.app.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 1:1 port of packages/ui/src/WelcomeScreen.tsx (login entry) — dark theme only.
 *
 * FILES PORTED
 *   - packages/ui/src/WelcomeScreen.tsx          (main / centered login card / footer)
 *   - packages/ui/src/openWorkspacePageThemeHero.tsx (ThemeHeroVisual backdrop, "dark" palette —
 *     the .dark block of styles.css is the theme MaridTokens documents, dark-only app)
 *   - packages/ui/src/components/ui/MaridAboutLogo.tsx (brand SVG → MaridAboutLogo2e ImageVector)
 *   - packages/ui/src/i18n/locales/en-US.ts      (exact strings: login.title, login.description,
 *     login.agreement.*, login.copyright)
 *
 * EXTRACTION NOTES (source class → Compose value; 1 spacing unit = 4dp)
 *   main "relative h-full min-h-dvh overflow-hidden bg-background text-foreground"
 *        → fillMaxSize + MaridTokens.Background (neutral-950? no: --color-background = neutral-900)
 *          + clipToBounds (= overflow-hidden).
 *   ThemeHeroVisual "absolute inset-0" (dark palette):
 *     panel linear-gradient(180deg,#060816 0%,#0b1333 38%,#12245a 72%,#173474 100%)
 *     before: radial-gradient(circle at 16% 20%, rgba(165,243,252,0.14), transparent 22%),
 *             radial-gradient(circle at 82% 14%, rgba(96,165,250,0.18), transparent 24%),
 *             radial-gradient(circle at 68% 84%, rgba(56,189,248,0.14), transparent 28%),
 *             linear-gradient(180deg, rgba(255,255,255,0.04), rgba(255,255,255,0))
 *     glowPrimary  "left-[-12%] top-[18%] h-[42rem] w-[42rem] rounded-full blur-3xl bg-cyan-300/22 mix-blend-screen"
 *     glowSecondary "right-[-18%] bottom-[-14%] h-[36rem] w-[36rem] rounded-full blur-3xl bg-blue-400/18 mix-blend-screen"
 *        → drawn in one drawBehind pass; CSS `circle` default ending shape (farthest-corner) is
 *          reproduced by measuring the farthest-corner distance per radial center; the two glow
 *          discs use BlendMode.Screen (= mix-blend-screen).
 *   content column "relative z-10 flex h-dvh flex-col items-center overflow-y-auto px-4 pb-6 pt-14"
 *        → Column, padding(horizontal 16dp, top 56dp, bottom 24dp); sm:px-6 is the >=640px
 *          desktop breakpoint, phone form factor keeps px-4.
 *   centered section "flex w-full max-w-sm shrink-0 flex-col gap-7 rounded-2xl bg-background
 *                     p-8 text-ui-base/relaxed shadow-md sm:p-10"
 *        → fillMaxWidth().widthIn(max = 384dp [max-w-sm]), RoundedCornerShape(16dp) [rounded-2xl],
 *          MaridTokens.Background, padding 32dp [p-8; sm:p-10 = desktop breakpoint, not used],
 *          shadow-md → elevation 4dp (approximation, see deviations).
 *   LoginPanelHeader "flex flex-col items-center gap-4 text-center"
 *        → Column(spacedBy 16dp).
 *   LoginPanelLogo "relative flex size-16 items-center justify-center rounded-2xl
 *                   bg-[linear-gradient(180deg,#000000_0%,#151718_100%)] text-[#ffffff]
 *                   shadow-lg/20 before:...border-[rgba(255,255,255,0.1)]"
 *        → 64dp Box, radius 16dp, vertical gradient #000000→#151718, white 10% 1dp border overlay,
 *          logo width 40dp [w-10] height 40*218/256dp (SVG viewBox 0 0 256 218, h-auto).
 *   h1 "text-ui-xl font-semibold tracking-tight" → 18sp [text-ui-xl = 14px+4px], SemiBold(600),
 *      letterSpacing -0.025em * 18sp = -0.45sp [tracking-tight].
 *   p "text-ui-base/relaxed text-foreground-subtle" → 14sp, line-height 1.625 → 22.75sp
 *      [/relaxed], MaridTokens.ForegroundSubtle.
 *   footer "flex shrink-0 flex-col items-center gap-2 text-center text-ui-xs text-foreground-subtle"
 *        → Column(spacedBy 8dp), 10sp [text-ui-xs = 14px-4px], MaridTokens.ForegroundSubtle.
 *   terms/privacy buttons "hover:text-foreground hover:underline underline-offset-2" → plain
 *      clickable spans (no hover on touch); real links (source opens LOGIN_TERMS_URL /
 *      LOGIN_PRIVACY_URL externally) via ACTION_VIEW.
 *
 * DEVIATIONS (all documented, none invented):
 *   1. The source idle-state action stack (OAuth "Login with Browser" / "Use API key" /
 *      region switch, container "min-h-36 space-y-6") is NOT ported: it is driven by
 *      useOAuth/OAuth providers which do not exist on Android; rendering it would produce
 *      dead buttons (forbidden). Only the logo/hero/headline typography + footer (the visual
 *      shell the task mandates) is ported; no prompt suggestions exist in the source.
 *   2. shadow-md / shadow-lg/20 → Compose elevation (4dp / 8dp). Tailwind shadows are
 *      two-layer colored box-shadows; Compose elevation is an approximation.
 *   3. `blur-3xl` (64px) glow discs are rendered as radial-gradient falloff discs of the same
 *      diameter/position/color (true gaussian blur is not available pre-API31).
 *   4. Font: Tailwind system-ui stack → platform default sans; en-US strings are hardcoded
 *      (no i18n runtime on Android yet), exactly as extracted from en-US.ts.
 *   5. DesktopOverlayWindowControls + native drag strip are desktop window chrome — not ported.
 *   6. `onNewChat` is kept per the mandated API; the source surface has no new-chat
 *      affordance to bind it to, so no visual element invokes it (nothing invented).
 */
@Composable
fun WelcomeScreen(onNewChat: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaridTokens.Background) // bg-background
            .clipToBounds(), // overflow-hidden
    ) {
        HeroBackdrop2e(modifier = Modifier.matchParentSize())

        // "relative z-10 flex h-dvh flex-col items-center overflow-y-auto px-4 pb-6 pt-14"
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 16.dp, top = 56.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // "flex w-full flex-1 shrink-0 items-center justify-center py-6"
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                // "section flex w-full max-w-sm shrink-0 flex-col gap-7 rounded-2xl bg-background p-8 ... shadow-md"
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 384.dp) // max-w-sm = 24rem
                        .shadow(elevation = 4.dp, shape = RoundedCornerShape(16.dp), clip = false)
                        .background(MaridTokens.Background, RoundedCornerShape(16.dp))
                        .padding(32.dp), // p-8
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // LoginPanelHeader: "flex flex-col items-center gap-4 text-center"
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        LoginPanelLogo2e()
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            // "flex flex-col items-center gap-1 text-center"
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = "Welcome to Marid", // login.title (en-US)
                                color = MaridTokens.Foreground,
                                fontSize = 18.sp, // text-ui-xl
                                fontWeight = FontWeight.SemiBold, // font-semibold
                                letterSpacing = (-0.45f).sp, // tracking-tight = -0.025em * 18sp
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                text = "Log in to Z.AI to start using Marid", // login.description (en-US)
                                color = MaridTokens.ForegroundSubtle,
                                fontSize = 14.sp, // text-ui-base
                                lineHeight = 22.75f.sp, // /relaxed = 1.625 * 14sp
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                    // DEVIATION 1: OAuth/API-key action stack ("min-h-36 space-y-6") not ported.
                }
            }

            // footer: "pointer-events-none flex shrink-0 flex-col items-center gap-2 text-center text-ui-xs text-foreground-subtle"
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val agreement = buildAnnotatedString {
                    append("By signing in, you agree to our ") // login.agreement.prefix
                    pushStringAnnotation(tag = "url", annotation = "https://marid.z.ai/terms")
                    append("Terms of Service") // login.agreement.terms
                    pop()
                    append(" and ") // login.agreement.conjunction
                    pushStringAnnotation(tag = "url", annotation = "https://marid.z.ai/privacy")
                    append("Privacy Policy") // login.agreement.privacy
                    pop()
                }
                ClickableText(
                    text = agreement,
                    style = TextStyle(
                        color = MaridTokens.ForegroundSubtle,
                        fontSize = 10.sp, // text-ui-xs
                        textDecoration = TextDecoration.None, // hover:underline only — no hover on touch
                        textAlign = TextAlign.Center,
                    ),
                    onClick = { offset ->
                        agreement.getStringAnnotations("url", offset, offset)
                            .firstOrNull()
                            ?.let { annotation ->
                                runCatching {
                                    context.startActivity(
                                        Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item))
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                    )
                                }
                            }
                    },
                )
                Text(
                    text = "© 2026 Marid All rights reserved.", // login.copyright (en-US)
                    color = MaridTokens.ForegroundSubtle,
                    fontSize = 10.sp, // text-ui-xs
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

/**
 * LoginPanelLogo (WelcomeScreen.tsx):
 * "relative flex size-16 items-center justify-center rounded-2xl
 *  bg-[linear-gradient(180deg,#000000_0%,#151718_100%)] text-[#ffffff] shadow-lg/20
 *  before:absolute before:inset-0 before:rounded-2xl before:border
 *  before:border-[rgba(255,255,255,0.1)]"
 */
@Composable
private fun LoginPanelLogo2e() {
    Box(
        modifier = Modifier
            .size(64.dp) // size-16
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(16.dp), clip = false) // shadow-lg/20 approx
            .background(
                brush = Brush.verticalGradient(listOf(Color(0xFF000000), Color(0xFF151718))),
                shape = RoundedCornerShape(16.dp), // rounded-2xl
            ),
        contentAlignment = Alignment.Center,
    ) {
        // before: inset-0 border rgba(255,255,255,0.1), same 16dp radius
        Box(
            modifier = Modifier
                .matchParentSize()
                .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(16.dp)),
        )
        // MaridAboutLogo "h-auto w-10" — viewBox 256x218 → height = 40 * 218/256
        Icon(
            imageVector = MaridAboutLogo2e,
            contentDescription = null, // source: aria-hidden="true" (tile carries role="img" aria-label="Marid")
            modifier = Modifier.size(width = 40.dp, height = 34.0625.dp),
            tint = Color(0xFFFFFFFF), // text-[#ffffff]
        )
    }
}

/**
 * ThemeHeroVisual (openWorkspacePageThemeHero.tsx), "dark" palette, drawn in one pass:
 *   panel linear-gradient + before: 3 radial gradients + white top fade
 *   + glowPrimary (cyan-300/22, screen) + glowSecondary (blue-400/18, screen).
 */
@Composable
private fun HeroBackdrop2e(modifier: Modifier = Modifier) {
    Spacer(
        modifier = modifier.drawBehind {
            // panel: bg-[linear-gradient(180deg,#060816_0%,#0b1333_38%,#12245a_72%,#173474_100%)]
            drawRect(
                Brush.verticalGradient(
                    colorStops = arrayOf(
                        0f to Color(0xFF060816),
                        0.38f to Color(0xFF0B1333),
                        0.72f to Color(0xFF12245A),
                        1f to Color(0xFF173474),
                    ),
                ),
            )

            // before: radial-gradient(circle at X% Y%, COLOR, transparent S%) — CSS `circle`
            // defaults to the farthest-corner ending shape, so the stop percentage scales
            // against the distance from the center to the farthest box corner.
            fun farthestCorner(center: Offset): Float {
                val corners = listOf(
                    Offset(0f, 0f), Offset(size.width, 0f),
                    Offset(0f, size.height), Offset(size.width, size.height),
                )
                return corners.maxOf { (it - center).getDistance() }
            }
            fun drawRadialStop(center: Offset, stop: Float, color: Color) {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(color, color.copy(alpha = 0f)),
                        center = center,
                        radius = farthestCorner(center) * stop,
                    ),
                )
            }
            drawRadialStop(
                center = Offset(size.width * 0.16f, size.height * 0.20f),
                stop = 0.22f,
                color = Color(0xFFA5F3FC).copy(alpha = 0.14f), // rgba(165,243,252,0.14)
            )
            drawRadialStop(
                center = Offset(size.width * 0.82f, size.height * 0.14f),
                stop = 0.24f,
                color = Color(0xFF60A5FA).copy(alpha = 0.18f), // rgba(96,165,250,0.18)
            )
            drawRadialStop(
                center = Offset(size.width * 0.68f, size.height * 0.84f),
                stop = 0.28f,
                color = Color(0xFF38BDF8).copy(alpha = 0.14f), // rgba(56,189,248,0.14)
            )
            // before: linear-gradient(180deg, rgba(255,255,255,0.04), rgba(255,255,255,0))
            drawRect(Brush.verticalGradient(listOf(Color(0x0AFFFFFF), Color(0x00FFFFFF))))

            // glowPrimary: "left-[-12%] top-[18%] h-[42rem] w-[42rem] rounded-full blur-3xl
            //               bg-cyan-300/22 mix-blend-screen" (42rem = 672dp, radius 336dp)
            val radius1 = 336.dp.toPx()
            val center1 = Offset(
                size.width * -0.12f + radius1,
                size.height * 0.18f + radius1,
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF67E8F9).copy(alpha = 0.22f), Color(0x0067E8F9)),
                    center = center1,
                    radius = radius1,
                ),
                blendMode = BlendMode.Screen, // mix-blend-screen
            )

            // glowSecondary: "right-[-18%] bottom-[-14%] h-[36rem] w-[36rem] rounded-full
            //                 blur-3xl bg-blue-400/18 mix-blend-screen" (36rem = 576dp)
            val radius2 = 288.dp.toPx()
            val center2 = Offset(
                size.width * 1.18f - radius2,
                size.height * 1.14f - radius2,
            )
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF60A5FA).copy(alpha = 0.18f), Color(0x0060A5FA)),
                    center = center2,
                    radius = radius2,
                ),
                blendMode = BlendMode.Screen,
            )
        },
    )
}

/**
 * MaridAboutLogo (components/ui/MaridAboutLogo.tsx) — exact brand SVG geometry,
 * viewBox 0 0 256 218, fill currentColor (rendered as white inside the logo tile).
 */
private val MaridAboutLogo2e: ImageVector by lazy {
    ImageVector.Builder(
        name = "Marid2e.AboutLogo",
        defaultWidth = 40.dp,
        defaultHeight = 34.0625.dp, // 40 * 218 / 256
        viewportWidth = 256f,
        viewportHeight = 218f,
    ).apply {
        path(fill = SolidColor(Color(0xFFFFFFFF))) { // text-current → tile uses text-[#ffffff]
            // M134.4 0.130152L116.48 25.6022C113.665 29.5699 109.054 32.0019 104.064 32.0019H6.3999V0C6.3999 0.130149 134.4 0.130152 134.4 0.130152Z
            moveTo(134.4f, 0.130152f)
            lineTo(116.48f, 25.6022f)
            curveTo(113.665f, 29.5699f, 109.054f, 32.0019f, 104.064f, 32.0019f)
            lineTo(6.3999f, 32.0019f)
            lineTo(6.3999f, 0f)
            curveTo(6.3999f, 0.130149f, 134.4f, 0.130152f, 134.4f, 0.130152f)
            close()
        }
        path(fill = SolidColor(Color(0xFFFFFFFF))) {
            // M256 0.130127L102.401 217.732H0L153.599 0.130127H256Z
            moveTo(256f, 0.130127f)
            lineTo(102.401f, 217.732f)
            lineTo(0f, 217.732f)
            lineTo(153.599f, 0.130127f)
            close()
        }
        path(fill = SolidColor(Color(0xFFFFFFFF))) {
            // M121.601 217.732L139.65 192.134C142.465 188.166 147.076 185.734 152.067 185.734H249.604V217.736H121.601V217.732Z
            moveTo(121.601f, 217.732f)
            lineTo(139.65f, 192.134f)
            curveTo(142.465f, 188.166f, 147.076f, 185.734f, 152.067f, 185.734f)
            lineTo(249.604f, 185.734f)
            lineTo(249.604f, 217.736f)
            lineTo(121.601f, 217.736f)
            close()
        }
    }.build()
}
