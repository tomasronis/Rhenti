package com.tomasronis.rhentiapp.presentation.calls

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tomasronis.rhentiapp.data.calls.models.CallLog
import com.tomasronis.rhentiapp.data.calls.models.CallType
import com.tomasronis.rhentiapp.presentation.calls.components.CallLogCard
import com.tomasronis.rhentiapp.presentation.calls.components.DialNumberDialog
import com.tomasronis.rhentiapp.presentation.calls.components.EmptyCallsState
import com.tomasronis.rhentiapp.presentation.calls.components.FilterSheet
import java.text.SimpleDateFormat
import java.util.*

/**
 * Calls screen showing call logs with search and filters.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallsScreen(
    onNavigateToActiveCall: (String) -> Unit,
    onNavigateToDetail: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier,
    viewModel: CallsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val callLogs by viewModel.callLogs.collectAsState()

    var showSearchBar by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }
    var showDialDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Load call logs on screen open
    LaunchedEffect(Unit) {
        viewModel.refreshCallLogs()
    }

    // Show error message
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(error)
            viewModel.clearError()
        }
    }

    Scaffold(
        topBar = {
            if (showSearchBar) {
                SearchBar(
                    query = uiState.searchQuery,
                    onQueryChange = { viewModel.searchCalls(it) },
                    onSearch = { /* Already searching on change */ },
                    active = true,
                    onActiveChange = { if (!it) showSearchBar = false },
                    placeholder = { Text("Search calls...") },
                    leadingIcon = {
                        IconButton(onClick = { showSearchBar = false }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Close search")
                        }
                    },
                    trailingIcon = {
                        if (uiState.searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.searchCalls("") }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Search results
                    if (callLogs.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (uiState.searchQuery.isEmpty()) "Start typing to search..." else "No results found",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        // Group calls by date
                        val groupedCalls = remember(callLogs) {
                            groupCallsByDate(callLogs)
                        }

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            groupedCalls.forEach { (dateHeader, calls) ->
                                // Date header
                                item(key = "header_$dateHeader") {
                                    Text(
                                        text = dateHeader,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                    )
                                }

                                // Call logs for this date
                                items(
                                    items = calls,
                                    key = { it.id }
                                ) { callLog ->
                                    CallLogCard(
                                        callLog = callLog,
                                        onClick = {
                                            showSearchBar = false
                                            viewModel.searchCalls("")
                                            if (onNavigateToDetail != null) {
                                                onNavigateToDetail(callLog.id)
                                            } else {
                                                onNavigateToActiveCall(callLog.contactPhone)
                                            }
                                        },
                                        onCallClick = {
                                            showSearchBar = false
                                            viewModel.searchCalls("")
                                            onNavigateToActiveCall(callLog.contactPhone)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                TopAppBar(
                    title = { Text("Calls") },
                    actions = {
                        IconButton(onClick = { showSearchBar = true }) {
                            Icon(Icons.Filled.Search, contentDescription = "Search")
                        }
                        IconButton(onClick = { showFilterSheet = true }) {
                            Icon(
                                imageVector = if (uiState.selectedFilter != null ||
                                    uiState.startDate != null) {
                                    Icons.Filled.FilterAltOff
                                } else {
                                    Icons.Filled.FilterAlt
                                },
                                contentDescription = "Filter"
                            )
                        }
                    }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            if (!showSearchBar) {
                FloatingActionButton(
                    onClick = { showDialDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Filled.Dialpad, contentDescription = "Dial number")
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading && callLogs.isEmpty() -> {
                    // Initial loading
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                callLogs.isEmpty() -> {
                    // No calls
                    EmptyCallsState()
                }
                else -> {
                    // Show calls with pull-to-refresh
                    PullToRefreshBox(
                        isRefreshing = uiState.isRefreshing,
                        onRefresh = { viewModel.refreshCallLogs() }
                    ) {
                        CallLogsList(
                            callLogs = callLogs,
                            onCallClick = { phoneNumber ->
                                // Navigate to active call screen
                                onNavigateToActiveCall(phoneNumber)
                            },
                            onDetailClick = { callId ->
                                // Navigate to call detail screen
                                onNavigateToDetail?.invoke(callId)
                            },
                            selectedFilter = uiState.selectedFilter
                        )
                    }
                }
            }

            // Active filter chips
            if (uiState.selectedFilter != null || uiState.startDate != null) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.small
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Filters active",
                            style = MaterialTheme.typography.bodySmall
                        )
                        TextButton(
                            onClick = { viewModel.clearFilters() }
                        ) {
                            Text("Clear")
                        }
                    }
                }
            }
        }
    }

    // Filter sheet
    if (showFilterSheet) {
        FilterSheet(
            selectedType = uiState.selectedFilter,
            onTypeSelected = { viewModel.filterByType(it) },
            onDismiss = { showFilterSheet = false }
        )
    }

    // Dial number dialog
    if (showDialDialog) {
        DialNumberDialog(
            onDismiss = { showDialDialog = false },
            onDial = { phoneNumber ->
                onNavigateToActiveCall(phoneNumber)
            }
        )
    }
}

/**
 * Call logs list with date grouping
 */
@Composable
private fun CallLogsList(
    callLogs: List<CallLog>,
    onCallClick: (String) -> Unit,
    onDetailClick: ((String) -> Unit)?,
    selectedFilter: CallType?,
    modifier: Modifier = Modifier
) {
    // Group calls by date
    val groupedCalls = remember(callLogs) {
        groupCallsByDate(callLogs)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        groupedCalls.forEach { (dateHeader, calls) ->
            // Date header
            item(key = "header_$dateHeader") {
                Text(
                    text = dateHeader,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Call logs for this date
            items(
                items = calls,
                key = { it.id }
            ) { callLog ->
                CallLogCard(
                    callLog = callLog,
                    onClick = {
                        if (onDetailClick != null) {
                            onDetailClick(callLog.id)
                        } else {
                            onCallClick(callLog.contactPhone)
                        }
                    },
                    onCallClick = { onCallClick(callLog.contactPhone) }
                )
            }
        }
    }
}

/**
 * Group call logs by date (Today, Yesterday, This Week, etc.)
 */
private fun groupCallsByDate(callLogs: List<CallLog>): Map<String, List<CallLog>> {
    val calendar = Calendar.getInstance()
    val today = calendar.apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val yesterday = calendar.apply {
        add(Calendar.DAY_OF_YEAR, -1)
    }.timeInMillis

    val thisWeekStart = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val dateFormat = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())

    return callLogs.groupBy { callLog ->
        when {
            callLog.timestamp >= today -> "Today"
            callLog.timestamp >= yesterday -> "Yesterday"
            callLog.timestamp >= thisWeekStart -> "This Week"
            else -> dateFormat.format(Date(callLog.timestamp))
        }
    }
}
