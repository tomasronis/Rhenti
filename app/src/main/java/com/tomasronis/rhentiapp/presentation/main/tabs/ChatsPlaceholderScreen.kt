package com.tomasronis.rhentiapp.presentation.main.tabs

import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.tomasronis.rhentiapp.data.chathub.models.ChatThread
import com.tomasronis.rhentiapp.data.contacts.models.Contact
import com.tomasronis.rhentiapp.presentation.main.chathub.ChatHubViewModel
import com.tomasronis.rhentiapp.presentation.main.chathub.ThreadDetailScreen
import com.tomasronis.rhentiapp.presentation.main.chathub.ThreadListScreen

/**
 * Chats tab content - manages navigation between thread list and detail.
 * Can automatically open a thread when coming from Contacts tab.
 */
@Composable
fun ChatsTabContent(
    contactToStartChat: Contact? = null,
    onContactChatOpened: () -> Unit = {}
) {
    val viewModel: ChatHubViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()

    var selectedThread by remember { mutableStateOf<ChatThread?>(null) }

    // Auto-open thread when coming from contact
    LaunchedEffect(contactToStartChat) {
        if (contactToStartChat != null) {
            // Find thread matching this contact by email or phone
            val matchingThread = uiState.threads.find { thread ->
                (!contactToStartChat.email.isNullOrBlank() && thread.email == contactToStartChat.email) ||
                (!contactToStartChat.phone.isNullOrBlank() && thread.phone == contactToStartChat.phone)
            }

            if (matchingThread != null) {
                selectedThread = matchingThread
            }

            // Clear the contact state
            onContactChatOpened()
        }
    }

    if (selectedThread != null) {
        ThreadDetailScreen(
            thread = selectedThread!!,
            onNavigateBack = { selectedThread = null }
        )
    } else {
        ThreadListScreen(
            onThreadClick = { thread ->
                selectedThread = thread
            }
        )
    }
}
