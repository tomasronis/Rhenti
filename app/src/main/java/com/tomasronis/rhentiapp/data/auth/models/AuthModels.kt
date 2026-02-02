package com.tomasronis.rhentiapp.data.auth.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.tomasronis.rhentiapp.BuildConfig

@JsonClass(generateAdapter = true)
data class LoginRequest(
    val email: String,
    val password: String,
    @Json(name = "white_label") val whiteLabel: String = BuildConfig.WHITE_LABEL
)

@JsonClass(generateAdapter = true)
data class SSOLoginRequest(
    val email: String,
    val token: String,
    val provider: String, // "google" or "microsoft"
    @Json(name = "white_label") val whiteLabel: String = BuildConfig.WHITE_LABEL
)

@JsonClass(generateAdapter = true)
data class RegistrationRequest(
    val firstName: String,
    val lastName: String,
    val email: String,
    val phone: String,
    val password: String,
    val confirmPassword: String,
    val role: String = "owner",
    @Json(name = "white_label") val whiteLabel: String = BuildConfig.WHITE_LABEL
)

@JsonClass(generateAdapter = true)
data class ForgotPasswordRequest(
    val email: String
)

@JsonClass(generateAdapter = true)
data class LoginResponse(
    val token: String,
    val userId: String,  // API returns camelCase
    @Json(name = "super_account_id") val superAccountId: String,
    val whiteLabel: String,  // API returns camelCase
    val profile: User  // API returns user data in "profile" field
)

@JsonClass(generateAdapter = true)
data class User(
    @Json(name = "_id") val id: String,
    val email: String,
    val firstName: String?,
    val lastName: String?,
    val phone: String?,
    val profilePhotoUri: String?,
    val createdAt: String?,  // API returns ISO 8601 string, not Unix timestamp
    val updatedAt: String?   // API returns ISO 8601 string, not Unix timestamp
)

enum class SSOProvider(val value: String) {
    GOOGLE("google"),
    MICROSOFT("microsoft")
}

sealed class AuthError {
    data class Network(val message: String) : AuthError()
    data class InvalidCredentials(val message: String) : AuthError()
    data class SSOCancelled(val provider: SSOProvider) : AuthError()
    data class SSOFailed(val provider: SSOProvider, val message: String) : AuthError()
    data object UserNotFound : AuthError()
    data object EmailAlreadyExists : AuthError()
    data class Unknown(val throwable: Throwable) : AuthError()
}
