package com.tomasronis.rhentiapp.data.chathub.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Domain model for a chat thread.
 */
data class ChatThread(
    val id: String,
    val displayName: String,
    val email: String?,
    val phone: String?,
    val imageUrl: String?,
    val unreadCount: Int,
    val lastMessage: String?,
    val lastMessageTime: Long?,
    val legacyChatSessionId: String?, // CRITICAL: Use for sending messages
    val renterId: String?,
    val ownerId: String?,
    val isPinned: Boolean,
    val members: Map<String, Int>? // CRITICAL: Member IDs mapped to badge counts
) {
    /**
     * Get the members object for sending messages.
     * Uses actual member IDs from API, not role names like "renter"/"owner".
     */
    val membersObject: Map<String, Int>
        get() = members ?: emptyMap()
}

/**
 * Domain model for a chat message.
 */
data class ChatMessage(
    val id: String,
    val threadId: String,
    val sender: String, // "owner", "renter", "system"
    val text: String?,
    val type: String, // "text", "image", "booking"
    val attachmentUrl: String?,
    val metadata: MessageMetadata?,
    val status: String, // "sending", "sent", "failed"
    val createdAt: Long
)

/**
 * Metadata for booking messages.
 */
data class MessageMetadata(
    val bookingId: String?,
    val propertyAddress: String?,
    val viewingTime: String?,
    val bookingStatus: String? // "pending", "confirmed", "declined"
)

/**
 * Request to fetch chat threads.
 */
@JsonClass(generateAdapter = true)
data class ThreadsRequest(
    @Json(name = "super_account_id")
    val superAccountId: String,
    @Json(name = "limit")
    val limit: Int? = 50,
    @Json(name = "offset")
    val offset: Int? = 0,
    @Json(name = "search")
    val search: String? = null
)

/**
 * Request to send a text message.
 */
@JsonClass(generateAdapter = true)
data class SendMessageRequest(
    @Json(name = "chat_session_id")
    val chatSessionId: String,
    @Json(name = "message")
    val message: String?,
    @Json(name = "type")
    val type: String, // "text", "image"
    @Json(name = "attachment")
    val attachment: String? = null, // Base64 encoded image
    @Json(name = "chatSessionMembersObj")
    val chatSessionMembersObj: Map<String, Int>? = null
)

/**
 * Request to handle booking action.
 */
@JsonClass(generateAdapter = true)
data class BookingActionRequest(
    @Json(name = "action")
    val action: String, // "confirm" or "decline"
    @Json(name = "super_account_id")
    val superAccountId: String
)

/**
 * Request to propose alternative viewing times.
 */
@JsonClass(generateAdapter = true)
data class AlternativeTimesRequest(
    @Json(name = "booking_id")
    val bookingId: String,
    @Json(name = "alternative_times")
    val alternativeTimes: List<String>,
    @Json(name = "super_account_id")
    val superAccountId: String
)
