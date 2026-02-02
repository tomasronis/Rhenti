package com.tomasronis.rhentiapp.core.database.dao

import androidx.room.*
import com.tomasronis.rhentiapp.core.database.entities.CachedThread
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for CachedThread entity.
 */
@Dao
interface ThreadDao {

    @Query("SELECT * FROM cached_threads ORDER BY lastMessageTime DESC")
    fun getAllThreads(): Flow<List<CachedThread>>

    @Query("SELECT * FROM cached_threads WHERE id = :threadId")
    fun getThreadById(threadId: String): Flow<CachedThread?>

    @Query("SELECT * FROM cached_threads WHERE unreadCount > 0 ORDER BY lastMessageTime DESC")
    fun getUnreadThreads(): Flow<List<CachedThread>>

    @Query("SELECT * FROM cached_threads WHERE isPinned = 1 ORDER BY lastMessageTime DESC")
    fun getPinnedThreads(): Flow<List<CachedThread>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertThread(thread: CachedThread)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertThreads(threads: List<CachedThread>)

    @Query("UPDATE cached_threads SET unreadCount = 0 WHERE id = :threadId")
    suspend fun clearUnreadCount(threadId: String)

    @Query("UPDATE cached_threads SET isPinned = :pinned WHERE id = :threadId")
    suspend fun updatePinned(threadId: String, pinned: Boolean)

    @Delete
    suspend fun deleteThread(thread: CachedThread)

    @Query("DELETE FROM cached_threads WHERE id = :threadId")
    suspend fun deleteThreadById(threadId: String)

    @Query("DELETE FROM cached_threads")
    suspend fun deleteAllThreads()
}
