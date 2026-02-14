package com.tomasronis.rhentiapp.core.notifications

import android.content.Context
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.tomasronis.rhentiapp.BuildConfig
import com.tomasronis.rhentiapp.core.voip.IncomingCallService
import com.tomasronis.rhentiapp.core.voip.TwilioManager
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
 * For incoming calls: delegates to IncomingCallService which runs as a
 * foreground service to keep the process alive and handle ringtone/vibration.
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

        fun getLastFcmEvent(context: Context): String {
            return context.getSharedPreferences(DEBUG_PREFS, Context.MODE_PRIVATE)
                .getString(KEY_LAST_FCM_EVENT, "No FCM messages received yet") ?: "No FCM messages received yet"
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
     * Delegates to IncomingCallService for reliable foreground-service-based ringing.
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

                        // Delegate to the foreground service
                        IncomingCallService.start(
                            this@RhentiFirebaseMessagingService,
                            callInvite
                        )
                    }

                    override fun onCancelledCallInvite(
                        cancelledCallInvite: CancelledCallInvite,
                        callException: com.twilio.voice.CallException?
                    ) {
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "Call cancelled: from=${cancelledCallInvite.from}, sid=${cancelledCallInvite.callSid}")
                        }
                        writeFcmDebug("CallCancelled from=${cancelledCallInvite.from}")

                        // Tell the foreground service to stop
                        IncomingCallService.callCancelled(
                            this@RhentiFirebaseMessagingService
                        )
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
