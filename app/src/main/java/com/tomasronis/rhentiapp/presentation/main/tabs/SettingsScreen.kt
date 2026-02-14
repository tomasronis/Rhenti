package com.tomasronis.rhentiapp.presentation.main.tabs

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.tomasronis.rhentiapp.core.preferences.MediaRetentionPeriod
import com.tomasronis.rhentiapp.core.preferences.ThemeMode
import com.tomasronis.rhentiapp.presentation.theme.*
import kotlinx.coroutines.launch

/**
 * Settings screen matching iOS design.
 * Features profile card, theme selector, storage settings, and more.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val selectedThemeMode by viewModel.selectedThemeMode.collectAsState()
    val mediaRetentionPeriod by viewModel.mediaRetentionPeriod.collectAsState()
    val messagesPerChat by viewModel.messagesPerChat.collectAsState()
    val twilioStatus by viewModel.twilioRegistrationStatus.collectAsState()
    val canUseFullScreenIntent by viewModel.canUseFullScreenIntent.collectAsState()
    val canDrawOverlays by viewModel.canDrawOverlays.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Refresh permissions when returning from system settings
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.refreshFullScreenIntentPermission()
                viewModel.refreshOverlayPermission()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var showUserDetailsDialog by remember { mutableStateOf(false) }
    var showClearCacheDialog by remember { mutableStateOf(false) }
    var showMediaRetentionDialog by remember { mutableStateOf(false) }
    var showMessagesPerChatDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            // Match the header style of Messages, Contacts, Calls
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(top = 4.dp) // Add slight top padding to match Messages header position
            ) {
                // Header row with title (matching other tabs)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Large title
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(MaterialTheme.colorScheme.background),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Profile Card
            item {
                ProfileCard(
                    name = uiState.userName,
                    email = uiState.userEmail,
                    avatarUrl = null,
                    onClick = { showUserDetailsDialog = true }
                )
            }

            // Preferences Section
            item {
                SectionHeader(title = "Preferences")
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DarkMode,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Theme",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Theme mode selector (Dark, Light, System)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ThemeModeOption(
                                name = "Dark",
                                icon = Icons.Filled.DarkMode,
                                isSelected = selectedThemeMode == ThemeMode.DARK,
                                onClick = { viewModel.setThemeMode(ThemeMode.DARK) }
                            )

                            ThemeModeOption(
                                name = "Light",
                                icon = Icons.Filled.LightMode,
                                isSelected = selectedThemeMode == ThemeMode.LIGHT,
                                onClick = { viewModel.setThemeMode(ThemeMode.LIGHT) }
                            )

                            ThemeModeOption(
                                name = "System",
                                icon = Icons.Filled.SettingsBrightness,
                                isSelected = selectedThemeMode == ThemeMode.SYSTEM,
                                onClick = { viewModel.setThemeMode(ThemeMode.SYSTEM) }
                            )
                        }
                    }
                }
            }

            // Storage Section
            item {
                SectionHeader(title = "Storage")
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column {
                        SettingsItem(
                            icon = Icons.Filled.Image,
                            iconTint = MaterialTheme.colorScheme.primary,
                            title = "Keep Media",
                            value = viewModel.getMediaRetentionLabel(mediaRetentionPeriod),
                            onClick = { showMediaRetentionDialog = true }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 52.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )

                        SettingsItem(
                            icon = Icons.Filled.ChatBubble,
                            iconTint = MaterialTheme.colorScheme.primary,
                            title = "Messages per Chat",
                            value = viewModel.getMessagesPerChatLabel(messagesPerChat),
                            onClick = { showMessagesPerChatDialog = true }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 52.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )

                        SettingsItem(
                            icon = Icons.Filled.Sd,
                            iconTint = MaterialTheme.colorScheme.primary,
                            title = "Storage Used",
                            value = uiState.storageUsed,
                            onClick = { /* Future: Show storage details */ },
                            showChevron = false
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 52.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )

                        SettingsItem(
                            icon = Icons.Filled.Delete,
                            iconTint = MaterialTheme.colorScheme.primary,
                            title = "Clear Cache",
                            value = uiState.cacheSize,
                            onClick = { showClearCacheDialog = true },
                            showChevron = false
                        )
                    }
                }
            }

            // About Section
            item {
                SectionHeader(title = "About")
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column {
                        SettingsItem(
                            icon = Icons.Filled.Info,
                            iconTint = MaterialTheme.colorScheme.primary,
                            title = "Version",
                            value = "2.0.0 (22)",
                            onClick = { /* Future: Show version details */ },
                            showChevron = false
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 52.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )

                        SettingsItem(
                            icon = Icons.Filled.Smartphone,
                            iconTint = MaterialTheme.colorScheme.primary,
                            title = "Device ID",
                            value = viewModel.deviceId,
                            onClick = { /* Info only */ },
                            showChevron = false
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 52.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )

                        SettingsItem(
                            icon = Icons.Filled.Phone,
                            iconTint = if (twilioStatus.startsWith("REGISTERED")) Color(0xFF34C759) else Color(0xFFFF3B30),
                            title = "VoIP Status",
                            value = if (twilioStatus.startsWith("REGISTERED")) "Ready" else "Not Ready",
                            onClick = { /* Info only */ },
                            showChevron = false
                        )

                        // Full-screen intent permission (Android 14+)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                            HorizontalDivider(
                                modifier = Modifier.padding(start = 52.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )

                            SettingsItem(
                                icon = Icons.Filled.Fullscreen,
                                iconTint = if (canUseFullScreenIntent) Color(0xFF34C759) else Color(0xFFFF3B30),
                                title = "Full-Screen Calls",
                                value = if (canUseFullScreenIntent) "Enabled" else "DISABLED - Tap to enable",
                                onClick = {
                                    if (!canUseFullScreenIntent) {
                                        try {
                                            val intent = Intent(
                                                android.provider.Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT,
                                                Uri.parse("package:${context.packageName}")
                                            )
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            // Fallback to app notification settings
                                            val intent = Intent(
                                                android.provider.Settings.ACTION_APP_NOTIFICATION_SETTINGS
                                            ).apply {
                                                putExtra(android.provider.Settings.EXTRA_APP_PACKAGE, context.packageName)
                                            }
                                            context.startActivity(intent)
                                        }
                                    }
                                    viewModel.refreshFullScreenIntentPermission()
                                },
                                showChevron = !canUseFullScreenIntent
                            )
                        }

                        // Display over other apps permission (needed for screen-on incoming calls)
                        HorizontalDivider(
                            modifier = Modifier.padding(start = 52.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )

                        SettingsItem(
                            icon = Icons.Filled.PhoneInTalk,
                            iconTint = if (canDrawOverlays) Color(0xFF34C759) else Color(0xFFFF3B30),
                            title = "Display Over Apps",
                            value = if (canDrawOverlays) "Enabled" else "DISABLED - Tap to enable",
                            onClick = {
                                if (!canDrawOverlays) {
                                    try {
                                        val intent = Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:${context.packageName}")
                                        )
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        // Fallback to general app settings
                                        val intent = Intent(
                                            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                            Uri.parse("package:${context.packageName}")
                                        )
                                        context.startActivity(intent)
                                    }
                                }
                                viewModel.refreshOverlayPermission()
                            },
                            showChevron = !canDrawOverlays
                        )
                    }
                }
            }

            // Help & Legal Section
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column {
                        SettingsItem(
                            icon = Icons.Filled.Help,
                            iconTint = MaterialTheme.colorScheme.primary,
                            title = "Help & Support",
                            onClick = {
                                // Open Notion help center in browser
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://rhenti.notion.site/Rhenti-Help-Center-5f98e36f7f0e4117b4f83bb816ab9f48"))
                                context.startActivity(intent)
                            }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 52.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )

                        SettingsItem(
                            icon = Icons.Filled.Shield,
                            iconTint = MaterialTheme.colorScheme.primary,
                            title = "Privacy Policy",
                            onClick = {
                                // Open privacy policy in browser
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://rhenti.com/privacy-policy.html"))
                                context.startActivity(intent)
                            }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 52.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )

                        SettingsItem(
                            icon = Icons.Filled.Description,
                            iconTint = MaterialTheme.colorScheme.primary,
                            title = "Terms of Service",
                            onClick = {
                                // Open terms in browser
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://rhenti.com/terms-of-service.html"))
                                context.startActivity(intent)
                            }
                        )
                    }
                }
            }

            // Sign Out Button
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    modifier = Modifier.clickable {
                        coroutineScope.launch {
                            viewModel.signOut()
                            onLogout()
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Logout,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Sign Out",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Footer
            item {
                Text(
                    text = "Made with ❤️ in Canada",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                )
            }
        }
    }

    // User Details Dialog
    if (showUserDetailsDialog) {
        UserDetailsDialog(
            name = uiState.userName,
            email = uiState.userEmail,
            phone = uiState.userPhone.ifEmpty { "Not set" },
            organization = uiState.userOrganization,
            onDismiss = { showUserDetailsDialog = false }
        )
    }

    // Clear Cache Confirmation Dialog
    if (showClearCacheDialog) {
        AlertDialog(
            onDismissRequest = { showClearCacheDialog = false },
            title = { Text("Clear Cache?") },
            text = { Text("This will clear ${uiState.cacheSize} of cached data. The app may need to reload some content.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearCache()
                        showClearCacheDialog = false
                    }
                ) {
                    Text("Clear")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearCacheDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Media Retention Dialog
    if (showMediaRetentionDialog) {
        MediaRetentionDialog(
            currentPeriod = mediaRetentionPeriod,
            onPeriodSelected = { period ->
                viewModel.setMediaRetentionPeriod(period)
                showMediaRetentionDialog = false
            },
            onDismiss = { showMediaRetentionDialog = false }
        )
    }

    // Messages Per Chat Dialog
    if (showMessagesPerChatDialog) {
        MessagesPerChatDialog(
            currentLimit = messagesPerChat,
            onLimitSelected = { limit ->
                viewModel.setMessagesPerChat(limit)
                showMessagesPerChatDialog = false
            },
            onDismiss = { showMessagesPerChatDialog = false }
        )
    }
}

/**
 * Profile card at top of settings.
 */
@Composable
private fun ProfileCard(
    name: String,
    email: String,
    avatarUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = name.split(" ").mapNotNull { it.firstOrNull() }.take(2).joinToString(""),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }

            // Name and email
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp),
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Text(
                    text = email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Chevron
            Icon(
                imageVector = Icons.Filled.ChevronRight,
                contentDescription = "View details",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
        }
    }
}

/**
 * User details dialog with improved spacing.
 */
@Composable
private fun UserDetailsDialog(
    name: String,
    email: String,
    phone: String,
    organization: String,
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp) // Further increased spacing for better readability
            ) {
                // Header
                Text(
                    text = "Profile Details",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                HorizontalDivider()

                // User details with more spacing
                UserDetailItem(
                    icon = Icons.Filled.Person,
                    label = "Name",
                    value = name
                )

                UserDetailItem(
                    icon = Icons.Filled.Email,
                    label = "Email",
                    value = email
                )

                UserDetailItem(
                    icon = Icons.Filled.Phone,
                    label = "Phone",
                    value = phone
                )

                UserDetailItem(
                    icon = Icons.Filled.Business,
                    label = "Organization",
                    value = organization
                )

                // Close button
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Close")
                }
            }
        }
    }
}

/**
 * User detail item in dialog.
 */
@Composable
private fun UserDetailItem(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )

        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Media retention period selection dialog.
 */
@Composable
private fun MediaRetentionDialog(
    currentPeriod: MediaRetentionPeriod,
    onPeriodSelected: (MediaRetentionPeriod) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Keep Media") },
        text = {
            Column {
                Text(
                    text = "Choose how long to keep media files in the app.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                MediaRetentionPeriod.values().forEach { period ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPeriodSelected(period) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentPeriod == period,
                            onClick = { onPeriodSelected(period) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (period) {
                                MediaRetentionPeriod.ONE_WEEK -> "1 Week"
                                MediaRetentionPeriod.ONE_MONTH -> "1 Month"
                                MediaRetentionPeriod.THREE_MONTHS -> "3 Months"
                                MediaRetentionPeriod.SIX_MONTHS -> "6 Months"
                                MediaRetentionPeriod.ONE_YEAR -> "1 Year"
                                MediaRetentionPeriod.FOREVER -> "Forever"
                            },
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Messages per chat limit selection dialog.
 */
@Composable
private fun MessagesPerChatDialog(
    currentLimit: Int,
    onLimitSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val options = listOf(-1, 50, 100, 250, 500) // -1 means unlimited

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Messages per Chat") },
        text = {
            Column {
                Text(
                    text = "Choose how many messages to keep for each chat thread. Older messages will be deleted first.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                options.forEach { limit ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onLimitSelected(limit) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentLimit == limit,
                            onClick = { onLimitSelected(limit) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (limit == -1) "All messages" else "$limit messages",
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Section header text.
 */
@Composable
private fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall.copy(fontSize = 14.sp),
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(horizontal = 4.dp)
    )
}

/**
 * Theme mode selection option (Dark/Light/System).
 */
@Composable
private fun ThemeModeOption(
    name: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceVariant
                )
                .then(
                    if (isSelected) {
                        Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = name,
                tint = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(32.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Individual settings item row.
 */
@Composable
private fun SettingsItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    value: String? = null,
    showChevron: Boolean = true,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )

            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            if (value != null) {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (showChevron) {
                Icon(
                    imageVector = Icons.Filled.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
