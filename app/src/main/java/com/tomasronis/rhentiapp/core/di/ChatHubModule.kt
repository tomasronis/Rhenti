package com.tomasronis.rhentiapp.core.di

import com.tomasronis.rhentiapp.core.database.dao.MessageDao
import com.tomasronis.rhentiapp.core.database.dao.ThreadDao
import com.tomasronis.rhentiapp.core.network.ApiClient
import com.tomasronis.rhentiapp.data.chathub.repository.ChatHubRepository
import com.tomasronis.rhentiapp.data.chathub.repository.ChatHubRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for Chat Hub dependencies.
 */
@Module
@InstallIn(SingletonComponent::class)
object ChatHubModule {

    @Provides
    @Singleton
    fun provideChatHubRepository(
        apiClient: ApiClient,
        threadDao: ThreadDao,
        messageDao: MessageDao
    ): ChatHubRepository {
        return ChatHubRepositoryImpl(
            apiClient = apiClient,
            threadDao = threadDao,
            messageDao = messageDao
        )
    }
}
