package com.tomasronis.rhentiapp.data.contacts.repository

import com.tomasronis.rhentiapp.BuildConfig
import com.tomasronis.rhentiapp.core.database.dao.ContactDao
import com.tomasronis.rhentiapp.core.database.entities.CachedContact
import com.tomasronis.rhentiapp.core.network.ApiClient
import com.tomasronis.rhentiapp.core.network.NetworkResult
import com.tomasronis.rhentiapp.data.contacts.models.Contact
import com.tomasronis.rhentiapp.data.contacts.models.ContactProfile
import com.tomasronis.rhentiapp.data.contacts.models.ContactProperty
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactsRepositoryImpl @Inject constructor(
    private val apiClient: ApiClient,
    private val contactDao: ContactDao
) : ContactsRepository {

    override suspend fun getContacts(superAccountId: String): NetworkResult<List<Contact>> {
        return try {
            val response = apiClient.getContacts(superAccountId)

            if (BuildConfig.DEBUG) {
                android.util.Log.d("ContactsRepository", "Contacts response: $response")
            }

            val contacts = parseContactsResponse(response)

            // Cache contacts
            val cachedContacts = contacts.map { it.toCachedContact() }
            contactDao.insertContacts(cachedContacts)

            NetworkResult.Success(contacts)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                android.util.Log.e("ContactsRepository", "Get contacts failed", e)
            }
            NetworkResult.Error(
                exception = e,
                cachedData = null
            )
        }
    }

    override suspend fun getContactProfile(
        contactId: String,
        superAccountId: String
    ): NetworkResult<ContactProfile> {
        return try {
            val request = mapOf(
                "contact_id" to contactId,
                "super_account_id" to superAccountId
            )

            val response = apiClient.getContactProfile(request)

            if (BuildConfig.DEBUG) {
                android.util.Log.d("ContactsRepository", "Contact profile response: $response")
            }

            val profile = parseContactProfileResponse(response)

            NetworkResult.Success(profile)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                android.util.Log.e("ContactsRepository", "Get contact profile failed", e)
            }
            NetworkResult.Error(
                exception = e,
                cachedData = null
            )
        }
    }

    override suspend fun searchContacts(query: String): Flow<List<Contact>> {
        return contactDao.searchContacts(query).map { cachedContacts ->
            cachedContacts.map { it.toDomainModel() }
        }
    }

    override fun observeContacts(): Flow<List<Contact>> {
        return contactDao.getAllContacts().map { cachedContacts ->
            cachedContacts.map { it.toDomainModel() }
        }
    }

    override fun observeContact(contactId: String): Flow<Contact?> {
        return contactDao.getContactById(contactId).map { it?.toDomainModel() }
    }

    override suspend fun refreshContacts(superAccountId: String): NetworkResult<Unit> {
        return when (val result = getContacts(superAccountId)) {
            is NetworkResult.Success -> NetworkResult.Success(Unit)
            is NetworkResult.Error -> NetworkResult.Error(
                exception = result.exception,
                cachedData = null
            )
            is NetworkResult.Loading -> NetworkResult.Loading()
        }
    }

    // Helper functions to parse API responses

    private fun parseContactsResponse(response: List<Map<String, Any>>): List<Contact> {
        val contacts = mutableListOf<Contact>()

        for (contactData in response) {
            try {
                val id = contactData["_id"] as? String
                    ?: contactData["id"] as? String
                    ?: continue

                val firstName = contactData["first_name"] as? String
                    ?: contactData["firstName"] as? String
                val lastName = contactData["last_name"] as? String
                    ?: contactData["lastName"] as? String
                val email = contactData["email"] as? String
                val phone = contactData["phone"] as? String
                val avatarUrl = contactData["avatar_url"] as? String
                    ?: contactData["avatarUrl"] as? String

                @Suppress("UNCHECKED_CAST")
                val propertyIds = (contactData["property_ids"] as? List<String>)
                    ?: (contactData["propertyIds"] as? List<String>)
                    ?: emptyList()

                val totalMessages = (contactData["total_messages"] as? Number
                    ?: contactData["totalMessages"] as? Number
                    ?: 0).toInt()

                val totalCalls = (contactData["total_calls"] as? Number
                    ?: contactData["totalCalls"] as? Number
                    ?: 0).toInt()

                val lastActivity = (contactData["last_activity"] as? Number
                    ?: contactData["lastActivity"] as? Number)?.toLong()

                contacts.add(
                    Contact(
                        id = id,
                        firstName = firstName,
                        lastName = lastName,
                        email = email,
                        phone = phone,
                        avatarUrl = avatarUrl,
                        propertyIds = propertyIds,
                        totalMessages = totalMessages,
                        totalCalls = totalCalls,
                        lastActivity = lastActivity
                    )
                )
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) {
                    android.util.Log.w("ContactsRepository", "Failed to parse contact: $contactData", e)
                }
            }
        }

        return contacts
    }

    private fun parseContactProfileResponse(response: Map<String, Any>): ContactProfile {
        val id = response["_id"] as? String
            ?: response["id"] as? String
            ?: throw IllegalArgumentException("Contact ID missing")

        val firstName = response["first_name"] as? String
            ?: response["firstName"] as? String
        val lastName = response["last_name"] as? String
            ?: response["lastName"] as? String
        val email = response["email"] as? String
        val phone = response["phone"] as? String
        val avatarUrl = response["avatar_url"] as? String
            ?: response["avatarUrl"] as? String
        val role = response["role"] as? String
        val notes = response["notes"] as? String

        val totalMessages = (response["total_messages"] as? Number
            ?: response["totalMessages"] as? Number
            ?: 0).toInt()
        val totalCalls = (response["total_calls"] as? Number
            ?: response["totalCalls"] as? Number
            ?: 0).toInt()

        val lastActivity = (response["last_activity"] as? Number
            ?: response["lastActivity"] as? Number)?.toLong()
        val createdAt = (response["created_at"] as? Number
            ?: response["createdAt"] as? Number
            ?: System.currentTimeMillis()).toLong()

        // Parse properties
        @Suppress("UNCHECKED_CAST")
        val propertiesData = response["properties"] as? List<Map<String, Any>> ?: emptyList()
        val properties = propertiesData.mapNotNull { propData ->
            try {
                val propId = propData["_id"] as? String
                    ?: propData["id"] as? String
                    ?: return@mapNotNull null
                val address = propData["address"] as? String ?: return@mapNotNull null
                val unit = propData["unit"] as? String
                val propRole = propData["role"] as? String ?: "tenant"

                ContactProperty(
                    id = propId,
                    address = address,
                    unit = unit,
                    role = propRole
                )
            } catch (e: Exception) {
                null
            }
        }

        return ContactProfile(
            id = id,
            firstName = firstName,
            lastName = lastName,
            email = email,
            phone = phone,
            avatarUrl = avatarUrl,
            properties = properties,
            role = role,
            notes = notes,
            totalMessages = totalMessages,
            totalCalls = totalCalls,
            lastActivity = lastActivity,
            createdAt = createdAt
        )
    }
}

// Extension functions to convert between domain and cache models

private fun Contact.toCachedContact(): CachedContact {
    return CachedContact(
        id = id,
        firstName = firstName,
        lastName = lastName,
        email = email,
        phone = phone,
        avatarUrl = avatarUrl,
        propertyIds = propertyIds.joinToString(","),
        totalMessages = totalMessages,
        totalCalls = totalCalls,
        lastActivity = lastActivity,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )
}

private fun CachedContact.toDomainModel(): Contact {
    return Contact(
        id = id,
        firstName = firstName,
        lastName = lastName,
        email = email,
        phone = phone,
        avatarUrl = avatarUrl,
        propertyIds = propertyIds?.split(",")?.filter { it.isNotBlank() } ?: emptyList(),
        totalMessages = totalMessages,
        totalCalls = totalCalls,
        lastActivity = lastActivity
    )
}
