package com.tomasronis.rhentiapp.data.calls.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Domain model for a call log entry
 */
data class CallLog(
    val id: String,
    val contactId: String?,
    val contactName: String?,
    val contactPhone: String, // The contact's phone number (resolved based on call direction)
    val contactAvatar: String?,
    val callType: CallType,
    val duration: Int, // seconds
    val timestamp: Long, // milliseconds
    val twilioCallSid: String?,
    val status: CallStatus,
    val callerNumber: String? = null, // The "from" number (raw from API)
    val receiverNumber: String? = null // The "to" number (raw from API)
)

/**
 * Call type enum
 */
enum class CallType {
    INCOMING,
    OUTGOING,
    MISSED
}

/**
 * Call status enum
 */
enum class CallStatus {
    COMPLETED,
    FAILED,
    BUSY,
    NO_ANSWER
}

/**
 * API response model for call log
 */
@JsonClass(generateAdapter = true)
data class CallLogResponse(
    @Json(name = "id") val id: String?,
    @Json(name = "_id") val _id: String?,
    @Json(name = "contact_id") val contactId: String?,
    @Json(name = "contactId") val contactIdCamel: String?,
    @Json(name = "phone_number") val phoneNumber: String?,
    @Json(name = "phoneNumber") val phoneNumberCamel: String?,
    @Json(name = "call_type") val callType: String?,
    @Json(name = "callType") val callTypeCamel: String?,
    @Json(name = "duration") val duration: Int?,
    @Json(name = "timestamp") val timestamp: Long?,
    @Json(name = "created_at") val createdAt: String?,
    @Json(name = "createdAt") val createdAtCamel: String?,
    @Json(name = "twilio_call_sid") val twilioCallSid: String?,
    @Json(name = "twilioCallSid") val twilioCallSidCamel: String?,
    @Json(name = "status") val status: String?
)

/**
 * API request model for recording call log
 */
@JsonClass(generateAdapter = true)
data class RecordCallLogRequest(
    @Json(name = "contactId") val contactId: String?,
    @Json(name = "phoneNumber") val phoneNumber: String,
    @Json(name = "callType") val callType: String,
    @Json(name = "duration") val duration: Int,
    @Json(name = "twilioCallSid") val twilioCallSid: String?,
    @Json(name = "status") val status: String
)

/**
 * Filter for call logs
 */
data class CallFilter(
    val type: CallType? = null,
    val startDate: Long? = null,
    val endDate: Long? = null,
    val searchQuery: String? = null
)

/**
 * Parse call type from string
 */
fun parseCallType(value: String?): CallType {
    return when (value?.lowercase()) {
        "incoming", "inbound" -> CallType.INCOMING
        "outgoing", "outbound" -> CallType.OUTGOING
        "missed" -> CallType.MISSED
        else -> CallType.OUTGOING
    }
}

/**
 * Parse call status from string
 */
fun parseCallStatus(value: String?): CallStatus {
    return when (value?.lowercase()) {
        "completed", "answered" -> CallStatus.COMPLETED
        "failed", "error" -> CallStatus.FAILED
        "busy" -> CallStatus.BUSY
        "no_answer", "no-answer", "noanswer" -> CallStatus.NO_ANSWER
        else -> CallStatus.COMPLETED
    }
}

/**
 * Convert call type to API string
 */
fun CallType.toApiString(): String {
    return when (this) {
        CallType.INCOMING -> "incoming"
        CallType.OUTGOING -> "outgoing"
        CallType.MISSED -> "missed"
    }
}

/**
 * Convert call status to API string
 */
fun CallStatus.toApiString(): String {
    return when (this) {
        CallStatus.COMPLETED -> "completed"
        CallStatus.FAILED -> "failed"
        CallStatus.BUSY -> "busy"
        CallStatus.NO_ANSWER -> "no_answer"
    }
}
