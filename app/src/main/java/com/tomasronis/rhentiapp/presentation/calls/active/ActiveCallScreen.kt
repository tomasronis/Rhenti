package com.tomasronis.rhentiapp.presentation.calls.active

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.tomasronis.rhentiapp.core.voip.CallState
import com.tomasronis.rhentiapp.presentation.calls.active.components.CallControlsRow
import com.tomasronis.rhentiapp.presentation.calls.active.components.DialPad

/**
 * Active call screen showing call status and controls.
 */
@Composable
fun ActiveCallScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ActiveCallViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val callState by viewModel.callState.collectAsState()

    // Navigate back when call ends
    LaunchedEffect(callState) {
        if (callState is CallState.Ended || callState is CallState.Failed) {
            onNavigateBack()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(64.dp))

            // Call info section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Avatar - show contact avatar if available
                val avatarUrl = when (val state = callState) {
                    is CallState.Active -> state.contactAvatar
                    is CallState.Ringing -> state.contactAvatar
                    is CallState.Dialing -> state.contactAvatar
                    else -> null
                }

                val contactName = when (val state = callState) {
                    is CallState.Active -> state.contactName
                    is CallState.Ringing -> state.contactName
                    is CallState.Dialing -> state.contactName
                    else -> null
                }

                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    // Show initials or fallback person icon
                    if (!contactName.isNullOrBlank() && avatarUrl.isNullOrBlank()) {
                        // Show initials when we have a name but no avatar image
                        val initials = getInitials(contactName)
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else {
                        // Show person icon as fallback (also shown behind avatar image)
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            modifier = Modifier.size(60.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    // Overlay contact avatar if available
                    if (!avatarUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = "Contact avatar",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }

                // Contact name or phone number
                when (val state = callState) {
                    is CallState.Active -> {
                        Text(
                            text = state.contactName ?: state.phoneNumber,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )

                        // Show phone number if we have contact name
                        if (state.contactName != null) {
                            Text(
                                text = state.phoneNumber,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Call duration
                        val minutes = state.duration / 60
                        val seconds = state.duration % 60
                        Text(
                            text = String.format("%02d:%02d", minutes, seconds),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    is CallState.Ringing -> {
                        Text(
                            text = state.contactName ?: state.phoneNumber,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )

                        // Show phone number if we have contact name
                        if (state.contactName != null) {
                            Text(
                                text = state.phoneNumber,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = "Ringing...",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    is CallState.Dialing -> {
                        Text(
                            text = state.contactName ?: state.phoneNumber,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )

                        // Show phone number if we have contact name
                        if (state.contactName != null) {
                            Text(
                                text = state.phoneNumber,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Text(
                            text = "Calling...",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    else -> {
                        Text(
                            text = "Unknown",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Dialpad (if visible)
            if (uiState.isKeypadVisible) {
                DialPad(
                    onDigitPressed = { digit ->
                        viewModel.sendDigits(digit)
                    },
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                Spacer(modifier = Modifier.weight(1f))
            }

            // Call controls
            CallControlsRow(
                isMuted = uiState.isMuted,
                isSpeakerOn = uiState.isSpeakerOn,
                isKeypadVisible = uiState.isKeypadVisible,
                onMuteClick = { viewModel.toggleMute() },
                onSpeakerClick = { viewModel.toggleSpeaker() },
                onKeypadClick = {
                    if (uiState.isKeypadVisible) {
                        viewModel.hideKeypad()
                    } else {
                        viewModel.showKeypad()
                    }
                },
                onEndCallClick = { viewModel.endCall() }
            )
        }
    }
}

/**
 * Get initials from a name for the avatar fallback.
 */
private fun getInitials(name: String): String {
    val parts = name.trim().split(" ")
    return when {
        parts.size >= 2 -> "${parts[0].firstOrNull()?.uppercase() ?: ""}${parts[1].firstOrNull()?.uppercase() ?: ""}"
        parts.isNotEmpty() -> parts[0].take(2).uppercase()
        else -> ""
    }
}
