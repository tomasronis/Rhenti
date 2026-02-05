package com.tomasronis.rhentiapp.presentation.main.tabs

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Profile/Settings tab - matches iOS design with settings screen as main view.
 * The Settings screen includes the profile card at the top.
 */
@Composable
fun ProfileTab(
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Use the new SettingsScreen which includes profile card at top
    SettingsScreen(
        onLogout = onLogout,
        modifier = modifier
    )
}
