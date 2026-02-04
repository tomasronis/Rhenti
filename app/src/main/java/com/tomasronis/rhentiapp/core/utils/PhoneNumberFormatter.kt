package com.tomasronis.rhentiapp.core.utils

/**
 * Utility for formatting phone numbers for Twilio calls.
 * Twilio requires E.164 format: +[country code][number]
 */
object PhoneNumberFormatter {

    /**
     * Format a phone number to E.164 format for Twilio.
     * Assumes North American numbers if no country code is present.
     */
    fun formatForTwilio(phoneNumber: String): String {
        // Remove all non-digit characters except +
        val cleaned = phoneNumber.replace(Regex("[^+\\d]"), "")

        // If already has + prefix, assume it's formatted correctly
        if (cleaned.startsWith("+")) {
            return cleaned
        }

        // If starts with 1 and has 11 digits total, add + prefix
        if (cleaned.startsWith("1") && cleaned.length == 11) {
            return "+$cleaned"
        }

        // If 10 digits, assume North American number and add +1
        if (cleaned.length == 10) {
            return "+1$cleaned"
        }

        // If less than 10 digits or more than 11, return as-is with + prefix
        // (might be short code or international number)
        return if (cleaned.isEmpty()) phoneNumber else "+$cleaned"
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
