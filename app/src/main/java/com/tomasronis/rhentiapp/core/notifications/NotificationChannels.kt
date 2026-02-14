package com.tomasronis.rhentiapp.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build

/**
 * Notification channels for the app.
 */
object NotificationChannels {
    // VoIP Call channels (Phase 7)
    const val CALL_CHANNEL_ID = "ongoing_calls"
    const val INCOMING_CALL_CHANNEL_ID = "incoming_calls_v2"

    // Push Notification channels (Phase 8)
    const val MESSAGES_CHANNEL_ID = "messages"
    const val VIEWINGS_CHANNEL_ID = "viewings"
    const val APPLICATIONS_CHANNEL_ID = "applications"
    const val GENERAL_CHANNEL_ID = "general"

    /**
     * Create notification channels.
     * Must be called once on app startup.
     */
    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            // Ongoing calls channel (low importance, no sound)
            val callChannel = NotificationChannel(
                CALL_CHANNEL_ID,
                "Ongoing Calls",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications for ongoing VoIP calls"
                setShowBadge(false)
                enableVibration(false)
                setSound(null, null)
            }

            // Incoming calls channel (max importance with ringtone/vibration)
            val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()

            val incomingCallChannel = NotificationChannel(
                INCOMING_CALL_CHANNEL_ID,
                "Incoming Calls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for incoming VoIP calls"
                setShowBadge(true)
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 1000, 500, 1000, 500, 1000)
                enableLights(true)
                lightColor = android.graphics.Color.GREEN
                setSound(ringtoneUri, audioAttributes)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
            }

            // Messages channel (high importance)
            val messagesChannel = NotificationChannel(
                MESSAGES_CHANNEL_ID,
                "Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for new messages"
                setShowBadge(true)
                enableVibration(true)
                enableLights(true)
            }

            // Viewing requests channel (high importance)
            val viewingsChannel = NotificationChannel(
                VIEWINGS_CHANNEL_ID,
                "Viewing Requests",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for new viewing requests"
                setShowBadge(true)
                enableVibration(true)
                enableLights(true)
            }

            // Applications channel (default importance)
            val applicationsChannel = NotificationChannel(
                APPLICATIONS_CHANNEL_ID,
                "Applications",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications for application updates"
                setShowBadge(true)
            }

            // General channel (default importance)
            val generalChannel = NotificationChannel(
                GENERAL_CHANNEL_ID,
                "General",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "General notifications"
                setShowBadge(false)
            }

            // Delete old incoming calls channel (Android caches channel settings after
            // first creation, so we need a new channel ID to ensure correct settings)
            notificationManager.deleteNotificationChannel("incoming_calls")

            notificationManager.createNotificationChannel(callChannel)
            notificationManager.createNotificationChannel(incomingCallChannel)
            notificationManager.createNotificationChannel(messagesChannel)
            notificationManager.createNotificationChannel(viewingsChannel)
            notificationManager.createNotificationChannel(applicationsChannel)
            notificationManager.createNotificationChannel(generalChannel)
        }
    }
}
