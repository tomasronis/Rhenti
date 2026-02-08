package com.tomasronis.rhentiapp.core.di

import com.tomasronis.rhentiapp.core.database.RhentiDatabase
import com.tomasronis.rhentiapp.core.database.dao.PropertyDao
import com.tomasronis.rhentiapp.core.network.ApiClient
import com.tomasronis.rhentiapp.data.properties.repository.PropertiesRepository
import com.tomasronis.rhentiapp.data.properties.repository.PropertiesRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for properties dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object PropertiesModule {

    @Provides
    @Singleton
    fun providePropertyDao(database: RhentiDatabase): PropertyDao {
        return database.propertyDao()
    }

    @Provides
    @Singleton
    fun providePropertiesRepository(
        apiClient: ApiClient,
        propertyDao: PropertyDao
    ): PropertiesRepository {
        return PropertiesRepositoryImpl(apiClient, propertyDao)
    }
}
