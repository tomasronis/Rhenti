package com.tomasronis.rhentiapp.core.database.dao

import androidx.room.*
import com.tomasronis.rhentiapp.core.database.entities.CachedCallLog
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for CachedCallLog entity.
 */
@Dao
interface CallLogDao {

    @Query("SELECT * FROM cached_call_logs ORDER BY startTime DESC")
    fun getAllCallLogs(): Flow<List<CachedCallLog>>

    @Query("SELECT * FROM cached_call_logs WHERE id = :callLogId")
    fun getCallLogById(callLogId: String): Flow<CachedCallLog?>

    @Query("SELECT * FROM cached_call_logs WHERE contactId = :contactId ORDER BY startTime DESC")
    fun getCallLogsByContact(contactId: String): Flow<List<CachedCallLog>>

    @Query("SELECT * FROM cached_call_logs WHERE callType = :callType ORDER BY startTime DESC")
    fun getCallLogsByType(callType: String): Flow<List<CachedCallLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallLog(callLog: CachedCallLog)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCallLogs(callLogs: List<CachedCallLog>)

    @Delete
    suspend fun deleteCallLog(callLog: CachedCallLog)

    @Query("DELETE FROM cached_call_logs WHERE id = :callLogId")
    suspend fun deleteCallLogById(callLogId: String)

    @Query("DELETE FROM cached_call_logs")
    suspend fun deleteAllCallLogs()
}
