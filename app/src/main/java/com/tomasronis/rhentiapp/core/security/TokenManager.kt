package com.tomasronis.rhentiapp.core.security

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages authentication tokens and user credentials.
 *
 * Provides secure storage and retrieval of JWT tokens, user IDs, and other
 * authentication-related data using EncryptedSharedPreferences.
 */
@Singleton
class TokenManager @Inject constructor(
    private val encryptedPreferences: EncryptedPreferences
) {

    /**
     * Saves the authentication token.
     */
    suspend fun saveAuthToken(token: String) {
        encryptedPreferences.putString(KEY_AUTH_TOKEN, token)
    }

    /**
     * Retrieves the authentication token.
     */
    suspend fun getAuthToken(): String? {
        return encryptedPreferences.getString(KEY_AUTH_TOKEN)
    }

    /**
     * Saves the white label identifier.
     */
    suspend fun saveWhiteLabel(whiteLabel: String) {
        encryptedPreferences.putString(KEY_WHITE_LABEL, whiteLabel)
    }

    /**
     * Retrieves the white label identifier.
     */
    suspend fun getWhiteLabel(): String? {
        return encryptedPreferences.getString(KEY_WHITE_LABEL)
    }

    /**
     * Saves the user ID.
     */
    suspend fun saveUserId(userId: String) {
        encryptedPreferences.putString(KEY_USER_ID, userId)
    }

    /**
     * Retrieves the user ID.
     */
    suspend fun getUserId(): String? {
        return encryptedPreferences.getString(KEY_USER_ID)
    }

    /**
     * Saves the super account ID.
     */
    suspend fun saveSuperAccountId(superAccountId: String) {
        encryptedPreferences.putString(KEY_SUPER_ACCOUNT_ID, superAccountId)
    }

    /**
     * Retrieves the super account ID.
     */
    suspend fun getSuperAccountId(): String? {
        return encryptedPreferences.getString(KEY_SUPER_ACCOUNT_ID)
    }

    /**
     * Saves the user's first name.
     */
    suspend fun saveUserFirstName(firstName: String) {
        encryptedPreferences.putString(KEY_USER_FIRST_NAME, firstName)
    }

    /**
     * Retrieves the user's first name.
     */
    suspend fun getUserFirstName(): String? {
        return encryptedPreferences.getString(KEY_USER_FIRST_NAME)
    }

    /**
     * Saves the user's last name.
     */
    suspend fun saveUserLastName(lastName: String) {
        encryptedPreferences.putString(KEY_USER_LAST_NAME, lastName)
    }

    /**
     * Retrieves the user's last name.
     */
    suspend fun getUserLastName(): String? {
        return encryptedPreferences.getString(KEY_USER_LAST_NAME)
    }

    /**
     * Saves the user's email.
     */
    suspend fun saveUserEmail(email: String) {
        encryptedPreferences.putString(KEY_USER_EMAIL, email)
    }

    /**
     * Retrieves the user's email.
     */
    suspend fun getUserEmail(): String? {
        return encryptedPreferences.getString(KEY_USER_EMAIL)
    }

    /**
     * Saves the account ID (for Twilio).
     */
    suspend fun saveAccount(account: String) {
        encryptedPreferences.putString(KEY_ACCOUNT, account)
    }

    /**
     * Retrieves the account ID (for Twilio).
     */
    suspend fun getAccount(): String? {
        return encryptedPreferences.getString(KEY_ACCOUNT)
    }

    /**
     * Saves the child account ID (for Twilio).
     */
    suspend fun saveChildAccount(childAccount: String) {
        encryptedPreferences.putString(KEY_CHILD_ACCOUNT, childAccount)
    }

    /**
     * Retrieves the child account ID (for Twilio).
     */
    suspend fun getChildAccount(): String? {
        return encryptedPreferences.getString(KEY_CHILD_ACCOUNT)
    }

    /**
     * Retrieves the user's full name.
     */
    suspend fun getUserFullName(): String {
        val firstName = getUserFirstName() ?: ""
        val lastName = getUserLastName() ?: ""
        return "$firstName $lastName".trim().ifEmpty { "Unknown User" }
    }

    /**
     * Saves all authentication data at once.
     */
    suspend fun saveAuthData(
        token: String,
        userId: String,
        whiteLabel: String,
        superAccountId: String,
        firstName: String? = null,
        lastName: String? = null,
        email: String? = null,
        account: String? = null,
        childAccount: String? = null
    ) {
        saveAuthToken(token)
        saveUserId(userId)
        saveWhiteLabel(whiteLabel)
        saveSuperAccountId(superAccountId)
        firstName?.let { saveUserFirstName(it) }
        lastName?.let { saveUserLastName(it) }
        email?.let { saveUserEmail(it) }
        // Use account/childAccount from API if provided, otherwise fallback to superAccountId/userId
        saveAccount(account ?: superAccountId)
        saveChildAccount(childAccount ?: userId)
    }

    /**
     * Checks if the user is authenticated (has a valid token).
     */
    suspend fun isAuthenticated(): Boolean {
        return !getAuthToken().isNullOrEmpty()
    }

    /**
     * Clears all authentication data.
     */
    suspend fun clearAuthData() {
        encryptedPreferences.remove(KEY_AUTH_TOKEN)
        encryptedPreferences.remove(KEY_USER_ID)
        encryptedPreferences.remove(KEY_WHITE_LABEL)
        encryptedPreferences.remove(KEY_SUPER_ACCOUNT_ID)
        encryptedPreferences.remove(KEY_USER_FIRST_NAME)
        encryptedPreferences.remove(KEY_USER_LAST_NAME)
        encryptedPreferences.remove(KEY_USER_EMAIL)
        encryptedPreferences.remove(KEY_ACCOUNT)
        encryptedPreferences.remove(KEY_CHILD_ACCOUNT)
    }

    companion object {
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_WHITE_LABEL = "white_label"
        private const val KEY_SUPER_ACCOUNT_ID = "super_account_id"
        private const val KEY_USER_FIRST_NAME = "user_first_name"
        private const val KEY_USER_LAST_NAME = "user_last_name"
        private const val KEY_USER_EMAIL = "user_email"
        private const val KEY_ACCOUNT = "account"
        private const val KEY_CHILD_ACCOUNT = "child_account"
    }
}
