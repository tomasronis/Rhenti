# Debugging Notification and Call Issues

**Date:** February 12, 2026

This document provides step-by-step debugging instructions to identify why push notifications and incoming calls are not working as expected.

---

## Changes Made

### Fixed Issues:
1. **TwilioManager FCM Token**: Fixed to use `PreferencesManager` instead of incorrect SharedPreferences lookup
2. **Comprehensive Logging**: Added detailed debug logging throughout the notification and call flow
3. **Deep Link Logging**: Added logging to trace the entire deep link parsing and navigation flow

### Files Modified:
- `TwilioManager.kt` - Fixed FCM token retrieval, added detailed logging
- `RhentiNotificationManager.kt` - Added logging for intent creation
- `MainActivity.kt` - Added logging for deep link handling
- `DeepLinkHandler.kt` - Added logging for URI and intent parsing
- `RhentiFirebaseMessagingService.kt` - Added logging for Twilio call handling

---

## Issue 1: Deep Linking Not Working

### What Should Happen:
1. User taps push notification
2. MainActivity receives intent with `rhenti://thread/{id}` URI
3. `DeepLinkHandler.parseIntent()` parses the URI
4. MainActivity routes to the correct screen via `MainTabViewModel`
5. App navigates to the thread and opens it

### Debug Steps:

#### Step 1: Build and Install
```bash
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

#### Step 2: Send Test Notification
From Firebase Console, send a test notification with data payload:
```json
{
  "type": "message",
  "title": "Test Message",
  "body": "Testing deep link",
  "threadId": "123"
}
```

#### Step 3: Check Logcat
Filter logcat for relevant tags:
```bash
adb logcat -s NotificationManager DeepLinkHandler MainActivity
```

**Expected logs when notification is created:**
```
NotificationManager: Creating notification intent with data URI: rhenti://thread/123
NotificationManager: Intent extras: {notification_type=message, thread_id=123}
```

**Expected logs when notification is tapped:**
```
MainActivity: handleDeepLink called:
MainActivity:   action: android.intent.action.VIEW
MainActivity:   data: rhenti://thread/123
MainActivity:   extras: notification_type, thread_id
DeepLinkHandler: parseIntent: action=android.intent.action.VIEW, data=rhenti://thread/123
DeepLinkHandler: parseIntent: Trying URI parsing first
DeepLinkHandler: parseUri: scheme=rhenti, host=thread, path=/123
DeepLinkHandler: parseUri: Thread destination, threadId=123
DeepLinkHandler: parseIntent: ✅ Successfully parsed from URI: Thread(threadId=123)
MainActivity: ✅ Deep link destination parsed: Thread(threadId=123)
MainActivity: ✅ Applying deep link navigation: Thread(threadId=123)
MainActivity:   → Opening thread: 123
```

#### Step 4: Identify the Problem

**If you see "Intent is null":**
- The notification intent is not being created properly
- Check `RhentiNotificationManager.showNotification()`

**If you see "Failed to parse deep link destination":**
- The URI or extras are not in the expected format
- Check the intent data and extras in the logs

**If you see "MainTabViewModel not available yet":**
- The app is not authenticated or MainTabScreen hasn't composed yet
- This is normal on cold start - pending deep link should be applied when VM is ready
- Check for "MainTabViewModel now available, applying pending deep link" later in logs

**If nothing happens after "Applying deep link navigation":**
- The navigation is being triggered but not working
- Check `MainTabViewModel.setThreadIdToOpen()` and `ChatsTabContent`

---

## Issue 2: No Incoming Call Notification

### What Should Happen:
1. Backend initiates call via Twilio
2. Twilio sends push notification via FCM with `twi_message_type` field
3. `RhentiFirebaseMessagingService` detects Twilio message
4. Calls `Voice.handleMessage()` to process call invite
5. Twilio SDK broadcasts `com.twilio.voice.INCOMING` action
6. `IncomingCallReceiver` receives broadcast
7. Shows notification with Answer/Decline buttons

### Debug Steps:

#### Step 1: Verify Twilio Registration
Check logcat for Twilio registration:
```bash
adb logcat -s TwilioManager
```

**Expected logs on app startup:**
```
TwilioManager: Initializing Twilio SDK - userId: xxx, email: xxx
TwilioManager: Twilio SDK initialized successfully
TwilioManager: Registering for incoming call invites with FCM token: xxx...
TwilioManager: ✅ Registered for incoming calls successfully
```

**If you see "FCM token not available yet":**
- The FCM token hasn't been fetched yet
- This is a timing issue - Twilio registration happens before FCM initialization completes
- **Workaround:** Restart the app (token will be available on second launch)
- **Proper fix:** Add retry logic or delay Twilio initialization

#### Step 2: Verify FCM Token is Saved
Check if FCM token is properly saved:
```bash
adb logcat -s FcmTokenManager
```

**Expected logs:**
```
FcmTokenManager: FCM Token fetched: xxx...
FcmTokenManager: FCM Token saved (token changed)
FcmTokenManager: User authenticated, syncing token with backend...
FcmTokenManager: ✅ Device registered successfully with backend
```

#### Step 3: Trigger an Incoming Call
Have someone call you or trigger a test call from Twilio Console.

**Monitor FCM service:**
```bash
adb logcat -s FCMService
```

**Expected logs when call comes in:**
```
FCMService: Message received from: xxx
FCMService: Message data: {twi_message_type=call_invite, ...}
FCMService: Received Twilio Voice call invite
FCMService: 🔔 Handling Twilio Voice call invite
FCMService:   Message data keys: twi_message_type, twi_call_sid, ...
FCMService: ✅ Call invite received from: +1234567890
FCMService:   Call SID: CAxxxx
FCMService: ✅ Twilio message was valid and handled
```

**Monitor IncomingCallReceiver:**
```bash
adb logcat -s IncomingCallReceiver
```

**Expected logs:**
```
IncomingCallReceiver: Incoming call from: +1234567890
IncomingCallReceiver: Showing incoming call notification for: +1234567890
IncomingCallReceiver: Incoming call notification displayed
```

#### Step 4: Identify the Problem

**If you don't see ANY FCM message:**
- The push notification is not reaching the device
- Check Firebase Console to verify notification was sent
- Check device internet connection
- Verify app is registered with correct FCM token in backend

**If you see "Failed to register for incoming calls" with error:**
- The Twilio registration failed
- Check the error message for details
- Possible causes: Invalid access token, FCM token issues, network error

**If you see FCM message but no "Twilio Voice call invite":**
- The message doesn't have `twi_message_type` field
- This is not a Twilio Voice notification
- Check with backend team to verify call notification format

**If you see "Call invite received" but no notification:**
- The IncomingCallReceiver is not receiving the broadcast
- Check if receiver is registered in AndroidManifest (it should be now)
- Check device notification permissions

**If notification shows but has wrong info:**
- Check IncomingCallReceiver notification building logic
- Verify call invite data (from, to, etc.)

---

## Additional Debugging Commands

### View All Notification-Related Logs:
```bash
adb logcat -s NotificationManager DeepLinkHandler MainActivity TwilioManager FCMService IncomingCallReceiver FcmTokenManager
```

### Clear App Data and Restart:
```bash
adb shell pm clear com.tomasronis.rhentiapp
adb shell am start -n com.tomasronis.rhentiapp/.presentation.MainActivity
```

### Check Notification Channels:
```bash
adb shell cmd notification list_channels com.tomasronis.rhentiapp
```

### Test Deep Link Manually (without notification):
```bash
adb shell am start -W -a android.intent.action.VIEW -d "rhenti://thread/123" com.tomasronis.rhentiapp
```

---

## Common Issues and Solutions

### Issue: "FCM token not available yet" during Twilio registration
**Cause:** TwilioManager initializes before FCM token is fetched
**Solution:** Restart app (token will be available on next launch)
**Proper Fix:** Add retry logic or delay initialization

### Issue: Deep link works from ADB but not from notification
**Cause:** Notification intent might not have proper flags or data
**Solution:** Check notification intent creation logs

### Issue: App navigates to wrong screen from notification
**Cause:** Deep link destination mapping is incorrect
**Solution:** Check DeepLinkHandler parsing logic for your notification type

### Issue: Notification permission denied
**Cause:** User denied POST_NOTIFICATIONS permission (Android 13+)
**Solution:** Go to Settings > Apps > Rhenti > Notifications > Enable

### Issue: IncomingCallReceiver not receiving broadcasts
**Cause:** Receiver not registered or exported incorrectly
**Solution:** Verify AndroidManifest has receiver with correct intent filters

---

## Testing Checklist

### Push Notification Deep Linking:
- [ ] Send test notification from Firebase Console
- [ ] App opens from killed state
- [ ] App opens from background state
- [ ] App is in foreground
- [ ] Navigates to correct thread/contact/call
- [ ] Works after login
- [ ] Works when already logged in

### Incoming Call Notification:
- [ ] Twilio registers successfully (check logs)
- [ ] FCM token is available when registering
- [ ] Incoming call triggers FCM notification
- [ ] IncomingCallReceiver receives broadcast
- [ ] Notification appears with Answer/Decline
- [ ] Answer button opens app and starts call
- [ ] Decline button rejects call and dismisses notification
- [ ] Notification shows caller name/number correctly

---

## Next Steps

After reviewing the logs:

1. **If deep linking logs show success but navigation fails:**
   - Check `MainTabViewModel` and `ChatsTabContent` navigation logic
   - Verify `threadIdToOpen` state is being observed

2. **If Twilio registration fails:**
   - Add retry logic after FCM token becomes available
   - Consider moving Twilio initialization to after successful FCM registration

3. **If IncomingCallReceiver doesn't receive broadcast:**
   - Verify Twilio SDK is actually broadcasting
   - Check if receiver needs to be exported=true
   - Consider showing notification directly from FCM service instead

4. **If everything logs correctly but still doesn't work:**
   - Share the full logcat output for analysis
   - Check for any system-level notification blocking

---

## Contact for Support

If issues persist after following this guide, please provide:
1. Full logcat output from app launch through notification tap
2. Android version and device model
3. Whether user is authenticated when testing
4. Screenshots of any error messages
