package com.tomasronis.rhentiapp.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.tomasronis.rhentiapp.presentation.auth.AuthViewModel
import com.tomasronis.rhentiapp.presentation.navigation.RhentiNavHost
import com.tomasronis.rhentiapp.presentation.theme.RhentiAppTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * Main Activity for the Rhenti App.
 *
 * This is the single activity that hosts all Compose screens.
 * Uses Hilt for dependency injection.
 * Handles back button navigation properly.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen before calling super.onCreate()
        installSplashScreen()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val authViewModel: AuthViewModel = hiltViewModel()
            val navController = rememberNavController()

            // Handle back button to navigate within app instead of exiting
            BackHandler(enabled = navController.previousBackStackEntry != null) {
                navController.popBackStack()
            }

            RhentiAppTheme {
                RhentiNavHost(
                    navController = navController,
                    authViewModel = authViewModel
                )
            }
        }
    }
}
