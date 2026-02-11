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
     * Section letter for grouped list (first letter of first name).
     */
    val sectionLetter: String
        get() = firstName?.firstOrNull()?.uppercaseChar()?.toString() ?: "#"
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
 * Response from /getViewingAndApplicationsByThreadId endpoint.
 */
data class ViewingsAndApplicationsResponse(
    val bookings: List<Booking>,
    val offers: List<Offer>?
)

/**
 * Booking/Viewing for a property.
 * Represents a scheduled viewing appointment.
 */
data class Booking(
    val id: String,
    val address: String?,
    val datetime: Long?, // Unix timestamp in milliseconds
    val dateTimeDayInTimeZone: String?, // Formatted string like "Jan 30, 2026 at 2:00 PM PST"
    val propertyTimeZone: String?,
    val status: String, // "pending", "confirmed", "declined"
    val hasPendingAlternatives: Boolean
) {
    /**
     * Get viewing status for UI display.
     */
    val viewingStatus: ViewingStatus
        get() = when (status) {
            "declined" -> ViewingStatus.CANCELLED
            "confirmed" -> ViewingStatus.CONFIRMED
            "pending" -> if (hasPendingAlternatives) ViewingStatus.AWAITING_RESPONSE else ViewingStatus.PENDING
            else -> ViewingStatus.PENDING
        }
}

/**
 * Viewing status enum for consistent UI display.
 */
enum class ViewingStatus(val label: String, val colorHex: String) {
    CONFIRMED("Confirmed", "#34C759"),
    PENDING("Pending", "#FF9500"),
    AWAITING_RESPONSE("Awaiting Response", "#FF9500"),
    CANCELLED("Cancelled", "#FF3B30")
}

/**
 * Rental application/offer.
 */
data class Offer(
    val id: String,
    val address: String?,
    val dateTimeDayInTimeZone: String?, // Formatted submission date
    val offer: OfferDetails?
) {
    /**
     * Get application status for UI display.
     */
    val applicationStatus: ApplicationStatus
        get() = when (offer?.status?.lowercase()) {
            "accepted", "approved" -> ApplicationStatus.APPROVED
            "pending" -> ApplicationStatus.PENDING
            "declined", "rejected" -> ApplicationStatus.REJECTED
            else -> ApplicationStatus.UNDER_REVIEW
        }
}

/**
 * Offer details nested within Offer.
 */
data class OfferDetails(
    val id: String,
    val price: Int?,
    val status: String?,
    val proposedStartDate: Long?,
    val createdAt: Long?
)

/**
 * Application status enum for consistent UI display.
 */
enum class ApplicationStatus(val label: String, val colorHex: String) {
    APPROVED("Approved", "#34C759"),
    UNDER_REVIEW("Under Review", "#FF9500"),
    PENDING("Pending", "#FF9500"),
    REJECTED("Rejected", "#FF3B30")
}

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

/**
 * Request to create a new contact/lead.
 */
@JsonClass(generateAdapter = true)
data class CreateContactRequest(
    @Json(name = "channelSource")
    val channelSource: String = "Other",
    @Json(name = "channel")
    val channel: String = "Other",
    @Json(name = "firstName")
    val firstName: String,
    @Json(name = "lastName")
    val lastName: String,
    @Json(name = "email")
    val email: String,
    @Json(name = "phone")
    val phone: String,
    @Json(name = "property")
    val property: String?,
    @Json(name = "leadOwnerId")
    val leadOwnerId: String
)

/**
 * Response from creating a new contact/lead.
 */
@JsonClass(generateAdapter = true)
data class CreateContactResponse(
    @Json(name = "result")
    val result: NewLeadResult
)

@JsonClass(generateAdapter = true)
data class NewLeadResult(
    @Json(name = "_id")
    val id: String,
    @Json(name = "customerAccountId")
    val customerAccountId: String,
    @Json(name = "profile")
    val profile: NewLeadProfile,
    @Json(name = "propertyId")
    val propertyId: String?
)

@JsonClass(generateAdapter = true)
data class NewLeadProfile(
    @Json(name = "_id")
    val id: String,
    @Json(name = "firstName")
    val firstName: String?,
    @Json(name = "lastName")
    val lastName: String?,
    @Json(name = "email")
    val email: String?,
    @Json(name = "phone")
    val phone: String?
)

/**
 * Lead owner for a property.
 */
data class LeadOwner(
    val name: String,
    val value: String // User ID
)
