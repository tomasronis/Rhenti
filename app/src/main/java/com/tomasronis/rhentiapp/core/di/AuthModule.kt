package com.tomasronis.rhentiapp.core.di

import com.squareup.moshi.Moshi
import com.tomasronis.rhentiapp.core.database.dao.UserDao
import com.tomasronis.rhentiapp.core.network.ApiClient
import com.tomasronis.rhentiapp.core.security.TokenManager
import com.tomasronis.rhentiapp.data.auth.repository.AuthRepository
import com.tomasronis.rhentiapp.data.auth.repository.AuthRepositoryImpl
import com.tomasronis.rhentiapp.data.auth.services.GoogleAuthService
import com.tomasronis.rhentiapp.data.auth.services.MicrosoftAuthService
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    @Provides
    @Singleton
    fun provideAuthRepository(
        apiClient: ApiClient,
        tokenManager: TokenManager,
        userDao: UserDao,
        googleAuthService: GoogleAuthService,
        microsoftAuthService: MicrosoftAuthService,
        moshi: Moshi
    ): AuthRepository {
        return AuthRepositoryImpl(
            apiClient,
            tokenManager,
            userDao,
            googleAuthService,
            microsoftAuthService,
            moshi
        )
    }
}
