# Message Display Fixes

## Issues Fixed

### 1. ✅ Message Order - Newest at Bottom
**Problem:** Newest messages were appearing at the top instead of the bottom.

**Root Cause:** The API returns messages **newest-first**, but we weren't reversing them.

**Solution:** Updated `ChatHubRepositoryImpl.parseMessagesResponse()` to reverse messages:
```kotlin
// API returns messages newest-first, reverse to oldest-first (matches iOS)
return messages.reversed()
```

**Result:** Messages now display in chronological order with:
- **Oldest messages at top** (can scroll up to see older)
- **Newest messages at bottom** (new messages appear here)
- Matches iOS behavior exactly

---

### 2. ✅ Timestamp Format - iOS Parity
**Problem:** Timestamps weren't showing the correct "MMM DD, Time" format.

**Root Cause:** Timestamp formatting logic didn't match iOS implementation.

**Solution:** Updated `MessageBubble.formatTimestamp()` to match iOS exactly:

#### Format Rules (matches iOS):
1. **Today** → Time only
   - Example: `"2:30 PM"`

2. **Yesterday** → "Yesterday" + Time
   - Example: `"Yesterday 2:30 PM"`

3. **This Week** → Day of week + Time
   - Example: `"Monday 2:30 PM"`

4. **This Year** → Month/Day + Time
   - Example: `"Jan 1, 2:30 PM"`

5. **Older** → Short date + Time
   - Example: `"12/25/24, 2:30 PM"`

#### Implementation:
```kotlin
private fun formatTimestamp(timestamp: Long): String {
    val calendar = Calendar.getInstance()
    val now = Calendar.getInstance()
    val messageDate = Calendar.getInstance().apply {
        timeInMillis = timestamp
    }

    return when {
        isToday(messageDate, now) -> timeFormat.format(Date(timestamp))
        isYesterday(messageDate, now) -> "Yesterday ${timeFormat.format(Date(timestamp))}"
        isThisWeek(messageDate, now) -> dayOfWeekTimeFormat.format(Date(timestamp))
        isSameYear(messageDate, now) -> monthDayTimeFormat.format(Date(timestamp))
        else -> shortDateTimeFormat.format(Date(timestamp))
    }
}
```

#### Helper Functions:
- `isToday()` - Checks if message is from today
- `isYesterday()` - Checks if message is from yesterday
- `isThisWeek()` - Checks if message is within last 7 days
- `isSameYear()` - Checks if message is from current year

---

## Files Modified

### 1. `MessageBubble.kt`
**Location:** `app/src/main/java/com/tomasronis/rhentiapp/presentation/main/chathub/components/`

**Changes:**
- Completely rewrote `formatTimestamp()` function
- Added helper functions for date comparison
- Now matches iOS `DateFormatters.formatMessageTimestamp()` exactly

### 2. `ChatHubRepositoryImpl.kt`
**Location:** `app/src/main/java/com/tomasronis/rhentiapp/data/chathub/repository/`

**Changes:**
- Added `.reversed()` to `parseMessagesResponse()` return statement
- Added comment explaining API behavior

---

## Behavior Verification

### Message Order
✅ Initial load: Newest message at bottom, auto-scrolls to bottom
✅ Send message: New message appears at bottom, auto-scrolls down
✅ Pagination: Older messages load at top, scroll position maintained
✅ Pending message: Appears at bottom while sending

### Timestamps
✅ Today's messages: Show time only
✅ Yesterday's messages: Show "Yesterday 2:30 PM"
✅ This week: Show "Monday 2:30 PM"
✅ This year: Show "Jan 1, 2:30 PM"
✅ Older: Show "12/25/24, 2:30 PM"

---

## iOS Parity Achieved

| Feature | iOS | Android | Status |
|---------|-----|---------|--------|
| Message order (oldest→newest) | ✅ | ✅ | ✅ Match |
| API response reversed | ✅ | ✅ | ✅ Match |
| Today timestamp format | Time only | Time only | ✅ Match |
| Yesterday format | "Yesterday Time" | "Yesterday Time" | ✅ Match |
| This week format | "Day Time" | "Day Time" | ✅ Match |
| This year format | "MMM D, Time" | "MMM D, Time" | ✅ Match |
| Older format | "M/D/YY, Time" | "M/D/YY, Time" | ✅ Match |
| Auto-scroll to newest | ✅ | ✅ | ✅ Match |

---

## Technical Details

### API Message Order
The Rhenti API returns messages in **newest-first** order:
```
[
  { id: "3", createdAt: 1000000, text: "Newest" },    // Index 0
  { id: "2", createdAt: 999999,  text: "Middle" },    // Index 1
  { id: "1", createdAt: 999998,  text: "Oldest" }     // Index 2
]
```

### After Reversal
Messages are reversed to **oldest-first** for display:
```
[
  { id: "1", createdAt: 999998,  text: "Oldest" },    // Index 0 (top)
  { id: "2", createdAt: 999999,  text: "Middle" },    // Index 1
  { id: "3", createdAt: 1000000, text: "Newest" }     // Index 2 (bottom)
]
```

### LazyColumn Display
With `reverseLayout = false`:
- Index 0 → Top of screen (oldest message)
- Index N → Bottom of screen (newest message)
- Scroll up → Load more old messages (pagination)
- Scroll down → See newest messages
- Auto-scroll → `scrollToItem(size - 1)` goes to newest

### Pagination Logic
When loading older messages:
```kotlin
// Older messages (already reversed by repository)
val olderMessages = repository.getMessages(threadId, beforeId)

// Prepend to existing messages (maintains chronological order)
messages = olderMessages + state.messages
```

Result:
```
Before: [msg3, msg4, msg5]          // Existing messages
Older:  [msg1, msg2]                // Fetched older messages
After:  [msg1, msg2, msg3, msg4, msg5] // Combined (oldest first)
```

---

## Testing Checklist

- [x] Newest message appears at bottom on initial load
- [x] Auto-scroll to bottom on first load
- [x] Send new message → appears at bottom
- [x] Auto-scroll to bottom when sending
- [x] Scroll to top → load older messages
- [x] Older messages appear at top, scroll position maintained
- [x] Today's message shows time only
- [x] Yesterday's message shows "Yesterday Time"
- [x] This week shows day + time
- [x] This year shows "MMM D, Time"
- [x] Old messages show full date + time
- [x] Pending message appears at bottom
- [x] Failed message shows retry button

---

## Related Documentation

- iOS reference: `Core/Extensions/DateFormatters.swift`
- iOS ViewModel: `Features/ChatHub/ViewModels/ChatViewModel.swift`
- Original implementation: `MESSAGE_LOADING_IOS_PARITY.md`

---

**Last Updated:** February 8, 2026
**Status:** ✅ Complete - Messages display correctly with proper ordering and timestamps
