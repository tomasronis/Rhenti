package com.tomasronis.rhentiapp.data.properties.models

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * Domain model for a property.
 */
data class Property(
    val id: String,
    val address: String,
    val unit: String? = null,
    val city: String? = null,
    val province: String? = null,
    val postalCode: String? = null,
    val country: String? = null,
    val status: String, // "active", "inactive", "archived"
    val propertyOwner: PropertyOwner? = null
) {
    /**
     * Display address with unit if available.
     */
    val displayAddress: String
        get() = if (unit != null) "$address, Unit $unit" else address

    /**
     * Full address with city, province, postal code.
     */
    val fullAddress: String
        get() = buildString {
            append(address)
            if (unit != null) append(", Unit $unit")
            if (city != null) append(", $city")
            if (province != null) append(", $province")
            if (postalCode != null) append(" $postalCode")
            if (country != null) append(", $country")
        }
}

/**
 * Property owner information.
 */
data class PropertyOwner(
    val name: String,
    val email: String,
    val phone: String? = null
)

/**
 * API response for properties list.
 */
@JsonClass(generateAdapter = true)
data class PropertiesResponse(
    @Json(name = "properties")
    val properties: List<PropertyDto>
)

/**
 * DTO for Property from API.
 * Maps both snake_case and camelCase field names for compatibility.
 */
@JsonClass(generateAdapter = true)
data class PropertyDto(
    @Json(name = "_id")
    val id: String,
    @Json(name = "address")
    val address: String,
    @Json(name = "unit")
    val unit: String? = null,
    @Json(name = "city")
    val city: String? = null,
    @Json(name = "province")
    val province: String? = null,
    @Json(name = "postal_code")
    val postalCode: String? = null,
    @Json(name = "postalCode")
    val postalCodeCamel: String? = null,
    @Json(name = "country")
    val country: String? = null,
    @Json(name = "status")
    val status: String? = "active",
    @Json(name = "property_owner")
    val propertyOwner: PropertyOwnerDto? = null,
    @Json(name = "propertyOwner")
    val propertyOwnerCamel: PropertyOwnerDto? = null
) {
    /**
     * Convert DTO to domain model.
     */
    fun toProperty(): Property = Property(
        id = id,
        address = address,
        unit = unit,
        city = city,
        province = province,
        postalCode = postalCode ?: postalCodeCamel,
        country = country,
        status = status ?: "active",
        propertyOwner = (propertyOwner ?: propertyOwnerCamel)?.toPropertyOwner()
    )
}

/**
 * DTO for PropertyOwner from API.
 */
@JsonClass(generateAdapter = true)
data class PropertyOwnerDto(
    @Json(name = "name")
    val name: String,
    @Json(name = "email")
    val email: String,
    @Json(name = "phone")
    val phone: String? = null
) {
    /**
     * Convert DTO to domain model.
     */
    fun toPropertyOwner(): PropertyOwner = PropertyOwner(
        name = name,
        email = email,
        phone = phone
    )
}
