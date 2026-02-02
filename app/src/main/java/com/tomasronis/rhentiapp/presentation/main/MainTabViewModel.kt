package com.tomasronis.rhentiapp.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomasronis.rhentiapp.core.preferences.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for MainTabScreen.
 * Manages selected tab persistence and unread badge count.
 */
@HiltViewModel
class MainTabViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    init {
        // Restore selected tab from preferences
        viewModelScope.launch {
            preferencesManager.selectedTab.collect { tab ->
                _selectedTab.value = tab
            }
        }
    }

    /**
     * Set the selected tab and persist to preferences.
     */
    suspend fun setSelectedTab(tabIndex: Int) {
        _selectedTab.value = tabIndex
        preferencesManager.setSelectedTab(tabIndex)
    }

    /**
     * Update unread count for Chats tab badge.
     * This will be called from ChatHubViewModel in Phase 3.2.
     */
    fun setUnreadCount(count: Int) {
        _unreadCount.value = count
    }
}
