package com.tomasronis.rhentiapp.core.voip

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import com.tomasronis.rhentiapp.BuildConfig
import com.tomasronis.rhentiapp.R
import com.tomasronis.rhentiapp.core.notifications.NotificationChannels
import com.tomasronis.rhentiapp.presentation.calls.active.IncomingCallActivity
import com.twilio.voice.CallInvite
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/**
 * Foreground service that keeps the process alive during incoming call ringing.
 * Handles ringtone looping, vibration, wake lock, and the CallStyle notification.
 * Auto-times out after 45 seconds.
 *
 * Strategy for launching IncomingCallActivity reliably:
 *  1. startForeground() with fullScreenIntent notification (screen-off/locked fallback)
 *  2. startActivity() from the foreground service (works for screen-on cases)
 *  Only ONE mechanism will actually show the activity — fullScreenIntent only fires
 *  when the screen is off/locked; startActivity() only succeeds after foreground promotion.
 */
@AndroidEntryPoint
class IncomingCallService : Service() {

    @Inject
    lateinit var twilioManager: TwilioManager

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val handler = Handler(Looper.getMainLooper())
    private var isRinging = false

    companion object {
        private const val TAG = "IncomingCallService"
        const val NOTIFICATION_ID = 9001

        private const val ACTION_INCOMING_CALL = "ACTION_INCOMING_CALL"
        private const val ACTION_ACCEPT = "ACTION_ACCEPT"
        private const val ACTION_DECLINE = "ACTION_DECLINE"
        private const val ACTION_CANCELLED = "ACTION_CANCELLED"
        private const val ACTION_ACTIVITY_SHOWN = "ACTION_ACTIVITY_SHOWN"

        private const val EXTRA_CALL_SID = "CALL_SID"
        private const val EXTRA_CALLER = "CALLER"

        private const val TIMEOUT_MS = 45_000L

        /** Currently ringing CallInvite, accessible by IncomingCallActivity. */
        @Volatile
        var activeCallInvite: CallInvite? = null
            private set

        fun clearCallInvite() {
            activeCallInvite = null
        }

        /** Start ringing for an incoming call. */
        fun start(context: Context, callInvite: CallInvite) {
            activeCallInvite = callInvite
            val intent = Intent(context, IncomingCallService::class.java).apply {
                action = ACTION_INCOMING_CALL
                putExtra(EXTRA_CALL_SID, callInvite.callSid)
                putExtra(EXTRA_CALLER, callInvite.from ?: "Unknown")
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        /** User accepted the call from the UI. Stop ringing; CallService takes over. */
        fun accept(context: Context) {
            val intent = Intent(context, IncomingCallService::class.java).apply {
                action = ACTION_ACCEPT
            }
            context.startService(intent)
        }

        /** User declined the call. Reject invite and stop service. */
        fun decline(context: Context) {
            // Reject the invite immediately (don't wait for service round-trip)
            activeCallInvite?.reject(context)
            activeCallInvite = null

            val intent = Intent(context, IncomingCallService::class.java).apply {
                action = ACTION_DECLINE
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                // Service not running — cancel notification directly
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.cancel(NOTIFICATION_ID)
            }
        }

        /**
         * Called when IncomingCallActivity is visible.
         * Replaces the heads-up notification with a silent version so the user
         * doesn't see both the full-screen activity AND a heads-up popup.
         */
        fun onActivityShown(context: Context) {
            val intent = Intent(context, IncomingCallService::class.java).apply {
                action = ACTION_ACTIVITY_SHOWN
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "onActivityShown: service not running")
                }
            }
        }

        /** Remote party cancelled the call. Stop service. */
        fun callCancelled(context: Context) {
            activeCallInvite = null
            val intent = Intent(context, IncomingCallService::class.java).apply {
                action = ACTION_CANCELLED
            }
            try {
                context.startService(intent)
            } catch (e: Exception) {
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                nm.cancel(NOTIFICATION_ID)
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "callCancelled: service not running, cancelled notification directly")
                }
            }
        }
    }

    private val timeoutRunnable = Runnable {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Incoming call timed out after ${TIMEOUT_MS / 1000}s")
        }
        activeCallInvite?.reject(applicationContext)
        activeCallInvite = null
        twilioManager.handleCallCancelled()
        stopAndCleanup()
    }

    override fun onCreate() {
        super.onCreate()
        // Watch callState so we auto-stop if the call is answered/ended elsewhere
        twilioManager.callState
            .onEach { state ->
                if (isRinging && state is CallState.Active) {
                    stopAndCleanup()
                }
            }
            .launchIn(scope)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_INCOMING_CALL -> handleIncomingCall(intent)
            ACTION_ACCEPT -> handleAccept()
            ACTION_DECLINE -> handleDecline()
            ACTION_CANCELLED -> handleCancelled()
            ACTION_ACTIVITY_SHOWN -> demoteNotification()
            else -> stopSelf()
        }
        return START_REDELIVER_INTENT
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        handler.removeCallbacks(timeoutRunnable)
        handler.removeCallbacksAndMessages(null)
        stopRinging()
        scope.cancel()
        super.onDestroy()
    }

    // ─── Action handlers ───────────────────────────────────────────

    private fun handleIncomingCall(intent: Intent) {
        val callSid = intent.getStringExtra(EXTRA_CALL_SID)
        val caller = intent.getStringExtra(EXTRA_CALLER) ?: "Unknown"
        val callerDisplay = caller.removePrefix("client:").takeIf { it.isNotBlank() } ?: "Unknown Caller"

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Incoming call from $callerDisplay (sid=$callSid)")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                Log.d(TAG, "canUseFullScreenIntent() = ${nm.canUseFullScreenIntent()}")
            }
        }

        // 1. Wake the screen
        acquireWakeLock()

        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        val screenOn = pm.isInteractive
        val canOverlay = Settings.canDrawOverlays(this)

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Screen on=$screenOn, canDrawOverlays=$canOverlay")
        }

        // 2. Go foreground with the appropriate notification.
        //    - Screen OFF: Full CallStyle notification with fullScreenIntent (system shows
        //      the activity over lock screen). This MUST be PRIORITY_MAX.
        //    - Screen ON: Silent notification to avoid a heads-up flash. We'll launch
        //      the activity directly via startActivity() instead.
        val notification = if (screenOn) {
            buildSilentNotification()
        } else {
            buildCallNotification(callerDisplay, caller)
        }
        startForeground(NOTIFICATION_ID, notification)

        // 3. Start ringtone + vibration
        startRinging()

        // 4. Schedule timeout
        handler.postDelayed(timeoutRunnable, TIMEOUT_MS)

        // 5. Notify TwilioManager so the call UI shows the ringing state
        activeCallInvite?.let { twilioManager.handleIncomingCallInvite(it) }

        // 6. Launch IncomingCallActivity directly.
        //    After startForeground(), we have background-activity-start privileges.
        //    Use a 300ms delay to ensure the system has fully processed foreground promotion.
        //    For screen-off, the fullScreenIntent above already handled it, and
        //    IncomingCallActivity's singleTop mode will just onNewIntent() — not duplicate.
        handler.postDelayed({
            try {
                val activityIntent = Intent(this, IncomingCallActivity::class.java).apply {
                    this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                 Intent.FLAG_ACTIVITY_SINGLE_TOP or
                                 Intent.FLAG_ACTIVITY_NO_USER_ACTION
                    action = IncomingCallActivity.ACTION_INCOMING_CALL
                    putExtra("CALL_SID", callSid)
                    putExtra("CALLER_NUMBER", caller)
                }
                startActivity(activityIntent)
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "startActivity() succeeded (screenOn=$screenOn, overlay=$canOverlay)")
                }
            } catch (e: Exception) {
                // If startActivity() fails and screen is on, upgrade to full notification
                // so the user at least gets the heads-up CallStyle as fallback.
                if (screenOn) {
                    if (BuildConfig.DEBUG) {
                        Log.w(TAG, "startActivity() failed, upgrading to full notification")
                    }
                    try {
                        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        nm.notify(NOTIFICATION_ID, buildCallNotification(callerDisplay, caller))
                    } catch (_: Exception) {}
                }
                if (BuildConfig.DEBUG) {
                    Log.w(TAG, "startActivity() failed (screenOn=$screenOn, overlay=$canOverlay): ${e.message}")
                }
            }
        }, 300)
    }

    private fun handleAccept() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Call accepted - stopping ringing")
        }
        handler.removeCallbacks(timeoutRunnable)
        stopRinging()

        val invite = activeCallInvite
        if (invite != null) {
            CallService.startForIncomingAccepted(this, invite.from ?: "Unknown")
        }

        stopForeground(STOP_FOREGROUND_REMOVE)
        cancelNotification()
        stopSelf()
    }

    private fun handleDecline() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Call declined")
        }
        handler.removeCallbacks(timeoutRunnable)
        // activeCallInvite was already rejected in companion decline()
        // but reject again in case service got the intent via another path
        activeCallInvite?.reject(applicationContext)
        activeCallInvite = null
        twilioManager.handleCallCancelled()
        stopAndCleanup()
    }

    private fun handleCancelled() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Call cancelled by remote")
        }
        handler.removeCallbacks(timeoutRunnable)
        activeCallInvite = null
        twilioManager.handleCallCancelled()
        stopAndCleanup()
    }

    /** Stop ringing, remove notification, and stop the service. */
    private fun stopAndCleanup() {
        stopRinging()
        stopForeground(STOP_FOREGROUND_REMOVE)
        cancelNotification()
        stopSelf()
    }

    private fun cancelNotification() {
        try {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(NOTIFICATION_ID)
        } catch (e: Exception) {
            // Ignore
        }
    }

    /**
     * Replace the heads-up notification with a silent one.
     * Called when IncomingCallActivity is visible so the user doesn't see
     * both the full-screen activity AND a heads-up popup.
     */
    private fun demoteNotification() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Demoting notification to silent (activity is visible)")
        }
        try {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.notify(NOTIFICATION_ID, buildSilentNotification())
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "Failed to demote notification", e)
            }
        }
    }

    /**
     * Build a silent notification that keeps the foreground service alive
     * but doesn't show as heads-up or full-screen.
     */
    private fun buildSilentNotification(): Notification {
        // Decline action → broadcast to IncomingCallReceiver
        val declineIntent = Intent(this, IncomingCallReceiver::class.java).apply {
            action = "com.rhentimobile.DECLINE_CALL"
        }
        val declinePi = PendingIntent.getBroadcast(
            this, NOTIFICATION_ID + 2,
            declineIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        // Content intent → re-open IncomingCallActivity if user taps notification
        val contentIntent = Intent(this, IncomingCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            action = IncomingCallActivity.ACTION_INCOMING_CALL
        }
        val contentPi = PendingIntent.getActivity(
            this, NOTIFICATION_ID,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, NotificationChannels.INCOMING_CALL_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Incoming Call")
            .setContentText("Tap to return to call")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(contentPi)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Decline", declinePi)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    // ─── Notification ──────────────────────────────────────────────

    private fun buildCallNotification(callerDisplay: String, callerRaw: String): Notification {
        // Full-screen intent → IncomingCallActivity (screen-off/locked only)
        val fullScreenIntent = Intent(this, IncomingCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            action = IncomingCallActivity.ACTION_INCOMING_CALL
            putExtra("CALLER_NUMBER", callerRaw)
        }
        val fullScreenPi = PendingIntent.getActivity(
            this, NOTIFICATION_ID,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Answer action → opens IncomingCallActivity with auto-answer
        val answerIntent = Intent(this, IncomingCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            action = IncomingCallActivity.ACTION_ANSWER_CALL
            putExtra("CALLER_NUMBER", callerRaw)
        }
        val answerPi = PendingIntent.getActivity(
            this, NOTIFICATION_ID + 1,
            answerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Decline action → broadcast to IncomingCallReceiver
        val declineIntent = Intent(this, IncomingCallReceiver::class.java).apply {
            action = "com.rhentimobile.DECLINE_CALL"
        }
        val declinePi = PendingIntent.getBroadcast(
            this, NOTIFICATION_ID + 2,
            declineIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val callerPerson = Person.Builder()
            .setName(callerDisplay)
            .setImportant(true)
            .build()

        return NotificationCompat.Builder(this, NotificationChannels.INCOMING_CALL_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Incoming Call")
            .setContentText(callerDisplay)
            .setStyle(
                NotificationCompat.CallStyle.forIncomingCall(
                    callerPerson, declinePi, answerPi
                )
            )
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setOngoing(true)
            .setAutoCancel(false)
            .setFullScreenIntent(fullScreenPi, true)
            .setContentIntent(fullScreenPi)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setVibrate(longArrayOf(0, 1000, 500, 1000))
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    // ─── Ringtone & Vibration ──────────────────────────────────────

    private fun startRinging() {
        if (isRinging) return
        isRinging = true

        try {
            val ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(applicationContext, ringtoneUri)
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Failed to start ringtone", e)
            }
        }

        try {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }

            val pattern = longArrayOf(0, 1000, 500, 1000, 500, 1000)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(
                    VibrationEffect.createWaveform(pattern, 0)
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Failed to start vibration", e)
            }
        }
    }

    private fun stopRinging() {
        isRinging = false

        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (e: Exception) {
            // Ignore
        }
        mediaPlayer = null

        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            // Ignore
        }
        vibrator = null

        releaseWakeLock()
    }

    // ─── Wake Lock ─────────────────────────────────────────────────

    private fun acquireWakeLock() {
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            @Suppress("DEPRECATION")
            wakeLock = pm.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                PowerManager.ACQUIRE_CAUSES_WAKEUP or
                PowerManager.ON_AFTER_RELEASE,
                "rhenti:incoming_call"
            )
            wakeLock?.acquire(TIMEOUT_MS + 5_000)
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Wake lock acquired (screen interactive=${pm.isInteractive})")
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Failed to acquire wake lock", e)
            }
        }
    }

    private fun releaseWakeLock() {
        try {
            wakeLock?.let {
                if (it.isHeld) it.release()
            }
        } catch (e: Exception) {
            // Ignore
        }
        wakeLock = null
    }
}
