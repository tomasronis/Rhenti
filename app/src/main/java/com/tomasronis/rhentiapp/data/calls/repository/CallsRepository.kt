package com.tomasronis.rhentiapp.data.calls.repository

import com.tomasronis.rhentiapp.core.network.NetworkResult
import com.tomasronis.rhentiapp.data.calls.models.CallFilter
import com.tomasronis.rhentiapp.data.calls.models.CallLog
import com.tomasronis.rhentiapp.data.calls.models.CallStatus
import com.tomasronis.rhentiapp.data.calls.models.CallType
import kotlinx.coroutines.flow.Flow

/**
 * Repository for managing call logs
 */
interface CallsRepository {
    /**
     * Get all call logs for a super account
     */
    suspend fun getCallLogs(superAccountId: String): NetworkResult<List<CallLog>>

    /**
     * Record a new call log
     */
    suspend fun recordCallLog(
        contactId: String?,
        phoneNumber: String,
        callType: CallType,
        duration: Int,
        twilioCallSid: String?,
        status: CallStatus
    ): NetworkResult<Unit>

    /**
     * Get Twilio access token for VoIP calls
     */
    suspend fun getTwilioAccessToken(identity: String): NetworkResult<String>

    /**
     * Observe all call logs from database
     */
    fun observeCallLogs(): Flow<List<CallLog>>

    /**
     * Observe filtered call logs from database
     */
    fun observeFilteredCallLogs(filter: CallFilter): Flow<List<CallLog>>

    /**
     * Delete a call log by ID
     */
    suspend fun deleteCallLog(callId: String)

    /**
     * Search call logs by query
     */
    fun searchCallLogs(query: String): Flow<List<CallLog>>
}
