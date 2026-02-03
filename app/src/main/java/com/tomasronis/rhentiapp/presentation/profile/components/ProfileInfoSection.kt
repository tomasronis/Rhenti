package com.tomasronis.rhentiapp.presentation.profile.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tomasronis.rhentiapp.core.database.entities.CachedUser

/**
 * Profile information section with editable fields.
 */
@Composable
fun ProfileInfoSection(
    user: CachedUser?,
    isEditing: Boolean,
    onSave: (String?, String?, String?, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var firstName by remember(user) { mutableStateOf(user?.firstName ?: "") }
    var lastName by remember(user) { mutableStateOf(user?.lastName ?: "") }
    var email by remember(user) { mutableStateOf(user?.email ?: "") }
    var phone by remember(user) { mutableStateOf(user?.phone ?: "") }

    // Reset fields when editing mode changes
    LaunchedEffect(isEditing) {
        if (!isEditing) {
            firstName = user?.firstName ?: ""
            lastName = user?.lastName ?: ""
            email = user?.email ?: ""
            phone = user?.phone ?: ""
        }
    }

    Card(
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Personal Information",
                style = MaterialTheme.typography.titleMedium
            )

            // First Name
            OutlinedTextField(
                value = firstName,
                onValueChange = { firstName = it },
                label = { Text("First Name") },
                leadingIcon = {
                    Icon(Icons.Filled.Person, contentDescription = null)
                },
                enabled = isEditing,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Last Name
            OutlinedTextField(
                value = lastName,
                onValueChange = { lastName = it },
                label = { Text("Last Name") },
                leadingIcon = {
                    Icon(Icons.Filled.Person, contentDescription = null)
                },
                enabled = isEditing,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            // Email
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                leadingIcon = {
                    Icon(Icons.Filled.Email, contentDescription = null)
                },
                enabled = isEditing,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            // Phone
            OutlinedTextField(
                value = phone,
                onValueChange = { phone = it },
                label = { Text("Phone") },
                leadingIcon = {
                    Icon(Icons.Filled.Phone, contentDescription = null)
                },
                enabled = isEditing,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )

            // Save button (only show when editing)
            if (isEditing) {
                Button(
                    onClick = {
                        onSave(
                            firstName.ifBlank { null },
                            lastName.ifBlank { null },
                            email.ifBlank { null },
                            phone.ifBlank { null }
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save Changes")
                }
            }
        }
    }
}
