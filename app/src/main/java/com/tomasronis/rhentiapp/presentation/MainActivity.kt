package com.tomasronis.rhentiapp.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.tomasronis.rhentiapp.core.preferences.PreferencesManager
import com.tomasronis.rhentiapp.core.preferences.ThemeMode
import com.tomasronis.rhentiapp.presentation.auth.AuthViewModel
import com.tomasronis.rhentiapp.presentation.navigation.RhentiNavHost
import com.tomasronis.rhentiapp.presentation.theme.RhentiAppTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Main Activity for the Rhenti App.
 *
 * This is the single activity that hosts all Compose screens.
 * Uses Hilt for dependency injection.
 * Back navigation is handled by individual screens and nested NavControllers.
 * Supports dynamic theme mode switching (Dark/Light/System).
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesManager: PreferencesManager

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Permission result handled by system notification settings */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen before calling super.onCreate()
        installSplashScreen()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        requestNotificationPermission()

        setContent {
            val authViewModel: AuthViewModel = hiltViewModel()
            val navController = rememberNavController()

            // Observe theme mode preference - use remember to prevent recreation
            val themeModeFlow = remember {
                preferencesManager.themeMode.stateIn(
                    scope = lifecycleScope,
                    started = SharingStarted.Eagerly,
                    initialValue = ThemeMode.SYSTEM // Default to system theme
                )
            }
            val themeMode by themeModeFlow.collectAsState()

            // Determine if dark theme should be used
            val systemInDarkTheme = isSystemInDarkTheme()
            val useDarkTheme = when (themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> systemInDarkTheme
            }

            RhentiAppTheme(darkTheme = useDarkTheme) {
                RhentiNavHost(
                    navController = navController,
                    authViewModel = authViewModel
                )
            }
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}
