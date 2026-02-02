package com.tomasronis.rhentiapp.core.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "rhenti_preferences")

/**
 * Manages app preferences using DataStore.
 * Used for persisting user settings like selected tab.
 */
@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.dataStore

    companion object {
        private val SELECTED_TAB_KEY = intPreferencesKey("selected_tab")
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
}
