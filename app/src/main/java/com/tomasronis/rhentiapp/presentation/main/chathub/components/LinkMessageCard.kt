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
 * Matches iOS design with dark blue/purple background, icon, title, subtitle, and address.
 */
@Composable
fun LinkMessageCard(
    type: LinkMessageType,
    propertyAddress: String,
    url: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val (icon, iconColor, title, subtitle, addressIcon) = when (type) {
        LinkMessageType.APPLICATION -> {
            Tuple5(
                Icons.Filled.Description,
                Color(0xFF9B7FD9), // Purple
                "Apply to Listing",
                "Submit your application",
                Icons.Filled.Apartment
            )
        }
        LinkMessageType.VIEWING -> {
            Tuple5(
                Icons.Filled.CalendarMonth,
                Color(0xFF5B9FFF), // Blue
                "Book a Viewing",
                "Schedule your visit",
                Icons.Filled.Info
            )
        }
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = Color(0xFF2E3A59), // Dark blue-gray background
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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

                // Title, subtitle, and address
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

                    // Subtitle
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontSize = 14.sp
                        ),
                        color = Color(0xFF8E8E93)
                    )

                    // Address with icon
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Icon(
                            imageVector = addressIcon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = propertyAddress,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 13.sp
                            ),
                            color = Color(0xFFB8B8B8)
                        )
                    }
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

/**
 * Helper data class for multiple return values.
 */
private data class Tuple5<A, B, C, D, E>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E
)
