package com.tomasronis.rhentiapp.presentation.main.tabs

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.tomasronis.rhentiapp.core.voip.CallService
import com.tomasronis.rhentiapp.data.contacts.models.Contact
import com.tomasronis.rhentiapp.presentation.main.contacts.ContactDetailScreen
import com.tomasronis.rhentiapp.presentation.main.contacts.ContactsListScreen

/**
 * Contacts tab content - manages navigation between contact list and detail.
 * Provides actions for starting chat and calling.
 */
@Composable
fun ContactsTabContent(
    onStartChat: (Contact) -> Unit = {}
) {
    val context = LocalContext.current
    var selectedContact by remember { mutableStateOf<Contact?>(null) }
    var pendingCallNumber by remember { mutableStateOf<String?>(null) }

    // Audio permission launcher
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            // Permission granted, start the call
            pendingCallNumber?.let { phoneNumber ->
                CallService.startCall(context, phoneNumber)
            }
        }
        pendingCallNumber = null
    }

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
