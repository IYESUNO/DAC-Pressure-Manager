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
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

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
        val diameter = min(size.width, size.height) * 0.72f
        val radius = diameter / 2f
        val topLeft = Offset(
            x = (size.width - diameter) / 2f,
            y = (size.height - diameter) / 2f,
        )
        drawArc(
            color = color,
            startAngle = 130f,
            sweepAngle = -280f,
            useCenter = false,
            topLeft = topLeft,
            size = Size(diameter, diameter),
            style = Stroke(width = strokePx, cap = StrokeCap.Round),
        )

        val endAngle = 210f * PI.toFloat() / 180f
        val arrowTip = Offset(
            x = center.x + radius * cos(endAngle),
            y = center.y + radius * sin(endAngle),
        )
        val direction = Offset(
            x = -0.5f,
            y = 0.866f,
        )
        val perpendicular = Offset(
            x = -direction.y,
            y = direction.x,
        )
        val arrowLength = diameter * 0.25f
        val arrowWidth = diameter * 0.14f
        val arrowBase = arrowTip - direction * arrowLength
        drawLine(
            color = color,
            start = arrowTip,
            end = arrowBase + perpendicular * arrowWidth,
            strokeWidth = strokePx,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = color,
            start = arrowTip,
            end = arrowBase - perpendicular * arrowWidth,
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
