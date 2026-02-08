package com.tomasronis.rhentiapp.presentation.calls

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tomasronis.rhentiapp.data.calls.models.CallLog
import com.tomasronis.rhentiapp.data.calls.models.CallType
import com.tomasronis.rhentiapp.presentation.main.chathub.ChatHubViewModel
import com.tomasronis.rhentiapp.presentation.main.contacts.ContactsViewModel
import com.tomasronis.rhentiapp.presentation.theme.RhentiCoral
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

/**
 * Call detail screen matching iOS design.
 * Shows comprehensive call information with action buttons at top.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallDetailScreen(
    callLog: CallLog,
    onNavigateBack: () -> Unit,
    onCallClick: (String) -> Unit,
    onMessageContact: (String) -> Unit,
    onViewContact: (String, String?) -> Unit, // (contactId, threadId)
    modifier: Modifier = Modifier,
    viewModel: CallsViewModel = hiltViewModel(),
    contactsViewModel: ContactsViewModel = hiltViewModel(),
    chatHubViewModel: ChatHubViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Get contacts and threads from ViewModels
    val contactsUiState by contactsViewModel.uiState.collectAsState()
    val chatHubUiState by chatHubViewModel.uiState.collectAsState()

    // Find matching contact and thread for this phone number
    val matchingContact = contactsUiState.contacts.firstOrNull { it.phone == callLog.contactPhone }
    val matchingThread = chatHubUiState.threads.firstOrNull { it.phone == callLog.contactPhone }

    Scaffold(
        topBar = {
            // iOS-style header with circular back button and centered title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular back button - iOS style
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = Color(0xFF2C2C2E),
                            shape = CircleShape
                        )
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onNavigateBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Center title
                Text(
                    text = "Call Details",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp
                    ),
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                // Spacer for balance
                Spacer(modifier = Modifier.size(40.dp))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Action buttons card
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column {
                        // Call Back button
                        Surface(
                            onClick = { onCallClick(callLog.contactPhone) },
                            color = Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Phone,
                                    contentDescription = null,
                                    tint = RhentiCoral,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "Call Back",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 52.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )

                        // Copy Number button
                        Surface(
                            onClick = {
                                copyToClipboard(context, callLog.contactPhone)
                                scope.launch {
                                    snackbarHostState.showSnackbar("Phone number copied")
                                }
                            },
                            color = Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ContentCopy,
                                    contentDescription = null,
                                    tint = RhentiCoral,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "Copy Number",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 52.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )

                        // Message Contact button
                        Surface(
                            onClick = {
                                // Navigate with thread ID if available
                                if (matchingThread != null) {
                                    onMessageContact(matchingThread.id)
                                } else {
                                    // Fallback to just switching tabs
                                    onMessageContact("")
                                }
                            },
                            color = Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Chat,
                                    contentDescription = null,
                                    tint = RhentiCoral,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "Message Contact",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 52.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )

                        // View Contact button
                        Surface(
                            onClick = {
                                // Navigate with contact and thread ID if available
                                if (matchingContact != null) {
                                    onViewContact(matchingContact.id, matchingThread?.id)
                                } else {
                                    // Fallback to just switching tabs
                                    onViewContact("", null)
                                }
                            },
                            color = Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Person,
                                    contentDescription = null,
                                    tint = RhentiCoral,
                                    modifier = Modifier.size(24.dp)
                                )
                                Text(
                                    text = "View Contact",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // Spacer before Call Details section
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Call Details Section
            item {
                SectionHeader(title = "Call Details")
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column {
                        DetailRow(
                            icon = Icons.Filled.CheckCircle,
                            label = "Status",
                            value = "Completed"
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 52.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )

                        DetailRow(
                            icon = Icons.Filled.Timer,
                            label = "Duration",
                            value = formatDurationSimple(callLog.duration)
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 52.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )

                        DetailRow(
                            icon = Icons.Filled.CalendarToday,
                            label = "Time",
                            value = formatFullDateTime(callLog.timestamp)
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 52.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )

                        DetailRow(
                            icon = when (callLog.callType) {
                                CallType.INCOMING -> Icons.Filled.CallReceived
                                CallType.OUTGOING -> Icons.Filled.CallMade
                                CallType.MISSED -> Icons.Filled.CallMissed
                            },
                            label = "Direction",
                            value = when (callLog.callType) {
                                CallType.INCOMING -> "Incoming"
                                CallType.OUTGOING -> "Outgoing"
                                CallType.MISSED -> "Missed"
                            }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 52.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )

                        DetailRow(
                            icon = Icons.Filled.Tag,
                            label = "Caller ID",
                            value = callLog.contactPhone
                        )
                    }
                }
            }

            // Spacer before Contact section
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Contact Section
            item {
                SectionHeader(title = "Contact")
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column {
                        DetailRow(
                            icon = Icons.Filled.Phone,
                            label = "Phone",
                            value = callLog.contactPhone
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 52.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )

                        DetailRow(
                            icon = Icons.Filled.Email,
                            label = "Email",
                            value = matchingContact?.email ?: matchingThread?.email ?: "Not available"
                        )
                    }
                }
            }

            // Spacer before Property section
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Property Section
            item {
                SectionHeader(title = "Property")
            }

            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    DetailRow(
                        icon = Icons.Filled.Business,
                        label = "Address",
                        value = matchingThread?.address ?: "No property associated"
                    )
                }
            }
        }
    }
}

/**
 * Section header text
 */
@Composable
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(horizontal = 4.dp)
    )
}

/**
 * Detail row with icon, label, and value
 */
@Composable
private fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = RhentiCoral,
            modifier = Modifier.size(24.dp)
        )

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 16.sp),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Format duration simply (e.g., "3:15")
 */
private fun formatDurationSimple(seconds: Int): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return String.format("%d:%02d", minutes, secs)
}

/**
 * Format full date and time (e.g., "Jan 30, 2026 at 1:13 PM")
 */
private fun formatFullDateTime(timestamp: Long): String {
    val format = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
    return format.format(Date(timestamp))
}

/**
 * Copy text to clipboard
 */
private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Phone Number", text)
    clipboard.setPrimaryClip(clip)
}
