package com.tomasronis.rhentiapp.core.voip

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.tomasronis.rhentiapp.BuildConfig
import com.tomasronis.rhentiapp.core.notifications.RhentiFirebaseMessagingService
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Broadcast receiver for incoming call actions (decline from notification).
 */
class IncomingCallReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface ReceiverEntryPoint {
        fun twilioManager(): TwilioManager
    }

    companion object {
        private const val TAG = "IncomingCallReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "com.rhentimobile.DECLINE_CALL" -> {
                handleDeclineCall(context, intent)
            }
        }
    }

    private fun handleDeclineCall(context: Context, intent: Intent) {
        try {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Declining call")
            }

            // Get CallInvite from static holder and reject it
            val callInvite = RhentiFirebaseMessagingService.activeCallInvite
            if (callInvite != null) {
                callInvite.reject(context)
                RhentiFirebaseMessagingService.clearCallInvite()
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Call rejected successfully")
                }
            } else if (BuildConfig.DEBUG) {
                Log.w(TAG, "No active CallInvite to decline")
            }

            // Reset TwilioManager state so IncomingCallActivity dismisses
            try {
                val entryPoint = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    ReceiverEntryPoint::class.java
                )
                entryPoint.twilioManager().handleCallCancelled()
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) {
                    Log.w(TAG, "Could not access TwilioManager to reset state", e)
                }
            }

            // Cancel the notification
            RhentiFirebaseMessagingService.cancelIncomingCallNotification(context)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Failed to decline call", e)
            }
            RhentiFirebaseMessagingService.cancelIncomingCallNotification(context)
        }
    }
}
