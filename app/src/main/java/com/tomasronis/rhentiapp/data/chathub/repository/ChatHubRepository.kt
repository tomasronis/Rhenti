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
     */
    suspend fun getThreads(superAccountId: String, search: String? = null): NetworkResult<List<ChatThread>>

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
     * @param chatSessionId The legacy chat session ID (from thread.legacyChatSessionId)
     * @param text The message text
     */
    suspend fun sendTextMessage(
        senderId: String,
        chatSessionId: String,
        text: String
    ): NetworkResult<ChatMessage>

    /**
     * Send an image message.
     *
     * @param senderId The sender's user ID
     * @param chatSessionId The legacy chat session ID
     * @param imageBase64 Base64 encoded image with data URI prefix
     */
    suspend fun sendImageMessage(
        senderId: String,
        chatSessionId: String,
        imageBase64: String
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
