package com.iyes.dacpressuremanager.ui

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.min

internal enum class OperatorGlyph {
    Plus,
    Minus,
}

@Composable
internal fun OperatorGlyphIcon(
    glyph: OperatorGlyph,
    color: Color,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 2.5.dp,
) {
    Canvas(modifier = modifier) {
        val strokePx = strokeWidth.toPx()
        val halfLength = min(size.width, size.height) * 0.34f
        val centerPoint = center
        drawLine(
            color = color,
            start = Offset(centerPoint.x - halfLength, centerPoint.y),
            end = Offset(centerPoint.x + halfLength, centerPoint.y),
            strokeWidth = strokePx,
            cap = StrokeCap.Round,
        )
        if (glyph == OperatorGlyph.Plus) {
            drawLine(
                color = color,
                start = Offset(centerPoint.x, centerPoint.y - halfLength),
                end = Offset(centerPoint.x, centerPoint.y + halfLength),
                strokeWidth = strokePx,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
internal fun ResetGlyph(
    color: Color,
    modifier: Modifier = Modifier,
    strokeWidth: Dp = 2.dp,
) {
    Canvas(modifier = modifier) {
        val strokePx = strokeWidth.toPx()
        val diameter = min(size.width, size.height) * 0.68f
        val topLeft = Offset(
            x = (size.width - diameter) / 2f,
            y = (size.height - diameter) / 2f,
        )
        drawArc(
            color = color,
            startAngle = -65f,
            sweepAngle = 300f,
            useCenter = false,
            topLeft = topLeft,
            size = Size(diameter, diameter),
            style = Stroke(width = strokePx, cap = StrokeCap.Round),
        )
        val arrowTip = Offset(
            x = topLeft.x + diameter * 0.11f,
            y = topLeft.y + diameter * 0.28f,
        )
        drawLine(
            color = color,
            start = arrowTip,
            end = Offset(
                x = arrowTip.x + diameter * 0.28f,
                y = arrowTip.y - diameter * 0.02f,
            ),
            strokeWidth = strokePx,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = arrowTip,
            end = Offset(
                x = arrowTip.x + diameter * 0.06f,
                y = arrowTip.y + diameter * 0.27f,
            ),
            strokeWidth = strokePx,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
internal fun DecimalGlyph(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        drawCircle(
            color = color,
            radius = min(size.width, size.height) / 2f,
        )
    }
}
