package com.tomasronis.rhentiapp.core.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for caching messages locally.
 */
@Entity(
    tableName = "cached_messages",
    foreignKeys = [
        ForeignKey(
            entity = CachedThread::class,
            parentColumns = ["id"],
            childColumns = ["threadId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("threadId")]
)
data class CachedMessage(
    @PrimaryKey
    val id: String,
    val threadId: String,
    val sender: String, // "owner", "renter", or "system"
    val text: String?,
    val type: String, // "text", "image", "pdf", "booking", "application"
    val attachmentUrl: String?,
    val metadata: String?, // JSON string for booking/application metadata
    val status: String, // "sending", "sent", "failed", "pending"
    val readAt: Long?,
    val createdAt: Long,
    val updatedAt: Long
)
