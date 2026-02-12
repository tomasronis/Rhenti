package com.tomasronis.rhentiapp.data.notifications.repository

import android.util.Log
import com.tomasronis.rhentiapp.BuildConfig
import com.tomasronis.rhentiapp.core.network.ApiClient
import com.tomasronis.rhentiapp.data.notifications.models.FcmTokenRequest
import com.tomasronis.rhentiapp.data.notifications.models.FcmUnregisterRequest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of NotificationsRepository.
 */
@Singleton
class NotificationsRepositoryImpl @Inject constructor(
    private val apiClient: ApiClient
) : NotificationsRepository {

    companion object {
        private const val TAG = "NotificationsRepository"
    }

    override suspend fun registerFcmToken(
        userId: String,
        superAccountId: String,
        fcmToken: String,
        platform: String,
        deviceId: String,
        appVersion: String
    ): Result<Unit> {
        return try {
            val request = FcmTokenRequest(
                userId = userId,
                superAccountId = superAccountId,
                fcmToken = fcmToken,
                platform = platform,
                deviceId = deviceId,
                appVersion = appVersion
            )

            val response = apiClient.registerFcmToken(request)

            if (response.success) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "FCM token registered: ${response.message}")
                }
                Result.success(Unit)
            } else {
                if (BuildConfig.DEBUG) {
                    Log.e(TAG, "FCM token registration failed: ${response.message}")
                }
                Result.failure(Exception(response.message ?: "Unknown error"))
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Error registering FCM token", e)
            }
            Result.failure(e)
        }
    }

    override suspend fun unregisterFcmToken(
        userId: String,
        fcmToken: String
    ): Result<Unit> {
        return try {
            val request = FcmUnregisterRequest(
                userId = userId,
                fcmToken = fcmToken
            )

            val response = apiClient.unregisterFcmToken(request)

            if (response.success) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "FCM token unregistered: ${response.message}")
                }
                Result.success(Unit)
            } else {
                if (BuildConfig.DEBUG) {
                    Log.e(TAG, "FCM token unregistration failed: ${response.message}")
                }
                Result.failure(Exception(response.message ?: "Unknown error"))
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Error unregistering FCM token", e)
            }
            Result.failure(e)
        }
    }
}
