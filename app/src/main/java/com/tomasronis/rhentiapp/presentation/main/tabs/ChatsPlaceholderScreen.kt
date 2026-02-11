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
                    // Use separate matching steps to ensure proper priority
                    val matchingContact = contactsUiState.contacts.firstOrNull { contact ->
                        // 1. Try ID matching first (most reliable)
                        thread.renterId != null && contact.id == thread.renterId
                    } ?: contactsUiState.contacts.firstOrNull { contact ->
                        // 2. Try email matching (reliable identifier)
                        !thread.email.isNullOrBlank() && !contact.email.isNullOrBlank() &&
                        contact.email.equals(thread.email, ignoreCase = true)
                    } ?: contactsUiState.contacts.firstOrNull { contact ->
                        // 3. Try phone matching (less reliable - can have duplicates)
                        !thread.phone.isNullOrBlank() && !contact.phone.isNullOrBlank() &&
                        contact.phone == thread.phone
                    }

                    val contactToShow = if (matchingContact != null) {
                        android.util.Log.d("ChatsPlaceholder", "Found matching contact: ${matchingContact.displayName}")

                        // Determine match type and check if it's a phone-only mismatch
                        val matchType = when {
                            thread.renterId != null && matchingContact.id == thread.renterId -> "Renter ID"
                            !thread.email.isNullOrBlank() && matchingContact.email?.equals(thread.email, ignoreCase = true) == true -> "Email"
                            !thread.phone.isNullOrBlank() && matchingContact.phone == thread.phone -> "Phone"
                            else -> "Unknown"
                        }

                        android.util.Log.d("ChatsPlaceholder", "Matched by: $matchType")

                        // Check if phone-only match has email mismatch
                        val isPhoneOnlyMismatch = matchType == "Phone" &&
                            !thread.email.isNullOrBlank() &&
                            !matchingContact.email.isNullOrBlank() &&
                            !matchingContact.email.equals(thread.email, ignoreCase = true)

                        if (isPhoneOnlyMismatch) {
                            android.util.Log.w("ChatsPlaceholder", "WARNING: Phone-only match with email mismatch")
                            android.util.Log.w("ChatsPlaceholder", "Thread email: ${thread.email} vs Contact email: ${matchingContact.email}")
                            android.util.Log.w("ChatsPlaceholder", "Thread name: ${thread.displayName} vs Contact name: ${matchingContact.displayName}")
                            android.util.Log.w("ChatsPlaceholder", "Creating new contact from thread data instead")

                            // Create contact from thread data instead of using mismatched contact
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
                        } else {
                            android.util.Log.d("ChatsPlaceholder", "Contact ID: ${matchingContact.id}")
                            android.util.Log.d("ChatsPlaceholder", "Contact email: ${matchingContact.email}")
                            android.util.Log.d("ChatsPlaceholder", "Contact avatarUrl BEFORE merge: ${matchingContact.avatarUrl}")
                            android.util.Log.d("ChatsPlaceholder", "Contact channel BEFORE merge: ${matchingContact.channel}")

                            // Merge thread data (imageUrl, channel, name) with contact data
                            // This ensures profile pic, channel, and name show immediately from thread
                            matchingContact.copy(
                                firstName = thread.displayName.split(" ").firstOrNull() ?: matchingContact.firstName,
                                lastName = thread.displayName.split(" ").drop(1).joinToString(" ").takeIf { it.isNotEmpty() } ?: matchingContact.lastName,
                                email = thread.email?.takeIf { it.isNotBlank() } ?: matchingContact.email,
                                avatarUrl = thread.imageUrl ?: matchingContact.avatarUrl,
                                channel = thread.channel ?: matchingContact.channel
                            )
                        }
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
