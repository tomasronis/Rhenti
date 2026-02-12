package com.tomasronis.rhentiapp

import android.app.Application
import com.tomasronis.rhentiapp.core.notifications.FcmTokenManager
import com.tomasronis.rhentiapp.core.notifications.NotificationChannels
import dagger.hilt.android.HiltAndroidApp
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
    lateinit var fcmTokenManager: FcmTokenManager

    override fun onCreate() {
        super.onCreate()

        // Initialize notification channels (Phase 7 + Phase 8)
        NotificationChannels.createChannels(this)

        // Initialize FCM token (Phase 8)
        // This will fetch the token and sync with backend if user is logged in
        fcmTokenManager.refreshToken()

        // Initialize any application-level components here
        // (e.g., logging, crash reporting, etc.)
    }
}
