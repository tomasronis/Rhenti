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
import com.google.firebase.messaging.FirebaseMessaging
import com.twilio.voice.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
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
    private val chatHubRepository: ChatHubRepository,
    private val preferencesManager: com.tomasronis.rhentiapp.core.preferences.PreferencesManager
) {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val audioManager = VoipAudioManager(context)

    private var accessToken: String? = null
    private var clientIdentity: String? = null  // Stored user ID for Twilio calls
    private var activeCall: Call? = null
    private var callInvite: CallInvite? = null

    private val _callState = MutableStateFlow<CallState>(CallState.Idle)
    val callState: StateFlow<CallState> = _callState.asStateFlow()

    // Debug status for diagnosing incoming call registration issues
    private val _registrationStatus = MutableStateFlow("Not initialized")
    val registrationStatus: StateFlow<String> = _registrationStatus.asStateFlow()

    // Detailed debug info for diagnosing push credential issues
    private val _debugInfo = MutableStateFlow("")
    val debugInfo: StateFlow<String> = _debugInfo.asStateFlow()

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

        /**
         * Decode a JWT token payload (without verification) to inspect grants.
         * Twilio access tokens are JWTs with grants in the payload.
         */
        fun decodeJwtPayload(jwt: String): String? {
            return try {
                val parts = jwt.split(".")
                if (parts.size != 3) return null
                val payload = parts[1]
                // Add padding if needed
                val padded = when (payload.length % 4) {
                    2 -> "$payload=="
                    3 -> "$payload="
                    else -> payload
                }
                val decoded = android.util.Base64.decode(padded, android.util.Base64.URL_SAFE or android.util.Base64.NO_WRAP)
                String(decoded, Charsets.UTF_8)
            } catch (e: Exception) {
                null
            }
        }

        /**
         * Simple JSON string value extractor - finds "key":"value" patterns.
         */
        fun extractJsonValue(json: String?, key: String): String? {
            if (json == null) return null
            return try {
                val pattern = """"$key"\s*:\s*"([^"]+)"""".toRegex()
                pattern.find(json)?.groupValues?.get(1)
            } catch (e: Exception) {
                null
            }
        }

        /**
         * Simple JSON number value extractor - finds "key":123 patterns.
         */
        fun extractJsonNumber(json: String?, key: String): Long? {
            if (json == null) return null
            return try {
                val pattern = """"$key"\s*:\s*(\d+)""".toRegex()
                pattern.find(json)?.groupValues?.get(1)?.toLongOrNull()
            } catch (e: Exception) {
                null
            }
        }
    }

    /**
     * Initialize Twilio SDK with access token
     */
    suspend fun initialize() {
        try {
            _registrationStatus.value = "Initializing..."
            val userId = tokenManager.getUserId() ?: run {
                _registrationStatus.value = "ERROR: No user ID"
                return
            }
            // Match old app: account = super_account_id, childAccount = userId
            val superAccountId = tokenManager.getSuperAccountId() ?: run {
                _registrationStatus.value = "ERROR: No super account ID"
                return
            }

            // Use device ID as identity (matches old app's DeviceInfo.getUniqueId())
            val deviceId = android.provider.Settings.Secure.getString(
                context.contentResolver,
                android.provider.Settings.Secure.ANDROID_ID
            )

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Initializing Twilio SDK - identity(deviceId): $deviceId, account(superAccountId): $superAccountId, childAccount(userId): $userId")
            }

            _registrationStatus.value = "Fetching access token..."

            // Get Twilio access token from API - match old app's parameters exactly
            when (val result = callsRepository.getTwilioAccessToken(
                identity = deviceId,
                os = "android",
                email = "", // old app doesn't send email
                account = superAccountId,
                childAccount = userId
            )) {
                is NetworkResult.Success -> {
                    accessToken = result.data
                    clientIdentity = deviceId
                    Voice.setLogLevel(if (BuildConfig.DEBUG) LogLevel.DEBUG else LogLevel.ERROR)

                    // Decode JWT to inspect grants and check for push_credential_sid
                    val jwtPayload = decodeJwtPayload(result.data)
                    val hasPushCred = jwtPayload?.contains("push_credential_sid") == true ||
                                     jwtPayload?.contains("pushCredentialSid") == true
                    val hasVoiceGrant = jwtPayload?.contains("voice") == true

                    // Extract key JWT claims
                    val pushCredSid = extractJsonValue(jwtPayload, "push_credential_sid")
                        ?: extractJsonValue(jwtPayload, "pushCredentialSid")
                    val expClaim = extractJsonNumber(jwtPayload, "exp")
                    val iatClaim = extractJsonNumber(jwtPayload, "iat")
                    val issClaim = extractJsonValue(jwtPayload, "iss")
                    val subClaim = extractJsonValue(jwtPayload, "sub")

                    val nowSec = System.currentTimeMillis() / 1000
                    val isExpired = expClaim != null && expClaim < nowSec
                    val ttlMin = if (expClaim != null && iatClaim != null) (expClaim - iatClaim) / 60 else null
                    val expiresIn = if (expClaim != null) (expClaim - nowSec) / 60 else null

                    val debugLines = StringBuilder()
                    debugLines.appendLine("Identity: $deviceId")
                    debugLines.appendLine("Voice Grant: ${if (hasVoiceGrant) "YES" else "NO"}")
                    debugLines.appendLine("Push Cred: ${pushCredSid ?: if (hasPushCred) "YES" else "MISSING!"}")
                    debugLines.appendLine("Issuer (API Key): ${issClaim ?: "?"}")
                    debugLines.appendLine("Account: ${subClaim ?: "?"}")
                    if (isExpired) {
                        debugLines.appendLine("TOKEN EXPIRED! (${-(expiresIn ?: 0)}min ago)")
                    } else if (expiresIn != null) {
                        debugLines.appendLine("Expires in: ${expiresIn}min (TTL: ${ttlMin}min)")
                    }

                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "========================================")
                        Log.d(TAG, "Twilio Access Token Analysis:")
                        Log.d(TAG, "  Identity: $deviceId")
                        Log.d(TAG, "  Voice Grant: $hasVoiceGrant")
                        Log.d(TAG, "  Push Cred SID: $pushCredSid")
                        Log.d(TAG, "  Issuer (API Key SID): $issClaim")
                        Log.d(TAG, "  Subject (Account SID): $subClaim")
                        Log.d(TAG, "  Expired: $isExpired, Expires in: ${expiresIn}min")
                        Log.d(TAG, "  Token length: ${result.data.length}")
                        Log.d(TAG, "  JWT Payload: $jwtPayload")
                        Log.d(TAG, "========================================")
                    }

                    if (!hasPushCred) {
                        debugLines.appendLine("WARNING: No push_credential_sid!")
                        Log.w(TAG, "ACCESS TOKEN MISSING PUSH CREDENTIAL SID")
                    }

                    _debugInfo.value = debugLines.toString().trim()
                    _registrationStatus.value = when {
                        isExpired -> "Token EXPIRED!"
                        hasPushCred -> "Token OK. Registering..."
                        else -> "Token OK but NO push credential!"
                    }

                    // Register for incoming call invites
                    registerForIncomingCalls(result.data)

                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "Twilio SDK initialized successfully")
                    }
                }
                is NetworkResult.Error -> {
                    _registrationStatus.value = "ERROR: Token fetch failed - ${result.exception?.message}"
                    if (BuildConfig.DEBUG) {
                        Log.e(TAG, "Failed to get Twilio access token", result.exception)
                    }
                }
                is NetworkResult.Loading -> {
                    // Still loading, do nothing
                }
            }
        } catch (e: Exception) {
            _registrationStatus.value = "ERROR: Init exception - ${e.message}"
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Failed to initialize Twilio SDK", e)
            }
        }
    }

    /**
     * Register for incoming call invites.
     * This enables the app to receive incoming VoIP calls.
     */
    private fun registerForIncomingCalls(accessToken: String) {
        scope.launch {
            try {
                // Get FCM token from preferences, or fetch directly from Firebase if not yet saved
                var fcmToken = preferencesManager.getFcmToken()
                if (fcmToken.isNullOrBlank()) {
                    if (BuildConfig.DEBUG) {
                        Log.d(TAG, "FCM token not in preferences, fetching directly from Firebase...")
                    }
                    try {
                        fcmToken = FirebaseMessaging.getInstance().token.await()
                        if (!fcmToken.isNullOrBlank()) {
                            preferencesManager.saveFcmToken(fcmToken)
                            if (BuildConfig.DEBUG) {
                                Log.d(TAG, "FCM token fetched and saved: ${fcmToken.take(20)}...")
                            }
                        }
                    } catch (e: Exception) {
                        if (BuildConfig.DEBUG) {
                            Log.e(TAG, "Failed to fetch FCM token from Firebase", e)
                        }
                    }
                }

                if (fcmToken.isNullOrBlank()) {
                    _registrationStatus.value = "ERROR: FCM token unavailable"
                    if (BuildConfig.DEBUG) {
                        Log.w(TAG, "Cannot register for incoming calls: FCM token not available")
                    }
                    return@launch
                }

                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Registering for incoming call invites with FCM token: ${fcmToken.take(20)}...")
                }

                _registrationStatus.value = "Calling Voice.register()..."

                Voice.register(
                    accessToken,
                    Voice.RegistrationChannel.FCM,
                    fcmToken,
                    object : RegistrationListener {
                        override fun onRegistered(accessToken: String, fcmToken: String) {
                            _registrationStatus.value = "REGISTERED - Ready for incoming calls"
                            // Append FCM token info to debug
                            val currentDebug = _debugInfo.value
                            _debugInfo.value = "$currentDebug\nFCM Token: ${fcmToken.take(20)}...\nRegistration: SUCCESS"
                            if (BuildConfig.DEBUG) {
                                Log.d(TAG, "✅ Registered for incoming calls successfully")
                                Log.d(TAG, "  FCM Token used: ${fcmToken.take(30)}...")
                            }
                        }

                        override fun onError(
                            registrationException: RegistrationException,
                            accessToken: String,
                            fcmToken: String
                        ) {
                            _registrationStatus.value = "REGISTER FAILED: ${registrationException.errorCode} - ${registrationException.message}"
                            val currentDebug = _debugInfo.value
                            _debugInfo.value = "$currentDebug\nRegistration: FAILED (${registrationException.errorCode})"
                            if (BuildConfig.DEBUG) {
                                Log.e(TAG, "❌ Failed to register for incoming calls: ${registrationException.message} (code: ${registrationException.errorCode})", registrationException)
                            }
                        }
                    }
                )
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) {
                    Log.e(TAG, "❌ Exception while registering for incoming calls", e)
                }
            }
        }
    }

    /**
     * Unregister from incoming call invites.
     * Called when user logs out or app is being destroyed.
     */
    fun unregisterForIncomingCalls() {
        scope.launch {
            try {
                val token = accessToken ?: return@launch
                val fcmToken = preferencesManager.getFcmToken()
                if (fcmToken.isNullOrBlank()) return@launch

                if (BuildConfig.DEBUG) {
                    Log.d(TAG, "Unregistering from incoming call invites")
                }

                Voice.unregister(
                    token,
                    Voice.RegistrationChannel.FCM,
                    fcmToken,
                    object : UnregistrationListener {
                        override fun onUnregistered(accessToken: String, fcmToken: String) {
                            if (BuildConfig.DEBUG) {
                                Log.d(TAG, "✅ Unregistered from incoming calls successfully")
                            }
                        }

                        override fun onError(
                            registrationException: RegistrationException,
                            accessToken: String,
                            fcmToken: String
                        ) {
                            if (BuildConfig.DEBUG) {
                                Log.e(TAG, "❌ Failed to unregister from incoming calls: ${registrationException.message}", registrationException)
                            }
                        }
                    }
                )
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) {
                    Log.e(TAG, "❌ Exception while unregistering from incoming calls", e)
                }
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
     * Get the currently stored CallInvite (for accepting from UI).
     */
    fun getCallInvite(): CallInvite? = callInvite

    /**
     * Handle incoming call invite - sets state to Ringing so the UI shows.
     * Called from MainActivity when a notification is tapped.
     */
    fun handleIncomingCallInvite(invite: CallInvite) {
        callInvite = invite
        currentCallPhoneNumber = invite.from
        currentCallType = CallType.INCOMING

        val callerNumber = invite.from ?: "Unknown"
        _callState.value = CallState.Ringing(
            phoneNumber = callerNumber,
            callSid = invite.callSid
        )

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Set Ringing state for incoming call from: $callerNumber")
        }

        // Look up contact info in background
        scope.launch {
            val contactInfo = lookupContactInfoByPhone(callerNumber)
            if (contactInfo != null) {
                currentContactId = contactInfo.id
                currentContactName = contactInfo.displayName
                currentContactAvatar = contactInfo.avatarUrl
                val current = _callState.value
                if (current is CallState.Ringing) {
                    _callState.value = current.copy(
                        contactName = contactInfo.displayName,
                        contactAvatar = contactInfo.avatarUrl
                    )
                }
            }
        }
    }

    /**
     * Accept incoming call
     */
    fun acceptIncomingCall(invite: CallInvite) {
        try {
            // Check microphone permission before accepting
            if (androidx.core.content.ContextCompat.checkSelfPermission(
                    context, android.Manifest.permission.RECORD_AUDIO
                ) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                if (BuildConfig.DEBUG) {
                    Log.e(TAG, "RECORD_AUDIO permission not granted - cannot accept call")
                }
                _callState.value = CallState.Failed(
                    "Microphone permission required. Please grant permission and try again."
                )
                return
            }

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Accepting incoming call from: ${invite.from}")
            }

            // Store for call logging
            currentCallPhoneNumber = invite.from
            currentCallType = CallType.INCOMING
            callInvite = invite

            val callerNumber = invite.from ?: "Unknown"

            // Set state immediately so ActiveCallScreen shows
            _callState.value = CallState.Ringing(
                phoneNumber = callerNumber,
                callSid = invite.callSid,
                contactName = currentContactName,
                contactAvatar = currentContactAvatar
            )

            // Look up contact info for the caller in background
            scope.launch {
                val contactInfo = lookupContactInfoByPhone(callerNumber)
                if (contactInfo != null) {
                    currentContactId = contactInfo.id
                    currentContactName = contactInfo.displayName
                    currentContactAvatar = contactInfo.avatarUrl

                    val currentState = _callState.value
                    when (currentState) {
                        is CallState.Ringing -> _callState.value = currentState.copy(
                            contactName = contactInfo.displayName,
                            contactAvatar = contactInfo.avatarUrl
                        )
                        is CallState.Active -> _callState.value = currentState.copy(
                            contactName = contactInfo.displayName,
                            contactAvatar = contactInfo.avatarUrl
                        )
                        else -> {}
                    }
                }
            }

            val acceptOptions = AcceptOptions.Builder()
                .build()

            activeCall = invite.accept(context, acceptOptions, callListener)

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
     * Force re-initialization. Useful for debugging.
     */
    suspend fun reinitialize() {
        _registrationStatus.value = "Re-initializing..."
        _debugInfo.value = ""
        accessToken = null
        clientIdentity = null
        initialize()
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
