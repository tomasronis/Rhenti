package com.tomasronis.rhentiapp.data.properties.repository

import com.tomasronis.rhentiapp.core.network.NetworkResult
import com.tomasronis.rhentiapp.data.properties.models.Property
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for properties operations.
 * Manages fetching and caching properties.
 */
interface PropertiesRepository {

    /**
     * Get all active properties for the current user.
     * Fetches from API and caches locally.
     */
    suspend fun getProperties(): NetworkResult<List<Property>>

    /**
     * Get a specific property by ID.
     */
    suspend fun getProperty(propertyId: String): NetworkResult<Property>

    /**
     * Observe cached properties (reactive).
     */
    fun observeProperties(): Flow<List<Property>>

    /**
     * Refresh properties from API.
     */
    suspend fun refreshProperties(): NetworkResult<Unit>

    /**
     * Clear all cached properties.
     */
    suspend fun clearCache()
}
