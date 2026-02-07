package com.tomasronis.rhentiapp.presentation.main.tabs

import androidx.compose.runtime.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.tomasronis.rhentiapp.data.chathub.models.ChatThread
import com.tomasronis.rhentiapp.data.contacts.models.Contact
import com.tomasronis.rhentiapp.presentation.main.chathub.ChatHubViewModel
import com.tomasronis.rhentiapp.presentation.main.chathub.ThreadDetailScreen
import com.tomasronis.rhentiapp.presentation.main.chathub.ThreadListScreen
import com.tomasronis.rhentiapp.presentation.main.contacts.ContactDetailScreen
import com.tomasronis.rhentiapp.presentation.main.contacts.ContactsViewModel

/**
 * Chats tab content - manages navigation between thread list and detail.
 * Can automatically open a thread when coming from Contacts tab.
 * Can navigate to contact detail from thread detail.
 */
@Composable
fun ChatsTabContent(
    contactToStartChat: Contact? = null,
    onContactChatOpened: () -> Unit = {},
    onStartCall: (String) -> Unit = {}
) {
    val chatViewModel: ChatHubViewModel = hiltViewModel()
    val contactsViewModel: ContactsViewModel = hiltViewModel()
    val chatUiState by chatViewModel.uiState.collectAsState()
    val contactsUiState by contactsViewModel.uiState.collectAsState()

    var selectedThread by remember { mutableStateOf<ChatThread?>(null) }
    var selectedContactFromThread by remember { mutableStateOf<Contact?>(null) }

    // Auto-open thread when coming from contact
    LaunchedEffect(contactToStartChat) {
        if (contactToStartChat != null) {
            // Find thread matching this contact by email or phone
            val matchingThread = chatUiState.threads.find { thread ->
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

    when {
        // Show contact detail if navigated from thread
        selectedContactFromThread != null -> {
            ContactDetailScreen(
                contact = selectedContactFromThread!!,
                onNavigateBack = { selectedContactFromThread = null },
                onStartChat = { contact ->
                    // Navigate back to thread
                    selectedContactFromThread = null
                },
                onCall = { contact ->
                    // Make call
                    contact.phone?.let { onStartCall(it) }
                }
            )
        }
        // Show thread detail if thread selected
        selectedThread != null -> {
            ThreadDetailScreen(
                thread = selectedThread!!,
                onNavigateBack = { selectedThread = null },
                onCall = onStartCall,
                onNavigateToContact = { thread ->
                    // Find matching contact by email or phone
                    val matchingContact = contactsUiState.contacts.find { contact ->
                        (!thread.email.isNullOrBlank() && contact.email == thread.email) ||
                        (!thread.phone.isNullOrBlank() && contact.phone == thread.phone) ||
                        (thread.renterId != null && contact.id == thread.renterId)
                    }

                    if (matchingContact != null) {
                        selectedContactFromThread = matchingContact
                    }
                }
            )
        }
        // Show thread list
        else -> {
            ThreadListScreen(
                onThreadClick = { thread ->
                    selectedThread = thread
                }
            )
        }
    }
}
