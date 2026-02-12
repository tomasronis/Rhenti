package com.tomasronis.rhentiapp.presentation.main.chathub.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tomasronis.rhentiapp.presentation.theme.RhentiCoral
import java.text.SimpleDateFormat
import java.util.*

/**
 * Data class representing a time slot in the grid.
 */
private data class GridTimeSlot(
    val hour: Int,      // 24-hour format (9-21)
    val minute: Int,    // 0, 15, 30, or 45
    val displayText: String
)

/**
 * Grid-based time picker showing times from 9 AM to 9 PM in 15-minute blocks.
 * Replaces the clock-based TimePicker with a more user-friendly grid layout.
 */
@Composable
fun GridTimePicker(
    onDismiss: () -> Unit,
    onTimeSelected: (hour: Int, minute: Int) -> Unit,
    initialHour: Int = 10,
    initialMinute: Int = 0,
    modifier: Modifier = Modifier
) {
    var selectedHour by remember { mutableStateOf(initialHour) }
    var selectedMinute by remember { mutableStateOf(initialMinute) }

    // Generate time slots from 9 AM to 9 PM in 15-minute intervals
    val timeSlots = remember {
        buildList {
            for (hour in 9..21) { // 9 AM to 9 PM (24-hour format)
                val minutes = if (hour == 21) listOf(0) else listOf(0, 15, 30, 45)
                for (minute in minutes) {
                    val calendar = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, hour)
                        set(Calendar.MINUTE, minute)
                    }
                    val displayFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
                    add(GridTimeSlot(hour, minute, displayFormat.format(calendar.time)))
                }
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding(),
            contentAlignment = Alignment.BottomCenter
        ) {
            // Semi-transparent background overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.4f))
                    .clickable(onClick = onDismiss)
            )

            // Modal content
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
                    .heightIn(max = 700.dp) // Increased height for better visibility
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 24.dp)
                    .clickable(enabled = false) {} // Prevent clicks from dismissing
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "Cancel",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 17.sp
                        )
                    }

                    Text(
                        text = "Select Time",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    TextButton(
                        onClick = {
                            onTimeSelected(selectedHour, selectedMinute)
                            onDismiss()
                        }
                    ) {
                        Text(
                            text = "Done",
                            color = RhentiCoral,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Time grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 100.dp), // Extra bottom padding for nav bar
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(timeSlots) { gridTimeSlot ->
                        TimeSlotButton(
                            gridTimeSlot = gridTimeSlot,
                            isSelected = gridTimeSlot.hour == selectedHour && gridTimeSlot.minute == selectedMinute,
                            onClick = {
                                selectedHour = gridTimeSlot.hour
                                selectedMinute = gridTimeSlot.minute
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Individual time slot button in the grid.
 */
@Composable
private fun TimeSlotButton(
    gridTimeSlot: GridTimeSlot,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(2.5f) // Wide rectangular buttons
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) {
                    RhentiCoral
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
            .border(
                width = if (isSelected) 2.dp else 0.dp,
                color = if (isSelected) RhentiCoral.copy(alpha = 0.5f) else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = gridTimeSlot.displayText,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected) {
                Color.White
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            fontSize = 15.sp
        )
    }
}
