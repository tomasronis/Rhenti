package com.tomasronis.rhentiapp.data.profile.repository

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.tomasronis.rhentiapp.BuildConfig
import com.tomasronis.rhentiapp.core.database.dao.UserDao
import com.tomasronis.rhentiapp.core.database.entities.CachedUser
import com.tomasronis.rhentiapp.core.network.ApiClient
import com.tomasronis.rhentiapp.core.network.NetworkResult
import com.tomasronis.rhentiapp.core.security.TokenManager
import com.tomasronis.rhentiapp.data.auth.models.User
import com.tomasronis.rhentiapp.data.profile.models.AppSettings
import com.tomasronis.rhentiapp.data.profile.models.DarkModePreference
import com.tomasronis.rhentiapp.data.profile.models.PasswordChangeRequest
import com.tomasronis.rhentiapp.data.profile.models.PasswordChangeResponse
import com.tomasronis.rhentiapp.data.profile.models.ProfileUpdateRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

@Singleton
class UserProfileRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiClient: ApiClient,
    private val userDao: UserDao,
    private val tokenManager: TokenManager
) : UserProfileRepository {

    private val settingsDataStore = context.settingsDataStore

    companion object {
        private const val TAG = "UserProfileRepository"

        // DataStore keys
        private val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        private val CALL_NOTIFICATIONS_ENABLED = booleanPreferencesKey("call_notifications_enabled")
        private val MESSAGE_NOTIFICATIONS_ENABLED = booleanPreferencesKey("message_notifications_enabled")
        private val DARK_MODE = stringPreferencesKey("dark_mode")
    }

    override fun observeCurrentUser(): Flow<CachedUser?> {
        return flow {
            val userId = tokenManager.getUserId()
            if (userId != null) {
                userDao.getUserById(userId).collect { user ->
                    emit(user)
                }
            } else {
                emit(null)
            }
        }
    }

    override suspend fun getUserProfile(userId: String): NetworkResult<User> {
        return try {
            val response = apiClient.getUserProfile(userId)

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Get user profile response: $response")
            }

            val user = parseUserResponse(response)

            // Cache user in Room
            cacheUser(user)

            NetworkResult.Success(user)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Failed to get user profile", e)
            }

            // Try to return cached user
            val cachedUser = userDao.getUserById(userId).first()
            val user = cachedUser?.let { convertCachedUserToUser(it) }

            NetworkResult.Error(
                exception = e,
                cachedData = user
            )
        }
    }

    override suspend fun updateProfile(
        userId: String,
        firstName: String?,
        lastName: String?,
        email: String?,
        phone: String?
    ): NetworkResult<User> {
        return try {
            val request: Map<String, Any> = mapOf(
                "id" to userId,
                "firstName" to (firstName ?: ""),
                "lastName" to (lastName ?: ""),
                "email" to (email ?: ""),
                "phone" to (phone ?: "")
            )

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Update profile request: $request")
            }

            val response = apiClient.updateUserProfile(request)

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Update profile response: $response")
            }

            val user = parseUserResponse(response)

            // Update cache
            cacheUser(user)

            NetworkResult.Success(user)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Failed to update profile", e)
            }

            NetworkResult.Error(
                exception = e,
                cachedData = null
            )
        }
    }

    override suspend fun uploadProfilePhoto(userId: String, imageBase64: String): NetworkResult<User> {
        return try {
            val request = mapOf(
                "id" to userId,
                "profilePhotoBase64" to imageBase64
            )

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Upload profile photo for user: $userId")
            }

            val response = apiClient.updateUserProfile(request)

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Upload profile photo response: $response")
            }

            val user = parseUserResponse(response)

            // Update cache
            cacheUser(user)

            NetworkResult.Success(user)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Failed to upload profile photo", e)
            }

            NetworkResult.Error(
                exception = e,
                cachedData = null
            )
        }
    }

    override suspend fun changePassword(
        userId: String,
        currentPassword: String,
        newPassword: String
    ): NetworkResult<Boolean> {
        return try {
            val request = mapOf(
                "userId" to userId,
                "currentPassword" to currentPassword,
                "newPassword" to newPassword
            )

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Change password for user: $userId")
            }

            val response = apiClient.updateUserProfile(request)

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Change password response: $response")
            }

            // Check if response indicates success
            val success = response["success"] as? Boolean ?: true

            NetworkResult.Success(success)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Failed to change password", e)
            }

            NetworkResult.Error(
                exception = e,
                cachedData = null
            )
        }
    }

    override suspend fun getAppSettings(): AppSettings {
        return settingsDataStore.data.map { preferences ->
            AppSettings(
                notificationsEnabled = preferences[NOTIFICATIONS_ENABLED] ?: true,
                callNotificationsEnabled = preferences[CALL_NOTIFICATIONS_ENABLED] ?: true,
                messageNotificationsEnabled = preferences[MESSAGE_NOTIFICATIONS_ENABLED] ?: true,
                darkMode = DarkModePreference.valueOf(
                    preferences[DARK_MODE] ?: DarkModePreference.SYSTEM.name
                )
            )
        }.first()
    }

    override suspend fun saveAppSettings(settings: AppSettings) {
        settingsDataStore.edit { preferences ->
            preferences[NOTIFICATIONS_ENABLED] = settings.notificationsEnabled
            preferences[CALL_NOTIFICATIONS_ENABLED] = settings.callNotificationsEnabled
            preferences[MESSAGE_NOTIFICATIONS_ENABLED] = settings.messageNotificationsEnabled
            preferences[DARK_MODE] = settings.darkMode.name
        }

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "App settings saved: $settings")
        }
    }

    private fun parseUserResponse(response: Map<String, Any>): User {
        // Support both snake_case and camelCase
        val id = (response["id"] ?: response["_id"]) as? String ?: ""
        val email = response["email"] as? String ?: ""
        val firstName = (response["firstName"] ?: response["first_name"]) as? String
        val lastName = (response["lastName"] ?: response["last_name"]) as? String
        val phone = response["phone"] as? String
        val profilePhotoUri = (response["profilePhotoUri"] ?: response["profile_photo_uri"]) as? String
        val createdAt = (response["createdAt"] ?: response["created_at"]) as? String ?: ""
        val updatedAt = (response["updatedAt"] ?: response["updated_at"]) as? String ?: ""

        return User(
            id = id,
            email = email,
            firstName = firstName,
            lastName = lastName,
            phone = phone,
            profilePhotoUri = profilePhotoUri,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    private suspend fun cacheUser(user: User) {
        val cachedUser = CachedUser(
            id = user.id,
            email = user.email,
            firstName = user.firstName,
            lastName = user.lastName,
            phone = user.phone,
            profilePhotoUri = user.profilePhotoUri,
            createdAt = parseIsoDateToTimestamp(user.createdAt),
            updatedAt = parseIsoDateToTimestamp(user.updatedAt)
        )
        userDao.insertUser(cachedUser)
    }

    private fun convertCachedUserToUser(cachedUser: CachedUser): User {
        return User(
            id = cachedUser.id,
            email = cachedUser.email,
            firstName = cachedUser.firstName,
            lastName = cachedUser.lastName,
            phone = cachedUser.phone,
            profilePhotoUri = cachedUser.profilePhotoUri,
            createdAt = timestampToIsoDate(cachedUser.createdAt),
            updatedAt = timestampToIsoDate(cachedUser.updatedAt)
        )
    }

    private fun parseIsoDateToTimestamp(isoDate: String?): Long {
        if (isoDate.isNullOrBlank()) return System.currentTimeMillis()

        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            format.timeZone = TimeZone.getTimeZone("UTC")
            format.parse(isoDate)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Failed to parse ISO date: $isoDate", e)
            }
            System.currentTimeMillis()
        }
    }

    private fun timestampToIsoDate(timestamp: Long): String {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
        format.timeZone = TimeZone.getTimeZone("UTC")
        return format.format(Date(timestamp))
    }
}
