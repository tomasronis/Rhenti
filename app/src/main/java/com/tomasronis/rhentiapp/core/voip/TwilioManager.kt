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
import com.tomasronis.rhentiapp.data.chathub.repository.ChatHubRepository
import com.twilio.voice.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
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
    data class Ringing(
        val phoneNumber: String,
        val callSid: String?,
        val contactName: String? = null,
        val contactAvatar: String? = null
    ) : CallState()
    data class Dialing(
        val phoneNumber: String,
        val contactName: String? = null,
        val contactAvatar: String? = null
    ) : CallState()
    data class Active(
        val phoneNumber: String,
        val callSid: String?,
        val duration: Int = 0,
        val isMuted: Boolean = false,
        val isSpeakerOn: Boolean = false,
        val contactName: String? = null,
        val contactAvatar: String? = null
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
    private val tokenManager: TokenManager,
    private val contactsRepository: com.tomasronis.rhentiapp.data.contacts.repository.ContactsRepository,
    private val chatHubRepository: ChatHubRepository
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

    // Store the phone number and call type for logging after call ends
    private var currentCallPhoneNumber: String? = null
    private var currentCallType: CallType? = null
    private var currentContactName: String? = null
    private var currentContactAvatar: String? = null
    private var currentContactId: String? = null

    /**
     * Contact info data class for lookup results from contacts or threads.
     */
    data class ContactInfo(
        val id: String?,
        val displayName: String,
        val avatarUrl: String?
    )

    /**
     * Look up contact by phone number from contacts list and chat threads.
     * Returns a ContactInfo with the best available name and avatar.
     */
    private suspend fun lookupContactInfoByPhone(phoneNumber: String): ContactInfo? {
        val normalizedSearch = phoneNumber.replace(Regex("[^0-9+]"), "")

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Looking up contact info for: $phoneNumber (normalized: $normalizedSearch)")
        }

        // First try: look up from contacts repository
        val contactInfo = lookupFromContacts(normalizedSearch)
        if (contactInfo != null) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Found contact from contacts repo: ${contactInfo.displayName}, avatar: ${contactInfo.avatarUrl}")
            }
            return contactInfo
        }

        // Second try: look up from chat threads (message threads may have contact info)
        val threadInfo = lookupFromThreads(normalizedSearch)
        if (threadInfo != null) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Found contact from threads: ${threadInfo.displayName}, avatar: ${threadInfo.avatarUrl}")
            }
            return threadInfo
        }

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "No contact info found for $phoneNumber in contacts or threads")
        }
        return null
    }

    /**
     * Look up contact by phone number from contacts repository.
     */
    private suspend fun lookupFromContacts(normalizedSearch: String): ContactInfo? {
        return try {
            val superAccountId = tokenManager.getSuperAccountId() ?: return null

            when (val result = contactsRepository.getContacts(superAccountId)) {
                is NetworkResult.Success -> {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "Got ${result.data.size} contacts to search")
                    }

                    val contact = result.data.firstOrNull { contact ->
                        val contactPhone = contact.phone?.replace(Regex("[^0-9+]"), "") ?: ""
                        contactPhone == normalizedSearch ||
                        contactPhone.endsWith(normalizedSearch.takeLast(10)) ||
                        normalizedSearch.endsWith(contactPhone.takeLast(10))
                    }

                    contact?.let {
                        ContactInfo(
                            id = it.id,
                            displayName = it.displayName,
                            avatarUrl = it.avatarUrl
                        )
                    }
                }
                else -> null
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Failed to lookup from contacts", e)
            }
            null
        }
    }

    /**
     * Look up contact by phone number from cached chat threads.
     * Threads store phone, displayName, and imageUrl for each participant.
     */
    private suspend fun lookupFromThreads(normalizedSearch: String): ContactInfo? {
        return try {
            val threads = chatHubRepository.observeThreads().firstOrNull() ?: return null

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Searching ${threads.size} cached threads for phone match")
            }

            val matchingThread = threads.firstOrNull { thread ->
                val threadPhone = thread.phone?.replace(Regex("[^0-9+]"), "") ?: ""
                threadPhone.isNotBlank() && (
                    threadPhone == normalizedSearch ||
                    threadPhone.endsWith(normalizedSearch.takeLast(10)) ||
                    normalizedSearch.endsWith(threadPhone.takeLast(10))
                )
            }

            matchingThread?.let {
                ContactInfo(
                    id = it.renterId ?: it.id,
                    displayName = it.displayName,
                    avatarUrl = it.imageUrl
                )
            }
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Failed to lookup from threads", e)
            }
            null
        }
    }

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

            // Store for call logging
            currentCallPhoneNumber = formattedNumber
            currentCallType = CallType.OUTGOING

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Stored for logging - phoneNumber: $currentCallPhoneNumber, callType: $currentCallType")
            }

            // Look up contact information before setting state (from contacts AND threads)
            scope.launch {
                val contactInfo = lookupContactInfoByPhone(formattedNumber)

                if (contactInfo != null) {
                    currentContactId = contactInfo.id
                    currentContactName = contactInfo.displayName
                    currentContactAvatar = contactInfo.avatarUrl

                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "Found contact info: ${contactInfo.displayName}, avatar: ${contactInfo.avatarUrl}")
                    }

                    // Update state with contact info
                    if (_callState.value is CallState.Dialing) {
                        _callState.value = CallState.Dialing(
                            phoneNumber = formattedNumber,
                            contactName = contactInfo.displayName,
                            contactAvatar = contactInfo.avatarUrl
                        )
                    }
                } else {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "No contact info found for $formattedNumber in contacts or threads")
                    }
                }
            }

            // Set initial state (contact info will be added by lookup above)
            _callState.value = CallState.Dialing(formattedNumber)

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Set Dialing state with phoneNumber: $formattedNumber")
            }

            // Backend workaround: Backend checks request.body.Caller.includes('client')
            // but Twilio sends caller as "From", not "Caller"
            // iOS sends both "to" (lowercase) and "Caller" parameters
            val params = hashMapOf<String, String>().apply {
                put("to", formattedNumber)  // Use formatted E.164 number - backend reads this
                put("Caller", "client:${clientIdentity ?: "android"}")  // Backend needs this
            }

            val connectOptions = ConnectOptions.Builder(token)
                .params(params)
                .build()

            activeCall = Voice.connect(context, connectOptions, callListener)
            audioManager.requestAudioFocus()
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

            // Store for call logging
            currentCallPhoneNumber = invite.from
            currentCallType = CallType.INCOMING

            // Look up contact info for the caller (from contacts AND threads)
            val callerNumber = invite.from
            if (callerNumber != null) {
                scope.launch {
                    val contactInfo = lookupContactInfoByPhone(callerNumber)
                    if (contactInfo != null) {
                        currentContactId = contactInfo.id
                        currentContactName = contactInfo.displayName
                        currentContactAvatar = contactInfo.avatarUrl

                        if (BuildConfig.DEBUG) {
                            Log.d(TAG, "Found incoming caller info: ${contactInfo.displayName}, avatar: ${contactInfo.avatarUrl}")
                        }

                        // Update call state with contact info
                        val currentState = _callState.value
                        when (currentState) {
                            is CallState.Ringing -> {
                                _callState.value = currentState.copy(
                                    contactName = contactInfo.displayName,
                                    contactAvatar = contactInfo.avatarUrl
                                )
                            }
                            is CallState.Active -> {
                                _callState.value = currentState.copy(
                                    contactName = contactInfo.displayName,
                                    contactAvatar = contactInfo.avatarUrl
                                )
                            }
                            else -> {} // Do nothing for other states
                        }
                    }
                }
            }

            val acceptOptions = AcceptOptions.Builder()
                .build()

            activeCall = invite.accept(context, acceptOptions, callListener)
            callInvite = null

            audioManager.requestAudioFocus()
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
                Log.d(TAG, "Call ringing - call.to: ${call.to}, call.from: ${call.from}")
            }

            // Preserve the phone number and contact info from Dialing state
            val currentState = _callState.value
            val (phoneNumber, contactName, contactAvatar) = if (currentState is CallState.Dialing) {
                Triple(currentState.phoneNumber, currentState.contactName, currentState.contactAvatar)
            } else {
                Triple(call.from ?: "Unknown", null, null) // For incoming calls
            }

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Call ringing - using phoneNumber: $phoneNumber, contactName: $contactName")
            }

            _callState.value = CallState.Ringing(phoneNumber, call.sid, contactName, contactAvatar)
        }

        override fun onConnected(call: Call) {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Call connected - call.to: ${call.to}, call.from: ${call.from}")
            }

            // Preserve the phone number and contact info from previous state
            val currentState = _callState.value
            val (phoneNumber, contactName, contactAvatar) = when (currentState) {
                is CallState.Dialing -> Triple(currentState.phoneNumber, currentState.contactName, currentState.contactAvatar)
                is CallState.Ringing -> Triple(currentState.phoneNumber, currentState.contactName, currentState.contactAvatar)
                else -> Triple(call.from ?: "Unknown", null, null) // For incoming calls
            }

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Call connected - using phoneNumber: $phoneNumber, contactName: $contactName")
            }

            _callState.value = CallState.Active(
                phoneNumber = phoneNumber,
                callSid = call.sid,
                duration = 0,
                isMuted = call.isMuted ?: false,
                isSpeakerOn = false,
                contactName = contactName,
                contactAvatar = contactAvatar
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
     * IMPORTANT: Must capture stored values immediately before they're cleared by cleanup()
     */
    private fun recordCallLog(call: Call, status: CallStatus) {
        // CRITICAL: Capture values NOW before cleanup() clears them
        val capturedPhoneNumber = currentCallPhoneNumber
        val capturedCallType = currentCallType
        val capturedContactId = currentContactId
        val capturedDuration = if (callStartTime > 0) {
            ((System.currentTimeMillis() - callStartTime) / 1000).toInt()
        } else 0

        scope.launch {
            try {
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "=== RECORD CALL LOG START ===")
                    Log.d(TAG, "Call object - to: ${call.to}, from: ${call.from}")
                    Log.d(TAG, "Captured values - phone: $capturedPhoneNumber, type: $capturedCallType, contactId: $capturedContactId")
                }

                // Use captured values (not current variables which may be cleared)
                val phoneNumber = if (capturedPhoneNumber != null) {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "✓ Using captured phone: $capturedPhoneNumber")
                    }
                    capturedPhoneNumber
                } else {
                    if (BuildConfig.DEBUG) {
                        Log.e(TAG, "✗ WARNING: No captured phone! Using call.from: ${call.from}")
                    }
                    call.from ?: return@launch
                }

                val callType = capturedCallType ?: CallType.INCOMING

                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Recording: phone=$phoneNumber, type=$callType, duration=$capturedDuration, contactId=$capturedContactId")
                }

                callsRepository.recordCallLog(
                    contactId = capturedContactId,
                    phoneNumber = phoneNumber,
                    callType = callType,
                    duration = capturedDuration,
                    twilioCallSid = call.sid,
                    status = status
                )

                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "=== CALL LOG RECORDED ===")
                }
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) {
                    Log.e(TAG, "=== RECORD FAILED ===", e)
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
        currentCallPhoneNumber = null
        currentCallType = null
        currentContactId = null
        currentContactName = null
        currentContactAvatar = null
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
