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
    onStartChat: (Contact) -> Unit = {},
    isTwilioInitialized: Boolean = false
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
            onNavigateBack = { selectedContact = null },
            onStartChat = { contact ->
                // Navigate to chats tab with this contact
                onStartChat(contact)
                selectedContact = null
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
