package com.tomasronis.rhentiapp.presentation.main.chathub.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Custom bot icon composable based on the SVG design.
 * Draws a cute robot face with circular background, screen, and eyes.
 */
@Composable
fun BotIcon(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    backgroundColor: Color = Color(0xFFFA9483),
    foregroundColor: Color = Color.White
) {
    Canvas(modifier = modifier.size(size)) {
        val canvasSize = this.size.minDimension
        val scale = canvasSize / 508f // SVG viewBox is 508x508

        // Draw circular background
        drawCircle(
            color = backgroundColor,
            radius = canvasSize / 2f,
            center = Offset(canvasSize / 2f, canvasSize / 2f)
        )

        // Draw screen (rounded rectangle in the middle)
        drawRoundedScreen(
            color = foregroundColor,
            scale = scale,
            canvasSize = canvasSize
        )

        // Draw eyes (two circles)
        drawEyes(
            color = foregroundColor,
            scale = scale,
            canvasSize = canvasSize
        )
    }
}

/**
 * Draw the robot's screen (rounded rectangle).
 * Uses the outer border path from the SVG for a more prominent screen.
 */
private fun DrawScope.drawRoundedScreen(
    color: Color,
    scale: Float,
    canvasSize: Float
) {
    // Screen coordinates from SVG outer path (more visible border)
    // Original SVG: x: 61.126, y: 144.383, width: 386, height: 219.491
    val screenX = 61.126f * scale
    val screenY = 144.383f * scale
    val screenWidth = 386f * scale
    val screenHeight = 219.491f * scale
    val cornerRadius = 20f * scale  // Increased corner radius for smoother look

    drawRoundRect(
        color = color,
        topLeft = Offset(screenX, screenY),
        size = Size(screenWidth, screenHeight),
        cornerRadius = CornerRadius(cornerRadius, cornerRadius)
    )
}

/**
 * Draw the robot's eyes (two large circles).
 * Eyes are quite prominent in the design - approximately 38.65 radius each.
 */
private fun DrawScope.drawEyes(
    color: Color,
    scale: Float,
    canvasSize: Float
) {
    // Eye radius calculated from SVG circle paths (approximately 38.65)
    val eyeRadius = 38.65f * scale

    // Left eye - center at (171.206, 230.968)
    val leftEyeX = 171.206f * scale
    val leftEyeY = 230.968f * scale

    drawCircle(
        color = color,
        radius = eyeRadius,
        center = Offset(leftEyeX, leftEyeY)
    )

    // Right eye - center at (337.05, 230.968)
    val rightEyeX = 337.05f * scale
    val rightEyeY = 230.968f * scale

    drawCircle(
        color = color,
        radius = eyeRadius,
        center = Offset(rightEyeX, rightEyeY)
    )
}
