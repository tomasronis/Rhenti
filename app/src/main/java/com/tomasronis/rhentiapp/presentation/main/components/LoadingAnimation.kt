package com.tomasronis.rhentiapp.presentation.main.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.*
import com.tomasronis.rhentiapp.R

/**
 * Reusable Lottie loading animation component.
 * Shows the cute loading cat animation.
 */
@Composable
fun LoadingAnimation(
    modifier: Modifier = Modifier
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.loading_cat)
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever
    )

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier.size(200.dp)
        )
    }
}
