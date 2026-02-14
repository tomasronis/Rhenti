package com.tomasronis.rhentiapp.core.voip

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.tomasronis.rhentiapp.BuildConfig

/**
 * Broadcast receiver for incoming call actions (decline from notification).
 * Rejects the call immediately and tells IncomingCallService to stop.
 */
class IncomingCallReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "IncomingCallReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "com.rhentimobile.DECLINE_CALL" -> {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Decline action received")
                }

                // 1. Reject the call invite immediately from the receiver
                //    (don't depend on service round-trip which can be slow)
                try {
                    IncomingCallService.activeCallInvite?.reject(context)
                    IncomingCallService.clearCallInvite()
                } catch (e: Exception) {
                    if (BuildConfig.DEBUG) {
                        Log.e(TAG, "Failed to reject call invite directly", e)
                    }
                }

                // 2. Cancel the notification immediately
                try {
                    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                    nm.cancel(IncomingCallService.NOTIFICATION_ID)
                } catch (e: Exception) {
                    // Ignore
                }

                // 3. Tell the service to stop ringing and clean up
                IncomingCallService.decline(context)

                // 4. Reset TwilioManager state so IncomingCallActivity dismisses
                try {
                    val entryPoint = dagger.hilt.android.EntryPointAccessors.fromApplication(
                        context.applicationContext,
                        TwilioEntryPoint::class.java
                    )
                    entryPoint.twilioManager().handleCallCancelled()
                } catch (e: Exception) {
                    if (BuildConfig.DEBUG) {
                        Log.w(TAG, "Could not access TwilioManager to reset state", e)
                    }
                }
            }
        }
    }

    @dagger.hilt.EntryPoint
    @dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
    interface TwilioEntryPoint {
        fun twilioManager(): TwilioManager
    }
}
