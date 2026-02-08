package com.tomasronis.rhentiapp.presentation.calls.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.tomasronis.rhentiapp.data.calls.models.CallLog
import com.tomasronis.rhentiapp.data.calls.models.CallType
import com.tomasronis.rhentiapp.presentation.theme.Success
import com.tomasronis.rhentiapp.presentation.theme.AccentBlue
import java.text.SimpleDateFormat
import java.util.*

/**
 * Call log card showing call details - iOS style.
 */
@Composable
fun CallLogCard(
    callLog: CallLog,
    onClick: () -> Unit,
    onCallClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Avatar or call type icon
                Box(
                    modifier = Modifier.size(56.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (!callLog.contactAvatar.isNullOrBlank()) {
                        // Show contact avatar
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            // Fallback icon shown behind the image
                            Icon(
                                imageVector = Icons.Filled.Person,
                                contentDescription = null,
                                modifier = Modifier.size(28.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            AsyncImage(
                                model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                                    .data(callLog.contactAvatar)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Contact avatar",
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape),
                                contentScale = ContentScale.Crop
                            )
                        }

                        // Call type badge indicator (small icon overlay at bottom-right)
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.background),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (callLog.callType) {
                                    CallType.INCOMING -> Icons.Filled.CallReceived
                                    CallType.OUTGOING -> Icons.Filled.CallMade
                                    CallType.MISSED -> Icons.Filled.CallMissed
                                },
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = when (callLog.callType) {
                                    CallType.INCOMING -> Success
                                    CallType.OUTGOING -> AccentBlue
                                    CallType.MISSED -> MaterialTheme.colorScheme.error
                                }
                            )
                        }
                    } else if (!callLog.contactName.isNullOrBlank()) {
                        // Show initials avatar
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = getInitials(callLog.contactName),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        // Call type badge indicator
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.background),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = when (callLog.callType) {
                                    CallType.INCOMING -> Icons.Filled.CallReceived
                                    CallType.OUTGOING -> Icons.Filled.CallMade
                                    CallType.MISSED -> Icons.Filled.CallMissed
                                },
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = when (callLog.callType) {
                                    CallType.INCOMING -> Success
                                    CallType.OUTGOING -> AccentBlue
                                    CallType.MISSED -> MaterialTheme.colorScheme.error
                                }
                            )
                        }
                    } else {
                        // No avatar or name - show call type icon only (original behavior)
                        Icon(
                            imageVector = when (callLog.callType) {
                                CallType.INCOMING -> Icons.Filled.CallReceived
                                CallType.OUTGOING -> Icons.Filled.CallMade
                                CallType.MISSED -> Icons.Filled.CallMissed
                            },
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = when (callLog.callType) {
                                CallType.INCOMING -> Success
                                CallType.OUTGOING -> AccentBlue
                                CallType.MISSED -> MaterialTheme.colorScheme.error
                            }
                        )
                    }
                }

                // Name and time
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    // Display name if available, otherwise phone number, otherwise "Unknown"
                    val displayName = when {
                        !callLog.contactName.isNullOrBlank() -> callLog.contactName
                        callLog.contactPhone.isNotBlank() -> callLog.contactPhone
                        else -> "Unknown"
                    }

                    Text(
                        text = displayName,
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 17.sp),
                        fontWeight = FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    // Show phone number as subtitle if we're showing the name
                    if (!callLog.contactName.isNullOrBlank() && callLog.contactPhone.isNotBlank()) {
                        Text(
                            text = callLog.contactPhone,
                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    Text(
                        text = formatCallTime(callLog.timestamp),
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Duration
                Text(
                    text = if (callLog.duration > 0) formatDuration(callLog.duration) else "0:00",
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Chevron
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = "View details",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Divider at bottom
            HorizontalDivider(
                modifier = Modifier.padding(start = 84.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )
        }
    }
}

/**
 * Get initials from name
 */
private fun getInitials(name: String): String {
    val parts = name.trim().split(" ")
    return when {
        parts.size >= 2 -> "${parts[0].firstOrNull()?.uppercase() ?: ""}${parts[1].firstOrNull()?.uppercase() ?: ""}"
        parts.isNotEmpty() -> parts[0].take(2).uppercase()
        else -> ""
    }
}

/**
 * Format call timestamp - iOS style (just time)
 */
private fun formatCallTime(timestamp: Long): String {
    val format = SimpleDateFormat("h:mm a", Locale.getDefault())
    return format.format(Date(timestamp)).replace("AM", "AM").replace("PM", "PM")
}

/**
 * Format call duration in seconds to mm:ss
 */
private fun formatDuration(seconds: Int): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", minutes, secs)
}
