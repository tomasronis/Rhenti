package com.tomasronis.rhentiapp.presentation.main.chathub.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.tomasronis.rhentiapp.presentation.theme.ChatBubbleOwner
import com.tomasronis.rhentiapp.presentation.theme.ChatBubbleRenter
import java.text.SimpleDateFormat
import java.util.*

/**
 * Message bubble component showing a chat message.
 * Owner messages appear right-aligned with blue background.
 * Renter messages appear left-aligned with gray background.
 */
@Composable
fun MessageBubble(
    message: ChatMessage,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isOwner = message.sender == "owner"

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
            // Message bubble
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
                        if (isOwner) ChatBubbleOwner else ChatBubbleRenter
                    )
                    .padding(12.dp)
            ) {
                if (message.text != null) {
                    Text(
                        text = message.text,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (isOwner) Color.White else Color.Black
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Timestamp and status
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatTimestamp(message.createdAt),
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )

                // Status indicator
                when (message.status) {
                    "sending" -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(12.dp),
                            strokeWidth = 1.dp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    "sent" -> {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = "Sent",
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    "failed" -> {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Error,
                                contentDescription = "Failed",
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.error
                            )
                            if (onRetry != null) {
                                TextButton(
                                    onClick = onRetry,
                                    modifier = Modifier.height(24.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp)
                                ) {
                                    Text(
                                        text = "Retry",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
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
