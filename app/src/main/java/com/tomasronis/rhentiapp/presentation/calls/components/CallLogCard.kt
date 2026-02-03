package com.tomasronis.rhentiapp.presentation.calls.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tomasronis.rhentiapp.data.calls.models.CallLog
import com.tomasronis.rhentiapp.data.calls.models.CallType
import java.text.SimpleDateFormat
import java.util.*

/**
 * Call log card showing call details.
 */
@Composable
fun CallLogCard(
    callLog: CallLog,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ListItem(
        headlineContent = {
            Text(
                text = callLog.contactName ?: callLog.contactPhone,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
        },
        supportingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Call type icon
                Icon(
                    imageVector = when (callLog.callType) {
                        CallType.INCOMING -> Icons.Filled.CallReceived
                        CallType.OUTGOING -> Icons.Filled.CallMade
                        CallType.MISSED -> Icons.Filled.CallMissed
                    },
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = when (callLog.callType) {
                        CallType.MISSED -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )

                // Timestamp
                Text(
                    text = formatCallTime(callLog.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = when (callLog.callType) {
                        CallType.MISSED -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        },
        leadingContent = {
            // Avatar
            if (callLog.contactAvatar != null) {
                AsyncImage(
                    model = callLog.contactAvatar,
                    contentDescription = null,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Initials or phone icon
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    if (callLog.contactName != null) {
                        Text(
                            text = getInitials(callLog.contactName),
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Phone,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        },
        trailingContent = {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Duration
                if (callLog.duration > 0) {
                    Text(
                        text = formatDuration(callLog.duration),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Call button
                IconButton(onClick = onClick) {
                    Icon(
                        imageVector = Icons.Filled.Phone,
                        contentDescription = "Call",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        },
        modifier = modifier.clickable { onClick() }
    )
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
 * Format call timestamp
 */
private fun formatCallTime(timestamp: Long): String {
    val calendar = Calendar.getInstance()
    val callCalendar = Calendar.getInstance().apply { timeInMillis = timestamp }

    // Check if same day
    val isToday = calendar.get(Calendar.YEAR) == callCalendar.get(Calendar.YEAR) &&
            calendar.get(Calendar.DAY_OF_YEAR) == callCalendar.get(Calendar.DAY_OF_YEAR)

    // Check if yesterday
    calendar.add(Calendar.DAY_OF_YEAR, -1)
    val isYesterday = calendar.get(Calendar.YEAR) == callCalendar.get(Calendar.YEAR) &&
            calendar.get(Calendar.DAY_OF_YEAR) == callCalendar.get(Calendar.DAY_OF_YEAR)

    return when {
        isToday -> {
            val format = SimpleDateFormat("h:mm a", Locale.getDefault())
            format.format(Date(timestamp))
        }
        isYesterday -> {
            val format = SimpleDateFormat("h:mm a", Locale.getDefault())
            "Yesterday, ${format.format(Date(timestamp))}"
        }
        else -> {
            val format = SimpleDateFormat("MMM dd, h:mm a", Locale.getDefault())
            format.format(Date(timestamp))
        }
    }
}

/**
 * Format call duration in seconds to mm:ss
 */
private fun formatDuration(seconds: Int): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", minutes, secs)
}
