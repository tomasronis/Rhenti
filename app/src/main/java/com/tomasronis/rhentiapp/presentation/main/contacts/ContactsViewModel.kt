package com.tomasronis.rhentiapp.presentation.main.contacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomasronis.rhentiapp.BuildConfig
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

    private val _hideContactsWithoutName = MutableStateFlow(false)
    val hideContactsWithoutName: StateFlow<Boolean> = _hideContactsWithoutName.asStateFlow()

    init {
        // Observe cached contacts with search and name filter
        viewModelScope.launch {
            repository.observeContacts()
                .combine(_searchQuery) { contacts, query ->
                    contacts to query
                }
                .combine(_hideContactsWithoutName) { (contacts, query), hideNoName ->
                    var filtered = contacts

                    // Apply name filter - hide contacts without proper names (empty or only numbers)
                    if (hideNoName) {
                        filtered = filtered.filter { contact ->
                            val hasValidFirstName = contact.firstName?.any { it.isLetter() } == true
                            val hasValidLastName = contact.lastName?.any { it.isLetter() } == true
                            hasValidFirstName || hasValidLastName
                        }
                    }

                    // Apply search filter
                    if (query.isNotBlank()) {
                        filtered = filtered.filter { contact ->
                            contact.displayName.contains(query, ignoreCase = true) ||
                            contact.email?.contains(query, ignoreCase = true) == true ||
                            contact.phone?.contains(query, ignoreCase = true) == true
                        }
                    }

                    filtered
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
        android.util.Log.d("ContactsViewModel", "AvatarUrl: ${contact.avatarUrl}")
        android.util.Log.d("ContactsViewModel", "Channel: ${contact.channel}")
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
                    createdAt = System.currentTimeMillis(),
                    channel = contact.channel
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
                    android.util.Log.d("ContactsViewModel", "=== PROFILE LOADED ===")
                    android.util.Log.d("ContactsViewModel", "Profile avatarUrl from DB: ${result.data?.avatarUrl}")
                    android.util.Log.d("ContactsViewModel", "Profile channel from DB: ${result.data?.channel}")
                    android.util.Log.d("ContactsViewModel", "Contact avatarUrl (merged): ${contact.avatarUrl}")
                    android.util.Log.d("ContactsViewModel", "Contact channel (merged): ${contact.channel}")

                    // Merge the passed contact's avatarUrl and channel with the loaded profile
                    // This ensures data from threads (which have more recent info) is preserved
                    val mergedProfile = result.data?.copy(
                        avatarUrl = contact.avatarUrl ?: result.data.avatarUrl,
                        channel = contact.channel ?: result.data.channel
                    )

                    android.util.Log.d("ContactsViewModel", "Final merged profile avatarUrl: ${mergedProfile?.avatarUrl}")
                    android.util.Log.d("ContactsViewModel", "Final merged profile channel: ${mergedProfile?.channel}")

                    _uiState.update {
                        it.copy(
                            contactProfile = mergedProfile,
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

    /**
     * Update a contact in the cache (e.g., to sync imageUrl and channel from thread).
     */
    fun updateContact(contact: Contact) {
        viewModelScope.launch {
            repository.updateContact(contact)
        }
    }

    /**
     * Toggle "Hide Contacts Without Name" filter.
     */
    fun setHideContactsWithoutName(hide: Boolean) {
        _hideContactsWithoutName.value = hide
    }

    /**
     * Load viewings and applications for a contact by thread ID.
     * This requires a threadId (chat session) to be available.
     */
    fun loadViewingsAndApplications(threadId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingViewings = true, viewingsError = null) }

            when (val result = repository.getViewingsAndApplications(threadId)) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            viewings = result.data?.bookings ?: emptyList(),
                            applications = result.data?.offers ?: emptyList(),
                            isLoadingViewings = false,
                            viewingsError = null
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoadingViewings = false,
                            viewingsError = result.exception.message ?: "Failed to load viewings and applications"
                        )
                    }
                }
                is NetworkResult.Loading -> {
                    _uiState.update { it.copy(isLoadingViewings = true) }
                }
            }
        }
    }

    /**
     * Create a new contact.
     */
    fun createContact(
        firstName: String,
        lastName: String,
        email: String,
        phone: String?,
        propertyId: String,
        leadOwnerId: String?,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = repository.createContact(
                firstName = firstName,
                lastName = lastName,
                email = email,
                phone = phone,
                propertyId = propertyId,
                leadOwnerId = leadOwnerId
            )) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, error = null) }
                    // Refresh contacts to show the new contact
                    refreshContacts()
                    onSuccess()
                }
                is NetworkResult.Error -> {
                    val errorMsg = result.exception.message ?: "Failed to create contact"
                    _uiState.update { it.copy(isLoading = false, error = errorMsg) }
                    onError(errorMsg)
                }
                is NetworkResult.Loading -> {
                    _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    /**
     * Get lead owners for a property.
     */
    fun getLeadOwners(propertyId: String, onResult: (List<com.tomasronis.rhentiapp.data.contacts.models.LeadOwner>) -> Unit) {
        viewModelScope.launch {
            when (val result = repository.getLeadOwners(propertyId)) {
                is NetworkResult.Success -> {
                    onResult(result.data ?: emptyList())
                }
                is NetworkResult.Error -> {
                    if (BuildConfig.DEBUG) {
                        android.util.Log.e("ContactsViewModel", "Failed to fetch lead owners", result.exception)
                    }
                    onResult(emptyList())
                }
                is NetworkResult.Loading -> {
                    // Do nothing
                }
            }
        }
    }
}

/**
 * UI state for Contacts screens.
 */
data class ContactsUiState(
    val contacts: List<Contact> = emptyList(),
    val selectedContact: Contact? = null,
    val contactProfile: ContactProfile? = null,
    val viewings: List<com.tomasronis.rhentiapp.data.contacts.models.Booking> = emptyList(),
    val applications: List<com.tomasronis.rhentiapp.data.contacts.models.Offer> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingViewings: Boolean = false,
    val error: String? = null,
    val viewingsError: String? = null
)
