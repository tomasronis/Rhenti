package com.tomasronis.rhentiapp.core.voip

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.tomasronis.rhentiapp.BuildConfig
import com.twilio.voice.CallInvite
import com.twilio.voice.CancelledCallInvite

/**
 * Broadcast receiver for incoming Twilio calls.
 */
class IncomingCallReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "IncomingCallReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "com.twilio.voice.INCOMING" -> {
                handleIncomingCall(context, intent)
            }
            "com.twilio.voice.CANCEL" -> {
                handleCancelledCall(context, intent)
            }
        }
    }

    /**
     * Handle incoming call invitation
     */
    private fun handleIncomingCall(context: Context, intent: Intent) {
        try {
            val callInvite = intent.getParcelableExtra<CallInvite>("INCOMING_CALL_INVITE")
            if (callInvite != null) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Incoming call from: ${callInvite.from}")
                }

                // Show incoming call notification
                showIncomingCallNotification(context, callInvite)
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Failed to handle incoming call", e)
            }
        }
    }

    /**
     * Handle cancelled call invitation
     */
    private fun handleCancelledCall(context: Context, intent: Intent) {
        try {
            val cancelledCallInvite = intent.getParcelableExtra<CancelledCallInvite>("CANCELLED_CALL_INVITE")
            if (cancelledCallInvite != null) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Cancelled call from: ${cancelledCallInvite.from}")
                }

                // Cancel incoming call notification
                cancelIncomingCallNotification(context)
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Failed to handle cancelled call", e)
            }
        }
    }

    /**
     * Show incoming call notification
     */
    private fun showIncomingCallNotification(context: Context, callInvite: CallInvite) {
        // TODO: Phase 7 - Implement full-screen incoming call notification
        // For now, this is a placeholder
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Would show incoming call notification for: ${callInvite.from}")
        }
    }

    /**
     * Cancel incoming call notification
     */
    private fun cancelIncomingCallNotification(context: Context) {
        // TODO: Phase 7 - Cancel notification
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Would cancel incoming call notification")
        }
    }
}
