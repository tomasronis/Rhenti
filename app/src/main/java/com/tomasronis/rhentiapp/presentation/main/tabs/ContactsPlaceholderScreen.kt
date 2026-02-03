package com.tomasronis.rhentiapp.presentation.main.tabs

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.tomasronis.rhentiapp.core.voip.CallService
import com.tomasronis.rhentiapp.presentation.main.contacts.ContactsListScreen
import com.tomasronis.rhentiapp.presentation.main.contacts.ContactDetailScreen
import com.tomasronis.rhentiapp.data.contacts.models.Contact

/**
 * Contacts tab content - manages navigation between contact list and detail.
 * Provides actions for starting chat and calling (call is placeholder for Phase 7).
 */
@Composable
fun ContactsTabContent(
    onStartChat: (Contact) -> Unit = {}
) {
    val context = LocalContext.current
    var selectedContact by remember { mutableStateOf<Contact?>(null) }

    if (selectedContact != null) {
        ContactDetailScreen(
            contact = selectedContact!!,
            onNavigateBack = { selectedContact = null },
            onStartChat = { contact ->
                // Navigate to chats tab with this contact
                onStartChat(contact)
                selectedContact = null
            },
            onCall = { contact ->
                // Initiate VoIP call using Twilio
                contact.phone?.let { phoneNumber ->
                    CallService.startCall(context, phoneNumber)
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
