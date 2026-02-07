package com.tomasronis.rhentiapp.presentation.main

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import androidx.hilt.navigation.compose.hiltViewModel
import com.tomasronis.rhentiapp.core.voip.CallState
import com.tomasronis.rhentiapp.presentation.auth.AuthViewModel
import com.tomasronis.rhentiapp.presentation.calls.active.ActiveCallScreen
import com.tomasronis.rhentiapp.presentation.main.tabs.*
import com.tomasronis.rhentiapp.presentation.theme.*
import kotlinx.coroutines.launch

/**
 * Main authenticated screen with bottom tab navigation.
 * Tabs: Chats, Contacts, Calls, Profile
 */
@Composable
fun MainTabScreen(
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val viewModel: MainTabViewModel = hiltViewModel()
    val selectedTab by viewModel.selectedTab.collectAsState()
    val unreadCount by viewModel.unreadCount.collectAsState()
    val contactToStartChat by viewModel.contactToStartChat.collectAsState()
    val callState by viewModel.callState.collectAsState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // State for pending phone number (waiting for permission)
    var pendingPhoneNumber by remember { mutableStateOf<String?>(null) }

    // Permission launcher for microphone access
    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, make the call
            pendingPhoneNumber?.let { phoneNumber ->
                viewModel.makeCall(phoneNumber)
                pendingPhoneNumber = null
            }
        } else {
            // Permission denied
            scope.launch {
                snackbarHostState.showSnackbar(
                    message = "Microphone permission is required to make calls",
                    duration = SnackbarDuration.Long
                )
            }
            pendingPhoneNumber = null
        }
    }

    // Function to check permission and make call
    fun handleMakeCall(phoneNumber: String) {
        val hasPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PermissionChecker.PERMISSION_GRANTED

        if (hasPermission) {
            // Permission already granted, make the call
            viewModel.makeCall(phoneNumber)
        } else {
            // Request permission
            pendingPhoneNumber = phoneNumber
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    // Initialize Twilio on first composition
    LaunchedEffect(Unit) {
        viewModel.initializeTwilio()
    }

    // Check if we should show the active call screen
    val showActiveCallScreen = callState is CallState.Active ||
            callState is CallState.Ringing ||
            callState is CallState.Dialing

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            // iOS-styled bottom navigation with dark rounded pill container
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 24.dp, end = 24.dp, top = 0.dp, bottom = 12.dp),
                shape = RoundedCornerShape(32.dp),
                color = Color(0xFF1C1C1E),
                tonalElevation = 8.dp
            ) {
                NavigationBar(
                    modifier = Modifier
                        .height(72.dp)
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onSurface
                ) {
                    NavigationBarItem(
                        icon = {
                            Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                                BadgedBox(
                                    badge = {
                                        if (unreadCount > 0) {
                                            Badge(
                                                containerColor = UnreadBadge,
                                                contentColor = Color.White
                                            ) {
                                                Text(
                                                    unreadCount.toString(),
                                                    style = MaterialTheme.typography.labelSmall
                                                )
                                            }
                                        }
                                    }
                                ) {
                                    Icon(
                                        Icons.Filled.Chat,
                                        contentDescription = null,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        },
                        label = {
                            Text(
                                "Messages",
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        selected = selectedTab == 0,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = RhentiCoral,
                            selectedTextColor = RhentiCoral,
                            unselectedIconColor = Color(0xFF8E8E93),
                            unselectedTextColor = Color(0xFF8E8E93),
                            indicatorColor = Color(0xFF3A3A3C)
                        ),
                        onClick = {
                            scope.launch {
                                viewModel.setSelectedTab(0)
                            }
                        }
                    )

                    NavigationBarItem(
                        icon = {
                            Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Filled.People,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        },
                        label = {
                            Text(
                                "Contacts",
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        selected = selectedTab == 1,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = RhentiCoral,
                            selectedTextColor = RhentiCoral,
                            unselectedIconColor = Color(0xFF8E8E93),
                            unselectedTextColor = Color(0xFF8E8E93),
                            indicatorColor = Color(0xFF3A3A3C)
                        ),
                        onClick = {
                            scope.launch {
                                viewModel.setSelectedTab(1)
                            }
                        }
                    )

                    NavigationBarItem(
                        icon = {
                            Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Filled.Phone,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        },
                        label = {
                            Text(
                                "Calls",
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        selected = selectedTab == 2,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = RhentiCoral,
                            selectedTextColor = RhentiCoral,
                            unselectedIconColor = Color(0xFF8E8E93),
                            unselectedTextColor = Color(0xFF8E8E93),
                            indicatorColor = Color(0xFF3A3A3C)
                        ),
                        onClick = {
                            scope.launch {
                                viewModel.setSelectedTab(2)
                            }
                        }
                    )

                    NavigationBarItem(
                        icon = {
                            Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Filled.Settings,
                                    contentDescription = null,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        },
                        label = {
                            Text(
                                "Settings",
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        selected = selectedTab == 3,
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = RhentiCoral,
                            selectedTextColor = RhentiCoral,
                            unselectedIconColor = Color(0xFF8E8E93),
                            unselectedTextColor = Color(0xFF8E8E93),
                            indicatorColor = Color(0xFF3A3A3C)
                        ),
                        onClick = {
                            scope.launch {
                                viewModel.setSelectedTab(3)
                            }
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            // Main tab content
            when (selectedTab) {
                0 -> ChatsTabContent(
                    contactToStartChat = contactToStartChat,
                    onContactChatOpened = {
                        viewModel.clearContactToStartChat()
                    },
                    onStartCall = { phoneNumber ->
                        handleMakeCall(phoneNumber)
                    }
                )
                1 -> ContactsTabContent(
                    onStartChat = { contact ->
                        // Set the contact and switch to Chats tab
                        viewModel.setContactToStartChat(contact)
                        scope.launch {
                            viewModel.setSelectedTab(0)
                        }
                    },
                    isTwilioInitialized = viewModel.isTwilioInitialized.collectAsState().value
                )
                2 -> CallsTab(
                    onStartCall = { phoneNumber ->
                        handleMakeCall(phoneNumber)
                    }
                )
                3 -> ProfileTab(
                    onLogout = { authViewModel.logout() }
                )
            }

            // Active call screen overlay (shown when in a call)
            if (showActiveCallScreen) {
                ActiveCallScreen(
                    onNavigateBack = {
                        // Do nothing - call screen will auto-dismiss when call ends
                    }
                )
            }
        }
    }
}
