package com.tomasronis.rhentiapp.core.database.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.tomasronis.rhentiapp.data.properties.models.Property
import com.tomasronis.rhentiapp.data.properties.models.PropertyOwner

/**
 * Cached property entity for offline access.
 */
@Entity(tableName = "cached_properties")
data class CachedProperty(
    @PrimaryKey
    val id: String,
    val address: String,
    val unit: String?,
    val city: String?,
    val province: String?,
    val postalCode: String?,
    val country: String?,
    val status: String,
    val ownerName: String?,
    val ownerEmail: String?,
    val ownerPhone: String?,
    val cachedAt: Long = System.currentTimeMillis()
) {
    /**
     * Convert to domain model.
     */
    fun toProperty(): Property = Property(
        id = id,
        address = address,
        unit = unit,
        city = city,
        province = province,
        postalCode = postalCode,
        country = country,
        status = status,
        propertyOwner = if (ownerName != null && ownerEmail != null) {
            PropertyOwner(
                name = ownerName,
                email = ownerEmail,
                phone = ownerPhone
            )
        } else null
    )

    companion object {
        /**
         * Convert from domain model.
         */
        fun from(property: Property): CachedProperty = CachedProperty(
            id = property.id,
            address = property.address,
            unit = property.unit,
            city = property.city,
            province = property.province,
            postalCode = property.postalCode,
            country = property.country,
            status = property.status,
            ownerName = property.propertyOwner?.name,
            ownerEmail = property.propertyOwner?.email,
            ownerPhone = property.propertyOwner?.phone
        )
    }
}
