package com.tomasronis.rhentiapp.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tomasronis.rhentiapp.data.profile.models.AppSettings
import com.tomasronis.rhentiapp.data.profile.models.DarkModePreference

/**
 * Settings screen for app preferences.
 * Allows user to configure notifications and appearance.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show success message
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearSuccessMessage()
        }
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Header row with title (no filter icon or search for settings)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
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
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Notifications Section
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        ListItem(
                            headlineContent = { Text("Notifications") },
                            leadingContent = {
                                Icon(Icons.Filled.Notifications, contentDescription = null)
                            },
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        HorizontalDivider()

                        SwitchListItem(
                            title = "Enable Notifications",
                            subtitle = "Receive app notifications",
                            checked = uiState.settings.notificationsEnabled,
                            onCheckedChange = { checked ->
                                viewModel.saveSettings(
                                    uiState.settings.copy(notificationsEnabled = checked)
                                )
                            }
                        )

                        if (uiState.settings.notificationsEnabled) {
                            HorizontalDivider()

                            SwitchListItem(
                                title = "Call Notifications",
                                subtitle = "Notify for incoming calls",
                                checked = uiState.settings.callNotificationsEnabled,
                                onCheckedChange = { checked ->
                                    viewModel.saveSettings(
                                        uiState.settings.copy(callNotificationsEnabled = checked)
                                    )
                                }
                            )

                            HorizontalDivider()

                            SwitchListItem(
                                title = "Message Notifications",
                                subtitle = "Notify for new messages",
                                checked = uiState.settings.messageNotificationsEnabled,
                                onCheckedChange = { checked ->
                                    viewModel.saveSettings(
                                        uiState.settings.copy(messageNotificationsEnabled = checked)
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // Appearance Section
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        ListItem(
                            headlineContent = { Text("Appearance") },
                            leadingContent = {
                                Icon(Icons.Filled.Palette, contentDescription = null)
                            },
                            modifier = Modifier.padding(vertical = 8.dp)
                        )

                        HorizontalDivider()

                        DarkModeSelector(
                            selectedMode = uiState.settings.darkMode,
                            onModeSelected = { mode ->
                                viewModel.saveSettings(
                                    uiState.settings.copy(darkMode = mode)
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Switch list item composable
 */
@Composable
private fun SwitchListItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange
            )
        },
        modifier = modifier.padding(vertical = 8.dp)
    )
}

/**
 * Dark mode selector composable
 */
@Composable
private fun DarkModeSelector(
    selectedMode: DarkModePreference,
    onModeSelected: (DarkModePreference) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.padding(16.dp)) {
        Text(
            text = "Dark Mode",
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(modifier = Modifier.height(8.dp))

        DarkModePreference.values().forEach { mode ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = selectedMode == mode,
                    onClick = { onModeSelected(mode) }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = when (mode) {
                            DarkModePreference.LIGHT -> "Light"
                            DarkModePreference.DARK -> "Dark"
                            DarkModePreference.SYSTEM -> "System Default"
                        },
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = when (mode) {
                            DarkModePreference.LIGHT -> "Always use light theme"
                            DarkModePreference.DARK -> "Always use dark theme"
                            DarkModePreference.SYSTEM -> "Follow system settings"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
