package com.tomasronis.rhentiapp.core.notifications

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.core.app.NotificationCompat
import com.tomasronis.rhentiapp.BuildConfig
import com.tomasronis.rhentiapp.R
import com.tomasronis.rhentiapp.presentation.MainActivity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages building and displaying push notifications.
 */
@Singleton
class RhentiNotificationManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val scope = CoroutineScope(Dispatchers.IO)

    companion object {
        private const val TAG = "NotificationManager"
        private const val NOTIFICATION_COLOR = 0xFFE8998D.toInt() // Rhenti coral color
    }

    /**
     * Display a notification from the given payload.
     */
    fun showNotification(payload: NotificationPayload) {
        scope.launch {
            try {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "========================================")
                    Log.d(TAG, "🔔 Building Notification")
                    Log.d(TAG, "========================================")
                }

                // Load image if URL provided
                val largeIcon = payload.imageUrl?.let {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "Loading notification image: ${it.take(50)}...")
                    }
                    loadImage(it)
                }

                // Build notification
                val notification = buildNotification(payload, largeIcon)

                // Show notification
                val notificationId = payload.getNotificationId()
                notificationManager.notify(notificationId, notification.build())

                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "✅ NOTIFICATION DISPLAYED SUCCESSFULLY")
                    Log.d(TAG, "  Notification ID: $notificationId")
                    Log.d(TAG, "  Type: ${payload.type}")
                    Log.d(TAG, "  Title: ${payload.title}")
                    Log.d(TAG, "  Body: ${payload.body}")
                    Log.d(TAG, "  Channel: ${payload.getChannelId()}")
                    Log.d(TAG, "========================================")
                }
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) {
                    Log.e(TAG, "❌ FAILED TO SHOW NOTIFICATION", e)
                    Log.e(TAG, "  Type: ${payload.type}")
                    Log.e(TAG, "  Title: ${payload.title}")
                }
            }
        }
    }

    /**
     * Build notification with proper styling and actions.
     */
    private fun buildNotification(
        payload: NotificationPayload,
        largeIcon: Bitmap?
    ): NotificationCompat.Builder {
        val channelId = payload.getChannelId()
        val intent = createIntentForPayload(payload)

        // Use a unique request code to avoid stale PendingIntents.
        // Combine notification ID with current time to ensure uniqueness.
        val requestCode = payload.getNotificationId() xor System.currentTimeMillis().toInt()

        // Create PendingIntent for notification tap
        val pendingIntent = PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "PendingIntent created: requestCode=$requestCode, intent=$intent")
        }

        return NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(payload.title)
            .setContentText(payload.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(payload.body))
            .setColor(NOTIFICATION_COLOR)
            .setPriority(getNotificationPriority(payload.type))
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .apply {
                // Add large icon if available
                largeIcon?.let { setLargeIcon(it) }

                // Add subtext for property address if available
                payload.propertyAddress?.let { setSubText(it) }

                // Add category
                setCategory(getNotificationCategory(payload.type))

                // Add group for threading
                payload.threadId?.let {
                    setGroup("thread_$it")
                }
            }
    }

    /**
     * Create intent with notification data for deep linking.
     */
    private fun createIntentForPayload(payload: NotificationPayload): Intent {
        val dataUri = when (payload.type) {
            NotificationType.MESSAGE, NotificationType.VIEWING, NotificationType.APPLICATION -> {
                payload.threadId?.let { android.net.Uri.parse("rhenti://thread/$it") }
                    ?: android.net.Uri.parse("rhenti://chats")
            }
            NotificationType.CALL -> {
                payload.contactId?.let { android.net.Uri.parse("rhenti://contact/$it") }
                    ?: payload.phoneNumber?.let { android.net.Uri.parse("rhenti://call/$it") }
                    ?: android.net.Uri.parse("rhenti://calls")
            }
            NotificationType.GENERAL -> android.net.Uri.parse("rhenti://chats")
        }

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "📱 Creating Notification Intent:")
            Log.d(TAG, "  Action: ${Intent.ACTION_VIEW}")
            Log.d(TAG, "  Data URI: $dataUri")
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            action = Intent.ACTION_VIEW
            data = dataUri
        }

        // Also add notification data as extras for fallback parsing
        val extras = DeepLinkHandler.createIntentExtras(payload)
        extras.forEach { (key, value) ->
            intent.putExtra(key, value)
        }

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "  Intent Extras:")
            extras.forEach { (key, value) ->
                Log.d(TAG, "    $key = $value")
            }
        }

        return intent
    }

    /**
     * Get notification priority based on type.
     */
    private fun getNotificationPriority(type: NotificationType): Int {
        return when (type) {
            NotificationType.MESSAGE, NotificationType.VIEWING -> NotificationCompat.PRIORITY_HIGH
            NotificationType.CALL -> NotificationCompat.PRIORITY_MAX
            else -> NotificationCompat.PRIORITY_DEFAULT
        }
    }

    /**
     * Get notification category for proper system handling.
     */
    private fun getNotificationCategory(type: NotificationType): String {
        return when (type) {
            NotificationType.MESSAGE -> NotificationCompat.CATEGORY_MESSAGE
            NotificationType.CALL -> NotificationCompat.CATEGORY_CALL
            else -> NotificationCompat.CATEGORY_STATUS
        }
    }

    /**
     * Load image from URL for large icon.
     */
    private suspend fun loadImage(url: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val connection = URL(url).openConnection()
            connection.doInput = true
            connection.connect()
            val input = connection.getInputStream()
            BitmapFactory.decodeStream(input)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Failed to load notification image: $url", e)
            }
            null
        }
    }

    /**
     * Cancel a notification by ID.
     */
    fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }

    /**
     * Cancel all notifications for a thread.
     */
    fun cancelThreadNotifications(threadId: String) {
        // Android doesn't provide a direct way to cancel by group
        // This would require tracking notification IDs separately
        // For now, we rely on auto-cancel when notification is tapped
    }
}
