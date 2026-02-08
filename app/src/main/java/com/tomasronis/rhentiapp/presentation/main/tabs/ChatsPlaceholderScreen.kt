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
 * Can automatically open a thread when coming from Contacts tab or Calls tab.
 * Can navigate to contact detail from thread detail.
 */
@Composable
fun ChatsTabContent(
    contactToStartChat: Contact? = null,
    threadIdToOpen: String? = null,
    onContactChatOpened: () -> Unit = {},
    onThreadOpened: () -> Unit = {},
    onStartCall: (String) -> Unit = {}
) {
    val chatViewModel: ChatHubViewModel = hiltViewModel()
    val contactsViewModel: ContactsViewModel = hiltViewModel()
    val chatUiState by chatViewModel.uiState.collectAsState()
    val contactsUiState by contactsViewModel.uiState.collectAsState()

    var selectedThread by remember { mutableStateOf<ChatThread?>(null) }
    var selectedContactFromThread by remember { mutableStateOf<Contact?>(null) }
    var selectedThreadIdForContact by remember { mutableStateOf<String?>(null) }

    // Auto-open thread by ID when coming from Calls tab
    LaunchedEffect(threadIdToOpen) {
        if (!threadIdToOpen.isNullOrBlank()) {
            // Find thread by ID
            val matchingThread = chatUiState.threads.find { thread ->
                thread.id == threadIdToOpen
            }

            if (matchingThread != null) {
                selectedThread = matchingThread
            }

            // Clear the thread ID state
            onThreadOpened()
        }
    }

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
                threadId = selectedThreadIdForContact, // Pass threadId for loading viewings/applications
                onNavigateBack = {
                    selectedContactFromThread = null
                    selectedThreadIdForContact = null
                },
                onStartChat = { contact ->
                    // Navigate back to thread
                    selectedContactFromThread = null
                    selectedThreadIdForContact = null
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
                    android.util.Log.d("ChatsPlaceholder", "=== NAVIGATE TO CONTACT ===")
                    android.util.Log.d("ChatsPlaceholder", "Thread: ${thread.displayName}")
                    android.util.Log.d("ChatsPlaceholder", "Thread imageUrl: ${thread.imageUrl}")
                    android.util.Log.d("ChatsPlaceholder", "Thread channel: ${thread.channel}")
                    android.util.Log.d("ChatsPlaceholder", "Thread email: ${thread.email}")
                    android.util.Log.d("ChatsPlaceholder", "Thread phone: ${thread.phone}")
                    android.util.Log.d("ChatsPlaceholder", "Thread renterId: ${thread.renterId}")
                    android.util.Log.d("ChatsPlaceholder", "Available contacts: ${contactsUiState.contacts.size}")

                    // Find matching contact - prioritize ID, then email, then phone
                    val matchingContact = contactsUiState.contacts.find { contact ->
                        // Prioritize ID matching (most reliable)
                        (thread.renterId != null && contact.id == thread.renterId) ||
                        // Fall back to email matching
                        (!thread.email.isNullOrBlank() && !contact.email.isNullOrBlank() &&
                         contact.email.equals(thread.email, ignoreCase = true)) ||
                        // Fall back to phone matching
                        (!thread.phone.isNullOrBlank() && !contact.phone.isNullOrBlank() &&
                         contact.phone == thread.phone)
                    }

                    val contactToShow = if (matchingContact != null) {
                        android.util.Log.d("ChatsPlaceholder", "Found matching contact: ${matchingContact.displayName}")
                        android.util.Log.d("ChatsPlaceholder", "Contact avatarUrl BEFORE merge: ${matchingContact.avatarUrl}")
                        android.util.Log.d("ChatsPlaceholder", "Contact channel BEFORE merge: ${matchingContact.channel}")

                        // Merge thread data (imageUrl, channel) with contact data
                        // This ensures profile pic and channel show immediately
                        matchingContact.copy(
                            avatarUrl = thread.imageUrl ?: matchingContact.avatarUrl,
                            channel = thread.channel ?: matchingContact.channel
                        )
                    } else {
                        android.util.Log.w("ChatsPlaceholder", "No matching contact found - creating from thread")

                        // Create a contact from thread data
                        Contact(
                            id = thread.renterId ?: thread.id,
                            firstName = thread.displayName.split(" ").firstOrNull(),
                            lastName = thread.displayName.split(" ").drop(1).joinToString(" ").takeIf { it.isNotEmpty() },
                            email = thread.email,
                            phone = thread.phone,
                            avatarUrl = thread.imageUrl,
                            propertyIds = listOfNotNull(thread.propertyId),
                            totalMessages = 0,
                            totalCalls = 0,
                            lastActivity = thread.lastMessageTime,
                            channel = thread.channel
                        )
                    }

                    android.util.Log.d("ChatsPlaceholder", "Final contact avatarUrl: ${contactToShow.avatarUrl}")
                    android.util.Log.d("ChatsPlaceholder", "Final contact channel: ${contactToShow.channel}")
                    android.util.Log.d("ChatsPlaceholder", "Thread ID for viewings/applications: ${thread.id}")

                    selectedContactFromThread = contactToShow
                    selectedThreadIdForContact = thread.id // Store thread ID for loading viewings/applications

                    // Persist the contact data to database for future use
                    contactsViewModel.updateContact(contactToShow)
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
