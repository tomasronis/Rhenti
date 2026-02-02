package com.tomasronis.rhentiapp.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for caching chat threads locally.
 */
@Entity(tableName = "cached_threads")
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
    val createdAt: Long,
    val updatedAt: Long
)
