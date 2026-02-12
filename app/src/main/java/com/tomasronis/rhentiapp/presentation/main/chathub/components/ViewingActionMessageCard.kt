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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tomasronis.rhentiapp.data.chathub.models.ChatMessage
import com.tomasronis.rhentiapp.presentation.theme.Success
import com.tomasronis.rhentiapp.presentation.theme.Warning

/**
 * Viewing action message card component.
 * Shows when an action has been taken on a viewing request (confirm, decline, or change_request).
 * Similar to BookingMessageCard but shows all 3 action buttons with appropriate ones greyed out.
 * For change_request, allows selecting from up to 3 alternative times.
 */
@Composable
fun ViewingActionMessageCard(
    message: ChatMessage,
    onProposeAlternative: (String) -> Unit,
    onAccept: ((String) -> Unit)? = null,
    onDecline: ((String) -> Unit)? = null,
    onCheckIn: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isOwner = message.sender == "owner"
    val metadata = message.metadata
    val bookingId = metadata?.bookViewingId ?: metadata?.bookingId ?: message.id
    val viewingType = metadata?.bookViewingType ?: ""

    // Determine status from bookViewingType or bookViewingRequestStatus
    val status = when (viewingType) {
        "confirm" -> "confirmed"
        "decline" -> "declined"
        "change_request" -> "alternative"
        else -> metadata?.bookViewingRequestStatus ?: metadata?.bookingStatus ?: "pending"
    }

    // For change_request, track selected alternative time
    var selectedAlternativeIndex by remember { mutableStateOf<Int?>(null) }
    val alternativeTimes = metadata?.bookViewingAlternativeArr ?: emptyList()

    // Debug logging
    if (com.tomasronis.rhentiapp.BuildConfig.DEBUG) {
        android.util.Log.d("ViewingActionCard", "=== Viewing Action Message ===")
        android.util.Log.d("ViewingActionCard", "Message ID: ${message.id}")
        android.util.Log.d("ViewingActionCard", "bookViewingType: $viewingType")
        android.util.Log.d("ViewingActionCard", "status: $status")
        android.util.Log.d("ViewingActionCard", "alternativeTimes: $alternativeTimes")
    }

    // Determine if Check-In should be shown:
    // Viewing must be confirmed AND viewing date is today or in the past
    val showCheckIn = remember(status, metadata) {
        if (status != "confirmed") return@remember false
        val rawTime = metadata?.bookViewingDateTimeArr?.firstOrNull()
            ?: metadata?.bookViewingTime
            ?: metadata?.viewingTime
            ?: return@remember false
        isViewingTodayOrPast(rawTime)
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
                    containerColor = Color(0xFF8B9DC3) // Darker blue-ish silver background
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.Start
                ) {
                    // Header: House icon + Title based on action type
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // House icon with border
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .background(
                                    color = Color(0xFFB8C6E0), // Light blue-ish silver
                                    shape = CircleShape
                                )
                                .border(
                                    width = 1.5.dp,
                                    color = Color(0xFF6B7D9F), // Darker blue-ish border
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Home,
                                contentDescription = "Property",
                                tint = Color(0xFF2C3E50), // Dark blue-grey icon
                                modifier = Modifier.size(26.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            val title = when (viewingType) {
                                "confirm" -> "Viewing Confirmed"
                                "decline" -> "Viewing Declined"
                                "change_request" -> "Alternative Time Proposed"
                                else -> "Viewing Updated"
                            }

                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2C2C2C) // Dark text on silver background
                            )

                            // Address line - single line, grey, truncated
                            if (metadata?.propertyAddress != null) {
                                Text(
                                    text = metadata.propertyAddress,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF606060), // Darker grey text
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Status indicator in rounded rectangle
                    // Debug logging for status
                    if (com.tomasronis.rhentiapp.BuildConfig.DEBUG) {
                        android.util.Log.d("ViewingActionCard", "Status value for indicator: '$status'")
                        android.util.Log.d("ViewingActionCard", "bookViewingRequestStatus: '${metadata?.bookViewingRequestStatus}'")
                        android.util.Log.d("ViewingActionCard", "bookingStatus: '${metadata?.bookingStatus}'")
                    }
                    ViewingStatusIndicator(status = status)

                    // Only show top date/time section if NOT showing alternative times
                    if (viewingType != "change_request" || alternativeTimes.isEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))

                        // Calendar icon + date requested - Surface for consistent sizing
                        // If bookViewingDateTimeArr has both date and time, combine them
                        val displayTime = when {
                            metadata?.bookViewingDateTimeArr?.size == 2 -> {
                                val formattedDate = formatDateWithShortMonth(metadata.bookViewingDateTimeArr[0])
                                "$formattedDate | ${metadata.bookViewingDateTimeArr[1]}"
                            }
                            metadata?.bookViewingTime != null -> {
                                formatViewingDateTime(metadata.bookViewingTime)
                            }
                            metadata?.viewingTime != null -> {
                                formatViewingDateTime(metadata.viewingTime)
                            }
                            else -> "Date not specified"
                        }

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFA4B5D4), // Lighter blue-ish silver for section
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
                                    tint = Color(0xFF2C3E50) // Dark blue-grey icon
                                )
                                Text(
                                    text = displayTime,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFF2C2C2C),
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Alternative times selection (for change_request only)
                    if (viewingType == "change_request" && alternativeTimes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Select Preferred Time:",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF2C2C2C),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // Show up to 3 alternative times
                        alternativeTimes.take(3).forEachIndexed { index, altTimeArr ->
                            // If altTimeArr has both date and time, combine them
                            val displayAltTime = when {
                                altTimeArr.size == 2 -> {
                                    val formattedDate = formatDateWithShortMonth(altTimeArr[0])
                                    "$formattedDate | ${altTimeArr[1]}"
                                }
                                altTimeArr.isNotEmpty() -> formatViewingDateTime(altTimeArr[0])
                                else -> "Alternative ${index + 1}"
                            }

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { selectedAlternativeIndex = index },
                                shape = RoundedCornerShape(12.dp),
                                color = if (selectedAlternativeIndex == index) {
                                    Color(0xFF007AFF).copy(alpha = 0.2f) // Light blue when selected
                                } else {
                                    Color(0xFFA4B5D4) // Lighter blue-ish silver
                                },
                                tonalElevation = 1.dp,
                                border = if (selectedAlternativeIndex == index) {
                                    androidx.compose.foundation.BorderStroke(2.dp, Color(0xFF007AFF))
                                } else null
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                                ) {
                                    Icon(
                                        imageVector = if (selectedAlternativeIndex == index) {
                                            Icons.Filled.CheckCircle
                                        } else {
                                            Icons.Filled.RadioButtonUnchecked
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = if (selectedAlternativeIndex == index) {
                                            Color(0xFF007AFF)
                                        } else {
                                            Color(0xFF2C3E50)
                                        }
                                    )
                                    Text(
                                        text = displayAltTime,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF2C2C2C),
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    // Action buttons row - Always show all 3 buttons
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color(0xFF808080).copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Determine which buttons should be enabled
                        val acceptEnabled = when {
                            viewingType == "confirm" -> false // Already confirmed
                            viewingType == "change_request" -> selectedAlternativeIndex != null // Only enable if time selected
                            else -> true // For decline or other types
                        }
                        val alterEnabled = true // Always enabled
                        val declineEnabled = viewingType != "decline"

                        // Accept button
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.alpha(if (acceptEnabled) 1f else 0.4f)
                        ) {
                            FilledIconButton(
                                onClick = {
                                    if (acceptEnabled) {
                                        if (com.tomasronis.rhentiapp.BuildConfig.DEBUG) {
                                            android.util.Log.d("ViewingActionCard", "Accept clicked for booking: $bookingId")
                                        }
                                        onAccept?.invoke(bookingId)
                                    }
                                },
                                modifier = Modifier.size(56.dp),
                                enabled = acceptEnabled,
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = Success,
                                    contentColor = Color.White,
                                    disabledContainerColor = Success.copy(alpha = 0.5f),
                                    disabledContentColor = Color.White.copy(alpha = 0.5f)
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
                                color = Color(0xFF2C2C2C)
                            )
                        }

                        // Alter button
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.alpha(if (alterEnabled) 1f else 0.4f)
                        ) {
                            FilledIconButton(
                                onClick = {
                                    if (alterEnabled) {
                                        onProposeAlternative(bookingId)
                                    }
                                },
                                modifier = Modifier.size(56.dp),
                                enabled = alterEnabled,
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = Warning,
                                    contentColor = Color.White,
                                    disabledContainerColor = Warning.copy(alpha = 0.5f),
                                    disabledContentColor = Color.White.copy(alpha = 0.5f)
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
                                color = Color(0xFF2C2C2C)
                            )
                        }

                        // Decline button
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.alpha(if (declineEnabled) 1f else 0.4f)
                        ) {
                            FilledIconButton(
                                onClick = {
                                    if (declineEnabled) {
                                        if (com.tomasronis.rhentiapp.BuildConfig.DEBUG) {
                                            android.util.Log.d("ViewingActionCard", "Decline clicked for booking: $bookingId")
                                        }
                                        onDecline?.invoke(bookingId)
                                    }
                                },
                                modifier = Modifier.size(56.dp),
                                enabled = declineEnabled,
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = Color(0xFFFF3B30), // iOS red
                                    contentColor = Color.White,
                                    disabledContainerColor = Color(0xFFFF3B30).copy(alpha = 0.5f),
                                    disabledContentColor = Color.White.copy(alpha = 0.5f)
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
                                color = Color(0xFF2C2C2C)
                            )
                        }
                    }

                    // Check-In button (shown separately below action buttons)
                    if (showCheckIn && onCheckIn != null) {
                        Spacer(modifier = Modifier.height(12.dp))

                        Button(
                            onClick = { onCheckIn(bookingId) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF007AFF), // iOS blue
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Check-In",
                                fontWeight = FontWeight.SemiBold
                            )
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
 * Format a date string from API format to MMM dd, yyyy format.
 * Handles formats like "February 12, 2026" and converts to "Feb 12, 2026"
 */
private fun formatDateWithShortMonth(dateString: String): String {
    return try {
        // Try to parse the full month name format
        val inputFormat = java.text.SimpleDateFormat("MMMM d, yyyy", java.util.Locale.ENGLISH)
        val outputFormat = java.text.SimpleDateFormat("MMM d, yyyy", java.util.Locale.ENGLISH)
        val date = inputFormat.parse(dateString)
        if (date != null) {
            outputFormat.format(date)
        } else {
            dateString // Return original if parsing fails
        }
    } catch (e: Exception) {
        // If parsing fails, return original string
        dateString
    }
}

/**
 * Status indicator showing accepted/pending/declined in a rounded rectangle pill.
 */
@Composable
private fun ViewingStatusIndicator(status: String) {
    val (color, label, textColor) = when (status) {
        "confirmed" -> Triple(Color(0xFF34C759), "Accepted", Color.White) // iOS green
        "declined" -> Triple(Color(0xFFFF3B30), "Declined", Color.White) // iOS red
        "alternative" -> Triple(Warning, "Alternative Proposed", Color.White)
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
 * Determine if a viewing date/time string represents today or a past date.
 * Supports ISO 8601 strings and Unix timestamp strings.
 */
private fun isViewingTodayOrPast(dateTimeString: String): Boolean {
    return try {
        val viewingDate = parseViewingDate(dateTimeString) ?: return false
        val now = java.util.Calendar.getInstance()
        val viewingCal = java.util.Calendar.getInstance().apply { time = viewingDate }

        // Check if viewing is today or in the past (compare date only, ignore time)
        val todayStart = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }

        !viewingCal.before(todayStart) && viewingCal.get(java.util.Calendar.YEAR) == now.get(java.util.Calendar.YEAR) &&
            viewingCal.get(java.util.Calendar.DAY_OF_YEAR) == now.get(java.util.Calendar.DAY_OF_YEAR) ||
            viewingCal.before(now)
    } catch (e: Exception) {
        false
    }
}

/**
 * Parse a viewing date/time string into a Date object.
 */
private fun parseViewingDate(dateTimeString: String): java.util.Date? {
    // Try parsing as Unix timestamp (milliseconds)
    val timestamp = dateTimeString.toLongOrNull()
    if (timestamp != null && timestamp > 1000000000000L) {
        return java.util.Date(timestamp)
    }

    // Try parsing as ISO 8601
    try {
        val isoFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
        isoFormat.isLenient = true
        return isoFormat.parse(dateTimeString.replace("Z", "").split(".")[0])
    } catch (_: Exception) {}

    // Try alternate format
    try {
        val altFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
        return altFormat.parse(dateTimeString)
    } catch (_: Exception) {}

    return null
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
    if (com.tomasronis.rhentiapp.BuildConfig.DEBUG) {
        android.util.Log.d("ViewingActionCard", "formatViewingDateTime input: $dateTimeString")
    }

    return try {
        // Try parsing as Unix timestamp (milliseconds)
        val timestamp = dateTimeString.toLongOrNull()
        val date = if (timestamp != null && timestamp > 1000000000000L) {
            // Valid Unix timestamp in milliseconds
            if (com.tomasronis.rhentiapp.BuildConfig.DEBUG) {
                android.util.Log.d("ViewingActionCard", "Parsed as Unix timestamp: $timestamp")
            }
            java.util.Date(timestamp)
        } else {
            // Try parsing as ISO 8601 with various formats
            try {
                // Try ISO 8601 with milliseconds and timezone: "2026-02-12T15:45:00.000Z"
                val isoFormatZ = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault())
                isoFormatZ.timeZone = java.util.TimeZone.getTimeZone("UTC")
                isoFormatZ.parse(dateTimeString)
            } catch (e: Exception) {
                try {
                    // Try ISO 8601 without milliseconds: "2026-02-12T15:45:00Z"
                    val isoFormatSimple = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", java.util.Locale.getDefault())
                    isoFormatSimple.timeZone = java.util.TimeZone.getTimeZone("UTC")
                    isoFormatSimple.parse(dateTimeString)
                } catch (e2: Exception) {
                    try {
                        // Try ISO 8601 without timezone: "2026-02-12T15:45:00"
                        val isoFormat = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", java.util.Locale.getDefault())
                        isoFormat.parse(dateTimeString)
                    } catch (e3: Exception) {
                        try {
                            // Try other common format: "2026-02-12 15:45:00"
                            val altFormat = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                            altFormat.parse(dateTimeString)
                        } catch (e4: Exception) {
                            // If all parsing fails, return the original string
                            return dateTimeString
                        }
                    }
                }
            }
        }

        // Format as: "MMM dd, yyyy | h:mm a"
        val dateFormat = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault())
        val timeFormat = java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())

        val formatted = "${dateFormat.format(date)} | ${timeFormat.format(date)}"
        if (com.tomasronis.rhentiapp.BuildConfig.DEBUG) {
            android.util.Log.d("ViewingActionCard", "Successfully formatted to: $formatted")
        }
        formatted
    } catch (e: Exception) {
        // If anything fails, return the original string
        if (com.tomasronis.rhentiapp.BuildConfig.DEBUG) {
            android.util.Log.e("ViewingActionCard", "Failed to parse datetime: ${e.message}", e)
        }
        dateTimeString
    }
}
