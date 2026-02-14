package com.tomasronis.rhentiapp.presentation.calls.active

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.tomasronis.rhentiapp.core.voip.CallState
import com.tomasronis.rhentiapp.presentation.calls.active.components.CallControlsRow
import com.tomasronis.rhentiapp.presentation.calls.active.components.DialPad
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CallEnd
import com.tomasronis.rhentiapp.presentation.theme.RhentiCoral
import com.tomasronis.rhentiapp.presentation.theme.RhentiCoralDark
import com.tomasronis.rhentiapp.presentation.theme.RhentiCoralLight
import com.tomasronis.rhentiapp.presentation.theme.Success

// Call screen brand colors
private val CallScreenDark = Color(0xFF1A1A2E)
private val CallScreenDarkAccent = Color(0xFF16213E)
private val CoralGlow = Color(0x33E8998D) // 20% opacity coral

/**
 * Active call screen showing call status and controls.
 * Branded with Rhenti coral accents and logo.
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
            .background(
                Brush.verticalGradient(
                    colors = listOf(CallScreenDark, CallScreenDarkAccent, Color(0xFF0F3460))
                )
            )
            .drawBehind {
                // Subtle coral decorative circles in the background
                drawCircle(
                    color = CoralGlow,
                    radius = size.width * 0.45f,
                    center = Offset(size.width * 0.85f, size.height * 0.12f)
                )
                drawCircle(
                    color = Color(0x1AE8998D), // 10% opacity
                    radius = size.width * 0.35f,
                    center = Offset(size.width * 0.1f, size.height * 0.85f)
                )
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Top: Rhenti logo
            Text(
                text = "rhenti",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 2.sp
                ),
                color = RhentiCoral.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 16.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Call info section
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Avatar with coral ring
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
                        .size(130.dp)
                        .border(
                            width = 3.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(RhentiCoral, RhentiCoralLight, RhentiCoralDark)
                            ),
                            shape = CircleShape
                        )
                        .padding(6.dp)
                        .clip(CircleShape)
                        .background(CallScreenDarkAccent),
                    contentAlignment = Alignment.Center
                ) {
                    if (!contactName.isNullOrBlank() && avatarUrl.isNullOrBlank()) {
                        val initials = getInitials(contactName)
                        Text(
                            text = initials,
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = RhentiCoral
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            modifier = Modifier.size(60.dp),
                            tint = RhentiCoral.copy(alpha = 0.6f)
                        )
                    }

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
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        if (state.contactName != null) {
                            Text(
                                text = state.phoneNumber,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }

                        // Call duration with coral accent
                        val minutes = state.duration / 60
                        val seconds = state.duration % 60
                        Text(
                            text = String.format("%02d:%02d", minutes, seconds),
                            style = MaterialTheme.typography.titleLarge,
                            color = RhentiCoral
                        )
                    }
                    is CallState.Ringing -> {
                        Text(
                            text = state.contactName ?: state.phoneNumber,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        if (state.contactName != null) {
                            Text(
                                text = state.phoneNumber,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }

                        Text(
                            text = "Incoming Call",
                            style = MaterialTheme.typography.titleMedium,
                            color = RhentiCoral
                        )
                    }
                    is CallState.Dialing -> {
                        Text(
                            text = state.contactName ?: state.phoneNumber,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )

                        if (state.contactName != null) {
                            Text(
                                text = state.phoneNumber,
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White.copy(alpha = 0.6f)
                            )
                        }

                        Text(
                            text = "Calling...",
                            style = MaterialTheme.typography.titleMedium,
                            color = RhentiCoral
                        )
                    }
                    else -> {
                        Text(
                            text = "Unknown",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }

            // Controls section
            val isIncomingRinging = callState is CallState.Ringing

            if (isIncomingRinging) {
                Spacer(modifier = Modifier.weight(1f))

                // Accept/Decline buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Decline button
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        FilledIconButton(
                            onClick = { viewModel.rejectIncomingCall() },
                            modifier = Modifier.size(72.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = Color(0xFFFF3B30),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CallEnd,
                                contentDescription = "Decline",
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Decline",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }

                    // Accept button
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        FilledIconButton(
                            onClick = { viewModel.acceptIncomingCall() },
                            modifier = Modifier.size(72.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(
                                containerColor = Success,
                                contentColor = Color.White
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Call,
                                contentDescription = "Accept",
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Accept",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(100.dp))
            } else {
                // Active/Dialing call controls
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
                    onEndCallClick = { viewModel.endCall() },
                    darkMode = true
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
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
