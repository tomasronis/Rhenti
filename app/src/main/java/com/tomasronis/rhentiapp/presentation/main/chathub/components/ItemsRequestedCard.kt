package com.tomasronis.rhentiapp.presentation.main.chathub.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tomasronis.rhentiapp.data.chathub.models.ChatMessage
import java.text.SimpleDateFormat
import java.util.*

/**
 * Message card for items-requested type messages.
 * Displays with a distinct color and bullet points for requested items.
 */
@Composable
fun ItemsRequestedCard(
    message: ChatMessage,
    modifier: Modifier = Modifier
) {
    val isOwner = message.sender == "owner"
    val items = message.metadata?.items ?: emptyList()

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
            // Message bubble with distinct color for items-requested
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isOwner) 16.dp else 4.dp,
                            bottomEnd = if (isOwner) 4.dp else 16.dp
                        )
                    )
                    .background(
                        // Use a pale/light green color for items-requested messages
                        Color(0xFFB8E6B8)
                    )
                    .padding(12.dp)
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Title text
                    Text(
                        text = message.text ?: "Additional items requested",
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = Color(0xFF1B5E20) // Dark green for contrast on pale green
                    )

                    // Bullet point list of items
                    if (items.isNotEmpty()) {
                        Column(
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items.forEach { item ->
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text(
                                        text = "•",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF1B5E20) // Dark green for contrast
                                    )
                                    Text(
                                        text = formatItemName(item),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color(0xFF1B5E20) // Dark green for contrast
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Smaller timestamp
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
 * Convert SCREAMING_SNAKE_CASE to Sentence case and remove underscores.
 * Examples:
 * - "CREDIT_REPORT" -> "Credit report"
 * - "FINGUARANTOR_DETAILS" -> "Finguarantor details"
 */
private fun formatItemName(item: String): String {
    return item
        .lowercase()
        .replace("_", " ")
        .replaceFirstChar { it.uppercase() }
}

/**
 * Format timestamp to always show full date and time.
 * Format: "MMM d, h:mm a" (e.g., "Jan 8, 2:30 PM")
 */
private fun formatTimestamp(timestamp: Long): String {
    val calendar = Calendar.getInstance()
    val now = Calendar.getInstance()
    val messageDate = Calendar.getInstance().apply {
        timeInMillis = timestamp
    }

    val monthDayTimeFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    val fullDateTimeFormat = SimpleDateFormat("MMM d, yyyy, h:mm a", Locale.getDefault())

    return if (isSameYear(messageDate, now)) {
        // This year: "Jan 8, 2:30 PM"
        monthDayTimeFormat.format(Date(timestamp))
    } else {
        // Different year: "Jan 8, 2024, 2:30 PM"
        fullDateTimeFormat.format(Date(timestamp))
    }
}

private fun isSameYear(date: Calendar, now: Calendar): Boolean {
    return date.get(Calendar.YEAR) == now.get(Calendar.YEAR)
}
