# Property Picker Update - Switch to getAddressesForChatHub

**Date:** February 8, 2026
**Status:** ✅ Complete - Ready for Testing

## Summary

Updated Android app to match iOS implementation by using `/getAddressesForChatHub` endpoint instead of `/management/properties` for the property picker in viewing/application link selection.

---

## What Changed

### 1. ✅ New Data Models

**File:** `app/src/main/java/com/tomasronis/rhentiapp/data/properties/models/ChatHubPropertyModels.kt`

Created iOS-matching models:

```kotlin
// Response is array of buildings
typealias AddressesForChatHubResponse = List<ChatHubBuilding>

// Building with nested units
data class ChatHubBuilding(
    val id: String,         // "_id"
    val address: String,
    val units: List<ChatHubUnit>
)

// Individual unit (becomes propertyId)
data class ChatHubUnit(
    val id: String,         // "_id" - used as propertyId in messages
    val unit: String?       // unit name/number (nullable)
)

// Flattened for display
data class ChatHubProperty(
    val id: String,         // Unit ID
    val address: String,    // Building address
    val unitName: String?,  // Unit name
    val buildingId: String
)
```

### 2. ✅ Updated Repository

**File:** `app/src/main/java/com/tomasronis/rhentiapp/data/properties/repository/PropertiesRepositoryImpl.kt`

Changed `getProperties()` to:
1. Call `apiClient.getAddressesForChatHub()` instead of `getProperties()`
2. Parse hierarchical response (buildings → units)
3. Flatten buildings+units into individual properties
4. Sort by address, then unit name
5. Convert to `Property` model for backward compatibility
6. Cache in Room as before

**Key Logic:**
```kotlin
// Fetch buildings
val buildings = parseBuildings(response)

// Flatten to properties (matches iOS)
val flattenedProperties = buildings.flatMap { building ->
    building.units.map { unit ->
        ChatHubProperty(
            id = unit.id,              // Unit ID becomes property ID
            address = building.address,
            unitName = unit.unit,
            buildingId = building.id
        )
    }
}.sortedWith(compareBy({ it.address }, { it.unitName ?: "" }))

// Convert to Property for compatibility
val properties = flattenedProperties.map { it.toProperty() }
```

### 3. ✅ Enhanced Logging

Added detailed debug logs:
- API endpoint being called
- Number of buildings received
- Number of units parsed
- Flattening results
- Final property count
- Individual property addresses with units

---

## API Endpoint Details

### Endpoint
```
GET /getAddressesForChatHub
```

### Headers
```
Authorization: bearer <JWT_TOKEN>
x-white-label: rhenti_mobile
```

### Response Format
```json
[
  {
    "_id": "building123",
    "address": "2 Shaw St, Toronto, ON M6K 3N5, Canada",
    "units": [
      {
        "_id": "property456",
        "unit": "101"
      },
      {
        "_id": "property789",
        "unit": "102"
      }
    ]
  },
  {
    "_id": "building999",
    "address": "456 Main St, Toronto, ON",
    "units": [
      {
        "_id": "property111",
        "unit": null
      }
    ]
  }
]
```

### Key Differences from /management/properties

| Feature | /management/properties | /getAddressesForChatHub |
|---------|----------------------|------------------------|
| Structure | Flat list of properties | Hierarchical (buildings → units) |
| Response Key | `{ "properties": [...] }` | Array directly `[...]` |
| Building Info | Included in each property | Separate building level |
| Multi-unit Support | Each unit is separate | Units grouped by building |
| Purpose | General property management | Chat hub property selection |

---

## Testing Checklist

### Build & Compile
- [ ] Project builds successfully in Android Studio
- [ ] No compilation errors
- [ ] All imports resolved

### Functional Testing
1. **Login to the app**
   - [ ] Login successful

2. **Open a chat thread**
   - [ ] Navigate to Messages tab
   - [ ] Open any thread

3. **Open property picker**
   - [ ] Tap attachment (paperclip) button
   - [ ] Select "Book a Viewing Link" or "Application Link"

4. **Verify property list**
   - [ ] Property selection bottom sheet appears
   - [ ] **All user properties displayed** (not just thread property)
   - [ ] Properties show address and unit number
   - [ ] Properties sorted by address, then unit
   - [ ] Search works correctly

5. **Select a property**
   - [ ] Tap a property
   - [ ] Link message sent with correct property ID and address
   - [ ] Bottom sheet closes

### Logcat Verification

Filter for "PropertiesRepository" and verify logs show:

```
D/PropertiesRepository: Fetching properties from /getAddressesForChatHub...
D/PropertiesRepository: API response received: X buildings
D/PropertiesRepository: Parsed X buildings
D/PropertiesRepository: Flattened to X properties
D/PropertiesRepository: Successfully fetched and cached X properties
D/PropertiesRepository:   [0] 2 Shaw St, Toronto, ON, Unit 101
D/PropertiesRepository:   [1] 2 Shaw St, Toronto, ON, Unit 102
D/PropertiesRepository:   [2] 456 Main St, Toronto, ON
```

### Edge Cases
- [ ] Buildings with no units handled correctly
- [ ] Buildings with single unit (no unit number)
- [ ] Buildings with multiple units
- [ ] Empty response (user has no properties)
- [ ] Network error - cached properties shown
- [ ] Offline mode - cached properties work

---

## Backward Compatibility

✅ **Fully backward compatible**

- Property picker UI unchanged
- Property model unchanged (uses existing `Property` class)
- Room caching unchanged
- ViewModel unchanged
- UI components unchanged

Only the **data source** changed (API endpoint + parsing logic).

---

## Benefits of This Change

1. ✅ **Matches iOS Implementation**
   - Same API endpoint
   - Same data structure
   - Same flattening logic
   - Easier to maintain parity

2. ✅ **Better Multi-Unit Support**
   - Buildings with units properly grouped
   - Clearer relationship between building and units
   - Reduces duplicate address strings

3. ✅ **Chat Hub Optimized**
   - Endpoint specifically designed for chat hub property selection
   - May have better filtering/permissions for chat context
   - Likely more performant for this use case

4. ✅ **Future-Proof**
   - If backend adds building-level features, we're ready
   - Consistent with iOS makes future API changes easier

---

## Files Changed

### New Files
1. `app/src/main/java/com/tomasronis/rhentiapp/data/properties/models/ChatHubPropertyModels.kt` (NEW)

### Modified Files
1. `app/src/main/java/com/tomasronis/rhentiapp/data/properties/repository/PropertiesRepositoryImpl.kt`

### Unchanged (Backward Compatible)
- ✅ `PropertyDao.kt` - Still uses same `CachedProperty` entity
- ✅ `Property.kt` - Model unchanged
- ✅ `PropertiesViewModel.kt` - No changes needed
- ✅ `PropertySelectionBottomSheet.kt` - UI unchanged
- ✅ `ThreadDetailScreen.kt` - Logic unchanged

---

## Next Steps

1. **Build the project** in Android Studio
2. **Run on device/emulator**
3. **Test property picker** with viewing/application links
4. **Verify Logcat logs** show correct API calls
5. **Report results** - especially property count and display

---

## Rollback Plan (If Needed)

If this causes issues, revert by:

1. Delete `ChatHubPropertyModels.kt`
2. Restore `PropertiesRepositoryImpl.kt` from git:
   ```bash
   git checkout app/src/main/java/com/tomasronis/rhentiapp/data/properties/repository/PropertiesRepositoryImpl.kt
   ```
3. Rebuild

The old implementation used `/management/properties` endpoint.

---

## Questions?

If properties still don't show:
1. Check Logcat for "PropertiesRepository" logs
2. Verify API response format matches expected structure
3. Check if user has properties in the system
4. Verify authentication token is valid

Share Logcat output for debugging!
