package com.tomasronis.rhentiapp.core.notifications

import android.content.Intent
import android.net.Uri
import android.util.Log
import com.tomasronis.rhentiapp.BuildConfig

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
    private const val TAG = "DeepLinkHandler"

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
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "parseUri: scheme=${uri.scheme}, host=${uri.host}, path=${uri.path}")
        }

        if (uri.scheme != "rhenti") {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "parseUri: Invalid scheme '${uri.scheme}', expected 'rhenti'")
            }
            return null
        }

        val destination = when (uri.host) {
            "thread" -> {
                val threadId = uri.pathSegments.firstOrNull()
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "parseUri: Thread destination, threadId=$threadId")
                }
                threadId?.let { DeepLinkDestination.Thread(it) }
            }
            "contact" -> {
                val contactId = uri.pathSegments.firstOrNull()
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "parseUri: Contact destination, contactId=$contactId")
                }
                contactId?.let { DeepLinkDestination.Contact(it) }
            }
            "call" -> {
                val phoneNumber = uri.pathSegments.firstOrNull()
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "parseUri: Call destination, phoneNumber=$phoneNumber")
                }
                phoneNumber?.let { DeepLinkDestination.Call(it) }
            }
            "chats" -> {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "parseUri: ChatsTab destination")
                }
                DeepLinkDestination.ChatsTab
            }
            "contacts" -> {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "parseUri: ContactsTab destination")
                }
                DeepLinkDestination.ContactsTab
            }
            "calls" -> {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "parseUri: CallsTab destination")
                }
                DeepLinkDestination.CallsTab
            }
            else -> {
                if (BuildConfig.DEBUG) {
                    Log.w(TAG, "parseUri: Unknown host '${uri.host}'")
                }
                null
            }
        }

        return destination
    }

    /**
     * Parse intent extras into deep link destination.
     */
    fun parseIntent(intent: Intent): DeepLinkDestination? {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "parseIntent: action=${intent.action}, data=${intent.data}")
        }

        // First try URI if present
        intent.data?.let { uri ->
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "parseIntent: Trying URI parsing first")
            }
            parseUri(uri)?.let {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "parseIntent: ✅ Successfully parsed from URI: $it")
                }
                return it
            }
        }

        // Then try intent extras
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "parseIntent: Trying extras parsing")
        }

        val type = intent.getStringExtra(EXTRA_NOTIFICATION_TYPE)
        if (type == null) {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "parseIntent: No notification type in extras")
            }
            return null
        }

        val notificationType = NotificationType.fromString(type)
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "parseIntent: notification type = $notificationType")
        }

        val destination = when (notificationType) {
            NotificationType.MESSAGE, NotificationType.VIEWING, NotificationType.APPLICATION -> {
                val threadId = intent.getStringExtra(EXTRA_THREAD_ID)
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "parseIntent: threadId from extras = $threadId")
                }
                threadId?.let { DeepLinkDestination.Thread(it) }
                    ?: DeepLinkDestination.ChatsTab
            }
            NotificationType.CALL -> {
                val contactId = intent.getStringExtra(EXTRA_CONTACT_ID)
                val phoneNumber = intent.getStringExtra(EXTRA_PHONE_NUMBER)
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "parseIntent: contactId=$contactId, phoneNumber=$phoneNumber")
                }
                contactId?.let { DeepLinkDestination.Contact(it) }
                    ?: phoneNumber?.let { DeepLinkDestination.Call(it) }
                    ?: DeepLinkDestination.CallsTab
            }
            NotificationType.GENERAL -> DeepLinkDestination.ChatsTab
        }

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "parseIntent: ✅ Successfully parsed from extras: $destination")
        }

        return destination
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
