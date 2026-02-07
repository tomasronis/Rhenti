package com.tomasronis.rhentiapp.presentation.main.contacts.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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

/**
 * Contact filters modal bottom sheet.
 * Based on iOS design with toggle switch for filtering contacts.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactFiltersModal(
    hideContactsWithoutName: Boolean,
    onHideContactsWithoutNameChange: (Boolean) -> Unit,
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
                    .heightIn(max = 440.dp) // Another 20dp taller (total 40dp more than original)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(Color(0xFF1C1C1E)) // Dark background
                    .padding(start = 24.dp, end = 24.dp, top = 0.dp, bottom = 94.dp) // Another 20dp to bottom
                    .clickable(enabled = false) {} // Prevent clicks from dismissing
            ) {
                Spacer(modifier = Modifier.height(8.dp)) // Minimal top spacing

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

                Spacer(modifier = Modifier.height(12.dp)) // Match Messages tab spacing

                // Contact Filters section
                Text(
                    text = "Contact Filters",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF8E8E93), // Gray text
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(12.dp)) // Match Messages tab spacing

                // Hide Contacts Without Name toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Hide Contacts Without Name",
                        color = Color.White,
                        fontSize = 17.sp
                    )

                    Switch(
                        checked = hideContactsWithoutName,
                        onCheckedChange = onHideContactsWithoutNameChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFF34C759), // iOS green
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFF39393D) // Dark gray
                        )
                    )
                }

                Spacer(modifier = Modifier.height(24.dp)) // Extra space before reset button

                // Reset Filters button
                TextButton(
                    onClick = {
                        onHideContactsWithoutNameChange(false)
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
