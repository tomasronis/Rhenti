package com.tomasronis.rhentiapp.data.chathub.repository

import com.tomasronis.rhentiapp.BuildConfig
import com.tomasronis.rhentiapp.core.database.dao.MessageDao
import com.tomasronis.rhentiapp.core.database.dao.ThreadDao
import com.tomasronis.rhentiapp.core.database.entities.CachedMessage
import com.tomasronis.rhentiapp.core.database.entities.CachedThread
import com.tomasronis.rhentiapp.core.network.ApiClient
import com.tomasronis.rhentiapp.core.network.NetworkResult
import com.tomasronis.rhentiapp.data.chathub.models.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatHubRepositoryImpl @Inject constructor(
    private val apiClient: ApiClient,
    private val threadDao: ThreadDao,
    private val messageDao: MessageDao
) : ChatHubRepository {

    override suspend fun getThreads(superAccountId: String, search: String?): NetworkResult<List<ChatThread>> {
        return try {
            val request = mapOf(
                "super_account_id" to superAccountId,
                "limit" to 50,
                "offset" to 0
            ).let { base ->
                search?.let { base + ("search" to it) } ?: base
            }

            val response = apiClient.getThreads(request)

            if (BuildConfig.DEBUG) {
                android.util.Log.d("ChatHubRepository", "Threads response: $response")
            }

            val threads = parseThreadsResponse(response)

            // Cache threads
            val cachedThreads = threads.map { it.toCachedThread() }
            threadDao.insertThreads(cachedThreads)

            NetworkResult.Success(threads)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                android.util.Log.e("ChatHubRepository", "Get threads failed", e)
            }
            // Return cached data on error
            val cachedThreads = threadDao.getAllThreads()
            NetworkResult.Error(
                exception = e,
                cachedData = null
            )
        }
    }

    override suspend fun getMessages(
        threadId: String,
        beforeId: String?,
        limit: Int
    ): NetworkResult<List<ChatMessage>> {
        return try {
            val response = apiClient.getMessages(threadId, limit, beforeId)

            if (BuildConfig.DEBUG) {
                android.util.Log.d("ChatHubRepository", "Messages response: $response")
            }

            val messages = parseMessagesResponse(response, threadId)

            // Cache messages
            val cachedMessages = messages.map { it.toCachedMessage() }
            messageDao.insertMessages(cachedMessages)

            NetworkResult.Success(messages)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                android.util.Log.e("ChatHubRepository", "Get messages failed", e)
            }
            NetworkResult.Error(
                exception = e,
                cachedData = null
            )
        }
    }

    override suspend fun sendTextMessage(
        senderId: String,
        chatSessionId: String,
        text: String
    ): NetworkResult<ChatMessage> {
        return try {
            val request = mapOf(
                "chat_session_id" to chatSessionId,
                "message" to text,
                "type" to "text",
                "chatSessionMembersObj" to mapOf(
                    "renter" to 0,
                    "owner" to 0
                )
            )

            val response = apiClient.sendMessage(senderId, request)

            if (BuildConfig.DEBUG) {
                android.util.Log.d("ChatHubRepository", "Send message response: $response")
            }

            val message = parseSendMessageResponse(response, chatSessionId)

            // Cache the sent message
            messageDao.insertMessage(message.toCachedMessage())

            NetworkResult.Success(message)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                android.util.Log.e("ChatHubRepository", "Send message failed", e)
            }
            NetworkResult.Error(
                exception = e,
                cachedData = null
            )
        }
    }

    override suspend fun sendImageMessage(
        senderId: String,
        chatSessionId: String,
        imageBase64: String
    ): NetworkResult<ChatMessage> {
        return try {
            val request = mapOf(
                "chat_session_id" to chatSessionId,
                "type" to "image",
                "attachment" to imageBase64,
                "chatSessionMembersObj" to mapOf(
                    "renter" to 0,
                    "owner" to 0
                )
            )

            val response = apiClient.sendMessage(senderId, request)

            if (BuildConfig.DEBUG) {
                android.util.Log.d("ChatHubRepository", "Send image response: $response")
            }

            val message = parseSendMessageResponse(response, chatSessionId)

            // Cache the sent message
            messageDao.insertMessage(message.toCachedMessage())

            NetworkResult.Success(message)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                android.util.Log.e("ChatHubRepository", "Send image failed", e)
            }
            NetworkResult.Error(
                exception = e,
                cachedData = null
            )
        }
    }

    override suspend fun clearBadge(threadId: String): NetworkResult<Unit> {
        return try {
            apiClient.clearBadge(threadId)

            // Update local cache
            threadDao.clearUnreadCount(threadId)

            NetworkResult.Success(Unit)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                android.util.Log.e("ChatHubRepository", "Clear badge failed", e)
            }
            NetworkResult.Error(
                exception = e,
                cachedData = null
            )
        }
    }

    override suspend fun handleBookingAction(
        bookingId: String,
        action: String,
        superAccountId: String
    ): NetworkResult<Unit> {
        return try {
            val request = mapOf(
                "action" to action,
                "super_account_id" to superAccountId
            )

            apiClient.handleBooking(bookingId, request)

            NetworkResult.Success(Unit)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                android.util.Log.e("ChatHubRepository", "Handle booking failed", e)
            }
            NetworkResult.Error(
                exception = e,
                cachedData = null
            )
        }
    }

    override suspend fun proposeAlternativeTimes(
        bookingId: String,
        times: List<String>,
        superAccountId: String
    ): NetworkResult<Unit> {
        return try {
            val request = mapOf(
                "booking_id" to bookingId,
                "alternative_times" to times,
                "super_account_id" to superAccountId
            )

            apiClient.proposeAlternativeTimes(request)

            NetworkResult.Success(Unit)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                android.util.Log.e("ChatHubRepository", "Propose alternatives failed", e)
            }
            NetworkResult.Error(
                exception = e,
                cachedData = null
            )
        }
    }

    override suspend fun updateThreadPinned(threadId: String, pinned: Boolean) {
        threadDao.updatePinned(threadId, pinned)
    }

    override suspend fun deleteThread(threadId: String) {
        threadDao.deleteThreadById(threadId)
    }

    override fun observeThreads(): Flow<List<ChatThread>> {
        return threadDao.getAllThreads().map { threads ->
            threads.map { it.toDomainModel() }
        }
    }

    override fun observeMessages(threadId: String): Flow<List<ChatMessage>> {
        return messageDao.getMessagesByThread(threadId).map { messages ->
            messages.map { it.toDomainModel() }
        }
    }

    override fun observeTotalUnreadCount(): Flow<Int> {
        return threadDao.getAllThreads().map { threads ->
            threads.sumOf { it.unreadCount }
        }
    }

    // Helper functions to parse API responses

    private fun parseThreadsResponse(response: Map<String, Any>): List<ChatThread> {
        val threads = mutableListOf<ChatThread>()

        @Suppress("UNCHECKED_CAST")
        val threadsData = response["threads"] as? List<Map<String, Any>> ?: emptyList()

        for (threadData in threadsData) {
            try {
                val id = threadData["_id"] as? String ?: continue
                val displayName = (threadData["display_name"] as? String)
                    ?: (threadData["displayName"] as? String)
                    ?: "Unknown"
                val email = threadData["email"] as? String
                val phone = threadData["phone"] as? String
                val imageUrl = (threadData["image_url"] as? String)
                    ?: (threadData["imageUrl"] as? String)
                val unreadCount = ((threadData["unread_count"] as? Number)
                    ?: (threadData["unreadCount"] as? Number))?.toInt() ?: 0
                val lastMessage = (threadData["last_message"] as? String)
                    ?: (threadData["lastMessage"] as? String)
                val lastMessageTime = ((threadData["last_message_time"] as? Number)
                    ?: (threadData["lastMessageTime"] as? Number))?.toLong()
                val legacyChatSessionId = (threadData["legacy_chat_session_id"] as? String)
                    ?: (threadData["legacyChatSessionId"] as? String)
                val renterId = (threadData["renter_id"] as? String)
                    ?: (threadData["renterId"] as? String)
                val ownerId = (threadData["owner_id"] as? String)
                    ?: (threadData["ownerId"] as? String)
                val isPinned = ((threadData["is_pinned"] as? Boolean)
                    ?: (threadData["isPinned"] as? Boolean)) ?: false

                threads.add(
                    ChatThread(
                        id = id,
                        displayName = displayName,
                        email = email,
                        phone = phone,
                        imageUrl = imageUrl,
                        unreadCount = unreadCount,
                        lastMessage = lastMessage,
                        lastMessageTime = lastMessageTime,
                        legacyChatSessionId = legacyChatSessionId,
                        renterId = renterId,
                        ownerId = ownerId,
                        isPinned = isPinned
                    )
                )
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) {
                    android.util.Log.w("ChatHubRepository", "Failed to parse thread: $threadData", e)
                }
            }
        }

        return threads
    }

    private fun parseMessagesResponse(response: Map<String, Any>, threadId: String): List<ChatMessage> {
        val messages = mutableListOf<ChatMessage>()

        @Suppress("UNCHECKED_CAST")
        val messagesData = response["messages"] as? List<Map<String, Any>> ?: emptyList()

        for (messageData in messagesData) {
            try {
                val id = messageData["_id"] as? String ?: continue
                val sender = messageData["sender"] as? String ?: "system"
                val text = messageData["text"] as? String
                val type = messageData["type"] as? String ?: "text"
                val attachmentUrl = (messageData["attachment_url"] as? String)
                    ?: (messageData["attachmentUrl"] as? String)
                val createdAt = ((messageData["created_at"] as? Number)
                    ?: (messageData["createdAt"] as? Number))?.toLong() ?: System.currentTimeMillis()

                // Parse metadata for booking messages
                val metadata = if (type == "booking") {
                    @Suppress("UNCHECKED_CAST")
                    val metadataMap = messageData["metadata"] as? Map<String, Any>
                    metadataMap?.let {
                        MessageMetadata(
                            bookingId = (it["booking_id"] as? String) ?: (it["bookingId"] as? String),
                            propertyAddress = (it["property_address"] as? String) ?: (it["propertyAddress"] as? String),
                            viewingTime = (it["viewing_time"] as? String) ?: (it["viewingTime"] as? String),
                            bookingStatus = (it["booking_status"] as? String) ?: (it["bookingStatus"] as? String)
                        )
                    }
                } else null

                messages.add(
                    ChatMessage(
                        id = id,
                        threadId = threadId,
                        sender = sender,
                        text = text,
                        type = type,
                        attachmentUrl = attachmentUrl,
                        metadata = metadata,
                        status = "sent",
                        createdAt = createdAt
                    )
                )
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) {
                    android.util.Log.w("ChatHubRepository", "Failed to parse message: $messageData", e)
                }
            }
        }

        return messages
    }

    private fun parseSendMessageResponse(response: Map<String, Any>, threadId: String): ChatMessage {
        val id = response["_id"] as? String ?: java.util.UUID.randomUUID().toString()
        val sender = response["sender"] as? String ?: "owner"
        val text = response["text"] as? String
        val type = response["type"] as? String ?: "text"
        val attachmentUrl = response["attachment_url"] as? String
        val createdAt = (response["created_at"] as? Number)?.toLong() ?: System.currentTimeMillis()

        return ChatMessage(
            id = id,
            threadId = threadId,
            sender = sender,
            text = text,
            type = type,
            attachmentUrl = attachmentUrl,
            metadata = null,
            status = "sent",
            createdAt = createdAt
        )
    }
}

// Extension functions to convert between domain and cache models

private fun ChatThread.toCachedThread(): CachedThread {
    return CachedThread(
        id = id,
        displayName = displayName,
        email = email,
        phone = phone,
        imageUrl = imageUrl,
        unreadCount = unreadCount,
        lastMessage = lastMessage,
        lastMessageTime = lastMessageTime,
        legacyChatSessionId = legacyChatSessionId,
        renterId = renterId,
        ownerId = ownerId,
        isPinned = isPinned,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )
}

private fun CachedThread.toDomainModel(): ChatThread {
    return ChatThread(
        id = id,
        displayName = displayName,
        email = email,
        phone = phone,
        imageUrl = imageUrl,
        unreadCount = unreadCount,
        lastMessage = lastMessage,
        lastMessageTime = lastMessageTime,
        legacyChatSessionId = legacyChatSessionId,
        renterId = renterId,
        ownerId = ownerId,
        isPinned = isPinned
    )
}

private fun ChatMessage.toCachedMessage(): CachedMessage {
    return CachedMessage(
        id = id,
        threadId = threadId,
        sender = sender,
        text = text,
        type = type,
        attachmentUrl = attachmentUrl,
        metadata = null, // Could serialize MessageMetadata to JSON if needed
        status = status,
        readAt = null,
        createdAt = createdAt,
        updatedAt = System.currentTimeMillis()
    )
}

private fun CachedMessage.toDomainModel(): ChatMessage {
    return ChatMessage(
        id = id,
        threadId = threadId,
        sender = sender,
        text = text,
        type = type,
        attachmentUrl = attachmentUrl,
        metadata = null, // Could parse from JSON metadata field
        status = status,
        createdAt = createdAt
    )
}
