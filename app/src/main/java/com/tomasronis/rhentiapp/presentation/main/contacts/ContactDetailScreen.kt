package com.tomasronis.rhentiapp.presentation.main.contacts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.tomasronis.rhentiapp.data.contacts.models.Contact
import com.tomasronis.rhentiapp.data.contacts.models.ContactProperty
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
        topBar = {
            TopAppBar(
                title = { Text("Contact Details") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
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
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header with avatar and name
        item {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Avatar
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    if (contact.avatarUrl != null) {
                        AsyncImage(
                            model = contact.avatarUrl,
                            contentDescription = "Avatar",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Text(
                            text = getInitials(contact.displayName),
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = contact.displayName,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )

                if (profile?.role != null) {
                    Text(
                        text = profile.role.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Action buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onStartChat,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Chat, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Message")
                }

                OutlinedButton(
                    onClick = onCall,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Phone, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Call")
                }
            }
        }

        // Contact information
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Contact Information",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (contact.email != null) {
                        ContactInfoRow(
                            icon = Icons.Filled.Email,
                            label = "Email",
                            value = contact.email
                        )
                    }

                    if (contact.phone != null) {
                        if (contact.email != null) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        ContactInfoRow(
                            icon = Icons.Filled.Phone,
                            label = "Phone",
                            value = contact.phone
                        )
                    }
                }
            }
        }

        // Activity stats
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Activity",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem(
                            icon = Icons.Filled.Chat,
                            label = "Messages",
                            value = contact.totalMessages.toString()
                        )

                        StatItem(
                            icon = Icons.Filled.Phone,
                            label = "Calls",
                            value = contact.totalCalls.toString()
                        )

                        if (contact.lastActivity != null) {
                            StatItem(
                                icon = Icons.Filled.History,
                                label = "Last Activity",
                                value = formatDate(contact.lastActivity)
                            )
                        }
                    }
                }
            }
        }

        // Properties
        if (profile != null && profile.properties.isNotEmpty()) {
            item {
                Text(
                    text = "Properties",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            items(
                items = profile.properties,
                key = { it.id }
            ) { property ->
                PropertyCard(property = property)
            }
        }

        // Notes
        if (profile?.notes != null) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Notes",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = profile.notes,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Contact information row.
 */
@Composable
private fun ContactInfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/**
 * Stat item for activity section.
 */
@Composable
private fun StatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Property card component.
 */
@Composable
private fun PropertyCard(
    property: ContactProperty
) {
    Card(
        modifier = Modifier.fillMaxWidth()
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
                tint = MaterialTheme.colorScheme.primary
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = property.address,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )

                if (property.unit != null) {
                    Text(
                        text = "Unit ${property.unit}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = property.role.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
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
