package com.tomasronis.rhentiapp.core.voip

import android.content.Context
import android.util.Log
import com.tomasronis.rhentiapp.BuildConfig
import com.tomasronis.rhentiapp.core.network.NetworkResult
import com.tomasronis.rhentiapp.core.security.TokenManager
import com.tomasronis.rhentiapp.core.utils.PhoneNumberFormatter
import com.tomasronis.rhentiapp.data.calls.models.CallStatus
import com.tomasronis.rhentiapp.data.calls.models.CallType
import com.tomasronis.rhentiapp.data.calls.repository.CallsRepository
import com.twilio.voice.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Call state sealed class
 */
sealed class CallState {
    object Idle : CallState()
    data class Ringing(val phoneNumber: String, val callSid: String?) : CallState()
    data class Dialing(val phoneNumber: String) : CallState()
    data class Active(
        val phoneNumber: String,
        val callSid: String?,
        val duration: Int = 0,
        val isMuted: Boolean = false,
        val isSpeakerOn: Boolean = false
    ) : CallState()
    data class Ended(val reason: String?) : CallState()
    data class Failed(val error: String) : CallState()
}

/**
 * Singleton manager for Twilio Voice SDK.
 * Handles call lifecycle, audio routing, and call state management.
 */
@Singleton
class TwilioManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val callsRepository: CallsRepository,
    private val tokenManager: TokenManager
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val audioManager = VoipAudioManager(context)

    private var accessToken: String? = null
    private var clientIdentity: String? = null  // Stored user ID for Twilio calls
    private var activeCall: Call? = null
    private var callInvite: CallInvite? = null

    private val _callState = MutableStateFlow<CallState>(CallState.Idle)
    val callState: StateFlow<CallState> = _callState.asStateFlow()

    private var callStartTime: Long = 0
    private var durationTimerJob: kotlinx.coroutines.Job? = null

    companion object {
        private const val TAG = "TwilioManager"
        private const val TOKEN_REFRESH_MARGIN_MS = 5 * 60 * 1000 // 5 minutes
    }

    /**
     * Initialize Twilio SDK with access token
     */
    suspend fun initialize() {
        try {
            val userId = tokenManager.getUserId() ?: return
            val email = tokenManager.getUserEmail() ?: return
            val account = tokenManager.getAccount() ?: return
            val childAccount = tokenManager.getChildAccount() ?: return

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Initializing Twilio SDK - userId: $userId, email: $email, account: $account, childAccount: $childAccount")
            }

            // Get Twilio access token from API
            // Use userId as identity (iOS uses device unique ID, but userId works as unique identifier)
            when (val result = callsRepository.getTwilioAccessToken(
                identity = userId,
                os = "android",
                email = email,
                account = account,
                childAccount = childAccount
            )) {
                is NetworkResult.Success -> {
                    accessToken = result.data
                    clientIdentity = userId  // Store user ID for call parameters
                    Voice.setLogLevel(if (BuildConfig.DEBUG) LogLevel.DEBUG else LogLevel.ERROR)

                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "Twilio SDK initialized successfully")
                    }
                }
                is NetworkResult.Error -> {
                    if (BuildConfig.DEBUG) {
                        Log.e(TAG, "Failed to get Twilio access token", result.exception)
                    }
                }
                is NetworkResult.Loading -> {
                    // Still loading, do nothing
                }
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Failed to initialize Twilio SDK", e)
            }
        }
    }

    /**
     * Make an outgoing call
     */
    fun makeOutgoingCall(phoneNumber: String) {
        val token = accessToken
        if (token == null) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Cannot make call: No access token")
            }
            _callState.value = CallState.Failed("Not authenticated. Please try again.")
            return
        }

        try {
            // Format phone number to E.164 format for Twilio
            val formattedNumber = PhoneNumberFormatter.formatForTwilio(phoneNumber)

            // Validate the formatted number
            if (!isValidE164Number(formattedNumber)) {
                if (BuildConfig.DEBUG) {
                    Log.e(TAG, "Invalid phone number format: $phoneNumber -> $formattedNumber")
                }
                _callState.value = CallState.Failed("Invalid phone number. Please check the number and try again.")
                return
            }

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Making outgoing call to: $phoneNumber (formatted: $formattedNumber)")
            }

            _callState.value = CallState.Dialing(formattedNumber)

            // Backend workaround: Backend checks request.body.Caller.includes('client')
            // but Twilio sends caller as "From", not "Caller"
            // iOS sends both "to" (lowercase) and "Caller" parameters
            val params = hashMapOf<String, String>().apply {
                put("to", phoneNumber)  // lowercase "to" - backend reads this
                put("Caller", "client:${clientIdentity ?: "android"}")  // Backend needs this
            }

            val connectOptions = ConnectOptions.Builder(token)
                .params(params)
                .build()

            activeCall = Voice.connect(context, connectOptions, callListener)
            audioManager.setAudioRoute(VoipAudioManager.AudioRoute.EARPIECE)

        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Failed to make outgoing call", e)
            }
            _callState.value = CallState.Failed(e.message ?: "Failed to make call")
        }
    }

    /**
     * Validate E.164 phone number format
     * E.164 format: +[1-15 digits]
     */
    private fun isValidE164Number(phoneNumber: String): Boolean {
        // Must start with +
        if (!phoneNumber.startsWith("+")) return false

        // Must have only digits after +
        val digits = phoneNumber.substring(1)
        if (!digits.all { it.isDigit() }) return false

        // Must have between 7 and 15 digits (E.164 standard)
        return digits.length in 7..15
    }

    /**
     * Accept incoming call
     */
    fun acceptIncomingCall(invite: CallInvite) {
        try {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Accepting incoming call from: ${invite.from}")
            }

            val acceptOptions = AcceptOptions.Builder()
                .build()

            activeCall = invite.accept(context, acceptOptions, callListener)
            callInvite = null

            audioManager.setAudioRoute(VoipAudioManager.AudioRoute.EARPIECE)

        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Failed to accept call", e)
            }
            _callState.value = CallState.Failed(e.message ?: "Failed to accept call")
        }
    }

    /**
     * Reject incoming call
     */
    fun rejectIncomingCall() {
        try {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Rejecting incoming call")
            }

            callInvite?.reject(context)
            callInvite = null
            _callState.value = CallState.Idle

        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Failed to reject call", e)
            }
        }
    }

    /**
     * End active call
     */
    fun endCall() {
        try {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Ending call")
            }

            activeCall?.disconnect()
            stopDurationTimer()

        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Failed to end call", e)
            }
        }
    }

    /**
     * Toggle mute
     */
    fun toggleMute(): Boolean {
        val call = activeCall ?: return false
        val newMuteState = !call.isMuted

        call.mute(newMuteState)

        if (_callState.value is CallState.Active) {
            val currentState = _callState.value as CallState.Active
            _callState.value = currentState.copy(isMuted = newMuteState)
        }

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Mute toggled: $newMuteState")
        }

        return newMuteState
    }

    /**
     * Toggle speaker
     */
    fun toggleSpeaker(): Boolean {
        val currentSpeakerState = (_callState.value as? CallState.Active)?.isSpeakerOn ?: false
        val newSpeakerState = !currentSpeakerState

        audioManager.setAudioRoute(
            if (newSpeakerState) VoipAudioManager.AudioRoute.SPEAKER
            else VoipAudioManager.AudioRoute.EARPIECE
        )

        if (_callState.value is CallState.Active) {
            val currentState = _callState.value as CallState.Active
            _callState.value = currentState.copy(isSpeakerOn = newSpeakerState)
        }

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Speaker toggled: $newSpeakerState")
        }

        return newSpeakerState
    }

    /**
     * Send DTMF digits
     */
    fun sendDigits(digits: String) {
        activeCall?.sendDigits(digits)

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Sent DTMF digits: $digits")
        }
    }

    /**
     * Call listener
     */
    private val callListener = object : Call.Listener {
        override fun onConnectFailure(call: Call, error: CallException) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Call connect failure: ${error.message} (Error code: ${error.errorCode})")
            }

            // Provide user-friendly error message based on Twilio error
            val userMessage = when {
                error.message?.contains("invalid", ignoreCase = true) == true ->
                    "Invalid phone number. Please check the number and try again."
                error.message?.contains("permission", ignoreCase = true) == true ->
                    "Unable to call this number. Please contact support."
                error.message?.contains("network", ignoreCase = true) == true ->
                    "Network error. Please check your connection."
                error.message?.contains("not permitted", ignoreCase = true) == true ->
                    "This number cannot be called. Please verify the number."
                else -> error.message ?: "Call failed. Please try again."
            }

            _callState.value = CallState.Failed(userMessage)
            recordCallLog(call, CallStatus.FAILED)
            cleanup()
        }

        override fun onRinging(call: Call) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Call ringing")
            }

            val phoneNumber = call.to ?: "Unknown"
            _callState.value = CallState.Ringing(phoneNumber, call.sid)
        }

        override fun onConnected(call: Call) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Call connected")
            }

            val phoneNumber = call.to ?: call.from ?: "Unknown"
            _callState.value = CallState.Active(
                phoneNumber = phoneNumber,
                callSid = call.sid,
                duration = 0,
                isMuted = call.isMuted ?: false,
                isSpeakerOn = false
            )

            callStartTime = System.currentTimeMillis()
            startDurationTimer()
        }

        override fun onReconnecting(call: Call, error: CallException) {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, "Call reconnecting: ${error.message}")
            }
        }

        override fun onReconnected(call: Call) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Call reconnected")
            }
        }

        override fun onDisconnected(call: Call, error: CallException?) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Call disconnected: ${error?.message ?: "Normal"}")
            }

            _callState.value = CallState.Ended(error?.message)

            val status = if (error != null) CallStatus.FAILED else CallStatus.COMPLETED
            recordCallLog(call, status)

            cleanup()
        }

        override fun onCallQualityWarningsChanged(
            call: Call,
            currentWarnings: MutableSet<Call.CallQualityWarning>,
            previousWarnings: MutableSet<Call.CallQualityWarning>
        ) {
            if (BuildConfig.DEBUG && currentWarnings.isNotEmpty()) {
                Log.w(TAG, "Call quality warnings: $currentWarnings")
            }
        }
    }

    /**
     * Start duration timer
     */
    private fun startDurationTimer() {
        durationTimerJob?.cancel()
        durationTimerJob = scope.launch {
            while (true) {
                kotlinx.coroutines.delay(1000) // Update every second

                if (_callState.value is CallState.Active) {
                    val duration = ((System.currentTimeMillis() - callStartTime) / 1000).toInt()
                    val currentState = _callState.value as CallState.Active
                    _callState.value = currentState.copy(duration = duration)
                }
            }
        }
    }

    /**
     * Stop duration timer
     */
    private fun stopDurationTimer() {
        durationTimerJob?.cancel()
        durationTimerJob = null
    }

    /**
     * Record call log after call ends
     */
    private fun recordCallLog(call: Call, status: CallStatus) {
        scope.launch {
            try {
                val phoneNumber = call.to ?: call.from ?: return@launch
                val duration = if (callStartTime > 0) {
                    ((System.currentTimeMillis() - callStartTime) / 1000).toInt()
                } else {
                    0
                }

                val callType = if (call.to != null) CallType.OUTGOING else CallType.INCOMING

                callsRepository.recordCallLog(
                    contactId = null, // TODO: Look up contact by phone number
                    phoneNumber = phoneNumber,
                    callType = callType,
                    duration = duration,
                    twilioCallSid = call.sid,
                    status = status
                )

                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Call log recorded: $phoneNumber, duration: $duration")
                }
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) {
                    Log.e(TAG, "Failed to record call log", e)
                }
            }
        }
    }

    /**
     * Cleanup after call ends
     */
    private fun cleanup() {
        activeCall = null
        callInvite = null
        callStartTime = 0
        stopDurationTimer()
        audioManager.cleanup()
    }

    /**
     * Release resources
     */
    fun release() {
        cleanup()
        scope.launch {
            // Cancel any ongoing operations
        }
    }
}
