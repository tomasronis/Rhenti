package com.tomasronis.rhentiapp

import android.app.Application
import android.util.Log
import com.tomasronis.rhentiapp.core.messaging.FCMTokenRepository
import com.tomasronis.rhentiapp.core.notifications.NotificationChannels
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Application class for Rhenti App.
 *
 * This class is annotated with @HiltAndroidApp to enable Hilt dependency injection
 * throughout the application.
 */
@HiltAndroidApp
class RhentiApplication : Application() {

    @Inject
    lateinit var fcmTokenRepository: FCMTokenRepository

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        // Initialize notification channels for all notification types
        NotificationChannels.createChannels(this)

        // Register FCM token with backend
        applicationScope.launch {
            try {
                fcmTokenRepository.registerCurrentToken()
            } catch (e: Exception) {
                Log.e("RhentiApplication", "Failed to register FCM token on startup", e)
            }
        }
    }
}
