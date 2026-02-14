package com.tomasronis.rhentiapp.core.voip

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.tomasronis.rhentiapp.BuildConfig

/**
 * Broadcast receiver for incoming call actions (decline from notification).
 * Delegates to IncomingCallService which owns the CallInvite and ringing state.
 */
class IncomingCallReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "IncomingCallReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            "com.rhentimobile.DECLINE_CALL" -> {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Decline action received - delegating to IncomingCallService")
                }
                IncomingCallService.decline(context)
            }
        }
    }
}
