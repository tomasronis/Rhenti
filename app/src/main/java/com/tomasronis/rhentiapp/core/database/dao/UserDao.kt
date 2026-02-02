package com.tomasronis.rhentiapp.core.database.dao

import androidx.room.*
import com.tomasronis.rhentiapp.core.database.entities.CachedUser
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for CachedUser entity.
 */
@Dao
interface UserDao {

    @Query("SELECT * FROM cached_users WHERE id = :userId")
    fun getUserById(userId: String): Flow<CachedUser?>

    @Query("SELECT * FROM cached_users")
    fun getAllUsers(): Flow<List<CachedUser>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: CachedUser)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<CachedUser>)

    @Delete
    suspend fun deleteUser(user: CachedUser)

    @Query("DELETE FROM cached_users WHERE id = :userId")
    suspend fun deleteUserById(userId: String)

    @Query("DELETE FROM cached_users")
    suspend fun deleteAllUsers()
}
