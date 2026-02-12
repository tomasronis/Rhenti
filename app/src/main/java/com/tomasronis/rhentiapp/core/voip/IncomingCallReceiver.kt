package com.tomasronis.rhentiapp.core.voip

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.tomasronis.rhentiapp.BuildConfig
import com.tomasronis.rhentiapp.R
import com.tomasronis.rhentiapp.core.notifications.NotificationChannels
import com.tomasronis.rhentiapp.presentation.MainActivity
import com.twilio.voice.CallInvite
import com.twilio.voice.CancelledCallInvite

/**
 * Broadcast receiver for incoming Twilio calls.
 */
class IncomingCallReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "IncomingCallReceiver"
        private const val INCOMING_CALL_NOTIFICATION_ID = 9001
        private const val NOTIFICATION_COLOR = 0xFF34C759.toInt() // iOS green for incoming calls
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
        try {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Showing incoming call notification for: ${callInvite.from}")
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Create intent to launch main activity (will show incoming call screen)
            val fullScreenIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                action = "com.rhentimobile.INCOMING_CALL"
                putExtra("CALL_INVITE", callInvite)
                putExtra("CALLER_NUMBER", callInvite.from)
            }

            val fullScreenPendingIntent = PendingIntent.getActivity(
                context,
                INCOMING_CALL_NOTIFICATION_ID,
                fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Create answer intent
            val answerIntent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                action = "com.rhentimobile.INCOMING_CALL"
                putExtra("CALL_INVITE", callInvite)
                putExtra("CALLER_NUMBER", callInvite.from)
                putExtra("AUTO_ANSWER", true)
            }

            val answerPendingIntent = PendingIntent.getActivity(
                context,
                INCOMING_CALL_NOTIFICATION_ID + 1,
                answerIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Create decline intent
            val declineIntent = Intent(context, IncomingCallReceiver::class.java).apply {
                action = "com.rhentimobile.DECLINE_CALL"
                putExtra("CALL_INVITE", callInvite)
            }

            val declinePendingIntent = PendingIntent.getBroadcast(
                context,
                INCOMING_CALL_NOTIFICATION_ID + 2,
                declineIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Format caller name/number
            val callerName = callInvite.from?.substringAfter("client:")?.takeIf { it.isNotBlank() }
                ?: callInvite.from ?: "Unknown Caller"

            // Build notification
            val notification = NotificationCompat.Builder(context, NotificationChannels.INCOMING_CALL_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Incoming Call")
                .setContentText(callerName)
                .setColor(NOTIFICATION_COLOR)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setOngoing(true) // Cannot be dismissed
                .setAutoCancel(false)
                .setFullScreenIntent(fullScreenPendingIntent, true) // Full-screen on lock screen
                .setContentIntent(fullScreenPendingIntent)
                .addAction(
                    R.drawable.ic_notification,
                    "Answer",
                    answerPendingIntent
                )
                .addAction(
                    R.drawable.ic_notification,
                    "Decline",
                    declinePendingIntent
                )
                .build()

            notificationManager.notify(INCOMING_CALL_NOTIFICATION_ID, notification)

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Incoming call notification displayed")
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Failed to show incoming call notification", e)
            }
        }
    }

    /**
     * Cancel incoming call notification
     */
    private fun cancelIncomingCallNotification(context: Context) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(INCOMING_CALL_NOTIFICATION_ID)

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Incoming call notification cancelled")
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Failed to cancel incoming call notification", e)
            }
        }
    }
}
