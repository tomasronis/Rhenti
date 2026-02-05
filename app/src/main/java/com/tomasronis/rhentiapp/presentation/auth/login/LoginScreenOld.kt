package com.tomasronis.rhentiapp.presentation.auth.login

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.tomasronis.rhentiapp.data.auth.models.AuthError
import com.tomasronis.rhentiapp.presentation.auth.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    authViewModel: AuthViewModel,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    val uiState by authViewModel.uiState.collectAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val isFormValid = email.isNotBlank() && password.isNotBlank()

    // Navigate on successful login
    LaunchedEffect(uiState.isAuthenticated) {
        if (uiState.isAuthenticated) {
            onLoginSuccess()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sign In") }
            )
        },
        snackbarHost = {
            SnackbarHost(
                hostState = remember { SnackbarHostState() }.apply {
                    LaunchedEffect(uiState.error) {
                        uiState.error?.let { error ->
                            showSnackbar(
                                message = error.toDisplayMessage(),
                                duration = SnackbarDuration.Short
                            )
                            authViewModel.clearError()
                        }
                    }
                }
            )
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
            // Logo or branding space
            Spacer(modifier = Modifier.height(48.dp))

            Text(
                text = "Welcome to Rhenti",
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Sign in to continue",
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
                    imeAction = ImeAction.Next
                ),
                keyboardActions = KeyboardActions(
                    onNext = { focusManager.moveFocus(FocusDirection.Down) }
                ),
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isEmailLoginLoading
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Password field
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Password,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        focusManager.clearFocus()
                        if (isFormValid) {
                            authViewModel.login(email, password)
                        }
                    }
                ),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(
                            imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = if (passwordVisible) "Hide password" else "Show password"
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isEmailLoginLoading
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Forgot password link
            TextButton(
                onClick = onNavigateToForgotPassword,
                modifier = Modifier.align(Alignment.End),
                enabled = !uiState.isEmailLoginLoading
            ) {
                Text("Forgot Password?")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sign in button
            Button(
                onClick = { authViewModel.login(email, password) },
                enabled = isFormValid && !uiState.isEmailLoginLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (uiState.isEmailLoginLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Sign In")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Divider with "OR"
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f))
                Text(
                    text = "OR",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider(modifier = Modifier.weight(1f))
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Google Sign-In button
            OutlinedButton(
                onClick = { authViewModel.loginWithGoogle(context as Activity) },
                enabled = !uiState.isGoogleLoginLoading && !uiState.isMicrosoftLoginLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (uiState.isGoogleLoginLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text("Continue with Google")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Microsoft Sign-In button
            OutlinedButton(
                onClick = { authViewModel.loginWithMicrosoft(context as Activity) },
                enabled = !uiState.isGoogleLoginLoading && !uiState.isMicrosoftLoginLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (uiState.isMicrosoftLoginLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text("Continue with Microsoft")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Register link
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Don't have an account?",
                    style = MaterialTheme.typography.bodyMedium
                )
                TextButton(
                    onClick = onNavigateToRegister,
                    enabled = !uiState.isEmailLoginLoading && !uiState.isGoogleLoginLoading && !uiState.isMicrosoftLoginLoading
                ) {
                    Text("Register")
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

private fun AuthError.toDisplayMessage(): String {
    return when (this) {
        is AuthError.InvalidCredentials -> "Invalid email or password"
        is AuthError.Network -> "Network error: $message"
        is AuthError.SSOCancelled -> "Sign-in cancelled"
        is AuthError.SSOFailed -> "Sign-in failed: $message"
        is AuthError.UserNotFound -> "User not found"
        is AuthError.EmailAlreadyExists -> "Email already exists"
        is AuthError.Unknown -> "An error occurred: ${throwable.message}"
    }
}
