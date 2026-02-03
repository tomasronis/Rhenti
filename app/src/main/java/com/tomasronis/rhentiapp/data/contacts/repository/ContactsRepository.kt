package com.tomasronis.rhentiapp.data.contacts.repository

import com.tomasronis.rhentiapp.core.network.NetworkResult
import com.tomasronis.rhentiapp.data.contacts.models.Contact
import com.tomasronis.rhentiapp.data.contacts.models.ContactProfile
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for contacts operations.
 * Manages fetching and caching contacts.
 */
interface ContactsRepository {

    /**
     * Get all contacts for the current user.
     * Fetches from API and caches locally.
     */
    suspend fun getContacts(superAccountId: String): NetworkResult<List<Contact>>

    /**
     * Get detailed profile for a specific contact.
     */
    suspend fun getContactProfile(contactId: String, email: String, superAccountId: String): NetworkResult<ContactProfile>

    /**
     * Search contacts by name, email, or phone.
     */
    suspend fun searchContacts(query: String): Flow<List<Contact>>

    /**
     * Observe cached contacts (reactive).
     */
    fun observeContacts(): Flow<List<Contact>>

    /**
     * Observe a specific contact (reactive).
     */
    fun observeContact(contactId: String): Flow<Contact?>

    /**
     * Refresh contacts from API.
     */
    suspend fun refreshContacts(superAccountId: String): NetworkResult<Unit>
}
