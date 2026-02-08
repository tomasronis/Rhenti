# iOS Message Loading Logic - Android Implementation

## Overview
Updated the Android app's message loading and display logic to match the iOS implementation exactly. This provides better message handling, optimistic updates, and smoother user experience.

## Key Changes

### 1. New Models Added (`ChatHubModels.kt`)

#### `PendingMessage` Data Class
```kotlin
data class PendingMessage(
    val localId: String,
    val text: String?,
    val imageData: String?,
    val createdAt: Long,
    val status: MessageStatus,
    val serverMessageId: String? = null,
    val uploadProgress: Float? = null
)
```
- Tracks messages being sent to the server
- Maintains local state until server confirms
- Supports progress tracking for large uploads

#### `MessageStatus` Enum
```kotlin
enum class MessageStatus {
    SENDING,
    SENT,
    FAILED
}
```
- Clear status tracking for pending messages

#### `DisplayMessage` Sealed Class
```kotlin
sealed class DisplayMessage {
    data class Server(val message: ChatMessage) : DisplayMessage()
    data class Pending(val pendingMessage: PendingMessage) : DisplayMessage()
}
```
- Type-safe combination of server and pending messages
- Used for UI display

### 2. Updated `ChatHubUiState`

**New Fields:**
- `pendingMessages: List<PendingMessage>` - Tracks messages being sent
- `isLoadingMore: Boolean` - Separate flag for pagination (doesn't conflict with initial load)
- `hasMoreMessages: Boolean` - Tracks if more messages available for pagination

**New Computed Property:**
- `displayMessages: List<DisplayMessage>` - Combines server + pending messages
  - Matches iOS `displayMessages` implementation
  - Filters out pending messages once server confirms
  - Sorted chronologically

### 3. Updated `ChatHubViewModel`

#### Message Loading (Matches iOS `loadMessages()`)
- **Initial Load**: Directly updates messages in state
- **Pagination**: Uses separate `isLoadingMore` flag
- **Incremental Refresh**: Merges new messages without replacing entire list (no flicker)

#### Pending Message Management (Matches iOS)
```kotlin
fun sendTextMessage(text: String) {
    // 1. Create pending message
    // 2. Add to pendingMessages list immediately
    // 3. Send to API
    // 4. Mark as SENT with serverMessageId
    // 5. Refresh incrementally to get server copy
}
```

#### Smart Cleanup (Matches iOS `cleanupPendingMessages()`)
```kotlin
private fun cleanupPendingMessages() {
    // 1. Check exact ID match (serverMessageId)
    // 2. Fallback: Fuzzy match (content + time + sender)
    // 3. Remove pending messages once confirmed on server
}
```

#### Incremental Refresh (Matches iOS `refreshMessagesIncrementally()`)
```kotlin
private fun refreshMessagesIncrementally() {
    // 1. Fetch latest messages
    // 2. Filter out messages we already have
    // 3. Insert new messages at correct chronological position
    // 4. No UI flicker - just adds new messages
    // 5. Silent failure (doesn't show errors)
}
```

#### Real-time Updates (Matches iOS `handleIncomingMessage()`)
```kotlin
fun handleIncomingMessage(message: ChatMessage): Boolean {
    // 1. Check if message is for current thread
    // 2. Check for duplicates
    // 3. Insert at correct chronological position
    // 4. Clean up matching pending messages
    // Future: Hook up to WebSocket/FCM
}
```

### 4. Updated `ThreadDetailScreen`

**Changes:**
- Uses `uiState.displayMessages` instead of `uiState.messages`
- Updated MessageList to handle `DisplayMessage` sealed class
- Separate loading indicators for initial load vs pagination
- Passes `hasMoreMessages` to prevent unnecessary pagination calls

**Message List Updates:**
- Handles both `DisplayMessage.Server` and `DisplayMessage.Pending`
- Converts pending messages to ChatMessage for display
- Retry button uses local ID for pending messages
- Only triggers pagination when `hasMoreMessages` is true

## iOS Parity Achieved

### ✅ Implemented iOS Features:

1. **Separate Pending Messages**
   - iOS: `private(set) var pendingMessages: [PendingMessage] = []`
   - Android: `val pendingMessages: List<PendingMessage> = emptyList()`

2. **Display Messages**
   - iOS: `var displayMessages: [DisplayMessage]`
   - Android: `val displayMessages: List<DisplayMessage>`

3. **Smart Cleanup**
   - iOS: `cleanupPendingMessages()` with exact + fuzzy matching
   - Android: Same logic implemented

4. **Incremental Refresh**
   - iOS: `refreshMessagesIncrementally()` merges without replacing
   - Android: Same behavior

5. **Pagination Flags**
   - iOS: `isLoadingMore` separate from `isLoading`, `hasMoreMessages` flag
   - Android: Same flags implemented

6. **Message Status Tracking**
   - iOS: `MessageStatus` enum (sending/sent/failed)
   - Android: Same enum

7. **Optimistic Updates**
   - iOS: Shows pending messages immediately, updates when confirmed
   - Android: Same behavior

8. **Real-time Support**
   - iOS: `handleIncomingMessage()` for socket updates
   - Android: Same method ready for WebSocket/FCM

## Benefits

### 1. Better User Experience
- Messages appear instantly when sent (optimistic updates)
- No flicker when new messages arrive (incremental refresh)
- Clear status indicators (sending/sent/failed)
- Smooth pagination without jumpiness

### 2. Improved Reliability
- Duplicate message prevention (smart cleanup)
- Failed message retry (tracked by local ID)
- Offline support (pending messages cached)

### 3. Consistent Behavior
- Matches iOS app exactly
- Same data flow and state management
- Easier to maintain both platforms

### 4. Future-Ready
- Real-time message support prepared
- Upload progress tracking ready
- Extensible for new message types

## Testing Checklist

- [ ] Send text message - appears instantly
- [ ] Send image message - shows immediately
- [ ] Failed message - shows retry button
- [ ] Retry failed message - creates new pending message
- [ ] Pagination - loads older messages at top
- [ ] Pagination - doesn't trigger when hasMoreMessages is false
- [ ] New message - auto-scrolls to bottom
- [ ] Pagination - doesn't auto-scroll when loading older messages
- [ ] Multiple messages sent - all tracked separately
- [ ] Message confirmed - pending message removed seamlessly
- [ ] Booking actions - incremental refresh (no flicker)

## Code Structure

### Before (Old Approach)
```
ViewModel:
- messages: List<ChatMessage> (observed from Room)
- Single isLoading flag
- Replace temp message with server message

UI:
- Display messages directly
- Simple retry logic
```

### After (iOS-Style Approach)
```
ViewModel:
- messages: List<ChatMessage> (server messages)
- pendingMessages: List<PendingMessage> (local messages)
- displayMessages: Computed (combines both)
- isLoading + isLoadingMore flags
- hasMoreMessages flag
- Smart cleanup logic
- Incremental refresh

UI:
- Display displayMessages
- Handle Server + Pending message types
- Retry by local ID
```

## Migration Notes

### Breaking Changes
- `retryMessage(ChatMessage)` deprecated, use `retryPendingMessage(String)`
- MessageList now expects `List<DisplayMessage>` instead of `List<ChatMessage>`

### Backwards Compatibility
- Old `retryMessage` still works (marked deprecated)
- Room observation still active (for offline support)
- API remains unchanged

## Performance Considerations

1. **Memory**: Minimal impact - pending messages are temporary
2. **Computation**: `displayMessages` is computed on access (very fast)
3. **Database**: Still caches messages in Room for offline support
4. **Network**: Incremental refresh reduces redundant API calls

## Future Enhancements

1. **WebSocket Integration**: `handleIncomingMessage()` ready to use
2. **Upload Progress**: `uploadProgress` field ready for large files
3. **Message Reactions**: Can extend `DisplayMessage` sealed class
4. **Read Receipts**: Can add to message metadata
5. **Thread Updates**: Can apply same pattern to thread list

## Related Files

- `ChatHubModels.kt` - Data models and sealed classes
- `ChatHubViewModel.kt` - Message loading and state management
- `ThreadDetailScreen.kt` - UI rendering
- `ChatHubRepository.kt` - API calls and caching

## iOS Reference

Source files from iOS implementation:
- `Features/ChatHub/ViewModels/ChatViewModel.swift`
- `Features/ChatHub/Views/ChatView.swift`
- `Features/ChatHub/Models/ChatModels.swift`

---

**Last Updated:** February 8, 2026
**Status:** ✅ Complete - Ready for Testing
