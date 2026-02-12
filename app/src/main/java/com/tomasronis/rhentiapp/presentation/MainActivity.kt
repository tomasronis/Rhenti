package com.tomasronis.rhentiapp.presentation

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
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
import com.tomasronis.rhentiapp.BuildConfig
import kotlinx.coroutines.launch
import com.tomasronis.rhentiapp.core.notifications.DeepLinkDestination
import com.tomasronis.rhentiapp.core.notifications.DeepLinkHandler
import com.tomasronis.rhentiapp.core.preferences.PreferencesManager
import com.tomasronis.rhentiapp.core.preferences.ThemeMode
import com.tomasronis.rhentiapp.presentation.auth.AuthViewModel
import com.tomasronis.rhentiapp.presentation.main.MainTabViewModel
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

    // Store reference to MainTabViewModel for deep link navigation
    private var mainTabViewModel: MainTabViewModel? = null

    // Permission launcher for notification permission (Android 13+)
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Notification permission granted: $isGranted")
        }
        // Permission result is logged, no action needed
        // User can still use the app without notifications
    }

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen before calling super.onCreate()
        installSplashScreen()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Handle deep link if present
        handleDeepLink(intent)

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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Handle deep link when app is already open and notification is tapped
        handleDeepLink(intent)
    }

    /**
     * Handle deep link from notification or URI.
     */
    private fun handleDeepLink(intent: Intent?) {
        intent ?: return

        val destination = DeepLinkHandler.parseIntent(intent)
        if (destination != null) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Deep link destination: $destination")
            }
            routeToDestination(destination)
        }
    }

    /**
     * Route to the appropriate destination based on deep link.
     */
    private fun routeToDestination(destination: DeepLinkDestination) {
        val viewModel = mainTabViewModel ?: run {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "MainTabViewModel not available yet, storing destination for later")
            }
            // TODO: Store destination and navigate after ViewModel is available
            return
        }

        lifecycleScope.launch {
            when (destination) {
                is DeepLinkDestination.Thread -> {
                    viewModel.setThreadIdToOpen(destination.threadId)
                    viewModel.setSelectedTab(0) // Navigate to Chats tab
                }
                is DeepLinkDestination.Contact -> {
                    viewModel.setContactIdToOpen(destination.contactId)
                    viewModel.setSelectedTab(1) // Navigate to Contacts tab
                }
                is DeepLinkDestination.Call -> {
                    // Navigate to Calls tab with phone number
                    viewModel.setSelectedTab(2)
                    // TODO: Implement call initiation if needed
                }
                is DeepLinkDestination.ChatsTab -> {
                    viewModel.setSelectedTab(0)
                }
                is DeepLinkDestination.ContactsTab -> {
                    viewModel.setSelectedTab(1)
                }
                is DeepLinkDestination.CallsTab -> {
                    viewModel.setSelectedTab(2)
                }
            }
        }
    }

    /**
     * Set MainTabViewModel reference from composable.
     * Called when ViewModel is created in the composition.
     */
    fun setMainTabViewModel(viewModel: MainTabViewModel) {
        mainTabViewModel = viewModel
    }

    /**
     * Request notification permission (Android 13+).
     * Should be called after login.
     */
    fun requestNotificationPermission() {
        // Only needed on Android 13 (API 33) and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "Notification permission already granted")
                    }
                }
                shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) -> {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "Showing notification permission rationale")
                    }
                    // Show rationale to user (optional - can be implemented with a dialog)
                    // For now, just request the permission
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                else -> {
                    // Request permission
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Notification permission not required on this Android version")
            }
        }
    }
}
