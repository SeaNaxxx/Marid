package com.marid.app.icons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Exact Lucide geometry so glyphs match the desktop UI. */
object MaridIcons {
    public val Menu: ImageVector by lazy {
        ImageVector.Builder(
            name = "Marid.Menu",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(4.0f, 5.0f)
            lineTo(20.0f, 5.0f)
        }
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(4.0f, 12.0f)
            lineTo(20.0f, 12.0f)
        }
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(4.0f, 19.0f)
            lineTo(20.0f, 19.0f)
        }
        }.build()
    }

    public val CircleHelp: ImageVector by lazy {
        ImageVector.Builder(
            name = "Marid.CircleHelp",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(22.0f, 12.0f)
            arcTo(10.0f, 10.0f, 0.0f, false, true, 2.0f, 12.0f)
            arcTo(10.0f, 10.0f, 0.0f, false, true, 22.0f, 12.0f)
            close()
        }
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(9.0f, 9.5f)
            arcTo(3.0f, 2.5f, 0.0f, false, true, 15.0f, 9.5f)
            curveTo(15.0f, 11.5f, 13.0f, 11.5f, 12.0f, 13.0f)
        }
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(12.0f, 17.0f)
            lineTo(12.01f, 17.0f)
        }
        }.build()
    }

    public val SquareTerminal: ImageVector by lazy {
        ImageVector.Builder(
            name = "Marid.SquareTerminal",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(7.0f, 11.0f)
            lineTo(9.0f, 9.0f)
            lineTo(7.0f, 7.0f)
        }
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(11.0f, 13.0f)
            lineTo(15.0f, 13.0f)
        }
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(5.0f, 3.0f)
            lineTo(19.0f, 3.0f)
            arcTo(2.0f, 2.0f, 0.0f, false, true, 21.0f, 5.0f)
            lineTo(21.0f, 19.0f)
            arcTo(2.0f, 2.0f, 0.0f, false, true, 19.0f, 21.0f)
            lineTo(5.0f, 21.0f)
            arcTo(2.0f, 2.0f, 0.0f, false, true, 3.0f, 19.0f)
            lineTo(3.0f, 5.0f)
            arcTo(2.0f, 2.0f, 0.0f, false, true, 5.0f, 3.0f)
            close()
        }
        }.build()
    }

    public val PanelRightOpen: ImageVector by lazy {
        ImageVector.Builder(
            name = "Marid.PanelRightOpen",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(5.0f, 3.0f)
            lineTo(19.0f, 3.0f)
            arcTo(2.0f, 2.0f, 0.0f, false, true, 21.0f, 5.0f)
            lineTo(21.0f, 19.0f)
            arcTo(2.0f, 2.0f, 0.0f, false, true, 19.0f, 21.0f)
            lineTo(5.0f, 21.0f)
            arcTo(2.0f, 2.0f, 0.0f, false, true, 3.0f, 19.0f)
            lineTo(3.0f, 5.0f)
            arcTo(2.0f, 2.0f, 0.0f, false, true, 5.0f, 3.0f)
            close()
        }
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(15.0f, 3.0f)
            lineTo(15.0f, 21.0f)
        }
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(10.0f, 15.0f)
            lineTo(7.0f, 12.0f)
            lineTo(10.0f, 9.0f)
        }
        }.build()
    }

    public val PanelRightClose: ImageVector by lazy {
        ImageVector.Builder(
            name = "Marid.PanelRightClose",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(5.0f, 3.0f)
            lineTo(19.0f, 3.0f)
            arcTo(2.0f, 2.0f, 0.0f, false, true, 21.0f, 5.0f)
            lineTo(21.0f, 19.0f)
            arcTo(2.0f, 2.0f, 0.0f, false, true, 19.0f, 21.0f)
            lineTo(5.0f, 21.0f)
            arcTo(2.0f, 2.0f, 0.0f, false, true, 3.0f, 19.0f)
            lineTo(3.0f, 5.0f)
            arcTo(2.0f, 2.0f, 0.0f, false, true, 5.0f, 3.0f)
            close()
        }
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(15.0f, 3.0f)
            lineTo(15.0f, 21.0f)
        }
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(8.0f, 9.0f)
            lineTo(11.0f, 12.0f)
            lineTo(8.0f, 15.0f)
        }
        }.build()
    }

    public val Plus: ImageVector by lazy {
        ImageVector.Builder(
            name = "Marid.Plus",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(5.0f, 12.0f)
            lineTo(19.0f, 12.0f)
        }
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(12.0f, 5.0f)
            lineTo(12.0f, 19.0f)
        }
        }.build()
    }

    public val ChevronDown: ImageVector by lazy {
        ImageVector.Builder(
            name = "Marid.ChevronDown",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(6.0f, 9.0f)
            lineTo(12.0f, 15.0f)
            lineTo(18.0f, 9.0f)
        }
        }.build()
    }

    public val ChevronRight: ImageVector by lazy {
        ImageVector.Builder(
            name = "Marid.ChevronRight",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(9.0f, 18.0f)
            lineTo(15.0f, 12.0f)
            lineTo(9.0f, 6.0f)
        }
        }.build()
    }

    public val ChevronLeft: ImageVector by lazy {
        ImageVector.Builder(
            name = "Marid.ChevronLeft",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(9.0f, 18.0f)
            lineTo(15.0f, 12.0f)
            lineTo(9.0f, 6.0f)
        }
        }.build()
    }

    public val Hand: ImageVector by lazy {
        ImageVector.Builder(
            name = "Marid.Hand",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(18.0f, 11.0f)
            lineTo(18.0f, 6.0f)
            arcTo(2.0f, 2.0f, 0.0f, false, false, 16.0f, 4.0f)
            arcTo(2.0f, 2.0f, 0.0f, false, false, 14.0f, 6.0f)
        }
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(14.0f, 10.0f)
            lineTo(14.0f, 4.0f)
            arcTo(2.0f, 2.0f, 0.0f, false, false, 12.0f, 2.0f)
            arcTo(2.0f, 2.0f, 0.0f, false, false, 10.0f, 4.0f)
            lineTo(10.0f, 6.0f)
        }
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(10.0f, 10.5f)
            lineTo(10.0f, 6.0f)
            arcTo(2.0f, 2.0f, 0.0f, false, false, 8.0f, 4.0f)
            arcTo(2.0f, 2.0f, 0.0f, false, false, 6.0f, 6.0f)
            lineTo(6.0f, 14.0f)
        }
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(18.0f, 8.0f)
            arcTo(2.0f, 2.0f, 0.0f, true, true, 22.0f, 8.0f)
            lineTo(22.0f, 14.0f)
            arcTo(8.0f, 8.0f, 0.0f, false, true, 14.0f, 22.0f)
            lineTo(12.0f, 22.0f)
            curveTo(9.2f, 22.0f, 7.5f, 21.14f, 6.01f, 19.66f)
            lineTo(2.41f, 16.06f)
            arcTo(2.0f, 2.0f, 0.0f, false, true, 5.24f, 13.24f)
            lineTo(7.0f, 15.0f)
        }
        }.build()
    }

    public val Package: ImageVector by lazy {
        ImageVector.Builder(
            name = "Marid.Package",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(11.0f, 21.73f)
            arcTo(2.0f, 2.0f, 0.0f, false, false, 13.0f, 21.73f)
            lineTo(20.0f, 17.73f)
            arcTo(2.0f, 2.0f, 0.0f, false, false, 21.0f, 16.0f)
            lineTo(21.0f, 8.0f)
            arcTo(2.0f, 2.0f, 0.0f, false, false, 20.0f, 6.27f)
            lineTo(13.0f, 2.27f)
            arcTo(2.0f, 2.0f, 0.0f, false, false, 11.0f, 2.27f)
            lineTo(4.0f, 6.27f)
            arcTo(2.0f, 2.0f, 0.0f, false, false, 3.0f, 8.0f)
            lineTo(3.0f, 16.0f)
            arcTo(2.0f, 2.0f, 0.0f, false, false, 4.0f, 17.73f)
            close()
        }
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(12.0f, 22.0f)
            lineTo(12.0f, 12.0f)
        }
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(3.29f, 7.0f)
            lineTo(12.0f, 12.0f)
            lineTo(20.71f, 7.0f)
        }
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(7.5f, 4.27f)
            lineTo(16.5f, 9.42f)
        }
        }.build()
    }

    public val ArrowUp: ImageVector by lazy {
        ImageVector.Builder(
            name = "Marid.ArrowUp",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(5.0f, 12.0f)
            lineTo(12.0f, 5.0f)
            lineTo(19.0f, 12.0f)
        }
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(12.0f, 19.0f)
            lineTo(12.0f, 5.0f)
        }
        }.build()
    }

    public val MessageCirclePlus: ImageVector by lazy {
        ImageVector.Builder(
            name = "Marid.MessageCirclePlus",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(2.992f, 16.342f)
            arcTo(2.0f, 2.0f, 0.0f, false, true, 3.086f, 17.509f)
            lineTo(2.021f, 20.799f)
            arcTo(1.0f, 1.0f, 0.0f, false, false, 3.257f, 21.967f)
            lineTo(6.67f, 20.969f)
            arcTo(2.0f, 2.0f, 0.0f, false, true, 7.769f, 21.061f)
            arcTo(10.0f, 10.0f, 0.0f, true, false, 2.992f, 16.342f)
        }
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(8.0f, 12.0f)
            lineTo(16.0f, 12.0f)
        }
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(12.0f, 8.0f)
            lineTo(12.0f, 16.0f)
        }
        }.build()
    }

    public val CalendarClock: ImageVector by lazy {
        ImageVector.Builder(
            name = "Marid.CalendarClock",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(16.0f, 14.0f)
            lineTo(16.0f, 16.2f)
            lineTo(17.6f, 17.2f)
        }
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(16.0f, 2.0f)
            lineTo(16.0f, 5.0f)
        }
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(21.0f, 7.338f)
            lineTo(21.0f, 5.0f)
            arcTo(2.0f, 2.0f, 0.0f, false, false, 19.0f, 3.0f)
            lineTo(5.0f, 3.0f)
            arcTo(2.0f, 2.0f, 0.0f, false, false, 3.0f, 5.0f)
            lineTo(3.0f, 19.0f)
            arcTo(2.0f, 2.0f, 0.0f, false, false, 5.0f, 21.0f)
            lineTo(7.338f, 21.0f)
        }
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(3.0f, 9.0f)
            lineTo(8.859f, 9.0f)
        }
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(8.0f, 2.0f)
            lineTo(8.0f, 5.0f)
        }
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(22.0f, 16.0f)
            arcTo(6.0f, 6.0f, 0.0f, false, true, 10.0f, 16.0f)
            arcTo(6.0f, 6.0f, 0.0f, false, true, 22.0f, 16.0f)
            close()
        }
        }.build()
    }

    public val Blocks: ImageVector by lazy {
        ImageVector.Builder(
            name = "Marid.Blocks",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(10.0f, 22.0f)
            lineTo(10.0f, 7.0f)
            arcTo(1.0f, 1.0f, 0.0f, false, false, 9.0f, 6.0f)
            lineTo(4.0f, 6.0f)
            arcTo(2.0f, 2.0f, 0.0f, false, false, 2.0f, 8.0f)
            lineTo(2.0f, 20.0f)
            arcTo(2.0f, 2.0f, 0.0f, false, false, 4.0f, 22.0f)
            lineTo(16.0f, 22.0f)
            arcTo(2.0f, 2.0f, 0.0f, false, false, 18.0f, 20.0f)
            lineTo(18.0f, 15.0f)
            arcTo(1.0f, 1.0f, 0.0f, false, false, 17.0f, 14.0f)
            lineTo(2.0f, 14.0f)
        }
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(15.0f, 2.0f)
            lineTo(21.0f, 2.0f)
            arcTo(1.0f, 1.0f, 0.0f, false, true, 22.0f, 3.0f)
            lineTo(22.0f, 9.0f)
            arcTo(1.0f, 1.0f, 0.0f, false, true, 21.0f, 10.0f)
            lineTo(15.0f, 10.0f)
            arcTo(1.0f, 1.0f, 0.0f, false, true, 14.0f, 9.0f)
            lineTo(14.0f, 3.0f)
            arcTo(1.0f, 1.0f, 0.0f, false, true, 15.0f, 2.0f)
            close()
        }
        }.build()
    }

    public val Search: ImageVector by lazy {
        ImageVector.Builder(
            name = "Marid.Search",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(21.0f, 21.0f)
            lineTo(16.66f, 16.66f)
        }
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(19.0f, 11.0f)
            arcTo(8.0f, 8.0f, 0.0f, false, true, 3.0f, 11.0f)
            arcTo(8.0f, 8.0f, 0.0f, false, true, 19.0f, 11.0f)
            close()
        }
        }.build()
    }

    public val ListFilter: ImageVector by lazy {
        ImageVector.Builder(
            name = "Marid.ListFilter",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(2.0f, 5.0f)
            lineTo(22.0f, 5.0f)
        }
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(6.0f, 12.0f)
            lineTo(18.0f, 12.0f)
        }
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(9.0f, 19.0f)
            lineTo(15.0f, 19.0f)
        }
        }.build()
    }

    public val Folder: ImageVector by lazy {
        ImageVector.Builder(
            name = "Marid.Folder",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(20.0f, 20.0f)
            arcTo(2.0f, 2.0f, 0.0f, false, false, 22.0f, 18.0f)
            lineTo(22.0f, 8.0f)
            arcTo(2.0f, 2.0f, 0.0f, false, false, 20.0f, 6.0f)
            lineTo(12.1f, 6.0f)
            arcTo(2.0f, 2.0f, 0.0f, false, true, 10.41f, 5.1f)
            lineTo(9.6f, 3.9f)
            arcTo(2.0f, 2.0f, 0.0f, false, false, 7.93f, 3.0f)
            lineTo(4.0f, 3.0f)
            arcTo(2.0f, 2.0f, 0.0f, false, false, 2.0f, 5.0f)
            lineTo(2.0f, 18.0f)
            arcTo(2.0f, 2.0f, 0.0f, false, false, 4.0f, 20.0f)
            close()
        }
        }.build()
    }

    public val Ellipsis: ImageVector by lazy {
        ImageVector.Builder(
            name = "Marid.Ellipsis",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(13.0f, 12.0f)
            arcTo(1.0f, 1.0f, 0.0f, false, true, 11.0f, 12.0f)
            arcTo(1.0f, 1.0f, 0.0f, false, true, 13.0f, 12.0f)
            close()
        }
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(20.0f, 12.0f)
            arcTo(1.0f, 1.0f, 0.0f, false, true, 18.0f, 12.0f)
            arcTo(1.0f, 1.0f, 0.0f, false, true, 20.0f, 12.0f)
            close()
        }
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(6.0f, 12.0f)
            arcTo(1.0f, 1.0f, 0.0f, false, true, 4.0f, 12.0f)
            arcTo(1.0f, 1.0f, 0.0f, false, true, 6.0f, 12.0f)
            close()
        }
        }.build()
    }

    public val Undo2: ImageVector by lazy {
        ImageVector.Builder(
            name = "Marid.Undo2",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(9.0f, 14.0f)
            lineTo(4.0f, 9.0f)
            lineTo(9.0f, 4.0f)
        }
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(4.0f, 9.0f)
            lineTo(14.5f, 9.0f)
            arcTo(5.5f, 5.5f, 0.0f, false, true, 20.0f, 14.5f)
            arcTo(5.5f, 5.5f, 0.0f, false, true, 14.5f, 20.0f)
            lineTo(11.0f, 20.0f)
        }
        }.build()
    }

    public val Check: ImageVector by lazy {
        ImageVector.Builder(
            name = "Marid.Check",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(20.0f, 6.0f)
            lineTo(9.0f, 17.0f)
            lineTo(4.0f, 12.0f)
        }
        }.build()
    }

    public val Settings: ImageVector by lazy {
        ImageVector.Builder(
            name = "Marid.Settings",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(9.671f, 4.136f)
            arcTo(2.34f, 2.34f, 0.0f, false, true, 14.33f, 4.136f)
            arcTo(2.34f, 2.34f, 0.0f, false, false, 17.649f, 6.051f)
            arcTo(2.34f, 2.34f, 0.0f, false, true, 19.979f, 10.084f)
            arcTo(2.34f, 2.34f, 0.0f, false, false, 19.979f, 13.915f)
            arcTo(2.34f, 2.34f, 0.0f, false, true, 17.649f, 17.948f)
            arcTo(2.34f, 2.34f, 0.0f, false, false, 14.33f, 19.863f)
            arcTo(2.34f, 2.34f, 0.0f, false, true, 9.671f, 19.863f)
            arcTo(2.34f, 2.34f, 0.0f, false, false, 6.351f, 17.948f)
            arcTo(2.34f, 2.34f, 0.0f, false, true, 4.021f, 13.915f)
            arcTo(2.34f, 2.34f, 0.0f, false, false, 4.021f, 10.084f)
            arcTo(2.34f, 2.34f, 0.0f, false, true, 6.35f, 6.051f)
            arcTo(2.34f, 2.34f, 0.0f, false, false, 9.669f, 4.136f)
        }
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(15.0f, 12.0f)
            arcTo(3.0f, 3.0f, 0.0f, false, true, 9.0f, 12.0f)
            arcTo(3.0f, 3.0f, 0.0f, false, true, 15.0f, 12.0f)
            close()
        }
        }.build()
    }

    public val User: ImageVector by lazy {
        ImageVector.Builder(
            name = "Marid.User",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(19.0f, 21.0f)
            lineTo(19.0f, 19.0f)
            arcTo(4.0f, 4.0f, 0.0f, false, false, 15.0f, 15.0f)
            lineTo(9.0f, 15.0f)
            arcTo(4.0f, 4.0f, 0.0f, false, false, 5.0f, 19.0f)
            lineTo(5.0f, 21.0f)
        }
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(16.0f, 7.0f)
            arcTo(4.0f, 4.0f, 0.0f, false, true, 8.0f, 7.0f)
            arcTo(4.0f, 4.0f, 0.0f, false, true, 16.0f, 7.0f)
            close()
        }
        }.build()
    }

    public val Smartphone: ImageVector by lazy {
        ImageVector.Builder(
            name = "Marid.Smartphone",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(7.0f, 2.0f)
            lineTo(17.0f, 2.0f)
            arcTo(2.0f, 2.0f, 0.0f, false, true, 19.0f, 4.0f)
            lineTo(19.0f, 20.0f)
            arcTo(2.0f, 2.0f, 0.0f, false, true, 17.0f, 22.0f)
            lineTo(7.0f, 22.0f)
            arcTo(2.0f, 2.0f, 0.0f, false, true, 5.0f, 20.0f)
            lineTo(5.0f, 4.0f)
            arcTo(2.0f, 2.0f, 0.0f, false, true, 7.0f, 2.0f)
            close()
        }
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(12.0f, 18.0f)
            lineTo(12.01f, 18.0f)
        }
        }.build()
    }

    public val X: ImageVector by lazy {
        ImageVector.Builder(
            name = "Marid.X",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(18.0f, 6.0f)
            lineTo(6.0f, 18.0f)
        }
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(6.0f, 6.0f)
            lineTo(18.0f, 18.0f)
        }
        }.build()
    }

    public val Square: ImageVector by lazy {
        ImageVector.Builder(
            name = "Marid.Square",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(5.0f, 3.0f)
            lineTo(19.0f, 3.0f)
            arcTo(2.0f, 2.0f, 0.0f, false, true, 21.0f, 5.0f)
            lineTo(21.0f, 19.0f)
            arcTo(2.0f, 2.0f, 0.0f, false, true, 19.0f, 21.0f)
            lineTo(5.0f, 21.0f)
            arcTo(2.0f, 2.0f, 0.0f, false, true, 3.0f, 19.0f)
            lineTo(3.0f, 5.0f)
            arcTo(2.0f, 2.0f, 0.0f, false, true, 5.0f, 3.0f)
            close()
        }
        }.build()
    }

    public val Brain: ImageVector by lazy {
        ImageVector.Builder(
            name = "Marid.Brain",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(12.0f, 18.0f)
            lineTo(12.0f, 5.0f)
        }
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(15.0f, 13.0f)
            arcTo(4.17f, 4.17f, 0.0f, false, true, 12.0f, 9.0f)
            arcTo(4.17f, 4.17f, 0.0f, false, true, 9.0f, 13.0f)
        }
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(17.707f, 6.294f)
            arcTo(3.0f, 3.0f, 0.0f, true, false, 12.0f, 5.0f)
            arcTo(3.0f, 3.0f, 0.0f, true, false, 6.293f, 6.294f)
        }
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(17.996f, 5.125f)
            arcTo(4.0f, 4.0f, 0.0f, false, true, 20.522f, 10.895f)
        }
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(18.0f, 18.0f)
            arcTo(4.0f, 4.0f, 0.0f, false, false, 20.0f, 10.535f)
        }
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(19.967f, 17.482f)
            arcTo(4.0f, 4.0f, 0.0f, true, true, 12.0f, 17.999f)
            arcTo(4.0f, 4.0f, 0.0f, true, true, 4.033f, 17.482f)
        }
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(6.0f, 18.0f)
            arcTo(4.0f, 4.0f, 0.0f, false, true, 4.0f, 10.535f)
        }
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(6.004f, 5.125f)
            arcTo(4.0f, 4.0f, 0.0f, false, false, 3.478f, 10.895f)
        }
        }.build()
    }

    public val Pencil: ImageVector by lazy {
        ImageVector.Builder(
            name = "Marid.Pencil",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(21.174f, 6.812f)
            arcTo(1.0f, 1.0f, 0.0f, false, false, 17.188f, 2.825f)
            lineTo(3.842f, 16.174f)
            arcTo(2.0f, 2.0f, 0.0f, false, false, 3.342f, 17.004f)
            lineTo(2.021f, 21.356f)
            arcTo(0.5f, 0.5f, 0.0f, false, false, 2.644f, 21.978f)
            lineTo(6.997f, 20.658f)
            arcTo(2.0f, 2.0f, 0.0f, false, false, 7.827f, 20.161f)
            close()
        }
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(15.0f, 5.0f)
            lineTo(19.0f, 9.0f)
        }
        }.build()
    }

    public val LoaderCircle: ImageVector by lazy {
        ImageVector.Builder(
            name = "Marid.LoaderCircle",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(21.0f, 12.0f)
            arcTo(9.0f, 9.0f, 0.0f, true, true, 14.781f, 3.44f)
        }
        }.build()
    }

    public val File: ImageVector by lazy {
        ImageVector.Builder(
            name = "Marid.File",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(6.0f, 22.0f)
            arcTo(2.0f, 2.0f, 0.0f, false, true, 4.0f, 20.0f)
            lineTo(4.0f, 4.0f)
            arcTo(2.0f, 2.0f, 0.0f, false, true, 6.0f, 2.0f)
            lineTo(14.0f, 2.0f)
            arcTo(2.4f, 2.4f, 0.0f, false, true, 15.704f, 2.706f)
            lineTo(19.292f, 6.294f)
            arcTo(2.4f, 2.4f, 0.0f, false, true, 20.0f, 8.0f)
            lineTo(20.0f, 20.0f)
            arcTo(2.0f, 2.0f, 0.0f, false, true, 18.0f, 22.0f)
            close()
        }
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(14.0f, 2.0f)
            lineTo(14.0f, 7.0f)
            arcTo(1.0f, 1.0f, 0.0f, false, false, 15.0f, 8.0f)
            lineTo(20.0f, 8.0f)
        }
        }.build()
    }

    public val Plug: ImageVector by lazy {
        ImageVector.Builder(
            name = "Marid.Plug",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(12.0f, 22.0f)
            lineTo(12.0f, 17.0f)
        }
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(15.0f, 8.0f)
            lineTo(15.0f, 2.0f)
        }
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(17.0f, 8.0f)
            arcTo(1.0f, 1.0f, 0.0f, false, true, 18.0f, 9.0f)
            lineTo(18.0f, 13.0f)
            arcTo(4.0f, 4.0f, 0.0f, false, true, 14.0f, 17.0f)
            lineTo(10.0f, 17.0f)
            arcTo(4.0f, 4.0f, 0.0f, false, true, 6.0f, 13.0f)
            lineTo(6.0f, 9.0f)
            arcTo(1.0f, 1.0f, 0.0f, false, true, 7.0f, 8.0f)
            close()
        }
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(9.0f, 8.0f)
            lineTo(9.0f, 2.0f)
        }
        }.build()
    }

    public val GitBranch: ImageVector by lazy {
        ImageVector.Builder(
            name = "Marid.GitBranch",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(15.0f, 6.0f)
            arcTo(9.0f, 9.0f, 0.0f, false, false, 6.0f, 15.0f)
            lineTo(6.0f, 3.0f)
        }
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(21.0f, 6.0f)
            arcTo(3.0f, 3.0f, 0.0f, false, true, 15.0f, 6.0f)
            arcTo(3.0f, 3.0f, 0.0f, false, true, 21.0f, 6.0f)
            close()
        }
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(9.0f, 18.0f)
            arcTo(3.0f, 3.0f, 0.0f, false, true, 3.0f, 18.0f)
            arcTo(3.0f, 3.0f, 0.0f, false, true, 9.0f, 18.0f)
            close()
        }
        }.build()
    }

    public val Paperclip: ImageVector by lazy {
        ImageVector.Builder(
            name = "Marid.Paperclip",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(16.0f, 6.0f)
            lineTo(7.586f, 14.586f)
            arcTo(2.0f, 2.0f, 0.0f, false, false, 10.415f, 17.415f)
            lineTo(18.829f, 8.829f)
            arcTo(4.0f, 4.0f, 0.0f, true, false, 13.172f, 3.172f)
            lineTo(4.793f, 11.723f)
            arcTo(6.0f, 6.0f, 0.0f, true, false, 13.278f, 20.208f)
            lineTo(21.657f, 11.657f)
        }
        }.build()
    }

    public val SquarePen: ImageVector by lazy {
        ImageVector.Builder(
            name = "Marid.SquarePen",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(12.0f, 3.0f)
            lineTo(5.0f, 3.0f)
            arcTo(2.0f, 2.0f, 0.0f, false, false, 3.0f, 5.0f)
            lineTo(3.0f, 19.0f)
            arcTo(2.0f, 2.0f, 0.0f, false, false, 5.0f, 21.0f)
            lineTo(19.0f, 21.0f)
            arcTo(2.0f, 2.0f, 0.0f, false, false, 21.0f, 19.0f)
            lineTo(21.0f, 12.0f)
        }
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(18.375f, 2.625f)
            arcTo(1.0f, 1.0f, 0.0f, false, true, 21.375f, 5.625f)
            lineTo(12.362f, 14.639f)
            arcTo(2.0f, 2.0f, 0.0f, false, true, 11.509f, 15.144f)
            lineTo(8.636f, 15.984f)
            arcTo(0.5f, 0.5f, 0.0f, false, true, 8.016f, 15.364f)
            lineTo(8.856f, 12.491f)
            arcTo(2.0f, 2.0f, 0.0f, false, true, 9.362f, 11.639f)
            close()
        }
        }.build()
    }

    public val Clock: ImageVector by lazy {
        ImageVector.Builder(
            name = "Marid.Clock",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(22.0f, 12.0f)
            arcTo(10.0f, 10.0f, 0.0f, false, true, 2.0f, 12.0f)
            arcTo(10.0f, 10.0f, 0.0f, false, true, 22.0f, 12.0f)
            close()
        }
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(12.0f, 6.0f)
            lineTo(12.0f, 12.0f)
            lineTo(16.0f, 14.0f)
        }
        }.build()
    }

    public val Lightbulb: ImageVector by lazy {
        ImageVector.Builder(
            name = "Marid.Lightbulb",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(15.0f, 14.0f)
            curveTo(15.2f, 13.0f, 15.7f, 12.3f, 16.5f, 11.5f)
            curveTo(17.5f, 10.6f, 18.0f, 9.3f, 18.0f, 8.0f)
            arcTo(6.0f, 6.0f, 0.0f, false, false, 6.0f, 8.0f)
            curveTo(6.0f, 9.0f, 6.2f, 10.2f, 7.5f, 11.5f)
            curveTo(8.2f, 12.2f, 8.8f, 13.0f, 9.0f, 14.0f)
        }
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(9.0f, 18.0f)
            lineTo(15.0f, 18.0f)
        }
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(10.0f, 22.0f)
            lineTo(14.0f, 22.0f)
        }
        }.build()
    }

    public val ShieldCheck: ImageVector by lazy {
        ImageVector.Builder(
            name = "Marid.ShieldCheck",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(20.0f, 13.0f)
            curveTo(20.0f, 18.0f, 16.5f, 20.5f, 12.34f, 21.95f)
            arcTo(1.0f, 1.0f, 0.0f, false, true, 11.67f, 21.94f)
            curveTo(7.5f, 20.5f, 4.0f, 18.0f, 4.0f, 13.0f)
            lineTo(4.0f, 6.0f)
            arcTo(1.0f, 1.0f, 0.0f, false, true, 5.0f, 5.0f)
            curveTo(7.0f, 5.0f, 9.5f, 3.8f, 11.24f, 2.28f)
            arcTo(1.17f, 1.17f, 0.0f, false, true, 12.76f, 2.28f)
            curveTo(14.51f, 3.81f, 17.0f, 5.0f, 19.0f, 5.0f)
            arcTo(1.0f, 1.0f, 0.0f, false, true, 20.0f, 6.0f)
            close()
        }
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(9.0f, 12.0f)
            lineTo(11.0f, 14.0f)
            lineTo(15.0f, 10.0f)
        }
        }.build()
    }

    public val ShieldAlert: ImageVector by lazy {
        ImageVector.Builder(
            name = "Marid.ShieldAlert",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(20.0f, 13.0f)
            curveTo(20.0f, 18.0f, 16.5f, 20.5f, 12.34f, 21.95f)
            arcTo(1.0f, 1.0f, 0.0f, false, true, 11.67f, 21.94f)
            curveTo(7.5f, 20.5f, 4.0f, 18.0f, 4.0f, 13.0f)
            lineTo(4.0f, 6.0f)
            arcTo(1.0f, 1.0f, 0.0f, false, true, 5.0f, 5.0f)
            curveTo(7.0f, 5.0f, 9.5f, 3.8f, 11.24f, 2.28f)
            arcTo(1.17f, 1.17f, 0.0f, false, true, 12.76f, 2.28f)
            curveTo(14.51f, 3.81f, 17.0f, 5.0f, 19.0f, 5.0f)
            arcTo(1.0f, 1.0f, 0.0f, false, true, 20.0f, 6.0f)
            close()
        }
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(12.0f, 8.0f)
            lineTo(12.0f, 12.0f)
        }
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(12.0f, 16.0f)
            lineTo(12.01f, 16.0f)
        }
        }.build()
    }

    public val Copy: ImageVector by lazy {
        ImageVector.Builder(
            name = "Marid.Copy",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(10.0f, 8.0f)
            lineTo(20.0f, 8.0f)
            arcTo(2.0f, 2.0f, 0.0f, false, true, 22.0f, 10.0f)
            lineTo(22.0f, 20.0f)
            arcTo(2.0f, 2.0f, 0.0f, false, true, 20.0f, 22.0f)
            lineTo(10.0f, 22.0f)
            arcTo(2.0f, 2.0f, 0.0f, false, true, 8.0f, 20.0f)
            lineTo(8.0f, 10.0f)
            arcTo(2.0f, 2.0f, 0.0f, false, true, 10.0f, 8.0f)
            close()
        }
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(4.0f, 16.0f)
            curveTo(2.9f, 16.0f, 2.0f, 15.1f, 2.0f, 14.0f)
            lineTo(2.0f, 4.0f)
            curveTo(2.0f, 2.9f, 2.9f, 2.0f, 4.0f, 2.0f)
            lineTo(14.0f, 2.0f)
            curveTo(15.1f, 2.0f, 16.0f, 2.9f, 16.0f, 4.0f)
        }
        }.build()
    }

    public val ThumbsUp: ImageVector by lazy {
        ImageVector.Builder(
            name = "Marid.ThumbsUp",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(15.0f, 5.88f)
            lineTo(14.0f, 10.0f)
            lineTo(19.83f, 10.0f)
            arcTo(2.0f, 2.0f, 0.0f, false, true, 21.75f, 12.56f)
            lineTo(19.42f, 20.56f)
            arcTo(2.0f, 2.0f, 0.0f, false, true, 17.5f, 22.0f)
            lineTo(4.0f, 22.0f)
            arcTo(2.0f, 2.0f, 0.0f, false, true, 2.0f, 20.0f)
            lineTo(2.0f, 12.0f)
            arcTo(2.0f, 2.0f, 0.0f, false, true, 4.0f, 10.0f)
            lineTo(6.76f, 10.0f)
            arcTo(2.0f, 2.0f, 0.0f, false, false, 8.55f, 8.89f)
            lineTo(12.0f, 2.0f)
            arcTo(3.13f, 3.13f, 0.0f, false, true, 15.0f, 5.88f)
            close()
        }
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(7.0f, 10.0f)
            lineTo(7.0f, 22.0f)
        }
        }.build()
    }

    public val ThumbsDown: ImageVector by lazy {
        ImageVector.Builder(
            name = "Marid.ThumbsDown",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f,
        ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(9.0f, 18.12f)
            lineTo(10.0f, 14.0f)
            lineTo(4.17f, 14.0f)
            arcTo(2.0f, 2.0f, 0.0f, false, true, 2.25f, 11.44f)
            lineTo(4.58f, 3.44f)
            arcTo(2.0f, 2.0f, 0.0f, false, true, 6.5f, 2.0f)
            lineTo(20.0f, 2.0f)
            arcTo(2.0f, 2.0f, 0.0f, false, true, 22.0f, 4.0f)
            lineTo(22.0f, 12.0f)
            arcTo(2.0f, 2.0f, 0.0f, false, true, 20.0f, 14.0f)
            lineTo(17.24f, 14.0f)
            arcTo(2.0f, 2.0f, 0.0f, false, false, 15.45f, 15.11f)
            lineTo(12.0f, 22.0f)
            arcTo(3.13f, 3.13f, 0.0f, false, true, 9.0f, 18.12f)
            close()
        }
        path(
            fill = null,
            stroke = SolidColor(Color(0xFF000000)),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        ) {
            moveTo(17.0f, 14.0f)
            lineTo(17.0f, 2.0f)
        }
        }.build()
    }

}
