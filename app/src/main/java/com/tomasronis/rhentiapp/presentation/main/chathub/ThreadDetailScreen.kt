package com.tomasronis.rhentiapp.presentation.main.chathub

import android.content.Context
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
    modifier: Modifier = Modifier,
    viewModel: ChatHubViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    var showAlternativeTimePicker by remember { mutableStateOf<String?>(null) }

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
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = thread.displayName,
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (thread.email != null || thread.phone != null) {
                            Text(
                                text = thread.email ?: thread.phone ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            MessageInputBar(
                onSendMessage = { text ->
                    viewModel.sendTextMessage(text)
                },
                onAttachmentClick = {
                    imagePickerLauncher.launch("image/*")
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading && uiState.messages.isEmpty() -> {
                    // Initial loading
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.messages.isEmpty() -> {
                    // No messages
                    EmptyMessagesView()
                }
                else -> {
                    // Show messages
                    MessageList(
                        messages = uiState.messages,
                        onRetryMessage = { message ->
                            viewModel.retryMessage(message)
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
                        listState = listState
                    )
                }
            }

            // Loading indicator for pagination
            if (uiState.isLoading && uiState.messages.isNotEmpty()) {
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
 */
@Composable
private fun MessageList(
    messages: List<com.tomasronis.rhentiapp.data.chathub.models.ChatMessage>,
    onRetryMessage: (com.tomasronis.rhentiapp.data.chathub.models.ChatMessage) -> Unit,
    onLoadMore: () -> Unit,
    onApproveBooking: (String) -> Unit,
    onDeclineBooking: (String) -> Unit,
    onProposeAlternative: (String) -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState
) {
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp),
        reverseLayout = false // Messages oldest to newest (top to bottom)
    ) {
        items(
            items = messages,
            key = { it.id }
        ) { message ->
            // System messages get special treatment (e.g., "Conversation about: [address]")
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
                    else -> {
                        MessageBubble(
                            message = message,
                            onRetry = if (message.status == "failed") {
                                { onRetryMessage(message) }
                            } else null
                        )
                    }
                }
            }
        }
    }

    // Detect scroll to top for pagination
    val firstVisibleItemIndex by remember {
        derivedStateOf { listState.firstVisibleItemIndex }
    }

    LaunchedEffect(firstVisibleItemIndex) {
        if (firstVisibleItemIndex == 0 && messages.isNotEmpty()) {
            onLoadMore()
        }
    }

    // Auto-scroll to bottom when new message is added
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }
}
