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

            // Extract the contacts array from the wrapper object
            @Suppress("UNCHECKED_CAST")
            val contactsList = response["contacts"] as? List<Map<String, Any>> ?: emptyList()

            val contacts = parseContactsResponse(contactsList)

            // Cache contacts - merge with existing data to preserve avatarUrl and channel
            val cachedContacts = contacts.map { newContact ->
                val existingContact = contactDao.getContactByIdOnce(newContact.id)

                // Merge: keep avatarUrl and channel from existing contact if new one doesn't have them
                if (existingContact != null) {
                    newContact.copy(
                        avatarUrl = newContact.avatarUrl ?: existingContact.avatarUrl,
                        channel = newContact.channel ?: existingContact.channel
                    )
                } else {
                    newContact
                }.toCachedContact()
            }
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
        email: String,
        superAccountId: String
    ): NetworkResult<ContactProfile> {
        return try {
            // TEMPORARY: API endpoint appears to be broken (always returns 500 "Email is required")
            // Skip API call and return basic profile from cached contact data
            if (BuildConfig.DEBUG) {
                android.util.Log.w("ContactsRepository", "Skipping broken API endpoint, using cached contact data")
            }

            // Get cached contact
            val cachedContact = contactDao.getContactByIdOnce(contactId)
                ?: throw Exception("Contact not found in cache")

            // Create basic profile from cached data
            val profile = ContactProfile(
                id = cachedContact.id,
                firstName = cachedContact.firstName,
                lastName = cachedContact.lastName,
                email = cachedContact.email,
                phone = cachedContact.phone,
                avatarUrl = cachedContact.avatarUrl,
                properties = emptyList(), // Would come from API
                role = null, // Would come from API
                notes = null, // Would come from API
                totalMessages = cachedContact.totalMessages,
                totalCalls = cachedContact.totalCalls,
                lastActivity = cachedContact.lastActivity,
                createdAt = cachedContact.createdAt,
                channel = cachedContact.channel
            )

            NetworkResult.Success(profile)

            /* ORIGINAL CODE - API endpoint broken
            val request = mapOf(
                "contact_id" to contactId,
                "email" to email,
                "super_account_id" to superAccountId
            )

            if (BuildConfig.DEBUG) {
                android.util.Log.d("ContactsRepository", "=== GET CONTACT PROFILE REQUEST ===")
                android.util.Log.d("ContactsRepository", "contactId: $contactId")
                android.util.Log.d("ContactsRepository", "email: '$email'")
                android.util.Log.d("ContactsRepository", "superAccountId: $superAccountId")
                android.util.Log.d("ContactsRepository", "Full request map: $request")
            }

            val response = apiClient.getContactProfile(request)

            android.util.Log.d("ContactsRepository", "Contact profile response: $response")

            val profile = parseContactProfileResponse(response)

            NetworkResult.Success(profile)
            */
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

    override suspend fun updateContact(contact: Contact) {
        try {
            val cachedContact = contact.toCachedContact()
            contactDao.insertContact(cachedContact)
            if (BuildConfig.DEBUG) {
                android.util.Log.d(
                    "ContactsRepository",
                    "Updated contact: ${contact.displayName} (avatarUrl: ${contact.avatarUrl}, channel: ${contact.channel})"
                )
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                android.util.Log.e("ContactsRepository", "Failed to update contact", e)
            }
        }
    }

    override suspend fun getViewingsAndApplications(threadId: String): NetworkResult<com.tomasronis.rhentiapp.data.contacts.models.ViewingsAndApplicationsResponse> {
        return try {
            if (BuildConfig.DEBUG) {
                android.util.Log.d("ContactsRepository", "Fetching viewings and applications for threadId: $threadId")
            }

            val response = apiClient.getViewingsAndApplications(threadId)

            if (BuildConfig.DEBUG) {
                android.util.Log.d("ContactsRepository", "Viewings/Applications response: $response")
            }

            val viewingsAndApps = parseViewingsAndApplicationsResponse(response)

            if (BuildConfig.DEBUG) {
                android.util.Log.d(
                    "ContactsRepository",
                    "Parsed ${viewingsAndApps.bookings.size} viewings and ${viewingsAndApps.offers?.size ?: 0} applications"
                )
            }

            NetworkResult.Success(viewingsAndApps)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                android.util.Log.e("ContactsRepository", "Get viewings and applications failed", e)
            }
            NetworkResult.Error(
                exception = e,
                cachedData = null
            )
        }
    }

    override suspend fun createContact(
        firstName: String,
        lastName: String,
        email: String,
        phone: String?,
        propertyId: String,
        leadOwnerId: String?
    ): NetworkResult<Contact> {
        return try {
            val request = buildMap<String, Any> {
                put("channelSource", "Other")
                put("channel", "Other")
                put("firstName", firstName)
                put("lastName", lastName)
                put("email", email)
                if (phone != null) {
                    put("phone", phone)
                }
                put("property", propertyId)
                if (leadOwnerId != null) {
                    put("leadOwnerId", leadOwnerId)
                }
            }

            if (BuildConfig.DEBUG) {
                android.util.Log.d("ContactsRepository", "Creating new contact: $request")
            }

            val response = apiClient.createNewLead(request)

            if (BuildConfig.DEBUG) {
                android.util.Log.d("ContactsRepository", "Create contact response: $response")
            }

            // Parse the response
            @Suppress("UNCHECKED_CAST")
            val result = response["result"] as? Map<String, Any>
                ?: throw Exception("Invalid response format")

            @Suppress("UNCHECKED_CAST")
            val profile = result["profile"] as? Map<String, Any>
                ?: throw Exception("Profile missing in response")

            val contactId = result["customerAccountId"] as? String
                ?: throw Exception("Customer account ID missing")

            // Create Contact object from response
            val contact = Contact(
                id = contactId,
                firstName = profile["firstName"] as? String,
                lastName = profile["lastName"] as? String,
                email = profile["email"] as? String,
                phone = profile["phone"] as? String,
                avatarUrl = null,
                propertyIds = listOfNotNull(result["propertyId"] as? String),
                totalMessages = 0,
                totalCalls = 0,
                lastActivity = System.currentTimeMillis(),
                channel = "Other"
            )

            // Cache the new contact
            contactDao.insertContact(contact.toCachedContact())

            if (BuildConfig.DEBUG) {
                android.util.Log.d("ContactsRepository", "Successfully created contact: ${contact.displayName}")
            }

            NetworkResult.Success(contact)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                android.util.Log.e("ContactsRepository", "Create contact failed", e)
            }
            NetworkResult.Error(
                exception = e,
                cachedData = null
            )
        }
    }

    override suspend fun getLeadOwners(propertyId: String): NetworkResult<List<com.tomasronis.rhentiapp.data.contacts.models.LeadOwner>> {
        return try {
            if (BuildConfig.DEBUG) {
                android.util.Log.d("ContactsRepository", "Fetching lead owners for property: $propertyId")
            }

            val response = apiClient.getLeadOwners(propertyId)

            if (BuildConfig.DEBUG) {
                android.util.Log.d("ContactsRepository", "Lead owners response: $response")
            }

            // Parse the response
            @Suppress("UNCHECKED_CAST")
            val data = response["data"] as? List<Map<String, Any>>
                ?: throw Exception("Invalid response format")

            val leadOwners = data.mapNotNull { ownerData ->
                try {
                    val name = ownerData["name"] as? String ?: return@mapNotNull null
                    val value = ownerData["value"] as? String ?: return@mapNotNull null
                    com.tomasronis.rhentiapp.data.contacts.models.LeadOwner(
                        name = name,
                        value = value
                    )
                } catch (e: Exception) {
                    if (BuildConfig.DEBUG) {
                        android.util.Log.w("ContactsRepository", "Failed to parse lead owner: $ownerData", e)
                    }
                    null
                }
            }

            if (BuildConfig.DEBUG) {
                android.util.Log.d("ContactsRepository", "Parsed ${leadOwners.size} lead owners")
            }

            NetworkResult.Success(leadOwners)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                android.util.Log.e("ContactsRepository", "Get lead owners failed", e)
            }
            NetworkResult.Error(
                exception = e,
                cachedData = null
            )
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

                if (BuildConfig.DEBUG && email.isNullOrBlank()) {
                    android.util.Log.w("ContactsRepository", "Contact $id ($firstName $lastName) has no email: $contactData")
                }
                val avatarUrlRaw = contactData["avatar_url"] as? String
                    ?: contactData["avatarUrl"] as? String
                    ?: contactData["image"] as? String
                    ?: contactData["imageUrl"] as? String
                val avatarUrl = avatarUrlRaw?.let { buildFullImageUrl(it) }

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

                val channel = contactData["channel"] as? String

                if (BuildConfig.DEBUG) {
                    android.util.Log.d(
                        "ContactsRepository",
                        "Parsed contact: id=$id, name=$firstName $lastName, avatarUrl=$avatarUrl, channel=$channel"
                    )
                }

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
                        lastActivity = lastActivity,
                        channel = channel
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
        val avatarUrlRaw = response["avatar_url"] as? String
            ?: response["avatarUrl"] as? String
            ?: response["image"] as? String
            ?: response["imageUrl"] as? String
        val avatarUrl = avatarUrlRaw?.let { buildFullImageUrl(it) }
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

        val channel = response["channel"] as? String

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
            createdAt = createdAt,
            channel = channel
        )
    }

    private fun parseViewingsAndApplicationsResponse(response: Map<String, Any>): com.tomasronis.rhentiapp.data.contacts.models.ViewingsAndApplicationsResponse {
        // Parse bookings (viewings)
        @Suppress("UNCHECKED_CAST")
        val bookingsData = response["bookings"] as? List<Map<String, Any>> ?: emptyList()
        val bookings = bookingsData.mapNotNull { bookingData ->
            try {
                val id = bookingData["_id"] as? String
                    ?: bookingData["id"] as? String
                    ?: return@mapNotNull null

                val address = bookingData["address"] as? String

                val datetime = (bookingData["datetime"] as? Number)?.toLong()

                val dateTimeDayInTimeZone = bookingData["dateTimeDayInTimeZone"] as? String
                    ?: bookingData["date_time_day_in_time_zone"] as? String

                val propertyTimeZone = bookingData["propertyTimeZone"] as? String
                    ?: bookingData["property_time_zone"] as? String

                val status = bookingData["status"] as? String ?: "pending"

                val hasPendingAlternatives = bookingData["hasPendingAlternatives"] as? Boolean
                    ?: bookingData["has_pending_alternatives"] as? Boolean
                    ?: false

                com.tomasronis.rhentiapp.data.contacts.models.Booking(
                    id = id,
                    address = address,
                    datetime = datetime,
                    dateTimeDayInTimeZone = dateTimeDayInTimeZone,
                    propertyTimeZone = propertyTimeZone,
                    status = status,
                    hasPendingAlternatives = hasPendingAlternatives
                )
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) {
                    android.util.Log.w("ContactsRepository", "Failed to parse booking: $bookingData", e)
                }
                null
            }
        }

        // Parse offers (applications)
        @Suppress("UNCHECKED_CAST")
        val offersData = response["offers"] as? List<Map<String, Any>>
        val offers = offersData?.mapNotNull { offerData ->
            try {
                val id = offerData["_id"] as? String
                    ?: offerData["id"] as? String
                    ?: return@mapNotNull null

                val address = offerData["address"] as? String

                val dateTimeDayInTimeZone = offerData["dateTimeDayInTimeZone"] as? String
                    ?: offerData["date_time_day_in_time_zone"] as? String

                // Parse nested offer object
                @Suppress("UNCHECKED_CAST")
                val offerDetailsData = offerData["offer"] as? Map<String, Any>
                val offerDetails = offerDetailsData?.let { details ->
                    try {
                        val detailsId = details["_id"] as? String
                            ?: details["id"] as? String
                            ?: id

                        val price = (details["price"] as? Number)?.toInt()
                        val status = details["status"] as? String
                        val proposedStartDate = (details["proposedStartDate"] as? Number
                            ?: details["proposed_start_date"] as? Number)?.toLong()
                        val createdAt = (details["createdAt"] as? Number
                            ?: details["created_at"] as? Number)?.toLong()

                        com.tomasronis.rhentiapp.data.contacts.models.OfferDetails(
                            id = detailsId,
                            price = price,
                            status = status,
                            proposedStartDate = proposedStartDate,
                            createdAt = createdAt
                        )
                    } catch (e: Exception) {
                        if (BuildConfig.DEBUG) {
                            android.util.Log.w("ContactsRepository", "Failed to parse offer details", e)
                        }
                        null
                    }
                }

                com.tomasronis.rhentiapp.data.contacts.models.Offer(
                    id = id,
                    address = address,
                    dateTimeDayInTimeZone = dateTimeDayInTimeZone,
                    offer = offerDetails
                )
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) {
                    android.util.Log.w("ContactsRepository", "Failed to parse offer: $offerData", e)
                }
                null
            }
        }

        return com.tomasronis.rhentiapp.data.contacts.models.ViewingsAndApplicationsResponse(
            bookings = bookings,
            offers = offers
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
        updatedAt = System.currentTimeMillis(),
        channel = channel
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
        lastActivity = lastActivity,
        channel = channel
    )
}

/**
 * Build full image URL from partial path.
 * If the URL is already complete (starts with http), return as-is.
 * Otherwise, prepend the UAT image base URL.
 */
private fun buildFullImageUrl(imagePath: String): String {
    return if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
        imagePath
    } else {
        // UAT image base URL
        "https://uatimgs.rhenti.com/images/${imagePath.trimStart('/')}"
    }
}
