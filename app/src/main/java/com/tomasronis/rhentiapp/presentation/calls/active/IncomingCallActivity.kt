package com.tomasronis.rhentiapp.presentation.calls.active

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.content.ContextCompat
import com.tomasronis.rhentiapp.BuildConfig
import com.tomasronis.rhentiapp.core.voip.CallState
import com.tomasronis.rhentiapp.core.voip.IncomingCallService
import com.tomasronis.rhentiapp.core.voip.TwilioManager
import com.tomasronis.rhentiapp.presentation.theme.RhentiAppTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Dedicated full-screen activity for incoming calls.
 * Shows over lock screen regardless of app/phone state.
 * No bottom navigation or other app chrome - just the call UI.
 */
@AndroidEntryPoint
class IncomingCallActivity : ComponentActivity() {

    @Inject
    lateinit var twilioManager: TwilioManager

    companion object {
        private const val TAG = "IncomingCallActivity"
        const val ACTION_INCOMING_CALL = "com.rhentimobile.INCOMING_CALL"
        const val ACTION_ANSWER_CALL = "com.rhentimobile.ANSWER_CALL"
    }

    private val micPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val invite = IncomingCallService.activeCallInvite
                ?: twilioManager.getCallInvite()
            if (invite != null) {
                twilioManager.acceptIncomingCall(invite)
                IncomingCallService.accept(this)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupLockScreenFlags()
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // Tell the service to demote the notification to silent so the user
        // doesn't see both this full-screen activity AND a heads-up popup.
        IncomingCallService.onActivityShown(this)

        if (savedInstanceState == null) {
            handleCallIntent(intent)
        }

        setContent {
            RhentiAppTheme {
                val callState by twilioManager.callState.collectAsState()

                // Finish activity when call ends or no call is active
                LaunchedEffect(callState) {
                    when (callState) {
                        is CallState.Idle -> finish()
                        is CallState.Ended, is CallState.Failed -> {
                            kotlinx.coroutines.delay(1500)
                            finish()
                        }
                        else -> {}
                    }
                }

                ActiveCallScreen(
                    onNavigateBack = { finish() }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    private fun setupLockScreenFlags() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }
    }

    private fun handleCallIntent(intent: Intent?) {
        val callInvite = IncomingCallService.activeCallInvite
        if (callInvite == null) {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "No active CallInvite - call may have been cancelled")
            }
            finish()
            return
        }

        when (intent?.action) {
            ACTION_ANSWER_CALL -> {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Auto-answering call from: ${callInvite.from}")
                }
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    != PackageManager.PERMISSION_GRANTED) {
                    twilioManager.handleIncomingCallInvite(callInvite)
                    micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                } else {
                    twilioManager.acceptIncomingCall(callInvite)
                    IncomingCallService.accept(this)
                }
            }
            else -> {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Showing ringing UI for: ${callInvite.from}")
                }
                // TwilioManager was already set to Ringing by IncomingCallService
            }
        }
    }
}
