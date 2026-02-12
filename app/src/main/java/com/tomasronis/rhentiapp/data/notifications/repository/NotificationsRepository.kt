package com.tomasronis.rhentiapp.data.notifications.repository

/**
 * Repository for managing FCM token registration and notifications.
 */
interface NotificationsRepository {

    /**
     * Register FCM token with backend.
     */
    suspend fun registerFcmToken(
        userId: String,
        superAccountId: String,
        fcmToken: String,
        platform: String,
        deviceId: String,
        appVersion: String
    ): Result<Unit>

    /**
     * Unregister FCM token from backend.
     */
    suspend fun unregisterFcmToken(
        userId: String,
        fcmToken: String
    ): Result<Unit>
}
