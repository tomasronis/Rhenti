package com.tomasronis.rhentiapp.data.properties.models

/**
 * Response from GET /getAddressesForChatHub
 * Returns array of buildings with nested units
 */
typealias AddressesForChatHubResponse = List<ChatHubBuilding>

/**
 * Building with multiple units.
 * Represents a physical building/property with potentially multiple rental units.
 */
data class ChatHubBuilding(
    val id: String,         // "_id" field from API
    val address: String,
    val units: List<ChatHubUnit>
)

/**
 * Individual unit within a building.
 * The unit ID becomes the propertyId used when sending messages.
 */
data class ChatHubUnit(
    val id: String,         // "_id" field - this becomes propertyId in messages
    val unit: String?       // Unit name/number (can be null for single-unit buildings)
) {
    /**
     * Display name for the unit.
     */
    val displayName: String
        get() = if (!unit.isNullOrEmpty()) "Unit $unit" else ""
}

/**
 * Flattened property for display in picker.
 * Combines building address with unit information.
 */
data class ChatHubProperty(
    val id: String,             // Unit ID (used as propertyId when sending links)
    val address: String,        // Building address
    val unitName: String?,      // Unit name/number
    val buildingId: String
) {
    /**
     * Display address with unit if available.
     */
    val displayAddress: String
        get() = if (!unitName.isNullOrEmpty()) {
            "$address - Unit $unitName"
        } else {
            address
        }

    /**
     * Full building address (without unit).
     */
    val fullAddress: String
        get() = address

    /**
     * Convert to Property model for compatibility with existing code.
     */
    fun toProperty(): Property = Property(
        id = id,
        address = address,
        unit = unitName,
        city = null,
        province = null,
        postalCode = null,
        country = null,
        status = "active",
        propertyOwner = null
    )
}
