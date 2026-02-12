package com.tomasronis.rhentiapp.core.messaging

import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.tomasronis.rhentiapp.core.notifications.PushNotificationManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Firebase Cloud Messaging service that handles incoming push notifications
 * and token refresh events.
 *
 * Notification types handled:
 * - "message" → New chat message
 * - "viewing" → Viewing request, confirmation, decline, or alternative
 * - "application" → Application update
 * - "call" → Incoming call notification
 */
@AndroidEntryPoint
class RhentiFCMService : FirebaseMessagingService() {

    @Inject
    lateinit var pushNotificationManager: PushNotificationManager

    @Inject
    lateinit var fcmTokenRepository: FCMTokenRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM token refreshed")

        serviceScope.launch {
            fcmTokenRepository.onTokenRefreshed(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "FCM message received: ${message.data}")

        val data = message.data
        val type = data["type"] ?: message.notification?.let { "message" } ?: return

        when (type) {
            "message" -> handleMessageNotification(data, message.notification)
            "viewing", "booking" -> handleViewingNotification(data, message.notification)
            "application" -> handleApplicationNotification(data, message.notification)
            "call" -> handleCallNotification(data, message.notification)
            else -> handleGenericNotification(data, message.notification)
        }
    }

    private fun handleMessageNotification(
        data: Map<String, String>,
        notification: RemoteMessage.Notification?
    ) {
        val threadId = data["threadId"] ?: data["thread_id"] ?: ""
        val senderName = data["senderName"] ?: data["sender_name"]
            ?: notification?.title ?: "New Message"
        val messageText = data["text"] ?: data["message"]
            ?: notification?.body ?: "You have a new message"

        pushNotificationManager.showMessageNotification(
            threadId = threadId,
            senderName = senderName,
            messageText = messageText
        )
    }

    private fun handleViewingNotification(
        data: Map<String, String>,
        notification: RemoteMessage.Notification?
    ) {
        val bookingId = data["bookingId"] ?: data["booking_id"]
            ?: data["bookViewingId"] ?: ""
        val viewingType = data["bookViewingType"] ?: data["viewing_type"] ?: ""
        val propertyAddress = data["propertyAddress"] ?: data["property_address"]
        val threadId = data["threadId"] ?: data["thread_id"]

        val title = notification?.title ?: when (viewingType) {
            "viewing_request" -> "New Viewing Request"
            "confirm" -> "Viewing Confirmed"
            "decline" -> "Viewing Declined"
            "change_request", "alternative" -> "Viewing Alternatives Proposed"
            else -> "Viewing Update"
        }

        val body = notification?.body ?: when (viewingType) {
            "viewing_request" -> "A renter has requested a viewing"
            "confirm" -> "A viewing has been confirmed"
            "decline" -> "A viewing has been declined"
            "change_request", "alternative" -> "Alternative viewing times have been proposed"
            else -> "A viewing has been updated"
        }

        pushNotificationManager.showViewingNotification(
            bookingId = bookingId,
            title = title,
            body = body,
            propertyAddress = propertyAddress,
            threadId = threadId
        )
    }

    private fun handleApplicationNotification(
        data: Map<String, String>,
        notification: RemoteMessage.Notification?
    ) {
        val applicationId = data["applicationId"] ?: data["application_id"] ?: ""
        val threadId = data["threadId"] ?: data["thread_id"]
        val title = notification?.title ?: "Application Update"
        val body = notification?.body ?: "You have a new application update"

        pushNotificationManager.showApplicationNotification(
            applicationId = applicationId,
            title = title,
            body = body,
            threadId = threadId
        )
    }

    private fun handleCallNotification(
        data: Map<String, String>,
        notification: RemoteMessage.Notification?
    ) {
        val callerName = data["callerName"] ?: data["caller_name"]
            ?: notification?.title ?: ""
        val callerNumber = data["callerNumber"] ?: data["caller_number"]
            ?: notification?.body ?: ""

        pushNotificationManager.showIncomingCallNotification(
            callerName = callerName,
            callerNumber = callerNumber
        )
    }

    private fun handleGenericNotification(
        data: Map<String, String>,
        notification: RemoteMessage.Notification?
    ) {
        val title = notification?.title ?: data["title"] ?: "Rhenti"
        val body = notification?.body ?: data["body"] ?: data["message"] ?: return

        pushNotificationManager.showMessageNotification(
            threadId = data["threadId"] ?: "",
            senderName = title,
            messageText = body
        )
    }

    companion object {
        private const val TAG = "RhentiFCMService"
    }
}
