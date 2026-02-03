package com.tomasronis.rhentiapp.presentation.calls

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomasronis.rhentiapp.BuildConfig
import com.tomasronis.rhentiapp.core.network.NetworkResult
import com.tomasronis.rhentiapp.core.security.TokenManager
import com.tomasronis.rhentiapp.data.calls.models.CallFilter
import com.tomasronis.rhentiapp.data.calls.models.CallLog
import com.tomasronis.rhentiapp.data.calls.models.CallType
import com.tomasronis.rhentiapp.data.calls.repository.CallsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * UI state for CallsScreen
 */
data class CallsUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val selectedFilter: CallType? = null,
    val searchQuery: String = "",
    val startDate: Long? = null,
    val endDate: Long? = null
)

/**
 * ViewModel for CallsScreen
 */
@HiltViewModel
class CallsViewModel @Inject constructor(
    private val callsRepository: CallsRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CallsUiState())
    val uiState: StateFlow<CallsUiState> = _uiState.asStateFlow()

    private val _currentFilter = MutableStateFlow(CallFilter())

    // Observe filtered call logs from repository
    val callLogs: StateFlow<List<CallLog>> = _currentFilter
        .flatMapLatest { filter ->
            callsRepository.observeFilteredCallLogs(filter)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    companion object {
        private const val TAG = "CallsViewModel"
    }

    init {
        loadCallLogs()
    }

    fun loadCallLogs() {
        viewModelScope.launch {
            val superAccountId = tokenManager.getSuperAccountId() ?: return@launch

            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = callsRepository.getCallLogs(superAccountId)) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                }
                is NetworkResult.Error -> {
                    if (BuildConfig.DEBUG) {
                        Log.e(TAG, "Failed to load call logs", result.exception)
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = if (result.cachedData.isNullOrEmpty()) {
                                result.exception?.message ?: "Failed to load call logs"
                            } else {
                                null // Don't show error if we have cached data
                            }
                        )
                    }
                }
                is NetworkResult.Loading -> {
                    // Still loading, keep showing progress
                }
            }
        }
    }

    fun refreshCallLogs() {
        viewModelScope.launch {
            val superAccountId = tokenManager.getSuperAccountId() ?: return@launch

            _uiState.update { it.copy(isRefreshing = true, error = null) }

            when (val result = callsRepository.getCallLogs(superAccountId)) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(isRefreshing = false) }
                }
                is NetworkResult.Error -> {
                    if (BuildConfig.DEBUG) {
                        Log.e(TAG, "Failed to refresh call logs", result.exception)
                    }
                    _uiState.update {
                        it.copy(
                            isRefreshing = false,
                            error = result.exception?.message ?: "Failed to refresh call logs"
                        )
                    }
                }
                is NetworkResult.Loading -> {
                    // Still loading, keep showing progress
                }
            }
        }
    }

    fun filterByType(type: CallType?) {
        _uiState.update { it.copy(selectedFilter = type) }
        _currentFilter.update { it.copy(type = type) }
    }

    fun filterByDateRange(startDate: Long?, endDate: Long?) {
        _uiState.update { it.copy(startDate = startDate, endDate = endDate) }
        _currentFilter.update { it.copy(startDate = startDate, endDate = endDate) }
    }

    fun searchCalls(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        _currentFilter.update { it.copy(searchQuery = query) }
    }

    fun clearFilters() {
        _uiState.update {
            it.copy(
                selectedFilter = null,
                searchQuery = "",
                startDate = null,
                endDate = null
            )
        }
        _currentFilter.update { CallFilter() }
    }

    fun deleteCall(callId: String) {
        viewModelScope.launch {
            callsRepository.deleteCallLog(callId)
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

// Extension function for updating StateFlow
private fun <T> MutableStateFlow<T>.update(function: (T) -> T) {
    value = function(value)
}
