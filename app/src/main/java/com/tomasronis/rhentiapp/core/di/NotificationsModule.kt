package com.tomasronis.rhentiapp.core.di

import com.tomasronis.rhentiapp.data.notifications.repository.NotificationsRepository
import com.tomasronis.rhentiapp.data.notifications.repository.NotificationsRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing notification-related dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationsModule {

    @Binds
    @Singleton
    abstract fun bindNotificationsRepository(
        impl: NotificationsRepositoryImpl
    ): NotificationsRepository
}
