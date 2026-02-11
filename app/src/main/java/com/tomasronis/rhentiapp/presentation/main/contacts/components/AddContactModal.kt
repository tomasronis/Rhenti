package com.tomasronis.rhentiapp.presentation.main.contacts.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.tomasronis.rhentiapp.data.properties.models.Property

/**
 * Modal for adding a new contact.
 * Required fields: First name, Last name, Email, Property.
 * Optional fields: Lead owner, Phone number.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddContactModal(
    properties: List<Property>,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (
        firstName: String,
        lastName: String,
        email: String,
        propertyId: String,
        phone: String?,
        leadOwner: String?
    ) -> Unit,
    modifier: Modifier = Modifier
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var leadOwner by remember { mutableStateOf("") }
    var selectedProperty by remember { mutableStateOf<Property?>(null) }
    var showPropertyPicker by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

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
                    .fillMaxHeight(0.85f)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(Color(0xFF1C1C1E))
                    .clickable(enabled = false) {}
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 8.dp, top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "Cancel",
                            color = Color(0xFFE8998D),
                            fontSize = 17.sp
                        )
                    }

                    Text(
                        text = "New Contact",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    TextButton(
                        onClick = {
                            // Validate required fields
                            when {
                                firstName.isBlank() -> validationError = "First name is required"
                                lastName.isBlank() -> validationError = "Last name is required"
                                email.isBlank() -> validationError = "Email is required"
                                !email.contains("@") || !email.contains(".") -> validationError = "Please enter a valid email"
                                selectedProperty == null -> validationError = "Property is required"
                                else -> {
                                    validationError = null
                                    onSubmit(
                                        firstName.trim(),
                                        lastName.trim(),
                                        email.trim(),
                                        selectedProperty!!.id,
                                        phone.trim().ifBlank { null },
                                        leadOwner.trim().ifBlank { null }
                                    )
                                }
                            }
                        },
                        enabled = !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color(0xFFE8998D),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Save",
                                color = Color(0xFFE8998D),
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                // Validation error
                if (validationError != null) {
                    Text(
                        text = validationError!!,
                        color = Color(0xFFFF3B30),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                    )
                }

                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.1f),
                    modifier = Modifier.padding(top = 8.dp)
                )

                // Form fields
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Required fields section
                    Text(
                        text = "Required",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF8E8E93),
                        fontWeight = FontWeight.SemiBold
                    )

                    // First Name
                    ContactFormField(
                        value = firstName,
                        onValueChange = { firstName = it; validationError = null },
                        label = "First Name",
                        required = true
                    )

                    // Last Name
                    ContactFormField(
                        value = lastName,
                        onValueChange = { lastName = it; validationError = null },
                        label = "Last Name",
                        required = true
                    )

                    // Email
                    ContactFormField(
                        value = email,
                        onValueChange = { email = it; validationError = null },
                        label = "Email",
                        required = true,
                        keyboardType = KeyboardType.Email
                    )

                    // Property selector
                    Text(
                        text = "Property *",
                        color = Color(0xFF8E8E93),
                        fontSize = 14.sp
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF2C2C2E))
                            .clickable {
                                showPropertyPicker = true
                                validationError = null
                            }
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedProperty?.displayAddress ?: "Select a property",
                                color = if (selectedProperty != null) Color.White else Color(0xFF8E8E93),
                                fontSize = 16.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = Icons.Filled.ArrowDropDown,
                                contentDescription = "Select property",
                                tint = Color(0xFF8E8E93)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Optional fields section
                    Text(
                        text = "Optional",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color(0xFF8E8E93),
                        fontWeight = FontWeight.SemiBold
                    )

                    // Phone
                    ContactFormField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = "Phone Number",
                        required = false,
                        keyboardType = KeyboardType.Phone
                    )

                    // Lead Owner
                    ContactFormField(
                        value = leadOwner,
                        onValueChange = { leadOwner = it },
                        label = "Lead Owner",
                        required = false
                    )

                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }

        // Property picker bottom sheet
        if (showPropertyPicker) {
            PropertyPickerDialog(
                properties = properties,
                selectedProperty = selectedProperty,
                onPropertySelected = {
                    selectedProperty = it
                    showPropertyPicker = false
                },
                onDismiss = { showPropertyPicker = false }
            )
        }
    }
}

/**
 * A styled text field for the contact form.
 */
@Composable
private fun ContactFormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    required: Boolean,
    keyboardType: KeyboardType = KeyboardType.Text,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = if (required) "$label *" else label,
            color = Color(0xFF8E8E93),
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = {
                Text(
                    text = label,
                    color = Color(0xFF8E8E93).copy(alpha = 0.6f)
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color(0xFFE8998D),
                focusedBorderColor = Color(0xFFE8998D),
                unfocusedBorderColor = Color(0xFF39393D),
                focusedContainerColor = Color(0xFF2C2C2E),
                unfocusedContainerColor = Color(0xFF2C2C2E)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/**
 * Property picker dialog with search functionality.
 */
@Composable
private fun PropertyPickerDialog(
    properties: List<Property>,
    selectedProperty: Property?,
    onPropertySelected: (Property) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredProperties = remember(properties, searchQuery) {
        if (searchQuery.isBlank()) properties
        else properties.filter {
            it.address.contains(searchQuery, ignoreCase = true) ||
            it.unit?.contains(searchQuery, ignoreCase = true) == true ||
            it.city?.contains(searchQuery, ignoreCase = true) == true
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

            // Picker content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.7f)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    .background(Color(0xFF1C1C1E))
                    .clickable(enabled = false) {}
            ) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select Property",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF8E8E93)
                        )
                    }
                }

                // Search field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search by address...", color = Color(0xFF8E8E93)) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.Search,
                            contentDescription = null,
                            tint = Color(0xFF8E8E93)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Clear",
                                    tint = Color(0xFF8E8E93)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color(0xFFE8998D),
                        focusedBorderColor = Color(0xFFE8998D),
                        unfocusedBorderColor = Color(0xFF39393D),
                        focusedContainerColor = Color(0xFF2C2C2E),
                        unfocusedContainerColor = Color(0xFF2C2C2E)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                )

                HorizontalDivider(
                    color = Color.White.copy(alpha = 0.1f),
                    modifier = Modifier.padding(top = 8.dp)
                )

                // Properties list
                if (filteredProperties.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isNotBlank()) "No properties found" else "No properties available",
                            color = Color(0xFF8E8E93),
                            fontSize = 16.sp
                        )
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        filteredProperties.forEach { property ->
                            val isSelected = selectedProperty?.id == property.id
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onPropertySelected(property) }
                                    .background(
                                        if (isSelected) Color(0xFFE8998D).copy(alpha = 0.15f)
                                        else Color.Transparent
                                    )
                                    .padding(horizontal = 20.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = property.displayAddress,
                                        color = Color.White,
                                        fontSize = 16.sp
                                    )
                                    if (property.city != null) {
                                        Text(
                                            text = listOfNotNull(property.city, property.province).joinToString(", "),
                                            color = Color(0xFF8E8E93),
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = "Selected",
                                        tint = Color(0xFFE8998D)
                                    )
                                }
                            }
                            HorizontalDivider(
                                color = Color.White.copy(alpha = 0.05f),
                                modifier = Modifier.padding(start = 20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
