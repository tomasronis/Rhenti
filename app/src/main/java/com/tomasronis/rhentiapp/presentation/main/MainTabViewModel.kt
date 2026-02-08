package com.tomasronis.rhentiapp.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomasronis.rhentiapp.core.preferences.PreferencesManager
import com.tomasronis.rhentiapp.core.voip.CallState
import com.tomasronis.rhentiapp.core.voip.TwilioManager
import com.tomasronis.rhentiapp.data.contacts.models.Contact
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
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
    private val twilioManager: TwilioManager,
    private val chatHubRepository: com.tomasronis.rhentiapp.data.chathub.repository.ChatHubRepository,
    private val contactsRepository: com.tomasronis.rhentiapp.data.contacts.repository.ContactsRepository,
    private val tokenManager: com.tomasronis.rhentiapp.core.security.TokenManager
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _unreadCount = MutableStateFlow(0)
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private val _contactToStartChat = MutableStateFlow<Contact?>(null)
    val contactToStartChat: StateFlow<Contact?> = _contactToStartChat.asStateFlow()

    private val _threadIdToOpen = MutableStateFlow<String?>(null)
    val threadIdToOpen: StateFlow<String?> = _threadIdToOpen.asStateFlow()

    private val _contactIdToOpen = MutableStateFlow<String?>(null)
    val contactIdToOpen: StateFlow<String?> = _contactIdToOpen.asStateFlow()

    private val _contactThreadIdToOpen = MutableStateFlow<String?>(null)
    val contactThreadIdToOpen: StateFlow<String?> = _contactThreadIdToOpen.asStateFlow()

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

        // Preload data in background to sync contact avatars and channel tags
        preloadDataForSync()
    }

    /**
     * Preload threads and contacts in the background at app startup.
     * This ensures profile pictures and channel tags are cached before user navigation.
     */
    private fun preloadDataForSync() {
        viewModelScope.launch {
            try {
                val superAccountId = tokenManager.getSuperAccountId()
                if (superAccountId != null) {
                    android.util.Log.d("MainTabViewModel", "Starting background data sync...")

                    // Fetch both threads and contacts in parallel for faster sync
                    val threadsJob = async {
                        try {
                            chatHubRepository.getThreads(superAccountId, search = null)
                            android.util.Log.d("MainTabViewModel", "Background threads sync completed")
                        } catch (e: Exception) {
                            android.util.Log.w("MainTabViewModel", "Background threads sync failed: ${e.message}")
                        }
                    }

                    val contactsJob = async {
                        try {
                            contactsRepository.refreshContacts(superAccountId)
                            android.util.Log.d("MainTabViewModel", "Background contacts sync completed")
                        } catch (e: Exception) {
                            android.util.Log.w("MainTabViewModel", "Background contacts sync failed: ${e.message}")
                        }
                    }

                    // Wait for both to complete
                    threadsJob.await()
                    contactsJob.await()

                    android.util.Log.d("MainTabViewModel", "Background data sync completed")
                }
            } catch (e: Exception) {
                // Silent failure - this is just for background sync
                android.util.Log.d("MainTabViewModel", "Background data sync failed: ${e.message}")
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
     * Set thread ID to open (from Calls tab).
     * This triggers navigation to a specific message thread.
     */
    fun setThreadIdToOpen(threadId: String?) {
        _threadIdToOpen.value = threadId
    }

    /**
     * Clear the thread ID to open (after navigation completes).
     */
    fun clearThreadIdToOpen() {
        _threadIdToOpen.value = null
    }

    /**
     * Set contact ID to open (from Calls tab).
     * This triggers navigation to a specific contact detail.
     */
    fun setContactIdToOpen(contactId: String?, threadId: String? = null) {
        _contactIdToOpen.value = contactId
        _contactThreadIdToOpen.value = threadId
    }

    /**
     * Clear the contact ID to open (after navigation completes).
     */
    fun clearContactIdToOpen() {
        _contactIdToOpen.value = null
        _contactThreadIdToOpen.value = null
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
