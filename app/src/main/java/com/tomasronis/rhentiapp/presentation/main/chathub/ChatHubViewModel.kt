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
     * Refresh threads from the API.
     */
    fun refreshThreads() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val superAccountId = tokenManager.getSuperAccountId() ?: run {
                _uiState.update { it.copy(isLoading = false, error = "Not authenticated") }
                return@launch
            }

            when (val result = repository.getThreads(superAccountId, _searchQuery.value.takeIf { it.isNotBlank() })) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, error = null) }
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
     * Search threads by query (filters locally).
     */
    fun searchThreads(query: String) {
        _searchQuery.value = query
    }

    /**
     * Select a thread to view details.
     */
    fun selectThread(thread: ChatThread) {
        _uiState.update { it.copy(currentThread = thread, messages = emptyList()) }

        // Load messages for the selected thread
        viewModelScope.launch {
            repository.observeMessages(thread.id).collect { messages ->
                _uiState.update { it.copy(messages = messages) }
            }
        }

        // Fetch fresh messages from API
        loadMessages(thread.id)

        // Clear unread badge
        clearThreadBadge(thread.id)
    }

    /**
     * Clear the selected thread (go back to list).
     */
    fun clearSelectedThread() {
        _uiState.update { it.copy(currentThread = null, messages = emptyList()) }
    }

    /**
     * Load messages for a thread.
     */
    private fun loadMessages(threadId: String, beforeId: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = repository.getMessages(threadId, beforeId)) {
                is NetworkResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, error = null) }
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
     */
    fun loadMoreMessages() {
        val currentThread = _uiState.value.currentThread ?: return
        val oldestMessage = _uiState.value.messages.firstOrNull() ?: return

        loadMessages(currentThread.id, oldestMessage.id)
    }

    /**
     * Send a text message.
     */
    fun sendTextMessage(text: String) {
        val currentThread = _uiState.value.currentThread ?: return
        val legacyChatSessionId = currentThread.legacyChatSessionId ?: return

        viewModelScope.launch {
            val userId = tokenManager.getUserId() ?: return@launch
            val userName = tokenManager.getUserFullName()

            // Create optimistic message
            val tempMessage = ChatMessage(
                id = UUID.randomUUID().toString(),
                threadId = currentThread.id,
                sender = "owner",
                text = text,
                type = "text",
                attachmentUrl = null,
                metadata = null,
                status = "sending",
                createdAt = System.currentTimeMillis()
            )

            // Add to UI immediately
            _uiState.update {
                it.copy(messages = it.messages + tempMessage)
            }

            // Send to API
            when (val result = repository.sendTextMessage(userId, userName, legacyChatSessionId, text, currentThread)) {
                is NetworkResult.Success -> {
                    // Replace temp message with real one
                    _uiState.update { state ->
                        state.copy(
                            messages = state.messages.map { msg ->
                                if (msg.id == tempMessage.id) result.data else msg
                            }
                        )
                    }
                }
                is NetworkResult.Error -> {
                    // Mark message as failed
                    _uiState.update { state ->
                        state.copy(
                            messages = state.messages.map { msg ->
                                if (msg.id == tempMessage.id) msg.copy(status = "failed") else msg
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
     */
    fun sendImageMessage(imageBase64: String) {
        val currentThread = _uiState.value.currentThread ?: return
        val legacyChatSessionId = currentThread.legacyChatSessionId ?: return

        viewModelScope.launch {
            val userId = tokenManager.getUserId() ?: return@launch
            val userName = tokenManager.getUserFullName()

            // Create optimistic message
            val tempMessage = ChatMessage(
                id = UUID.randomUUID().toString(),
                threadId = currentThread.id,
                sender = "owner",
                text = null,
                type = "image",
                attachmentUrl = imageBase64, // Will be replaced with URL from server
                metadata = null,
                status = "sending",
                createdAt = System.currentTimeMillis()
            )

            // Add to UI immediately
            _uiState.update {
                it.copy(messages = it.messages + tempMessage)
            }

            // Send to API
            when (val result = repository.sendImageMessage(userId, userName, legacyChatSessionId, imageBase64, currentThread)) {
                is NetworkResult.Success -> {
                    // Replace temp message with real one
                    _uiState.update { state ->
                        state.copy(
                            messages = state.messages.map { msg ->
                                if (msg.id == tempMessage.id) result.data else msg
                            }
                        )
                    }
                }
                is NetworkResult.Error -> {
                    // Mark message as failed
                    _uiState.update { state ->
                        state.copy(
                            messages = state.messages.map { msg ->
                                if (msg.id == tempMessage.id) msg.copy(status = "failed") else msg
                            }
                        )
                    }
                }
                is NetworkResult.Loading -> {}
            }
        }
    }

    /**
     * Retry sending a failed message.
     */
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
                    // Refresh messages to see updated booking status
                    _uiState.value.currentThread?.let { thread ->
                        loadMessages(thread.id)
                    }
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
                    // Refresh messages
                    _uiState.value.currentThread?.let { thread ->
                        loadMessages(thread.id)
                    }
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
     * Clear error message.
     */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}

/**
 * UI state for Chat Hub screens.
 */
data class ChatHubUiState(
    val threads: List<ChatThread> = emptyList(),
    val currentThread: ChatThread? = null,
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val totalUnreadCount: Int = 0
)
