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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Link message card types.
 */
enum class LinkMessageType {
    APPLICATION,
    VIEWING
}

/**
 * Link message card for "Apply to Listing" and "Book a Viewing" messages.
 * Matches iOS design with dark blue/purple background, icon, title, and address.
 * Right-aligned for owner messages, left-aligned for contact messages, with 280dp max width.
 */
@Composable
fun LinkMessageCard(
    type: LinkMessageType,
    propertyAddress: String,
    isFromOwner: Boolean = true,
    url: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (icon, iconColor, title, backgroundColor) = when (type) {
        LinkMessageType.APPLICATION -> {
            Quadruple(
                Icons.Filled.Description,
                Color(0xFF9B7FD9), // Purple
                if (isFromOwner) "Apply to Listing" else "Application Received",
                Color(0xFF3A2E59) // Purple-tinted background
            )
        }
        LinkMessageType.VIEWING -> {
            Quadruple(
                Icons.Filled.CalendarMonth,
                Color(0xFF5B9FFF), // Blue
                "Book a Viewing",
                Color(0xFF2E3A59) // Blue-tinted background
            )
        }
    }

    // Wrapper Row for alignment (right-aligned for owner, left-aligned for contact)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = if (isFromOwner) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(16.dp),
            color = backgroundColor,
            modifier = Modifier.widthIn(max = 280.dp) // Max width constraint
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left side: Icon + Content
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Main icon with colored circular background
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(
                                color = iconColor.copy(alpha = 0.2f),
                                shape = CircleShape
                            )
                            .border(
                                width = 1.dp,
                                color = iconColor.copy(alpha = 0.3f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = iconColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Title and address (subtitle replaced with address)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        // Title
                        Text(
                            text = title,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = Color.White
                        )

                        // Property address (replaces subtitle)
                        Text(
                            text = propertyAddress,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 13.sp
                            ),
                            color = Color(0xFF8E8E93),
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }

                // Right side: Arrow icon
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = Color(0xFF3E4D6B),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowForward,
                        contentDescription = "Open",
                        tint = Color(0xFF7B92B2),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

/**
 * Helper data class for multiple return values.
 */
private data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)

