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
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatHubRepositoryImpl @Inject constructor(
    private val apiClient: ApiClient,
    private val threadDao: ThreadDao,
    private val messageDao: MessageDao,
    private val tokenManager: com.tomasronis.rhentiapp.core.security.TokenManager,
    private val contactDao: com.tomasronis.rhentiapp.core.database.dao.ContactDao
) : ChatHubRepository {

    override suspend fun getThreads(
        superAccountId: String,
        search: String?,
        skip: Int,
        limit: Int
    ): NetworkResult<List<ChatThread>> {
        return try {
            // Build request with correct format (skip instead of offset)
            val request = buildMap<String, Any> {
                put("searchText", search ?: "")
                put("skip", skip)
                put("limit", limit)
                // Only include null values for the API - they're explicitly expected
            }

            if (BuildConfig.DEBUG) {
                android.util.Log.d("ChatHubRepository", "Fetching threads with request: $request")
            }

            val response = apiClient.getThreads(request)

            if (BuildConfig.DEBUG) {
                android.util.Log.d("ChatHubRepository", "Threads response: $response")
            }

            val threads = parseThreadsResponse(response)

            if (BuildConfig.DEBUG) {
                android.util.Log.d("ChatHubRepository", "Parsed ${threads.size} threads")
                threads.firstOrNull()?.let { thread ->
                    android.util.Log.d("ChatHubRepository", "First thread: id=${thread.id}, name=${thread.displayName}, members=${thread.members}")
                }
            }

            // Cache threads
            val cachedThreads = threads.map { it.toCachedThread() }
            threadDao.insertThreads(cachedThreads)

            if (BuildConfig.DEBUG) {
                android.util.Log.d("ChatHubRepository", "Cached ${cachedThreads.size} threads to database")
            }

            // Update contacts with channel data from threads
            updateContactsChannelFromThreads(threads)

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
        userName: String,
        chatSessionId: String,
        text: String,
        thread: ChatThread
    ): NetworkResult<ChatMessage> {
        return try {
            val timestamp = System.currentTimeMillis()

            // Build request with correct iOS format
            val request = mapOf(
                "message" to mapOf(
                    "_id" to timestamp,
                    "createdAt" to timestamp,
                    "text" to text,
                    "user" to mapOf(
                        "name" to userName,
                        "_id" to senderId
                    )
                ),
                "chatSessionId" to chatSessionId,
                "chatSessionMembersObj" to thread.membersObject
            )

            if (BuildConfig.DEBUG) {
                android.util.Log.d("ChatHubRepository", "Sending message with format: $request")
            }

            val response = apiClient.sendMessage(senderId, request)

            if (BuildConfig.DEBUG) {
                android.util.Log.d("ChatHubRepository", "Send message response: $response")
            }

            val message = parseSendMessageResponse(response, thread.id)

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
        userName: String,
        chatSessionId: String,
        imageBase64: String,
        thread: ChatThread
    ): NetworkResult<ChatMessage> {
        return try {
            val timestamp = System.currentTimeMillis()

            // Add data URI prefix as iOS does
            val base64WithPrefix = "data:image/jpeg;base64,$imageBase64"

            // Build request with correct iOS format
            val request = mapOf(
                "message" to mapOf(
                    "_id" to timestamp,
                    "createdAt" to timestamp,
                    "base64" to base64WithPrefix,  // Use "base64", not "attachment"
                    "user" to mapOf(
                        "name" to userName,
                        "_id" to senderId
                    )
                ),
                "chatSessionId" to chatSessionId,
                "chatSessionMembersObj" to thread.membersObject
            )

            if (BuildConfig.DEBUG) {
                android.util.Log.d("ChatHubRepository", "Sending image with format: $request")
            }

            val response = apiClient.sendMessage(senderId, request)

            if (BuildConfig.DEBUG) {
                android.util.Log.d("ChatHubRepository", "Send image response: $response")
            }

            val message = parseSendMessageResponse(response, thread.id)

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

    override suspend fun sendLinkMessage(
        senderId: String,
        userName: String,
        chatSessionId: String,
        messageType: String,
        text: String,
        propertyAddress: String,
        propertyId: String?,
        thread: ChatThread
    ): NetworkResult<ChatMessage> {
        return try {
            val timestamp = System.currentTimeMillis()

            // Determine correct type and metadata based on messageType
            val (actualType, linkTypeKey, linkTypeValue) = when (messageType) {
                "viewing-link" -> Triple("booking", "bookViewingType", "link")
                "application-link" -> Triple("application", "applicationType", "link")
                else -> Triple("text", null, null)
            }

            // Build request with correct type and metadata structure
            val messageMap = mutableMapOf<String, Any>(
                "_id" to timestamp,
                "createdAt" to timestamp,
                "text" to text,
                "type" to actualType,
                "user" to mapOf(
                    "name" to userName,
                    "_id" to senderId
                )
            )

            // Add metadata with property information and link type
            val metadata = mutableMapOf<String, Any>(
                "propertyAddress" to propertyAddress
            )
            propertyId?.let { metadata["propertyId"] = it }
            // Add the specific link type indicator (e.g., applicationType: "link" or bookViewingType: "link")
            if (linkTypeKey != null && linkTypeValue != null) {
                metadata[linkTypeKey] = linkTypeValue
            }
            messageMap["metadata"] = metadata

            val request = mapOf(
                "message" to messageMap,
                "chatSessionId" to chatSessionId,
                "chatSessionMembersObj" to thread.membersObject
            )

            if (BuildConfig.DEBUG) {
                android.util.Log.d("ChatHubRepository", "Sending link message with format: $request")
            }

            val response = apiClient.sendMessage(senderId, request)

            if (BuildConfig.DEBUG) {
                android.util.Log.d("ChatHubRepository", "Send link message response: $response")
            }

            val message = parseSendMessageResponse(response, thread.id)

            // Cache the sent message
            messageDao.insertMessage(message.toCachedMessage())

            NetworkResult.Success(message)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                android.util.Log.e("ChatHubRepository", "Send link message failed", e)
            }
            NetworkResult.Error(
                exception = e,
                cachedData = null
            )
        }
    }

    override suspend fun clearBadge(threadId: String): NetworkResult<Unit> {
        return try {
            // Get the thread to access its members and legacyChatSessionId
            val thread = threadDao.getThreadById(threadId)
                .map { it?.toDomainModel() }
                .firstOrNull()

            if (thread == null) {
                if (BuildConfig.DEBUG) {
                    android.util.Log.e("ChatHubRepository", "Clear badge failed: Thread not found")
                }
                return NetworkResult.Error(
                    exception = IllegalStateException("Thread not found"),
                    cachedData = null
                )
            }

            // Get super account ID from token manager
            val superAccountId = tokenManager.getSuperAccountId()

            if (superAccountId == null || thread.legacyChatSessionId == null) {
                if (BuildConfig.DEBUG) {
                    android.util.Log.e("ChatHubRepository", "Clear badge failed: Missing superAccountId or legacyChatSessionId")
                }
                return NetworkResult.Error(
                    exception = IllegalStateException("Missing required data"),
                    cachedData = null
                )
            }

            // Build request matching iOS implementation
            val memberIds = thread.membersObject.keys.toList()
            val request = mapOf(
                "allMembersObjArr" to memberIds.map { mapOf("uid" to it) },
                "chatSessionId" to thread.legacyChatSessionId,
                "currentUserId" to superAccountId
            )

            if (BuildConfig.DEBUG) {
                android.util.Log.d("ChatHubRepository", "Clearing badge with request: $request")
            }

            apiClient.clearBadge(request)

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

    /**
     * Parse timestamp from API - handles both Unix timestamp (Number) and ISO 8601 string (String).
     */
    private fun parseTimestamp(value: Any?, messageId: String): Long {
        return when (value) {
            is Number -> {
                value.toLong()
            }
            is String -> {
                try {
                    // Try parsing ISO 8601 format: "2026-02-08T12:00:00Z" or "2026-02-08T12:00:00.000Z"
                    val isoFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.US).apply {
                        timeZone = java.util.TimeZone.getTimeZone("UTC")
                    }
                    val isoFormatWithMillis = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", java.util.Locale.US).apply {
                        timeZone = java.util.TimeZone.getTimeZone("UTC")
                    }

                    val cleanedValue = value.replace("Z", "").replace("+00:00", "")
                    val date = try {
                        isoFormatWithMillis.parse(cleanedValue)
                    } catch (e: Exception) {
                        isoFormat.parse(cleanedValue)
                    }

                    date?.time ?: run {
                        if (BuildConfig.DEBUG) {
                            android.util.Log.w("ChatHubRepository", "⚠️ Failed to parse ISO date string: $value for message $messageId")
                        }
                        System.currentTimeMillis()
                    }
                } catch (e: Exception) {
                    if (BuildConfig.DEBUG) {
                        android.util.Log.w("ChatHubRepository", "⚠️ Failed to parse timestamp string: $value for message $messageId", e)
                    }
                    System.currentTimeMillis()
                }
            }
            else -> {
                if (BuildConfig.DEBUG) {
                    android.util.Log.w("ChatHubRepository", "⚠️ No timestamp found for message $messageId (value: $value, type: ${value?.javaClass?.simpleName}), using current time")
                }
                System.currentTimeMillis()
            }
        }
    }

    private fun parseThreadsResponse(response: Map<String, Any>): List<ChatThread> {
        val threads = mutableListOf<ChatThread>()

        @Suppress("UNCHECKED_CAST")
        val threadsData = response["chatThreads"] as? List<Map<String, Any>> ?: emptyList()

        for (threadData in threadsData) {
            try {
                // Support both "id" and "_id" field names
                val id = (threadData["id"] as? String)
                    ?: (threadData["_id"] as? String)
                    ?: continue
                val displayName = (threadData["display_name"] as? String)
                    ?: (threadData["displayName"] as? String)
                    ?: "Unknown"
                val email = threadData["email"] as? String
                val phone = threadData["phone"] as? String
                val imageUrlRaw = (threadData["image"] as? String)
                    ?: (threadData["image_url"] as? String)
                    ?: (threadData["imageUrl"] as? String)
                val imageUrl = imageUrlRaw?.let { buildFullImageUrl(it) }
                // Support both unReadCount and unreadCount
                val unreadCount = ((threadData["unReadCount"] as? Number)
                    ?: (threadData["unreadCount"] as? Number)
                    ?: (threadData["unread_count"] as? Number))?.toInt() ?: 0
                val lastMessage = (threadData["last_message"] as? String)
                    ?: (threadData["lastMessage"] as? String)
                // Support both lastMessageTime string and timestamp
                val lastMessageTimeStr = (threadData["lastMessageTime"] as? String)
                    ?: (threadData["last_message_time"] as? String)
                val lastMessageTime = if (lastMessageTimeStr != null) {
                    // Parse ISO 8601 string to timestamp
                    try {
                        java.time.Instant.parse(lastMessageTimeStr).toEpochMilli()
                    } catch (e: Exception) {
                        null
                    }
                } else {
                    ((threadData["lastMessageTime"] as? Number)
                        ?: (threadData["last_message_time"] as? Number))?.toLong()
                }
                val legacyChatSessionId = (threadData["legacy_chat_session_id"] as? String)
                    ?: (threadData["legacyChatSessionId"] as? String)
                val renterId = (threadData["renter_id"] as? String)
                    ?: (threadData["renterId"] as? String)
                val ownerId = (threadData["owner_id"] as? String)
                    ?: (threadData["ownerId"] as? String)
                val isPinned = ((threadData["is_pinned"] as? Boolean)
                    ?: (threadData["isPinned"] as? Boolean)) ?: false

                // Parse members map - convert Number values to Int
                @Suppress("UNCHECKED_CAST")
                val membersRaw = threadData["members"] as? Map<String, Any>
                val members = membersRaw?.mapValues { (_, value) ->
                    (value as? Number)?.toInt() ?: 0
                }

                // Parse new fields from iOS implementation
                val address = threadData["address"] as? String
                val propertyId = (threadData["property_id"] as? String)
                    ?: (threadData["propertyId"] as? String)
                val applicationStatus = (threadData["application_status"] as? String)
                    ?: (threadData["applicationStatus"] as? String)
                val bookingStatus = (threadData["booking_status"] as? String)
                    ?: (threadData["bookingStatus"] as? String)
                val channel = threadData["channel"] as? String

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
                        isPinned = isPinned,
                        members = members,
                        address = address,
                        propertyId = propertyId,
                        applicationStatus = applicationStatus,
                        bookingStatus = bookingStatus,
                        channel = channel
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

                if (BuildConfig.DEBUG) {
                    android.util.Log.d("ChatHubRepository", "Message data keys: ${messageData.keys}")
                    android.util.Log.d("ChatHubRepository", "  _id: ${messageData["_id"]}")
                    android.util.Log.d("ChatHubRepository", "  sentAt: ${messageData["sentAt"]} (${messageData["sentAt"]?.javaClass?.simpleName})")
                    android.util.Log.d("ChatHubRepository", "  createdAt: ${messageData["createdAt"]} (${messageData["createdAt"]?.javaClass?.simpleName})")
                }

                val sender = messageData["sender"] as? String ?: "system"
                val text = messageData["text"] as? String
                val type = messageData["type"] as? String ?: "text"
                val attachmentUrl = (messageData["attachment_url"] as? String)
                    ?: (messageData["attachmentUrl"] as? String)

                if (BuildConfig.DEBUG) {
                    android.util.Log.d("ChatHubRepository", "Message $id: type='$type', sender='$sender', text='${text?.take(50)}'")
                }

                // Parse timestamp - can be Number (Unix timestamp) or String (ISO 8601)
                val createdAtRaw = parseTimestamp(
                    messageData["sentAt"]
                        ?: messageData["sent_at"]
                        ?: messageData["createdAt"]
                        ?: messageData["created_at"],
                    id
                )

                // Check if timestamp is in seconds (10 digits) or milliseconds (13 digits)
                val createdAt = if (createdAtRaw < 10000000000L) {
                    // Timestamp is in seconds, convert to milliseconds
                    if (BuildConfig.DEBUG) {
                        android.util.Log.d("ChatHubRepository", "Converting timestamp from seconds to milliseconds: $createdAtRaw -> ${createdAtRaw * 1000}")
                    }
                    createdAtRaw * 1000
                } else {
                    // Already in milliseconds
                    createdAtRaw
                }

                if (BuildConfig.DEBUG) {
                    android.util.Log.d("ChatHubRepository", "  Final timestamp: $createdAt (${java.util.Date(createdAt)})")
                }

                // Parse metadata for booking, application, and items-requested messages
                val metadata = if (type == "booking" || type == "items-requested" || type == "application") {
                    @Suppress("UNCHECKED_CAST")
                    val metadataMap = messageData["metadata"] as? Map<String, Any>

                    if (BuildConfig.DEBUG && metadataMap != null) {
                        android.util.Log.d("ChatHubRepository", "=== METADATA FOR MESSAGE $id ===")
                        android.util.Log.d("ChatHubRepository", "Type: $type")
                        android.util.Log.d("ChatHubRepository", "Metadata keys: ${metadataMap.keys}")
                        metadataMap.forEach { (key, value) ->
                            android.util.Log.d("ChatHubRepository", "  $key: $value (${value?.javaClass?.simpleName})")
                        }
                    }

                    metadataMap?.let { parseMessageMetadata(it) }
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

        // API returns messages newest-first, reverse to oldest-first (matches iOS)
        val reversed = messages.reversed()

        if (BuildConfig.DEBUG && reversed.isNotEmpty()) {
            android.util.Log.d("ChatHubRepository", "Parsed ${reversed.size} messages:")
            android.util.Log.d("ChatHubRepository", "  First (oldest): id=${reversed.first().id}, createdAt=${reversed.first().createdAt}, text=${reversed.first().text?.take(30)}")
            android.util.Log.d("ChatHubRepository", "  Last (newest): id=${reversed.last().id}, createdAt=${reversed.last().createdAt}, text=${reversed.last().text?.take(30)}")

            // Check if timestamps are in correct order
            val sorted = reversed.sortedBy { it.createdAt }
            if (sorted != reversed) {
                android.util.Log.w("ChatHubRepository", "⚠️ Messages are NOT in chronological order after reversal!")
                android.util.Log.w("ChatHubRepository", "First timestamp: ${reversed.first().createdAt}")
                android.util.Log.w("ChatHubRepository", "Last timestamp: ${reversed.last().createdAt}")
            } else {
                android.util.Log.d("ChatHubRepository", "✓ Messages are in correct chronological order")
            }
        }

        return reversed
    }

    private fun parseSendMessageResponse(response: Map<String, Any>, threadId: String): ChatMessage {
        val id = response["_id"] as? String ?: java.util.UUID.randomUUID().toString()
        val sender = response["sender"] as? String ?: "owner"
        val text = response["text"] as? String
        val type = response["type"] as? String ?: "text"
        val attachmentUrl = response["attachment_url"] as? String

        // Parse timestamp - can be Number (Unix timestamp) or String (ISO 8601)
        val createdAtRaw = parseTimestamp(
            response["sentAt"]
                ?: response["sent_at"]
                ?: response["createdAt"]
                ?: response["created_at"],
            id
        )

        // Check if timestamp is in seconds or milliseconds
        val createdAt = if (createdAtRaw < 10000000000L) {
            createdAtRaw * 1000
        } else {
            createdAtRaw
        }

        // Parse metadata for booking, application, and items-requested messages
        val metadata = if (type == "booking" || type == "items-requested" || type == "application") {
            @Suppress("UNCHECKED_CAST")
            val metadataMap = response["metadata"] as? Map<String, Any>
            metadataMap?.let { parseMessageMetadata(it) }
        } else null

        return ChatMessage(
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
    }

    /**
     * Update contacts with channel data from threads.
     * This syncs the channel information from chat threads to contacts.
     */
    private suspend fun updateContactsChannelFromThreads(threads: List<ChatThread>) {
        try {
            if (BuildConfig.DEBUG) {
                android.util.Log.d("ChatHubRepository", "=== SYNCING CHANNEL DATA FROM THREADS TO CONTACTS ===")
                android.util.Log.d("ChatHubRepository", "Processing ${threads.size} threads")
            }

            var updatedCount = 0
            for (thread in threads) {
                if (BuildConfig.DEBUG) {
                    android.util.Log.d(
                        "ChatHubRepository",
                        "Thread: id=${thread.id}, name=${thread.displayName}, email=${thread.email}, phone=${thread.phone}, channel=${thread.channel}"
                    )
                }

                // Only process threads with channel or image data
                if (thread.channel.isNullOrBlank() && thread.imageUrl.isNullOrBlank()) {
                    if (BuildConfig.DEBUG) {
                        android.util.Log.d("ChatHubRepository", "  -> Skipping: no channel or image data")
                    }
                    continue
                }

                // Try to find contact by email or phone (renterId is often null)
                val existingContact = if (!thread.renterId.isNullOrBlank()) {
                    // Try by renter ID first
                    contactDao.getContactByIdOnce(thread.renterId)
                } else if (!thread.email.isNullOrBlank()) {
                    // Try by email
                    contactDao.getContactByEmail(thread.email)
                } else if (!thread.phone.isNullOrBlank()) {
                    // Try by phone
                    contactDao.getContactByPhone(thread.phone)
                } else {
                    null
                }

                if (existingContact == null) {
                    if (BuildConfig.DEBUG) {
                        android.util.Log.d("ChatHubRepository", "  -> Contact not found (tried ID, email, phone)")
                    }
                    continue
                }
                if (BuildConfig.DEBUG) {
                    android.util.Log.d(
                        "ChatHubRepository",
                        "  -> Found contact: ${existingContact.firstName} ${existingContact.lastName}, current channel: ${existingContact.channel}, avatarUrl: ${existingContact.avatarUrl}"
                    )
                }

                // Check if we need to update the contact
                val needsChannelUpdate = existingContact.channel.isNullOrBlank() && !thread.channel.isNullOrBlank()
                val needsImageUpdate = existingContact.avatarUrl.isNullOrBlank() && !thread.imageUrl.isNullOrBlank()

                if (needsChannelUpdate || needsImageUpdate) {
                    val updatedContact = existingContact.copy(
                        channel = if (needsChannelUpdate) thread.channel else existingContact.channel,
                        avatarUrl = if (needsImageUpdate) thread.imageUrl else existingContact.avatarUrl,
                        updatedAt = System.currentTimeMillis()
                    )
                    contactDao.insertContact(updatedContact)
                    updatedCount++

                    if (BuildConfig.DEBUG) {
                        val updates = mutableListOf<String>()
                        if (needsChannelUpdate) updates.add("channel: ${thread.channel}")
                        if (needsImageUpdate) updates.add("avatarUrl: ${thread.imageUrl}")
                        android.util.Log.d(
                            "ChatHubRepository",
                            "  -> ✅ Updated contact with ${updates.joinToString(", ")}"
                        )
                    }
                } else {
                    if (BuildConfig.DEBUG) {
                        android.util.Log.d("ChatHubRepository", "  -> Already has channel and image, skipping")
                    }
                }
            }

            if (BuildConfig.DEBUG) {
                android.util.Log.d("ChatHubRepository", "=== SYNC COMPLETE: Updated $updatedCount contacts ===")
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                android.util.Log.e("ChatHubRepository", "Failed to update contacts with channel data", e)
            }
            // Don't throw - this is not critical, just log the error
        }
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
        members = members,
        address = address,
        propertyId = propertyId,
        applicationStatus = applicationStatus,
        bookingStatus = bookingStatus,
        channel = channel,
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
        isPinned = isPinned,
        members = members,
        address = address,
        propertyId = propertyId,
        applicationStatus = applicationStatus,
        bookingStatus = bookingStatus,
        channel = channel
    )
}

private fun ChatMessage.toCachedMessage(): CachedMessage {
    // Serialize metadata to JSON string
    val metadataJson = metadata?.let {
        org.json.JSONObject().apply {
            it.bookingId?.let { value -> put("bookingId", value) }
            it.propertyAddress?.let { value -> put("propertyAddress", value) }
            it.viewingTime?.let { value -> put("viewingTime", value) }
            it.bookingStatus?.let { value -> put("bookingStatus", value) }
            it.items?.let { items -> put("items", org.json.JSONArray(items)) }
        }.toString()
    }

    return CachedMessage(
        id = id,
        threadId = threadId,
        sender = sender,
        text = text,
        type = type,
        attachmentUrl = attachmentUrl,
        metadata = metadataJson,
        status = status,
        readAt = null,
        createdAt = createdAt,
        updatedAt = System.currentTimeMillis()
    )
}

private fun CachedMessage.toDomainModel(): ChatMessage {
    // Deserialize metadata from JSON string
    val metadataObject = metadata?.let { jsonString ->
        try {
            val json = org.json.JSONObject(jsonString)
            MessageMetadata(
                bookingId = json.optString("bookingId").takeIf { it.isNotEmpty() },
                propertyAddress = json.optString("propertyAddress").takeIf { it.isNotEmpty() },
                viewingTime = json.optString("viewingTime").takeIf { it.isNotEmpty() },
                bookingStatus = json.optString("bookingStatus").takeIf { it.isNotEmpty() },
                items = json.optJSONArray("items")?.let { array ->
                    (0 until array.length()).map { array.getString(it) }
                }
            )
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                android.util.Log.w("ChatHubRepository", "Failed to parse metadata JSON: $jsonString", e)
            }
            null
        }
    }

    return ChatMessage(
        id = id,
        threadId = threadId,
        sender = sender,
        text = text,
        type = type,
        attachmentUrl = attachmentUrl,
        metadata = metadataObject,
        status = status,
        createdAt = createdAt
    )
}

/**
 * Parse message metadata from API response map.
 * Supports both new iOS field names (bookViewingTime, bookViewingDateTimeArr, etc.)
 * and legacy field names for backward compatibility.
 */
@Suppress("UNCHECKED_CAST")
private fun parseMessageMetadata(metadataMap: Map<String, Any>): MessageMetadata {
    return MessageMetadata(
        // Booking/Viewing fields (new iOS spec)
        bookViewing = metadataMap["bookViewing"] as? Boolean
            ?: metadataMap["book_viewing"] as? Boolean,
        bookViewingType = metadataMap["bookViewingType"] as? String
            ?: metadataMap["book_viewing_type"] as? String,
        bookViewingRequestStatus = metadataMap["bookViewingRequestStatus"] as? String
            ?: metadataMap["book_viewing_request_status"] as? String,
        bookViewingId = metadataMap["bookViewingId"] as? String
            ?: metadataMap["book_viewing_id"] as? String,
        bookViewingTime = metadataMap["bookViewingTime"] as? String
            ?: metadataMap["book_viewing_time"] as? String,
        bookViewingDateTimeArr = (metadataMap["bookViewingDateTimeArr"] as? List<*>)?.mapNotNull { it as? String }
            ?: (metadataMap["book_viewing_date_time_arr"] as? List<*>)?.mapNotNull { it as? String },
        bookViewingAlternative = (metadataMap["bookViewingAlternative"] as? List<*>)?.mapNotNull { it as? String }
            ?: (metadataMap["book_viewing_alternative"] as? List<*>)?.mapNotNull { it as? String },
        bookViewingAlternativeArr = (metadataMap["bookViewingAlternativeArr"] as? List<*>)
            ?.mapNotNull { item -> (item as? List<*>)?.mapNotNull { it as? String } }
            ?: (metadataMap["book_viewing_alternative_arr"] as? List<*>)
                ?.mapNotNull { item -> (item as? List<*>)?.mapNotNull { it as? String } },

        // Property fields
        propertyId = metadataMap["propertyId"] as? String
            ?: metadataMap["property_id"] as? String,
        propertyAddress = metadataMap["propertyAddress"] as? String
            ?: metadataMap["property_address"] as? String,

        // Application fields
        application = metadataMap["application"] as? Boolean,
        applicationType = metadataMap["applicationType"] as? String
            ?: metadataMap["application_type"] as? String,
        applicationId = metadataMap["applicationId"] as? String
            ?: metadataMap["application_id"] as? String,

        // Items requested fields
        itemsRequested = metadataMap["itemsRequested"] as? Boolean
            ?: metadataMap["items_requested"] as? Boolean,
        items = (metadataMap["items"] as? List<*>)?.mapNotNull { it as? String },

        // Attachment fields
        image = metadataMap["image"] as? String,
        originalUrl = metadataMap["originalUrl"] as? String
            ?: metadataMap["original_url"] as? String,
        height = (metadataMap["height"] as? Number)?.toInt(),
        width = (metadataMap["width"] as? Number)?.toInt(),
        fileUrl = metadataMap["fileUrl"] as? String
            ?: metadataMap["file_url"] as? String,
        fileType = metadataMap["fileType"] as? String
            ?: metadataMap["file_type"] as? String,
        fileName = metadataMap["fileName"] as? String
            ?: metadataMap["file_name"] as? String,

        // Legacy field names for backward compatibility
        bookingId = metadataMap["bookingId"] as? String
            ?: metadataMap["booking_id"] as? String,
        viewingTime = metadataMap["viewingTime"] as? String
            ?: metadataMap["viewing_time"] as? String,
        bookingStatus = metadataMap["bookingStatus"] as? String
            ?: metadataMap["booking_status"] as? String
    )
}

/**
 * Build full image URL from partial path.
 * If the URL is already complete (starts with http), return as-is.
 * Otherwise, prepend the UAT image base URL.
 */
private fun buildFullImageUrl(imagePath: String): String {
    return if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
        imagePath
    } else {
        // UAT image base URL
        "https://uatimgs.rhenti.com/images/${imagePath.trimStart('/')}"
    }
}
