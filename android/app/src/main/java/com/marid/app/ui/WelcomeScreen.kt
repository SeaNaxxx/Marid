package com.marid.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// onNewChat is unused here — kept for caller API symmetry; this screen has no new-chat affordance.
@Composable
fun WelcomeScreen(onNewChat: (() -> Unit)? = null, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaridTokens.Background)
            .clipToBounds(),
    ) {
        HeroBackdrop2e(modifier = Modifier.matchParentSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 16.dp, end = 16.dp, top = 56.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 384.dp)
                        .shadow(elevation = 4.dp, shape = RoundedCornerShape(16.dp), clip = false)
                        .background(MaridTokens.Background, RoundedCornerShape(16.dp))
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        LoginPanelLogo2e()
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text(
                                text = "Welcome to Marid",
                                color = MaridTokens.Foreground,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = (-0.45f).sp,
                                textAlign = TextAlign.Center,
                            )
                            Text(
                                text = "Personal Autonomous Android Agent",
                                color = MaridTokens.ForegroundSubtle,
                                fontSize = 14.sp,
                                lineHeight = 22.75f.sp,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "© 2026 Marid All rights reserved.",
                    color = MaridTokens.ForegroundSubtle,
                    fontSize = 10.sp,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun LoginPanelLogo2e() {
    Box(
        modifier = Modifier
            .size(64.dp)
            .shadow(elevation = 8.dp, shape = RoundedCornerShape(16.dp), clip = false)
            .background(
                brush = Brush.verticalGradient(listOf(Color(0xFF000000), Color(0xFF151718))),
                shape = RoundedCornerShape(16.dp),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(16.dp)),
        )
        Icon(
            imageVector = MaridMarkM2e,
            contentDescription = null, // decorative; the tile carries the accessible label
            modifier = Modifier.size(width = 40.dp, height = 29.2.dp),
            tint = Color(0xFFFFFFFF),
        )
    }
}

/** Glow discs approximate blur-3xl with radial-gradient falloff (no gaussian blur pre-API31). */
@Composable
private fun HeroBackdrop2e(modifier: Modifier = Modifier) {
    Spacer(
        modifier = modifier.drawBehind {
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

            // CSS radial `circle` ends at the farthest corner, so radius = distance to farthest corner * stop.
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
                color = Color(0xFFA5F3FC).copy(alpha = 0.14f),
            )
            drawRadialStop(
                center = Offset(size.width * 0.82f, size.height * 0.14f),
                stop = 0.24f,
                color = Color(0xFF60A5FA).copy(alpha = 0.18f),
            )
            drawRadialStop(
                center = Offset(size.width * 0.68f, size.height * 0.84f),
                stop = 0.28f,
                color = Color(0xFF38BDF8).copy(alpha = 0.14f),
            )
            drawRect(Brush.verticalGradient(listOf(Color(0x0AFFFFFF), Color(0x00FFFFFF))))

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
                blendMode = BlendMode.Screen,
            )

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

private val MaridMarkM2e: ImageVector by lazy {
    ImageVector.Builder(
        name = "Marid2e.MarkM",
        defaultWidth = 40.dp,
        defaultHeight = 29.2.dp,
        viewportWidth = 100f,
        viewportHeight = 73f,
    ).apply {
        path(fill = SolidColor(Color(0xFFFFFFFF))) {
            moveTo(0f, 0f)
            lineTo(20f, 0f)
            lineTo(20f, 73f)
            lineTo(0f, 73f)
            close()
        }
        path(fill = SolidColor(Color(0xFFFFFFFF))) {
            moveTo(80f, 0f)
            lineTo(100f, 0f)
            lineTo(100f, 73f)
            lineTo(80f, 73f)
            close()
        }
        path(fill = SolidColor(Color(0xFFFFFFFF))) {
            moveTo(20f, 0f)
            lineTo(38f, 0f)
            lineTo(50f, 28.4f)
            lineTo(50f, 45f)
            close()
        }
        path(fill = SolidColor(Color(0xFFFFFFFF))) {
            moveTo(80f, 0f)
            lineTo(62f, 0f)
            lineTo(50f, 28.4f)
            lineTo(50f, 45f)
            close()
        }
    }.build()
}
