package com.tomasronis.rhentiapp.presentation.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tomasronis.rhentiapp.presentation.profile.components.PasswordChangeDialog
import com.tomasronis.rhentiapp.presentation.profile.components.ProfileAvatarSection
import com.tomasronis.rhentiapp.presentation.profile.components.ProfileInfoSection

/**
 * Profile screen showing user information and settings.
 * Allows editing profile, changing password, and logging out.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateToSettings: () -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var showPasswordDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Load profile on screen open
    LaunchedEffect(Unit) {
        if (currentUser == null) {
            viewModel.loadProfile()
        }
    }

    // Show success/error messages
    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearSuccessMessage()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                actions = {
                    if (!uiState.isEditing) {
                        IconButton(onClick = { viewModel.setEditing(true) }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Edit Profile")
                        }
                    } else {
                        IconButton(onClick = { viewModel.setEditing(false) }) {
                            Icon(Icons.Filled.Close, contentDescription = "Cancel")
                        }
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (currentUser == null && uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Profile Avatar
                    item {
                        ProfileAvatarSection(
                            user = currentUser,
                            isUploading = uiState.isUploadingPhoto,
                            isEditing = uiState.isEditing,
                            onPhotoSelected = { base64 ->
                                viewModel.uploadPhoto(base64)
                            }
                        )
                    }

                    // Profile Info
                    item {
                        ProfileInfoSection(
                            user = currentUser,
                            isEditing = uiState.isEditing,
                            onSave = { firstName, lastName, email, phone ->
                                viewModel.updateProfile(firstName, lastName, email, phone)
                            }
                        )
                    }

                    // Settings Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column {
                                ListItem(
                                    headlineContent = { Text("Settings") },
                                    leadingContent = {
                                        Icon(Icons.Filled.Settings, contentDescription = null)
                                    },
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )

                                HorizontalDivider()

                                ListItem(
                                    headlineContent = { Text("App Settings") },
                                    supportingContent = { Text("Notifications, appearance, and more") },
                                    leadingContent = {
                                        Icon(Icons.Filled.Tune, contentDescription = null)
                                    },
                                    trailingContent = {
                                        Icon(Icons.Filled.ChevronRight, contentDescription = null)
                                    },
                                    modifier = Modifier
                                        .clickable { onNavigateToSettings() }
                                        .padding(vertical = 8.dp)
                                )

                                HorizontalDivider()

                                ListItem(
                                    headlineContent = { Text("Change Password") },
                                    leadingContent = {
                                        Icon(Icons.Filled.Lock, contentDescription = null)
                                    },
                                    trailingContent = {
                                        Icon(Icons.Filled.ChevronRight, contentDescription = null)
                                    },
                                    modifier = Modifier
                                        .clickable { showPasswordDialog = true }
                                        .padding(vertical = 8.dp)
                                )
                            }
                        }
                    }

                    // Logout Button
                    item {
                        Button(
                            onClick = {
                                viewModel.logout()
                                onLogout()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Filled.Logout, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Logout")
                        }
                    }
                }
            }
        }
    }

    // Password Change Dialog
    if (showPasswordDialog) {
        PasswordChangeDialog(
            onDismiss = { showPasswordDialog = false },
            onConfirm = { currentPassword, newPassword ->
                viewModel.changePassword(currentPassword, newPassword)
                showPasswordDialog = false
            }
        )
    }
}
