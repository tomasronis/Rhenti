package com.tomasronis.rhentiapp.presentation

import android.Manifest
import android.app.KeyguardManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
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
import com.tomasronis.rhentiapp.core.notifications.FcmTokenManager
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

    @Inject
    lateinit var fcmTokenManager: FcmTokenManager

    @Inject
    lateinit var twilioManager: com.tomasronis.rhentiapp.core.voip.TwilioManager

    // Store reference to MainTabViewModel for deep link navigation
    private var mainTabViewModel: MainTabViewModel? = null

    // Store pending deep link destination when ViewModel not yet available
    private var pendingDeepLink: DeepLinkDestination? = null

    // Permission launcher for notification + microphone permissions
    private val permissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (BuildConfig.DEBUG) {
            permissions.forEach { (perm, granted) ->
                Log.d(TAG, "Permission $perm granted: $granted")
            }
        }
        if (permissions[Manifest.permission.POST_NOTIFICATIONS] == true) {
            fcmTokenManager.refreshToken()
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen before calling super.onCreate()
        installSplashScreen()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Enable showing over lock screen for incoming calls
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val keyguardManager = getSystemService(KeyguardManager::class.java)
            keyguardManager?.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
            )
        }

        // Handle deep link if present (only on fresh launch, not configuration change)
        if (savedInstanceState == null) {
            handleDeepLink(intent)
        }

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
        setIntent(intent) // Important: Update the activity's intent

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "onNewIntent: action=${intent.action}, extras=${intent.extras?.keySet()?.joinToString()}")
        }

        // Wake screen for incoming call intents
        val isCallIntent = intent.action == "com.rhentimobile.INCOMING_CALL" ||
                           intent.action == "com.rhentimobile.ANSWER_CALL"
        if (isCallIntent) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setTurnScreenOn(true)
            }
        }

        // Handle deep link when app is already open and notification is tapped
        handleDeepLink(intent)
    }

    /**
     * Handle deep link from notification or URI.
     * Also handles FCM data extras from auto-displayed notifications.
     */
    private fun handleDeepLink(intent: Intent?) {
        if (intent == null) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "handleDeepLink: Intent is null")
            }
            return
        }

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "========================================")
            Log.d(TAG, "🔗 handleDeepLink called")
            Log.d(TAG, "========================================")
            Log.d(TAG, "Intent Details:")
            Log.d(TAG, "  action: ${intent.action}")
            Log.d(TAG, "  data: ${intent.data}")
            Log.d(TAG, "  categories: ${intent.categories?.joinToString()}")

            // Log all extras
            intent.extras?.let { bundle ->
                Log.d(TAG, "Intent Extras (${bundle.keySet().size} keys):")
                bundle.keySet().forEach { key ->
                    val value = bundle.get(key)
                    Log.d(TAG, "    $key = $value")
                }
            } ?: run {
                Log.d(TAG, "Intent Extras: none")
            }
            Log.d(TAG, "========================================")
        }

        // Handle incoming call actions from notification
        val isIncomingCallAction = intent.action == "com.rhentimobile.INCOMING_CALL" ||
                                   intent.action == "com.rhentimobile.ANSWER_CALL"
        if (isIncomingCallAction) {
            handleIncomingCallIntent(intent)
            return
        }

        // Check if this is a Firebase auto-displayed notification tap (OPEN_ACTIVITY action).
        // When the app is in background and FCM has a notification field, Firebase auto-displays
        // the notification. Tapping it fires an intent with action "OPEN_ACTIVITY".
        val isFirebaseAutoNotificationTap = intent.action == "OPEN_ACTIVITY"
        if (isFirebaseAutoNotificationTap && BuildConfig.DEBUG) {
            Log.d(TAG, "Detected Firebase auto-displayed notification tap (OPEN_ACTIVITY)")
        }

        // First try normal deep link parsing (URI or notification_type extras)
        var destination = DeepLinkHandler.parseIntent(intent)

        // If that fails, try extracting FCM data fields directly
        // (Firebase passes data payload as extras when auto-displaying notifications)
        if (destination == null) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Trying to parse FCM data extras...")
            }
            destination = parseFcmDataExtras(intent)
        }

        // If still no destination but this looks like a notification tap, use smart default
        if (destination == null) {
            destination = getDefaultNotificationDestination(intent)
        }

        // For Firebase auto-displayed notification taps, always navigate to Messages tab
        // even if we couldn't parse a specific destination from the intent
        if (destination == null && isFirebaseAutoNotificationTap) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Firebase auto notification tap with no specific destination -> Messages tab")
            }
            destination = DeepLinkDestination.ChatsTab
        }

        if (destination != null) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "✅ Deep link destination parsed: $destination")
            }
            routeToDestination(destination)

            // Clear the intent data to prevent re-processing on configuration change
            intent?.let {
                it.action = null
                it.data = null
                it.replaceExtras(null as android.os.Bundle?)
            }
        } else {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "❌ No deep link destination found")
            }
        }
    }

    /**
     * Check if this intent came from a notification tap.
     * Returns the appropriate default destination based on notification type.
     * Detects both our custom PendingIntent and Firebase auto-displayed notification taps.
     */
    private fun getDefaultNotificationDestination(intent: Intent): DeepLinkDestination? {
        val extras = intent.extras

        // Check if launched from notification by looking at extras
        val hasNotificationExtras = extras?.keySet()?.any { key ->
            key.startsWith("google.") ||
            key.startsWith("gcm.") ||
            key == "from" ||
            key == "collapse_key"
        } ?: false

        // Also check if the intent has our custom notification extras
        val hasCustomExtras = extras?.containsKey(DeepLinkHandler.EXTRA_NOTIFICATION_TYPE) == true

        // Check if launched via ACTION_VIEW with rhenti:// scheme (our PendingIntent)
        val hasRhentiUri = intent.data?.scheme == "rhenti"

        if (!hasNotificationExtras && !hasCustomExtras && !hasRhentiUri) {
            return null
        }

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Detected notification tap (firebase=$hasNotificationExtras, custom=$hasCustomExtras, uri=$hasRhentiUri)")
        }

        // Check notification type from title/body to determine default destination
        val notificationTitle = extras?.getString("gcm.notification.title") ?: ""
        val notificationBody = extras?.getString("gcm.notification.body") ?: ""
        val notificationText = "$notificationTitle $notificationBody".lowercase()

        // Check for call-related keywords
        val isCallNotification = notificationText.contains("call") ||
                                notificationText.contains("missed") ||
                                notificationText.contains("incoming") ||
                                notificationText.contains("voicemail")

        return if (isCallNotification) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Call-related notification detected -> Opening Calls tab")
            }
            DeepLinkDestination.CallsTab
        } else {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Message notification detected -> Opening Messages tab")
            }
            DeepLinkDestination.ChatsTab
        }
    }

    /**
     * Check if this intent came from a notification tap.
     */
    private fun isNotificationIntent(intent: Intent): Boolean {
        return getDefaultNotificationDestination(intent) != null
    }

    /**
     * Parse FCM data extras from intent.
     * When Firebase auto-displays notifications (app in background with notification field),
     * it passes the data payload as intent extras when the notification is tapped.
     */
    private fun parseFcmDataExtras(intent: Intent): DeepLinkDestination? {
        val extras = intent.extras ?: return null

        // Try to extract FCM data fields
        val type = extras.getString("type")
        val threadId = extras.getString("threadId")
        val contactId = extras.getString("contactId")
        val phoneNumber = extras.getString("phoneNumber")

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "FCM Data Extras Found:")
            Log.d(TAG, "  type: $type")
            Log.d(TAG, "  threadId: $threadId")
            Log.d(TAG, "  contactId: $contactId")
            Log.d(TAG, "  phoneNumber: $phoneNumber")
        }

        // Route based on type and available data
        return when (type?.lowercase()) {
            "message", "viewing", "application" -> {
                if (!threadId.isNullOrBlank()) {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "✅ Parsed FCM extras → Thread destination: $threadId")
                    }
                    DeepLinkDestination.Thread(threadId)
                } else {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "ℹ️ Message type but no threadId, navigating to Chats tab")
                    }
                    DeepLinkDestination.ChatsTab
                }
            }
            "call" -> {
                when {
                    !contactId.isNullOrBlank() -> {
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "✅ Parsed FCM extras → Contact destination: $contactId")
                        }
                        DeepLinkDestination.Contact(contactId)
                    }
                    !phoneNumber.isNullOrBlank() -> {
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "✅ Parsed FCM extras → Call destination: $phoneNumber")
                        }
                        DeepLinkDestination.Call(phoneNumber)
                    }
                    else -> {
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "ℹ️ Call type but no contactId/phoneNumber, navigating to Calls tab")
                        }
                        DeepLinkDestination.CallsTab
                    }
                }
            }
            else -> {
                // If we found threadId even without type, use it
                if (!threadId.isNullOrBlank()) {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "✅ Found threadId in extras (no type), navigating to thread: $threadId")
                    }
                    DeepLinkDestination.Thread(threadId)
                } else if (!contactId.isNullOrBlank()) {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "✅ Found contactId in extras (no type), navigating to contact: $contactId")
                    }
                    DeepLinkDestination.Contact(contactId)
                } else {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "⚠️ No recognizable FCM data in extras")
                    }
                    // Don't return a default here - let the caller decide
                    null
                }
            }
        }
    }

    /**
     * Handle incoming call intent from notification tap (Answer or view incoming call).
     * Uses the static CallInvite stored by RhentiFirebaseMessagingService.
     */
    private fun handleIncomingCallIntent(intent: Intent) {
        val callSid = intent.getStringExtra("CALL_SID")
        val callerNumber = intent.getStringExtra("CALLER_NUMBER")
        val autoAnswer = intent.action == "com.rhentimobile.ANSWER_CALL"

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Handling incoming call intent: action=${intent.action}, callSid=$callSid, autoAnswer=$autoAnswer")
        }

        // Cancel the notification
        com.tomasronis.rhentiapp.core.notifications.RhentiFirebaseMessagingService.cancelIncomingCallNotification(this)

        // Get the CallInvite from the static holder
        val callInvite = com.tomasronis.rhentiapp.core.notifications.RhentiFirebaseMessagingService.activeCallInvite
        if (callInvite == null) {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "No active CallInvite found - call may have been cancelled")
            }
            return
        }

        if (autoAnswer) {
            // Check microphone permission before accepting
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
                if (BuildConfig.DEBUG) {
                    Log.w(TAG, "RECORD_AUDIO not granted, showing ringing UI and requesting permission")
                }
                // Show ringing UI instead of auto-answering
                twilioManager.handleIncomingCallInvite(callInvite)
                // Request the permission so user can accept after granting
                permissionsLauncher.launch(arrayOf(Manifest.permission.RECORD_AUDIO))
                return
            }

            // Answer the call immediately
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Auto-answering incoming call from: ${callInvite.from}")
            }
            twilioManager.acceptIncomingCall(callInvite)
            com.tomasronis.rhentiapp.core.notifications.RhentiFirebaseMessagingService.clearCallInvite()
        } else {
            // Show the ringing UI (ActiveCallScreen will show as overlay)
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Showing ringing UI for incoming call from: ${callInvite.from}")
            }
            twilioManager.handleIncomingCallInvite(callInvite)
        }

        // Clear intent to prevent re-processing
        intent.action = null
        intent.replaceExtras(null as android.os.Bundle?)
    }

    /**
     * Route to the appropriate destination based on deep link.
     */
    private fun routeToDestination(destination: DeepLinkDestination) {
        val viewModel = mainTabViewModel

        if (viewModel == null) {
            // ViewModel not available yet (app cold start from notification)
            // Store destination and apply it when ViewModel becomes available
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "MainTabViewModel not available yet, storing destination: $destination")
            }
            pendingDeepLink = destination
            return
        }

        // Apply navigation immediately
        applyDeepLinkNavigation(viewModel, destination)
    }

    /**
     * Apply deep link navigation to the ViewModel.
     */
    private fun applyDeepLinkNavigation(viewModel: MainTabViewModel, destination: DeepLinkDestination) {
        lifecycleScope.launch {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "✅ Applying deep link navigation: $destination")
            }

            when (destination) {
                is DeepLinkDestination.Thread -> {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "  → Opening thread: ${destination.threadId}")
                    }
                    viewModel.setThreadIdToOpen(destination.threadId)
                    viewModel.setSelectedTab(0) // Navigate to Chats tab
                }
                is DeepLinkDestination.Contact -> {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "  → Opening contact: ${destination.contactId}")
                    }
                    viewModel.setContactIdToOpen(destination.contactId)
                    viewModel.setSelectedTab(1) // Navigate to Contacts tab
                }
                is DeepLinkDestination.Call -> {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "  → Opening call: ${destination.phoneNumber}")
                    }
                    // Navigate to Calls tab with phone number
                    viewModel.setSelectedTab(2)
                    // TODO: Implement call initiation if needed
                }
                is DeepLinkDestination.ChatsTab -> {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "  → Opening Chats tab")
                    }
                    viewModel.setSelectedTab(0)
                }
                is DeepLinkDestination.ContactsTab -> {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "  → Opening Contacts tab")
                    }
                    viewModel.setSelectedTab(1)
                }
                is DeepLinkDestination.CallsTab -> {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "  → Opening Calls tab")
                    }
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

        // Apply pending deep link if any (from notification when app was killed)
        pendingDeepLink?.let { destination ->
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "MainTabViewModel now available, applying pending deep link: $destination")
            }
            applyDeepLinkNavigation(viewModel, destination)
            pendingDeepLink = null // Clear after applying
        }
    }

    /**
     * Request notification permission (Android 13+).
     * Should be called after login.
     */
    fun requestNotificationPermission() {
        val permissionsToRequest = mutableListOf<String>()

        // Notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Microphone permission (needed for VoIP calls)
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.RECORD_AUDIO)
        }

        if (permissionsToRequest.isNotEmpty()) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Requesting permissions: $permissionsToRequest")
            }
            permissionsLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "All permissions already granted")
            }
            fcmTokenManager.refreshToken()
        }
    }
}
