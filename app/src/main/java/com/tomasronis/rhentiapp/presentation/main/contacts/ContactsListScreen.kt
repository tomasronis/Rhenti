package com.tomasronis.rhentiapp.presentation.main.contacts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tomasronis.rhentiapp.data.contacts.models.Contact
import com.tomasronis.rhentiapp.presentation.main.chathub.components.ErrorStateView
import com.tomasronis.rhentiapp.presentation.main.contacts.components.*

/**
 * Contacts list screen showing all contacts.
 * Includes search and refresh functionality.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactsListScreen(
    onContactClick: (Contact) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ContactsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    var showSearchBar by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.refreshContacts()
    }

    Scaffold(
        topBar = {
            if (showSearchBar) {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { viewModel.searchContacts(it) },
                    onSearch = { /* Already searching on change */ },
                    active = true,
                    onActiveChange = { if (!it) showSearchBar = false },
                    placeholder = { Text("Search contacts...") },
                    leadingIcon = {
                        IconButton(onClick = { showSearchBar = false }) {
                            Icon(Icons.Filled.ArrowBack, contentDescription = "Close search")
                        }
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { viewModel.searchContacts("") }) {
                                Icon(Icons.Filled.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Search results
                    if (uiState.contacts.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (searchQuery.isEmpty()) "Start typing to search..." else "No results found",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        // Group contacts by first letter
                        val groupedContacts = uiState.contacts.groupBy { it.sectionLetter }.toSortedMap()

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            groupedContacts.forEach { (letter, contactsInSection) ->
                                // Section header
                                item(key = "header_$letter") {
                                    Text(
                                        text = letter,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.padding(vertical = 8.dp)
                                    )
                                }

                                // Contacts in section
                                items(
                                    items = contactsInSection,
                                    key = { it.id }
                                ) { contact ->
                                    ContactCard(
                                        contact = contact,
                                        onClick = {
                                            showSearchBar = false
                                            viewModel.searchContacts("")
                                            onContactClick(contact)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                TopAppBar(
                    title = { Text("Contacts") },
                    actions = {
                        IconButton(onClick = { viewModel.refreshContacts() }) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                        }
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
                uiState.isLoading && uiState.contacts.isEmpty() -> {
                    // Initial loading
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.error != null && uiState.contacts.isEmpty() -> {
                    // Error with no cached data
                    ErrorStateView(
                        message = uiState.error ?: "An error occurred",
                        onRetry = { viewModel.refreshContacts() }
                    )
                }
                uiState.contacts.isEmpty() -> {
                    // No contacts
                    EmptyContactsView()
                }
                else -> {
                    // Show contacts
                    ContactsList(
                        contacts = uiState.contacts,
                        onContactClick = onContactClick,
                        isRefreshing = uiState.isLoading
                    )
                }
            }

            // Show error snackbar if there's an error but we have cached data
            if (uiState.error != null && uiState.contacts.isNotEmpty()) {
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

            // Show loading indicator while refreshing
            if (uiState.isLoading && uiState.contacts.isNotEmpty()) {
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                )
            }
        }
    }
}

/**
 * List of contacts grouped by section letter.
 */
@Composable
private fun ContactsList(
    contacts: List<Contact>,
    onContactClick: (Contact) -> Unit,
    isRefreshing: Boolean,
    modifier: Modifier = Modifier
) {
    // Group contacts by first letter of last name
    val groupedContacts = contacts.groupBy { it.sectionLetter }
        .toSortedMap()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        groupedContacts.forEach { (letter, contactsInSection) ->
            // Section header
            item(key = "header_$letter") {
                Text(
                    text = letter,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            // Contacts in section
            items(
                items = contactsInSection,
                key = { it.id }
            ) { contact ->
                ContactCard(
                    contact = contact,
                    onClick = { onContactClick(contact) }
                )
            }
        }
    }
}
