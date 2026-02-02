package com.tomasronis.rhentiapp.presentation.main

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.tomasronis.rhentiapp.presentation.auth.AuthViewModel
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
    val scope = rememberCoroutineScope()

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
            when (selectedTab) {
                0 -> ChatsTabContent()
                1 -> ContactsTabContent(
                    onStartChat = { contact ->
                        // Switch to Chats tab when starting a chat from contacts
                        scope.launch {
                            viewModel.setSelectedTab(0)
                        }
                        // TODO: Could navigate to specific thread if exists
                    }
                )
                2 -> CallsPlaceholderScreen()
                3 -> ProfilePlaceholderScreen(
                    onLogout = { authViewModel.logout() }
                )
            }
        }
    }
}
