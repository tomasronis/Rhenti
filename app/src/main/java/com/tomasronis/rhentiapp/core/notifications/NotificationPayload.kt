package com.tomasronis.rhentiapp.core.notifications

import com.google.firebase.messaging.RemoteMessage

/**
 * Notification types supported by the app.
 */
enum class NotificationType {
    MESSAGE,
    VIEWING,
    APPLICATION,
    CALL,
    GENERAL;

    companion object {
        fun fromString(type: String?): NotificationType {
            return when (type?.lowercase()) {
                "message" -> MESSAGE
                "viewing" -> VIEWING
                "application" -> APPLICATION
                "call" -> CALL
                else -> GENERAL
            }
        }
    }
}

/**
 * Parsed notification payload from FCM message.
 */
data class NotificationPayload(
    val type: NotificationType,
    val title: String,
    val body: String,
    val threadId: String? = null,
    val bookingId: String? = null,
    val applicationId: String? = null,
    val contactId: String? = null,
    val phoneNumber: String? = null,
    val propertyAddress: String? = null,
    val imageUrl: String? = null
) {
    companion object {
        private const val TAG = "NotificationPayload"

        /**
         * Parse FCM RemoteMessage into NotificationPayload.
         * Tries data payload first, then falls back to notification object.
         */
        fun fromRemoteMessage(remoteMessage: RemoteMessage): NotificationPayload? {
            val data = remoteMessage.data

            // Try data payload first (data-only messages from backend)
            if (data.isNotEmpty()) {
                val title = data["title"]
                val body = data["body"]
                if (title != null && body != null) {
                    val type = NotificationType.fromString(data["type"])
                    if (com.tomasronis.rhentiapp.BuildConfig.DEBUG) {
                        android.util.Log.d(TAG, "Parsed from data payload: type=$type, title=$title")
                    }
                    return NotificationPayload(
                        type = type,
                        title = title,
                        body = body,
                        threadId = data["threadId"],
                        bookingId = data["bookingId"],
                        applicationId = data["applicationId"],
                        contactId = data["contactId"],
                        phoneNumber = data["phoneNumber"],
                        propertyAddress = data["propertyAddress"],
                        imageUrl = data["imageUrl"]
                    )
                }
            }

            // Fall back to notification object (Firebase Console or notification+data messages)
            remoteMessage.notification?.let { notification ->
                val title = notification.title ?: return@let
                val body = notification.body ?: ""
                // Try to infer type from data payload even if title/body came from notification
                val type = NotificationType.fromString(data["type"])
                if (com.tomasronis.rhentiapp.BuildConfig.DEBUG) {
                    android.util.Log.d(TAG, "Parsed from notification object: type=$type, title=$title")
                }
                return NotificationPayload(
                    type = type,
                    title = title,
                    body = body,
                    threadId = data["threadId"],
                    bookingId = data["bookingId"],
                    applicationId = data["applicationId"],
                    contactId = data["contactId"],
                    phoneNumber = data["phoneNumber"],
                    propertyAddress = data["propertyAddress"],
                    imageUrl = notification.imageUrl?.toString() ?: data["imageUrl"]
                )
            }

            if (com.tomasronis.rhentiapp.BuildConfig.DEBUG) {
                android.util.Log.w(TAG, "Could not parse: data=${data.keys}, notification=${remoteMessage.notification != null}")
            }
            return null
        }
    }

    /**
     * Get the notification channel ID for this payload.
     */
    fun getChannelId(): String {
        return when (type) {
            NotificationType.MESSAGE -> NotificationChannels.MESSAGES_CHANNEL_ID
            NotificationType.VIEWING -> NotificationChannels.VIEWINGS_CHANNEL_ID
            NotificationType.APPLICATION -> NotificationChannels.APPLICATIONS_CHANNEL_ID
            NotificationType.CALL -> NotificationChannels.INCOMING_CALL_CHANNEL_ID
            NotificationType.GENERAL -> NotificationChannels.GENERAL_CHANNEL_ID
        }
    }

    /**
     * Generate a unique notification ID for grouping/replacing.
     */
    fun getNotificationId(): Int {
        return when {
            threadId != null -> "thread_$threadId".hashCode()
            contactId != null -> "contact_$contactId".hashCode()
            else -> System.currentTimeMillis().toInt()
        }
    }
}
