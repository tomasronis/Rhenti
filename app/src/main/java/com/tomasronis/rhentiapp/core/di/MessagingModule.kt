package com.tomasronis.rhentiapp.core.di

import android.content.Context
import com.tomasronis.rhentiapp.core.messaging.FCMTokenRepository
import com.tomasronis.rhentiapp.core.network.ApiClient
import com.tomasronis.rhentiapp.core.notifications.PushNotificationManager
import com.tomasronis.rhentiapp.core.security.EncryptedPreferences
import com.tomasronis.rhentiapp.core.security.TokenManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MessagingModule {

    @Provides
    @Singleton
    fun providePushNotificationManager(
        @ApplicationContext context: Context
    ): PushNotificationManager {
        return PushNotificationManager(context)
    }

    @Provides
    @Singleton
    fun provideFCMTokenRepository(
        apiClient: ApiClient,
        tokenManager: TokenManager,
        encryptedPreferences: EncryptedPreferences
    ): FCMTokenRepository {
        return FCMTokenRepository(apiClient, tokenManager, encryptedPreferences)
    }
}
