package com.tomasronis.rhentiapp.data.chathub.repository

import com.tomasronis.rhentiapp.core.network.NetworkResult
import com.tomasronis.rhentiapp.data.chathub.models.*
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for chat hub operations.
 * Manages fetching, caching, and sending messages.
 */
interface ChatHubRepository {

    /**
     * Get all threads for the current user.
     * Fetches from API and caches locally.
     *
     * @param superAccountId The super account ID
     * @param search Optional search query
     * @param skip Number of threads to skip (for pagination)
     * @param limit Number of threads to fetch
     */
    suspend fun getThreads(
        superAccountId: String,
        search: String? = null,
        skip: Int = 0,
        limit: Int = 20
    ): NetworkResult<List<ChatThread>>

    /**
     * Get messages for a specific thread with pagination.
     *
     * @param threadId The thread ID
     * @param beforeId Message ID to fetch messages before (for pagination)
     * @param limit Number of messages to fetch
     */
    suspend fun getMessages(
        threadId: String,
        beforeId: String? = null,
        limit: Int = 20
    ): NetworkResult<List<ChatMessage>>

    /**
     * Send a text message.
     *
     * @param senderId The sender's user ID
     * @param userName The sender's full name
     * @param chatSessionId The legacy chat session ID (from thread.legacyChatSessionId)
     * @param text The message text
     * @param thread The current thread (for membersObject)
     */
    suspend fun sendTextMessage(
        senderId: String,
        userName: String,
        chatSessionId: String,
        text: String,
        thread: ChatThread
    ): NetworkResult<ChatMessage>

    /**
     * Send an image message.
     *
     * @param senderId The sender's user ID
     * @param userName The sender's full name
     * @param chatSessionId The legacy chat session ID
     * @param imageBase64 Base64 encoded image (without data URI prefix)
     * @param thread The current thread (for membersObject)
     */
    suspend fun sendImageMessage(
        senderId: String,
        userName: String,
        chatSessionId: String,
        imageBase64: String,
        thread: ChatThread
    ): NetworkResult<ChatMessage>

    /**
     * Send a link message (viewing or application).
     *
     * @param senderId The sender's user ID
     * @param userName The sender's full name
     * @param chatSessionId The legacy chat session ID
     * @param messageType "viewing-link" or "application-link"
     * @param text The message text
     * @param propertyAddress The property address
     * @param propertyId Optional property ID
     * @param thread The current thread (for membersObject)
     */
    suspend fun sendLinkMessage(
        senderId: String,
        userName: String,
        chatSessionId: String,
        messageType: String,
        text: String,
        propertyAddress: String,
        propertyId: String?,
        thread: ChatThread
    ): NetworkResult<ChatMessage>

    /**
     * Clear unread badge for a thread.
     */
    suspend fun clearBadge(threadId: String): NetworkResult<Unit>

    /**
     * Handle booking action (approve/decline).
     */
    suspend fun handleBookingAction(
        bookingId: String,
        action: String,
        superAccountId: String
    ): NetworkResult<Unit>

    /**
     * Propose alternative viewing times.
     */
    suspend fun proposeAlternativeTimes(
        bookingId: String,
        times: List<String>,
        superAccountId: String
    ): NetworkResult<Unit>

    /**
     * Send a pre-approved viewing to the contact.
     * Creates a booking message that is already confirmed.
     */
    suspend fun sendPreApprovedViewing(
        senderId: String,
        userName: String,
        chatSessionId: String,
        propertyAddress: String,
        propertyId: String?,
        viewingTimeIso: String,
        thread: ChatThread
    ): NetworkResult<ChatMessage>

    /**
     * Check-in to a confirmed viewing.
     */
    suspend fun checkInViewing(
        bookingId: String,
        superAccountId: String
    ): NetworkResult<Unit>

    /**
     * Pin or unpin a thread.
     */
    suspend fun updateThreadPinned(threadId: String, pinned: Boolean)

    /**
     * Delete a thread.
     */
    suspend fun deleteThread(threadId: String)

    /**
     * Observe cached threads (reactive).
     */
    fun observeThreads(): Flow<List<ChatThread>>

    /**
     * Observe cached messages for a thread (reactive).
     */
    fun observeMessages(threadId: String): Flow<List<ChatMessage>>

    /**
     * Get total unread count across all threads.
     */
    fun observeTotalUnreadCount(): Flow<Int>
}
