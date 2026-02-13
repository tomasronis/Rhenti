package com.tomasronis.rhentiapp.core.notifications

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.tomasronis.rhentiapp.BuildConfig
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Firebase Cloud Messaging service for receiving push notifications.
 */
@AndroidEntryPoint
class RhentiFirebaseMessagingService : FirebaseMessagingService() {

    @Inject
    lateinit var fcmTokenManager: FcmTokenManager

    @Inject
    lateinit var notificationManager: RhentiNotificationManager

    companion object {
        private const val TAG = "FCMService"
    }

    /**
     * Called when a message is received from Firebase.
     */
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "========================================")
            Log.d(TAG, "📬 FCM Message Received")
            Log.d(TAG, "========================================")
            Log.d(TAG, "From: ${remoteMessage.from}")
            Log.d(TAG, "Message ID: ${remoteMessage.messageId}")
            Log.d(TAG, "Sent Time: ${remoteMessage.sentTime}")

            // Log FCM notification content if present
            remoteMessage.notification?.let { notification ->
                Log.d(TAG, "FCM Notification:")
                Log.d(TAG, "  Title: ${notification.title}")
                Log.d(TAG, "  Body: ${notification.body}")
            }

            // Log all data fields
            Log.d(TAG, "Data Payload (${remoteMessage.data.size} fields):")
            remoteMessage.data.forEach { (key, value) ->
                Log.d(TAG, "  $key = $value")
            }
            Log.d(TAG, "========================================")
        }

        // Check if this is a Twilio Voice call invite
        val isTwilioMessage = remoteMessage.data["twi_message_type"] != null
        if (isTwilioMessage) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "🔔 Detected Twilio Voice call invite")
            }
            handleTwilioCallInvite(remoteMessage)
            return
        }

        // Parse notification payload (tries data payload first, then notification object)
        val payload = NotificationPayload.fromRemoteMessage(remoteMessage)

        if (payload == null) {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "❌ Failed to parse notification payload")
                Log.w(TAG, "  No data fields and no notification object found")
            }
            return
        }

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "✅ Notification Parsed Successfully:")
            Log.d(TAG, "  Type: ${payload.type}")
            Log.d(TAG, "  Title: ${payload.title}")
            Log.d(TAG, "  Body: ${payload.body}")
            Log.d(TAG, "  Thread ID: ${payload.threadId ?: "none"}")
            Log.d(TAG, "  Booking ID: ${payload.bookingId ?: "none"}")
            Log.d(TAG, "  Application ID: ${payload.applicationId ?: "none"}")
            Log.d(TAG, "  Contact ID: ${payload.contactId ?: "none"}")
            Log.d(TAG, "  Phone Number: ${payload.phoneNumber ?: "none"}")
            Log.d(TAG, "  Property Address: ${payload.propertyAddress ?: "none"}")
            Log.d(TAG, "  Image URL: ${payload.imageUrl ?: "none"}")
        }

        // Display notification
        notificationManager.showNotification(payload)

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "✅ Notification display triggered")
        }
    }

    /**
     * Handle Twilio Voice call invite from FCM.
     * Twilio sends push notifications with special data fields for incoming calls.
     */
    private fun handleTwilioCallInvite(remoteMessage: RemoteMessage) {
        try {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "========================================")
                Log.d(TAG, "📞 Handling Twilio Voice Call Invite")
                Log.d(TAG, "========================================")
                Log.d(TAG, "Twilio Data Fields:")
                remoteMessage.data.forEach { (key, value) ->
                    Log.d(TAG, "  $key = $value")
                }
            }

            // Twilio Voice SDK handles the call invite internally
            // We just need to let it process the message
            val valid = com.twilio.voice.Voice.handleMessage(
                this,
                remoteMessage.data,
                object : com.twilio.voice.MessageListener {
                    override fun onCallInvite(callInvite: com.twilio.voice.CallInvite) {
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "✅ Call Invite Received Successfully:")
                            Log.d(TAG, "  From: ${callInvite.from}")
                            Log.d(TAG, "  To: ${callInvite.to}")
                            Log.d(TAG, "  Call SID: ${callInvite.callSid}")
                            Log.d(TAG, "  Custom Parameters: ${callInvite.customParameters}")
                        }
                        // The IncomingCallReceiver will handle showing the notification
                        // This is done automatically by Twilio SDK when call invite is received
                    }

                    override fun onCancelledCallInvite(
                        cancelledCallInvite: com.twilio.voice.CancelledCallInvite,
                        callException: com.twilio.voice.CallException?
                    ) {
                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "❌ Call Invite Cancelled:")
                            Log.d(TAG, "  From: ${cancelledCallInvite.from}")
                            Log.d(TAG, "  To: ${cancelledCallInvite.to}")
                            Log.d(TAG, "  Call SID: ${cancelledCallInvite.callSid}")
                            callException?.let {
                                Log.d(TAG, "  Error Code: ${it.errorCode}")
                                Log.d(TAG, "  Error Message: ${it.message}")
                            }
                        }
                    }
                }
            )

            if (valid) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "✅ Twilio message was valid and handled")
                    Log.d(TAG, "========================================")
                }
            } else {
                if (BuildConfig.DEBUG) {
                    Log.w(TAG, "⚠️ Twilio message was NOT valid")
                    Log.w(TAG, "  This may not be a Twilio Voice message")
                    Log.w(TAG, "========================================")
                }
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "❌ Exception while handling Twilio call invite", e)
                Log.e(TAG, "  Error: ${e.message}")
                e.printStackTrace()
                Log.d(TAG, "========================================")
            }
        }
    }

    /**
     * Called when a new FCM token is generated.
     * This happens on initial app install and whenever the token is refreshed.
     */
    override fun onNewToken(token: String) {
        super.onNewToken(token)

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "New FCM token: $token")
        }

        // Save and sync token with backend
        CoroutineScope(Dispatchers.IO).launch {
            fcmTokenManager.onNewToken(token)
        }
    }
}
