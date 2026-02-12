package com.tomasronis.rhentiapp.data.notifications.repository

import android.util.Log
import com.tomasronis.rhentiapp.BuildConfig
import com.tomasronis.rhentiapp.core.network.ApiClient
import com.tomasronis.rhentiapp.data.notifications.models.DeviceRegistrationRequest
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
        private const val RATE_LIMIT_MS = 5000L // 5 seconds
    }

    private var lastRegistrationTime = 0L

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

    override suspend fun registerDevice(
        deviceId: String,
        account: String,
        childAccount: String,
        manufacturer: String,
        model: String,
        version: String,
        appVersion: String,
        name: String,
        token: String,
        brand: String,
        os: String
    ): Result<Unit> {
        return try {
            // Rate limiting: prevent registration more than once per 5 seconds
            val currentTime = System.currentTimeMillis()
            val timeSinceLastRegistration = currentTime - lastRegistrationTime

            if (timeSinceLastRegistration < RATE_LIMIT_MS) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "⏱️ Rate limit: skipping registration (${timeSinceLastRegistration}ms since last attempt)")
                }
                return Result.success(Unit) // Skip but return success
            }

            lastRegistrationTime = currentTime

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "📡 Making API call to POST /devices/unauthorized")
            }

            val request = DeviceRegistrationRequest(
                deviceId = deviceId,
                account = account,
                childAccount = childAccount,
                manufacturer = manufacturer,
                model = model,
                version = version,
                appVersion = appVersion,
                name = name,
                token = token,
                ablepush = true,
                brand = brand,
                os = os
            )

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Request payload prepared, calling apiClient.registerDevice()...")
            }

            val response = apiClient.registerDevice(request)

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "API Response received: _id=${response.id}, device_id=${response.deviceId}")
            }

            // Check if registration was successful
            // API returns the created device document with _id field on success
            if (response.id != null || response.success == true) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "✅ Device registered successfully!")
                    Log.d(TAG, "   MongoDB ID: ${response.id}")
                    Log.d(TAG, "   Device ID: ${response.deviceId}")
                    Log.d(TAG, "   Created: ${response.createdAt}")
                }
                Result.success(Unit)
            } else {
                if (BuildConfig.DEBUG) {
                    Log.e(TAG, "❌ Device registration failed: ${response.message}")
                }
                Result.failure(Exception(response.message ?: "Unknown error"))
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "❌ Exception while registering device: ${e.message}", e)
            }
            Result.failure(e)
        }
    }

    override suspend fun signoutDevice(deviceId: String): Result<Unit> {
        return try {
            val response = apiClient.signoutDevice(deviceId)

            // Check if signout was successful
            if (response.id != null || response.success == true) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "✅ Device signed out successfully")
                }
                Result.success(Unit)
            } else {
                if (BuildConfig.DEBUG) {
                    Log.e(TAG, "❌ Device signout failed: ${response.message}")
                }
                Result.failure(Exception(response.message ?: "Unknown error"))
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Error signing out device", e)
            }
            Result.failure(e)
        }
    }
}
