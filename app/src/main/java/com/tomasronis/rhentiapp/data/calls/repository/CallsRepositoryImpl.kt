package com.tomasronis.rhentiapp.data.calls.repository

import android.util.Log
import com.tomasronis.rhentiapp.BuildConfig
import com.tomasronis.rhentiapp.core.database.dao.CallLogDao
import com.tomasronis.rhentiapp.core.database.dao.ContactDao
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
    private val contactDao: ContactDao
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

    override suspend fun getTwilioAccessToken(identity: String): NetworkResult<String> {
        return try {
            val request = mapOf("identity" to identity)

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Get Twilio access token for: $identity")
            }

            val response = apiClient.getTwilioAccessToken(request)

            // Extract token from response
            val token = (response["token"] ?: response["accessToken"]) as? String
                ?: throw Exception("Token not found in response")

            if (BuildConfig.DEBUG) {
                Log.d(TAG, "Twilio access token retrieved")
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
        return callLogDao.getAllCallLogs()
            .combine(contactDao.getAllContacts()) { callLogs, contacts ->
                // Create a contact lookup map
                val contactMap = contacts.associateBy { it.id }

                // Enrich call logs with contact data
                callLogs.map { cachedLog ->
                    val contact = cachedLog.contactId?.let { contactMap[it] }
                    CallLog(
                        id = cachedLog.id,
                        contactId = cachedLog.contactId,
                        contactName = contact?.let {
                            "${it.firstName ?: ""} ${it.lastName ?: ""}".trim()
                        } ?: cachedLog.callerName,
                        contactPhone = cachedLog.callerNumber ?: "",
                        contactAvatar = contact?.avatarUrl,
                        callType = parseCallType(cachedLog.callType),
                        duration = cachedLog.callDuration,
                        timestamp = cachedLog.startTime,
                        twilioCallSid = cachedLog.callSid,
                        status = parseCallStatus(cachedLog.callStatus)
                    )
                }
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
            val id = (response["id"] ?: response["_id"]) as? String ?: return null
            val contactId = (response["contactId"] ?: response["contact_id"]) as? String
            val phoneNumber = (response["phoneNumber"] ?: response["phone_number"]) as? String ?: ""
            val callType = (response["callType"] ?: response["call_type"]) as? String
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

            CallLog(
                id = id,
                contactId = contactId,
                contactName = null, // Will be enriched from contacts
                contactPhone = phoneNumber,
                contactAvatar = null, // Will be enriched from contacts
                callType = parseCallType(callType),
                duration = duration,
                timestamp = timestamp,
                twilioCallSid = twilioCallSid,
                status = parseCallStatus(status)
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
                callerNumber = log.contactPhone,
                callerName = log.contactName,
                contactId = log.contactId,
                createdAt = currentTime,
                updatedAt = currentTime
            )
        }
        callLogDao.insertCallLogs(cachedLogs)
    }

    private fun convertCachedCallLog(cachedLog: CachedCallLog): CallLog {
        return CallLog(
            id = cachedLog.id,
            contactId = cachedLog.contactId,
            contactName = cachedLog.callerName,
            contactPhone = cachedLog.callerNumber ?: "",
            contactAvatar = null,
            callType = parseCallType(cachedLog.callType),
            duration = cachedLog.callDuration,
            timestamp = cachedLog.startTime,
            twilioCallSid = cachedLog.callSid,
            status = parseCallStatus(cachedLog.callStatus)
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
