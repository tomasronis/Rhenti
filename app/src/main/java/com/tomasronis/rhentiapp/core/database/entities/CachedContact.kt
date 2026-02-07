package com.tomasronis.rhentiapp.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for caching contacts locally.
 */
@Entity(tableName = "cached_contacts")
data class CachedContact(
    @PrimaryKey
    val id: String,
    val firstName: String?,
    val lastName: String?,
    val email: String?,
    val phone: String?,
    val avatarUrl: String?,
    val propertyIds: String?, // Comma-separated property IDs
    val totalMessages: Int,
    val totalCalls: Int,
    val lastActivity: Long?,
    val createdAt: Long,
    val updatedAt: Long,
    val channel: String? = null // Channel/platform source (e.g., "facebook", "kijiji", "rhenti")
) {
    /**
     * Returns the first letter of the contact's last name for section grouping.
     */
    val sectionLetter: String
        get() = lastName?.firstOrNull()?.uppercase() ?: "#"
}
