package com.tomasronis.rhentiapp.core.notifications

import android.content.Context
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.tomasronis.rhentiapp.BuildConfig
import com.tomasronis.rhentiapp.core.preferences.PreferencesManager
import com.tomasronis.rhentiapp.core.security.TokenManager
import com.tomasronis.rhentiapp.data.notifications.repository.NotificationsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages FCM token lifecycle: fetching, storing, and syncing with backend.
 */
@Singleton
class FcmTokenManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val preferencesManager: PreferencesManager,
    private val tokenManager: TokenManager,
    private val notificationsRepository: NotificationsRepository
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        private const val TAG = "FcmTokenManager"
    }

    /**
     * Fetch current FCM token and sync with backend if needed.
     * Called on app startup and after login.
     */
    fun refreshToken() {
        scope.launch {
            try {
                // Get current token
                val token = FirebaseMessaging.getInstance().token.await()
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "FCM Token fetched: $token")
                }

                // Check if token changed
                val savedToken = preferencesManager.getFcmToken()
                if (token != savedToken) {
                    // Save new token
                    preferencesManager.saveFcmToken(token)

                    // Sync with backend if user is logged in
                    if (tokenManager.isAuthenticated()) {
                        syncTokenWithBackend(token)
                    }
                }
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) {
                    Log.e(TAG, "Failed to fetch FCM token", e)
                }
            }
        }
    }

    /**
     * Sync FCM token with backend.
     * Called when token changes or user logs in.
     */
    suspend fun syncTokenWithBackend(token: String) {
        try {
            val userId = tokenManager.getUserId() ?: run {
                if (BuildConfig.DEBUG) {
                    Log.w(TAG, "Cannot sync token: User ID not available")
                }
                return
            }

            val superAccountId = tokenManager.getSuperAccountId() ?: run {
                if (BuildConfig.DEBUG) {
                    Log.w(TAG, "Cannot sync token: Super Account ID not available")
                }
                return
            }

            // Get device ID (use Android ID as device identifier)
            val deviceId = android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            )

            // Register token with backend
            val result = notificationsRepository.registerFcmToken(
                userId = userId,
                superAccountId = superAccountId,
                fcmToken = token,
                platform = "android",
                deviceId = deviceId,
                appVersion = BuildConfig.VERSION_NAME
            )

            result.onSuccess {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "FCM token synced successfully with backend")
                }
            }.onFailure { error ->
                if (BuildConfig.DEBUG) {
                    Log.e(TAG, "Failed to sync FCM token with backend: ${error.message}")
                }
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Error syncing FCM token", e)
            }
        }
    }

    /**
     * Unregister FCM token from backend.
     * Called on logout.
     */
    suspend fun unregisterToken() {
        try {
            val token = preferencesManager.getFcmToken() ?: return
            val userId = tokenManager.getUserId() ?: return

            // Unregister from backend
            val result = notificationsRepository.unregisterFcmToken(
                userId = userId,
                fcmToken = token
            )

            result.onSuccess {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "FCM token unregistered successfully")
                }
                // Clear local token
                preferencesManager.clearFcmToken()
            }.onFailure { error ->
                if (BuildConfig.DEBUG) {
                    Log.e(TAG, "Failed to unregister FCM token: ${error.message}")
                }
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Error unregistering FCM token", e)
            }
        }
    }

    /**
     * Handle new token received from Firebase.
     * Called automatically by FirebaseMessagingService.
     */
    suspend fun onNewToken(token: String) {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "New FCM token received: $token")
        }

        // Save new token
        preferencesManager.saveFcmToken(token)

        // Sync with backend if user is logged in
        if (tokenManager.isAuthenticated()) {
            syncTokenWithBackend(token)
        }
    }
}
