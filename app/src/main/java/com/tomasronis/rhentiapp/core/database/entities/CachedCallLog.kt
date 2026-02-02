package com.tomasronis.rhentiapp.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for caching call logs locally.
 */
@Entity(tableName = "cached_call_logs")
data class CachedCallLog(
    @PrimaryKey
    val id: String,
    val callSid: String?,
    val callStatus: String, // "completed", "failed", "busy", "no-answer"
    val callType: String, // "incoming", "outgoing"
    val startTime: Long,
    val callDuration: Int, // in seconds
    val callerNumber: String?,
    val callerName: String?,
    val contactId: String?,
    val createdAt: Long,
    val updatedAt: Long
) {
    /**
     * Returns a formatted duration string (e.g., "2:30").
     */
    val formattedDuration: String
        get() {
            val minutes = callDuration / 60
            val seconds = callDuration % 60
            return String.format("%d:%02d", minutes, seconds)
        }
}
