package com.tomasronis.rhentiapp.data.auth.repository

import com.tomasronis.rhentiapp.core.network.NetworkResult
import com.tomasronis.rhentiapp.data.auth.models.*
import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    suspend fun login(email: String, password: String): NetworkResult<LoginResponse>
    suspend fun register(request: RegistrationRequest): NetworkResult<Unit>
    suspend fun forgotPassword(email: String): NetworkResult<Unit>
    suspend fun loginWithSSO(email: String, token: String, provider: SSOProvider): NetworkResult<LoginResponse>
    suspend fun getGoogleIdToken(activity: android.app.Activity): NetworkResult<Pair<String, String>>
    suspend fun getMicrosoftAccessToken(activity: android.app.Activity): NetworkResult<Triple<String, String, String?>>
    suspend fun saveAuthData(response: LoginResponse)
    suspend fun isAuthenticated(): Boolean
    suspend fun getCurrentUser(): User?
    suspend fun clearAuthData()
    fun observeAuthState(): Flow<Boolean>
    fun observeCurrentUser(): Flow<User?>
}
