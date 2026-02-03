package com.tomasronis.rhentiapp.presentation.calls.active

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomasronis.rhentiapp.core.voip.CallState
import com.tomasronis.rhentiapp.core.voip.TwilioManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

/**
 * UI state for active call screen
 */
data class ActiveCallUiState(
    val isMuted: Boolean = false,
    val isSpeakerOn: Boolean = false,
    val isKeypadVisible: Boolean = false
)

/**
 * ViewModel for ActiveCallScreen
 */
@HiltViewModel
class ActiveCallViewModel @Inject constructor(
    private val twilioManager: TwilioManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActiveCallUiState())
    val uiState: StateFlow<ActiveCallUiState> = _uiState.asStateFlow()

    // Observe call state from TwilioManager
    val callState = twilioManager.callState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CallState.Idle
        )

    fun endCall() {
        twilioManager.endCall()
    }

    fun toggleMute() {
        val newMuteState = twilioManager.toggleMute()
        _uiState.update { it.copy(isMuted = newMuteState) }
    }

    fun toggleSpeaker() {
        val newSpeakerState = twilioManager.toggleSpeaker()
        _uiState.update { it.copy(isSpeakerOn = newSpeakerState) }
    }

    fun showKeypad() {
        _uiState.update { it.copy(isKeypadVisible = true) }
    }

    fun hideKeypad() {
        _uiState.update { it.copy(isKeypadVisible = false) }
    }

    fun sendDigits(digits: String) {
        twilioManager.sendDigits(digits)
    }
}

// Extension function for updating StateFlow
private fun <T> MutableStateFlow<T>.update(function: (T) -> T) {
    value = function(value)
}
