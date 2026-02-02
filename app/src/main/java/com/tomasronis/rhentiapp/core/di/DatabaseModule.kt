package com.tomasronis.rhentiapp.core.di

import android.content.Context
import androidx.room.Room
import com.tomasronis.rhentiapp.core.database.RhentiDatabase
import com.tomasronis.rhentiapp.core.database.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing database-related dependencies.
 *
 * Provides the Room database instance and all DAOs.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideRhentiDatabase(
        @ApplicationContext context: Context
    ): RhentiDatabase {
        return Room.databaseBuilder(
            context,
            RhentiDatabase::class.java,
            RhentiDatabase.DATABASE_NAME
        )
            .fallbackToDestructiveMigration() // For development - remove in production
            .build()
    }

    @Provides
    @Singleton
    fun provideUserDao(database: RhentiDatabase): UserDao {
        return database.userDao()
    }

    @Provides
    @Singleton
    fun provideThreadDao(database: RhentiDatabase): ThreadDao {
        return database.threadDao()
    }

    @Provides
    @Singleton
    fun provideMessageDao(database: RhentiDatabase): MessageDao {
        return database.messageDao()
    }

    @Provides
    @Singleton
    fun provideContactDao(database: RhentiDatabase): ContactDao {
        return database.contactDao()
    }

    @Provides
    @Singleton
    fun provideCallLogDao(database: RhentiDatabase): CallLogDao {
        return database.callLogDao()
    }
}
