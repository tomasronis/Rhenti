# Android vs iOS Chat API Implementation Comparison

## Date: February 2, 2026
## Status: **CRITICAL ISSUES FOUND** ⚠️

---

## Executive Summary

After comparing the Android implementation with the iOS reference files and official API documentation, **several critical discrepancies** were found that will cause message sending and thread fetching to fail.

### Critical Issues:
1. ❌ **Wrong message send format** - Android uses flat snake_case structure, iOS uses nested camelCase
2. ❌ **Missing `members` object** - Android doesn't parse or use the actual thread members
3. ❌ **Hardcoded member IDs** - Android sends `{"renter": 0, "owner": 0}` instead of actual user IDs
4. ❌ **Wrong response parsing** - Android looks for `threads`, API returns `chatThreads`
5. ❌ **Missing user info** - Android doesn't include `user` object with `name` and `_id`

---

## 1. Fetching Chat Threads

### Endpoint: `POST /chat-hub/threads`

#### ✅ Request Format (Both Match)
```json
{
  "searchText": "",
  "skip": 0,
  "limit": 20,
  "hasUnread": null,
  "applicationStatus": null,
  "bookingStatus": null,
  "noActivity": null
}
```

Android implementation:
```kotlin
val request = mapOf(
    "super_account_id" to superAccountId,  // ⚠️ Not in iOS docs
    "limit" to 50,
    "offset" to 0  // ⚠️ iOS uses "skip", not "offset"
)
```

**Issue:** Android uses `offset` instead of `skip`, and includes extra `super_account_id` field.

#### ❌ Response Parsing (MISMATCH)

**iOS Documentation Says:**
```json
{
  "chatThreads": [...],  // <-- "chatThreads" key
  "pagination": {
    "total": 42,
    "skip": 0,
    "limit": 20
  }
}
```

**Android Implementation:**
```kotlin
val threadsData = response["threads"] as? List<Map<String, Any>> ?: emptyList()
// ❌ Looking for "threads" but API returns "chatThreads"
```

**FIX REQUIRED:** Change `response["threads"]` to `response["chatThreads"]`

---

## 2. Thread Model - `members` Field

### iOS Thread Model Includes:
```swift
struct ChatThread {
    let members: [String: Int]? // Member IDs mapped to badge counts
    // Example: {"69571ab7aedcd7b711e1d43f": 0, "owner_user_id_here": 5}

    var membersObject: [String: Int] {
        if let members = members {
            return members  // Use actual member IDs from API
        }
        // Fallback to constructing from renter/owner
        var result: [String: Int] = [:]
        if let renter = renter { result[renter] = 0 }
        if let owner = owner { result[owner] = 0 }
        return result
    }
}
```

### ❌ Android Implementation:
```kotlin
data class ChatThread(
    // ... other fields ...
    // ❌ NO members field!
)

// In ChatHubRepositoryImpl.kt parsing:
// ❌ Doesn't parse "members" from API response

// When sending messages:
"chatSessionMembersObj" to mapOf(
    "renter" to 0,
    "owner" to 0
)
// ❌ WRONG! Uses literal strings "renter" and "owner"
// instead of actual user IDs like "69571ab7aedcd7b711e1d43f"
```

**CRITICAL:** The `members` object must contain **actual user IDs**, not role names!

---

## 3. Sending Messages (MOST CRITICAL)

### Endpoint: `POST /message/{senderId}`

#### ❌ Android Implementation (WRONG)

**Android Request:**
```kotlin
val request = mapOf(
    "chat_session_id" to chatSessionId,       // ❌ snake_case
    "message" to text,                        // ❌ Text as root field
    "type" to "text",                         // ❌ Type as root field
    "chatSessionMembersObj" to mapOf(
        "renter" to 0,                       // ❌ Hardcoded strings
        "owner" to 0
    )
)
```

#### ✅ iOS Implementation (CORRECT)

**iOS Request Structure:**
```swift
{
  "message": {                              // ✅ Nested message object
    "_id": 1735778751929,                   // ✅ Timestamp in ms
    "createdAt": 1735778751929,             // ✅ Same timestamp
    "text": "Hello there!",                 // ✅ Text inside message
    "user": {                               // ✅ User info included
      "name": "Peter Chen",                 // ✅ Sender's full name
      "_id": "69571ab7aedcd7b711e1d43f"     // ✅ Sender's user ID (_id not userId)
    }
  },
  "chatSessionId": "-OhwFp1_9mjyO57ZsF0w",  // ✅ camelCase
  "chatSessionMembersObj": {                // ✅ Actual user IDs
    "69571ab7aedcd7b711e1d43f": 0,
    "owner_user_id_here": 5
  }
}
```

**iOS Code:**
```swift
let request = SendMessageRequest(
    message: MessageContent(
        _id: timestamp,
        createdAt: timestamp,
        text: pendingMessage.text,
        user: UserInfo(
            name: currentUser.fullName,
            _id: userId
        )
    ),
    chatSessionId: thread.chatSessionId,
    chatSessionMembersObj: thread.membersObject  // ✅ Uses actual member IDs from thread
)
```

---

## 4. Sending Image Messages

### ❌ Android Implementation (WRONG)

```kotlin
val request = mapOf(
    "chat_session_id" to chatSessionId,
    "type" to "image",
    "attachment" to imageBase64,              // ❌ Wrong field name
    "chatSessionMembersObj" to mapOf(
        "renter" to 0,
        "owner" to 0
    )
)
```

### ✅ iOS Implementation (CORRECT)

```swift
{
  "message": {
    "_id": 1735778751929,
    "createdAt": 1735778751929,
    "base64": "data:image/jpeg;base64,/9j/...",  // ✅ base64 field with data URI prefix
    "height": 1920,                               // ✅ Image dimensions
    "width": 1080,
    "user": {
      "name": "Peter Chen",
      "_id": "69571ab7aedcd7b711e1d43f"
    }
  },
  "chatSessionId": "-OhwFp1_9mjyO57ZsF0w",
  "chatSessionMembersObj": {
    "69571ab7aedcd7b711e1d43f": 0,
    "owner_user_id_here": 5
  }
}
```

**iOS Code:**
```swift
let base64WithPrefix = "data:image/jpeg;base64,\(imageData.base64EncodedString())"

let request = SendMessageRequest(
    message: MessageContent(
        _id: timestamp,
        createdAt: timestamp,
        text: nil,
        base64: base64WithPrefix,             // ✅ Includes "data:image/jpeg;base64," prefix
        height: imageHeight,
        width: imageWidth,
        user: userInfo
    ),
    chatSessionId: thread.chatSessionId,
    chatSessionMembersObj: thread.membersObject
)
```

---

## 5. API Documentation References

### From iOS `CHAT_API_DOCUMENTATION.md`:

**Common Mistakes Section (Lines 215-262):**

❌ **DO NOT** use an empty object for `chatSessionMembersObj`:
```json
{
  "chatSessionMembersObj": {}  // WRONG! Server will reject with HTTP 500
}
```

✅ **DO** include all thread members with their badge counts:
```json
{
  "chatSessionMembersObj": {
    "current_user_id": 0,
    "other_user_id": 5
  }  // CORRECT! Use thread.membersObject
}
```

❌ **DO NOT** use `userId` in the user object:
```json
{
  "user": {
    "userId": "123"  // WRONG! Use "_id" instead
  }
}
```

✅ **DO** use `_id`:
```json
{
  "user": {
    "_id": "123",
    "name": "John Doe"
  }
}
```

---

## 6. Summary of Required Android Fixes

### Fix #1: Parse `members` from Thread Response
```kotlin
// In parseThreadsResponse():
@Suppress("UNCHECKED_CAST")
val members = threadData["members"] as? Map<String, Int>

ChatThread(
    // ... other fields ...
    members = members  // ✅ Add members field
)
```

### Fix #2: Update ChatThread Model
```kotlin
data class ChatThread(
    // ... existing fields ...
    val members: Map<String, Int>?,  // ✅ Add this field

    // Computed property for sending messages
    val membersObject: Map<String, Int>
        get() = members ?: mapOf()
)
```

### Fix #3: Fix Thread Response Key
```kotlin
// In parseThreadsResponse():
val threadsData = response["chatThreads"] as? List<Map<String, Any>> ?: emptyList()
// ✅ Changed from "threads" to "chatThreads"
```

### Fix #4: Fix Send Message Format
```kotlin
override suspend fun sendTextMessage(
    senderId: String,
    chatSessionId: String,
    text: String,
    userName: String,  // ✅ Need user's full name
    thread: ChatThread  // ✅ Need thread to get membersObject
): NetworkResult<ChatMessage> {
    return try {
        val timestamp = System.currentTimeMillis()

        val request = mapOf(
            "message" to mapOf(  // ✅ Nested message object
                "_id" to timestamp,
                "createdAt" to timestamp,
                "text" to text,
                "user" to mapOf(
                    "name" to userName,
                    "_id" to senderId
                )
            ),
            "chatSessionId" to chatSessionId,  // ✅ camelCase
            "chatSessionMembersObj" to thread.membersObject  // ✅ Actual member IDs
        )

        val response = apiClient.sendMessage(senderId, request)
        // ...
    }
}
```

### Fix #5: Fix Image Message Format
```kotlin
override suspend fun sendImageMessage(
    senderId: String,
    chatSessionId: String,
    imageBase64: String,
    userName: String,
    thread: ChatThread
): NetworkResult<ChatMessage> {
    return try {
        val timestamp = System.currentTimeMillis()

        // ✅ Add data URI prefix
        val base64WithPrefix = "data:image/jpeg;base64,$imageBase64"

        val request = mapOf(
            "message" to mapOf(
                "_id" to timestamp,
                "createdAt" to timestamp,
                "base64" to base64WithPrefix,  // ✅ Not "attachment"
                "user" to mapOf(
                    "name" to userName,
                    "_id" to senderId
                )
            ),
            "chatSessionId" to chatSessionId,
            "chatSessionMembersObj" to thread.membersObject
        )

        val response = apiClient.sendMessage(senderId, request)
        // ...
    }
}
```

---

## 7. Testing Checklist

After implementing fixes, verify:

- [ ] Threads fetch successfully with `chatThreads` key
- [ ] Thread `members` object is parsed and stored
- [ ] Text messages send with correct format (nested `message` object)
- [ ] `chatSessionMembersObj` contains actual user IDs (not "renter"/"owner")
- [ ] User object includes both `name` and `_id` fields
- [ ] Image messages use `base64` field with data URI prefix
- [ ] Image messages include user info
- [ ] No HTTP 500 errors from server due to wrong format

---

## 8. iOS Reference Files

For complete implementation details, refer to:
- `iOS Code Base to Use as Example Only/docs/CHAT_API_DOCUMENTATION.md` (Lines 1-300)
- `iOS Code Base to Use as Example Only/Core/Services/ChatService.swift` (Lines 1-400)
- `iOS Code Base to Use as Example Only/Features/ChatHub/Models/ChatModels.swift` (Lines 530-600)

---

## Conclusion

The Android implementation has **significant format mismatches** with the actual API that the iOS app uses successfully. These need to be fixed immediately for chat functionality to work properly.

**Priority:** 🔴 CRITICAL - Messages likely failing to send correctly
**Impact:** High - Core chat functionality affected
**Effort:** Medium - Requires model updates and request format changes
