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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
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

// Static formatters to avoid recreation (thread-safe in modern Android)
private val dayFormatter = SimpleDateFormat("EEEE", Locale.getDefault())
private val dateFormatter = SimpleDateFormat("MMM d", Locale.getDefault())

// Static platform list to avoid recreation
private val PLATFORMS = listOf(
    "Rhenti-powered listing pages",
    "Facebook",
    "Kijiji",
    "Zumper",
    "rhenti"
)

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
    // Cache expensive calculations
    val formattedTime = remember(thread.lastMessageTime) {
        formatTimestamp(thread.lastMessageTime)
    }
    val propertyAddress = remember { getPropertyAddress(thread) }
    val platformName = remember(thread.id) { getPlatformName(thread) }
    val badges = remember(thread.id) { getStatusBadges(thread) }

    // Cache all color scheme values to prevent recomposition
    val backgroundColor = MaterialTheme.colorScheme.background
    val onBackground = MaterialTheme.colorScheme.onBackground
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val outlineVariant = MaterialTheme.colorScheme.outlineVariant

    Column(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer { } // Enable hardware acceleration for smoother scrolling
            .clickable(onClick = onClick)
            .background(backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar (no badges for performance)
            ThreadAvatar(
                imageUrl = thread.imageUrl,
                displayName = thread.displayName
            )

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
                        color = onBackground
                    )

                    Text(
                        text = formattedTime,
                        style = MaterialTheme.typography.labelMedium.copy(fontSize = 14.sp),
                        color = onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Property address (without icon for performance)
                Text(
                    text = propertyAddress,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                    color = onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Last message
                Text(
                    text = thread.lastMessage ?: "No messages yet",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                    color = onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
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
                    tint = onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
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
            .border(2.dp, backgroundColor, CircleShape),
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
 * Optimized to use static platform list.
 */
private fun getPlatformName(thread: ChatThread): String {
    // TODO: Add platform/source field to ChatThread model
    // For now, assign based on thread ID for variety
    return PLATFORMS[thread.id.hashCode().mod(PLATFORMS.size)]
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
    val initials = remember(displayName) { getInitials(displayName) }

    Box(
        modifier = modifier
            .size(60.dp)
            .clip(CircleShape)
            .background(Color(0xFFE0E0E5)),  // Light gray background for initials
        contentAlignment = Alignment.Center
    ) {
        // Always show initials as background/fallback
        Text(
            text = initials,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp),
            color = Color(0xFF6B6B70),  // Dark gray text for initials
            fontWeight = FontWeight.SemiBold
        )

        // Images disabled for performance - initials only
        // if (!imageUrl.isNullOrBlank()) {
        //     AsyncImage(
        //         model = imageUrl,
        //         contentDescription = "Avatar",
        //         modifier = Modifier.fillMaxSize(),
        //         contentScale = ContentScale.Crop,
        //         placeholder = null,
        //         error = null,
        //         onError = { }
        //     )
        // }
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
 * Optimized to use cached formatters.
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
        diff < 604800_000 -> dayFormatter.format(Date(timestamp)) // Less than 7 days
        else -> dateFormatter.format(Date(timestamp)) // More than 7 days
    }
}

/**
 * Data class representing a status badge.
 */
private data class StatusBadge(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val backgroundColor: Color
)

/**
 * Get status badges for thread based on viewing/application status.
 * Returns list of badges to display on avatar.
 */
private fun getStatusBadges(thread: ChatThread): List<StatusBadge> {
    val badges = mutableListOf<StatusBadge>()

    // TODO: Once booking metadata is added to ChatThread, implement proper status checking
    // For now, we'll return mock badges based on thread ID for demonstration

    // Mock implementation - replace with actual metadata when available:
    // if (thread.viewingStatus == "pending") { ... }
    // if (thread.applicationStatus == "approved") { ... }

    val mockType = thread.id.hashCode().mod(5)
    when (mockType) {
        0 -> {
            // Pending Viewing
            badges.add(StatusBadge(
                icon = Icons.Filled.CalendarMonth,
                backgroundColor = Color(0xFFFF9500) // Orange
            ))
        }
        1 -> {
            // Approved Viewing
            badges.add(StatusBadge(
                icon = Icons.Filled.CalendarMonth,
                backgroundColor = Color(0xFF34C759) // Green
            ))
        }
        2 -> {
            // Pending Application
            badges.add(StatusBadge(
                icon = Icons.Filled.Description,
                backgroundColor = Color(0xFFFF9500) // Orange
            ))
        }
        3 -> {
            // Approved Application
            badges.add(StatusBadge(
                icon = Icons.Filled.Description,
                backgroundColor = Color(0xFF34C759) // Green
            ))
        }
        // 4 -> No badges
    }

    return badges
}
