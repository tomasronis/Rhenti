# Debug Checklist for Chat Issues

## Step 1: Verify API Calls are Happening

Run the app with Android Studio's Logcat filtered for "ChatHub" to see:

```
ChatHubRepository: Fetching threads with request: {...}
ChatHubRepository: Threads response: {...}
ChatHubRepository: Parsed X threads
ChatHubRepository: Messages response: {...}
```

## Step 2: Check What APIs Are Being Called

Look for these log patterns:
- ✅ `POST /chat-hub/threads` - Should happen when opening Chat tab
- ✅ `GET /chat-hub/messages/{threadId}` - Should happen when opening a thread
- ❌ `GET /users/{userId}` - **MISSING** - Should be called after login
- ❌ `GET /admin/whitelabel/settings` - **MISSING** - Optional but good to have

## Step 3: Verify Current Implementation

### Check if user profile is being fetched:
1. After login, should call `GET /users/{userId}` to get full user profile
2. Should store firstName and lastName in TokenManager
3. Should use full name when sending messages

### Check if threads are being loaded:
1. Open ChatHub screen
2. Look for "Fetching threads" log
3. Look for "Parsed X threads" log
4. If threads = 0, check API response format

### Check if messages are being loaded:
1. Tap on a thread
2. Look for "Messages response" log
3. Check if messages array is empty
4. Verify message parsing logic

## Step 4: Add Missing API Calls

### Priority 1: Fetch User Profile After Login

**File:** `AuthRepositoryImpl.kt`

Add after successful login:
```kotlin
// After saving auth data, fetch full user profile
private suspend fun fetchAndCacheUserProfile(userId: String) {
    try {
        val userProfile = apiClient.getUserProfile(userId)
        // Parse and cache user profile
        val user = parseUserProfile(userProfile)
        userDao.insertUser(user.toCachedUser())
    } catch (e: Exception) {
        // Log but don't fail login
        if (BuildConfig.DEBUG) {
            Log.w("AuthRepository", "Failed to fetch user profile", e)
        }
    }
}
```

### Priority 2: Fetch Settings After Login

**File:** `AuthRepositoryImpl.kt`

Add optional settings fetch:
```kotlin
private suspend fun fetchWhiteLabelSettings() {
    try {
        val settings = apiClient.getWhiteLabelSettings()
        // Store settings for UI customization
        preferencesManager.saveSettings(settings)
    } catch (e: Exception) {
        // Not critical - use defaults
        if (BuildConfig.DEBUG) {
            Log.w("AuthRepository", "Failed to fetch settings", e)
        }
    }
}
```

## Step 5: Verify API Response Parsing

Check that response keys match what the API actually returns:
- ✅ `chatThreads` (not `threads`)
- ✅ `members` field is being parsed
- ✅ `messages` array is being parsed

## Common Issues and Fixes

### Issue: "Threads list is empty"
**Cause:** Response parsing looking for wrong key
**Fix:** Use `response["chatThreads"]` not `response["threads"]`

### Issue: "Messages not showing"
**Cause:** Message parsing failure or empty messages array
**Fix:** Check `response["messages"]` parsing and verify message type handling

### Issue: "Can't send messages - HTTP 500"
**Cause:** Missing or invalid `chatSessionMembersObj`
**Fix:** Ensure `thread.members` is populated and passed correctly

### Issue: "User name not showing in messages"
**Cause:** User profile not fetched
**Fix:** Call `GET /users/{userId}` after login and store firstName/lastName

## Quick Test

Run this sequence:
1. Clear app data
2. Login
3. Check logcat for these calls in order:
   - `POST /login` → Success
   - `GET /users/{userId}` → **SHOULD BE HERE (currently missing)**
   - `POST /chat-hub/threads` → Should return threads
   - Tap a thread
   - `GET /chat-hub/messages/{threadId}` → Should return messages

If you don't see user profile call, that's the first thing to fix.
