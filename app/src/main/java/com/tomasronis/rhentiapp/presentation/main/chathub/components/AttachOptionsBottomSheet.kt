package com.tomasronis.rhentiapp.presentation.main.chathub.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Attachment type options for the attach menu.
 */
enum class AttachmentType {
    CAMERA,
    PHOTOS,
    PDF_FILE,
    BOOK_VIEWING_LINK,
    APPLICATION_LINK
}

/**
 * Bottom sheet showing attachment options matching iOS design.
 * Displays: Camera, Photos, PDF File, Book a Viewing Link, Application Link
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttachOptionsBottomSheet(
    onDismiss: () -> Unit,
    onOptionSelected: (AttachmentType) -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            // Camera
            AttachOptionItem(
                icon = Icons.Filled.CameraAlt,
                label = "Camera",
                onClick = {
                    onOptionSelected(AttachmentType.CAMERA)
                    onDismiss()
                }
            )

            HorizontalDivider(
                modifier = Modifier.padding(start = 80.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )

            // Photos
            AttachOptionItem(
                icon = Icons.Filled.Image,
                label = "Photos",
                onClick = {
                    onOptionSelected(AttachmentType.PHOTOS)
                    onDismiss()
                }
            )

            HorizontalDivider(
                modifier = Modifier.padding(start = 80.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )

            // PDF File
            AttachOptionItem(
                icon = Icons.Filled.InsertDriveFile,
                label = "PDF File",
                onClick = {
                    onOptionSelected(AttachmentType.PDF_FILE)
                    onDismiss()
                }
            )

            HorizontalDivider(
                modifier = Modifier.padding(start = 80.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )

            // Book a Viewing Link
            AttachOptionItem(
                icon = Icons.Filled.CalendarMonth,
                label = "Book a Viewing Link",
                onClick = {
                    onOptionSelected(AttachmentType.BOOK_VIEWING_LINK)
                    onDismiss()
                }
            )

            HorizontalDivider(
                modifier = Modifier.padding(start = 80.dp),
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )

            // Application Link
            AttachOptionItem(
                icon = Icons.Filled.Description,
                label = "Application Link",
                onClick = {
                    onOptionSelected(AttachmentType.APPLICATION_LINK)
                    onDismiss()
                }
            )

            // Bottom spacing
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Single option item in the attach menu.
 */
@Composable
private fun AttachOptionItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon with circular background
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Label
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Normal
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
