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
import com.tomasronis.rhentiapp.presentation.properties.PropertiesViewModel
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
    viewModel: ChatHubViewModel = hiltViewModel(),
    propertiesViewModel: PropertiesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    val context = LocalContext.current
    var showAlternativeTimePicker by remember { mutableStateOf<String?>(null) }
    var showManageViewingMessage by remember { mutableStateOf<com.tomasronis.rhentiapp.data.chathub.models.ChatMessage?>(null) }
    var showAttachOptions by remember { mutableStateOf(false) }
    var showPropertySelection by remember { mutableStateOf(false) }
    var showPreApprovedViewing by remember { mutableStateOf(false) }
    var pendingLinkType by remember { mutableStateOf<String?>(null) } // "viewing-link" or "application-link"

    // Get all properties from ViewModel
    val allProperties by propertiesViewModel.properties.collectAsState()

    // Refresh properties when screen loads
    LaunchedEffect(Unit) {
        android.util.Log.d("ThreadDetailScreen", "Screen loaded, refreshing properties...")
        propertiesViewModel.refreshProperties()
    }

    // Debug logging
    LaunchedEffect(allProperties) {
        android.util.Log.d("ThreadDetailScreen", "Properties updated: ${allProperties.size} properties available")
        allProperties.forEachIndexed { index, property ->
            android.util.Log.d("ThreadDetailScreen", "  [$index] ${property.address}")
        }
    }

    // Get the most recent property address from messages
    val propertyAddress = remember(uiState.messages) {
        uiState.messages
            .lastOrNull { it.metadata?.propertyAddress != null }
            ?.metadata?.propertyAddress
    } ?: thread.address ?: "Property"

    // Convert properties to PropertyOption format
    val propertyOptions = remember(allProperties, thread.propertyId) {
        val options = allProperties.map { property ->
            PropertyOption(
                id = property.id,
                address = property.address,
                unit = property.unit,
                city = property.city
            )
        }

        // If thread has a property that's not in the list, add it at the top
        if (thread.propertyId != null && thread.address != null) {
            val threadPropertyExists = options.any { it.id == thread.propertyId }
            if (!threadPropertyExists) {
                val threadProperty = PropertyOption(
                    id = thread.propertyId,
                    address = thread.address,
                    unit = null,
                    city = null
                )
                listOf(threadProperty) + options
            } else {
                // Move thread property to the top
                val threadProperty = options.find { it.id == thread.propertyId }
                if (threadProperty != null) {
                    listOf(threadProperty) + options.filter { it.id != thread.propertyId }
                } else {
                    options
                }
            }
        } else {
            options
        }
    }

    // Image picker launcher (for Photos option)
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

    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        bitmap?.let {
            // Convert bitmap to base64
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 80, outputStream)
            val imageBytes = outputStream.toByteArray()
            val base64 = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
            viewModel.sendImageMessage(base64)
        }
    }

    // PDF file picker launcher
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            // TODO: Handle PDF file upload
            // For now, just log it
            android.util.Log.d("ThreadDetail", "PDF selected: $uri")
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
                        showAttachOptions = true
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
                        onCheckInBooking = { bookingId ->
                            viewModel.checkInViewing(bookingId)
                        },
                        onManageViewing = { bookingId ->
                            // Find the message with this booking ID to pass to the sheet
                            val targetMessage = uiState.displayMessages
                                .filterIsInstance<com.tomasronis.rhentiapp.data.chathub.models.DisplayMessage.Server>()
                                .map { it.message }
                                .firstOrNull { msg ->
                                    val meta = msg.metadata
                                    (meta?.bookViewingId ?: meta?.bookingId ?: msg.id) == bookingId
                                }
                            if (targetMessage != null) {
                                showManageViewingMessage = targetMessage
                            }
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

        // Attach options bottom sheet
        if (showAttachOptions) {
            AttachOptionsBottomSheet(
                onDismiss = { showAttachOptions = false },
                onOptionSelected = { type ->
                    when (type) {
                        AttachmentType.CAMERA -> {
                            cameraLauncher.launch(null)
                        }
                        AttachmentType.PHOTOS -> {
                            imagePickerLauncher.launch("image/*")
                        }
                        AttachmentType.PDF_FILE -> {
                            pdfPickerLauncher.launch("application/pdf")
                        }
                        AttachmentType.BOOK_VIEWING_LINK -> {
                            // Show property selection for viewing link
                            pendingLinkType = "viewing-link"
                            if (propertyOptions.isEmpty()) {
                                // No properties available, send with generic address
                                viewModel.sendLinkMessage(
                                    type = "viewing-link",
                                    propertyAddress = propertyAddress,
                                    propertyId = null
                                )
                            } else {
                                showPropertySelection = true
                            }
                        }
                        AttachmentType.PRE_APPROVED_VIEWING -> {
                            // Show pre-approved viewing sheet
                            showPreApprovedViewing = true
                        }
                        AttachmentType.APPLICATION_LINK -> {
                            // Show property selection for application link
                            pendingLinkType = "application-link"
                            if (propertyOptions.isEmpty()) {
                                // No properties available, send with generic address
                                viewModel.sendLinkMessage(
                                    type = "application-link",
                                    propertyAddress = propertyAddress,
                                    propertyId = null
                                )
                            } else {
                                showPropertySelection = true
                            }
                        }
                    }
                }
            )
        }

        // Property selection bottom sheet
        if (showPropertySelection && pendingLinkType != null) {
            PropertySelectionBottomSheet(
                properties = propertyOptions,
                onDismiss = {
                    showPropertySelection = false
                    pendingLinkType = null
                },
                onPropertySelected = { property ->
                    viewModel.sendLinkMessage(
                        type = pendingLinkType!!,
                        propertyAddress = property.displayAddress,
                        propertyId = property.id
                    )
                    showPropertySelection = false
                    pendingLinkType = null
                }
            )
        }

        // Pre-approved viewing bottom sheet
        if (showPreApprovedViewing) {
            PreApprovedViewingSheet(
                properties = propertyOptions,
                onDismiss = { showPreApprovedViewing = false },
                onSend = { propertyId, address, viewingTimeIso ->
                    viewModel.sendPreApprovedViewing(
                        propertyAddress = address,
                        propertyId = propertyId,
                        viewingTimeIso = viewingTimeIso
                    )
                    showPreApprovedViewing = false
                }
            )
        }

        // Manage viewing bottom sheet
        showManageViewingMessage?.let { message ->
            ManageViewingSheet(
                message = message,
                onDismiss = { showManageViewingMessage = null },
                onCheckInRenter = { bookingId ->
                    viewModel.checkInViewing(bookingId)
                    showManageViewingMessage = null
                },
                onEditViewing = { bookingId ->
                    // Open alternative time picker for editing the viewing
                    showManageViewingMessage = null
                    showAlternativeTimePicker = bookingId
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
    onCheckInBooking: (String) -> Unit,
    onManageViewing: (String) -> Unit,
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
                    // Debug logging to see message types
                    android.util.Log.d("ThreadDetail", "Message type: ${message.type}, sender: ${message.sender}, metadata: ${message.metadata}")

                    // System messages get special treatment
                    if (message.sender == "system") {
                        SystemMessageView(message = message)
                    } else {
                        when (message.type) {
                            "image" -> {
                                ImageMessageView(message = message)
                            }
                            "booking" -> {
                                val bookViewingType = message.metadata?.bookViewingType
                                val isViewingStatus = bookViewingType in listOf("confirm", "decline", "change_request", "alternative")

                                if (isViewingStatus) {
                                    // Viewing status message: confirmed, declined, or alternatives proposed
                                    com.tomasronis.rhentiapp.presentation.main.chathub.components.ViewingStatusMessageCard(
                                        message = message,
                                        onManageViewing = onManageViewing
                                    )
                                } else {
                                    val isPreApproved = message.metadata?.bookViewingRequestStatus == "confirmed" &&
                                        message.sender == "owner"
                                    if (message.sender == "owner" && !isPreApproved) {
                                        // Owner sending viewing link to contact
                                        LinkMessageCard(
                                            type = LinkMessageType.VIEWING,
                                            propertyAddress = message.metadata?.propertyAddress ?: message.text ?: "Property",
                                            isFromOwner = true,
                                            url = null,
                                            onClick = {
                                                android.util.Log.d("ThreadDetail", "Viewing link clicked")
                                            }
                                        )
                                    } else {
                                        // Contact viewing request OR owner pre-approved viewing
                                        BookingMessageCard(
                                            message = message,
                                            onApprove = onApproveBooking,
                                            onDecline = onDeclineBooking,
                                            onProposeAlternative = onProposeAlternative,
                                            onCheckIn = onCheckInBooking
                                        )
                                    }
                                }
                            }
                            "application" -> {
                                // Application link (applicationType: "link")
                                LinkMessageCard(
                                    type = LinkMessageType.APPLICATION,
                                    propertyAddress = message.metadata?.propertyAddress ?: message.text ?: "Property",
                                    isFromOwner = message.sender == "owner",
                                    url = null, // TODO: Extract URL from message
                                    onClick = {
                                        // TODO: Open link in browser or handle application
                                        android.util.Log.d("ThreadDetail", "Application link clicked")
                                    }
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
                        type = pending.type,
                        attachmentUrl = pending.imageData,
                        metadata = pending.metadata,
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
                        "booking" -> {
                            val bookViewingType = chatMessage.metadata?.bookViewingType
                            val isViewingStatus = bookViewingType in listOf("confirm", "decline", "change_request", "alternative")

                            if (isViewingStatus) {
                                // Viewing status message (pending): confirmed, declined, or alternatives proposed
                                com.tomasronis.rhentiapp.presentation.main.chathub.components.ViewingStatusMessageCard(
                                    message = chatMessage,
                                    onManageViewing = onManageViewing
                                )
                            } else {
                                val isPreApproved = chatMessage.metadata?.bookViewingRequestStatus == "confirmed" &&
                                    chatMessage.sender == "owner"
                                if (chatMessage.sender == "owner" && !isPreApproved) {
                                    // Owner sending viewing link to contact (pending)
                                    LinkMessageCard(
                                        type = LinkMessageType.VIEWING,
                                        propertyAddress = chatMessage.metadata?.propertyAddress ?: chatMessage.text ?: "Property",
                                        isFromOwner = true,
                                        url = null,
                                        onClick = {
                                            android.util.Log.d("ThreadDetail", "Viewing link clicked (pending)")
                                        }
                                    )
                                } else {
                                    // Contact viewing request or owner pre-approved viewing (pending)
                                    BookingMessageCard(
                                        message = chatMessage,
                                        onApprove = onApproveBooking,
                                        onDecline = onDeclineBooking,
                                        onProposeAlternative = onProposeAlternative,
                                        onCheckIn = onCheckInBooking
                                    )
                                }
                            }
                        }
                        "application" -> {
                            // Application link (pending)
                            LinkMessageCard(
                                type = LinkMessageType.APPLICATION,
                                propertyAddress = chatMessage.metadata?.propertyAddress ?: chatMessage.text ?: "Property",
                                isFromOwner = chatMessage.sender == "owner",
                                url = null,
                                onClick = {
                                    android.util.Log.d("ThreadDetail", "Application link clicked (pending)")
                                }
                            )
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
