package com.tomasronis.rhentiapp.data.calls.repository

import android.util.Log
import com.tomasronis.rhentiapp.BuildConfig
import com.tomasronis.rhentiapp.core.database.dao.CallLogDao
import com.tomasronis.rhentiapp.core.database.dao.ContactDao
import com.tomasronis.rhentiapp.core.database.dao.ThreadDao
import com.tomasronis.rhentiapp.core.database.entities.CachedCallLog
import com.tomasronis.rhentiapp.core.network.ApiClient
import com.tomasronis.rhentiapp.core.network.NetworkResult
import com.tomasronis.rhentiapp.data.calls.models.*
import kotlinx.coroutines.flow.*
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CallsRepositoryImpl @Inject constructor(
    private val apiClient: ApiClient,
    private val callLogDao: CallLogDao,
    private val contactDao: ContactDao,
    private val threadDao: ThreadDao
) : CallsRepository {

    companion object {
        private const val TAG = "CallsRepository"
    }

    override suspend fun getCallLogs(superAccountId: String): NetworkResult<List<CallLog>> {
        return try {
            val response = apiClient.getCallLogs(superAccountId)

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Get call logs response: $response")
            }

            // Extract the call logs array from the wrapper object
            // Try common keys: callLogs, call_logs, logs, data
            @Suppress("UNCHECKED_CAST")
            val callLogsList = (response["callLogs"]
                ?: response["call_logs"]
                ?: response["logs"]
                ?: response["data"]
                ?: emptyList<Map<String, Any>>()) as? List<Map<String, Any>> ?: emptyList()

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Extracted ${callLogsList.size} call logs")
            }

            val callLogs = callLogsList.mapNotNull { parseCallLogResponse(it) }

            // Cache in Room
            cacheCallLogs(callLogs)

            NetworkResult.Success(callLogs)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Failed to get call logs", e)
            }

            // Return cached data
            val cachedLogs = callLogDao.getAllCallLogs().first()
            val callLogs = cachedLogs.map { convertCachedCallLog(it) }

            NetworkResult.Error(
                exception = e,
                cachedData = callLogs
            )
        }
    }

    override suspend fun recordCallLog(
        contactId: String?,
        phoneNumber: String,
        callType: CallType,
        duration: Int,
        twilioCallSid: String?,
        status: CallStatus
    ): NetworkResult<Unit> {
        return try {
            val request: Map<String, Any> = mapOf(
                "contactId" to (contactId ?: ""),
                "phoneNumber" to phoneNumber,
                "callType" to callType.toApiString(),
                "duration" to duration,
                "twilioCallSid" to (twilioCallSid ?: ""),
                "status" to status.toApiString()
            )

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Record call log request: $request")
            }

            apiClient.recordCallLog(request)

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Call log recorded successfully")
            }

            NetworkResult.Success(Unit)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Failed to record call log", e)
            }

            NetworkResult.Error(
                exception = e,
                cachedData = null
            )
        }
    }

    override suspend fun getTwilioAccessToken(
        identity: String,
        os: String,
        email: String,
        account: String,
        childAccount: String
    ): NetworkResult<String> {
        return try {
            val request = mapOf(
                "identity" to identity,
                "os" to os,
                "email" to email,
                "account" to account,
                "childAccount" to childAccount
            )

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Get Twilio access token - identity: $identity, os: $os, email: $email, account: $account, childAccount: $childAccount")
            }

            val responseBody = apiClient.getTwilioAccessToken(request)

            // Response is the token itself (plain text, not JSON)
            val token = responseBody.string().trim()

            if (token.isBlank()) {
                throw Exception("Received empty token from server")
            }

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Twilio access token retrieved successfully (${token.length} chars)")
            }

            NetworkResult.Success(token)
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Failed to get Twilio access token", e)
            }

            NetworkResult.Error(
                exception = e,
                cachedData = null
            )
        }
    }

    override fun observeCallLogs(): Flow<List<CallLog>> {
        return combine(
            callLogDao.getAllCallLogs(),
            contactDao.getAllContacts(),
            threadDao.getAllThreads()
        ) { callLogs, contacts, threads ->
            // Create contact lookup maps by ID and by phone number
            val contactMap = contacts.associateBy { it.id }
            val phoneMap = contacts.filter { it.phone != null }
                .associateBy { normalizePhoneNumber(it.phone!!) }

            // Create thread lookup map by phone number (for fallback contact info)
            val threadPhoneMap = threads.filter { !it.phone.isNullOrBlank() }
                .associateBy { normalizePhoneNumber(it.phone!!) }

            // Enrich call logs with contact data (from contacts first, then threads as fallback)
            callLogs.map { cachedLog ->
                val parsedCallType = parseCallType(cachedLog.callType)

                // Determine the contact's phone number based on call direction:
                // - For OUTGOING calls: the contact is the receiver (the "to" number)
                // - For INCOMING/MISSED calls: the contact is the caller (the "from" number)
                val rawContactPhone = when (parsedCallType) {
                    CallType.OUTGOING -> cachedLog.receiverNumber ?: cachedLog.callerNumber
                    CallType.INCOMING, CallType.MISSED -> cachedLog.callerNumber ?: cachedLog.receiverNumber
                }

                // Look up contact by ID first, then by the correct phone number
                val contact = cachedLog.contactId?.let { contactMap[it] }
                    ?: rawContactPhone?.let { phoneMap[normalizePhoneNumber(it)] }

                // If no contact found, try looking up from threads
                val thread = if (contact == null) {
                    rawContactPhone?.let { threadPhoneMap[normalizePhoneNumber(it)] }
                } else null

                // Resolve name: contact name > thread name > cached caller name
                val resolvedName = contact?.let {
                    "${it.firstName ?: ""} ${it.lastName ?: ""}".trim()
                }?.takeIf { it.isNotBlank() }
                    ?: thread?.displayName?.takeIf { it.isNotBlank() }
                    ?: cachedLog.callerName

                // Resolve avatar: contact avatar > thread image
                val resolvedAvatar = contact?.avatarUrl ?: thread?.imageUrl

                CallLog(
                    id = cachedLog.id,
                    contactId = cachedLog.contactId ?: contact?.id,
                    contactName = resolvedName,
                    // Use contact's phone number if available, otherwise use the direction-resolved number
                    contactPhone = contact?.phone ?: rawContactPhone ?: "",
                    contactAvatar = resolvedAvatar,
                    callType = parsedCallType,
                    duration = cachedLog.callDuration,
                    timestamp = cachedLog.startTime,
                    twilioCallSid = cachedLog.callSid,
                    status = parseCallStatus(cachedLog.callStatus),
                    callerNumber = cachedLog.callerNumber,
                    receiverNumber = cachedLog.receiverNumber
                )
            }
        }
    }

    /**
     * Normalize phone number for matching (remove formatting, keep only digits and +)
     */
    private fun normalizePhoneNumber(phone: String): String {
        val normalized = phone.replace(Regex("[^0-9+]"), "")
        // If it has 10 digits and no country code, assume North America (+1)
        return if (normalized.length == 10 && !normalized.startsWith("+")) {
            "+1$normalized"
        } else {
            normalized
        }
    }

    override fun observeFilteredCallLogs(filter: CallFilter): Flow<List<CallLog>> {
        return observeCallLogs().map { callLogs ->
            var filtered = callLogs

            // Filter by type
            filter.type?.let { type ->
                filtered = filtered.filter { it.callType == type }
            }

            // Filter by date range
            if (filter.startDate != null && filter.endDate != null) {
                filtered = filtered.filter {
                    it.timestamp >= filter.startDate && it.timestamp <= filter.endDate
                }
            }

            // Filter by search query
            filter.searchQuery?.let { query ->
                if (query.isNotBlank()) {
                    filtered = filtered.filter {
                        it.contactName?.contains(query, ignoreCase = true) == true ||
                                it.contactPhone.contains(query, ignoreCase = true)
                    }
                }
            }

            filtered
        }
    }

    override suspend fun deleteCallLog(callId: String) {
        callLogDao.deleteCallLogById(callId)

        if (BuildConfig.DEBUG) {
            Log.d(TAG, "Call log deleted: $callId")
        }
    }

    override fun searchCallLogs(query: String): Flow<List<CallLog>> {
        return observeCallLogs().map { callLogs ->
            callLogs.filter {
                it.contactName?.contains(query, ignoreCase = true) == true ||
                        it.contactPhone.contains(query, ignoreCase = true)
            }
        }
    }

    private fun parseCallLogResponse(response: Map<String, Any>): CallLog? {
        return try {
            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Parsing call log response: $response")
            }

            val id = (response["id"] ?: response["_id"]) as? String ?: return null
            val contactId = (response["contactId"] ?: response["contact_id"]) as? String

            // Extract "from" number (the caller)
            val callerNumber = (response["callerNumber"]
                ?: response["caller_number"]
                ?: response["from"]
                ?: response["fromNumber"]
                ?: response["from_number"]) as? String

            // Extract "to" number (the recipient)
            val receiverNumber = (response["to"]
                ?: response["toNumber"]
                ?: response["to_number"]
                ?: response["receiverNumber"]
                ?: response["receiver_number"]
                ?: response["calledNumber"]
                ?: response["called_number"]) as? String

            // Generic phone number field (legacy fallback)
            val genericPhoneNumber = (response["phoneNumber"]
                ?: response["phone_number"]
                ?: response["number"]) as? String

            // Try multiple name field variations
            val contactName = (response["contactName"]
                ?: response["contact_name"]
                ?: response["callerName"]
                ?: response["caller_name"]
                ?: response["name"]) as? String

            val callType = (response["callType"] ?: response["call_type"]) as? String
            val parsedCallType = parseCallType(callType)
            val duration = (response["duration"] as? Number)?.toInt() ?: 0
            val twilioCallSid = (response["twilioCallSid"] ?: response["twilio_call_sid"]) as? String
            val status = response["status"] as? String

            // Parse timestamp
            val timestamp = when {
                response["timestamp"] is Number -> (response["timestamp"] as Number).toLong()
                response["created_at"] is String -> parseIsoDateToTimestamp(response["created_at"] as String)
                response["createdAt"] is String -> parseIsoDateToTimestamp(response["createdAt"] as String)
                else -> System.currentTimeMillis()
            }

            // Determine the contact's phone number based on call direction:
            // - For OUTGOING calls: the contact is the receiver (the "to" number)
            // - For INCOMING/MISSED calls: the contact is the caller (the "from" number)
            val contactPhone = when (parsedCallType) {
                CallType.OUTGOING -> receiverNumber ?: genericPhoneNumber ?: callerNumber ?: ""
                CallType.INCOMING, CallType.MISSED -> callerNumber ?: genericPhoneNumber ?: receiverNumber ?: ""
            }

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Parsed call log - id: $id, callerNumber: $callerNumber, receiverNumber: $receiverNumber, " +
                        "genericPhone: $genericPhoneNumber, resolvedContactPhone: $contactPhone, name: $contactName, type: $callType")
            }

            CallLog(
                id = id,
                contactId = contactId,
                contactName = contactName,
                contactPhone = contactPhone,
                contactAvatar = null, // Will be enriched from contacts
                callType = parsedCallType,
                duration = duration,
                timestamp = timestamp,
                twilioCallSid = twilioCallSid,
                status = parseCallStatus(status),
                callerNumber = callerNumber ?: genericPhoneNumber,
                receiverNumber = receiverNumber
            )
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Failed to parse call log response", e)
            }
            null
        }
    }

    private suspend fun cacheCallLogs(callLogs: List<CallLog>) {
        val currentTime = System.currentTimeMillis()
        val cachedLogs = callLogs.map { log ->
            CachedCallLog(
                id = log.id,
                callSid = log.twilioCallSid,
                callStatus = log.status.toApiString(),
                callType = log.callType.toApiString(),
                startTime = log.timestamp,
                callDuration = log.duration,
                callerNumber = log.callerNumber ?: log.contactPhone,
                callerName = log.contactName,
                receiverNumber = log.receiverNumber,
                contactId = log.contactId,
                createdAt = currentTime,
                updatedAt = currentTime
            )
        }
        callLogDao.insertCallLogs(cachedLogs)
    }

    private fun convertCachedCallLog(cachedLog: CachedCallLog): CallLog {
        val parsedCallType = parseCallType(cachedLog.callType)

        // Determine the contact's phone number based on call direction:
        // - For OUTGOING calls: the contact is the receiver (the "to" number)
        // - For INCOMING/MISSED calls: the contact is the caller (the "from" number)
        val contactPhone = when (parsedCallType) {
            CallType.OUTGOING -> cachedLog.receiverNumber ?: cachedLog.callerNumber ?: ""
            CallType.INCOMING, CallType.MISSED -> cachedLog.callerNumber ?: cachedLog.receiverNumber ?: ""
        }

        return CallLog(
            id = cachedLog.id,
            contactId = cachedLog.contactId,
            contactName = cachedLog.callerName,
            contactPhone = contactPhone,
            contactAvatar = null,
            callType = parsedCallType,
            duration = cachedLog.callDuration,
            timestamp = cachedLog.startTime,
            twilioCallSid = cachedLog.callSid,
            status = parseCallStatus(cachedLog.callStatus),
            callerNumber = cachedLog.callerNumber,
            receiverNumber = cachedLog.receiverNumber
        )
    }

    private fun parseIsoDateToTimestamp(isoDate: String?): Long {
        if (isoDate.isNullOrBlank()) return System.currentTimeMillis()

        return try {
            val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            format.timeZone = TimeZone.getTimeZone("UTC")
            format.parse(isoDate)?.time ?: System.currentTimeMillis()
        } catch (e: Exception) {
            if (BuildConfig.DEBUG) {
                Log.e(TAG, "Failed to parse ISO date: $isoDate", e)
            }
            System.currentTimeMillis()
        }
    }
}
