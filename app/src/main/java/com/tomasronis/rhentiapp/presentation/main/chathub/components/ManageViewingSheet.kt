package com.tomasronis.rhentiapp.presentation.main.chathub.components

import android.location.Geocoder
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.tomasronis.rhentiapp.data.chathub.models.ChatMessage
import com.tomasronis.rhentiapp.presentation.theme.Success
import com.tomasronis.rhentiapp.presentation.theme.Warning
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

/**
 * Bottom sheet modal for managing a viewing.
 * Shows full viewing details, a map with the property location,
 * a "Check-In Renter" button, and an "Edit Viewing" button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageViewingSheet(
    message: ChatMessage,
    onDismiss: () -> Unit,
    onCheckInRenter: (String) -> Unit,
    onEditViewing: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val metadata = message.metadata
    val bookingId = metadata?.bookViewingId ?: metadata?.bookingId ?: message.id
    val viewingType = metadata?.bookViewingType ?: ""
    val status = metadata?.bookViewingRequestStatus ?: when (viewingType) {
        "confirm" -> "confirmed"
        "decline" -> "declined"
        "change_request", "alternative" -> "alternative"
        else -> "pending"
    }
    val propertyAddress = metadata?.propertyAddress ?: "Address not available"

    // Parse viewing date/time
    val rawTime = metadata?.bookViewingDateTimeArr?.firstOrNull()
        ?: metadata?.bookViewingTime
        ?: metadata?.viewingTime

    val (displayDate, displayTime) = remember(rawTime) {
        parseAndFormatDateTime(rawTime)
    }

    // Geocode the address to get coordinates for the map
    val context = LocalContext.current
    var location by remember { mutableStateOf<LatLng?>(null) }
    var geocodeAttempted by remember { mutableStateOf(false) }

    LaunchedEffect(propertyAddress) {
        if (propertyAddress != "Address not available") {
            withContext(Dispatchers.IO) {
                try {
                    @Suppress("DEPRECATION")
                    val geocoder = Geocoder(context, Locale.getDefault())
                    val results = geocoder.getFromLocationName(propertyAddress, 1)
                    if (!results.isNullOrEmpty()) {
                        location = LatLng(results[0].latitude, results[0].longitude)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("ManageViewingSheet", "Geocoding failed", e)
                }
                geocodeAttempted = true
            }
        } else {
            geocodeAttempted = true
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Visibility,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "Manage Viewing",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }

            HorizontalDivider()

            // Status section
            ViewingDetailStatus(status = status, viewingType = viewingType)

            // Property address section
            ViewingDetailRow(
                icon = Icons.Filled.Home,
                label = "Property",
                value = propertyAddress
            )

            // Date section
            ViewingDetailRow(
                icon = Icons.Filled.CalendarToday,
                label = "Date",
                value = displayDate
            )

            // Time section
            ViewingDetailRow(
                icon = Icons.Filled.Schedule,
                label = "Time",
                value = displayTime
            )

            // Booking ID section
            ViewingDetailRow(
                icon = Icons.Filled.Tag,
                label = "Booking ID",
                value = bookingId
            )

            // Alternative times section (if applicable)
            if ((viewingType == "change_request" || viewingType == "alternative") &&
                metadata?.bookViewingAlternativeArr != null
            ) {
                Text(
                    text = "Proposed Alternatives",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                metadata.bookViewingAlternativeArr.forEachIndexed { index, altTimeArr ->
                    val altDisplay = altTimeArr.joinToString(" at ")
                    OutlinedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .background(
                                        color = Warning.copy(alpha = 0.15f),
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Warning
                                )
                            }
                            Text(
                                text = altDisplay,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Map section
            Text(
                text = "Location",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                shape = RoundedCornerShape(16.dp),
                tonalElevation = 1.dp
            ) {
                if (location != null) {
                    val cameraPositionState = rememberCameraPositionState {
                        position = CameraPosition.fromLatLngZoom(location!!, 15f)
                    }

                    GoogleMap(
                        modifier = Modifier.fillMaxSize(),
                        cameraPositionState = cameraPositionState,
                        uiSettings = MapUiSettings(
                            zoomControlsEnabled = false,
                            scrollGesturesEnabled = true,
                            zoomGesturesEnabled = true,
                            tiltGesturesEnabled = false,
                            rotationGesturesEnabled = false,
                            mapToolbarEnabled = false
                        )
                    ) {
                        Marker(
                            state = MarkerState(position = location!!),
                            title = propertyAddress
                        )
                    }
                } else {
                    // Fallback: show a placeholder while geocoding or if it failed
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!geocodeAttempted) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = "Loading map...",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.LocationOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "Map unavailable",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Action buttons
            Button(
                onClick = { onCheckInRenter(bookingId) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Success,
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Check-In Renter",
                    fontWeight = FontWeight.SemiBold
                )
            }

            OutlinedButton(
                onClick = { onEditViewing(bookingId) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    imageVector = Icons.Filled.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Edit Viewing",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

/**
 * Status display for the manage viewing sheet.
 */
@Composable
private fun ViewingDetailStatus(status: String, viewingType: String) {
    val (label, color) = when {
        status == "confirmed" || viewingType == "confirm" -> "Approved" to Success
        status == "declined" || viewingType == "decline" -> "Declined" to Color(0xFFFF3B30)
        status == "alternative" || viewingType == "change_request" || viewingType == "alternative" ->
            "Alternatives Proposed" to Warning
        status == "canceled" -> "Canceled" to Color(0xFF8E8E93)
        else -> "Pending" to Warning
    }

    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.outlinedCardColors(
            containerColor = color.copy(alpha = 0.08f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = color
            )
        }
    }
}

/**
 * A detail row with icon, label, and value.
 */
@Composable
private fun ViewingDetailRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String
) {
    OutlinedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/**
 * Parse a raw date/time string and return formatted (date, time) pair.
 */
private fun parseAndFormatDateTime(rawTime: String?): Pair<String, String> {
    if (rawTime == null) return "Not specified" to "Not specified"

    return try {
        val timestamp = rawTime.toLongOrNull()
        val date = if (timestamp != null && timestamp > 1000000000000L) {
            Date(timestamp)
        } else {
            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            isoFormat.isLenient = true
            try {
                isoFormat.parse(rawTime.replace("Z", "").split(".")[0])
            } catch (_: Exception) {
                try {
                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(rawTime)
                } catch (_: Exception) {
                    return rawTime to ""
                }
            }
        }

        if (date != null) {
            val dateFormat = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
            val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
            dateFormat.format(date) to timeFormat.format(date)
        } else {
            rawTime to ""
        }
    } catch (_: Exception) {
        rawTime to ""
    }
}
