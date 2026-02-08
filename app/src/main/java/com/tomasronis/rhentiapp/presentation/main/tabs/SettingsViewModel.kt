package com.tomasronis.rhentiapp.presentation.main.tabs

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tomasronis.rhentiapp.core.preferences.MediaRetentionPeriod
import com.tomasronis.rhentiapp.core.preferences.PreferencesManager
import com.tomasronis.rhentiapp.core.preferences.ThemeMode
import com.tomasronis.rhentiapp.core.security.TokenManager
import com.tomasronis.rhentiapp.data.chathub.repository.ChatHubRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject

/**
 * ViewModel for Settings screen.
 * Manages user data, theme selection, storage info, and sign out.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val tokenManager: TokenManager,
    private val preferencesManager: PreferencesManager,
    private val chatHubRepository: ChatHubRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _selectedThemeMode = MutableStateFlow(ThemeMode.DARK)
    val selectedThemeMode: StateFlow<ThemeMode> = _selectedThemeMode.asStateFlow()

    private val _mediaRetentionPeriod = MutableStateFlow(MediaRetentionPeriod.FOREVER)
    val mediaRetentionPeriod: StateFlow<MediaRetentionPeriod> = _mediaRetentionPeriod.asStateFlow()

    private val _messagesPerChat = MutableStateFlow(-1) // -1 means unlimited
    val messagesPerChat: StateFlow<Int> = _messagesPerChat.asStateFlow()

    init {
        loadUserData()
        loadThemePreference()
        loadStoragePreferences()
        calculateStorage()
        checkMediaCleanup()
    }

    /**
     * Load user data from TokenManager.
     */
    private fun loadUserData() {
        viewModelScope.launch {
            try {
                val firstName = tokenManager.getUserFirstName() ?: ""
                val lastName = tokenManager.getUserLastName() ?: ""
                val email = tokenManager.getUserEmail() ?: ""
                val whiteLabel = tokenManager.getWhiteLabel() ?: ""
                val phone = "" // Phone not stored in TokenManager currently

                val name = "$firstName $lastName".trim().ifEmpty { "Unknown User" }
                val organization = when (whiteLabel) {
                    "rhenti_mobile" -> "Rhenti"
                    else -> whiteLabel.replace("_", " ").replaceFirstChar { it.uppercase() }
                }

                _uiState.value = _uiState.value.copy(
                    userName = name,
                    userEmail = email,
                    userPhone = phone,
                    userOrganization = organization,
                    userFirstName = firstName,
                    userLastName = lastName
                )
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "Failed to load user data", e)
            }
        }
    }

    /**
     * Load theme preference from PreferencesManager.
     */
    private fun loadThemePreference() {
        viewModelScope.launch {
            preferencesManager.themeMode.collect { mode ->
                _selectedThemeMode.value = mode
            }
        }
    }

    /**
     * Load storage preferences from PreferencesManager.
     */
    private fun loadStoragePreferences() {
        viewModelScope.launch {
            launch {
                preferencesManager.mediaRetentionPeriod.collect { period ->
                    _mediaRetentionPeriod.value = period
                }
            }
            launch {
                preferencesManager.messagesPerChat.collect { limit ->
                    _messagesPerChat.value = limit
                }
            }
        }
    }

    /**
     * Calculate storage usage for cache and media.
     */
    private fun calculateStorage() {
        viewModelScope.launch {
            try {
                val cacheDir = context.cacheDir
                val cacheSize = calculateDirectorySize(cacheDir)

                _uiState.value = _uiState.value.copy(
                    cacheSize = formatBytes(cacheSize),
                    storageUsed = formatBytes(cacheSize) // For now, just cache
                )
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "Failed to calculate storage", e)
            }
        }
    }

    /**
     * Calculate the size of a directory recursively.
     */
    private fun calculateDirectorySize(directory: File): Long {
        var size: Long = 0
        if (directory.isDirectory) {
            directory.listFiles()?.forEach { file ->
                size += if (file.isDirectory) {
                    calculateDirectorySize(file)
                } else {
                    file.length()
                }
            }
        } else {
            size = directory.length()
        }
        return size
    }

    /**
     * Format bytes to human-readable string.
     */
    private fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
            bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
            else -> String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0))
        }
    }

    /**
     * Set the selected theme mode and persist it.
     */
    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            preferencesManager.setThemeMode(mode)
            _selectedThemeMode.value = mode
        }
    }

    /**
     * Set the media retention period and persist it.
     */
    fun setMediaRetentionPeriod(period: MediaRetentionPeriod) {
        viewModelScope.launch {
            preferencesManager.setMediaRetentionPeriod(period)
            _mediaRetentionPeriod.value = period
            // Trigger immediate cleanup if needed
            performMediaCleanup()
        }
    }

    /**
     * Set the messages per chat limit and persist it.
     */
    fun setMessagesPerChat(limit: Int) {
        viewModelScope.launch {
            preferencesManager.setMessagesPerChat(limit)
            _messagesPerChat.value = limit
            // Trigger immediate cleanup
            performMessageCleanup()
        }
    }

    /**
     * Check if media cleanup is needed and perform it.
     */
    private fun checkMediaCleanup() {
        viewModelScope.launch {
            try {
                val lastCleanup = preferencesManager.lastMediaCleanup
                lastCleanup.collect { timestamp ->
                    val now = System.currentTimeMillis()
                    val daysSinceCleanup = (now - timestamp) / (1000 * 60 * 60 * 24)

                    // Run cleanup once per day
                    if (daysSinceCleanup >= 1) {
                        performMediaCleanup()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "Failed to check media cleanup", e)
            }
        }
    }

    /**
     * Perform media cleanup based on retention period.
     */
    private suspend fun performMediaCleanup() {
        try {
            val period = _mediaRetentionPeriod.value
            if (period == MediaRetentionPeriod.FOREVER) {
                return // Don't delete anything
            }

            val cacheDir = context.cacheDir
            val now = System.currentTimeMillis()
            val retentionMillis = period.days * 24L * 60 * 60 * 1000

            deleteOldFiles(cacheDir, now - retentionMillis)

            // Update last cleanup time
            preferencesManager.setLastMediaCleanup(now)

            // Recalculate storage
            calculateStorage()
        } catch (e: Exception) {
            android.util.Log.e("SettingsViewModel", "Failed to perform media cleanup", e)
        }
    }

    /**
     * Delete files older than the cutoff time.
     */
    private fun deleteOldFiles(directory: File, cutoffTime: Long) {
        if (directory.isDirectory) {
            directory.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    deleteOldFiles(file, cutoffTime)
                } else if (file.lastModified() < cutoffTime) {
                    file.delete()
                }
            }
        }
    }

    /**
     * Perform message cleanup based on per-chat limit.
     */
    private suspend fun performMessageCleanup() {
        try {
            val limit = _messagesPerChat.value
            if (limit == -1) {
                return // Unlimited messages
            }

            // This would need to call the repository to delete old messages
            // For now, just log it
            android.util.Log.d("SettingsViewModel", "Would clean up messages to limit: $limit per chat")

            // TODO: Implement actual message cleanup via ChatHubRepository
            // chatHubRepository.cleanupOldMessages(limit)
        } catch (e: Exception) {
            android.util.Log.e("SettingsViewModel", "Failed to perform message cleanup", e)
        }
    }

    /**
     * Clear app cache.
     */
    fun clearCache() {
        viewModelScope.launch {
            try {
                val cacheDir = context.cacheDir
                deleteDirectory(cacheDir)
                calculateStorage() // Recalculate after clearing
            } catch (e: Exception) {
                android.util.Log.e("SettingsViewModel", "Failed to clear cache", e)
            }
        }
    }

    /**
     * Delete a directory and all its contents.
     */
    private fun deleteDirectory(directory: File) {
        if (directory.isDirectory) {
            directory.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    deleteDirectory(file)
                } else {
                    file.delete()
                }
            }
        }
    }

    /**
     * Sign out the user by clearing auth data.
     */
    suspend fun signOut() {
        tokenManager.clearAuthData()
    }

    /**
     * Get human-readable label for media retention period.
     */
    fun getMediaRetentionLabel(period: MediaRetentionPeriod): String {
        return when (period) {
            MediaRetentionPeriod.ONE_WEEK -> "1 Week"
            MediaRetentionPeriod.ONE_MONTH -> "1 Month"
            MediaRetentionPeriod.THREE_MONTHS -> "3 Months"
            MediaRetentionPeriod.SIX_MONTHS -> "6 Months"
            MediaRetentionPeriod.ONE_YEAR -> "1 Year"
            MediaRetentionPeriod.FOREVER -> "Forever"
        }
    }

    /**
     * Get human-readable label for messages per chat.
     */
    fun getMessagesPerChatLabel(limit: Int): String {
        return if (limit == -1) "All messages" else "$limit messages"
    }
}

/**
 * UI state for Settings screen.
 */
data class SettingsUiState(
    val userName: String = "Loading...",
    val userEmail: String = "",
    val userPhone: String = "",
    val userOrganization: String = "",
    val userFirstName: String = "",
    val userLastName: String = "",
    val cacheSize: String = "0 MB",
    val storageUsed: String = "0 MB"
)
