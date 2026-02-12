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
            Log.d(TAG, "Message received from: ${remoteMessage.from}")
            Log.d(TAG, "Message data: ${remoteMessage.data}")
        }

        // Parse notification payload
        val payload = NotificationPayload.fromRemoteMessage(remoteMessage)
        if (payload == null) {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "Failed to parse notification payload - missing required fields")
            }
            return
        }

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Parsed notification: type=${payload.type}, title=${payload.title}")
        }

        // Display notification
        notificationManager.showNotification(payload)
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
