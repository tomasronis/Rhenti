package com.tomasronis.rhentiapp.core.di

import com.tomasronis.rhentiapp.core.database.dao.ContactDao
import com.tomasronis.rhentiapp.core.network.ApiClient
import com.tomasronis.rhentiapp.data.contacts.repository.ContactsRepository
import com.tomasronis.rhentiapp.data.contacts.repository.ContactsRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for Contacts dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object ContactsModule {

    @Provides
    @Singleton
    fun provideContactsRepository(
        apiClient: ApiClient,
        contactDao: ContactDao
    ): ContactsRepository {
        return ContactsRepositoryImpl(
            apiClient = apiClient,
            contactDao = contactDao
        )
    }
}
