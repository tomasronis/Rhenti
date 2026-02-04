package com.tomasronis.rhentiapp.core.utils

/**
 * Utility for formatting phone numbers for Twilio calls.
 * Twilio requires E.164 format: +[country code][number]
 */
object PhoneNumberFormatter {

    /**
     * Format a phone number to E.164 format for Twilio.
     * Assumes North American numbers if no country code is present.
     *
     * Examples:
     * - "5551234567" -> "+15551234567"
     * - "+15551234567" -> "+15551234567"
     * - "(555) 123-4567" -> "+15551234567"
     * - "15551234567" -> "+15551234567"
     */
    fun formatForTwilio(phoneNumber: String): String {
        // Remove all non-digit characters except +
        var cleaned = phoneNumber.replace(Regex("[^+\\d]"), "")

        // If already has + prefix, validate it
        if (cleaned.startsWith("+")) {
            // Remove the + for validation, we'll add it back
            cleaned = cleaned.substring(1)
        }

        // Now cleaned should only contain digits
        // Validate and format based on length
        val formatted = when {
            // 10 digits - North American number without country code
            cleaned.length == 10 -> {
                "+1$cleaned"
            }
            // 11 digits starting with 1 - North American number with country code
            cleaned.length == 11 && cleaned.startsWith("1") -> {
                "+$cleaned"
            }
            // International number (assume it's already correct if 11+ digits)
            cleaned.length >= 11 -> {
                "+$cleaned"
            }
            // Too short or empty - return original with validation note
            else -> {
                // Return the cleaned version with + if we have any digits
                if (cleaned.isNotEmpty()) "+$cleaned" else phoneNumber
            }
        }

        android.util.Log.d("PhoneNumberFormatter", "Input: '$phoneNumber' -> Formatted: '$formatted'")
        return formatted
    }

    /**
     * Format for display (adds dashes for readability).
     */
    fun formatForDisplay(phoneNumber: String): String {
        val cleaned = phoneNumber.replace(Regex("[^\\d]"), "")

        return when {
            cleaned.length == 10 -> {
                "(${cleaned.substring(0, 3)}) ${cleaned.substring(3, 6)}-${cleaned.substring(6)}"
            }
            cleaned.length == 11 && cleaned.startsWith("1") -> {
                "+1 (${cleaned.substring(1, 4)}) ${cleaned.substring(4, 7)}-${cleaned.substring(7)}"
            }
            else -> phoneNumber
        }
    }
}
