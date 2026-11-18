package com.qodex.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.qodex.app.icons.QodexIcons

/**
 * Clean, developer-centric empty state for Qodex agent sessions:
 * Replaces generic AI-slop decorative radial gradients with practical prompt suggestions,
 * workspace context indicators, and terminal-grade monospace aesthetics.
 */
@Composable
fun WelcomeScreen(
    onNewChat: (() -> Unit)? = null,
    onQuickPrompt: ((String) -> Unit)? = null,
    workspacePath: String? = null,
    selectedModel: String? = null,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(QodexTokens.Background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 480.dp)
                .padding(horizontal = 24.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            // Header glyph: terminal prompt in dark container
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .background(QodexTokens.Surface, RoundedCornerShape(12.dp))
                    .border(1.dp, QodexTokens.Border, RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = TerminalPromptIcon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = QodexTokens.Brand,
                )
            }

            // Title & subtitle
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = "Qodex Agent",
                    color = QodexTokens.Foreground,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = (-0.3).sp,
                )
                Text(
                    text = "Autonomous Android Agent • In-Process Bionic Engine",
                    color = QodexTokens.ForegroundSubtle,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                )
            }

            // Workspace context badge
            if (!workspacePath.isNullOrBlank()) {
                Row(
                    modifier = Modifier
                        .background(QodexTokens.Surface, RoundedCornerShape(6.dp))
                        .border(1.dp, QodexTokens.Border, RoundedCornerShape(6.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        imageVector = QodexIcons.Folder,
                        contentDescription = null,
                        tint = QodexTokens.Brand,
                        modifier = Modifier.size(13.dp),
                    )
                    Text(
                        text = workspacePath.substringAfterLast('/'),
                        color = QodexTokens.Foreground,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Quick developer starter prompts
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                QuickPromptChip(
                    title = "Review workspace structure",
                    subtitle = "List project files, directories, and entry points",
                    icon = QodexIcons.Folder,
                    onClick = {
                        onQuickPrompt?.invoke("Examine the workspace layout and summarize the core source directories and architecture.")
                    },
                )
                QuickPromptChip(
                    title = "Check git status & diff",
                    subtitle = "Inspect modified files and uncommitted changes",
                    icon = QodexIcons.GitBranch,
                    onClick = {
                        onQuickPrompt?.invoke("Check the current git branch, status, and summary of changes.")
                    },
                )
                QuickPromptChip(
                    title = "Verify Bionic Node.js runtime",
                    subtitle = "Probe local Node environment and stdio IPC",
                    icon = QodexIcons.SquareTerminal,
                    onClick = {
                        onQuickPrompt?.invoke("Run an environment probe to check Node version, Bionic usr/bin, and system resources.")
                    },
                )
            }

            // Engine status footnote
            Text(
                text = if (!selectedModel.isNullOrBlank()) "Active model: $selectedModel" else "Ready for input",
                color = QodexTokens.ForegroundSubtlest,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun QuickPromptChip(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, QodexTokens.Border, RoundedCornerShape(8.dp))
            .background(QodexTokens.Surface, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(QodexTokens.SurfaceHover, RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = QodexTokens.Brand,
                modifier = Modifier.size(16.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = QodexTokens.Foreground,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            )
            Text(
                text = subtitle,
                color = QodexTokens.ForegroundSubtle,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = QodexIcons.ChevronRight,
            contentDescription = null,
            tint = QodexTokens.ForegroundSubtlest,
            modifier = Modifier.size(14.dp),
        )
    }
}

private val TerminalPromptIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "TerminalPrompt",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        // Angle '>'
        path(
            fill = null,
            stroke = SolidColor(QodexTokens.Brand),
            strokeLineWidth = 2.2f,
        ) {
            moveTo(4f, 6f)
            lineTo(12f, 12f)
            lineTo(4f, 18f)
        }
        // Underscore '_'
        path(
            fill = null,
            stroke = SolidColor(QodexTokens.Foreground),
            strokeLineWidth = 2.2f,
        ) {
            moveTo(14f, 18f)
            lineTo(20f, 18f)
        }
    }.build()
}
