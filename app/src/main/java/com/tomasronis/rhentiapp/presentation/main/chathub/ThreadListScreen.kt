package com.tomasronis.rhentiapp.presentation.main.chathub

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tomasronis.rhentiapp.data.chathub.models.ChatThread
import com.tomasronis.rhentiapp.presentation.main.chathub.components.*
import com.tomasronis.rhentiapp.presentation.main.components.FilterIcon
import com.tomasronis.rhentiapp.presentation.main.components.LoadingAnimation
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

                    // Filter icon with badge when any filter is active
                    IconButton(onClick = { showFiltersModal = true }) {
                        FilterIcon(
                            tint = MaterialTheme.colorScheme.onBackground,
                            showBadge = uiState.hasActiveFilters
                        )
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
                        onDeleteThread = { showDeleteDialog = it },
                        isLoadingMore = uiState.isLoadingMoreThreads,
                        hasMoreThreads = uiState.hasMoreThreads,
                        onLoadMore = { viewModel.loadMoreThreads() }
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
                unreadOnly = uiState.unreadOnly,
                onUnreadOnlyChange = { viewModel.setUnreadOnly(it) },
                noActivity = uiState.noActivity,
                onNoActivityChange = { viewModel.setNoActivity(it) },
                applicationStatus = uiState.applicationStatus,
                onApplicationStatusChange = { viewModel.setApplicationStatus(it) },
                viewingStatus = uiState.viewingStatus,
                onViewingStatusChange = { viewModel.setViewingStatus(it) },
                onResetFilters = { viewModel.resetFilters() },
                onDismiss = { showFiltersModal = false }
            )
        }
    }
}

/**
 * List of threads with swipe actions.
 * Optimized for smooth scrolling performance.
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
    modifier: Modifier = Modifier,
    isLoadingMore: Boolean = false,
    hasMoreThreads: Boolean = true,
    onLoadMore: () -> Unit = {}
) {
    Box(modifier = modifier.fillMaxSize()) {
        val listState = androidx.compose.foundation.lazy.rememberLazyListState()

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 0.dp,
                end = 0.dp,
                top = 0.dp,
                bottom = 0.dp
            ),
            verticalArrangement = Arrangement.spacedBy(0.dp) // No spacing for better performance
        ) {
            items(
                items = threads,
                key = { it.id },
                contentType = { "thread" } // Helps with composition reuse
            ) { thread ->
                // CRITICAL: Remember the lambda to avoid allocation on every recomposition
                val onClick = remember(thread.id) { { onThreadClick(thread) } }

                ThreadCard(
                    thread = thread,
                    onClick = onClick
                )
            }

            // Loading indicator at bottom when loading more
            if (isLoadingMore) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        }

        // Detect when user scrolls to bottom and load more
        LaunchedEffect(listState) {
            snapshotFlow {
                val layoutInfo = listState.layoutInfo
                val visibleItemsInfo = layoutInfo.visibleItemsInfo
                if (layoutInfo.totalItemsCount == 0) {
                    false
                } else {
                    val lastVisibleItem = visibleItemsInfo.lastOrNull()
                        ?: return@snapshotFlow false
                    lastVisibleItem.index >= layoutInfo.totalItemsCount - 3
                }
            }.collect { shouldLoadMore ->
                if (shouldLoadMore && !isLoadingMore && hasMoreThreads) {
                    onLoadMore()
                }
            }
        }
    }
}

/**
 * Thread card with swipe actions (pin/delete).
 * Optimized for smooth scrolling performance.
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
                    onPin()
                    false
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    onDelete()
                    true
                }
                else -> false
            }
        }
    )

    // Cache background calculations to avoid recomposition during scroll
    val primaryColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            // Simplified background - only compose when actually swiping
            if (dismissState.dismissDirection != SwipeToDismissBoxValue.Settled) {
                val backgroundColor = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) {
                    primaryColor
                } else {
                    errorColor
                }

                val icon = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) {
                    if (thread.isPinned) Icons.Filled.StarBorder else Icons.Filled.Star
                } else {
                    Icons.Filled.Delete
                }

                val alignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) {
                    Alignment.CenterStart
                } else {
                    Alignment.CenterEnd
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(backgroundColor)
                        .padding(horizontal = 24.dp),
                    contentAlignment = alignment
                ) {
                    Icon(
                        imageVector = icon,
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
