package com.tomasronis.rhentiapp.core.notifications

import android.content.Intent
import android.net.Uri

/**
 * Deep link destinations for navigation from notifications.
 */
sealed class DeepLinkDestination {
    data class Thread(val threadId: String) : DeepLinkDestination()
    data class Contact(val contactId: String) : DeepLinkDestination()
    data class Call(val phoneNumber: String) : DeepLinkDestination()
    data object ChatsTab : DeepLinkDestination()
    data object ContactsTab : DeepLinkDestination()
    data object CallsTab : DeepLinkDestination()
}

/**
 * Handles parsing deep links from notifications and URIs.
 */
object DeepLinkHandler {

    // Intent extra keys for notification data
    const val EXTRA_NOTIFICATION_TYPE = "notification_type"
    const val EXTRA_THREAD_ID = "thread_id"
    const val EXTRA_CONTACT_ID = "contact_id"
    const val EXTRA_PHONE_NUMBER = "phone_number"

    /**
     * Parse notification payload into deep link destination.
     */
    fun parsePayload(payload: NotificationPayload): DeepLinkDestination {
        return when (payload.type) {
            NotificationType.MESSAGE, NotificationType.VIEWING, NotificationType.APPLICATION -> {
                payload.threadId?.let { DeepLinkDestination.Thread(it) }
                    ?: DeepLinkDestination.ChatsTab
            }
            NotificationType.CALL -> {
                payload.contactId?.let { DeepLinkDestination.Contact(it) }
                    ?: payload.phoneNumber?.let { DeepLinkDestination.Call(it) }
                    ?: DeepLinkDestination.CallsTab
            }
            NotificationType.GENERAL -> DeepLinkDestination.ChatsTab
        }
    }

    /**
     * Parse URI into deep link destination.
     * Supported URIs:
     * - rhenti://thread/{threadId}
     * - rhenti://contact/{contactId}
     * - rhenti://call/{phoneNumber}
     * - rhenti://chats
     * - rhenti://contacts
     * - rhenti://calls
     */
    fun parseUri(uri: Uri): DeepLinkDestination? {
        if (uri.scheme != "rhenti") return null

        return when (uri.host) {
            "thread" -> {
                val threadId = uri.pathSegments.firstOrNull()
                threadId?.let { DeepLinkDestination.Thread(it) }
            }
            "contact" -> {
                val contactId = uri.pathSegments.firstOrNull()
                contactId?.let { DeepLinkDestination.Contact(it) }
            }
            "call" -> {
                val phoneNumber = uri.pathSegments.firstOrNull()
                phoneNumber?.let { DeepLinkDestination.Call(it) }
            }
            "chats" -> DeepLinkDestination.ChatsTab
            "contacts" -> DeepLinkDestination.ContactsTab
            "calls" -> DeepLinkDestination.CallsTab
            else -> null
        }
    }

    /**
     * Parse intent extras into deep link destination.
     */
    fun parseIntent(intent: Intent): DeepLinkDestination? {
        // First try URI if present
        intent.data?.let { uri ->
            parseUri(uri)?.let { return it }
        }

        // Then try intent extras
        val type = intent.getStringExtra(EXTRA_NOTIFICATION_TYPE) ?: return null
        val notificationType = NotificationType.fromString(type)

        return when (notificationType) {
            NotificationType.MESSAGE, NotificationType.VIEWING, NotificationType.APPLICATION -> {
                val threadId = intent.getStringExtra(EXTRA_THREAD_ID)
                threadId?.let { DeepLinkDestination.Thread(it) }
                    ?: DeepLinkDestination.ChatsTab
            }
            NotificationType.CALL -> {
                val contactId = intent.getStringExtra(EXTRA_CONTACT_ID)
                val phoneNumber = intent.getStringExtra(EXTRA_PHONE_NUMBER)
                contactId?.let { DeepLinkDestination.Contact(it) }
                    ?: phoneNumber?.let { DeepLinkDestination.Call(it) }
                    ?: DeepLinkDestination.CallsTab
            }
            NotificationType.GENERAL -> DeepLinkDestination.ChatsTab
        }
    }

    /**
     * Create intent with notification data for deep linking.
     */
    fun createIntentExtras(payload: NotificationPayload): Map<String, String> {
        val extras = mutableMapOf(
            EXTRA_NOTIFICATION_TYPE to payload.type.name
        )

        payload.threadId?.let { extras[EXTRA_THREAD_ID] = it }
        payload.contactId?.let { extras[EXTRA_CONTACT_ID] = it }
        payload.phoneNumber?.let { extras[EXTRA_PHONE_NUMBER] = it }

        return extras
    }
}
