package com.tomasronis.rhentiapp.presentation.calls.active.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tomasronis.rhentiapp.presentation.theme.RhentiCoral

/**
 * Call controls row with mute, speaker, keypad, and end call buttons.
 */
@Composable
fun CallControlsRow(
    isMuted: Boolean,
    isSpeakerOn: Boolean,
    isKeypadVisible: Boolean,
    onMuteClick: () -> Unit,
    onSpeakerClick: () -> Unit,
    onKeypadClick: () -> Unit,
    onEndCallClick: () -> Unit,
    modifier: Modifier = Modifier,
    darkMode: Boolean = false
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top row: Mute, Speaker, Keypad
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            ControlButton(
                icon = if (isMuted) Icons.Filled.MicOff else Icons.Filled.Mic,
                label = "Mute",
                isActive = isMuted,
                onClick = onMuteClick,
                darkMode = darkMode
            )

            ControlButton(
                icon = if (isSpeakerOn) Icons.Filled.VolumeUp else Icons.Filled.VolumeDown,
                label = "Speaker",
                isActive = isSpeakerOn,
                onClick = onSpeakerClick,
                darkMode = darkMode
            )

            ControlButton(
                icon = Icons.Filled.Dialpad,
                label = "Keypad",
                isActive = isKeypadVisible,
                onClick = onKeypadClick,
                darkMode = darkMode
            )
        }

        // Bottom row: End call button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            FloatingActionButton(
                onClick = onEndCallClick,
                modifier = Modifier.size(72.dp),
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            ) {
                Icon(
                    imageVector = Icons.Filled.CallEnd,
                    contentDescription = "End Call",
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

/**
 * Individual control button
 */
@Composable
private fun ControlButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    darkMode: Boolean = false
) {
    // Colors adapt based on dark mode and active state
    val activeContainerColor = if (darkMode) RhentiCoral else MaterialTheme.colorScheme.primary
    val activeContentColor = if (darkMode) Color.White else MaterialTheme.colorScheme.onPrimary
    val inactiveContainerColor = if (darkMode) Color.White.copy(alpha = 0.12f) else MaterialTheme.colorScheme.surfaceVariant
    val inactiveContentColor = if (darkMode) Color.White.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurfaceVariant
    val labelColor = if (darkMode) Color.White.copy(alpha = 0.7f) else {
        if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        FilledIconButton(
            onClick = onClick,
            modifier = Modifier.size(56.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = if (isActive) activeContainerColor else inactiveContainerColor,
                contentColor = if (isActive) activeContentColor else inactiveContentColor
            )
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                modifier = Modifier.size(24.dp)
            )
        }

        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = labelColor
        )
    }
}
