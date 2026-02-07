package com.tomasronis.rhentiapp.data.contacts.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Domain model for a contact.
 */
data class Contact(
    val id: String,
    val firstName: String?,
    val lastName: String?,
    val email: String?,
    val phone: String?,
    val avatarUrl: String?,
    val propertyIds: List<String>,
    val totalMessages: Int,
    val totalCalls: Int,
    val lastActivity: Long?,
    val channel: String? // Channel/platform source (e.g., "facebook", "kijiji", "rhenti")
) {
    /**
     * Display name combining first and last name.
     */
    val displayName: String
        get() = listOfNotNull(firstName, lastName)
            .joinToString(" ")
            .ifBlank { email ?: phone ?: "Unknown Contact" }

    /**
     * Section letter for grouped list (first letter of last name).
     */
    val sectionLetter: String
        get() = lastName?.firstOrNull()?.uppercaseChar()?.toString() ?: "#"
}

/**
 * Detailed contact profile with property information.
 */
data class ContactProfile(
    val id: String,
    val firstName: String?,
    val lastName: String?,
    val email: String?,
    val phone: String?,
    val avatarUrl: String?,
    val properties: List<ContactProperty>,
    val role: String?, // "tenant", "owner", "prospect"
    val notes: String?,
    val totalMessages: Int,
    val totalCalls: Int,
    val lastActivity: Long?,
    val createdAt: Long,
    val channel: String? // Channel/platform source (e.g., "facebook", "kijiji", "rhenti")
) {
    val displayName: String
        get() = listOfNotNull(firstName, lastName)
            .joinToString(" ")
            .ifBlank { email ?: phone ?: "Unknown Contact" }
}

/**
 * Property associated with a contact.
 */
data class ContactProperty(
    val id: String,
    val address: String,
    val unit: String?,
    val role: String // "tenant", "applicant", "interested"
)

/**
 * Request to fetch contacts.
 */
@JsonClass(generateAdapter = true)
data class GetContactsRequest(
    @Json(name = "super_account_id")
    val superAccountId: String,
    @Json(name = "limit")
    val limit: Int? = 100,
    @Json(name = "offset")
    val offset: Int? = 0
)

/**
 * Request to get contact profile details.
 */
@JsonClass(generateAdapter = true)
data class GetContactProfileRequest(
    @Json(name = "contact_id")
    val contactId: String,
    @Json(name = "super_account_id")
    val superAccountId: String
)
