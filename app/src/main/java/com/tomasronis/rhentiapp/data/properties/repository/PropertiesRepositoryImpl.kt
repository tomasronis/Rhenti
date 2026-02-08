package com.tomasronis.rhentiapp.data.properties.repository

import com.tomasronis.rhentiapp.core.database.dao.PropertyDao
import com.tomasronis.rhentiapp.core.database.entities.CachedProperty
import com.tomasronis.rhentiapp.core.network.ApiClient
import com.tomasronis.rhentiapp.core.network.NetworkResult
import com.tomasronis.rhentiapp.data.properties.models.Property
import com.tomasronis.rhentiapp.data.properties.models.PropertyDto
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
            // Fetch from API
            val response = apiClient.getProperties(status = "active")

            // Parse response
            val propertiesData = response["properties"] as? List<*>
            if (propertiesData == null) {
                // Return cached if API fails
                val cached = propertyDao.getProperties()
                return if (cached.isNotEmpty()) {
                    NetworkResult.Success(cached.map { it.toProperty() })
                } else {
                    NetworkResult.Error(Exception("No properties found"))
                }
            }

            // Map to Property objects
            val properties = propertiesData.mapNotNull { item ->
                try {
                    val map = item as? Map<*, *> ?: return@mapNotNull null
                    val id = map["_id"] as? String ?: return@mapNotNull null
                    val address = map["address"] as? String ?: return@mapNotNull null
                    val unit = map["unit"] as? String
                    val city = map["city"] as? String
                    val province = map["province"] as? String
                    val postalCode = (map["postal_code"] ?: map["postalCode"]) as? String
                    val country = map["country"] as? String
                    val status = (map["status"] as? String) ?: "active"

                    // Parse property owner if available
                    val ownerMap = (map["property_owner"] ?: map["propertyOwner"]) as? Map<*, *>
                    val propertyOwner = if (ownerMap != null) {
                        com.tomasronis.rhentiapp.data.properties.models.PropertyOwner(
                            name = ownerMap["name"] as? String ?: "",
                            email = ownerMap["email"] as? String ?: "",
                            phone = ownerMap["phone"] as? String
                        )
                    } else null

                    Property(
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
                } catch (e: Exception) {
                    android.util.Log.e("PropertiesRepository", "Error parsing property: ${e.message}")
                    null
                }
            }

            // Cache in Room
            propertyDao.insertProperties(properties.map { CachedProperty.from(it) })

            NetworkResult.Success(properties)
        } catch (e: Exception) {
            android.util.Log.e("PropertiesRepository", "Error fetching properties: ${e.message}")

            // Return cached properties if available
            val cached = propertyDao.getProperties()
            if (cached.isNotEmpty()) {
                NetworkResult.Success(cached.map { it.toProperty() })
            } else {
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
            is NetworkResult.Loading -> NetworkResult.Loading
        }
    }

    override suspend fun clearCache() {
        propertyDao.deleteAll()
    }
}
