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
        // Observe cached contacts with search filter
        viewModelScope.launch {
            repository.observeContacts()
                .combine(_searchQuery) { contacts, query ->
                    if (query.isBlank()) {
                        contacts
                    } else {
                        contacts.filter { contact ->
                            contact.displayName.contains(query, ignoreCase = true) ||
                            contact.email?.contains(query, ignoreCase = true) == true ||
                            contact.phone?.contains(query, ignoreCase = true) == true
                        }
                    }
                }
                .collect { filteredContacts ->
                    _uiState.update { it.copy(contacts = filteredContacts) }
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
     * Search contacts by query (filters locally).
     */
    fun searchContacts(query: String) {
        _searchQuery.value = query
    }

    /**
     * Select a contact to view details.
     */
    fun selectContact(contact: Contact) {
        android.util.Log.d("ContactsViewModel", "=== SELECT CONTACT ===")
        android.util.Log.d("ContactsViewModel", "Contact ID: ${contact.id}")
        android.util.Log.d("ContactsViewModel", "Display Name: ${contact.displayName}")
        android.util.Log.d("ContactsViewModel", "Email: '${contact.email}' (is null: ${contact.email == null}, is blank: ${contact.email?.isBlank()})")
        android.util.Log.d("ContactsViewModel", "Phone: ${contact.phone}")
        android.util.Log.d("ContactsViewModel", "Full contact: $contact")

        _uiState.update { it.copy(selectedContact = contact, contactProfile = null) }

        // Load full profile
        loadContactProfile(contact)
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
    private fun loadContactProfile(contact: Contact) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val superAccountId = tokenManager.getSuperAccountId() ?: run {
                _uiState.update { it.copy(isLoading = false, error = "Not authenticated") }
                return@launch
            }

            // Email is required by the API
            val email = contact.email?.takeIf { it.isNotBlank() } ?: run {
                android.util.Log.w("ContactsViewModel", "Contact ${contact.id} has no email, showing basic profile")
                // Create a basic profile from contact data (no API call)
                val basicProfile = ContactProfile(
                    id = contact.id,
                    firstName = contact.firstName,
                    lastName = contact.lastName,
                    email = null,
                    phone = contact.phone,
                    avatarUrl = contact.avatarUrl,
                    properties = emptyList(),
                    role = null,
                    notes = null,
                    totalMessages = contact.totalMessages,
                    totalCalls = contact.totalCalls,
                    lastActivity = contact.lastActivity,
                    createdAt = System.currentTimeMillis()
                )
                _uiState.update {
                    it.copy(
                        contactProfile = basicProfile,
                        isLoading = false,
                        error = null
                    )
                }
                return@launch
            }

            when (val result = repository.getContactProfile(contact.id, email, superAccountId)) {
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
