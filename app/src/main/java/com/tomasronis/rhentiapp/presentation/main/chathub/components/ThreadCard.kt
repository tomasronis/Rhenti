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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tomasronis.rhentiapp.data.chathub.models.ChatThread
import com.tomasronis.rhentiapp.presentation.theme.*
import java.text.SimpleDateFormat
import java.util.*

/**
 * Card component for displaying a chat thread in the list.
 * Matches iOS design with property address, platform tags, and badges.
 */
@Composable
fun ThreadCard(
    thread: ChatThread,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.background
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar with badges
            Box {
                ThreadAvatar(
                    imageUrl = thread.imageUrl,
                    displayName = thread.displayName
                )

                // Small badge icons on avatar (bottom right)
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 4.dp, y = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    // Property icon badge
                    SmallBadgeIcon(
                        icon = Icons.Filled.Home,
                        backgroundColor = RhentiCoral
                    )

                    // Message status icon badge
                    SmallBadgeIcon(
                        icon = Icons.Filled.ChatBubble,
                        backgroundColor = AccentBlue
                    )
                }
            }

            // Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .align(Alignment.CenterVertically)
            ) {
                // Name and timestamp row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = thread.displayName,
                        style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp),
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    Text(
                        text = formatTimestamp(thread.lastMessageTime),
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Property address with location pin (if available)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = getPropertyAddress(thread),
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Last message
                Text(
                    text = thread.lastMessage ?: "No messages yet",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Platform tag (blue pill)
                PlatformTag(platform = getPlatformName(thread))
            }

            // Right side - unread badge and chevron
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.align(Alignment.CenterVertically)
            ) {
                if (thread.unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1C2B3A)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (thread.unreadCount > 9) "9+" else thread.unreadCount.toString(),
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = "Open conversation",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Divider at bottom
        HorizontalDivider(
            modifier = Modifier.padding(start = 84.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
        )
    }
}

/**
 * Small circular badge icon for avatar overlay.
 */
@Composable
private fun SmallBadgeIcon(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    backgroundColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .border(2.dp, MaterialTheme.colorScheme.background, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(12.dp)
        )
    }
}

/**
 * Platform tag pill (e.g., "Rhenti-powered listing pages").
 */
@Composable
private fun PlatformTag(
    platform: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = AccentBlue,
        contentColor = Color.White
    ) {
        Text(
            text = platform,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

/**
 * Get property address from thread data (mock for now).
 */
private fun getPropertyAddress(thread: ChatThread): String {
    // TODO: Add property address field to ChatThread model
    // For now, generate a mock address
    return "88 Queen St E, Unit 4B, Toronto"
}

/**
 * Get platform name from thread data (mock for now).
 */
private fun getPlatformName(thread: ChatThread): String {
    // TODO: Add platform/source field to ChatThread model
    // For now, assign based on thread ID for variety
    val platforms = listOf(
        "Rhenti-powered listing pages",
        "Facebook",
        "Kijiji",
        "Zumper",
        "rhenti"
    )
    return platforms[thread.id.hashCode().mod(platforms.size)]
}

/**
 * Avatar for thread - shows image or initials.
 * Matches iOS design with larger size.
 */
@Composable
private fun ThreadAvatar(
    imageUrl: String?,
    displayName: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(60.dp)
            .clip(CircleShape)
            .background(Color(0xFFE0E0E5)),  // Light gray background for initials
        contentAlignment = Alignment.Center
    ) {
        // Always show initials as background/fallback
        Text(
            text = getInitials(displayName),
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
            color = Color(0xFF6B6B70),  // Dark gray text for initials
            fontWeight = FontWeight.SemiBold
        )

        // Overlay image if URL exists
        if (!imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = "Avatar",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                onError = {
                    // Image failed to load, initials will show through
                }
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
 * Format timestamp to relative time (2m, 1h, Yesterday, Jan 15).
 */
private fun formatTimestamp(timestamp: Long?): String {
    if (timestamp == null) return ""

    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < 60_000 -> "Just now" // Less than 1 minute
        diff < 3600_000 -> "${diff / 60_000}m" // Less than 1 hour
        diff < 86400_000 -> "${diff / 3600_000}h" // Less than 1 day
        diff < 172800_000 -> "Yesterday" // Less than 2 days
        diff < 604800_000 -> { // Less than 7 days
            val dayFormat = SimpleDateFormat("EEEE", Locale.getDefault())
            dayFormat.format(Date(timestamp))
        }
        else -> { // More than 7 days
            val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())
            dateFormat.format(Date(timestamp))
        }
    }
}
