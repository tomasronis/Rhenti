# Chat API Fixes Applied - Android Implementation

## Date: February 2, 2026
## Status: ✅ **COMPLETE**

---

## Overview

Fixed critical discrepancies between the Android chat implementation and the iOS/API reference to ensure chat messages are sent with the correct format matching the production API.

---

## Changes Made

### 1. ✅ Updated ChatThread Model

**File:** `app/src/main/java/com/tomasronis/rhentiapp/data/chathub/models/ChatHubModels.kt`

**Changes:**
- Added `members: Map<String, Int>?` field to store actual member IDs and badge counts from API
- Added computed property `membersObject` that returns the members map for sending messages

**Before:**
```kotlin
data class ChatThread(
    val id: String,
    val displayName: String,
    // ... other fields ...
    val isPinned: Boolean
)
```

**After:**
```kotlin
data class ChatThread(
    val id: String,
    val displayName: String,
    // ... other fields ...
    val isPinned: Boolean,
    val members: Map<String, Int>?
) {
    val membersObject: Map<String, Int>
        get() = members ?: emptyMap()
}
```

---

### 2. ✅ Fixed Thread Response Parsing

**File:** `app/src/main/java/com/tomasronis/rhentiapp/data/chathub/repository/ChatHubRepositoryImpl.kt`

**Changes:**
- Changed response key from `"threads"` to `"chatThreads"`
- Added parsing of `members` field from API response
- Updated thread request format to use `"skip"` instead of `"offset"`
- Removed unnecessary `super_account_id` from request root

**Before:**
```kotlin
val threadsData = response["threads"] as? List<Map<String, Any>> ?: emptyList()
```

**After:**
```kotlin
val threadsData = response["chatThreads"] as? List<Map<String, Any>> ?: emptyList()
// ... later in parsing ...
val members = threadData["members"] as? Map<String, Int>
```

---

### 3. ✅ Fixed Send Message Format

**File:** `app/src/main/java/com/tomasronis/rhentiapp/data/chathub/repository/ChatHubRepositoryImpl.kt`

**Changes:**
- Completely restructured message sending to match iOS format
- Added nested `message` object with `_id`, `createdAt`, `text`, and `user` fields
- Changed `chatSessionMembersObj` to use actual member IDs from thread instead of hardcoded "renter"/"owner"
- Changed keys from snake_case to camelCase (`chatSessionId` not `chat_session_id`)
- Added user info with `name` and `_id` fields

**Before:**
```kotlin
val request = mapOf(
    "chat_session_id" to chatSessionId,
    "message" to text,
    "type" to "text",
    "chatSessionMembersObj" to mapOf(
        "renter" to 0,
        "owner" to 0
    )
)
```

**After:**
```kotlin
val timestamp = System.currentTimeMillis()
val request = mapOf(
    "message" to mapOf(
        "_id" to timestamp,
        "createdAt" to timestamp,
        "text" to text,
        "user" to mapOf(
            "name" to userName,
            "_id" to senderId
        )
    ),
    "chatSessionId" to chatSessionId,
    "chatSessionMembersObj" to thread.membersObject  // Actual user IDs!
)
```

---

### 4. ✅ Fixed Send Image Format

**File:** `app/src/main/java/com/tomasronis/rhentiapp/data/chathub/repository/ChatHubRepositoryImpl.kt`

**Changes:**
- Changed field from `"attachment"` to `"base64"`
- Added `"data:image/jpeg;base64,"` prefix to base64 string
- Used nested message structure matching text messages
- Included user info with name and ID

**Before:**
```kotlin
val request = mapOf(
    "chat_session_id" to chatSessionId,
    "type" to "image",
    "attachment" to imageBase64,
    "chatSessionMembersObj" to mapOf("renter" to 0, "owner" to 0)
)
```

**After:**
```kotlin
val timestamp = System.currentTimeMillis()
val base64WithPrefix = "data:image/jpeg;base64,$imageBase64"
val request = mapOf(
    "message" to mapOf(
        "_id" to timestamp,
        "createdAt" to timestamp,
        "base64" to base64WithPrefix,
        "user" to mapOf(
            "name" to userName,
            "_id" to senderId
        )
    ),
    "chatSessionId" to chatSessionId,
    "chatSessionMembersObj" to thread.membersObject
)
```

---

### 5. ✅ Updated TokenManager for User Names

**File:** `app/src/main/java/com/tomasronis/rhentiapp/core/security/TokenManager.kt`

**Changes:**
- Added methods to save/retrieve user's first and last name
- Added `getUserFullName()` method to get formatted full name
- Updated `saveAuthData()` to accept firstName and lastName parameters
- Updated `clearAuthData()` to also clear name fields

**New Methods:**
```kotlin
suspend fun saveUserFirstName(firstName: String)
suspend fun getUserFirstName(): String?
suspend fun saveUserLastName(lastName: String)
suspend fun getUserLastName(): String?
suspend fun getUserFullName(): String
```

---

### 6. ✅ Updated AuthRepository to Save Names

**File:** `app/src/main/java/com/tomasronis/rhentiapp/data/auth/repository/AuthRepositoryImpl.kt`

**Changes:**
- Modified `saveAuthData()` to save user's first and last name to TokenManager

**Before:**
```kotlin
tokenManager.saveAuthData(
    token = response.token,
    userId = response.userId,
    whiteLabel = response.whiteLabel,
    superAccountId = response.superAccountId
)
```

**After:**
```kotlin
tokenManager.saveAuthData(
    token = response.token,
    userId = response.userId,
    whiteLabel = response.whiteLabel,
    superAccountId = response.superAccountId,
    firstName = response.profile.firstName ?: "",
    lastName = response.profile.lastName ?: ""
)
```

---

### 7. ✅ Updated ChatHubViewModel

**File:** `app/src/main/java/com/tomasronis/rhentiapp/presentation/main/chathub/ChatHubViewModel.kt`

**Changes:**
- Updated `sendTextMessage()` to get user's full name and pass it to repository
- Updated `sendImageMessage()` to get user's full name and pass it to repository
- Now passes the entire thread object to repository for accessing membersObject

**Changes:**
```kotlin
// In sendTextMessage() and sendImageMessage():
val userId = tokenManager.getUserId() ?: return@launch
val userName = tokenManager.getUserFullName()  // ✅ Added

// Updated repository calls:
repository.sendTextMessage(userId, userName, legacyChatSessionId, text, currentThread)
repository.sendImageMessage(userId, userName, legacyChatSessionId, imageBase64, currentThread)
```

---

### 8. ✅ Updated Repository Interface

**File:** `app/src/main/java/com/tomasronis/rhentiapp/data/chathub/repository/ChatHubRepository.kt`

**Changes:**
- Updated method signatures to accept `userName` and `thread` parameters

**Before:**
```kotlin
suspend fun sendTextMessage(
    senderId: String,
    chatSessionId: String,
    text: String
): NetworkResult<ChatMessage>
```

**After:**
```kotlin
suspend fun sendTextMessage(
    senderId: String,
    userName: String,
    chatSessionId: String,
    text: String,
    thread: ChatThread
): NetworkResult<ChatMessage>
```

---

### 9. ✅ Updated Database Schema

**Files:**
- `app/src/main/java/com/tomasronis/rhentiapp/core/database/entities/CachedThread.kt`
- `app/src/main/java/com/tomasronis/rhentiapp/core/database/RhentiDatabase.kt`
- `app/src/main/java/com/tomasronis/rhentiapp/core/database/Converters.kt` (NEW)

**Changes:**
- Added `members: Map<String, Int>?` field to `CachedThread` entity
- Created `Converters.kt` with type converters for `Map<String, Int>`
- Updated `RhentiDatabase` to use converters and bumped version from 1 to 2
- Updated conversion functions to include members field

**New File:** `Converters.kt`
```kotlin
class Converters {
    private val moshi = Moshi.Builder().build()
    private val mapAdapter = moshi.adapter<Map<String, Int>>(...)

    @TypeConverter
    fun fromStringIntMap(value: Map<String, Int>?): String?

    @TypeConverter
    fun toStringIntMap(value: String?): Map<String, Int>?
}
```

**Database Version:**
- Changed from version 1 to version 2
- Room will automatically recreate the database on first run

---

## Testing Checklist

After deploying these changes, verify:

- [x] App compiles successfully
- [ ] Threads fetch without errors (response uses `chatThreads` key)
- [ ] Thread `members` object is populated from API
- [ ] Text messages send successfully with correct format
- [ ] `chatSessionMembersObj` contains actual user IDs (not "renter"/"owner")
- [ ] User name appears in message `user` object
- [ ] Image messages send with `base64` field and data URI prefix
- [ ] No HTTP 500 errors from malformed requests
- [ ] Messages appear correctly in thread detail view
- [ ] Database migration works (or clear app data for clean install)

---

## API Format Reference

### ✅ Correct Message Send Format

```json
POST /message/{senderId}

{
  "message": {
    "_id": 1738468800000,
    "createdAt": 1738468800000,
    "text": "Hello!",
    "user": {
      "name": "John Doe",
      "_id": "69571ab7aedcd7b711e1d43f"
    }
  },
  "chatSessionId": "-OhwFp1_9mjyO57ZsF0w",
  "chatSessionMembersObj": {
    "69571ab7aedcd7b711e1d43f": 0,
    "other_user_id_here": 5
  }
}
```

### ✅ Correct Image Send Format

```json
POST /message/{senderId}

{
  "message": {
    "_id": 1738468800000,
    "createdAt": 1738468800000,
    "base64": "data:image/jpeg;base64,/9j/4AAQSkZJRg...",
    "user": {
      "name": "John Doe",
      "_id": "69571ab7aedcd7b711e1d43f"
    }
  },
  "chatSessionId": "-OhwFp1_9mjyO57ZsF0w",
  "chatSessionMembersObj": {
    "69571ab7aedcd7b711e1d43f": 0,
    "other_user_id_here": 5
  }
}
```

---

## Database Migration Note

⚠️ **IMPORTANT:** The database schema has changed (version 1 → 2).

When users update the app:
- Room will automatically drop and recreate the database
- All cached data (threads, messages, contacts) will be cleared
- Data will be re-fetched from the API on first use

For development/testing:
- Clear app data manually if needed: Settings → Apps → Rhenti → Clear Data
- Or uninstall and reinstall the app

---

## iOS Reference Files

These fixes were based on:
- `iOS Code Base to Use as Example Only/docs/CHAT_API_DOCUMENTATION.md`
- `iOS Code Base to Use as Example Only/Core/Services/ChatService.swift`
- `iOS Code Base to Use as Example Only/Features/ChatHub/Models/ChatModels.swift`

---

## Summary

All critical issues identified in the comparison document have been fixed:

1. ✅ Thread response now parses `chatThreads` instead of `threads`
2. ✅ `members` field is now parsed and stored
3. ✅ Message format uses correct nested structure with iOS format
4. ✅ `chatSessionMembersObj` now uses actual user IDs from thread
5. ✅ User info (name and _id) is included in all messages
6. ✅ Image messages use `base64` field with proper data URI prefix
7. ✅ All keys use camelCase matching iOS implementation
8. ✅ User's full name is stored and accessible for message sending

The Android implementation now matches the iOS implementation and should work correctly with the production API.
