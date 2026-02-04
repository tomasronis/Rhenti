package com.tomasronis.rhentiapp.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.tomasronis.rhentiapp.core.database.dao.*
import com.tomasronis.rhentiapp.core.database.entities.*

/**
 * Room database for the Rhenti application.
 *
 * This database provides offline storage for:
 * - Users
 * - Chat threads and messages
 * - Contacts
 * - Call logs
 *
 * Version 1 - Initial schema
 * Version 2 - Added members field to CachedThread
 */
@Database(
    entities = [
        CachedUser::class,
        CachedThread::class,
        CachedMessage::class,
        CachedContact::class,
        CachedCallLog::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class RhentiDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun threadDao(): ThreadDao
    abstract fun messageDao(): MessageDao
    abstract fun contactDao(): ContactDao
    abstract fun callLogDao(): CallLogDao

    companion object {
        const val DATABASE_NAME = "rhenti_database"
    }
}
