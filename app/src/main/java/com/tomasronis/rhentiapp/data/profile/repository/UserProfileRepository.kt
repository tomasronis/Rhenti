package com.tomasronis.rhentiapp.data.profile.repository

import com.tomasronis.rhentiapp.core.database.entities.CachedUser
import com.tomasronis.rhentiapp.data.auth.models.User
import com.tomasronis.rhentiapp.data.profile.models.AppSettings
import com.tomasronis.rhentiapp.data.profile.models.PasswordChangeRequest
import com.tomasronis.rhentiapp.data.profile.models.ProfileUpdateRequest
import com.tomasronis.rhentiapp.core.network.NetworkResult
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing user profile and app settings
 */
interface UserProfileRepository {
    /**
     * Observe the current user profile
     */
    fun observeCurrentUser(): Flow<CachedUser?>

    /**
     * Get user profile by ID
     */
    suspend fun getUserProfile(userId: String): NetworkResult<User>

    /**
     * Update user profile
     */
    suspend fun updateProfile(
        userId: String,
        firstName: String?,
        lastName: String?,
        email: String?,
        phone: String?
    ): NetworkResult<User>

    /**
     * Upload profile photo
     */
    suspend fun uploadProfilePhoto(userId: String, imageBase64: String): NetworkResult<User>

    /**
     * Change password
     */
    suspend fun changePassword(
        userId: String,
        currentPassword: String,
        newPassword: String
    ): NetworkResult<Boolean>

    /**
     * Get app settings
     */
    suspend fun getAppSettings(): AppSettings

    /**
     * Save app settings
     */
    suspend fun saveAppSettings(settings: AppSettings)
}
