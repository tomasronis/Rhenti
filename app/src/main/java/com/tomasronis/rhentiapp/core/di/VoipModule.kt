package com.tomasronis.rhentiapp.core.di

import android.content.Context
import com.tomasronis.rhentiapp.core.security.TokenManager
import com.tomasronis.rhentiapp.core.voip.TwilioManager
import com.tomasronis.rhentiapp.data.calls.repository.CallsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing VoIP dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object VoipModule {

    @Provides
    @Singleton
    fun provideTwilioManager(
        @ApplicationContext context: Context,
        callsRepository: CallsRepository,
        tokenManager: TokenManager
    ): TwilioManager {
        return TwilioManager(context, callsRepository, tokenManager)
    }
}
