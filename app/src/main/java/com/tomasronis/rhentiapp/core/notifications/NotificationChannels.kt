package com.tomasronis.rhentiapp.core.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

/**
 * Notification channels for the app.
 */
object NotificationChannels {
    const val CALL_CHANNEL_ID = "ongoing_calls"
    const val INCOMING_CALL_CHANNEL_ID = "incoming_calls"

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

            // Incoming calls channel (high importance with sound/vibration)
            val incomingCallChannel = NotificationChannel(
                INCOMING_CALL_CHANNEL_ID,
                "Incoming Calls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for incoming VoIP calls"
                setShowBadge(true)
                enableVibration(true)
                enableLights(true)
            }

            notificationManager.createNotificationChannel(callChannel)
            notificationManager.createNotificationChannel(incomingCallChannel)
        }
    }
}
