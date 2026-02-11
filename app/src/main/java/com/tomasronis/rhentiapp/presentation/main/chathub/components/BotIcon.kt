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
import androidx.compose.ui.graphics.drawscope.Stroke
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
 * Draw the robot's screen (rounded rectangle with thick border).
 * Optimized size with rounder corners for a cuter look.
 */
private fun DrawScope.drawRoundedScreen(
    color: Color,
    scale: Float,
    canvasSize: Float
) {
    // Screen size - slightly bigger (80% of original)
    val screenWidth = 310f * scale  // Increased from 270
    val screenHeight = 176f * scale  // Increased from 154
    val centerX = (254f - screenWidth / scale / 2) * scale  // Center horizontally
    val centerY = 160f * scale  // Slightly higher

    // Much rounder corners for a friendlier look
    val cornerRadius = 45f * scale  // Rounder appearance

    // Border thickness
    val borderWidth = 22f * scale

    // Draw bordered rounded rectangle (stroke only)
    drawRoundRect(
        color = color,
        topLeft = Offset(centerX, centerY),
        size = Size(screenWidth, screenHeight),
        cornerRadius = CornerRadius(cornerRadius, cornerRadius),
        style = Stroke(width = borderWidth)
    )
}

/**
 * Draw the robot's eyes (two large circles).
 * Eyes brought closer together for a friendlier appearance.
 */
private fun DrawScope.drawEyes(
    color: Color,
    scale: Float,
    canvasSize: Float
) {
    // Eye radius calculated from SVG circle paths (approximately 38.65)
    val eyeRadius = 38.65f * scale

    // Bring eyes closer together by moving them toward center
    // Original left eye: 171.206, moving right by 25 units
    val leftEyeX = 196f * scale  // Moved from 171.206 toward center
    val leftEyeY = 230.968f * scale

    drawCircle(
        color = color,
        radius = eyeRadius,
        center = Offset(leftEyeX, leftEyeY)
    )

    // Original right eye: 337.05, moving left by 25 units
    val rightEyeX = 312f * scale  // Moved from 337.05 toward center
    val rightEyeY = 230.968f * scale

    drawCircle(
        color = color,
        radius = eyeRadius,
        center = Offset(rightEyeX, rightEyeY)
    )
}
