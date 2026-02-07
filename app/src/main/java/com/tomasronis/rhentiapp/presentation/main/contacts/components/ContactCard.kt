package com.tomasronis.rhentiapp.presentation.main.contacts.components

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.tomasronis.rhentiapp.data.contacts.models.Contact
import java.text.SimpleDateFormat
import java.util.*

/**
 * Card component for displaying a contact in the list.
 */
@Composable
fun ContactCard(
    contact: Contact,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Debug logging
    android.util.Log.d(
        "ContactCard",
        "Contact: ${contact.displayName}, avatarUrl=${contact.avatarUrl}, channel=${contact.channel}"
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar
            ContactAvatar(
                avatarUrl = contact.avatarUrl,
                displayName = contact.displayName
            )

            // Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically)
            ) {
                Text(
                    text = contact.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Contact methods
                if (contact.email != null || contact.phone != null) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (contact.email != null) {
                            Icon(
                                imageVector = Icons.Filled.Email,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = contact.email,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }

                        if (contact.phone != null) {
                            if (contact.email != null) {
                                Text(
                                    text = "•",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                imageVector = Icons.Filled.Phone,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = contact.phone,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Activity stats
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (contact.totalMessages > 0) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Chat,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${contact.totalMessages}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (contact.totalCalls > 0) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Phone,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = "${contact.totalCalls}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (contact.lastActivity != null) {
                        Text(
                            text = "• ${formatTimestamp(contact.lastActivity)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Channel tag
                if (contact.channel != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = getPlatformName(contact.channel),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Chevron icon
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = "View details",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Avatar for contact - shows image or initials.
 */
@Composable
private fun ContactAvatar(
    avatarUrl: String?,
    displayName: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        // Always show initials as background/fallback
        Text(
            text = getInitials(displayName),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.Bold
        )

        // Show image if available (overlays initials)
        if (!avatarUrl.isNullOrBlank()) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = "Avatar",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

/**
 * Get initials from display name (first letter of first two words).
 */
private fun getInitials(name: String): String {
    val parts = name.trim().split(" ")
    return when {
        parts.size >= 2 -> "${parts[0].firstOrNull()?.uppercaseChar() ?: ""}${parts[1].firstOrNull()?.uppercaseChar() ?: ""}"
        parts.isNotEmpty() -> "${parts[0].firstOrNull()?.uppercaseChar() ?: ""}"
        else -> "?"
    }
}

/**
 * Format timestamp to relative time.
 */
private fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 3600_000 -> "Active recently"
        diff < 86400_000 -> "Today"
        diff < 172800_000 -> "Yesterday"
        diff < 604800_000 -> {
            val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
            dayFormat.format(Date(timestamp))
        }
        else -> {
            val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
            dateFormat.format(Date(timestamp))
        }
    }
}

/**
 * Get platform name from channel data.
 * Maps channel names to display names.
 */
private fun getPlatformName(channel: String?): String {
    return when (channel?.lowercase()) {
        "facebook" -> "Facebook"
        "kijiji" -> "Kijiji"
        "zumper" -> "Zumper"
        "rhenti" -> "rhenti"
        "facebook-listing-page", "facebook_listing_page" -> "Rhenti-powered listing pages"
        else -> channel?.replaceFirstChar { it.uppercase() } ?: "Unknown"
    }
}
