package com.tomasronis.rhentiapp.data.notifications.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Request to register device for push notifications.
 * Matches the spec from push-notification-registration.md
 */
@JsonClass(generateAdapter = true)
data class DeviceRegistrationRequest(
    @Json(name = "device_id")
    val deviceId: String,
    @Json(name = "account")
    val account: String,
    @Json(name = "childAccount")
    val childAccount: String,
    @Json(name = "manufacturer")
    val manufacturer: String,
    @Json(name = "model")
    val model: String,
    @Json(name = "version")
    val version: String,
    @Json(name = "app_version")
    val appVersion: String,
    @Json(name = "name")
    val name: String,
    @Json(name = "token")
    val token: String,
    @Json(name = "ablepush")
    val ablepush: Boolean = true,
    @Json(name = "brand")
    val brand: String,
    @Json(name = "os")
    val os: String
)

/**
 * Response from device registration.
 */
@JsonClass(generateAdapter = true)
data class DeviceRegistrationResponse(
    @Json(name = "success")
    val success: Boolean,
    @Json(name = "message")
    val message: String? = null,
    @Json(name = "device_id")
    val deviceId: String? = null
)
