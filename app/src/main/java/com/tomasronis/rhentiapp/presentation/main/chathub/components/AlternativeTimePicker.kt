package com.tomasronis.rhentiapp.presentation.main.chathub.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

/**
 * Bottom sheet for proposing alternative viewing times.
 * Allows selecting up to 3 date/time slots.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlternativeTimePicker(
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTimes by remember { mutableStateOf<List<String>>(emptyList()) }
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Text(
                text = "Propose Alternative Times",
                style = MaterialTheme.typography.titleLarge
            )

            Text(
                text = "Select up to 3 alternative viewing times.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Divider()

            // Time slots
            selectedTimes.forEach { time ->
                TimeSlotChip(
                    time = time,
                    onRemove = {
                        selectedTimes = selectedTimes - time
                    }
                )
            }

            // Add time button
            if (selectedTimes.size < 3) {
                OutlinedButton(
                    onClick = {
                        // Add a new time slot (for now, just use current time + increment)
                        // In a real app, this would open a date/time picker
                        val cal = Calendar.getInstance()
                        cal.add(Calendar.DAY_OF_MONTH, selectedTimes.size + 1)
                        cal.set(Calendar.HOUR_OF_DAY, 10)
                        cal.set(Calendar.MINUTE, 0)
                        val format = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
                        selectedTimes = selectedTimes + format.format(cal.time)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Time Slot")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        if (selectedTimes.isNotEmpty()) {
                            onConfirm(selectedTimes)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = selectedTimes.isNotEmpty()
                ) {
                    Text("Send")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Chip displaying a time slot with remove button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeSlotChip(
    time: String,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    InputChip(
        selected = true,
        onClick = { },
        label = { Text(time) },
        trailingIcon = {
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Remove",
                    modifier = Modifier.size(16.dp)
                )
            }
        },
        modifier = modifier
    )
}
