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
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.size.Scale
import com.tomasronis.rhentiapp.data.chathub.models.ChatThread
import com.tomasronis.rhentiapp.presentation.theme.*
import java.text.SimpleDateFormat
import java.util.*

// Static formatters to avoid recreation (thread-safe in modern Android)
private val dayFormatter = SimpleDateFormat("EEEE", Locale.getDefault())
private val dateFormatter = SimpleDateFormat("MMM d", Locale.getDefault())

/**
 * Card component for displaying a chat thread in the list.
 * Matches iOS design with address, snippet, and status indicators.
 * Optimized for smooth scrolling performance.
 */
@Composable
fun ThreadCard(
    thread: ChatThread,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // CRITICAL: Pre-compute ALL expensive operations outside composition
    val initials = remember(thread.displayName) { getInitials(thread.displayName) }
    val propertyAddress = remember(thread.id) { getPropertyAddress(thread) }
    val messageSnippet = remember(thread.lastMessage) {
        thread.lastMessage?.take(50) ?: "No messages yet"
    }
    val timestamp = remember(thread.lastMessageTime) { formatTimestamp(thread.lastMessageTime) }
    val statusBadges = remember(thread.id) { getStatusBadges(thread) }
    val platformName = remember(thread.id) { getPlatformName(thread) }
    val unreadBadgeText = remember(thread.unreadCount) {
        if (thread.unreadCount > 9) "9+" else thread.unreadCount.toString()
    }

    // CRITICAL: Pre-compute colors to avoid .copy() allocations on every recomposition
    val iconTint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    val addressColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
    val snippetColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    val timestampColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    val chevronTint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
    val dividerColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)

    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp) // CRITICAL: Fixed height for smooth scrolling (tall enough for all content + platform tag)
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically // Center chevron vertically
        ) {
        // Avatar with unread indicator ring and status badges
        Box {
            AvatarWithIndicator(
                imageUrl = thread.imageUrl,
                initials = initials,
                hasUnread = thread.unreadCount > 0
            )

            // Status badges (viewing/application indicators)
            if (statusBadges.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .offset(x = 4.dp, y = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    statusBadges.forEach { badge ->
                        SmallBadgeIcon(
                            icon = badge.icon,
                            backgroundColor = badge.backgroundColor
                        )
                    }
                }
            }
        }

        // Content column (name, address, snippet)
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp) // Increased spacing for better readability
        ) {
            // Name
            Text(
                text = thread.displayName,
                style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp),
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Property address with location icon
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    tint = iconTint, // Pre-computed
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = propertyAddress,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                    color = addressColor, // Pre-computed
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Message snippet
            Text(
                text = messageSnippet,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                color = snippetColor, // Pre-computed
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            // Source channel tag
            PlatformTag(platform = platformName)
        }

        // Right side (timestamp and badge)
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Timestamp
            Text(
                text = timestamp,
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                color = timestampColor // Pre-computed
            )

            Spacer(modifier = Modifier.weight(1f))

            // Unread badge
            if (thread.unreadCount > 0) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1C2B3A)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = unreadBadgeText, // Pre-computed
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Chevron - vertically centered
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = "View conversation",
            tint = chevronTint, // Pre-computed
            modifier = Modifier.size(20.dp)
        )
    }

        // Divider
        HorizontalDivider(
            modifier = Modifier.padding(start = 80.dp),
            color = dividerColor, // Pre-computed
            thickness = 0.5.dp
        )
    }
}

/**
 * Avatar with coral/pink ring indicator for unread messages.
 * CRITICAL: Uses FIXED sizing for smooth scrolling performance.
 */
@Composable
private fun AvatarWithIndicator(
    imageUrl: String?,
    initials: String,
    hasUnread: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.size(56.dp), // FIXED size
        contentAlignment = Alignment.Center
    ) {
        // Avatar circle - ALWAYS 60dp (fixed size)
        Box(
            modifier = Modifier
                .size(56.dp) // FIXED size - never changes
                .clip(CircleShape)
                .background(Color(0xFFE0E0E5)),
            contentAlignment = Alignment.Center
        ) {
            // Always show initials as fallback
            Text(
                text = initials,
                color = Color(0xFF6B6B70),
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold
            )

            // Load image if available - FIXED decode size for performance
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                        .data(imageUrl)
                        .crossfade(false) // No animation for instant display
                        .size(120) // FIXED size - always 60dp * 2
                        .scale(Scale.FILL)
                        // Hardware bitmaps are FASTER - don't disable them!
                        // Memory and disk caching enabled by default in Coil
                        .build(),
                    contentDescription = "Avatar",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }

        // Coral/pink ring overlay for unread (drawn on top, doesn't affect sizing)
        if (hasUnread) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .border(3.dp, RhentiCoral, CircleShape)
            )
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
 * Simplified platform tag for better scroll performance.
 * Uses Box instead of Surface to reduce composition overhead.
 */
@Composable
private fun PlatformTag(
    platform: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(AccentBlue)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = platform,
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp),
            fontWeight = FontWeight.Medium,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Get property address from thread data.
 */
private fun getPropertyAddress(thread: ChatThread): String {
    return thread.address ?: "Address not available"
}

/**
 * Get platform name from thread data.
 * Maps channel names to display names.
 */
private fun getPlatformName(thread: ChatThread): String {
    return when (thread.channel?.lowercase()) {
        "facebook" -> "Facebook"
        "kijiji" -> "Kijiji"
        "zumper" -> "Zumper"
        "rhenti" -> "rhenti"
        "facebook-listing-page", "facebook_listing_page" -> "Rhenti-powered listing pages"
        else -> thread.channel?.replaceFirstChar { it.uppercase() } ?: "Rhenti"
    }
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
    val context = androidx.compose.ui.platform.LocalContext.current

    Box(
        modifier = modifier
            .size(56.dp)
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

        // Images disabled - even with optimizations they cause scroll jank
        // Avatars show colored initials only for best performance
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
 *
 * Orange = Pending, Green = Confirmed/Approved
 * Calendar icon = Viewing/Booking, Document icon = Application
 */
private fun getStatusBadges(thread: ChatThread): List<StatusBadge> {
    val badges = mutableListOf<StatusBadge>()

    // Booking/Viewing status badge
    when (thread.bookingStatus?.lowercase()) {
        "pending" -> {
            badges.add(StatusBadge(
                icon = Icons.Filled.CalendarMonth,
                backgroundColor = Color(0xFFFF9500) // Orange for pending
            ))
        }
        "confirmed", "approved" -> {
            badges.add(StatusBadge(
                icon = Icons.Filled.CalendarMonth,
                backgroundColor = Color(0xFF34C759) // Green for confirmed/approved
            ))
        }
    }

    // Application status badge
    when (thread.applicationStatus?.lowercase()) {
        "pending" -> {
            badges.add(StatusBadge(
                icon = Icons.Filled.Description,
                backgroundColor = Color(0xFFFF9500) // Orange for pending
            ))
        }
        "approved" -> {
            badges.add(StatusBadge(
                icon = Icons.Filled.Description,
                backgroundColor = Color(0xFF34C759) // Green for approved
            ))
        }
    }

    return badges
}
