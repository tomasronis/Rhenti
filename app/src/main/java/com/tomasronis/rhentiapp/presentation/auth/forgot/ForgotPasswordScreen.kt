package com.tomasronis.rhentiapp.presentation.auth.forgot

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tomasronis.rhentiapp.data.auth.models.AuthError
import com.tomasronis.rhentiapp.presentation.auth.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    authViewModel: AuthViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by authViewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    var email by remember { mutableStateOf("") }
    val isEmailValid = email.isNotBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()

    val snackbarHostState = remember { SnackbarHostState() }

    // Handle success
    LaunchedEffect(uiState.forgotPasswordSuccess) {
        if (uiState.forgotPasswordSuccess) {
            snackbarHostState.showSnackbar(
                message = "Password reset email sent! Please check your inbox.",
                duration = SnackbarDuration.Long
            )
            authViewModel.clearForgotPasswordSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reset Password") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "Forgot Your Password?",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Enter your email address and we'll send you a link to reset your password.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Email field
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        if (isEmailValid) {
                            authViewModel.forgotPassword(email)
                        }
                    }
                ),
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading,
                isError = email.isNotBlank() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches(),
                supportingText = {
                    if (email.isNotBlank() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                        Text("Please enter a valid email address")
                    }
                }
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Send reset link button
            Button(
                onClick = { authViewModel.forgotPassword(email) },
                enabled = isEmailValid && !uiState.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Send Reset Link")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Back to login button
            TextButton(
                onClick = onNavigateBack,
                enabled = !uiState.isLoading
            ) {
                Text("Back to Sign In")
            }

            // Error display
            uiState.error?.let { error ->
                LaunchedEffect(error) {
                    snackbarHostState.showSnackbar(
                        message = error.toDisplayMessage(),
                        duration = SnackbarDuration.Short
                    )
                    authViewModel.clearError()
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

private fun AuthError.toDisplayMessage(): String {
    return when (this) {
        is AuthError.UserNotFound -> "No account found with this email"
        is AuthError.Network -> "Network error: $message"
        is AuthError.Unknown -> "An error occurred: ${throwable.message}"
        else -> "Failed to send reset link"
    }
}
