package com.tomasronis.rhentiapp.data.notifications.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Request to register FCM token with backend.
 */
@JsonClass(generateAdapter = true)
data class FcmTokenRequest(
    @Json(name = "user_id")
    val userId: String,
    @Json(name = "super_account_id")
    val superAccountId: String,
    @Json(name = "fcm_token")
    val fcmToken: String,
    @Json(name = "platform")
    val platform: String,
    @Json(name = "device_id")
    val deviceId: String,
    @Json(name = "app_version")
    val appVersion: String
)

/**
 * Response from FCM token registration.
 */
@JsonClass(generateAdapter = true)
data class FcmTokenResponse(
    @Json(name = "success")
    val success: Boolean,
    @Json(name = "message")
    val message: String? = null
)

/**
 * Request to unregister FCM token.
 */
@JsonClass(generateAdapter = true)
data class FcmUnregisterRequest(
    @Json(name = "user_id")
    val userId: String,
    @Json(name = "fcm_token")
    val fcmToken: String
)
