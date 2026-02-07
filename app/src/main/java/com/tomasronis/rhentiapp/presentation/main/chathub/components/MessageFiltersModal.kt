package com.tomasronis.rhentiapp.presentation.main.chathub.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
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

/**
 * Message filters modal bottom sheet.
 * Based on iOS design with toggle switches and dropdowns for filtering messages.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MessageFiltersModal(
    unreadOnly: Boolean,
    onUnreadOnlyChange: (Boolean) -> Unit,
    noActivity: Boolean,
    onNoActivityChange: (Boolean) -> Unit,
    applicationStatus: String,
    onApplicationStatusChange: (String) -> Unit,
    viewingStatus: String,
    onViewingStatusChange: (String) -> Unit,
    onResetFilters: () -> Unit,
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
                .navigationBarsPadding(), // Account for Android nav bar
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
                    .heightIn(max = 680.dp) // Reduced by 20dp
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(Color(0xFF1C1C1E)) // Dark background
                    .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 24.dp) // Reduced top padding
                    .clickable(enabled = false) {} // Prevent clicks from dismissing
            ) {
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
                    color = Color.White
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

            Spacer(modifier = Modifier.height(12.dp)) // Reduced space before scrollable content

            // Scrollable content
            Column(
                modifier = Modifier
                    .weight(1f) // Take available space and allow scrolling
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Status Filters section
                Text(
                    text = "Status Filters",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF8E8E93), // Gray text
                    fontWeight = FontWeight.SemiBold
                )

                // Unread Only toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Unread Only",
                        color = Color.White,
                        fontSize = 17.sp
                    )

                    Switch(
                        checked = unreadOnly,
                        onCheckedChange = onUnreadOnlyChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF34C759), // iOS green
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFF39393D) // Dark gray
                        )
                    )
                }

                // No Activity toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "No Activity",
                        color = Color.White,
                        fontSize = 17.sp
                    )

                    Switch(
                        checked = noActivity,
                        onCheckedChange = onNoActivityChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF34C759),
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFF39393D)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(8.dp)) // Extra space before Application Status

                // Application Status section
                Text(
                    text = "Application Status",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF8E8E93),
                    fontWeight = FontWeight.SemiBold
                )

                FilterDropdown(
                    label = "Status",
                    selectedValue = applicationStatus,
                    onValueChange = onApplicationStatusChange,
                    options = listOf("All", "Pending", "Approved")
                )

                Spacer(modifier = Modifier.height(0.dp)) // Reduce space before Viewing Status

                // Viewing Status section
                Text(
                    text = "Viewing Status",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF8E8E93),
                    fontWeight = FontWeight.SemiBold
                )

                FilterDropdown(
                    label = "Status",
                    selectedValue = viewingStatus,
                    onValueChange = onViewingStatusChange,
                    options = listOf("All", "Pending", "Approved")
                )

                Spacer(modifier = Modifier.height(0.dp)) // Reduce space before Reset Filters

                // Reset Filters button
                TextButton(
                    onClick = onResetFilters,
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
 * Dropdown component for filter options.
 * Displays label on left and selected value with chevron on right.
 */
@Composable
private fun FilterDropdown(
    label: String,
    selectedValue: String,
    onValueChange: (String) -> Unit,
    options: List<String>,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = Color(0xFF2C2C2E), // Dark gray background
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable { expanded = true }
                .padding(horizontal = 16.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = Color.White,
                fontSize = 17.sp
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedValue,
                    color = Color(0xFF8E8E93),
                    fontSize = 17.sp
                )

                androidx.compose.material3.Icon(
                    imageVector = androidx.compose.material.icons.Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Expand",
                    tint = Color(0xFF8E8E93),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF2C2C2E))
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = option,
                            color = if (option == selectedValue) Color(0xFFE8998D) else Color.White,
                            fontSize = 17.sp
                        )
                    },
                    onClick = {
                        onValueChange(option)
                        expanded = false
                    },
                    colors = MenuDefaults.itemColors(
                        textColor = Color.White
                    )
                )
            }
        }
    }
}

