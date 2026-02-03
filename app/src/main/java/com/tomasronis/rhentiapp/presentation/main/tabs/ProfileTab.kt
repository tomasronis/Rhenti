package com.tomasronis.rhentiapp.presentation.main.tabs

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tomasronis.rhentiapp.presentation.profile.ProfileScreen
import com.tomasronis.rhentiapp.presentation.profile.SettingsScreen

/**
 * Profile tab with internal navigation for profile and settings screens.
 */
@Composable
fun ProfileTab(
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "profile",
        modifier = modifier
    ) {
        composable("profile") {
            ProfileScreen(
                onNavigateToSettings = {
                    navController.navigate("settings")
                },
                onLogout = onLogout
            )
        }

        composable("settings") {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
