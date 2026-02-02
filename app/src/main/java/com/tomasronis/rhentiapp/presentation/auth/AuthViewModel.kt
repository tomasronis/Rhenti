package com.tomasronis.rhentiapp.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomasronis.rhentiapp.data.auth.models.*
import com.tomasronis.rhentiapp.data.auth.repository.AuthRepository
import com.tomasronis.rhentiapp.data.auth.services.GoogleAuthException
import com.tomasronis.rhentiapp.data.auth.services.MicrosoftAuthException
import com.tomasronis.rhentiapp.core.network.NetworkResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthUiState(
    val isLoading: Boolean = false,
    val isAuthenticated: Boolean = false,
    val currentUser: User? = null,
    val error: AuthError? = null,
    val isEmailLoginLoading: Boolean = false,
    val isGoogleLoginLoading: Boolean = false,
    val isMicrosoftLoginLoading: Boolean = false,
    val registrationSuccess: Boolean = false,
    val forgotPasswordSuccess: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        observeAuthState()
        checkAuthStatus()
    }

    private fun observeAuthState() {
        viewModelScope.launch {
            authRepository.observeAuthState().collect { isAuth ->
                _uiState.update { it.copy(isAuthenticated = isAuth) }
            }
        }

        viewModelScope.launch {
            authRepository.observeCurrentUser().collect { user ->
                _uiState.update { it.copy(currentUser = user) }
            }
        }
    }

    private fun checkAuthStatus() {
        viewModelScope.launch {
            val isAuth = authRepository.isAuthenticated()
            val user = if (isAuth) authRepository.getCurrentUser() else null
            _uiState.update { it.copy(isAuthenticated = isAuth, currentUser = user) }
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isEmailLoginLoading = true, error = null) }

            when (val result = authRepository.login(email, password)) {
                is NetworkResult.Success -> {
                    // Auth state updates via observer
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(error = result.toAuthError()) }
                }
                is NetworkResult.Loading -> {
                    // Shouldn't happen in this flow
                }
            }

            _uiState.update { it.copy(isEmailLoginLoading = false) }
        }
    }

    fun loginWithGoogle(activity: android.app.Activity) {
        viewModelScope.launch {
            _uiState.update { it.copy(isGoogleLoginLoading = true, error = null) }

            when (val tokenResult = authRepository.getGoogleIdToken(activity)) {
                is NetworkResult.Success -> {
                    val (idToken, email) = tokenResult.data
                    when (val loginResult = authRepository.loginWithSSO(email, idToken, SSOProvider.GOOGLE)) {
                        is NetworkResult.Success -> {
                            // Success - auth state updates via observer
                        }
                        is NetworkResult.Error -> {
                            _uiState.update { it.copy(error = loginResult.toAuthError()) }
                        }
                        is NetworkResult.Loading -> {
                            // Shouldn't happen
                        }
                    }
                }
                is NetworkResult.Error -> {
                    if (tokenResult.exception !is GoogleAuthException.Cancelled) {
                        _uiState.update {
                            it.copy(error = AuthError.SSOFailed(SSOProvider.GOOGLE, tokenResult.exception.message ?: "Unknown error"))
                        }
                    }
                }
                is NetworkResult.Loading -> {
                    // Shouldn't happen
                }
            }

            _uiState.update { it.copy(isGoogleLoginLoading = false) }
        }
    }

    fun loginWithMicrosoft(activity: android.app.Activity) {
        viewModelScope.launch {
            _uiState.update { it.copy(isMicrosoftLoginLoading = true, error = null) }

            when (val tokenResult = authRepository.getMicrosoftAccessToken(activity)) {
                is NetworkResult.Success -> {
                    val (accessToken, email, _) = tokenResult.data
                    when (val loginResult = authRepository.loginWithSSO(email, accessToken, SSOProvider.MICROSOFT)) {
                        is NetworkResult.Success -> {
                            // Success - auth state updates via observer
                        }
                        is NetworkResult.Error -> {
                            _uiState.update { it.copy(error = loginResult.toAuthError()) }
                        }
                        is NetworkResult.Loading -> {
                            // Shouldn't happen
                        }
                    }
                }
                is NetworkResult.Error -> {
                    if (tokenResult.exception !is MicrosoftAuthException.Cancelled) {
                        _uiState.update {
                            it.copy(error = AuthError.SSOFailed(SSOProvider.MICROSOFT, tokenResult.exception.message ?: "Unknown error"))
                        }
                    }
                }
                is NetworkResult.Loading -> {
                    // Shouldn't happen
                }
            }

            _uiState.update { it.copy(isMicrosoftLoginLoading = false) }
        }
    }

    fun register(request: RegistrationRequest) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, registrationSuccess = false) }

            when (val result = authRepository.register(request)) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(registrationSuccess = true) }
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(error = result.toAuthError()) }
                }
                is NetworkResult.Loading -> {
                    // Shouldn't happen
                }
            }

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun forgotPassword(email: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, forgotPasswordSuccess = false) }

            when (val result = authRepository.forgotPassword(email)) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(forgotPasswordSuccess = true) }
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(error = result.toAuthError()) }
                }
                is NetworkResult.Loading -> {
                    // Shouldn't happen
                }
            }

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.clearAuthData()
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearRegistrationSuccess() {
        _uiState.update { it.copy(registrationSuccess = false) }
    }

    fun clearForgotPasswordSuccess() {
        _uiState.update { it.copy(forgotPasswordSuccess = false) }
    }
}

private fun <T> NetworkResult.Error<T>.toAuthError(): AuthError {
    return when {
        exception.message?.contains("401") == true -> AuthError.InvalidCredentials(exception.message ?: "Invalid credentials")
        exception.message?.contains("404") == true -> AuthError.UserNotFound
        exception.message?.contains("email already exists", ignoreCase = true) == true -> AuthError.EmailAlreadyExists
        exception.message?.contains("network", ignoreCase = true) == true -> AuthError.Network(exception.message ?: "Network error")
        else -> AuthError.Unknown(exception as? Throwable ?: Exception(exception.message))
    }
}
