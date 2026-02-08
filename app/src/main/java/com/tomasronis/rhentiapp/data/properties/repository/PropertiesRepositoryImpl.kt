package com.tomasronis.rhentiapp.data.properties.repository

import com.tomasronis.rhentiapp.core.database.dao.PropertyDao
import com.tomasronis.rhentiapp.core.database.entities.CachedProperty
import com.tomasronis.rhentiapp.core.network.ApiClient
import com.tomasronis.rhentiapp.core.network.NetworkResult
import com.tomasronis.rhentiapp.data.properties.models.Property
import com.tomasronis.rhentiapp.data.properties.models.PropertyDto
import com.tomasronis.rhentiapp.data.properties.models.ChatHubBuilding
import com.tomasronis.rhentiapp.data.properties.models.ChatHubUnit
import com.tomasronis.rhentiapp.data.properties.models.ChatHubProperty
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of PropertiesRepository.
 * Fetches properties from API and caches them locally in Room.
 */
@Singleton
class PropertiesRepositoryImpl @Inject constructor(
    private val apiClient: ApiClient,
    private val propertyDao: PropertyDao
) : PropertiesRepository {

    override suspend fun getProperties(): NetworkResult<List<Property>> {
        return try {
            android.util.Log.d("PropertiesRepository", "Fetching properties from /getAddressesForChatHub...")

            // Fetch buildings from API (matches iOS implementation)
            val response = apiClient.getAddressesForChatHub()

            android.util.Log.d("PropertiesRepository", "API response received: ${response.size} buildings")

            // Parse buildings and units
            val buildings = response.mapNotNull { buildingData ->
                try {
                    val buildingMap = buildingData as? Map<*, *> ?: return@mapNotNull null
                    val buildingId = buildingMap["_id"] as? String ?: return@mapNotNull null
                    val address = buildingMap["address"] as? String ?: return@mapNotNull null
                    val unitsData = buildingMap["units"] as? List<*> ?: emptyList<Any?>()

                    val units = unitsData.mapNotNull { unitData ->
                        try {
                            val unitMap = unitData as? Map<*, *> ?: return@mapNotNull null
                            val unitId = unitMap["_id"] as? String ?: return@mapNotNull null
                            val unitName = unitMap["unit"] as? String

                            ChatHubUnit(
                                id = unitId,
                                unit = unitName
                            )
                        } catch (e: Exception) {
                            android.util.Log.w("PropertiesRepository", "Error parsing unit: ${e.message}")
                            null
                        }
                    }

                    ChatHubBuilding(
                        id = buildingId,
                        address = address,
                        units = units
                    )
                } catch (e: Exception) {
                    android.util.Log.e("PropertiesRepository", "Error parsing building: ${e.message}")
                    null
                }
            }

            android.util.Log.d("PropertiesRepository", "Parsed ${buildings.size} buildings")

            // Flatten buildings + units into individual properties (matches iOS implementation)
            val flattenedProperties = buildings.flatMap { building ->
                building.units.map { unit ->
                    ChatHubProperty(
                        id = unit.id,
                        address = building.address,
                        unitName = unit.unit,
                        buildingId = building.id
                    )
                }
            }.sortedWith(compareBy({ it.address }, { it.unitName ?: "" }))

            // Convert to Property model for compatibility
            val properties = flattenedProperties.map { it.toProperty() }

            android.util.Log.d("PropertiesRepository", "Flattened to ${properties.size} properties")

            // Cache in Room
            propertyDao.insertProperties(properties.map { CachedProperty.from(it) })

            android.util.Log.d("PropertiesRepository", "Successfully fetched and cached ${properties.size} properties")
            properties.forEachIndexed { index, property ->
                android.util.Log.d("PropertiesRepository", "  [$index] ${property.address}${property.unit?.let { ", Unit $it" } ?: ""}")
            }

            NetworkResult.Success(properties)
        } catch (e: Exception) {
            android.util.Log.e("PropertiesRepository", "Error fetching properties: ${e.message}", e)

            // Return cached properties if available
            val cached = propertyDao.getProperties()
            if (cached.isNotEmpty()) {
                android.util.Log.d("PropertiesRepository", "Returning ${cached.size} cached properties")
                NetworkResult.Success(cached.map { it.toProperty() })
            } else {
                android.util.Log.e("PropertiesRepository", "No cached properties available")
                NetworkResult.Error(e)
            }
        }
    }

    override suspend fun getProperty(propertyId: String): NetworkResult<Property> {
        return try {
            // Try to get from cache first
            val cached = propertyDao.getProperty(propertyId)
            if (cached != null) {
                return NetworkResult.Success(cached.toProperty())
            }

            // Fetch from API if not cached
            val response = apiClient.getProperty(propertyId)

            // Parse response
            val id = response["_id"] as? String ?: return NetworkResult.Error(Exception("Invalid property ID"))
            val address = response["address"] as? String ?: return NetworkResult.Error(Exception("Invalid address"))
            val unit = response["unit"] as? String
            val city = response["city"] as? String
            val province = response["province"] as? String
            val postalCode = (response["postal_code"] ?: response["postalCode"]) as? String
            val country = response["country"] as? String
            val status = (response["status"] as? String) ?: "active"

            // Parse property owner if available
            val ownerMap = (response["property_owner"] ?: response["propertyOwner"]) as? Map<*, *>
            val propertyOwner = if (ownerMap != null) {
                com.tomasronis.rhentiapp.data.properties.models.PropertyOwner(
                    name = ownerMap["name"] as? String ?: "",
                    email = ownerMap["email"] as? String ?: "",
                    phone = ownerMap["phone"] as? String
                )
            } else null

            val property = Property(
                id = id,
                address = address,
                unit = unit,
                city = city,
                province = province,
                postalCode = postalCode,
                country = country,
                status = status,
                propertyOwner = propertyOwner
            )

            // Cache the property
            propertyDao.insertProperty(CachedProperty.from(property))

            NetworkResult.Success(property)
        } catch (e: Exception) {
            android.util.Log.e("PropertiesRepository", "Error fetching property: ${e.message}")

            // Try to return cached property
            val cached = propertyDao.getProperty(propertyId)
            if (cached != null) {
                NetworkResult.Success(cached.toProperty())
            } else {
                NetworkResult.Error(e)
            }
        }
    }

    override fun observeProperties(): Flow<List<Property>> {
        return propertyDao.observeProperties().map { cached ->
            cached.map { it.toProperty() }
        }
    }

    override suspend fun refreshProperties(): NetworkResult<Unit> {
        return when (val result = getProperties()) {
            is NetworkResult.Success -> NetworkResult.Success(Unit)
            is NetworkResult.Error -> NetworkResult.Error(result.exception)
            is NetworkResult.Loading -> NetworkResult.Loading()
        }
    }

    override suspend fun clearCache() {
        propertyDao.deleteAll()
    }
}
