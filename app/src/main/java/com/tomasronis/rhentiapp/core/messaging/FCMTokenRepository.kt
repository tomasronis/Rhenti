package com.tomasronis.rhentiapp.core.messaging

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.tomasronis.rhentiapp.core.network.ApiClient
import com.tomasronis.rhentiapp.core.security.EncryptedPreferences
import com.tomasronis.rhentiapp.core.security.TokenManager
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages FCM token lifecycle: retrieval, storage, and registration with the backend.
 */
@Singleton
class FCMTokenRepository @Inject constructor(
    private val apiClient: ApiClient,
    private val tokenManager: TokenManager,
    private val encryptedPreferences: EncryptedPreferences
) {
    /**
     * Retrieve the current FCM token and register it with the backend.
     * Called on app startup and after login.
     */
    suspend fun registerCurrentToken() {
        try {
            val fcmToken = FirebaseMessaging.getInstance().token.await()
            Log.d(TAG, "FCM token retrieved")
            registerTokenWithBackend(fcmToken)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to retrieve FCM token", e)
        }
    }

    /**
     * Called by RhentiFCMService when the token is refreshed.
     */
    suspend fun onTokenRefreshed(newToken: String) {
        Log.d(TAG, "FCM token refreshed, registering with backend")
        encryptedPreferences.putString(KEY_FCM_TOKEN, newToken)
        registerTokenWithBackend(newToken)
    }

    /**
     * Register the FCM token with the Rhenti backend.
     */
    private suspend fun registerTokenWithBackend(fcmToken: String) {
        try {
            val userId = tokenManager.getUserId() ?: run {
                // Not logged in yet, store token for later registration
                encryptedPreferences.putString(KEY_FCM_TOKEN, fcmToken)
                Log.d(TAG, "User not authenticated, storing token for later")
                return
            }

            val superAccountId = tokenManager.getSuperAccountId() ?: return

            val previousToken = encryptedPreferences.getString(KEY_FCM_TOKEN)
            encryptedPreferences.putString(KEY_FCM_TOKEN, fcmToken)

            val request = mapOf(
                "token" to fcmToken,
                "platform" to "android",
                "userId" to userId,
                "superAccountId" to superAccountId
            )

            apiClient.registerDeviceToken(request)
            Log.d(TAG, "FCM token registered with backend")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register FCM token with backend", e)
        }
    }

    /**
     * Register a previously stored token after the user logs in.
     * Should be called after successful authentication.
     */
    suspend fun registerPendingToken() {
        val storedToken = encryptedPreferences.getString(KEY_FCM_TOKEN)
        if (storedToken != null) {
            registerTokenWithBackend(storedToken)
        } else {
            registerCurrentToken()
        }
    }

    /**
     * Unregister the device token when the user logs out.
     */
    suspend fun unregisterToken() {
        try {
            val fcmToken = encryptedPreferences.getString(KEY_FCM_TOKEN) ?: return
            val userId = tokenManager.getUserId() ?: return
            val superAccountId = tokenManager.getSuperAccountId() ?: return

            val request = mapOf(
                "token" to fcmToken,
                "platform" to "android",
                "userId" to userId,
                "superAccountId" to superAccountId
            )

            apiClient.unregisterDeviceToken(request)
            encryptedPreferences.remove(KEY_FCM_TOKEN)
            Log.d(TAG, "FCM token unregistered from backend")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to unregister FCM token", e)
        }
    }

    companion object {
        private const val TAG = "FCMTokenRepository"
        private const val KEY_FCM_TOKEN = "fcm_token"
    }
}
