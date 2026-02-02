package com.tomasronis.rhentiapp.core.database

import androidx.room.Database
import androidx.room.RoomDatabase
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
 */
@Database(
    entities = [
        CachedUser::class,
        CachedThread::class,
        CachedMessage::class,
        CachedContact::class,
        CachedCallLog::class
    ],
    version = 1,
    exportSchema = false
)
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
