package com.tomasronis.rhentiapp.presentation.properties

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomasronis.rhentiapp.core.network.NetworkResult
import com.tomasronis.rhentiapp.data.properties.models.Property
import com.tomasronis.rhentiapp.data.properties.repository.PropertiesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for managing properties.
 * Provides properties list and refresh functionality.
 */
@HiltViewModel
class PropertiesViewModel @Inject constructor(
    private val repository: PropertiesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PropertiesUiState())
    val uiState: StateFlow<PropertiesUiState> = _uiState.asStateFlow()

    // Observable properties from Room database
    val properties: StateFlow<List<Property>> = repository.observeProperties()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Load properties on init
        refreshProperties()
    }

    /**
     * Refresh properties from API.
     */
    fun refreshProperties() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = repository.getProperties()) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = null
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.exception.message ?: "Failed to load properties"
                        )
                    }
                }
                is NetworkResult.Loading -> {
                    _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    /**
     * Get a specific property by ID.
     */
    fun getProperty(propertyId: String, onResult: (Property?) -> Unit) {
        viewModelScope.launch {
            when (val result = repository.getProperty(propertyId)) {
                is NetworkResult.Success -> {
                    onResult(result.data)
                }
                is NetworkResult.Error -> {
                    onResult(null)
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    /**
     * Clear error message.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Clear cache.
     */
    fun clearCache() {
        viewModelScope.launch {
            repository.clearCache()
            refreshProperties()
        }
    }
}

/**
 * UI state for properties screen.
 */
data class PropertiesUiState(
    val isLoading: Boolean = false,
    val error: String? = null
)
