package com.tomasronis.rhentiapp.data.profile.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Request model for updating user profile
 */
@JsonClass(generateAdapter = true)
data class ProfileUpdateRequest(
    @Json(name = "firstName") val firstName: String?,
    @Json(name = "lastName") val lastName: String?,
    @Json(name = "email") val email: String?,
    @Json(name = "phone") val phone: String?,
    @Json(name = "profilePhotoBase64") val profilePhotoBase64: String?
)

/**
 * Request model for changing password
 */
@JsonClass(generateAdapter = true)
data class PasswordChangeRequest(
    @Json(name = "currentPassword") val currentPassword: String,
    @Json(name = "newPassword") val newPassword: String
)

/**
 * Response model for password change
 */
@JsonClass(generateAdapter = true)
data class PasswordChangeResponse(
    @Json(name = "success") val success: Boolean,
    @Json(name = "message") val message: String?
)

/**
 * App settings model
 */
data class AppSettings(
    val notificationsEnabled: Boolean = true,
    val callNotificationsEnabled: Boolean = true,
    val messageNotificationsEnabled: Boolean = true,
    val darkMode: DarkModePreference = DarkModePreference.SYSTEM
)

/**
 * Dark mode preference
 */
enum class DarkModePreference {
    LIGHT,
    DARK,
    SYSTEM
}
