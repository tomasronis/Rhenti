package com.tomasronis.rhentiapp.presentation.main.tabs

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import com.tomasronis.rhentiapp.core.voip.CallService
import com.tomasronis.rhentiapp.data.contacts.models.Contact
import com.tomasronis.rhentiapp.presentation.main.contacts.ContactDetailScreen
import com.tomasronis.rhentiapp.presentation.main.contacts.ContactsListScreen
import com.tomasronis.rhentiapp.presentation.main.contacts.ContactsViewModel

/**
 * Contacts tab content - manages navigation between contact list and detail.
 * Provides actions for starting chat and calling.
 */
@Composable
fun ContactsTabContent(
    contactIdToOpen: String? = null,
    threadIdToOpen: String? = null, // Thread ID for loading viewings/applications
    onStartChat: (Contact) -> Unit = {},
    onContactOpened: () -> Unit = {},
    isTwilioInitialized: Boolean = false
) {
    val context = LocalContext.current
    val contactsViewModel: ContactsViewModel = hiltViewModel()
    val contactsUiState by contactsViewModel.uiState.collectAsState()

    var selectedContact by remember { mutableStateOf<Contact?>(null) }
    var selectedThreadId by remember { mutableStateOf<String?>(null) }
    var pendingCallNumber by remember { mutableStateOf<String?>(null) }

    // Auto-open contact by ID when coming from Calls tab
    LaunchedEffect(contactIdToOpen, threadIdToOpen) {
        if (!contactIdToOpen.isNullOrBlank()) {
            // Find contact by ID
            val matchingContact = contactsUiState.contacts.find { contact ->
                contact.id == contactIdToOpen
            }

            if (matchingContact != null) {
                selectedContact = matchingContact
                selectedThreadId = threadIdToOpen // Store threadId for loading viewings/applications
            }

            // Clear the contact ID state
            onContactOpened()
        }
    }

    // Audio permission launcher
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, start the call
            pendingCallNumber?.let { phoneNumber ->
                try {
                    android.util.Log.d("ContactsTab", "Starting call to: $phoneNumber")
                    CallService.startCall(context, phoneNumber)
                } catch (e: Exception) {
                    android.util.Log.e("ContactsTab", "Failed to start call", e)
                    // Show error to user
                    android.widget.Toast.makeText(
                        context,
                        "Failed to start call: ${e.message}",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
        } else {
            android.util.Log.d("ContactsTab", "Audio permission denied")
        }
        pendingCallNumber = null
    }

    if (selectedContact != null) {
        ContactDetailScreen(
            contact = selectedContact!!,
            threadId = selectedThreadId, // Pass thread ID to load viewings/applications
            onNavigateBack = {
                selectedContact = null
                selectedThreadId = null // Clear threadId when navigating back
            },
            onStartChat = { contact ->
                // Navigate to chats tab with this contact
                onStartChat(contact)
                selectedContact = null
                selectedThreadId = null
            },
            onCall = { contact ->
                // Check if Twilio is initialized
                if (!isTwilioInitialized) {
                    android.widget.Toast.makeText(
                        context,
                        "Call service is initializing, please try again in a moment",
                        android.widget.Toast.LENGTH_SHORT
                    ).show()
                    return@ContactDetailScreen
                }

                // Check for audio permission before initiating call
                contact.phone?.let { phoneNumber ->
                    pendingCallNumber = phoneNumber
                    audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            }
        )
    } else {
        ContactsListScreen(
            onContactClick = { contact ->
                selectedContact = contact
            }
        )
    }
}
