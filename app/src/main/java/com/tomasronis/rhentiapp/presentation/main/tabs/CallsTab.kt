package com.tomasronis.rhentiapp.presentation.main.tabs

import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.tomasronis.rhentiapp.presentation.calls.CallDetailScreen
import com.tomasronis.rhentiapp.presentation.calls.CallsScreen
import com.tomasronis.rhentiapp.presentation.calls.CallsViewModel

/**
 * Calls tab wrapper for the main tab screen with navigation.
 */
@Composable
fun CallsTab(
    onStartCall: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val viewModel: CallsViewModel = hiltViewModel()
    val callLogs by viewModel.callLogs.collectAsState()

    NavHost(
        navController = navController,
        startDestination = "calls_list",
        modifier = modifier
    ) {
        composable("calls_list") {
            CallsScreen(
                onNavigateToActiveCall = { phoneNumber ->
                    onStartCall(phoneNumber)
                },
                onNavigateToDetail = { callId ->
                    navController.navigate("call_detail/$callId")
                },
                modifier = modifier
            )
        }

        composable("call_detail/{callId}") { backStackEntry ->
            val callId = backStackEntry.arguments?.getString("callId")
            val callLog = callLogs.firstOrNull { it.id == callId }

            if (callLog != null) {
                CallDetailScreen(
                    callLog = callLog,
                    onNavigateBack = { navController.popBackStack() },
                    onCallClick = { phoneNumber: String ->
                        onStartCall(phoneNumber)
                    }
                )
            }
        }
    }
}
