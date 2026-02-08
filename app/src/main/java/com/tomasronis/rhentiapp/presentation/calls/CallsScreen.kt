package com.tomasronis.rhentiapp.presentation.calls

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tomasronis.rhentiapp.data.calls.models.CallLog
import com.tomasronis.rhentiapp.data.calls.models.CallType
import com.tomasronis.rhentiapp.presentation.calls.components.CallLogCard
import com.tomasronis.rhentiapp.presentation.calls.components.DialNumberDialog
import com.tomasronis.rhentiapp.presentation.calls.components.EmptyCallsState
import com.tomasronis.rhentiapp.presentation.calls.components.CallFiltersModal
import com.tomasronis.rhentiapp.presentation.main.components.FilterIcon
import com.tomasronis.rhentiapp.presentation.main.components.LoadingAnimation
import com.tomasronis.rhentiapp.presentation.main.components.RhentiSearchBar
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
    val searchQuery = uiState.searchQuery

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
                        text = "Calls",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    // Filter icon with badge when filter is active
                    IconButton(onClick = { showFilterSheet = true }) {
                        FilterIcon(
                            tint = MaterialTheme.colorScheme.onBackground,
                            showBadge = uiState.selectedFilter != null
                        )
                    }
                }

                // Search bar
                RhentiSearchBar(
                    query = searchQuery,
                    onQueryChange = { viewModel.searchCalls(it) },
                    placeholder = "Search by name or number",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showDialDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Filled.Dialpad, contentDescription = "Dial number")
            }
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                callLogs.isEmpty() -> {
                    // No calls
                    EmptyCallsState()
                }
                else -> {
                    // Show calls list
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
    }

    // Filter modal
    if (showFilterSheet) {
        CallFiltersModal(
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
            // Date header - iOS style with more visual separation
            item(key = "header_$dateHeader") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp, bottom = 8.dp)
                ) {
                    Text(
                        text = dateHeader,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
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

            // Add spacing after each date group for visual separation
            item(key = "spacer_$dateHeader") {
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

/**
 * Group call logs by date - iOS style format (e.g., "January 30, 2026")
 */
private fun groupCallsByDate(callLogs: List<CallLog>): Map<String, List<CallLog>> {
    val dateFormat = SimpleDateFormat("MMMM d, yyyy", Locale.getDefault())

    return callLogs.groupBy { callLog ->
        dateFormat.format(Date(callLog.timestamp))
    }
}
