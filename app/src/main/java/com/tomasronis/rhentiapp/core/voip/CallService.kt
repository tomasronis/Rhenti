package com.tomasronis.rhentiapp.core.voip

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.tomasronis.rhentiapp.R
import com.tomasronis.rhentiapp.core.notifications.NotificationChannels
import com.tomasronis.rhentiapp.presentation.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/**
 * Foreground service for managing VoIP calls.
 */
@AndroidEntryPoint
class CallService : Service() {

    @Inject
    lateinit var twilioManager: TwilioManager

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val notificationId = 1001

    companion object {
        private const val ACTION_START_CALL = "START_CALL"
        private const val ACTION_END_CALL = "END_CALL"
        private const val ACTION_ACCEPT_CALL = "ACCEPT_CALL"
        private const val ACTION_REJECT_CALL = "REJECT_CALL"
        private const val ACTION_INCOMING_ACCEPTED = "INCOMING_ACCEPTED"

        private const val EXTRA_PHONE_NUMBER = "phone_number"

        fun startCall(context: Context, phoneNumber: String) {
            val intent = Intent(context, CallService::class.java).apply {
                action = ACTION_START_CALL
                putExtra(EXTRA_PHONE_NUMBER, phoneNumber)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun endCall(context: Context) {
            val intent = Intent(context, CallService::class.java).apply {
                action = ACTION_END_CALL
            }
            context.startService(intent)
        }

        /**
         * Start this service as the ongoing-call foreground service after accepting an incoming call.
         * IncomingCallService calls this so the user sees "Ongoing Call" in the notification tray.
         */
        fun startForIncomingAccepted(context: Context, phoneNumber: String) {
            val intent = Intent(context, CallService::class.java).apply {
                action = ACTION_INCOMING_ACCEPTED
                putExtra(EXTRA_PHONE_NUMBER, phoneNumber)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        // Observe call state changes
        twilioManager.callState
            .onEach { state ->
                when (state) {
                    is CallState.Active -> {
                        updateNotification(state)
                    }
                    is CallState.Ended, is CallState.Failed -> {
                        stopForeground(STOP_FOREGROUND_REMOVE)
                        stopSelf()
                    }
                    else -> {
                        // Update notification for other states if needed
                    }
                }
            }
            .launchIn(scope)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_CALL -> {
                val phoneNumber = intent.getStringExtra(EXTRA_PHONE_NUMBER) ?: return START_NOT_STICKY
                startForeground(notificationId, createNotification("Calling $phoneNumber..."))
                twilioManager.makeOutgoingCall(phoneNumber)
            }
            ACTION_END_CALL -> {
                twilioManager.endCall()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_ACCEPT_CALL -> {
                // TODO: Handle accept call action
            }
            ACTION_REJECT_CALL -> {
                twilioManager.rejectIncomingCall()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            ACTION_INCOMING_ACCEPTED -> {
                val phoneNumber = intent.getStringExtra(EXTRA_PHONE_NUMBER) ?: "Unknown"
                startForeground(notificationId, createNotification("Connected - $phoneNumber"))
            }
        }

        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    /**
     * Create notification for ongoing call
     */
    private fun createNotification(contentText: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // End call action
        val endCallIntent = Intent(this, CallService::class.java).apply {
            action = ACTION_END_CALL
        }
        val endCallPendingIntent = PendingIntent.getService(
            this,
            0,
            endCallIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, NotificationChannels.CALL_CHANNEL_ID)
            .setContentTitle("Ongoing Call")
            .setContentText(contentText)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(
                R.drawable.ic_launcher_foreground,
                "End Call",
                endCallPendingIntent
            )
            .build()
    }

    /**
     * Update notification with call state
     */
    private fun updateNotification(state: CallState.Active) {
        val minutes = state.duration / 60
        val seconds = state.duration % 60
        val durationText = String.format("%02d:%02d", minutes, seconds)

        val notification = createNotification("$durationText - ${state.phoneNumber}")

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, notification)
    }
}
