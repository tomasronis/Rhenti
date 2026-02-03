package com.tomasronis.rhentiapp.core.security

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.tomasronis.rhentiapp.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.crypto.AEADBadTagException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wrapper around EncryptedSharedPreferences for secure storage.
 *
 * Uses AES256-GCM encryption for values and deterministic AES256-SIV for keys.
 */
@Singleton
class EncryptedPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    private val sharedPreferences: SharedPreferences by lazy {
        try {
            EncryptedSharedPreferences.create(
                context,
                PREFS_FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            // Handle corrupted encrypted preferences (AEADBadTagException)
            // This can happen after app reinstall or keystore changes
            if (BuildConfig.DEBUG) {
                Log.e("EncryptedPreferences", "Encrypted preferences corrupted, recreating", e)
            }

            // Delete the corrupted file
            val prefsFile = context.getSharedPreferences(PREFS_FILE_NAME, Context.MODE_PRIVATE)
            prefsFile.edit().clear().apply()
            context.deleteSharedPreferences(PREFS_FILE_NAME)

            // Recreate with clean slate
            EncryptedSharedPreferences.create(
                context,
                PREFS_FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
    }

    /**
     * Saves a string value securely.
     */
    fun putString(key: String, value: String?) {
        if (value == null) {
            sharedPreferences.edit().remove(key).apply()
        } else {
            sharedPreferences.edit().putString(key, value).apply()
        }
    }

    /**
     * Retrieves a securely stored string value.
     */
    fun getString(key: String, defaultValue: String? = null): String? {
        return sharedPreferences.getString(key, defaultValue)
    }

    /**
     * Saves a boolean value securely.
     */
    fun putBoolean(key: String, value: Boolean) {
        sharedPreferences.edit().putBoolean(key, value).apply()
    }

    /**
     * Retrieves a securely stored boolean value.
     */
    fun getBoolean(key: String, defaultValue: Boolean = false): Boolean {
        return sharedPreferences.getBoolean(key, defaultValue)
    }

    /**
     * Removes a value from secure storage.
     */
    fun remove(key: String) {
        sharedPreferences.edit().remove(key).apply()
    }

    /**
     * Clears all securely stored values.
     */
    fun clear() {
        sharedPreferences.edit().clear().apply()
    }

    /**
     * Checks if a key exists in secure storage.
     */
    fun contains(key: String): Boolean {
        return sharedPreferences.contains(key)
    }

    companion object {
        private const val PREFS_FILE_NAME = "rhenti_secure_prefs"
    }
}
