package com.tomasronis.rhentiapp.presentation.main.chathub.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tomasronis.rhentiapp.data.chathub.models.ChatMessage
import com.tomasronis.rhentiapp.presentation.theme.Success
import com.tomasronis.rhentiapp.presentation.theme.Warning

/**
 * Booking message card component.
 * Shows booking details with status indicator, date, questionnaire link,
 * and approve/alter/decline actions for owner.
 */
@Composable
fun BookingMessageCard(
    message: ChatMessage,
    onApprove: (String) -> Unit,
    onDecline: (String) -> Unit,
    onProposeAlternative: (String) -> Unit,
    onQuestionnaireClick: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isOwner = message.sender == "owner"
    val metadata = message.metadata
    val bookingId = metadata?.bookViewingId ?: metadata?.bookingId ?: message.id
    val status = metadata?.bookViewingRequestStatus ?: metadata?.bookingStatus ?: "pending"

    // Debug logging to see what metadata we have
    if (com.tomasronis.rhentiapp.BuildConfig.DEBUG) {
        android.util.Log.d("BookingMessageCard", "=== Booking Message ===")
        android.util.Log.d("BookingMessageCard", "Message ID: ${message.id}")
        android.util.Log.d("BookingMessageCard", "Metadata: $metadata")
        android.util.Log.d("BookingMessageCard", "bookViewingDateTimeArr: ${metadata?.bookViewingDateTimeArr}")
        android.util.Log.d("BookingMessageCard", "bookViewingTime: ${metadata?.bookViewingTime}")
        android.util.Log.d("BookingMessageCard", "viewingTime (legacy): ${metadata?.viewingTime}")
        android.util.Log.d("BookingMessageCard", "propertyAddress: ${metadata?.propertyAddress}")
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = if (isOwner) Arrangement.End else Arrangement.Start
    ) {
        Column(
            horizontalAlignment = if (isOwner) Alignment.End else Alignment.Start,
            modifier = Modifier.widthIn(max = 280.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF2C3E50) // Dark grey background
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    // Header: House icon + "Viewing Requested"
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // House icon with border (grey theme)
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    color = Color(0xFF7B92B2).copy(alpha = 0.2f), // Grey-blue with alpha
                                    shape = CircleShape
                                )
                                .border(
                                    width = 1.dp,
                                    color = Color(0xFF7B92B2).copy(alpha = 0.3f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Home,
                                contentDescription = "Property",
                                tint = Color(0xFF7B92B2), // Grey-blue icon
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Viewing Requested",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White // White text on dark background
                            )

                            // Address line - single line, grey, truncated
                            if (metadata?.propertyAddress != null) {
                                Text(
                                    text = metadata.propertyAddress,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF8E8E93), // Gray text like LinkMessageCard
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Status indicator in rounded rectangle
                    BookingStatusIndicator(status = status)

                    Spacer(modifier = Modifier.height(12.dp))

                    // Calendar icon + date requested - Surface for consistent sizing
                    // Get the viewing time from metadata and format it nicely
                    val rawTime = metadata?.bookViewingDateTimeArr?.firstOrNull()
                        ?: metadata?.bookViewingTime
                        ?: metadata?.viewingTime

                    val displayTime = if (rawTime != null) {
                        formatViewingDateTime(rawTime)
                    } else {
                        "Date not specified"
                    }

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF34495E), // Slightly lighter grey for section
                        tonalElevation = 1.dp
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CalendarToday,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = Color(0xFF7B92B2) // Grey-blue icon
                            )
                            Text(
                                text = displayTime,
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Renter Questionnaire - Card-wide clickable button (aligned with calendar)
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onQuestionnaireClick?.invoke(bookingId) },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF34495E), // Same grey as calendar section
                        tonalElevation = 1.dp
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Assignment,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = Color(0xFF7B92B2) // Grey-blue icon (aligned with calendar)
                            )
                            Text(
                                text = "Renter Questionnaire",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Filled.ChevronRight,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = Color(0xFF7B92B2) // Grey-blue icon
                            )
                        }
                    }

                    // Action buttons (only for pending bookings) - Circular iOS-style
                    if (status == "pending") {
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            // Accept button - circular
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilledIconButton(
                                    onClick = { onApprove(bookingId) },
                                    modifier = Modifier.size(56.dp),
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = Success,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = "Accept",
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Text(
                                    text = "Accept",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White // White text on dark background
                                )
                            }

                            // Alter button - circular
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilledIconButton(
                                    onClick = { onProposeAlternative(bookingId) },
                                    modifier = Modifier.size(56.dp),
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = Warning,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Schedule,
                                        contentDescription = "Alter",
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Text(
                                    text = "Alter",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White // White text on dark background
                                )
                            }

                            // Decline button - circular (RED)
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                FilledIconButton(
                                    onClick = { onDecline(bookingId) },
                                    modifier = Modifier.size(56.dp),
                                    colors = IconButtonDefaults.filledIconButtonColors(
                                        containerColor = Color(0xFFFF3B30), // iOS red
                                        contentColor = Color.White
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = "Decline",
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Text(
                                    text = "Decline",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White // White text on dark background
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Timestamp
            Text(
                text = formatTimestamp(message.createdAt),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * Status indicator showing approved/pending/declined in a rounded rectangle pill.
 */
@Composable
private fun BookingStatusIndicator(status: String) {
    val (color, label, textColor) = when (status) {
        "confirmed" -> Triple(Success, "Approved", Color.White)
        "declined" -> Triple(Color(0xFFFF3B30), "Declined", Color.White)
        else -> Triple(Warning, "Pending", Color.White)
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.25f),
        modifier = Modifier.wrapContentWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(color = color, shape = CircleShape)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = textColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Format timestamp (reuse from MessageBubble).
 */
private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 86400_000 -> {
            val timeFormat = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
            timeFormat.format(java.util.Date(timestamp))
        }
        diff < 604800_000 -> {
            val dayTimeFormat = java.text.SimpleDateFormat("EEE h:mm a", java.util.Locale.getDefault())
            dayTimeFormat.format(java.util.Date(timestamp))
        }
        else -> {
            val dateTimeFormat = java.text.SimpleDateFormat("MMM d, h:mm a", java.util.Locale.getDefault())
            dateTimeFormat.format(java.util.Date(timestamp))
        }
    }
}

/**
 * Format viewing date/time to single line format.
 * Supports multiple date formats from the API:
 * - ISO 8601 strings (e.g., "2026-02-01T14:00:00Z")
 * - Unix timestamps as strings (e.g., "1738429200000")
 * - Already formatted strings (returned as-is if parsing fails)
 *
 * Returns format: "MMM DD, YYYY | TIME"
 * Example: "Feb 01, 2026 | 2:00 PM"
 */
private fun formatViewingDateTime(dateTimeString: String): String {
    return try {
        // Try parsing as Unix timestamp (milliseconds)
        val timestamp = dateTimeString.toLongOrNull()
        val date = if (timestamp != null && timestamp > 1000000000000L) {
            // Valid Unix timestamp in milliseconds
            java.util.Date(timestamp)
        } else {
            // Try parsing as ISO 8601 or other date format
            val isoFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
            isoFormat.isLenient = true
            try {
                isoFormat.parse(dateTimeString)
            } catch (e: Exception) {
                // Try other common formats
                try {
                    val altFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                    altFormat.parse(dateTimeString)
                } catch (e2: Exception) {
                    // If all parsing fails, return the original string
                    return dateTimeString
                }
            }
        }

        // Format as: "MMM DD, YYYY | TIME"
        val dateFormat = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
        val timeFormat = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())

        "${dateFormat.format(date)} | ${timeFormat.format(date)}"
    } catch (e: Exception) {
        // If anything fails, return the original string
        dateTimeString
    }
}
