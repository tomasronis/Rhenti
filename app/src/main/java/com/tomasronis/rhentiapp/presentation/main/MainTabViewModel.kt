package com.tomasronis.rhentiapp.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomasronis.rhentiapp.core.preferences.PreferencesManager
import com.tomasronis.rhentiapp.core.voip.CallState
import com.tomasronis.rhentiapp.core.voip.TwilioManager
import com.tomasronis.rhentiapp.data.contacts.models.Contact
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for MainTabScreen.
 * Manages selected tab persistence and unread badge count.
 */
@HiltViewModel
class MainTabViewModel @Inject constructor(
    private val preferencesManager: PreferencesManager,
    private val twilioManager: TwilioManager
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private val _contactToStartChat = MutableStateFlow<Contact?>(null)
    val contactToStartChat: StateFlow<Contact?> = _contactToStartChat.asStateFlow()

    private val _isTwilioInitialized = MutableStateFlow(false)
    val isTwilioInitialized: StateFlow<Boolean> = _isTwilioInitialized.asStateFlow()

    // Observe call state from TwilioManager
    val callState: StateFlow<CallState> = twilioManager.callState
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = CallState.Idle
        )

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

    /**
     * Set contact to start chat with (from Contacts tab).
     * This triggers navigation to a specific chat thread.
     */
    fun setContactToStartChat(contact: Contact?) {
        _contactToStartChat.value = contact
    }

    /**
     * Clear the contact to start chat (after navigation completes).
     */
    fun clearContactToStartChat() {
        _contactToStartChat.value = null
    }

    /**
     * Initialize Twilio for VoIP calls.
     * Called when the user enters the main authenticated screen.
     */
    fun initializeTwilio() {
        viewModelScope.launch {
            try {
                android.util.Log.d("MainTabViewModel", "Starting Twilio initialization...")
                twilioManager.initialize()
                _isTwilioInitialized.value = true
                android.util.Log.d("MainTabViewModel", "Twilio initialized successfully")
            } catch (e: Exception) {
                android.util.Log.e("MainTabViewModel", "Twilio initialization failed", e)
                _isTwilioInitialized.value = false
            }
        }
    }

    /**
     * Start an outgoing call
     */
    fun makeCall(phoneNumber: String) {
        twilioManager.makeOutgoingCall(phoneNumber)
    }
}
