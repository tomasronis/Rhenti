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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tomasronis.rhentiapp.data.chathub.models.ChatThread
import com.tomasronis.rhentiapp.presentation.main.chathub.components.*

/**
 * Thread list screen showing all chat conversations.
 * Includes search, pull-to-refresh, and swipe actions.
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
    var showSearchBar by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf<ChatThread?>(null) }

    LaunchedEffect(Unit) {
        viewModel.refreshThreads()
    }

    Scaffold(
        topBar = {
            if (showSearchBar) {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { viewModel.searchThreads(it) },
                    onSearch = { /* Already searching on change */ },
                    active = true,
                    onActiveChange = { if (!it) showSearchBar = false },
                    placeholder = { Text("Search conversations...") },
                    leadingIcon = {
                        IconButton(onClick = { showSearchBar = false }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Close search")
                        }
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.searchThreads("") }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear search")
                            }
                        }
                    }
                ) {
                    // Search results (same as main list)
                }
            } else {
                TopAppBar(
                    title = { Text("Chats") },
                    actions = {
                        IconButton(onClick = { showSearchBar = true }) {
                            Icon(Icons.Filled.Search, contentDescription = "Search")
                        }
                    }
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
    }
}

/**
 * List of threads with pull-to-refresh and swipe actions.
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
    val pullRefreshState = rememberPullToRefreshState()

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
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

        if (pullRefreshState.isRefreshing) {
            LaunchedEffect(Unit) {
                onRefresh()
            }
        }

        if (!isRefreshing && pullRefreshState.isRefreshing) {
            LaunchedEffect(Unit) {
                pullRefreshState.endRefresh()
            }
        }

        PullToRefreshContainer(
            state = pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
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
