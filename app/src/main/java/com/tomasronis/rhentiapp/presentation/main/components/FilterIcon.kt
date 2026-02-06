package com.tomasronis.rhentiapp.presentation.main.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp

/**
 * Custom filter icon with three horizontal lines that decrease in width.
 * Lines are center-justified to create a funnel/filter appearance.
 */
@Composable
fun FilterIcon(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onBackground
) {
    Canvas(
        modifier = modifier.size(24.dp)
    ) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        val strokeWidth = 2.dp.toPx()
        val spacing = 4.dp.toPx()

        // Calculate vertical positions for three lines
        val topY = canvasHeight * 0.25f
        val middleY = canvasHeight * 0.5f
        val bottomY = canvasHeight * 0.75f

        // Line widths (from top to bottom: 20dp, 16dp, 12dp)
        val topLineWidth = 20.dp.toPx()
        val middleLineWidth = 16.dp.toPx()
        val bottomLineWidth = 12.dp.toPx()

        // Draw top line (longest)
        val topLineStart = (canvasWidth - topLineWidth) / 2f
        drawLine(
            color = tint,
            start = Offset(topLineStart, topY),
            end = Offset(topLineStart + topLineWidth, topY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        // Draw middle line
        val middleLineStart = (canvasWidth - middleLineWidth) / 2f
        drawLine(
            color = tint,
            start = Offset(middleLineStart, middleY),
            end = Offset(middleLineStart + middleLineWidth, middleY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )

        // Draw bottom line (shortest)
        val bottomLineStart = (canvasWidth - bottomLineWidth) / 2f
        drawLine(
            color = tint,
            start = Offset(bottomLineStart, bottomY),
            end = Offset(bottomLineStart + bottomLineWidth, bottomY),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round
        )
    }
}
