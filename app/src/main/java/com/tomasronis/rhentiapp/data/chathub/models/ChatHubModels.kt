package com.tomasronis.rhentiapp.data.chathub.models

import androidx.compose.runtime.Immutable
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Domain model for a chat thread.
 * Marked @Immutable for Compose stability and performance.
 */
@Immutable
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
    val members: Map<String, Int>?, // CRITICAL: Member IDs mapped to badge counts

    // New fields from iOS implementation
    val address: String?, // Property address
    val propertyId: String?, // Property ID
    val applicationStatus: String?, // Application status (e.g., "pending", "approved")
    val bookingStatus: String?, // Viewing/booking status (e.g., "pending", "confirmed")
    val channel: String? // Channel/platform source (e.g., "facebook", "kijiji", "rhenti")
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
 * Marked @Immutable for Compose stability and performance.
 */
@Immutable
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
 * Marked @Immutable for Compose stability and performance.
 */
@Immutable
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

/**
 * Chat member for clear badge request.
 */
@JsonClass(generateAdapter = true)
data class ChatMember(
    @Json(name = "uid")
    val uid: String
)

/**
 * Request to clear unread message badge.
 * Matches iOS implementation for proper badge clearing.
 */
@JsonClass(generateAdapter = true)
data class ClearBadgeRequest(
    @Json(name = "allMembersObjArr")
    val allMembersObjArr: List<ChatMember>,
    @Json(name = "chatSessionId")
    val chatSessionId: String,
    @Json(name = "currentUserId")
    val currentUserId: String
)

/**
 * Represents a message that's pending send to the server.
 * Tracks local state until confirmed by server.
 */
@Immutable
data class PendingMessage(
    val localId: String,
    val text: String?,
    val imageData: String?, // Base64 or data URI
    val createdAt: Long,
    val status: MessageStatus,
    val serverMessageId: String? = null, // Matched server message ID once sent
    val uploadProgress: Float? = null // For large file uploads
)

/**
 * Status of a pending message.
 */
enum class MessageStatus {
    SENDING,
    SENT,
    FAILED
}

/**
 * Display message combining server and pending messages.
 * Sealed class for type-safe message display.
 */
sealed class DisplayMessage {
    abstract val id: String
    abstract val createdAt: Long
    abstract val sender: String
    abstract val text: String?
    abstract val type: String

    data class Server(val message: ChatMessage) : DisplayMessage() {
        override val id: String = message.id
        override val createdAt: Long = message.createdAt
        override val sender: String = message.sender
        override val text: String? = message.text
        override val type: String = message.type
    }

    data class Pending(val pendingMessage: PendingMessage) : DisplayMessage() {
        override val id: String = pendingMessage.serverMessageId ?: pendingMessage.localId
        override val createdAt: Long = pendingMessage.createdAt
        override val sender: String = "owner" // Pending messages are always from us
        override val text: String? = pendingMessage.text
        override val type: String = if (pendingMessage.imageData != null) "image" else "text"

        val isPending: Boolean = true
        val status: MessageStatus = pendingMessage.status
        val uploadProgress: Float? = pendingMessage.uploadProgress
    }
}
