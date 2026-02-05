package com.tomasronis.rhentiapp.presentation.auth.login

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tomasronis.rhentiapp.data.auth.models.AuthError
import com.tomasronis.rhentiapp.presentation.auth.AuthViewModel
import com.tomasronis.rhentiapp.presentation.theme.*

/**
 * Redesigned login screen matching iOS design.
 * Features gradient background, glassmorphic card, and modern styling.
 */
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
    val snackbarHostState = remember { SnackbarHostState() }

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

    // Show error snackbar
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            snackbarHostState.showSnackbar(
                message = error.toDisplayMessage(),
                duration = SnackbarDuration.Short
            )
            authViewModel.clearError()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF2C3E50), // Dark blue
                        Color(0xFF34495E), // Slightly lighter blue
                        Color(0xFF2C3E50)  // Dark blue
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(60.dp))

            // Rhenti logo text
            Text(
                text = "rhenti",
                style = MaterialTheme.typography.displayLarge.copy(
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 2.sp
                ),
                color = Color.White
            )

            Spacer(modifier = Modifier.height(60.dp))

            // Glassmorphic login card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                shape = RoundedCornerShape(24.dp),
                color = GlassmorphicOverlay,
                border = androidx.compose.foundation.BorderStroke(1.dp, GlassmorphicBorder)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Email field with icon
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                "Email",
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Email,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.7f)
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email,
                            imeAction = ImeAction.Next
                        ),
                        keyboardActions = KeyboardActions(
                            onNext = { focusManager.moveFocus(FocusDirection.Down) }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.White.copy(alpha = 0.5f),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            cursorColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        enabled = !uiState.isEmailLoginLoading
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Password field with icon
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                "Password",
                                color = Color.White.copy(alpha = 0.5f)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.7f)
                            )
                        },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                    tint = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
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
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color.White.copy(alpha = 0.5f),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            cursorColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        enabled = !uiState.isEmailLoginLoading
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Sign In button (coral color)
                    Button(
                        onClick = { authViewModel.login(email, password) },
                        enabled = isFormValid && !uiState.isEmailLoginLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RhentiCoral,
                            contentColor = Color.White
                        )
                    ) {
                        if (uiState.isEmailLoginLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White
                            )
                        } else {
                            Text(
                                "Sign In",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Forgot password link
                    TextButton(
                        onClick = onNavigateToForgotPassword,
                        enabled = !uiState.isEmailLoginLoading
                    ) {
                        Text(
                            "Forgot Password?",
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // "or continue with" divider
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = Color.White.copy(alpha = 0.3f)
                )
                Text(
                    text = "or continue with",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )
                HorizontalDivider(
                    modifier = Modifier.weight(1f),
                    color = Color.White.copy(alpha = 0.3f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // SSO buttons (circular)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Google button
                Surface(
                    onClick = { authViewModel.loginWithGoogle(context as Activity) },
                    modifier = Modifier.size(64.dp),
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        Color.White.copy(alpha = 0.3f)
                    ),
                    enabled = !uiState.isGoogleLoginLoading && !uiState.isMicrosoftLoginLoading
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (uiState.isGoogleLoginLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White
                            )
                        } else {
                            Text(
                                "G",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(24.dp))

                // Microsoft button
                Surface(
                    onClick = { authViewModel.loginWithMicrosoft(context as Activity) },
                    modifier = Modifier.size(64.dp),
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        Color.White.copy(alpha = 0.3f)
                    ),
                    enabled = !uiState.isGoogleLoginLoading && !uiState.isMicrosoftLoginLoading
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        if (uiState.isMicrosoftLoginLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White
                            )
                        } else {
                            // Microsoft logo (simplified as 4 squares)
                            Icon(
                                imageVector = Icons.Filled.Window,
                                contentDescription = "Microsoft",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(60.dp))
        }

        // Snackbar at bottom
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
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
