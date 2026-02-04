package com.tomasronis.rhentiapp.presentation.calls

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import com.tomasronis.rhentiapp.data.calls.models.CallLog
import com.tomasronis.rhentiapp.data.calls.models.CallStatus
import com.tomasronis.rhentiapp.data.calls.models.CallType
import java.text.SimpleDateFormat
import java.util.*

/**
 * Call detail screen showing full information about a call.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallDetailScreen(
    callLog: CallLog,
    onNavigateBack: () -> Unit,
    onCallClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CallsViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Call Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Avatar
            if (callLog.contactAvatar != null) {
                AsyncImage(
                    model = callLog.contactAvatar,
                    contentDescription = null,
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    if (callLog.contactName != null) {
                        Text(
                            text = getInitials(callLog.contactName),
                            style = MaterialTheme.typography.headlineLarge,
                            fontSize = 48.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Phone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(60.dp)
                        )
                    }
                }
            }

            // Contact name
            Text(
                text = callLog.contactName ?: "Unknown",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            // Phone number
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = callLog.contactPhone,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(
                    onClick = {
                        copyToClipboard(context, callLog.contactPhone)
                        // Show snackbar
                        scope.launch {
                            snackbarHostState.showSnackbar("Copied to clipboard")
                        }
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = "Copy number",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Call info card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Call type
                    CallInfoRow(
                        icon = when (callLog.callType) {
                            CallType.INCOMING -> Icons.Filled.CallReceived
                            CallType.OUTGOING -> Icons.Filled.CallMade
                            CallType.MISSED -> Icons.Filled.CallMissed
                        },
                        label = "Type",
                        value = when (callLog.callType) {
                            CallType.INCOMING -> "Incoming Call"
                            CallType.OUTGOING -> "Outgoing Call"
                            CallType.MISSED -> "Missed Call"
                        },
                        valueColor = if (callLog.callType == CallType.MISSED) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )

                    HorizontalDivider()

                    // Status
                    CallInfoRow(
                        icon = when (callLog.status) {
                            CallStatus.COMPLETED -> Icons.Filled.CheckCircle
                            CallStatus.FAILED -> Icons.Filled.Error
                            CallStatus.BUSY -> Icons.Filled.Block
                            CallStatus.NO_ANSWER -> Icons.Filled.PhoneMissed
                        },
                        label = "Status",
                        value = when (callLog.status) {
                            CallStatus.COMPLETED -> "Completed"
                            CallStatus.FAILED -> "Failed"
                            CallStatus.BUSY -> "Busy"
                            CallStatus.NO_ANSWER -> "No Answer"
                        },
                        valueColor = when (callLog.status) {
                            CallStatus.FAILED -> MaterialTheme.colorScheme.error
                            CallStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )

                    HorizontalDivider()

                    // Duration
                    if (callLog.duration > 0) {
                        CallInfoRow(
                            icon = Icons.Filled.Timer,
                            label = "Duration",
                            value = formatDuration(callLog.duration)
                        )

                        HorizontalDivider()
                    }

                    // Date
                    CallInfoRow(
                        icon = Icons.Filled.CalendarToday,
                        label = "Date",
                        value = formatDate(callLog.timestamp)
                    )

                    HorizontalDivider()

                    // Time
                    CallInfoRow(
                        icon = Icons.Filled.AccessTime,
                        label = "Time",
                        value = formatTime(callLog.timestamp)
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Action buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Call button
                Button(
                    onClick = { onCallClick(callLog.contactPhone) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Phone,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Call ${callLog.contactName ?: "Back"}",
                        style = MaterialTheme.typography.titleMedium
                    )
                }

                // Copy number button
                OutlinedButton(
                    onClick = {
                        copyToClipboard(context, callLog.contactPhone)
                        scope.launch {
                            snackbarHostState.showSnackbar("Phone number copied")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Copy Number",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    }
}

/**
 * Call info row showing icon, label, and value
 */
@Composable
private fun CallInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    valueColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = valueColor
        )
    }
}

/**
 * Get initials from name
 */
private fun getInitials(name: String): String {
    val parts = name.trim().split(" ")
    return when {
        parts.size >= 2 -> "${parts[0].firstOrNull()?.uppercase() ?: ""}${parts[1].firstOrNull()?.uppercase() ?: ""}"
        parts.isNotEmpty() -> parts[0].take(2).uppercase()
        else -> ""
    }
}

/**
 * Format duration in seconds to human-readable format
 */
private fun formatDuration(seconds: Int): String {
    return when {
        seconds < 60 -> "$seconds sec"
        seconds < 3600 -> {
            val minutes = seconds / 60
            val secs = seconds % 60
            if (secs > 0) "$minutes min $secs sec" else "$minutes min"
        }
        else -> {
            val hours = seconds / 3600
            val minutes = (seconds % 3600) / 60
            if (minutes > 0) "$hours hr $minutes min" else "$hours hr"
        }
    }
}

/**
 * Format timestamp as date
 */
private fun formatDate(timestamp: Long): String {
    val calendar = Calendar.getInstance()
    val callCalendar = Calendar.getInstance().apply { timeInMillis = timestamp }

    // Check if same year
    val sameYear = calendar.get(Calendar.YEAR) == callCalendar.get(Calendar.YEAR)

    val format = if (sameYear) {
        SimpleDateFormat("EEEE, MMMM dd", Locale.getDefault())
    } else {
        SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault())
    }

    return format.format(Date(timestamp))
}

/**
 * Format timestamp as time
 */
private fun formatTime(timestamp: Long): String {
    val format = SimpleDateFormat("h:mm a", Locale.getDefault())
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
