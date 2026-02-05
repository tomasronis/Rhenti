package com.tomasronis.rhentiapp.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for splash/loading screen.
 * Handles initialization and determines when to navigate to main screen.
 */
@HiltViewModel
class SplashViewModel @Inject constructor() : ViewModel() {

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        // Simulate app initialization
        viewModelScope.launch {
            // TODO: Add actual initialization logic here
            // - Check if user is logged in
            // - Load cached data
            // - Initialize services
            delay(2000) // Minimum splash duration for animation

            _isLoading.value = false
        }
    }
}
