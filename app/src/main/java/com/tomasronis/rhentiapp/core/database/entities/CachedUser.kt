package com.tomasronis.rhentiapp.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity for caching user data locally.
 */
@Entity(tableName = "cached_users")
data class CachedUser(
    @PrimaryKey
    val id: String,
    val email: String,
    val firstName: String?,
    val lastName: String?,
    val phone: String?,
    val profilePhotoUri: String?,
    val createdAt: Long,
    val updatedAt: Long
)
