package com.tomasronis.rhentiapp.presentation.main.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomasronis.rhentiapp.core.network.NetworkResult
import com.tomasronis.rhentiapp.core.security.TokenManager
import com.tomasronis.rhentiapp.data.contacts.models.Contact
import com.tomasronis.rhentiapp.data.contacts.models.ContactProfile
import com.tomasronis.rhentiapp.data.contacts.repository.ContactsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Contacts screens (list and detail).
 * Manages contacts data and search functionality.
 */
@HiltViewModel
class ContactsViewModel @Inject constructor(
    private val repository: ContactsRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContactsUiState())
    val uiState: StateFlow<ContactsUiState> = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        // Observe cached contacts
        viewModelScope.launch {
            repository.observeContacts().collect { contacts ->
                _uiState.update { it.copy(contacts = contacts) }
            }
        }

        // Load contacts on init
        refreshContacts()
    }

    /**
     * Refresh contacts from the API.
     */
    fun refreshContacts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val superAccountId = tokenManager.getSuperAccountId() ?: run {
                _uiState.update { it.copy(isLoading = false, error = "Not authenticated") }
                return@launch
            }

            when (val result = repository.getContacts(superAccountId)) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, error = null) }
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.exception.message ?: "Failed to load contacts"
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
     * Search contacts by query.
     */
    fun searchContacts(query: String) {
        _searchQuery.value = query

        if (query.isBlank()) {
            // Show all contacts when search is cleared
            viewModelScope.launch {
                repository.observeContacts().collect { contacts ->
                    _uiState.update { it.copy(contacts = contacts) }
                }
            }
        } else {
            // Filter contacts by search query
            viewModelScope.launch {
                repository.searchContacts(query).collect { contacts ->
                    _uiState.update { it.copy(contacts = contacts) }
                }
            }
        }
    }

    /**
     * Select a contact to view details.
     */
    fun selectContact(contact: Contact) {
        _uiState.update { it.copy(selectedContact = contact, contactProfile = null) }

        // Load full profile
        loadContactProfile(contact.id)
    }

    /**
     * Clear selected contact (go back to list).
     */
    fun clearSelectedContact() {
        _uiState.update { it.copy(selectedContact = null, contactProfile = null) }
    }

    /**
     * Load detailed profile for a contact.
     */
    private fun loadContactProfile(contactId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val superAccountId = tokenManager.getSuperAccountId() ?: run {
                _uiState.update { it.copy(isLoading = false, error = "Not authenticated") }
                return@launch
            }

            when (val result = repository.getContactProfile(contactId, superAccountId)) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            contactProfile = result.data,
                            isLoading = false,
                            error = null
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.exception.message ?: "Failed to load contact profile"
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
     * Clear error message.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

/**
 * UI state for Contacts screens.
 */
data class ContactsUiState(
    val contacts: List<Contact> = emptyList(),
    val selectedContact: Contact? = null,
    val contactProfile: ContactProfile? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)
