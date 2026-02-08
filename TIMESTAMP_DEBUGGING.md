# Timestamp Debugging Guide

## Issue
All messages showing today's date instead of actual send date.

## Possible Causes
1. API not returning timestamp field
2. Field name mismatch (createdAt vs created_at vs something else)
3. Timestamp format issue (seconds vs milliseconds)
4. Field type mismatch (String vs Number)

## Debug Logging Added

### What to Look For in Logcat

Run the app and filter logcat for `ChatHubRepository`. You should see:

```
ChatHubRepository: Message data keys: [_id, sender, text, type, createdAt, ...]
ChatHubRepository:   _id: abc123
ChatHubRepository:   createdAt: 1738900000000
ChatHubRepository:   created_at: null
ChatHubRepository:   Final timestamp: 1738900000000 (Mon Feb 08 12:00:00 PST 2026)
```

### Scenarios

#### ✅ Good - Timestamp Found
```
Message data keys: [..., createdAt, ...]
  createdAt: 1738900000000
  Final timestamp: 1738900000000 (Mon Feb 08 12:00:00 PST 2026)
```

#### ❌ Bad - No Timestamp (fallback to current time)
```
Message data keys: [_id, sender, text, type]  // No createdAt!
  createdAt: null
  created_at: null
⚠️ No timestamp found for message abc123, using current time
  Final timestamp: 1739876543210 (Sat Feb 08 15:30:43 PST 2026)  // Today!
```

#### ⚠️ Wrong Format - Timestamp in Seconds
```
Message data keys: [..., createdAt, ...]
  createdAt: 1738900000  // Only 10 digits!
Converting timestamp from seconds to milliseconds: 1738900000 -> 1738900000000
  Final timestamp: 1738900000000
```

#### 🔍 Wrong Field Name
```
Message data keys: [_id, sender, text, type, timestamp, date, ...]  // Different field!
  createdAt: null
  created_at: null
⚠️ No timestamp found for message abc123, using current time
```

## How to Check Logs

### Android Studio
1. Open Logcat panel (bottom of screen)
2. Select your device/emulator
3. Filter by "ChatHubRepository"
4. Open a message thread
5. Look for the debug messages

### Command Line (if adb is set up)
```bash
adb logcat | grep ChatHubRepository
```

## What to Report

If timestamps are wrong, please share:

1. **The keys in the message data:**
   ```
   Message data keys: [...]
   ```

2. **The createdAt values:**
   ```
   createdAt: <value>
   created_at: <value>
   ```

3. **The final timestamp:**
   ```
   Final timestamp: <value> (<date>)
   ```

4. **Whether you see the warning:**
   ```
   ⚠️ No timestamp found for message...
   ```

## Possible Fixes

### If timestamp field has a different name
We need to add that field name to the code. Example:
```kotlin
val createdAtRaw = when {
    messageData.containsKey("createdAt") -> (messageData["createdAt"] as? Number)?.toLong()
    messageData.containsKey("created_at") -> (messageData["created_at"] as? Number)?.toLong()
    messageData.containsKey("timestamp") -> (messageData["timestamp"] as? Number)?.toLong()  // NEW
    messageData.containsKey("date") -> (messageData["date"] as? Number)?.toLong()  // NEW
    else -> null
}
```

### If timestamp is a String (ISO 8601)
Need to parse it:
```kotlin
val createdAtRaw = when (val value = messageData["createdAt"]) {
    is Number -> value.toLong()
    is String -> {
        // Parse ISO 8601: "2026-02-08T12:00:00Z"
        val date = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(value)
        date?.time
    }
    else -> null
}
```

### If timestamp is in a nested object
Need to navigate to it:
```kotlin
val metadata = messageData["metadata"] as? Map<String, Any>
val createdAtRaw = (metadata?.get("createdAt") as? Number)?.toLong()
```

## Testing

To verify timestamps are working:

1. Send a new message
2. Check logcat for that message's timestamp
3. Should show current time (not fallback)
4. Should display correctly in UI

## Next Steps

Once we identify the issue from the logs, we can:
1. Update the field name if different
2. Add proper parsing if format is different
3. Handle the correct data structure

---

**Note:** The detailed logging will only appear in DEBUG builds, not RELEASE builds.
