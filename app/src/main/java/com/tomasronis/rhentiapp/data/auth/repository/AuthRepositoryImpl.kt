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
        // Save token and IDs
        tokenManager.saveAuthData(
            token = response.token,
            userId = response.userId,
            whiteLabel = response.whiteLabel,
            superAccountId = response.superAccountId
        )

        // Cache user data
        val cachedUser = CachedUser(
            id = response.user.id,
            email = response.user.email,
            firstName = response.user.firstName,
            lastName = response.user.lastName,
            phone = response.user.phone,
            profilePhotoUri = response.user.profilePhotoUri,
            createdAt = response.user.createdAt ?: System.currentTimeMillis(),
            updatedAt = response.user.updatedAt ?: System.currentTimeMillis()
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
                createdAt = it.createdAt,
                updatedAt = it.updatedAt
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
                            createdAt = it.createdAt,
                            updatedAt = it.updatedAt
                        )
                    }
                }.collect { emit(it) }
            } else {
                emit(null)
            }
        }
    }
}
