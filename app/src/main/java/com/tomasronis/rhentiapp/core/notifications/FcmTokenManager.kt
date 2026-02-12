package com.tomasronis.rhentiapp.core.notifications

import android.content.Context
import android.os.Build
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

                // Save token if changed
                val savedToken = preferencesManager.getFcmToken()
                if (token != savedToken) {
                    preferencesManager.saveFcmToken(token)
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "FCM Token saved (token changed)")
                    }
                }

                // Always sync with backend if user is authenticated
                // (Token may have been fetched before login, so we need to register it now)
                if (tokenManager.isAuthenticated()) {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "User authenticated, syncing token with backend...")
                    }
                    syncTokenWithBackend(token)
                } else {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "User not authenticated, deferring device registration")
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
     * Sync FCM token with backend using full device metadata.
     * Called when token changes or user logs in.
     */
    suspend fun syncTokenWithBackend(token: String) {
        try {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "syncTokenWithBackend() called")
            }

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

            // Collect device metadata
            val manufacturer = Build.MANUFACTURER
            val model = Build.MODEL
            val version = Build.VERSION.RELEASE
            val brand = Build.BRAND
            val deviceName = Build.DEVICE // Use Build.DEVICE for device name
            val os = "Android"

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Registering device with backend:")
                Log.d(TAG, "  device_id: $deviceId")
                Log.d(TAG, "  account: $superAccountId")
                Log.d(TAG, "  childAccount: $userId")
                Log.d(TAG, "  manufacturer: $manufacturer")
                Log.d(TAG, "  model: $model")
                Log.d(TAG, "  version: $version")
                Log.d(TAG, "  app_version: ${BuildConfig.VERSION_NAME}")
                Log.d(TAG, "  name: $deviceName")
                Log.d(TAG, "  token: ${token.take(20)}...")
                Log.d(TAG, "  brand: $brand")
                Log.d(TAG, "  os: $os")
                Log.d(TAG, "Calling POST /devices/unauthorized...")
            }

            // Register device with full metadata (new spec)
            val result = notificationsRepository.registerDevice(
                deviceId = deviceId,
                account = superAccountId,
                childAccount = userId,
                manufacturer = manufacturer,
                model = model,
                version = version,
                appVersion = BuildConfig.VERSION_NAME,
                name = deviceName,
                token = token,
                brand = brand,
                os = os
            )

            result.onSuccess {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "✅ Device registered successfully with backend")
                    Log.d(TAG, "Device: $manufacturer $model, OS: $os $version")
                }
            }.onFailure { error ->
                if (BuildConfig.DEBUG) {
                    Log.e(TAG, "❌ Failed to register device with backend: ${error.message}")
                    error.printStackTrace()
                }
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "❌ Error syncing device registration", e)
                e.printStackTrace()
            }
        }
    }

    /**
     * Unregister device from push notifications.
     * Called on logout.
     */
    suspend fun unregisterToken() {
        try {
            // Get device ID
            val deviceId = android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            )

            // Sign out device from backend
            val result = notificationsRepository.signoutDevice(deviceId)

            result.onSuccess {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Device signed out successfully")
                }
                // Clear local token
                preferencesManager.clearFcmToken()
            }.onFailure { error ->
                if (BuildConfig.DEBUG) {
                    Log.e(TAG, "Failed to sign out device: ${error.message}")
                }
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Error signing out device", e)
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
