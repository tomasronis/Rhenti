# Incoming Call Ringing - Debugging Log

**Date:** February 13, 2026
**Status:** BLOCKED on backend - awaiting Twilio FCM Push Credential configuration

---

## Problem

When calling a virtual number linked to the user, the phone does not ring. The missed call appears in the call logs afterward, meaning the backend knows about the call but cannot push it to the Android device.

## Investigation Summary

### What Works
- Twilio `Voice.register()` succeeds (VoIP Status shows "REGISTERED - Ready for incoming calls")
- FCM token is available and valid
- Outgoing calls work
- Regular FCM notifications (messages, viewings, etc.) work
- Call logs are recorded by the backend

### Root Cause Found
**Twilio is not sending FCM push notifications to the Android device.**

Verified by adding a `Last FCM` diagnostic to Settings > About that writes to SharedPreferences every time `onMessageReceived()` is called. After calling the virtual number, it still shows "No FCM messages received yet" - confirming the FCM push never arrives.

This means the **Twilio account does not have FCM Push Credentials configured** for Android. Without this, Twilio cannot send data-only FCM messages to wake the app for incoming calls.

## Backend Action Required

### Step 1: Get Firebase Server Key
- Firebase Console > Project (rhenti-chat) > Project Settings > Cloud Messaging
- Copy the Server Key (or create FCM v1 service account credentials)

### Step 2: Create Push Credential in Twilio Console
- Twilio Console > Voice > Manage > Push Credentials
- Create new credential, type: FCM
- Paste Firebase Server Key
- Note the Push Credential SID (starts with `CR...`)

### Step 3: Update Access Token Generation
- The backend endpoint `POST /phone-tracking/accessToken` generates Twilio Access Tokens
- When `os=android`, the VoiceGrant must include the FCM Push Credential SID:
  ```javascript
  // Node.js example
  const voiceGrant = new VoiceGrant({
      outgoingApplicationSid: twimlAppSid,
      pushCredentialSid: fcmPushCredentialSid  // <-- ADD THIS FOR ANDROID
  });
  ```
- iOS likely already has `pushCredentialSid` set to an APNs credential

### Step 4: Identity Must Be Device ID
- The `identity` parameter in the access token request is now the Android device ID (`Settings.Secure.ANDROID_ID`), not the user ID
- This matches iOS behavior which uses device unique ID
- Current test device ID: `30ef76b049db90a5`

---

## Code Fixes Applied (Not Yet Committed)

### Fix 1: Forward CallInvite to IncomingCallReceiver
**File:** `core/notifications/RhentiFirebaseMessagingService.kt`

Twilio Voice SDK 6.x does NOT automatically broadcast `com.twilio.voice.INCOMING` intents. The `onCallInvite()` callback was logging but doing nothing. Now it explicitly sends a broadcast to `IncomingCallReceiver`:
```kotlin
override fun onCallInvite(callInvite: CallInvite) {
    val intent = Intent(context, IncomingCallReceiver::class.java).apply {
        action = "com.twilio.voice.INCOMING"
        putExtra("INCOMING_CALL_INVITE", callInvite)
    }
    sendBroadcast(intent)
}
```
Same fix applied for `onCancelledCallInvite()`.

### Fix 2: FCM Token Race Condition + Device ID Identity
**File:** `core/voip/TwilioManager.kt`

- `registerForIncomingCalls()` now fetches FCM token directly from Firebase if not in DataStore preferences (fixes race condition where `refreshToken()` hasn't saved the token yet)
- `initialize()` now uses `Settings.Secure.ANDROID_ID` as the identity parameter (was userId)
- Added `registrationStatus` StateFlow for UI diagnostics

### Fix 3: USE_FULL_SCREEN_INTENT Permission
**File:** `AndroidManifest.xml`

Added `android.permission.USE_FULL_SCREEN_INTENT` - required on Android 14+ (API 34+) for the incoming call full-screen notification.

---

## Debug Diagnostics Added

### Settings > About Section
Three new rows visible in the app's Settings screen:
- **Device ID**: Shows `Settings.Secure.ANDROID_ID` (sent to backend)
- **VoIP Status**: Shows Twilio registration status (green/red/orange)
- **Last FCM**: Shows last FCM message received (tap to refresh). Written via SharedPreferences from `RhentiFirebaseMessagingService`

### SharedPreferences Debug
`RhentiFirebaseMessagingService` writes to `fcm_debug` SharedPreferences on every FCM message, including:
- Timestamp
- Whether it's a Twilio message
- Data field keys
- `Voice.handleMessage()` result
- `CallInvite` details if received

### Files Modified for Debug
- `SettingsViewModel.kt` - Added `twilioRegistrationStatus`, `lastFcmEvent`, `deviceId`
- `SettingsScreen.kt` - Added Device ID, VoIP Status, Last FCM rows in About section
- `TwilioManager.kt` - Added `registrationStatus` StateFlow
- `RhentiFirebaseMessagingService.kt` - Added `writeFcmDebug()` and `getLastFcmEvent()`

---

## Testing Checklist (Once Backend Is Configured)

1. Install latest build
2. Open app, go to main screen (triggers Twilio init)
3. Check Settings > About > VoIP Status shows "REGISTERED"
4. Call the virtual number from another phone
5. Check Settings > About > Last FCM - should show Twilio call invite data
6. Phone should ring with incoming call notification
7. Tap "Answer" - should connect the call
8. Tap "Decline" - should reject and dismiss notification

---

## Other Notes
- Samsung battery optimization: Set Rhenti to "Unrestricted" in Settings > Apps > Rhenti > Battery
- Samsung logcat export (JSON format) does NOT include app process logs (Log.d). Use `adb logcat` directly or the SharedPreferences debug approach.
- Parse Samsung logcat JSON with: `python3 -c "import json; data = json.load(open('file.logcat')); ..."`
- Project docs were archived from root to `docs/` folder on Feb 13, 2026
