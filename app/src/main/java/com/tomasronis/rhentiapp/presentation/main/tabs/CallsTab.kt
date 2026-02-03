package com.tomasronis.rhentiapp.presentation.main.tabs

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tomasronis.rhentiapp.presentation.calls.CallsScreen

/**
 * Calls tab wrapper for the main tab screen.
 */
@Composable
fun CallsTab(
    modifier: Modifier = Modifier
) {
    CallsScreen(
        onNavigateToActiveCall = { phoneNumber ->
            // TODO: Phase 7 - Navigate to active call screen
            // For now, this is a placeholder
        },
        modifier = modifier
    )
}
