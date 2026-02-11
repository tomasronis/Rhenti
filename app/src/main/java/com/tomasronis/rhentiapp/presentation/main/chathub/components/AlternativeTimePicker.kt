package com.tomasronis.rhentiapp.presentation.main.chathub.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.*

/**
 * Bottom sheet for proposing alternative viewing times.
 * Allows selecting up to 3 date/time slots using Material 3 date and time pickers.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlternativeTimePicker(
    onDismiss: () -> Unit,
    onConfirm: (List<String>) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTimes by remember { mutableStateOf<List<Long>>(emptyList()) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // State for the date picker dialog
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis() + 86400_000L // Tomorrow
    )

    // State for the time picker dialog
    var showTimePicker by remember { mutableStateOf(false) }
    var pendingDateMillis by remember { mutableStateOf<Long?>(null) }
    val timePickerState = rememberTimePickerState(
        initialHour = 10,
        initialMinute = 0,
        is24Hour = false
    )

    val displayFormat = remember { SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault()) }
    val isoFormat = remember {
        SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
    }

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
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "Select up to 3 alternative viewing times.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider()

            // Time slots
            selectedTimes.forEachIndexed { index, timestamp ->
                TimeSlotChip(
                    time = displayFormat.format(Date(timestamp)),
                    onRemove = {
                        selectedTimes = selectedTimes.toMutableList().also { it.removeAt(index) }
                    }
                )
            }

            // Add time button - opens date picker first
            if (selectedTimes.size < 3) {
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
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
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        if (selectedTimes.isNotEmpty()) {
                            // Convert timestamps to ISO 8601 strings for the API
                            val isoStrings = selectedTimes.map { isoFormat.format(Date(it)) }
                            onConfirm(isoStrings)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    enabled = selectedTimes.isNotEmpty(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Send")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Date picker dialog
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { dateMillis ->
                            pendingDateMillis = dateMillis
                            showDatePicker = false
                            showTimePicker = true
                        }
                    }
                ) {
                    Text("Next")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                title = {
                    Text(
                        text = "Select viewing date",
                        modifier = Modifier.padding(start = 24.dp, top = 16.dp)
                    )
                }
            )
        }
    }

    // Time picker dialog
    if (showTimePicker && pendingDateMillis != null) {
        AlertDialog(
            onDismissRequest = {
                showTimePicker = false
                pendingDateMillis = null
            },
            title = { Text("Select viewing time") },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(state = timePickerState)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingDateMillis?.let { dateMillis ->
                            // Combine date and time
                            val calendar = Calendar.getInstance().apply {
                                timeInMillis = dateMillis
                                set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                                set(Calendar.MINUTE, timePickerState.minute)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            selectedTimes = selectedTimes + calendar.timeInMillis
                        }
                        showTimePicker = false
                        pendingDateMillis = null
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showTimePicker = false
                        pendingDateMillis = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
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
