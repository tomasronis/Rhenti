package com.tomasronis.rhentiapp.core.voip

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
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
            val intent = Intent(context, IncomingCallService::class.java).apply {
                action = ACTION_DECLINE
            }
            context.startService(intent)
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
                // Service may not be running yet
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "callCancelled: service not running, ignoring")
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
        stopRinging()
        stopSelf()
    }

    override fun onCreate() {
        super.onCreate()
        // Watch callState so we auto-stop if the call is answered/ended elsewhere
        twilioManager.callState
            .onEach { state ->
                if (isRinging && state is CallState.Active) {
                    // Call was accepted (possibly from another path) - stop ringing
                    stopRinging()
                    stopSelf()
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
            else -> {
                // Unexpected - stop self
                stopSelf()
            }
        }
        return START_REDELIVER_INTENT
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        handler.removeCallbacks(timeoutRunnable)
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

            // Log full-screen intent permission status (critical on Android 14+)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val canFullScreen = nm.canUseFullScreenIntent()
                Log.d(TAG, "canUseFullScreenIntent() = $canFullScreen")
                if (!canFullScreen) {
                    Log.w(TAG, "FULL_SCREEN_INTENT permission NOT granted! Notification will not launch full-screen activity.")
                }
            }
        }

        // 1. Wake the screen FIRST (always, not conditionally)
        acquireWakeLock()

        // 2. Build notification with fullScreenIntent
        val notification = buildCallNotification(callerDisplay, caller)

        // 3. Post via NotificationManager.notify() FIRST.
        //    Some Samsung/OEM devices only trigger fullScreenIntent from notify(),
        //    not from startForeground(). Posting first ensures the system sees the
        //    fullScreenIntent and can launch the activity immediately.
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIFICATION_ID, notification)

        // 4. Then promote to foreground service (takes over the same notification ID)
        startForeground(NOTIFICATION_ID, notification)

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Notification posted + startForeground called")
        }

        // 5. Start ringtone + vibration
        startRinging()

        // 6. Schedule timeout
        handler.postDelayed(timeoutRunnable, TIMEOUT_MS)

        // 7. Notify TwilioManager so the UI can show the ringing state
        activeCallInvite?.let { twilioManager.handleIncomingCallInvite(it) }

        // 8. After a short delay (giving system time to process foreground promotion),
        //    try to launch IncomingCallActivity directly. Now that we're a foreground
        //    service, we should have background activity start privileges.
        handler.postDelayed({
            try {
                val activityIntent = Intent(this, IncomingCallActivity::class.java).apply {
                    this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                                 Intent.FLAG_ACTIVITY_CLEAR_TOP or
                                 Intent.FLAG_ACTIVITY_NO_USER_ACTION
                    action = IncomingCallActivity.ACTION_INCOMING_CALL
                    putExtra("CALL_SID", callSid)
                    putExtra("CALLER_NUMBER", caller)
                }
                startActivity(activityIntent)
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Direct activity start succeeded")
                }
            } catch (e: Exception) {
                // fullScreenIntent on the notification handles this case
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Direct activity start failed: ${e.message}")
                }
            }
        }, 200) // 200ms delay for foreground promotion to take effect
    }

    private fun handleAccept() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Call accepted - stopping ringing")
        }
        handler.removeCallbacks(timeoutRunnable)
        stopRinging()

        // Start the ongoing-call foreground service
        val invite = activeCallInvite
        if (invite != null) {
            CallService.startForIncomingAccepted(this, invite.from ?: "Unknown")
        }

        stopSelf()
    }

    private fun handleDecline() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Call declined")
        }
        handler.removeCallbacks(timeoutRunnable)
        activeCallInvite?.reject(applicationContext)
        activeCallInvite = null
        twilioManager.handleCallCancelled()
        stopRinging()
        stopSelf()
    }

    private fun handleCancelled() {
        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Call cancelled by remote")
        }
        handler.removeCallbacks(timeoutRunnable)
        activeCallInvite = null
        twilioManager.handleCallCancelled()
        stopRinging()
        stopSelf()
    }

    // ─── Notification ──────────────────────────────────────────────

    private fun buildCallNotification(callerDisplay: String, callerRaw: String): Notification {
        // Full-screen intent → IncomingCallActivity
        val fullScreenIntent = Intent(this, IncomingCallActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
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
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
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
            // Vibration pattern on the notification so the system sees it as high-priority.
            // (Ringtone is handled by MediaPlayer; continuous vibration by the Vibrator service.)
            .setVibrate(longArrayOf(0, 1000, 500, 1000))
            // Show foreground service notification immediately (Android 12+ delays by ~10s otherwise)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    // ─── Ringtone & Vibration ──────────────────────────────────────

    private fun startRinging() {
        if (isRinging) return
        isRinging = true

        // Ringtone via MediaPlayer (loops)
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

        // Vibration (repeating pattern)
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
                    VibrationEffect.createWaveform(pattern, 0) // repeat from index 0
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
            // Always acquire — even when screen is on, this ensures the device stays
            // awake and ACQUIRE_CAUSES_WAKEUP turns the screen on when it's off.
            @Suppress("DEPRECATION")
            wakeLock = pm.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK or
                PowerManager.ACQUIRE_CAUSES_WAKEUP or
                PowerManager.ON_AFTER_RELEASE,
                "rhenti:incoming_call"
            )
            wakeLock?.acquire(TIMEOUT_MS + 5_000) // auto-release slightly after timeout
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
