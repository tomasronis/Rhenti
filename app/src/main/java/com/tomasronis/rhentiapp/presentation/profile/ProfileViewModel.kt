package com.tomasronis.rhentiapp.presentation.profile

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomasronis.rhentiapp.BuildConfig
import com.tomasronis.rhentiapp.core.network.NetworkResult
import com.tomasronis.rhentiapp.core.security.TokenManager
import com.tomasronis.rhentiapp.data.auth.repository.AuthRepository
import com.tomasronis.rhentiapp.data.auth.models.User
import com.tomasronis.rhentiapp.data.profile.models.AppSettings
import com.tomasronis.rhentiapp.data.profile.repository.UserProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

// Extension function for updating StateFlow
private fun <T> MutableStateFlow<T>.update(function: (T) -> T) {
    value = function(value)
}

/**
 * UI state for ProfileScreen
 */
data class ProfileUiState(
    val isLoading: Boolean = false,
    val isEditing: Boolean = false,
    val isUploadingPhoto: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val settings: AppSettings = AppSettings()
)

/**
 * ViewModel for ProfileScreen
 */
@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: UserProfileRepository,
    private val authRepository: AuthRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    // Observe current user from database
    val currentUser = profileRepository.observeCurrentUser()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    companion object {
        private const val TAG = "ProfileViewModel"
    }

    init {
        loadSettings()
    }

    fun loadProfile() {
        viewModelScope.launch {
            val userId = tokenManager.getUserId() ?: return@launch

            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = profileRepository.getUserProfile(userId)) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(isLoading = false)
                    }
                }
                is NetworkResult.Error -> {
                    if (BuildConfig.DEBUG) {
                        Log.e(TAG, "Failed to load profile", result.exception)
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.exception?.message ?: "Failed to load profile"
                        )
                    }
                }
                is NetworkResult.Loading -> {
                    // Still loading
                }
            }
        }
    }

    fun updateProfile(
        firstName: String?,
        lastName: String?,
        email: String?,
        phone: String?
    ) {
        viewModelScope.launch {
            val userId = tokenManager.getUserId() ?: return@launch

            _uiState.update { it.copy(isLoading = true, error = null, successMessage = null) }

            when (val result = profileRepository.updateProfile(userId, firstName, lastName, email, phone)) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isEditing = false,
                            successMessage = "Profile updated successfully"
                        )
                    }
                }
                is NetworkResult.Error -> {
                    if (BuildConfig.DEBUG) {
                        Log.e(TAG, "Failed to update profile", result.exception)
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.exception?.message ?: "Failed to update profile"
                        )
                    }
                }
                is NetworkResult.Loading -> {
                    // Still loading
                }
            }
        }
    }

    fun uploadPhoto(imageBase64: String) {
        viewModelScope.launch {
            val userId = tokenManager.getUserId() ?: return@launch

            _uiState.update { it.copy(isUploadingPhoto = true, error = null, successMessage = null) }

            when (val result = profileRepository.uploadProfilePhoto(userId, imageBase64)) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isUploadingPhoto = false,
                            successMessage = "Profile photo updated successfully"
                        )
                    }
                }
                is NetworkResult.Error -> {
                    if (BuildConfig.DEBUG) {
                        Log.e(TAG, "Failed to upload photo", result.exception)
                    }
                    _uiState.update {
                        it.copy(
                            isUploadingPhoto = false,
                            error = result.exception?.message ?: "Failed to upload photo"
                        )
                    }
                }
                is NetworkResult.Loading -> {
                    // Still loading
                }
            }
        }
    }

    fun changePassword(currentPassword: String, newPassword: String) {
        viewModelScope.launch {
            val userId = tokenManager.getUserId() ?: return@launch

            _uiState.update { it.copy(isLoading = true, error = null, successMessage = null) }

            when (val result = profileRepository.changePassword(userId, currentPassword, newPassword)) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            successMessage = "Password changed successfully"
                        )
                    }
                }
                is NetworkResult.Error -> {
                    if (BuildConfig.DEBUG) {
                        Log.e(TAG, "Failed to change password", result.exception)
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.exception?.message ?: "Failed to change password"
                        )
                    }
                }
                is NetworkResult.Loading -> {
                    // Still loading
                }
            }
        }
    }

    fun loadSettings() {
        viewModelScope.launch {
            val settings = profileRepository.getAppSettings()
            _uiState.update { it.copy(settings = settings) }
        }
    }

    fun saveSettings(settings: AppSettings) {
        viewModelScope.launch {
            profileRepository.saveAppSettings(settings)
            _uiState.update { it.copy(settings = settings, successMessage = "Settings saved") }
        }
    }

    fun setEditing(editing: Boolean) {
        _uiState.update { it.copy(isEditing = editing) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearSuccessMessage() {
        _uiState.update { it.copy(successMessage = null) }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.clearAuthData()
        }
    }
}
