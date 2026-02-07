package com.tomasronis.rhentiapp.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.tomasronis.rhentiapp.core.database.Converters

/**
 * Room entity for caching chat threads locally.
 */
@Entity(tableName = "cached_threads")
@TypeConverters(Converters::class)
data class CachedThread(
    @PrimaryKey
    val id: String,
    val displayName: String,
    val email: String?,
    val phone: String?,
    val imageUrl: String?,
    val unreadCount: Int,
    val lastMessage: String?,
    val lastMessageTime: Long?,
    val legacyChatSessionId: String?,
    val renterId: String?,
    val ownerId: String?,
    val isPinned: Boolean,
    val members: Map<String, Int>?,
    val address: String?, // Property address
    val propertyId: String?, // Property ID
    val applicationStatus: String?, // Application status
    val bookingStatus: String?, // Booking/viewing status
    val channel: String?, // Channel/platform source
    val createdAt: Long,
    val updatedAt: Long
)
