package com.tomasronis.rhentiapp.presentation.main.chathub

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tomasronis.rhentiapp.data.chathub.models.ChatThread
import com.tomasronis.rhentiapp.presentation.main.chathub.components.*
import com.tomasronis.rhentiapp.presentation.main.components.FilterIcon
import com.tomasronis.rhentiapp.presentation.main.components.RhentiSearchBar

/**
 * Thread list screen showing all chat conversations.
 * Includes search, manual refresh button, and swipe actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThreadListScreen(
    onThreadClick: (ChatThread) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChatHubViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    var showDeleteDialog by remember { mutableStateOf<ChatThread?>(null) }
    var showFiltersModal by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshThreads()
    }

    Scaffold(
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
            ) {
                // Header row with title and filter icon
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Large title
                    Text(
                        text = "Messages",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    // Filter icon
                    IconButton(onClick = { showFiltersModal = true }) {
                        FilterIcon(tint = MaterialTheme.colorScheme.onBackground)
                    }
                }

                // Always-visible search bar using RhentiSearchBar
                RhentiSearchBar(
                    query = searchQuery,
                    onQueryChange = { viewModel.searchThreads(it) },
                    placeholder = "Search by name or email",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading && uiState.threads.isEmpty() -> {
                    // Initial loading
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.error != null && uiState.threads.isEmpty() -> {
                    // Error with no cached data
                    ErrorStateView(
                        message = uiState.error ?: "An error occurred",
                        onRetry = { viewModel.refreshThreads() }
                    )
                }
                uiState.threads.isEmpty() -> {
                    // No threads
                    EmptyThreadsView()
                }
                else -> {
                    // Show threads
                    ThreadList(
                        threads = uiState.threads,
                        onThreadClick = onThreadClick,
                        onRefresh = { viewModel.refreshThreads() },
                        isRefreshing = uiState.isLoading,
                        onPinThread = { viewModel.toggleThreadPinned(it) },
                        onDeleteThread = { showDeleteDialog = it }
                    )
                }
            }

            // Show error snackbar if there's an error but we have cached data
            if (uiState.error != null && uiState.threads.isNotEmpty()) {
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearError() }) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(uiState.error ?: "An error occurred")
                }
            }
        }

        // Delete confirmation dialog
        showDeleteDialog?.let { thread ->
            AlertDialog(
                onDismissRequest = { showDeleteDialog = null },
                title = { Text("Delete conversation?") },
                text = { Text("Are you sure you want to delete this conversation with ${thread.displayName}?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.deleteThread(thread)
                            showDeleteDialog = null
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Filters modal
        if (showFiltersModal) {
            MessageFiltersModal(
                onDismiss = { showFiltersModal = false }
            )
        }
    }
}

/**
 * List of threads with swipe actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ThreadList(
    threads: List<ChatThread>,
    onThreadClick: (ChatThread) -> Unit,
    onRefresh: () -> Unit,
    isRefreshing: Boolean,
    onPinThread: (ChatThread) -> Unit,
    onDeleteThread: (ChatThread) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 0.dp  // Minimize gap between last thread and bottom tabs
            ),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = threads,
                key = { it.id }
            ) { thread ->
                SwipeableThreadCard(
                    thread = thread,
                    onClick = { onThreadClick(thread) },
                    onPin = { onPinThread(thread) },
                    onDelete = { onDeleteThread(thread) }
                )
            }
        }

        // Show loading indicator while refreshing
        if (isRefreshing) {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            )
        }
    }
}

/**
 * Thread card with swipe actions (pin/delete).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeableThreadCard(
    thread: ChatThread,
    onClick: () -> Unit,
    onPin: () -> Unit,
    onDelete: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { direction ->
            when (direction) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    // Right swipe - Pin/Unpin
                    onPin()
                    false // Don't actually dismiss
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    // Left swipe - Delete
                    onDelete()
                    true // Dismiss (will be removed by ViewModel)
                }
                else -> false
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val backgroundColor = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primary
                SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error
                else -> Color.Transparent
            }

            val icon = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> if (thread.isPinned) Icons.Filled.StarBorder else Icons.Filled.Star
                SwipeToDismissBoxValue.EndToStart -> Icons.Filled.Delete
                else -> null
            }

            val alignment = when (dismissState.dismissDirection) {
                SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
                SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                else -> Alignment.Center
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(backgroundColor)
                    .padding(horizontal = 24.dp),
                contentAlignment = alignment
            ) {
                icon?.let {
                    Icon(
                        imageVector = it,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    ) {
        ThreadCard(
            thread = thread,
            onClick = onClick
        )
    }
}
