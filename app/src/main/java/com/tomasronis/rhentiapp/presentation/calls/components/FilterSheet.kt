package com.tomasronis.rhentiapp.presentation.calls.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.tomasronis.rhentiapp.data.calls.models.CallType

/**
 * Bottom sheet for filtering call logs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterSheet(
    selectedType: CallType?,
    onTypeSelected: (CallType?) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Text(
                text = "Filter Calls",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            HorizontalDivider()

            // Call type filter
            Text(
                text = "Call Type",
                style = MaterialTheme.typography.titleMedium
            )

            Column(
                modifier = Modifier.selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // All calls
                FilterOption(
                    label = "All Calls",
                    icon = Icons.Filled.Phone,
                    selected = selectedType == null,
                    onClick = {
                        onTypeSelected(null)
                        onDismiss()
                    }
                )

                // Incoming calls
                FilterOption(
                    label = "Incoming",
                    icon = Icons.Filled.CallReceived,
                    selected = selectedType == CallType.INCOMING,
                    onClick = {
                        onTypeSelected(CallType.INCOMING)
                        onDismiss()
                    }
                )

                // Outgoing calls
                FilterOption(
                    label = "Outgoing",
                    icon = Icons.Filled.CallMade,
                    selected = selectedType == CallType.OUTGOING,
                    onClick = {
                        onTypeSelected(CallType.OUTGOING)
                        onDismiss()
                    }
                )

                // Missed calls
                FilterOption(
                    label = "Missed",
                    icon = Icons.Filled.CallMissed,
                    selected = selectedType == CallType.MISSED,
                    onClick = {
                        onTypeSelected(CallType.MISSED)
                        onDismiss()
                    }
                )
            }

            // Clear filters button
            if (selectedType != null) {
                HorizontalDivider()

                TextButton(
                    onClick = {
                        onTypeSelected(null)
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Clear, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Clear Filters")
                }
            }

            // Bottom spacing
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Filter option composable
 */
@Composable
private fun FilterOption(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .selectable(
                selected = selected,
                onClick = onClick,
                role = Role.RadioButton
            ),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surface
        },
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                }
            )

            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.weight(1f)
            )

            RadioButton(
                selected = selected,
                onClick = null // Handled by Surface selectable
            )
        }
    }
}
