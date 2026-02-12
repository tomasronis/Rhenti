package com.tomasronis.rhentiapp.presentation.navigation

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.tomasronis.rhentiapp.presentation.MainActivity
import com.tomasronis.rhentiapp.presentation.auth.AuthViewModel
import com.tomasronis.rhentiapp.presentation.auth.login.LoginScreen
import com.tomasronis.rhentiapp.presentation.auth.register.RegistrationScreen
import com.tomasronis.rhentiapp.presentation.auth.forgot.ForgotPasswordScreen
import com.tomasronis.rhentiapp.presentation.main.MainTabScreen

@Composable
fun RhentiNavHost(
    navController: NavHostController,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val authState by authViewModel.uiState.collectAsState()
    val context = LocalContext.current

    NavHost(
        navController = navController,
        startDestination = if (authState.isAuthenticated) "main" else "auth"
    ) {
        navigation(startDestination = "login", route = "auth") {
            composable("login") {
                LoginScreen(
                    authViewModel = authViewModel,
                    onNavigateToRegister = { navController.navigate("register") },
                    onNavigateToForgotPassword = { navController.navigate("forgot_password") },
                    onLoginSuccess = {
                        navController.navigate("main") {
                            popUpTo("auth") { inclusive = true }
                        }
                        // Request notification permission after successful login (Phase 8)
                        (context as? MainActivity)?.requestNotificationPermission()
                    }
                )
            }

            composable("register") {
                RegistrationScreen(
                    authViewModel = authViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onRegistrationSuccess = { navController.popBackStack() }
                )
            }

            composable("forgot_password") {
                ForgotPasswordScreen(
                    authViewModel = authViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }

        composable("main") {
            MainTabScreen(authViewModel = authViewModel)
        }
    }

    // Observe auth state changes
    LaunchedEffect(authState.isAuthenticated) {
        if (!authState.isAuthenticated && navController.currentDestination?.route == "main") {
            navController.navigate("auth") {
                popUpTo(0) { inclusive = true }
            }
        }
    }
}
