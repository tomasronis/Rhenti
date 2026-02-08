package com.tomasronis.rhentiapp.presentation.main.chathub

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tomasronis.rhentiapp.data.chathub.models.ChatThread
import com.tomasronis.rhentiapp.presentation.main.chathub.components.*
import java.io.ByteArrayOutputStream

/**
 * Thread detail screen showing messages and input bar.
 * Supports text messaging with optimistic updates and pagination.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreadDetailScreen(
    thread: ChatThread,
    onNavigateBack: () -> Unit,
    onCall: (String) -> Unit,
    modifier: Modifier = Modifier,
    onNavigateToContact: (ChatThread) -> Unit = {},
    viewModel: ChatHubViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    var showAlternativeTimePicker by remember { mutableStateOf<String?>(null) }

    // Get the most recent property address from messages
    val propertyAddress = remember(uiState.messages) {
        uiState.messages
            .lastOrNull { it.metadata?.propertyAddress != null }
            ?.metadata?.propertyAddress
    }

    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val base64 = convertImageToBase64(context, it)
            if (base64 != null) {
                viewModel.sendImageMessage(base64)
            }
        }
    }

    // Select thread when screen opens
    LaunchedEffect(thread.id) {
        viewModel.selectThread(thread)
    }

    // Clear thread when screen closes
    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearSelectedThread()
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0), // Handle insets manually
        topBar = {
            // iOS-style header with circular buttons and centered text
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular back button
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(32.dp)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            shape = CircleShape
                        )
                        .background(
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Center content - User name, email, and property address
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = thread.displayName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 17.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onNavigateToContact(thread)
                        }
                    )
                }

                // Circular call button
                val hasPhone = !thread.phone.isNullOrBlank()
                IconButton(
                    onClick = {
                        thread.phone?.let { phoneNumber ->
                            onCall(phoneNumber)
                        }
                    },
                    enabled = hasPhone,
                    modifier = Modifier
                        .size(32.dp)
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                            shape = CircleShape
                        )
                        .background(
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            shape = CircleShape
                        )
                        .alpha(if (hasPhone) 1f else 0.4f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Phone,
                        contentDescription = if (hasPhone) "Call" else "No phone number",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        bottomBar = {
            // Detect keyboard visibility for conditional positioning
            val density = LocalDensity.current
            val imeBottomPadding = with(density) { WindowInsets.ime.getBottom(this).toDp() }
            val isKeyboardVisible = imeBottomPadding > 0.dp

            // Offset down when keyboard visible to compensate for over-positioning
            // Final adjustment: 120dp + 8dp for remaining 2mm = 128dp total
            val offsetY = if (isKeyboardVisible) 128.dp else 0.dp

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding() // Position above keyboard
                    .offset(y = offsetY) // Move down when keyboard visible to fix positioning
                    .padding(bottom = if (isKeyboardVisible) 0.dp else 6.dp)
            ) {
                MessageInputBar(
                    onSendMessage = { text ->
                        viewModel.sendTextMessage(text)
                    },
                    onAttachmentClick = {
                        imagePickerLauncher.launch("image/*")
                    }
                )
            }
        }
    ) { paddingValues ->
        // Detect keyboard visibility for conditional content padding
        val density = LocalDensity.current
        val imeBottomPadding = with(density) { WindowInsets.ime.getBottom(this).toDp() }
        val isKeyboardVisible = imeBottomPadding > 0.dp

        // Bottom padding: 3mm gap when keyboard visible, 12mm higher when hidden
        val contentBottomPadding = if (isKeyboardVisible) 12.dp else 57.dp

        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(
                    top = paddingValues.calculateTopPadding(),
                    start = paddingValues.calculateLeftPadding(layoutDirection = androidx.compose.ui.unit.LayoutDirection.Ltr),
                    end = paddingValues.calculateRightPadding(layoutDirection = androidx.compose.ui.unit.LayoutDirection.Ltr),
                    bottom = contentBottomPadding
                )
        ) {
            when {
                uiState.isLoading && uiState.displayMessages.isEmpty() -> {
                    // Initial loading
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.displayMessages.isEmpty() -> {
                    // No messages
                    EmptyMessagesView()
                }
                else -> {
                    // Show messages
                    MessageList(
                        messages = uiState.displayMessages,
                        onRetryMessage = { localId ->
                            viewModel.retryPendingMessage(localId)
                        },
                        onLoadMore = {
                            viewModel.loadMoreMessages()
                        },
                        onApproveBooking = { bookingId ->
                            viewModel.handleBookingAction(bookingId, "confirm")
                        },
                        onDeclineBooking = { bookingId ->
                            viewModel.handleBookingAction(bookingId, "decline")
                        },
                        onProposeAlternative = { bookingId ->
                            showAlternativeTimePicker = bookingId
                        },
                        listState = listState,
                        hasMoreMessages = uiState.hasMoreMessages
                    )
                }
            }

            // Loading indicator for pagination (only show when loading more, not initial load)
            if (uiState.isLoadingMore && uiState.displayMessages.isNotEmpty()) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                )
            }

            // Error snackbar
            if (uiState.error != null) {
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(uiState.error ?: "An error occurred")
                }
            }
        }

        // Alternative time picker bottom sheet
        showAlternativeTimePicker?.let { bookingId ->
            AlternativeTimePicker(
                onDismiss = { showAlternativeTimePicker = null },
                onConfirm = { times ->
                    viewModel.proposeAlternativeTimes(bookingId, times)
                    showAlternativeTimePicker = null
                }
            )
        }
    }
}

/**
 * Convert image URI to base64 string with data URI prefix.
 */
private fun convertImageToBase64(context: Context, uri: Uri): String? {
    return try {
        val inputStream = context.contentResolver.openInputStream(uri) ?: return null
        val bytes = ByteArrayOutputStream()
        val buffer = ByteArray(1024)
        var length: Int

        while (inputStream.read(buffer).also { length = it } != -1) {
            bytes.write(buffer, 0, length)
        }
        inputStream.close()

        val base64String = Base64.encodeToString(bytes.toByteArray(), Base64.DEFAULT)
        "data:image/jpeg;base64,$base64String"
    } catch (e: Exception) {
        null
    }
}

/**
 * List of messages with pagination support.
 * Updated to use DisplayMessage (matches iOS implementation).
 */
@Composable
private fun MessageList(
    messages: List<com.tomasronis.rhentiapp.data.chathub.models.DisplayMessage>,
    onRetryMessage: (String) -> Unit, // Pass local ID for retry
    onLoadMore: () -> Unit,
    onApproveBooking: (String) -> Unit,
    onDeclineBooking: (String) -> Unit,
    onProposeAlternative: (String) -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState,
    hasMoreMessages: Boolean
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        reverseLayout = false // Messages display oldest first, newest at bottom
    ) {
        items(
            items = messages,
            key = { it.id }
        ) { displayMessage ->
            when (displayMessage) {
                is com.tomasronis.rhentiapp.data.chathub.models.DisplayMessage.Server -> {
                    val message = displayMessage.message
                    // System messages get special treatment
                    if (message.sender == "system") {
                        SystemMessageView(message = message)
                    } else {
                        when (message.type) {
                            "image" -> {
                                ImageMessageView(message = message)
                            }
                            "booking" -> {
                                BookingMessageCard(
                                    message = message,
                                    onApprove = onApproveBooking,
                                    onDecline = onDeclineBooking,
                                    onProposeAlternative = onProposeAlternative
                                )
                            }
                            "items-requested" -> {
                                ItemsRequestedCard(message = message)
                            }
                            else -> {
                                MessageBubble(
                                    message = message,
                                    onRetry = null // Server messages don't need retry
                                )
                            }
                        }
                    }
                }
                is com.tomasronis.rhentiapp.data.chathub.models.DisplayMessage.Pending -> {
                    val pending = displayMessage.pendingMessage
                    // Convert pending message to ChatMessage for display
                    val chatMessage = com.tomasronis.rhentiapp.data.chathub.models.ChatMessage(
                        id = pending.localId,
                        threadId = "", // Not used in UI
                        sender = "owner",
                        text = pending.text,
                        type = if (pending.imageData != null) "image" else "text",
                        attachmentUrl = pending.imageData,
                        metadata = null,
                        status = when (displayMessage.status) {
                            com.tomasronis.rhentiapp.data.chathub.models.MessageStatus.SENDING -> "sending"
                            com.tomasronis.rhentiapp.data.chathub.models.MessageStatus.SENT -> "sent"
                            com.tomasronis.rhentiapp.data.chathub.models.MessageStatus.FAILED -> "failed"
                        },
                        createdAt = pending.createdAt
                    )

                    when (chatMessage.type) {
                        "image" -> {
                            ImageMessageView(message = chatMessage)
                        }
                        else -> {
                            MessageBubble(
                                message = chatMessage,
                                onRetry = if (displayMessage.status == com.tomasronis.rhentiapp.data.chathub.models.MessageStatus.FAILED) {
                                    { onRetryMessage(pending.localId) }
                                } else null
                            )
                        }
                    }
                }
            }
        }
    }

    // Track previous message count to detect NEW messages vs pagination
    val previousMessageCount = remember { mutableStateOf(0) }
    val isInitialLoad = remember { mutableStateOf(true) }

    // Detect scroll to top for loading older messages (only if hasMoreMessages)
    val firstVisibleItemIndex by remember {
        derivedStateOf { listState.firstVisibleItemIndex }
    }

    LaunchedEffect(firstVisibleItemIndex, hasMoreMessages) {
        // Load more when scrolled to top (first 3 items visible) and more messages available
        if (firstVisibleItemIndex <= 2 && messages.isNotEmpty() && hasMoreMessages) {
            onLoadMore()
        }
    }

    // Smart auto-scroll: scroll to bottom for new messages, not for pagination
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            val currentCount = messages.size
            val previousCount = previousMessageCount.value

            when {
                // Initial load - instant scroll to bottom (newest message)
                isInitialLoad.value -> {
                    listState.scrollToItem(messages.size - 1)
                    isInitialLoad.value = false
                    previousMessageCount.value = currentCount
                }
                // New message added (small increase) - smooth scroll to bottom
                currentCount > previousCount && currentCount - previousCount <= 3 -> {
                    listState.animateScrollToItem(messages.size - 1)
                    previousMessageCount.value = currentCount
                }
                // Pagination (large increase) - maintain current position, don't scroll
                else -> {
                    previousMessageCount.value = currentCount
                }
            }
        }
    }
}
