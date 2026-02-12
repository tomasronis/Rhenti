package com.tomasronis.rhentiapp.presentation.calls.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.tomasronis.rhentiapp.data.calls.models.CallType

/**
 * Call filters modal bottom sheet.
 * Based on iOS design matching Messages and Contacts tabs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallFiltersModal(
    selectedType: CallType?,
    onTypeSelected: (CallType?) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
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
                    .heightIn(max = 480.dp) // 40dp taller
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(start = 24.dp, end = 24.dp, top = 0.dp, bottom = 94.dp)
                    .clickable(enabled = false) {} // Prevent clicks from dismissing
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(48.dp)) // Balance the Done button

                    Text(
                        text = "Filters",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "Done",
                            color = Color(0xFFE8998D), // Coral color
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Scrollable content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(0.dp)
                ) {
                    // Call Type section
                    Text(
                        text = "Call Type",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // All Calls option
                    CallTypeOption(
                        label = "All Calls",
                        selected = selectedType == null,
                        onClick = {
                            onTypeSelected(null)
                            // Don't dismiss - let user continue selecting
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Incoming option
                    CallTypeOption(
                        label = "Incoming",
                        selected = selectedType == CallType.INCOMING,
                        onClick = {
                            onTypeSelected(CallType.INCOMING)
                            // Don't dismiss - let user continue selecting
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Outgoing option
                    CallTypeOption(
                        label = "Outgoing",
                        selected = selectedType == CallType.OUTGOING,
                        onClick = {
                            onTypeSelected(CallType.OUTGOING)
                            // Don't dismiss - let user continue selecting
                        }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Missed option
                    CallTypeOption(
                        label = "Missed",
                        selected = selectedType == CallType.MISSED,
                        onClick = {
                            onTypeSelected(CallType.MISSED)
                            // Don't dismiss - let user continue selecting
                        }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Reset Filters button
                    TextButton(
                        onClick = {
                            onTypeSelected(null)
                            // Don't dismiss - let user continue
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Reset Filters",
                            color = Color(0xFFFF3B30), // iOS red
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Call type option row with iOS styling and toggle switch
 */
@Composable
private fun CallTypeOption(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 0.dp, vertical = 0.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 17.sp
        )

        // iOS-style toggle switch
        Switch(
            checked = selected,
            onCheckedChange = { onClick() },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF34C759), // iOS green
                uncheckedThumbColor = Color.White,
                uncheckedTrackColor = Color(0xFF39393D) // Dark gray
            )
        )
    }
}
