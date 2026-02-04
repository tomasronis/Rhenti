package com.tomasronis.rhentiapp.data.auth.repository

import android.app.Activity
import com.tomasronis.rhentiapp.core.database.dao.UserDao
import com.tomasronis.rhentiapp.core.database.entities.CachedUser
import com.tomasronis.rhentiapp.core.network.ApiClient
import com.tomasronis.rhentiapp.core.network.NetworkResult
import com.tomasronis.rhentiapp.core.security.TokenManager
import com.tomasronis.rhentiapp.data.auth.models.*
import com.tomasronis.rhentiapp.data.auth.services.GoogleAuthException
import com.tomasronis.rhentiapp.data.auth.services.GoogleAuthService
import com.tomasronis.rhentiapp.data.auth.services.MicrosoftAuthException
import com.tomasronis.rhentiapp.data.auth.services.MicrosoftAuthService
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val apiClient: ApiClient,
    private val tokenManager: TokenManager,
    private val userDao: UserDao,
    private val googleAuthService: GoogleAuthService,
    private val microsoftAuthService: MicrosoftAuthService
) : AuthRepository {

    private val _authStateFlow = MutableStateFlow(false)

    override suspend fun login(email: String, password: String): NetworkResult<LoginResponse> {
        return try {
            val request = LoginRequest(email = email, password = password)

            if (com.tomasronis.rhentiapp.BuildConfig.DEBUG) {
                android.util.Log.d("AuthRepository", "Login attempt for email: $email")
            }

            val loginResponse = apiClient.login(request)

            if (com.tomasronis.rhentiapp.BuildConfig.DEBUG) {
                android.util.Log.d("AuthRepository", "Login successful for user: ${loginResponse.userId}")
            }

            saveAuthData(loginResponse)

            // Fetch full user profile
            fetchUserProfile(loginResponse.userId)

            // Fetch white label settings
            fetchWhiteLabelSettings()

            NetworkResult.Success(loginResponse)
        } catch (e: Exception) {
            if (com.tomasronis.rhentiapp.BuildConfig.DEBUG) {
                android.util.Log.e("AuthRepository", "Login failed", e)
                android.util.Log.e("AuthRepository", "Error message: ${e.message}")
                android.util.Log.e("AuthRepository", "Error type: ${e.javaClass.simpleName}")
            }
            NetworkResult.Error(
                exception = e,
                cachedData = null
            )
        }
    }

    override suspend fun register(request: RegistrationRequest): NetworkResult<Unit> {
        return try {
            apiClient.register(request)
            NetworkResult.Success(Unit)
        } catch (e: Exception) {
            if (com.tomasronis.rhentiapp.BuildConfig.DEBUG) {
                android.util.Log.e("AuthRepository", "Registration failed", e)
            }
            NetworkResult.Error(
                exception = e,
                cachedData = null
            )
        }
    }

    override suspend fun forgotPassword(email: String): NetworkResult<Unit> {
        return try {
            val request = ForgotPasswordRequest(email = email)
            apiClient.forgotPassword(request)
            NetworkResult.Success(Unit)
        } catch (e: Exception) {
            if (com.tomasronis.rhentiapp.BuildConfig.DEBUG) {
                android.util.Log.e("AuthRepository", "Forgot password failed", e)
            }
            NetworkResult.Error(
                exception = e,
                cachedData = null
            )
        }
    }

    override suspend fun loginWithSSO(
        email: String,
        token: String,
        provider: SSOProvider
    ): NetworkResult<LoginResponse> {
        return try {
            val request = SSOLoginRequest(
                email = email,
                token = token,
                provider = provider.value
            )

            if (com.tomasronis.rhentiapp.BuildConfig.DEBUG) {
                android.util.Log.d("AuthRepository", "SSO login attempt for email: $email, provider: ${provider.value}")
            }

            val loginResponse = apiClient.ssoLogin(request)

            if (com.tomasronis.rhentiapp.BuildConfig.DEBUG) {
                android.util.Log.d("AuthRepository", "SSO login successful for user: ${loginResponse.userId}")
            }

            saveAuthData(loginResponse)

            // Fetch full user profile
            fetchUserProfile(loginResponse.userId)

            // Fetch white label settings
            fetchWhiteLabelSettings()

            NetworkResult.Success(loginResponse)
        } catch (e: Exception) {
            if (com.tomasronis.rhentiapp.BuildConfig.DEBUG) {
                android.util.Log.e("AuthRepository", "SSO login failed", e)
            }
            NetworkResult.Error(
                exception = e,
                cachedData = null
            )
        }
    }

    override suspend fun getGoogleIdToken(activity: Activity): NetworkResult<Pair<String, String>> {
        return try {
            val result = googleAuthService.signIn(activity)
            if (result.isSuccess) {
                NetworkResult.Success(result.getOrThrow())
            } else {
                NetworkResult.Error(
                    exception = result.exceptionOrNull() as? Exception
                        ?: Exception("Google sign-in failed"),
                    cachedData = null
                )
            }
        } catch (e: GoogleAuthException.Cancelled) {
            NetworkResult.Error(
                exception = e,
                cachedData = null
            )
        } catch (e: Exception) {
            NetworkResult.Error(
                exception = e,
                cachedData = null
            )
        }
    }

    override suspend fun getMicrosoftAccessToken(activity: Activity): NetworkResult<Triple<String, String, String?>> {
        return try {
            val result = microsoftAuthService.signIn(activity)
            if (result.isSuccess) {
                NetworkResult.Success(result.getOrThrow())
            } else {
                NetworkResult.Error(
                    exception = result.exceptionOrNull() as? Exception
                        ?: Exception("Microsoft sign-in failed"),
                    cachedData = null
                )
            }
        } catch (e: MicrosoftAuthException.Cancelled) {
            NetworkResult.Error(
                exception = e,
                cachedData = null
            )
        } catch (e: Exception) {
            NetworkResult.Error(
                exception = e,
                cachedData = null
            )
        }
    }

    override suspend fun saveAuthData(response: LoginResponse) {
        // Save token, IDs, and user name
        tokenManager.saveAuthData(
            token = response.token,
            userId = response.userId,
            whiteLabel = response.whiteLabel,
            superAccountId = response.superAccountId,
            firstName = response.profile.firstName ?: "",
            lastName = response.profile.lastName ?: "",
            email = response.profile.email,
            account = response.account,
            childAccount = response.childAccount
        )

        // Cache user data
        val cachedUser = CachedUser(
            id = response.profile.id,
            email = response.profile.email,
            firstName = response.profile.firstName,
            lastName = response.profile.lastName,
            phone = response.profile.phone,
            profilePhotoUri = response.profile.profilePhotoUri,
            createdAt = parseIsoDateToTimestamp(response.profile.createdAt),
            updatedAt = parseIsoDateToTimestamp(response.profile.updatedAt)
        )
        userDao.insertUser(cachedUser)

        // Update auth state
        _authStateFlow.value = true
    }

    override suspend fun isAuthenticated(): Boolean {
        return tokenManager.isAuthenticated()
    }

    override suspend fun getCurrentUser(): User? {
        val userId = tokenManager.getUserId() ?: return null
        val cachedUser = userDao.getUserById(userId).first()
        return cachedUser?.let {
            User(
                id = it.id,
                email = it.email,
                firstName = it.firstName,
                lastName = it.lastName,
                phone = it.phone,
                profilePhotoUri = it.profilePhotoUri,
                createdAt = timestampToIsoDate(it.createdAt),
                updatedAt = timestampToIsoDate(it.updatedAt)
            )
        }
    }

    override suspend fun clearAuthData() {
        // Sign out from SSO providers
        googleAuthService.signOut()
        microsoftAuthService.signOut()

        // Clear token manager
        tokenManager.clearAuthData()

        // Clear cached user
        userDao.deleteAllUsers()

        // Update auth state
        _authStateFlow.value = false
    }

    override fun observeAuthState(): Flow<Boolean> {
        return _authStateFlow.asStateFlow()
    }

    override fun observeCurrentUser(): Flow<User?> {
        return flow {
            val userId = tokenManager.getUserId()
            if (userId != null) {
                userDao.getUserById(userId).map { cachedUser ->
                    cachedUser?.let {
                        User(
                            id = it.id,
                            email = it.email,
                            firstName = it.firstName,
                            lastName = it.lastName,
                            phone = it.phone,
                            profilePhotoUri = it.profilePhotoUri,
                            createdAt = timestampToIsoDate(it.createdAt),
                            updatedAt = timestampToIsoDate(it.updatedAt)
                        )
                    }
                }.collect { emit(it) }
            } else {
                emit(null)
            }
        }
    }

    private fun parseIsoDateToTimestamp(isoDate: String?): Long {
        if (isoDate == null) return System.currentTimeMillis()
        return try {
            java.time.Instant.parse(isoDate).toEpochMilli()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }

    private fun timestampToIsoDate(timestamp: Long): String {
        return try {
            java.time.Instant.ofEpochMilli(timestamp).toString()
        } catch (e: Exception) {
            java.time.Instant.now().toString()
        }
    }

    /**
     * Fetch full user profile from server and update cache.
     * Called after successful login to ensure we have complete user data.
     */
    private suspend fun fetchUserProfile(userId: String) {
        try {
            if (com.tomasronis.rhentiapp.BuildConfig.DEBUG) {
                android.util.Log.d("AuthRepository", "Fetching user profile for userId: $userId")
            }

            val userProfileResponse = apiClient.getUserProfile(userId)

            if (com.tomasronis.rhentiapp.BuildConfig.DEBUG) {
                android.util.Log.d("AuthRepository", "User profile response: $userProfileResponse")
            }

            // Parse user profile response
            val id = userProfileResponse["_id"] as? String ?: userId
            val email = userProfileResponse["email"] as? String ?: ""
            val firstName = userProfileResponse["firstName"] as? String
            val lastName = userProfileResponse["lastName"] as? String
            val phone = userProfileResponse["phone"] as? String
            val profilePhotoUri = userProfileResponse["profilePhotoUri"] as? String
            val createdAt = userProfileResponse["createdAt"] as? String
            val updatedAt = userProfileResponse["updatedAt"] as? String

            // Update cached user with complete profile data
            val cachedUser = CachedUser(
                id = id,
                email = email,
                firstName = firstName,
                lastName = lastName,
                phone = phone,
                profilePhotoUri = profilePhotoUri,
                createdAt = parseIsoDateToTimestamp(createdAt),
                updatedAt = parseIsoDateToTimestamp(updatedAt)
            )
            userDao.insertUser(cachedUser)

            // Update name in token manager if we got it
            if (firstName != null || lastName != null) {
                tokenManager.saveUserFirstName(firstName ?: "")
                tokenManager.saveUserLastName(lastName ?: "")
            }

            if (com.tomasronis.rhentiapp.BuildConfig.DEBUG) {
                android.util.Log.d("AuthRepository", "User profile updated successfully")
            }
        } catch (e: Exception) {
            // Don't fail login if profile fetch fails - we have basic data from login response
            if (com.tomasronis.rhentiapp.BuildConfig.DEBUG) {
                android.util.Log.w("AuthRepository", "Failed to fetch user profile (non-critical)", e)
            }
        }
    }

    /**
     * Fetch white label settings from server.
     * This includes app customization, branding, and feature flags.
     */
    private suspend fun fetchWhiteLabelSettings() {
        try {
            if (com.tomasronis.rhentiapp.BuildConfig.DEBUG) {
                android.util.Log.d("AuthRepository", "Fetching white label settings")
            }

            val settingsResponse = apiClient.getWhiteLabelSettings()

            if (com.tomasronis.rhentiapp.BuildConfig.DEBUG) {
                android.util.Log.d("AuthRepository", "White label settings response: $settingsResponse")
            }

            // TODO: Parse and store settings as needed
            // For now, just log that we received them
            // You can add PreferencesManager or SettingsRepository to store these later

        } catch (e: Exception) {
            // Don't fail login if settings fetch fails - use defaults
            if (com.tomasronis.rhentiapp.BuildConfig.DEBUG) {
                android.util.Log.w("AuthRepository", "Failed to fetch white label settings (non-critical)", e)
            }
        }
    }
}
