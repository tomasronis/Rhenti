package com.tomasronis.rhentiapp.presentation.main.contacts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.tomasronis.rhentiapp.data.contacts.models.Contact
import com.tomasronis.rhentiapp.presentation.main.chathub.components.ErrorStateView
import com.tomasronis.rhentiapp.presentation.main.contacts.components.*
import com.tomasronis.rhentiapp.presentation.main.components.FilterIcon
import com.tomasronis.rhentiapp.presentation.main.components.RhentiSearchBar

/**
 * Contacts list screen showing all contacts.
 * iOS-style design with large title, search bar, and alphabetical index.
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
    val hideContactsWithoutName by viewModel.hideContactsWithoutName.collectAsState()
    val listState = rememberLazyListState()
    var showFiltersModal by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.refreshContacts()
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
                        text = "Contacts",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )

                    // Filter icon with badge when filter is active
                    IconButton(onClick = { showFiltersModal = true }) {
                        FilterIcon(
                            tint = MaterialTheme.colorScheme.onBackground,
                            showBadge = hideContactsWithoutName
                        )
                    }
                }

                // Search bar
                RhentiSearchBar(
                    query = searchQuery,
                    onQueryChange = { viewModel.searchContacts(it) },
                    placeholder = "Search contacts",
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
                    // Show contacts with alphabetical index
                    Box(modifier = Modifier.fillMaxSize()) {
                        ContactsListWithIndex(
                            contacts = uiState.contacts,
                            onContactClick = onContactClick,
                            listState = listState,
                            isRefreshing = uiState.isLoading,
                            coroutineScope = coroutineScope
                        )
                    }
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

        // Filters modal
        if (showFiltersModal) {
            ContactFiltersModal(
                hideContactsWithoutName = hideContactsWithoutName,
                onHideContactsWithoutNameChange = { viewModel.setHideContactsWithoutName(it) },
                onDismiss = { showFiltersModal = false }
            )
        }
    }
}

/**
 * List of contacts with alphabetical index on the right.
 * iOS-style design with section headers and simplified contact cards.
 */
@Composable
private fun ContactsListWithIndex(
    contacts: List<Contact>,
    onContactClick: (Contact) -> Unit,
    listState: androidx.compose.foundation.lazy.LazyListState,
    isRefreshing: Boolean,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
    modifier: Modifier = Modifier
) {
    // Group contacts by first letter
    val groupedContacts = contacts.groupBy { it.sectionLetter }.toSortedMap()
    val alphabet = listOf("#") + ('A'..'Z').map { it.toString() }

    // Build a map of section letter to list index
    val sectionIndices = remember(groupedContacts) {
        val indices = mutableMapOf<String, Int>()
        var currentIndex = 0
        groupedContacts.forEach { (letter, contactsInSection) ->
            indices[letter] = currentIndex
            // +1 for header, +size for contacts
            currentIndex += 1 + contactsInSection.size
        }
        indices
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Contacts list
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 40.dp, top = 8.dp, bottom = 8.dp)
        ) {
            groupedContacts.forEach { (letter, contactsInSection) ->
                // Section header
                item(key = "header_$letter") {
                    Text(
                        text = letter,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp, horizontal = 8.dp)
                    )
                }

                // Contacts in section
                items(
                    items = contactsInSection,
                    key = { it.id }
                ) { contact ->
                    IOSContactCard(
                        contact = contact,
                        onClick = { onContactClick(contact) }
                    )
                    HorizontalDivider(
                        modifier = Modifier.padding(start = 76.dp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
                    )
                }
            }
        }

        // Alphabetical index on the right - iOS style with tap to scroll
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight() // Fill height to show all letters
                .wrapContentWidth()
                .padding(end = 4.dp, top = 0.dp, bottom = 0.dp), // No top/bottom padding
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly // Distribute evenly to fill height
        ) {
            alphabet.forEach { letter ->
                // Wider touch target for easier selection
                Box(
                    modifier = Modifier
                        .width(24.dp) // Wider touch target
                        .weight(1f) // Equal weight for each letter - fills available space
                        .clickable(enabled = groupedContacts.containsKey(letter)) {
                            // Scroll to the section
                            val index = sectionIndices[letter]
                            if (index != null) {
                                coroutineScope.launch {
                                    listState.animateScrollToItem(index)
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = letter,
                        fontSize = 8.sp, // Even smaller to ensure all 27 fit
                        color = if (groupedContacts.containsKey(letter)) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                        },
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

/**
 * iOS-style contact card with circular avatar, name, and chevron.
 */
@Composable
private fun IOSContactCard(
    contact: Contact,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Circular avatar
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondaryContainer),
            contentAlignment = Alignment.Center
        ) {
            if (contact.avatarUrl != null) {
                AsyncImage(
                    model = contact.avatarUrl,
                    contentDescription = "Avatar",
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                Text(
                    text = getInitials(contact.displayName),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        // Contact name
        Text(
            text = contact.displayName,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 17.sp,
            modifier = Modifier.weight(1f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        // Chevron icon
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = "View contact",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Get initials from display name (first letter of first two words).
 */
private fun getInitials(name: String): String {
    val parts = name.trim().split(" ")
    return when {
        parts.size >= 2 -> "${parts[0].firstOrNull()?.uppercaseChar() ?: ""}${parts[1].firstOrNull()?.uppercaseChar() ?: ""}"
        parts.isNotEmpty() -> parts[0].take(2).uppercase()
        else -> "?"
    }
}
