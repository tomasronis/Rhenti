package com.tomasronis.rhentiapp.presentation.main.tabs

import androidx.compose.runtime.*
import com.tomasronis.rhentiapp.presentation.main.chathub.ThreadListScreen
import com.tomasronis.rhentiapp.presentation.main.chathub.ThreadDetailScreen
import com.tomasronis.rhentiapp.data.chathub.models.ChatThread

/**
 * Chats tab content - manages navigation between thread list and detail.
 */
@Composable
fun ChatsTabContent() {
    var selectedThread by remember { mutableStateOf<ChatThread?>(null) }

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
