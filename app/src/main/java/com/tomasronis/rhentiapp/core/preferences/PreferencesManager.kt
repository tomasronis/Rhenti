package com.tomasronis.rhentiapp.core.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "rhenti_preferences")

/**
 * Theme mode preference options.
 */
enum class ThemeMode {
    DARK,    // Always use dark theme
    LIGHT,   // Always use light theme
    SYSTEM   // Follow system settings
}

/**
 * Media retention period options.
 */
enum class MediaRetentionPeriod(val days: Int) {
    ONE_WEEK(7),
    ONE_MONTH(30),
    THREE_MONTHS(90),
    SIX_MONTHS(180),
    ONE_YEAR(365),
    FOREVER(-1)  // Never delete
}

/**
 * Manages app preferences using DataStore.
 * Used for persisting user settings like selected tab, theme mode, and storage settings.
 */
@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        private val SELECTED_TAB_KEY = intPreferencesKey("selected_tab")
        private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        private val MEDIA_RETENTION_KEY = stringPreferencesKey("media_retention")
        private val MESSAGES_PER_CHAT_KEY = intPreferencesKey("messages_per_chat")
        private val LAST_MEDIA_CLEANUP_KEY = longPreferencesKey("last_media_cleanup")
    }

    /**
     * Flow that emits the currently selected tab index.
     */
    val selectedTab: Flow<Int> = dataStore.data.map { preferences ->
        preferences[SELECTED_TAB_KEY] ?: 0 // Default to first tab (Chats)
    }

    /**
     * Save the selected tab index.
     */
    suspend fun setSelectedTab(tabIndex: Int) {
        dataStore.edit { preferences ->
            preferences[SELECTED_TAB_KEY] = tabIndex
        }
    }

    /**
     * Flow that emits the currently selected theme mode.
     */
    val themeMode: Flow<ThemeMode> = dataStore.data.map { preferences ->
        val modeString = preferences[THEME_MODE_KEY] ?: "DARK"
        try {
            ThemeMode.valueOf(modeString)
        } catch (e: IllegalArgumentException) {
            ThemeMode.DARK // Default to dark theme
        }
    }

    /**
     * Save the selected theme mode.
     */
    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = mode.name
        }
    }

    /**
     * Flow that emits the media retention period.
     */
    val mediaRetentionPeriod: Flow<MediaRetentionPeriod> = dataStore.data.map { preferences ->
        val periodString = preferences[MEDIA_RETENTION_KEY] ?: "FOREVER"
        try {
            MediaRetentionPeriod.valueOf(periodString)
        } catch (e: IllegalArgumentException) {
            MediaRetentionPeriod.FOREVER
        }
    }

    /**
     * Save the media retention period.
     */
    suspend fun setMediaRetentionPeriod(period: MediaRetentionPeriod) {
        dataStore.edit { preferences ->
            preferences[MEDIA_RETENTION_KEY] = period.name
        }
    }

    /**
     * Flow that emits the messages per chat limit.
     */
    val messagesPerChat: Flow<Int> = dataStore.data.map { preferences ->
        preferences[MESSAGES_PER_CHAT_KEY] ?: -1 // -1 means unlimited
    }

    /**
     * Save the messages per chat limit.
     */
    suspend fun setMessagesPerChat(limit: Int) {
        dataStore.edit { preferences ->
            preferences[MESSAGES_PER_CHAT_KEY] = limit
        }
    }

    /**
     * Get the last media cleanup timestamp.
     */
    val lastMediaCleanup: Flow<Long> = dataStore.data.map { preferences ->
        preferences[LAST_MEDIA_CLEANUP_KEY] ?: 0L
    }

    /**
     * Save the last media cleanup timestamp.
     */
    suspend fun setLastMediaCleanup(timestamp: Long) {
        dataStore.edit { preferences ->
            preferences[LAST_MEDIA_CLEANUP_KEY] = timestamp
        }
    }
}
