package com.tomasronis.rhentiapp.presentation.main.chathub.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tomasronis.rhentiapp.data.chathub.models.ChatMessage
import com.tomasronis.rhentiapp.presentation.theme.Success
import com.tomasronis.rhentiapp.presentation.theme.Warning

/**
 * Viewing status message card displayed when a viewing is confirmed, declined,
 * or alternatives are proposed. Shows viewing details with a "Manage Viewing" button.
 *
 * bookViewingType values handled:
 * - "confirm" → Viewing Confirmed (green)
 * - "decline" → Viewing Declined (red)
 * - "change_request" / "alternative" → Alternatives Proposed (orange)
 */
@Composable
fun ViewingStatusMessageCard(
    message: ChatMessage,
    onManageViewing: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isOwner = message.sender == "owner"
    val metadata = message.metadata
    val bookingId = metadata?.bookViewingId ?: metadata?.bookingId ?: message.id
    val viewingType = metadata?.bookViewingType ?: ""

    val (statusLabel, statusColor, headerTitle, headerIcon) = when (viewingType) {
        "confirm" -> StatusInfo(
            label = "Approved",
            color = Success,
            title = "Viewing Confirmed",
            icon = Icons.Filled.CheckCircle
        )
        "decline" -> StatusInfo(
            label = "Declined",
            color = Color(0xFFFF3B30),
            title = "Viewing Declined",
            icon = Icons.Filled.Cancel
        )
        "change_request", "alternative" -> StatusInfo(
            label = "Alternatives",
            color = Warning,
            title = "Alternatives Proposed",
            icon = Icons.Filled.Schedule
        )
        else -> StatusInfo(
            label = "Updated",
            color = Color(0xFF007AFF),
            title = "Viewing Updated",
            icon = Icons.Filled.Update
        )
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
                    containerColor = Color(0xFF2C3E50)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    // Header: Icon + Title
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Status icon with colored circular background
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    color = statusColor.copy(alpha = 0.2f),
                                    shape = CircleShape
                                )
                                .border(
                                    width = 1.dp,
                                    color = statusColor.copy(alpha = 0.3f),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = headerIcon,
                                contentDescription = headerTitle,
                                tint = statusColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = headerTitle,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )

                            if (metadata?.propertyAddress != null) {
                                Text(
                                    text = metadata.propertyAddress,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF8E8E93),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Status pill indicator
                    ViewingStatusPill(label = statusLabel, color = statusColor)

                    Spacer(modifier = Modifier.height(12.dp))

                    // Date/time section
                    val rawTime = metadata?.bookViewingDateTimeArr?.firstOrNull()
                        ?: metadata?.bookViewingTime
                        ?: metadata?.viewingTime

                    if (rawTime != null) {
                        val displayTime = formatViewingStatusDateTime(rawTime)

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF34495E),
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
                                    tint = Color(0xFF7B92B2)
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
                    }

                    // Alternative times section (for change_request)
                    if ((viewingType == "change_request" || viewingType == "alternative") &&
                        metadata?.bookViewingAlternativeArr != null
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))
                        metadata.bookViewingAlternativeArr.forEach { altTimeArr ->
                            val altDisplay = altTimeArr.joinToString(" | ")
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF34495E),
                                tonalElevation = 1.dp
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AccessTime,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = Warning
                                    )
                                    Text(
                                        text = altDisplay,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                    Spacer(modifier = Modifier.height(16.dp))

                    // Manage Viewing button
                    Button(
                        onClick = { onManageViewing(bookingId) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF007AFF),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Manage Viewing",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Timestamp
            Text(
                text = formatStatusTimestamp(message.createdAt),
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

/**
 * Status pill indicator showing the viewing status.
 */
@Composable
private fun ViewingStatusPill(label: String, color: Color) {
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
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

/**
 * Helper data class for status card configuration.
 */
private data class StatusInfo(
    val label: String,
    val color: Color,
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

/**
 * Format timestamp for the status card.
 */
private fun formatStatusTimestamp(timestamp: Long): String {
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
 * Format viewing date/time for display in the status card.
 * Supports ISO 8601, Unix timestamps, and pre-formatted strings.
 */
private fun formatViewingStatusDateTime(dateTimeString: String): String {
    return try {
        val timestamp = dateTimeString.toLongOrNull()
        val date = if (timestamp != null && timestamp > 1000000000000L) {
            java.util.Date(timestamp)
        } else {
            val isoFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
            isoFormat.isLenient = true
            try {
                isoFormat.parse(dateTimeString.replace("Z", "").split(".")[0])
            } catch (_: Exception) {
                try {
                    val altFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                    altFormat.parse(dateTimeString)
                } catch (_: Exception) {
                    return dateTimeString
                }
            }
        }

        val dateFormat = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
        val timeFormat = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())

        "${dateFormat.format(date)} | ${timeFormat.format(date)}"
    } catch (_: Exception) {
        dateTimeString
    }
}
