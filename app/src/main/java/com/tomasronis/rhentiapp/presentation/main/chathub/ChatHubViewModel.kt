package com.tomasronis.rhentiapp.presentation.main.chathub

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomasronis.rhentiapp.core.network.NetworkResult
import com.tomasronis.rhentiapp.core.security.TokenManager
import com.tomasronis.rhentiapp.data.chathub.models.ChatMessage
import com.tomasronis.rhentiapp.data.chathub.models.ChatThread
import com.tomasronis.rhentiapp.data.chathub.repository.ChatHubRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel for Chat Hub screens (thread list and thread detail).
 * Manages chat threads, messages, and sending functionality.
 */
@HiltViewModel
class ChatHubViewModel @Inject constructor(
    private val repository: ChatHubRepository,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatHubUiState())
    val uiState: StateFlow<ChatHubUiState> = _uiState.asStateFlow()

    // Job for observing messages - cancel old one when selecting new thread
    private var messagesObserverJob: kotlinx.coroutines.Job? = null

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    init {
        // Observe cached threads with search filter
        viewModelScope.launch {
            repository.observeThreads()
                .combine(_searchQuery) { threads, query ->
                    if (query.isBlank()) {
                        threads
                    } else {
                        threads.filter { thread ->
                            thread.displayName.contains(query, ignoreCase = true) ||
                            thread.lastMessage?.contains(query, ignoreCase = true) == true
                        }
                    }
                }
                .collect { filteredThreads ->
                    _uiState.update { it.copy(threads = filteredThreads) }
                }
        }

        // Observe total unread count
        viewModelScope.launch {
            repository.observeTotalUnreadCount().collect { count ->
                _uiState.update { it.copy(totalUnreadCount = count) }
            }
        }
    }

    /**
     * Refresh threads from the API (resets pagination).
     */
    fun refreshThreads() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, threadsPage = 0, hasMoreThreads = true) }

            val superAccountId = tokenManager.getSuperAccountId() ?: run {
                _uiState.update { it.copy(isLoading = false, error = "Not authenticated") }
                return@launch
            }

            when (val result = repository.getThreads(
                superAccountId = superAccountId,
                search = _searchQuery.value.takeIf { it.isNotBlank() },
                skip = 0,
                limit = 20
            )) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = null,
                            threadsPage = 0,
                            hasMoreThreads = result.data.size >= 20
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.exception.message ?: "Failed to load threads"
                        )
                    }
                }
                is NetworkResult.Loading -> {
                    _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    /**
     * Load more threads (pagination).
     */
    fun loadMoreThreads() {
        val currentState = _uiState.value

        // Guard against loading while already loading or no more threads
        if (currentState.isLoadingMoreThreads || currentState.isLoading || !currentState.hasMoreThreads) {
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMoreThreads = true) }

            val superAccountId = tokenManager.getSuperAccountId() ?: return@launch
            val nextPage = currentState.threadsPage + 1
            val skip = nextPage * 20

            when (val result = repository.getThreads(
                superAccountId = superAccountId,
                search = _searchQuery.value.takeIf { it.isNotBlank() },
                skip = skip,
                limit = 20
            )) {
                is NetworkResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoadingMoreThreads = false,
                            threadsPage = nextPage,
                            hasMoreThreads = result.data.size >= 20
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(isLoadingMoreThreads = false) }
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    /**
     * Search threads by query (filters locally).
     */
    fun searchThreads(query: String) {
        _searchQuery.value = query
        // Reset pagination when search changes
        if (query.isNotBlank()) {
            refreshThreads()
        }
    }

    /**
     * Select a thread to view details.
     * Matches iOS init(thread:) behavior - no Room observer, API only.
     */
    fun selectThread(thread: ChatThread) {
        // Cancel previous observer to avoid conflicts
        messagesObserverJob?.cancel()

        _uiState.update {
            it.copy(
                currentThread = thread,
                messages = emptyList(),
                pendingMessages = emptyList(),
                hasMoreMessages = true,
                isLoading = false,
                isLoadingMore = false
            )
        }

        // Fetch messages from API (Room is used only for caching, not reactive updates)
        // This matches iOS behavior where they don't use a database observer
        loadMessages(thread.id)

        // Clear unread badge
        clearThreadBadge(thread.id)
    }

    /**
     * Clear the selected thread (go back to list).
     */
    fun clearSelectedThread() {
        messagesObserverJob?.cancel()
        messagesObserverJob = null
        _uiState.update { it.copy(currentThread = null, messages = emptyList()) }
    }

    /**
     * Load messages for a thread (initial load).
     * Matches iOS loadMessages() implementation.
     */
    private fun loadMessages(threadId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = repository.getMessages(threadId, null)) {
                is NetworkResult.Success -> {
                    if (com.tomasronis.rhentiapp.BuildConfig.DEBUG) {
                        android.util.Log.d("ChatHubViewModel", "Initial load: ${result.data.size} messages")
                        if (result.data.isNotEmpty()) {
                            android.util.Log.d("ChatHubViewModel", "  First: ${result.data.first().id} @ ${result.data.first().createdAt}")
                            android.util.Log.d("ChatHubViewModel", "  Last: ${result.data.last().id} @ ${result.data.last().createdAt}")
                        }
                    }

                    // Update messages directly (messages are already sorted by repository)
                    _uiState.update {
                        it.copy(
                            messages = result.data,
                            isLoading = false,
                            error = null,
                            hasMoreMessages = result.data.size >= 20
                        )
                    }
                    cleanupPendingMessages()
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = result.exception.message ?: "Failed to load messages"
                        )
                    }
                }
                is NetworkResult.Loading -> {
                    _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    /**
     * Load more messages (pagination).
     * Matches iOS loadMoreMessages() implementation.
     */
    fun loadMoreMessages() {
        val currentState = _uiState.value

        // Guard against loading while already loading or no more messages
        if (currentState.isLoadingMore || currentState.isLoading || !currentState.hasMoreMessages) {
            return
        }

        val currentThread = currentState.currentThread ?: return
        val oldestMessage = currentState.messages.firstOrNull() ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMore = true) }

            when (val result = repository.getMessages(currentThread.id, oldestMessage.id)) {
                is NetworkResult.Success -> {
                    // Insert older messages at the beginning (they're already reversed from API)
                    val olderMessages = result.data
                    _uiState.update { state ->
                        state.copy(
                            messages = olderMessages + state.messages,
                            isLoadingMore = false,
                            hasMoreMessages = olderMessages.size >= 20
                        )
                    }
                }
                is NetworkResult.Error -> {
                    _uiState.update { it.copy(isLoadingMore = false) }
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    /**
     * Refresh messages incrementally by merging new ones into existing array (no flicker).
     * Matches iOS refreshMessagesIncrementally() implementation.
     */
    private fun refreshMessagesIncrementally() {
        val currentThread = _uiState.value.currentThread ?: return

        viewModelScope.launch {
            when (val result = repository.getMessages(currentThread.id, null)) {
                is NetworkResult.Success -> {
                    val newMessages = result.data
                    val existingIds = _uiState.value.messages.map { it.id }.toSet()

                    val messagesToAdd = newMessages.filter { !existingIds.contains(it.id) }

                    if (messagesToAdd.isNotEmpty()) {
                        _uiState.update { state ->
                            val updatedMessages = state.messages.toMutableList()

                            // Insert each new message at correct chronological position
                            for (message in messagesToAdd) {
                                val insertIndex = updatedMessages.indexOfFirst { it.createdAt > message.createdAt }
                                if (insertIndex != -1) {
                                    updatedMessages.add(insertIndex, message)
                                } else {
                                    updatedMessages.add(message)
                                }
                            }

                            state.copy(messages = updatedMessages)
                        }

                        cleanupPendingMessages()
                    }
                }
                is NetworkResult.Error -> {
                    // Silent fail for incremental refresh
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    /**
     * Cleanup pending messages that have been successfully delivered to server.
     * Matches iOS cleanupPendingMessages() implementation.
     */
    private fun cleanupPendingMessages() {
        val serverIds = _uiState.value.messages.map { it.id }.toSet()

        _uiState.update { state ->
            val cleanedPending = state.pendingMessages.filter { pending ->
                // Check exact ID match
                if (pending.serverMessageId != null && serverIds.contains(pending.serverMessageId)) {
                    return@filter false // Remove this pending message
                }

                // Fallback: Check fuzzy match (content + time + sender)
                // Only check recently sent messages (within last 10 seconds)
                if (pending.status == com.tomasronis.rhentiapp.data.chathub.models.MessageStatus.SENT) {
                    val match = state.messages.lastOrNull { message ->
                        val textMatch = pending.text != null && pending.text == message.text
                        val timeDiff = kotlin.math.abs(pending.createdAt - message.createdAt)
                        val timeMatch = timeDiff < 10_000 // 10 seconds

                        message.sender == "owner" && (textMatch || (pending.text == null && timeMatch))
                    }

                    if (match != null) {
                        return@filter false // Remove this pending message
                    }
                }

                true // Keep this pending message
            }

            state.copy(pendingMessages = cleanedPending)
        }
    }

    /**
     * Send a text message.
     * Matches iOS sendMessage() implementation with pending message tracking.
     */
    fun sendTextMessage(text: String) {
        val trimmedText = text.trim()
        if (trimmedText.isEmpty()) return

        val currentThread = _uiState.value.currentThread ?: return
        val legacyChatSessionId = currentThread.legacyChatSessionId ?: return

        viewModelScope.launch {
            val userId = tokenManager.getUserId() ?: return@launch
            val userName = tokenManager.getUserFullName()

            // Create pending message
            val localId = UUID.randomUUID().toString()
            val timestamp = System.currentTimeMillis()

            val pendingMessage = com.tomasronis.rhentiapp.data.chathub.models.PendingMessage(
                localId = localId,
                text = trimmedText,
                imageData = null,
                type = "text",
                metadata = null,
                createdAt = timestamp,
                status = com.tomasronis.rhentiapp.data.chathub.models.MessageStatus.SENDING,
                serverMessageId = null
            )

            // Add to pending messages immediately
            _uiState.update {
                it.copy(pendingMessages = it.pendingMessages + pendingMessage)
            }

            // Send to API
            when (val result = repository.sendTextMessage(userId, userName, legacyChatSessionId, trimmedText, currentThread)) {
                is NetworkResult.Success -> {
                    // Update pending message with server ID and mark as sent
                    _uiState.update { state ->
                        state.copy(
                            pendingMessages = state.pendingMessages.map { pending ->
                                if (pending.localId == localId) {
                                    pending.copy(
                                        status = com.tomasronis.rhentiapp.data.chathub.models.MessageStatus.SENT,
                                        serverMessageId = result.data.id
                                    )
                                } else pending
                            }
                        )
                    }

                    // Refresh messages incrementally to pick up server copy
                    refreshMessagesIncrementally()
                }
                is NetworkResult.Error -> {
                    // Mark message as failed
                    _uiState.update { state ->
                        state.copy(
                            pendingMessages = state.pendingMessages.map { pending ->
                                if (pending.localId == localId) {
                                    pending.copy(status = com.tomasronis.rhentiapp.data.chathub.models.MessageStatus.FAILED)
                                } else pending
                            }
                        )
                    }
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    /**
     * Send an image message.
     * Matches iOS sendImage() implementation with pending message tracking.
     */
    fun sendImageMessage(imageBase64: String) {
        val currentThread = _uiState.value.currentThread ?: return
        val legacyChatSessionId = currentThread.legacyChatSessionId ?: return

        viewModelScope.launch {
            val userId = tokenManager.getUserId() ?: return@launch
            val userName = tokenManager.getUserFullName()

            // Create pending message
            val localId = UUID.randomUUID().toString()
            val timestamp = System.currentTimeMillis()

            val pendingMessage = com.tomasronis.rhentiapp.data.chathub.models.PendingMessage(
                localId = localId,
                text = null,
                imageData = imageBase64,
                type = "image",
                metadata = null,
                createdAt = timestamp,
                status = com.tomasronis.rhentiapp.data.chathub.models.MessageStatus.SENDING,
                serverMessageId = null
            )

            // Add to pending messages immediately
            _uiState.update {
                it.copy(pendingMessages = it.pendingMessages + pendingMessage)
            }

            // Send to API
            when (val result = repository.sendImageMessage(userId, userName, legacyChatSessionId, imageBase64, currentThread)) {
                is NetworkResult.Success -> {
                    // Update pending message with server ID and mark as sent
                    _uiState.update { state ->
                        state.copy(
                            pendingMessages = state.pendingMessages.map { pending ->
                                if (pending.localId == localId) {
                                    pending.copy(
                                        status = com.tomasronis.rhentiapp.data.chathub.models.MessageStatus.SENT,
                                        serverMessageId = result.data.id
                                    )
                                } else pending
                            }
                        )
                    }

                    // Refresh messages incrementally to pick up server copy
                    refreshMessagesIncrementally()
                }
                is NetworkResult.Error -> {
                    // Mark message as failed
                    _uiState.update { state ->
                        state.copy(
                            pendingMessages = state.pendingMessages.map { pending ->
                                if (pending.localId == localId) {
                                    pending.copy(status = com.tomasronis.rhentiapp.data.chathub.models.MessageStatus.FAILED)
                                } else pending
                            }
                        )
                    }
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    /**
     * Send a link message (viewing-link or application-link).
     */
    fun sendLinkMessage(type: String, propertyAddress: String, propertyId: String? = null) {
        val currentThread = _uiState.value.currentThread ?: return
        val legacyChatSessionId = currentThread.legacyChatSessionId ?: return

        // Create message text based on type
        val messageText = when (type) {
            "viewing-link" -> "Book a Viewing: $propertyAddress"
            "application-link" -> "Apply to Listing: $propertyAddress"
            else -> return
        }

        // Log property selection for debugging
        android.util.Log.d("ChatHubViewModel", "Sending $type for property: $propertyAddress (ID: $propertyId)")

        viewModelScope.launch {
            val userId = tokenManager.getUserId() ?: return@launch
            val userName = tokenManager.getUserFullName()

            // Create pending message
            val localId = UUID.randomUUID().toString()
            val timestamp = System.currentTimeMillis()

            // Determine message type and metadata
            val (messageType, metadata) = when (type) {
                "viewing-link" -> "booking" to com.tomasronis.rhentiapp.data.chathub.models.MessageMetadata(
                    bookingId = null,
                    propertyAddress = propertyAddress,
                    viewingTime = null,
                    bookingStatus = null,
                    items = null
                )
                "application-link" -> "application" to com.tomasronis.rhentiapp.data.chathub.models.MessageMetadata(
                    bookingId = null,
                    propertyAddress = propertyAddress,
                    viewingTime = null,
                    bookingStatus = null,
                    items = null
                )
                else -> "text" to null
            }

            val pendingMessage = com.tomasronis.rhentiapp.data.chathub.models.PendingMessage(
                localId = localId,
                text = messageText,
                imageData = null,
                type = messageType,
                metadata = metadata,
                createdAt = timestamp,
                status = com.tomasronis.rhentiapp.data.chathub.models.MessageStatus.SENDING,
                serverMessageId = null
            )

            // Add to pending messages immediately
            _uiState.update {
                it.copy(pendingMessages = it.pendingMessages + pendingMessage)
            }

            // Send to API as link message with proper type and metadata
            when (val result = repository.sendLinkMessage(
                senderId = userId,
                userName = userName,
                chatSessionId = legacyChatSessionId,
                messageType = type,
                text = messageText,
                propertyAddress = propertyAddress,
                propertyId = propertyId,
                thread = currentThread
            )) {
                is NetworkResult.Success -> {
                    // Update pending message with server ID and mark as sent
                    _uiState.update { state ->
                        state.copy(
                            pendingMessages = state.pendingMessages.map { pending ->
                                if (pending.localId == localId) {
                                    pending.copy(
                                        status = com.tomasronis.rhentiapp.data.chathub.models.MessageStatus.SENT,
                                        serverMessageId = result.data.id
                                    )
                                } else pending
                            }
                        )
                    }

                    // Refresh messages incrementally to pick up server copy
                    refreshMessagesIncrementally()
                }
                is NetworkResult.Error -> {
                    // Mark message as failed
                    _uiState.update { state ->
                        state.copy(
                            pendingMessages = state.pendingMessages.map { pending ->
                                if (pending.localId == localId) {
                                    pending.copy(status = com.tomasronis.rhentiapp.data.chathub.models.MessageStatus.FAILED)
                                } else pending
                            }
                        )
                    }
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    /**
     * Retry sending a failed pending message.
     */
    fun retryPendingMessage(localId: String) {
        val pending = _uiState.value.pendingMessages.find { it.localId == localId } ?: return

        // Remove the failed message
        _uiState.update { state ->
            state.copy(pendingMessages = state.pendingMessages.filter { it.localId != localId })
        }

        // Re-send
        if (pending.text != null) {
            sendTextMessage(pending.text)
        } else if (pending.imageData != null) {
            sendImageMessage(pending.imageData)
        }
    }

    /**
     * Cancel a pending message.
     */
    fun cancelPendingMessage(localId: String) {
        _uiState.update { state ->
            state.copy(pendingMessages = state.pendingMessages.filter { it.localId != localId })
        }
    }

    /**
     * Legacy retry method for backwards compatibility.
     */
    @Deprecated("Use retryPendingMessage instead", ReplaceWith("retryPendingMessage(message.id)"))
    fun retryMessage(message: ChatMessage) {
        if (message.text != null) {
            sendTextMessage(message.text)
        }
    }

    /**
     * Clear unread badge for a thread.
     */
    private fun clearThreadBadge(threadId: String) {
        viewModelScope.launch {
            repository.clearBadge(threadId)
        }
    }

    /**
     * Pin or unpin a thread.
     */
    fun toggleThreadPinned(thread: ChatThread) {
        viewModelScope.launch {
            repository.updateThreadPinned(thread.id, !thread.isPinned)
        }
    }

    /**
     * Delete a thread.
     */
    fun deleteThread(thread: ChatThread) {
        viewModelScope.launch {
            repository.deleteThread(thread.id)
        }
    }

    /**
     * Handle booking action (approve/decline).
     */
    fun handleBookingAction(bookingId: String, action: String) {
        viewModelScope.launch {
            val superAccountId = tokenManager.getSuperAccountId() ?: return@launch

            when (repository.handleBookingAction(bookingId, action, superAccountId)) {
                is NetworkResult.Success -> {
                    // Use incremental refresh to avoid UI flicker
                    refreshMessagesIncrementally()
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(error = "Failed to $action booking")
                    }
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    /**
     * Propose alternative viewing times.
     */
    fun proposeAlternativeTimes(bookingId: String, times: List<String>) {
        viewModelScope.launch {
            val superAccountId = tokenManager.getSuperAccountId() ?: return@launch

            when (repository.proposeAlternativeTimes(bookingId, times, superAccountId)) {
                is NetworkResult.Success -> {
                    // Use incremental refresh to avoid UI flicker
                    refreshMessagesIncrementally()
                }
                is NetworkResult.Error -> {
                    _uiState.update {
                        it.copy(error = "Failed to propose alternative times")
                    }
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    /**
     * Handle an incoming message from real-time updates (future: WebSocket/FCM).
     * Matches iOS handleIncomingMessage() implementation.
     * Returns true if the message was for the current thread and was added.
     */
    fun handleIncomingMessage(message: ChatMessage): Boolean {
        val currentThread = _uiState.value.currentThread ?: return false

        // Only handle messages for the current thread
        if (message.threadId != currentThread.id) {
            return false
        }

        // Check if we already have this message
        if (_uiState.value.messages.any { it.id == message.id }) {
            return false
        }

        // Add the message to the list
        _uiState.update { state ->
            val updatedMessages = state.messages.toMutableList()

            // Insert at correct chronological position
            val insertIndex = updatedMessages.indexOfFirst { it.createdAt > message.createdAt }
            if (insertIndex != -1) {
                updatedMessages.add(insertIndex, message)
            } else {
                updatedMessages.add(message)
            }

            state.copy(messages = updatedMessages)
        }

        // Clean up any pending messages that match this new message
        cleanupPendingMessages()

        return true
    }

    /**
     * Clear error message.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /**
     * Update filter: Unread Only
     */
    fun setUnreadOnly(enabled: Boolean) {
        _uiState.update { it.copy(unreadOnly = enabled) }
    }

    /**
     * Update filter: No Activity
     */
    fun setNoActivity(enabled: Boolean) {
        _uiState.update { it.copy(noActivity = enabled) }
    }

    /**
     * Update filter: Application Status
     */
    fun setApplicationStatus(status: String) {
        _uiState.update { it.copy(applicationStatus = status) }
    }

    /**
     * Update filter: Viewing Status
     */
    fun setViewingStatus(status: String) {
        _uiState.update { it.copy(viewingStatus = status) }
    }

    /**
     * Reset all filters to default state
     */
    fun resetFilters() {
        _uiState.update {
            it.copy(
                unreadOnly = false,
                noActivity = false,
                applicationStatus = "All",
                viewingStatus = "All"
            )
        }
    }
}

/**
 * UI state for Chat Hub screens.
 */
data class ChatHubUiState(
    val threads: List<ChatThread> = emptyList(),
    val currentThread: ChatThread? = null,
    val messages: List<ChatMessage> = emptyList(),
    val pendingMessages: List<com.tomasronis.rhentiapp.data.chathub.models.PendingMessage> = emptyList(),
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMoreMessages: Boolean = true,
    val error: String? = null,
    val totalUnreadCount: Int = 0,
    // Thread list pagination
    val threadsPage: Int = 0,
    val isLoadingMoreThreads: Boolean = false,
    val hasMoreThreads: Boolean = true,
    // Filter state
    val unreadOnly: Boolean = false,
    val noActivity: Boolean = false,
    val applicationStatus: String = "All",
    val viewingStatus: String = "All"
) {
    /**
     * Check if any filters are currently active.
     */
    val hasActiveFilters: Boolean
        get() = unreadOnly || noActivity ||
                applicationStatus != "All" ||
                viewingStatus != "All"

    /**
     * Combined messages for display (server messages + pending).
     * Matches iOS implementation's displayMessages logic.
     */
    val displayMessages: List<com.tomasronis.rhentiapp.data.chathub.models.DisplayMessage>
        get() {
            val result = mutableListOf<com.tomasronis.rhentiapp.data.chathub.models.DisplayMessage>()

            // Add all server messages
            result.addAll(messages.map { com.tomasronis.rhentiapp.data.chathub.models.DisplayMessage.Server(it) })

            // Build set of server message IDs for matching
            val serverIds = messages.map { it.id }.toSet()

            // Add pending messages that don't have a matching server message yet
            for (pending in pendingMessages) {
                val hasMatchingServerMessage = pending.serverMessageId?.let { serverIds.contains(it) } ?: false

                if (!hasMatchingServerMessage) {
                    result.add(com.tomasronis.rhentiapp.data.chathub.models.DisplayMessage.Pending(pending))
                }
            }

            // Sort by date (oldest first)
            val sorted = result.sortedBy { it.createdAt }

            if (com.tomasronis.rhentiapp.BuildConfig.DEBUG && sorted.size >= 3) {
                android.util.Log.d("ChatHubViewModel", "displayMessages: ${sorted.size} total")
                android.util.Log.d("ChatHubViewModel", "  First 3 timestamps: ${sorted.take(3).map { it.createdAt }}")
                android.util.Log.d("ChatHubViewModel", "  Last 3 timestamps: ${sorted.takeLast(3).map { it.createdAt }}")

                // Verify sorted order
                val isSorted = sorted.zipWithNext().all { (a, b) -> a.createdAt <= b.createdAt }
                if (isSorted) {
                    android.util.Log.d("ChatHubViewModel", "✓ Display messages are properly sorted")
                } else {
                    android.util.Log.e("ChatHubViewModel", "❌ Display messages are NOT sorted correctly!")
                }
            }

            return sorted
        }
}
