package com.tomasronis.rhentiapp.core.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.tomasronis.rhentiapp.BuildConfig
import com.tomasronis.rhentiapp.R
import com.tomasronis.rhentiapp.core.voip.TwilioManager
import com.tomasronis.rhentiapp.presentation.calls.active.IncomingCallActivity
import com.twilio.voice.CallInvite
import com.twilio.voice.CancelledCallInvite
import com.twilio.voice.MessageListener
import com.twilio.voice.Voice
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * Firebase Cloud Messaging service for receiving push notifications.
 * Handles both regular app notifications and Twilio Voice incoming calls.
 *
 * For incoming calls: shows notification DIRECTLY from this service
 * (no broadcast to IncomingCallReceiver) for reliable background delivery.
 */
@AndroidEntryPoint
class RhentiFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var fcmTokenManager: FcmTokenManager

    @Inject
    lateinit var notificationManager: RhentiNotificationManager

    @Inject
    lateinit var twilioManager: TwilioManager

    companion object {
        private const val TAG = "FCMService"
        const val DEBUG_PREFS = "fcm_debug"
        const val KEY_LAST_FCM_EVENT = "last_fcm_event"
        const val INCOMING_CALL_NOTIFICATION_ID = 9001

        // Static holder for the active CallInvite so MainActivity can access it
        @Volatile
        var activeCallInvite: CallInvite? = null
            private set

        fun getLastFcmEvent(context: Context): String {
            return context.getSharedPreferences(DEBUG_PREFS, Context.MODE_PRIVATE)
                .getString(KEY_LAST_FCM_EVENT, "No FCM messages received yet") ?: "No FCM messages received yet"
        }

        fun clearCallInvite() {
            activeCallInvite = null
        }

        fun cancelIncomingCallNotification(context: Context) {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(INCOMING_CALL_NOTIFICATION_ID)
        }
    }

    private fun writeFcmDebug(message: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val entry = "[$timestamp] $message"
        getSharedPreferences(DEBUG_PREFS, MODE_PRIVATE)
            .edit()
            .putString(KEY_LAST_FCM_EVENT, entry)
            .apply()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        val isTwilio = remoteMessage.data["twi_message_type"] != null ||
                       remoteMessage.data["twi_account_sid"] != null
        val dataKeys = remoteMessage.data.keys.joinToString(", ")
        writeFcmDebug("FCM from=${remoteMessage.from}, twilio=$isTwilio, keys=[$dataKeys]")

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "========================================")
            Log.d(TAG, "FCM Message Received")
            Log.d(TAG, "From: ${remoteMessage.from}")
            Log.d(TAG, "Data Payload (${remoteMessage.data.size} fields):")
            remoteMessage.data.forEach { (key, value) ->
                Log.d(TAG, "  $key = $value")
            }
            Log.d(TAG, "========================================")
        }

        // Check if this is a Twilio Voice call invite
        if (isTwilio) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Detected Twilio Voice message")
            }
            handleTwilioCallInvite(remoteMessage)
            return
        }

        // Parse and display regular notification
        val payload = NotificationPayload.fromRemoteMessage(remoteMessage)
        if (payload != null) {
            notificationManager.showNotification(payload)
        } else if (BuildConfig.DEBUG) {
            Log.w(TAG, "Failed to parse notification payload")
        }
    }

    /**
     * Handle Twilio Voice call invite from FCM.
     * Shows incoming call notification DIRECTLY from this service for reliable
     * background delivery (no broadcast needed).
     */
    private fun handleTwilioCallInvite(remoteMessage: RemoteMessage) {
        try {
            val valid = Voice.handleMessage(
                this,
                remoteMessage.data,
                object : MessageListener {
                    override fun onCallInvite(callInvite: CallInvite) {
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "CallInvite received: from=${callInvite.from}, sid=${callInvite.callSid}")
                        }

                        writeFcmDebug("CallInvite! from=${callInvite.from}, sid=${callInvite.callSid}")

                        // Store CallInvite so MainActivity/TwilioManager can access it
                        activeCallInvite = callInvite

                        // Show notification directly (works in foreground AND background)
                        showIncomingCallNotification(callInvite)
                    }

                    override fun onCancelledCallInvite(
                        cancelledCallInvite: CancelledCallInvite,
                        callException: com.twilio.voice.CallException?
                    ) {
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "Call cancelled: from=${cancelledCallInvite.from}, sid=${cancelledCallInvite.callSid}")
                        }

                        writeFcmDebug("CallCancelled from=${cancelledCallInvite.from}")

                        // Clear the stored invite and dismiss notification
                        activeCallInvite = null
                        cancelIncomingCallNotification(this@RhentiFirebaseMessagingService)

                        // Reset TwilioManager state so IncomingCallActivity dismisses
                        twilioManager.handleCallCancelled()
                    }
                }
            )

            if (valid) {
                writeFcmDebug("Voice.handleMessage=VALID")
            } else {
                writeFcmDebug("Voice.handleMessage=INVALID")
                if (BuildConfig.DEBUG) {
                    Log.w(TAG, "Twilio message was NOT valid")
                }
            }
        } catch (e: Exception) {
            writeFcmDebug("Twilio EXCEPTION: ${e.message}")
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Exception handling Twilio call invite", e)
            }
        }
    }

    /**
     * Show incoming call notification with ringtone, vibration, and action buttons.
     * Uses CallStyle for proper heads-up display and system call treatment.
     * Acquires a wake lock to ensure the screen turns on.
     */
    private fun showIncomingCallNotification(callInvite: CallInvite) {
        try {
            // Wake the screen if it's off (USE_FULL_SCREEN_INTENT is restricted on Android 14+)
            val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!powerManager.isInteractive) {
                @Suppress("DEPRECATION")
                val wakeLock = powerManager.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                    "rhenti:incoming_call"
                )
                wakeLock.acquire(30_000L) // Auto-release after 30 seconds
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Wake lock acquired for incoming call")
                }
            }

            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Format caller display
            val callerNumber = callInvite.from ?: "Unknown"
            val callerDisplay = callerNumber
                .removePrefix("client:")
                .takeIf { it.isNotBlank() } ?: "Unknown Caller"

            // Full-screen intent - opens dedicated IncomingCallActivity
            val fullScreenIntent = Intent(this, IncomingCallActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                action = IncomingCallActivity.ACTION_INCOMING_CALL
                putExtra("CALL_SID", callInvite.callSid)
                putExtra("CALLER_NUMBER", callerNumber)
            }
            val fullScreenPendingIntent = PendingIntent.getActivity(
                this, INCOMING_CALL_NOTIFICATION_ID,
                fullScreenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Answer action - opens IncomingCallActivity and auto-answers
            val answerIntent = Intent(this, IncomingCallActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                action = IncomingCallActivity.ACTION_ANSWER_CALL
                putExtra("CALL_SID", callInvite.callSid)
                putExtra("CALLER_NUMBER", callerNumber)
            }
            val answerPendingIntent = PendingIntent.getActivity(
                this, INCOMING_CALL_NOTIFICATION_ID + 1,
                answerIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Decline action - handled by broadcast receiver
            val declineIntent = Intent(this, com.tomasronis.rhentiapp.core.voip.IncomingCallReceiver::class.java).apply {
                action = "com.rhentimobile.DECLINE_CALL"
                putExtra("CALL_SID", callInvite.callSid)
            }
            val declinePendingIntent = PendingIntent.getBroadcast(
                this, INCOMING_CALL_NOTIFICATION_ID + 2,
                declineIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )

            // Create Person for CallStyle notification (proper incoming call UI on Android 12+)
            val callerPerson = Person.Builder()
                .setName(callerDisplay)
                .setImportant(true)
                .build()

            val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

            val notification = NotificationCompat.Builder(this, NotificationChannels.INCOMING_CALL_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Incoming Call")
                .setContentText(callerDisplay)
                .setStyle(NotificationCompat.CallStyle.forIncomingCall(
                    callerPerson, declinePendingIntent, answerPendingIntent
                ))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_CALL)
                .setOngoing(true)
                .setAutoCancel(false)
                .setFullScreenIntent(fullScreenPendingIntent, true)
                .setContentIntent(fullScreenPendingIntent)
                .setSound(ringtoneUri)
                .setVibrate(longArrayOf(0, 1000, 500, 1000, 500, 1000))
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setTimeoutAfter(30000) // Auto-dismiss after 30s
                .build()

            nm.notify(INCOMING_CALL_NOTIFICATION_ID, notification)

            // Also directly start IncomingCallActivity for immediate full-screen experience
            // When app is in foreground, the full-screen intent may only show heads-up notification.
            // This ensures the full-screen call UI always appears.
            try {
                val directIntent = Intent(this, IncomingCallActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    action = IncomingCallActivity.ACTION_INCOMING_CALL
                    putExtra("CALL_SID", callInvite.callSid)
                    putExtra("CALLER_NUMBER", callerNumber)
                }
                startActivity(directIntent)
            } catch (e: Exception) {
                // Expected to fail when app is in background on Android 10+
                // The notification's fullScreenIntent handles that case
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Direct activity start failed (expected in background): ${e.message}")
                }
            }

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Incoming call notification displayed (CallStyle) for: $callerDisplay")
            }
        } catch (e: Exception) {
            writeFcmDebug("Notification FAILED: ${e.message}")
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Failed to show incoming call notification", e)
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "New FCM token: $token")
        }

        CoroutineScope(Dispatchers.IO).launch {
            fcmTokenManager.onNewToken(token)
        }
    }
}
