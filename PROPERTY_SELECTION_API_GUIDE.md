# Property Selection API Guide for Viewing/Application Links

**Date:** February 8, 2026
**Reference:** iOS Code Base Documentation

## Overview

Based on the iOS implementation, here's how to properly fetch and display properties for viewing and application link selection in chat threads.

---

## API Endpoints

### 1. **Get All Properties**
```
GET /management/properties?status=active
```

**Headers:**
```
Authorization: bearer <JWT_TOKEN>
x-white-label: rhenti_mobile
```

**Response:**
```json
{
  "properties": [
    {
      "_id": "6839cac1a8da4a9e67a93746",
      "address": "2 Shaw St, Toronto, ON M6K 3N5, Canada",
      "unit": "101",
      "status": "active",
      "propertyOwner": {
        "name": "Leasa Demo",
        "email": "demo@rhenti.com",
        "phone": "4163334556"
      }
    }
  ]
}
```

**Purpose:** Fetch all active properties owned by the current user

---

### 2. **Get Property Details**
```
GET /properties/{propertyId}
```

**Headers:**
```
Authorization: bearer <JWT_TOKEN>
x-white-label: rhenti_mobile
```

**Response:**
```json
{
  "_id": "6839cac1a8da4a9e67a93746",
  "address": "2 Shaw St, Toronto, ON M6K 3N5, Canada",
  "unit": "101",
  "city": "Toronto",
  "province": "ON",
  "postalCode": "M6K 3N5",
  "country": "Canada",
  "propertyOwner": {
    "name": "Leasa Demo",
    "email": "demo@rhenti.com"
  }
}
```

**Purpose:** Get detailed information for a specific property

---

### 3. **Get Viewings and Applications by Thread**
```
GET /getViewingAndApplicationsByThreadId/{threadId}
```

**Headers:**
```
Authorization: bearer <JWT_TOKEN>
x-white-label: rhenti_mobile
```

**Response:**
```json
{
  "bookings": [
    {
      "_id": "696546f5080c3d94cbed15ea",
      "property_id": "6839cac1a8da4a9e67a93746",
      "address": "2 Shaw St, Toronto, ON M6K 3N5, Canada",
      "datetime": "2026-01-15T15:00:00.000Z",
      "dateTimeDayInTimeZone": "January 15, 2026 at 10:00 AM",
      "status": "pending",
      "pending": true,
      "hasPendingAlternatives": false,
      "renter_id": "5c944be93fb2b900175e24af",
      "propertyTimeZone": "America/Toronto"
    }
  ],
  "offers": [
    {
      "_id": "offer_id_here",
      "address": "2 Shaw St, Toronto, ON M6K 3N5, Canada",
      "dateTimeDayInTimeZone": "January 10, 2026",
      "offer": {
        "_id": "offer_details_id",
        "price": 2500,
        "status": "pending",
        "proposedStartDate": "2026-02-01T00:00:00.000Z",
        "createdAt": "2026-01-10T12:00:00.000Z"
      }
    }
  ],
  "preAssessment": null,
  "hasSmsCapability": false
}
```

**Purpose:** Get existing viewings (bookings) and applications (offers) for a specific thread/contact

---

## Implementation Strategy

### Current Implementation (Thread Property Only)

**What we have now:**
- Property selection sheet shows only the current thread's property
- Falls back to thread address if no property available
- Works, but limited to single property

### Recommended Implementation (Full Property List)

**What we should implement:**

#### 1. **Fetch User's Properties on App Start**
```kotlin
// In MainTabViewModel or a PropertiesViewModel
class PropertiesRepository @Inject constructor(
    private val apiClient: ApiClient
) {
    suspend fun getProperties(): NetworkResult<List<Property>> {
        return try {
            val response = apiClient.get("/management/properties?status=active")
            NetworkResult.Success(response.properties)
        } catch (e: Exception) {
            NetworkResult.Error(e)
        }
    }
}
```

#### 2. **Cache Properties Locally**
```kotlin
// Room entity
@Entity(tableName = "cached_properties")
data class CachedProperty(
    @PrimaryKey val id: String,
    val address: String,
    val unit: String?,
    val city: String?,
    val status: String,
    val cachedAt: Long
)
```

#### 3. **Show All Properties in Selection Sheet**
```kotlin
// In ThreadDetailScreen
val allProperties = remember {
    // Get from PropertiesViewModel or Repository
    propertiesViewModel.properties.collectAsState()
}

// Pass to PropertySelectionBottomSheet
PropertySelectionBottomSheet(
    properties = allProperties.value,
    onPropertySelected = { property ->
        // Send link with selected property
    }
)
```

#### 4. **Filter Properties by Relevance**
When showing properties for viewing/application links, optionally prioritize:
1. **Thread's property** (if available) - shown first
2. **Properties with existing activity** - properties that already have viewings/applications
3. **All other active properties** - user's full property list

---

## Data Models

### Property Model (Android)
```kotlin
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
)

data class PropertyOwner(
    val name: String,
    val email: String,
    val phone: String?
)
```

### Viewing/Application Models
```kotlin
// Booking (Viewing)
data class Booking(
    val id: String,
    val propertyId: String?,
    val address: String?,
    val datetime: Long, // Unix timestamp in milliseconds
    val dateTimeDayInTimeZone: String?, // Formatted: "January 15, 2026 at 10:00 AM"
    val status: String, // "pending", "confirmed", "declined"
    val pending: Boolean,
    val hasPendingAlternatives: Boolean,
    val renterId: String?,
    val propertyTimeZone: String?
)

// Application (Offer)
data class Offer(
    val id: String,
    val address: String?,
    val dateTimeDayInTimeZone: String?,
    val offer: OfferDetails?
)

data class OfferDetails(
    val id: String,
    val price: Int?,
    val status: String?, // "pending", "approved", "declined"
    val proposedStartDate: Long?,
    val createdAt: Long?
)
```

---

## Integration with Link Messages

### Sending Viewing Link with Property
```kotlin
// In ChatHubViewModel
fun sendLinkMessage(
    type: String, // "viewing-link" or "application-link"
    propertyId: String,
    propertyAddress: String
) {
    val messageText = when (type) {
        "viewing-link" -> "Book a Viewing: $propertyAddress"
        "application-link" -> "Apply to Listing: $propertyAddress"
        else -> return
    }

    // Create message with metadata
    val message = ChatMessage(
        // ... standard fields
        type = type,
        text = messageText,
        metadata = MessageMetadata(
            propertyId = propertyId,
            propertyAddress = propertyAddress
        )
    )

    // Send message
    sendMessage(message)
}
```

### Rendering Link Message Cards
```kotlin
// In MessageList composable
when (message.type) {
    "viewing-link" -> {
        LinkMessageCard(
            type = LinkMessageType.VIEWING,
            propertyAddress = message.metadata?.propertyAddress ?: "Unknown",
            onClick = {
                // Open booking flow for this property
                navigateToBooking(message.metadata?.propertyId)
            }
        )
    }
    "application-link" -> {
        LinkMessageCard(
            type = LinkMessageType.APPLICATION,
            propertyAddress = message.metadata?.propertyAddress ?: "Unknown",
            onClick = {
                // Open application flow for this property
                navigateToApplication(message.metadata?.propertyId)
            }
        )
    }
}
```

---

## Implementation Steps

### Phase 1: Basic Property Fetching (Current Sprint)
1. ✅ Create PropertySelectionBottomSheet component
2. ✅ Show thread's property as default option
3. ⏳ Add `/management/properties` API endpoint
4. ⏳ Fetch user's properties on login
5. ⏳ Cache properties in Room database

### Phase 2: Enhanced Property Selection (Next Sprint)
1. ⏳ Show all user properties in selection sheet
2. ⏳ Add property search by address
3. ⏳ Prioritize relevant properties (thread property first)
4. ⏳ Add property creation option in sheet

### Phase 3: Link Actions (Future Sprint)
1. ⏳ Implement booking flow when viewing link clicked
2. ⏳ Implement application flow when application link clicked
3. ⏳ Deep linking support for property URLs
4. ⏳ Handle link opening from push notifications

---

## API Client Implementation

### Add Properties Endpoints
```kotlin
// In ApiService.kt
interface ApiService {
    @GET("/management/properties")
    suspend fun getProperties(
        @Query("status") status: String = "active"
    ): Response<PropertiesResponse>

    @GET("/properties/{propertyId}")
    suspend fun getPropertyDetails(
        @Path("propertyId") propertyId: String
    ): Response<Property>

    @GET("/getViewingAndApplicationsByThreadId/{threadId}")
    suspend fun getViewingsAndApplications(
        @Path("threadId") threadId: String
    ): Response<ViewingsAndApplicationsResponse>
}
```

### Repository Implementation
```kotlin
class PropertiesRepositoryImpl @Inject constructor(
    private val apiService: ApiService,
    private val propertyDao: PropertyDao
) : PropertiesRepository {

    override suspend fun getProperties(): NetworkResult<List<Property>> {
        return try {
            val response = apiService.getProperties()
            if (response.isSuccessful && response.body() != null) {
                val properties = response.body()!!.properties
                // Cache in Room
                propertyDao.insertProperties(properties.map { CachedProperty.from(it) })
                NetworkResult.Success(properties)
            } else {
                NetworkResult.Error(Exception("Failed to fetch properties"))
            }
        } catch (e: Exception) {
            // Return cached properties if available
            val cached = propertyDao.getProperties()
            if (cached.isNotEmpty()) {
                NetworkResult.Success(cached.map { it.toProperty() })
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
}
```

---

## Testing Checklist

### Property Fetching
- [ ] Properties fetch on login
- [ ] Properties cached in Room
- [ ] Properties update on pull-to-refresh
- [ ] Offline mode shows cached properties
- [ ] Empty state when no properties

### Property Selection
- [ ] Selection sheet shows all properties
- [ ] Search filters properties by address
- [ ] Thread property highlighted/prioritized
- [ ] Selected property passed to link message
- [ ] Property details display correctly (address, unit, city)

### Link Messages
- [ ] Viewing link includes property ID and address
- [ ] Application link includes property ID and address
- [ ] Link cards display correct property
- [ ] Clicking link card opens booking/application flow

---

## Next Steps

1. **Immediate (This Sprint):**
   - Add `/management/properties` endpoint to ApiService
   - Create PropertiesRepository and PropertiesViewModel
   - Add CachedProperty entity to Room database
   - Fetch properties on login/app start
   - Update PropertySelectionBottomSheet to show all properties

2. **Short-Term (Next Sprint):**
   - Add property filtering and prioritization
   - Implement property creation in selection sheet
   - Add property refresh/sync logic
   - Handle property updates from API

3. **Long-Term (Future Sprints):**
   - Implement booking flow integration
   - Implement application flow integration
   - Add deep linking for property URLs
   - Add property analytics and tracking

---

**Key Takeaway:** The iOS app fetches ALL user properties from `/management/properties?status=active` and shows them in the selection sheet. This allows users to send viewing/application links for any of their properties, not just the one associated with the current thread.
