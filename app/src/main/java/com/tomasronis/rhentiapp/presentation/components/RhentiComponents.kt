package com.tomasronis.rhentiapp.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tomasronis.rhentiapp.presentation.theme.*

/**
 * Rhenti-styled card with dark/light theme support and rounded corners
 */
@Composable
fun RhentiCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val backgroundColor = if (isSystemInDarkTheme()) DarkSurface else LightSurface
    val borderColor = if (isSystemInDarkTheme()) DarkOutline else LightOutline

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = backgroundColor
            ),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, borderColor)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                content = content
            )
        }
    } else {
        Card(
            modifier = modifier,
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = backgroundColor
            ),
            border = androidx.compose.foundation.BorderStroke(0.5.dp, borderColor)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                content = content
            )
        }
    }
}

/**
 * Rhenti-styled primary button with coral color and rounded shape
 */
@Composable
fun RhentiButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .height(52.dp)
            .fillMaxWidth(),
        enabled = enabled,
        shape = RoundedCornerShape(26.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = RhentiCoral,
            contentColor = Color.White,
            disabledContainerColor = RhentiCoral.copy(alpha = 0.5f),
            disabledContentColor = Color.White.copy(alpha = 0.5f)
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp
        )
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Rhenti-styled search bar with rounded corners
 */
@Composable
fun RhentiSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Search"
) {
    val backgroundColor = if (isSystemInDarkTheme()) {
        DarkSurfaceVariant
    } else {
        Color(0xFFF2F2F7)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp),
        shape = RoundedCornerShape(12.dp),
        color = backgroundColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = "Search",
                tint = TextSecondary,
                modifier = Modifier.size(20.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                textStyle = TextStyle(
                    color = if (isSystemInDarkTheme()) DarkOnSurface else LightOnSurface,
                    fontSize = 16.sp
                ),
                singleLine = true,
                decorationBox = { innerTextField ->
                    if (query.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyLarge,
                            color = TextSecondary
                        )
                    }
                    innerTextField()
                }
            )
        }
    }
}

/**
 * Rhenti-styled chip/tag with various colors
 */
@Composable
fun RhentiChip(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = ChipBlue
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelMedium,
            color = Color.White,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Rhenti-styled unread count badge
 */
@Composable
fun RhentiUnreadBadge(
    count: Int,
    modifier: Modifier = Modifier
) {
    if (count > 0) {
        Surface(
            modifier = modifier.size(24.dp),
            shape = CircleShape,
            color = UnreadBadge
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (count > 99) "99+" else count.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp
                )
            }
        }
    }
}

/**
 * Profile avatar with initials fallback
 */
@Composable
fun ProfileAvatar(
    name: String,
    imageUrl: String? = null,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    showStatus: Boolean = false,
    isOnline: Boolean = false
) {
    Box(modifier = modifier) {
        // Avatar circle
        Surface(
            modifier = Modifier.size(size),
            shape = CircleShape,
            color = if (isSystemInDarkTheme()) DarkSurfaceVariant else Color(0xFFE5E5EA)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                // For now, just show initials (image loading can be added later)
                Text(
                    text = getInitials(name),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isSystemInDarkTheme()) DarkOnSurface else LightOnSurface,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Status indicator
        if (showStatus) {
            Surface(
                modifier = Modifier
                    .size(14.dp)
                    .align(Alignment.BottomEnd),
                shape = CircleShape,
                color = if (isOnline) Success else TextSecondary,
                border = androidx.compose.foundation.BorderStroke(
                    2.dp,
                    if (isSystemInDarkTheme()) DarkBackground else LightBackground
                )
            ) {}
        }
    }
}

/**
 * Get initials from a name
 */
private fun getInitials(name: String): String {
    val parts = name.trim().split(" ")
    return when {
        parts.size >= 2 -> "${parts[0].firstOrNull()?.uppercase() ?: ""}${parts[1].firstOrNull()?.uppercase() ?: ""}"
        parts.isNotEmpty() -> parts[0].take(2).uppercase()
        else -> "?"
    }
}
