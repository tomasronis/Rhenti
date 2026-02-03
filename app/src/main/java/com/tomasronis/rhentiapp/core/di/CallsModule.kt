package com.tomasronis.rhentiapp.core.di

import com.tomasronis.rhentiapp.data.calls.repository.CallsRepository
import com.tomasronis.rhentiapp.data.calls.repository.CallsRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing calls dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class CallsModule {

    @Binds
    @Singleton
    abstract fun bindCallsRepository(
        impl: CallsRepositoryImpl
    ): CallsRepository
}
