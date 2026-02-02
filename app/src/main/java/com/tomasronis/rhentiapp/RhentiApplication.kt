package com.tomasronis.rhentiapp

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application class for Rhenti App.
 *
 * This class is annotated with @HiltAndroidApp to enable Hilt dependency injection
 * throughout the application.
 */
@HiltAndroidApp
class RhentiApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize any application-level components here
        // (e.g., logging, crash reporting, etc.)
    }
}
