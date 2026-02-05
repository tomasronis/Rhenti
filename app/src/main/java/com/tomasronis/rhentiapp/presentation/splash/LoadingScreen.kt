package com.tomasronis.rhentiapp.presentation.splash

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tomasronis.rhentiapp.R
import com.tomasronis.rhentiapp.presentation.theme.RhentiCoral
import kotlinx.coroutines.delay

/**
 * Animated loading screen with bouncing logo.
 * Inspired by playful loading animations.
 */
@Composable
fun LoadingScreen(
    onLoadingComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Auto-complete after 2 seconds (you can adjust or remove this)
    LaunchedEffect(Unit) {
        delay(2000)
        onLoadingComplete()
    }

    // Gradient background (navy blue gradient like login screen)
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF2C3E50),
                        Color(0xFF34495E),
                        Color(0xFF2C3E50)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Bouncing logo animation
            BouncingLogo()

            Spacer(modifier = Modifier.height(32.dp))

            // "rhenti" text with fade animation
            FadingText()

            Spacer(modifier = Modifier.height(16.dp))

            // Loading dots animation
            LoadingDots()
        }
    }
}

/**
 * Bouncing logo animation
 */
@Composable
private fun BouncingLogo() {
    val infiniteTransition = rememberInfiniteTransition(label = "bounce")

    // Bounce animation (up and down)
    val bounce by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -30f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 600,
                easing = EaseInOutCubic
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bounce"
    )

    // Scale animation (squash and stretch effect)
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 600,
                easing = EaseInOutCubic
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = Modifier
            .offset(y = bounce.dp)
            .scale(scale)
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo_rhenti),
            contentDescription = "Rhenti Logo",
            modifier = Modifier.size(120.dp)
        )
    }
}

/**
 * Fading text animation
 */
@Composable
private fun FadingText() {
    val infiniteTransition = rememberInfiniteTransition(label = "fade")

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 1000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Text(
        text = "rhenti",
        style = MaterialTheme.typography.displayMedium.copy(
            fontSize = 48.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = 3.sp
        ),
        color = Color.White.copy(alpha = alpha)
    )
}

/**
 * Loading dots animation
 */
@Composable
private fun LoadingDots() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            LoadingDot(delay = index * 200)
        }
    }
}

/**
 * Single animated dot
 */
@Composable
private fun LoadingDot(delay: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "dot_$delay")

    val scale by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1200
                0.5f at 0
                1.2f at 400
                0.5f at 800
                0.5f at 1200
            },
            repeatMode = RepeatMode.Restart,
            initialStartOffset = StartOffset(delay)
        ),
        label = "dot_scale_$delay"
    )

    Box(
        modifier = Modifier
            .size(12.dp)
            .scale(scale)
            .background(
                color = RhentiCoral,
                shape = androidx.compose.foundation.shape.CircleShape
            )
    )
}

/**
 * Alternative: Cute bouncing cat-like character animation
 * (Simplified version inspired by the loading cat)
 */
@Composable
fun CuteCatLoadingAnimation() {
    val infiniteTransition = rememberInfiniteTransition(label = "cat")

    // Bounce animation
    val bounce by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -40f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 500,
                easing = EaseInOutCubic
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cat_bounce"
    )

    // Rotation for playful effect
    val rotation by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 500,
                easing = EaseInOutCubic
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cat_rotation"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF2C3E50),
                        Color(0xFF34495E)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Cute cat emoji or character
            Text(
                text = "🐱",
                fontSize = 80.sp,
                modifier = Modifier.offset(y = bounce.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Loading text
            Text(
                text = "Loading...",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White.copy(alpha = 0.8f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Animated dots
            LoadingDots()
        }
    }
}
