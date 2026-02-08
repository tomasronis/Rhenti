package com.tomasronis.rhentiapp.core.database.dao

import androidx.room.*
import com.tomasronis.rhentiapp.core.database.entities.CachedProperty
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for cached properties.
 */
@Dao
interface PropertyDao {

    /**
     * Get all cached properties.
     */
    @Query("SELECT * FROM cached_properties WHERE status = 'active' ORDER BY address ASC")
    suspend fun getProperties(): List<CachedProperty>

    /**
     * Observe all cached properties (reactive).
     */
    @Query("SELECT * FROM cached_properties WHERE status = 'active' ORDER BY address ASC")
    fun observeProperties(): Flow<List<CachedProperty>>

    /**
     * Get a specific property by ID.
     */
    @Query("SELECT * FROM cached_properties WHERE id = :propertyId")
    suspend fun getProperty(propertyId: String): CachedProperty?

    /**
     * Insert or update properties.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProperties(properties: List<CachedProperty>)

    /**
     * Insert or update a single property.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProperty(property: CachedProperty)

    /**
     * Delete all cached properties.
     */
    @Query("DELETE FROM cached_properties")
    suspend fun deleteAll()

    /**
     * Delete old cached properties (older than 7 days).
     */
    @Query("DELETE FROM cached_properties WHERE cachedAt < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)
}
