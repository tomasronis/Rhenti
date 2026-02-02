package com.tomasronis.rhentiapp.core.database.dao

import androidx.room.*
import com.tomasronis.rhentiapp.core.database.entities.CachedMessage
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for CachedMessage entity.
 */
@Dao
interface MessageDao {

    @Query("SELECT * FROM cached_messages WHERE threadId = :threadId ORDER BY createdAt ASC")
    fun getMessagesByThread(threadId: String): Flow<List<CachedMessage>>

    @Query("SELECT * FROM cached_messages WHERE id = :messageId")
    fun getMessageById(messageId: String): Flow<CachedMessage?>

    @Query("SELECT * FROM cached_messages WHERE status = 'pending' OR status = 'failed' ORDER BY createdAt ASC")
    fun getPendingMessages(): Flow<List<CachedMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: CachedMessage)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessages(messages: List<CachedMessage>)

    @Query("UPDATE cached_messages SET status = :status WHERE id = :messageId")
    suspend fun updateMessageStatus(messageId: String, status: String)

    @Query("UPDATE cached_messages SET readAt = :readAt WHERE id = :messageId")
    suspend fun markAsRead(messageId: String, readAt: Long)

    @Delete
    suspend fun deleteMessage(message: CachedMessage)

    @Query("DELETE FROM cached_messages WHERE id = :messageId")
    suspend fun deleteMessageById(messageId: String)

    @Query("DELETE FROM cached_messages WHERE threadId = :threadId")
    suspend fun deleteMessagesByThread(threadId: String)

    @Query("DELETE FROM cached_messages")
    suspend fun deleteAllMessages()

    @Query("SELECT COUNT(*) FROM cached_messages WHERE threadId = :threadId")
    suspend fun getMessageCountForThread(threadId: String): Int
}
