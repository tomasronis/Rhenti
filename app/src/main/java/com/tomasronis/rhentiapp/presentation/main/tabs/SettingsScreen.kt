package com.tomasronis.rhentiapp.presentation.main.tabs

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tomasronis.rhentiapp.presentation.theme.*

/**
 * Settings screen matching iOS design.
 * Features profile card, theme selector, storage settings, and more.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTheme by remember { mutableStateOf("Rhenti") }

    Scaffold(
        topBar = {
            // Large title header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
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
                    name = "Peter Chen",
                    email = "peter+demo@pchen.ca",
                    avatarUrl = null,
                    onClick = { /* TODO: Navigate to profile edit */ }
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
                                imageVector = Icons.Filled.Palette,
                                contentDescription = null,
                                tint = RhentiCoral,
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                text = "Theme Color",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Theme color selector
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            ThemeColorOption(
                                name = "Rhenti",
                                color = RhentiCoral,
                                isSelected = selectedTheme == "Rhenti",
                                onClick = { selectedTheme = "Rhenti" }
                            )

                            ThemeColorOption(
                                name = "Ocean",
                                color = AccentBlue,
                                isSelected = selectedTheme == "Ocean",
                                onClick = { selectedTheme = "Ocean" }
                            )

                            ThemeColorOption(
                                name = "Earth",
                                color = Color(0xFF5AC8A8),
                                isSelected = selectedTheme == "Earth",
                                onClick = { selectedTheme = "Earth" }
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
                            iconTint = RhentiCoral,
                            title = "Keep Media",
                            value = "1 Week",
                            onClick = { /* TODO: Open media retention settings */ }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 52.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )

                        SettingsItem(
                            icon = Icons.Filled.ChatBubble,
                            iconTint = RhentiCoral,
                            title = "Messages per Chat",
                            value = "200 messages",
                            onClick = { /* TODO: Open message limit settings */ }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 52.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )

                        SettingsItem(
                            icon = Icons.Filled.Sd,
                            iconTint = RhentiCoral,
                            title = "Storage Used",
                            value = "9.9 MB",
                            onClick = { /* TODO: Show storage details */ },
                            showChevron = false
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 52.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )

                        SettingsItem(
                            icon = Icons.Filled.Delete,
                            iconTint = RhentiCoral,
                            title = "Clear Cache",
                            value = "9.2 MB",
                            onClick = { /* TODO: Clear cache */ },
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
                            icon = Icons.Filled.Business,
                            iconTint = RhentiCoral,
                            title = "Organization",
                            value = "Demo Properties",
                            onClick = { /* TODO: Show organization details */ },
                            showChevron = false
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 52.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )

                        SettingsItem(
                            icon = Icons.Filled.Info,
                            iconTint = RhentiCoral,
                            title = "Version",
                            value = "2.0.0 (22)",
                            onClick = { /* TODO: Show version details */ },
                            showChevron = false
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 52.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )

                        SettingsItem(
                            icon = Icons.Filled.Wifi,
                            iconTint = RhentiCoral,
                            title = "Connection",
                            value = "Wi-Fi",
                            onClick = { /* TODO: Show connection details */ },
                            showChevron = false
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
                            iconTint = RhentiCoral,
                            title = "Help & Support",
                            onClick = { /* TODO: Open help */ }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 52.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )

                        SettingsItem(
                            icon = Icons.Filled.Shield,
                            iconTint = RhentiCoral,
                            title = "Privacy Policy",
                            onClick = { /* TODO: Open privacy policy */ }
                        )

                        HorizontalDivider(
                            modifier = Modifier.padding(start = 52.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                        )

                        SettingsItem(
                            icon = Icons.Filled.Description,
                            iconTint = RhentiCoral,
                            title = "Terms of Service",
                            onClick = { /* TODO: Open terms */ }
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
                    modifier = Modifier.clickable(onClick = onLogout)
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
                            tint = RhentiCoral,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "Sign Out",
                            style = MaterialTheme.typography.bodyLarge,
                            color = RhentiCoral,
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
                    color = RhentiCoral
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
                contentDescription = "Edit profile",
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
        }
    }
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
 * Theme color selection option.
 */
@Composable
private fun ThemeColorOption(
    name: String,
    color: Color,
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
                .background(color)
                .then(
                    if (isSelected) {
                        Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = "Selected",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant
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
                    color = RhentiCoral
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
