package com.tomasronis.rhentiapp.presentation.main.contacts

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.tomasronis.rhentiapp.data.contacts.models.Contact
import com.tomasronis.rhentiapp.data.contacts.models.ContactProperty
import com.tomasronis.rhentiapp.presentation.theme.RhentiCoral
import java.text.SimpleDateFormat
import java.util.*

/**
 * Contact detail screen showing full profile information.
 * Includes action buttons for chat and call.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactDetailScreen(
    contact: Contact,
    onNavigateBack: () -> Unit,
    onStartChat: (Contact) -> Unit,
    onCall: (Contact) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ContactsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Select contact when screen opens
    LaunchedEffect(contact.id) {
        viewModel.selectContact(contact)
    }

    // Clear contact when screen closes
    DisposableEffect(Unit) {
        onDispose {
            viewModel.clearSelectedContact()
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            // iOS-style header with circular back button and centered title
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular back button - iOS style (dark circle in light mode)
                val isDarkTheme = isSystemInDarkTheme()
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = if (isDarkTheme) MaterialTheme.colorScheme.surfaceVariant else Color(0xFF2C2C2E),
                            shape = CircleShape
                        )
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { onNavigateBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = if (isDarkTheme) MaterialTheme.colorScheme.onSurfaceVariant else Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Center title
                Text(
                    text = "Contact Details",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                // Spacer for balance
                Spacer(modifier = Modifier.size(40.dp))
            }
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading && uiState.contactProfile == null -> {
                    // Loading profile
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.contactProfile != null -> {
                    // Show profile
                    ContactDetailContent(
                        contact = contact,
                        profile = uiState.contactProfile!!,
                        onStartChat = { onStartChat(contact) },
                        onCall = { onCall(contact) }
                    )
                }
                else -> {
                    // Show basic contact info while loading
                    ContactDetailContent(
                        contact = contact,
                        profile = null,
                        onStartChat = { onStartChat(contact) },
                        onCall = { onCall(contact) }
                    )
                }
            }

            // Error snackbar
            if (uiState.error != null) {
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(uiState.error ?: "An error occurred")
                }
            }
        }
    }
}

/**
 * Contact detail content with profile information.
 */
@Composable
private fun ContactDetailContent(
    contact: Contact,
    profile: com.tomasronis.rhentiapp.data.contacts.models.ContactProfile?,
    onStartChat: () -> Unit,
    onCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    var viewingsExpanded by remember { mutableStateOf(false) }
    var applicationsExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with avatar and name
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                // Avatar - iOS style larger avatar
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    // Always show initials as background/fallback
                    Text(
                        text = getInitials(contact.displayName),
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontSize = 36.sp
                        ),
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Bold
                    )

                    // Show image if available (overlays initials)
                    if (!contact.avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = contact.avatarUrl,
                            contentDescription = "Avatar",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Name
                Text(
                    text = contact.displayName,
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Role badge
                if (profile?.role != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF007AFF).copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = profile.role.replaceFirstChar { it.uppercase() },
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 13.sp
                            ),
                            color = Color(0xFF007AFF),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // Action buttons - iOS style
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Message button
                Button(
                    onClick = onStartChat,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE8998D) // Rhenti coral
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Filled.Chat,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Message",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                // Call button
                Button(
                    onClick = onCall,
                    enabled = contact.phone != null,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .alpha(if (contact.phone != null) 1f else 0.5f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (contact.phone != null) Color(0xFF34C759) else MaterialTheme.colorScheme.surfaceVariant,
                        disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        Icons.Filled.Phone,
                        contentDescription = if (contact.phone != null) "Call" else "No phone number",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "Call",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }

        // Contact information card
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surface
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Contact Information",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (contact.email != null) {
                        ContactInfoRow(
                            icon = Icons.Filled.Email,
                            label = "Email",
                            value = contact.email
                        )
                    }

                    if (contact.phone != null) {
                        if (contact.email != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        ContactInfoRow(
                            icon = Icons.Filled.Phone,
                            label = "Phone",
                            value = contact.phone
                        )
                    }

                    // Channel information
                    if (contact.channel != null) {
                        if (contact.email != null || contact.phone != null) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        ContactInfoRow(
                            icon = Icons.Filled.Tag,
                            label = "Channel",
                            value = getPlatformName(contact.channel)
                        )
                    }
                }
            }
        }

        // Viewings expandable section
        item {
            ExpandableSection(
                title = "Viewings",
                icon = Icons.Filled.CalendarMonth,
                count = 3, // TODO: Get from API
                expanded = viewingsExpanded,
                onToggle = { viewingsExpanded = !viewingsExpanded }
            ) {
                // Viewing items (placeholder)
                ViewingItem(
                    propertyAddress = "123 Main St, Unit 5",
                    dateTime = "Feb 10, 2026 at 2:00 PM",
                    status = "Confirmed"
                )
                Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
                ViewingItem(
                    propertyAddress = "456 Oak Ave, Unit 12",
                    dateTime = "Feb 12, 2026 at 10:00 AM",
                    status = "Pending"
                )
                Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
                ViewingItem(
                    propertyAddress = "789 Elm St, Unit 3",
                    dateTime = "Feb 8, 2026 at 3:30 PM",
                    status = "Completed"
                )
            }
        }

        // Applications expandable section
        item {
            ExpandableSection(
                title = "Applications",
                icon = Icons.Filled.Description,
                count = 2, // TODO: Get from API
                expanded = applicationsExpanded,
                onToggle = { applicationsExpanded = !applicationsExpanded }
            ) {
                // Application items (placeholder)
                ApplicationItem(
                    propertyAddress = "123 Main St, Unit 5",
                    submittedDate = "Feb 1, 2026",
                    status = "Under Review"
                )
                Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 8.dp))
                ApplicationItem(
                    propertyAddress = "456 Oak Ave, Unit 12",
                    submittedDate = "Jan 28, 2026",
                    status = "Approved"
                )
            }
        }

        // Properties - iOS dark cards
        if (profile != null && profile.properties.isNotEmpty()) {
            item {
                Text(
                    text = "Properties",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = Color(0xFF8E8E93),
                    modifier = Modifier.padding(start = 4.dp)
                )
            }

            items(
                items = profile.properties,
                key = { it.id }
            ) { property ->
                PropertyCard(property = property)
            }
        }

        // Notes - iOS dark card
        if (profile?.notes != null) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFF1C1C1E)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Notes",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = Color.White
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = profile.notes,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontSize = 15.sp
                            ),
                            color = Color(0xFF8E8E93)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Contact information row - iOS dark style.
 */
@Composable
private fun ContactInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = RhentiCoral,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 15.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 15.sp
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Expandable section component - iOS dark style.
 */
@Composable
private fun ExpandableSection(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    count: Int,
    expanded: Boolean,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val rotationAngle by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        label = "chevron rotation"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onToggle() }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = RhentiCoral,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    // Count badge
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFF3A3A3C)
                    ) {
                        Text(
                            text = count.toString(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 12.sp
                            ),
                            color = Color(0xFF8E8E93),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Filled.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = Color(0xFF8E8E93),
                    modifier = Modifier
                        .size(24.dp)
                        .rotate(rotationAngle)
                )
            }

            // Expanded content
            if (expanded) {
                Divider(color = MaterialTheme.colorScheme.outlineVariant, thickness = 0.5.dp)
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    content()
                }
            }
        }
    }
}

/**
 * Viewing item component.
 */
@Composable
private fun ViewingItem(
    propertyAddress: String,
    dateTime: String,
    status: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = propertyAddress,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            ),
            color = Color.White
        )
        Text(
            text = dateTime,
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 13.sp
            ),
            color = Color(0xFF8E8E93)
        )
        // Status badge
        val statusColor = when (status) {
            "Confirmed" -> Color(0xFF34C759)
            "Pending" -> Color(0xFFFF9500)
            "Completed" -> Color(0xFF007AFF)
            else -> Color(0xFF8E8E93)
        }
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = statusColor.copy(alpha = 0.2f)
        ) {
            Text(
                text = status,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 12.sp
                ),
                color = statusColor,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

/**
 * Application item component.
 */
@Composable
private fun ApplicationItem(
    propertyAddress: String,
    submittedDate: String,
    status: String
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = propertyAddress,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium
            ),
            color = Color.White
        )
        Text(
            text = "Submitted $submittedDate",
            style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 13.sp
            ),
            color = Color(0xFF8E8E93)
        )
        // Status badge
        val statusColor = when (status) {
            "Approved" -> Color(0xFF34C759)
            "Under Review" -> Color(0xFFFF9500)
            "Rejected" -> Color(0xFFFF3B30)
            else -> Color(0xFF8E8E93)
        }
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = statusColor.copy(alpha = 0.2f)
        ) {
            Text(
                text = status,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 12.sp
                ),
                color = statusColor,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

/**
 * Property card component - iOS dark style.
 */
@Composable
private fun PropertyCard(
    property: ContactProperty
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Home,
                contentDescription = null,
                tint = RhentiCoral,
                modifier = Modifier.size(20.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = property.address,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = Color.White
                )

                if (property.unit != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Unit ${property.unit}",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontSize = 13.sp
                        ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF007AFF).copy(alpha = 0.2f)
                ) {
                    Text(
                        text = property.role.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontSize = 12.sp
                        ),
                        color = Color(0xFF007AFF),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * Get initials from display name.
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
 * Format date for display.
 */
private fun formatDate(timestamp: Long): String {
    val format = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
    return format.format(Date(timestamp))
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
