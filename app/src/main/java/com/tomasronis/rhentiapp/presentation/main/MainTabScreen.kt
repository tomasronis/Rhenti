package com.tomasronis.rhentiapp.presentation.main

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.tomasronis.rhentiapp.core.voip.CallState
import com.tomasronis.rhentiapp.presentation.auth.AuthViewModel
import com.tomasronis.rhentiapp.presentation.calls.active.ActiveCallScreen
import com.tomasronis.rhentiapp.presentation.main.tabs.*
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

    // Initialize Twilio on first composition
    LaunchedEffect(Unit) {
        viewModel.initializeTwilio()
    }

    // Check if we should show the active call screen
    val showActiveCallScreen = callState is CallState.Active ||
            callState is CallState.Ringing ||
            callState is CallState.Dialing

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    icon = {
                        BadgedBox(
                            badge = {
                                if (unreadCount > 0) {
                                    Badge {
                                        Text(unreadCount.toString())
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Filled.Chat, contentDescription = "Chats")
                        }
                    },
                    label = { Text("Chats") },
                    selected = selectedTab == 0,
                    onClick = {
                        scope.launch {
                            viewModel.setSelectedTab(0)
                        }
                    }
                )

                NavigationBarItem(
                    icon = { Icon(Icons.Filled.People, contentDescription = "Contacts") },
                    label = { Text("Contacts") },
                    selected = selectedTab == 1,
                    onClick = {
                        scope.launch {
                            viewModel.setSelectedTab(1)
                        }
                    }
                )

                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Phone, contentDescription = "Calls") },
                    label = { Text("Calls") },
                    selected = selectedTab == 2,
                    onClick = {
                        scope.launch {
                            viewModel.setSelectedTab(2)
                        }
                    }
                )

                NavigationBarItem(
                    icon = { Icon(Icons.Filled.Person, contentDescription = "Profile") },
                    label = { Text("Profile") },
                    selected = selectedTab == 3,
                    onClick = {
                        scope.launch {
                            viewModel.setSelectedTab(3)
                        }
                    }
                )
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
                        viewModel.makeCall(phoneNumber)
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
