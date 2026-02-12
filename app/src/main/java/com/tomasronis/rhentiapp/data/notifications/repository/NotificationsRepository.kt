package com.tomasronis.rhentiapp.data.notifications.repository

/**
 * Repository for managing FCM token registration and notifications.
 */
interface NotificationsRepository {

    /**
     * Register FCM token with backend (legacy endpoint).
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
     * Unregister FCM token from backend (legacy endpoint).
     */
    suspend fun unregisterFcmToken(
        userId: String,
        fcmToken: String
    ): Result<Unit>

    /**
     * Register device with full metadata (new spec).
     */
    suspend fun registerDevice(
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
    ): Result<Unit>

    /**
     * Sign out device from push notifications.
     */
    suspend fun signoutDevice(deviceId: String): Result<Unit>
}
