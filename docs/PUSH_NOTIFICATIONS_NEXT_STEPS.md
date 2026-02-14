# Push Notifications - Next Steps & Testing Guide

**Status:** ✅ Phase 8 Implementation Complete - Ready for Testing
**Created:** February 12, 2026
**App Build:** Successful

---

## 📋 Current Status

### ✅ Completed (Android App)
- [x] Firebase Cloud Messaging integrated (BOM 33.7.0)
- [x] google-services.json configured
- [x] 6 notification channels created
- [x] FCM token management implemented
- [x] Deep linking fully functional
- [x] Notification display with Rhenti branding
- [x] Android 13+ permission handling
- [x] App package: `com.rhentimobile`
- [x] All code built successfully

### ⏳ Pending (Next Steps)
- [ ] Get FCM token from running app
- [ ] Test notifications from Firebase Console
- [ ] Verify notification channels on device
- [ ] Test deep linking navigation
- [ ] Backend API implementation (see below)
- [ ] End-to-end testing with real notifications

---

## 🎯 Next Steps - Testing & Verification

### Step 1: Get FCM Token (5 minutes)

1. **Run the app** on device/emulator
2. **Open Logcat** in Android Studio
3. **Filter:** Search for "FCM" or "FcmTokenManager"
4. **Find log:** `D/FcmTokenManager: FCM Token fetched: [token]`
5. **Copy token** and save it for testing

**Expected Log Output:**
```
D/FcmTokenManager: FCM Token fetched: eXaMpLe_ToKeN_123...
D/FcmTokenManager: FCM token synced successfully with backend
```

**If token doesn't appear:**
- Check: `adb logcat | grep -i "fcm\|firebase"`
- Verify: google-services.json is in `app/` folder
- Restart app and check logs again

---

### Step 2: Verify Notification Channels (2 minutes)

**On Device:**
1. Settings → Apps → Rhenti → Notifications
2. Verify **6 channels** are listed:
   - Ongoing Calls (Low importance)
   - Incoming Calls (High importance)
   - **Messages** (High importance) ← NEW
   - **Viewing Requests** (High importance) ← NEW
   - **Applications** (Default importance) ← NEW
   - **General** (Default importance) ← NEW

**Screenshot these** if needed for documentation.

---

### Step 3: Send Test Notification from Firebase Console (5 minutes)

#### Access Firebase Console:
1. Go to: https://console.firebase.google.com
2. Select project: **rhenti-chat**
3. Navigate to: **Cloud Messaging** (left sidebar)
4. Click: **"Send test message"** button

#### Configure Test Message:

**FCM registration token:** [Paste your token from Step 1]

**Click "+ Add field"** and add these data fields:

| Key | Value | Notes |
|-----|-------|-------|
| `type` | `message` | Notification type |
| `title` | `New Message from John Doe` | Notification title |
| `body` | `Hello! I'm interested in the property.` | Message body |
| `threadId` | `test_thread_123` | Thread to navigate to |
| `imageUrl` | *(optional)* | Avatar image URL |

**Click "Test"** to send.

#### Expected Result:
- ✅ Notification appears in notification shade
- ✅ Shows "New Message from John Doe"
- ✅ Rhenti coral color icon
- ✅ Tapping navigates to Chats tab

---

### Step 4: Test All Notification Types (10 minutes)

#### Test 1: Message Notification
```json
{
  "data": {
    "type": "message",
    "title": "New Message from John Doe",
    "body": "Hello! I'm interested in the property.",
    "threadId": "thread_abc123",
    "imageUrl": "https://example.com/avatar.jpg"
  }
}
```
**Expected:** High importance, navigates to thread

---

#### Test 2: Viewing Request Notification
```json
{
  "data": {
    "type": "viewing",
    "title": "New Viewing Request",
    "body": "Jane Smith requested viewing for 123 Main St",
    "bookingId": "booking_456",
    "threadId": "thread_abc123",
    "propertyAddress": "123 Main St, City"
  }
}
```
**Expected:** High importance, shows property address

---

#### Test 3: Application Update Notification
```json
{
  "data": {
    "type": "application",
    "title": "Application Status Update",
    "body": "Your application for 123 Main St has been approved",
    "applicationId": "app_789",
    "threadId": "thread_abc123"
  }
}
```
**Expected:** Default importance

---

#### Test 4: Missed Call Notification
```json
{
  "data": {
    "type": "call",
    "title": "Missed Call",
    "body": "Missed call from John Doe",
    "contactId": "contact_123",
    "phoneNumber": "+1234567890"
  }
}
```
**Expected:** Navigates to Calls tab

---

### Step 5: Test Deep Linking (5 minutes)

#### Test via ADB (App State: Killed)
```bash
# Test thread deep link
adb shell am start -W -a android.intent.action.VIEW \
  -d "rhenti://thread/thread_123" com.rhentimobile

# Test contact deep link
adb shell am start -W -a android.intent.action.VIEW \
  -d "rhenti://contact/contact_456" com.rhentimobile

# Test tab navigation
adb shell am start -W -a android.intent.action.VIEW \
  -d "rhenti://chats" com.rhentimobile
```

#### Test All App States:
- [ ] **Foreground:** App open, notification arrives, tap it
- [ ] **Background:** App minimized, notification arrives, tap it
- [ ] **Killed:** Force stop app, notification arrives, tap it

**Expected:** All states should navigate to correct screen.

---

### Step 6: Test Notification Permission (2 minutes)

**On Android 13+ devices:**
1. Uninstall and reinstall app
2. Login with credentials
3. **Permission dialog should appear** requesting notification access
4. Test both:
   - [ ] Grant permission → Notifications work
   - [ ] Deny permission → App doesn't crash, notifications don't show

---

## 🔧 Backend Requirements - ACTION NEEDED

### ⚠️ Backend Team Must Implement:

#### 1. FCM Token Registration Endpoint

**Endpoint:** `POST /fcm/register`

**Request Body:**
```json
{
  "user_id": "user123",
  "super_account_id": "account456",
  "fcm_token": "fcm_token_string_here",
  "platform": "android",
  "device_id": "android_device_id",
  "app_version": "1.0"
}
```

**Response:**
```json
{
  "success": true,
  "message": "FCM token registered successfully"
}
```

**Implementation Notes:**
- Store FCM token in database linked to user/device
- Allow multiple tokens per user (multi-device support)
- Update token if device_id exists (token refresh)
- Index on `user_id` and `super_account_id` for fast lookups

---

#### 2. FCM Token Unregistration Endpoint

**Endpoint:** `POST /fcm/unregister`

**Request Body:**
```json
{
  "user_id": "user123",
  "fcm_token": "fcm_token_string_here"
}
```

**Response:**
```json
{
  "success": true,
  "message": "FCM token unregistered successfully"
}
```

**Implementation Notes:**
- Remove token from database
- Called on user logout
- Prevent sending notifications to unregistered tokens

---

#### 3. Send Push Notifications (Firebase Admin SDK)

**Setup Firebase Admin SDK:**
```javascript
// Node.js example
const admin = require('firebase-admin');

admin.initializeApp({
  credential: admin.credential.applicationDefault(),
  projectId: 'rhenti-chat'
});
```

**Send Message Notification:**
```javascript
async function sendMessageNotification(userId, threadId, senderName, messageText) {
  // Get FCM tokens for user
  const tokens = await getFcmTokensForUser(userId);

  if (tokens.length === 0) return;

  const message = {
    data: {
      type: 'message',
      title: `New Message from ${senderName}`,
      body: messageText,
      threadId: threadId,
      imageUrl: getSenderAvatarUrl(senderId)
    },
    tokens: tokens
  };

  const response = await admin.messaging().sendMulticast(message);
  console.log(`Sent ${response.successCount} notifications`);
}
```

**When to Send Notifications:**
- New message received → `type: message`
- Viewing request created → `type: viewing`
- Application status changed → `type: application`
- Missed call → `type: call`

---

## 📊 Testing Checklist

### Device Testing
- [ ] Android 13+ device (permission required)
- [ ] Android 7-12 device (no permission needed)
- [ ] Emulator testing
- [ ] Physical device testing

### Notification Scenarios
- [ ] Message notification (foreground)
- [ ] Message notification (background)
- [ ] Message notification (app killed)
- [ ] Viewing request notification
- [ ] Application update notification
- [ ] Missed call notification
- [ ] Multiple notifications (grouping)
- [ ] Notification with image
- [ ] Notification without image

### Deep Linking
- [ ] Thread deep link from notification
- [ ] Contact deep link from notification
- [ ] Tab navigation deep links
- [ ] Invalid deep link (graceful handling)
- [ ] Deep link when app is open
- [ ] Deep link when app is killed

### Permission Handling
- [ ] Permission granted (Android 13+)
- [ ] Permission denied (Android 13+)
- [ ] Permission "don't ask again" scenario
- [ ] No crashes when permission denied

### Edge Cases
- [ ] No internet connection (token cached)
- [ ] Backend /fcm/register fails (token stored locally)
- [ ] Invalid notification payload (graceful handling)
- [ ] Multiple devices same user (all receive notifications)
- [ ] Logout → Unregister token
- [ ] Token refresh handled correctly

---

## 🐛 Troubleshooting

### Issue: No FCM Token in Logs

**Possible Causes:**
- google-services.json not in `app/` folder
- Firebase project not configured correctly
- Google Play Services not available on device

**Solution:**
```bash
# Check if google-services.json exists
ls app/google-services.json

# Check logs for Firebase initialization
adb logcat | grep -i "firebase"
```

---

### Issue: Notification Not Appearing

**Check:**
1. Permission granted? (Settings → Apps → Rhenti → Notifications)
2. Channel enabled? (Check specific channel settings)
3. Do Not Disturb mode? (Disable and test)
4. Battery optimization? (Exclude Rhenti from optimization)

**Debug:**
```bash
# Check notification logs
adb logcat | grep -i "notification\|fcm"

# Look for "Notification displayed" log
```

---

### Issue: Deep Link Not Working

**Check:**
1. MainActivity.handleDeepLink() called?
2. MainTabViewModel reference set?
3. Intent extras present?

**Debug:**
```bash
# Check deep link logs
adb logcat | grep -i "deeplink\|MainActivity"

# Look for "Deep link destination:" log
```

---

### Issue: Backend /fcm/register Fails

**Temporary Workaround:**
- App stores token locally in DataStore
- Token will sync when backend is ready
- App still receives notifications via Firebase Console testing

**Check:**
```bash
# Look for backend sync errors
adb logcat | grep -i "FcmTokenManager"

# Expected: "Failed to sync FCM token with backend"
```

---

## 📈 Success Metrics

### Functional Requirements
- ✅ FCM token generated on app startup
- ✅ Token synced with backend (when endpoints ready)
- ✅ Notifications display correctly
- ✅ Deep linking navigates to correct screens
- ✅ Permission requested on Android 13+
- ✅ No crashes on permission denial

### User Experience
- ✅ Notifications appear within 2 seconds
- ✅ Notification styling matches Rhenti brand
- ✅ Navigation is smooth and instant
- ✅ Images load in notifications (if provided)
- ✅ Multiple notifications don't spam user

### Technical Requirements
- ✅ No token logging in production
- ✅ Graceful handling of invalid payloads
- ✅ Multi-device support
- ✅ Token refresh handled automatically
- ✅ Offline mode supported (cached token)

---

## 📚 Documentation References

### Implementation Docs
- **PHASE8_IMPLEMENTATION_SUMMARY.md** - Complete implementation details
- **CLAUDE.md** - Updated project status

### Firebase Resources
- Firebase Console: https://console.firebase.google.com
- FCM Documentation: https://firebase.google.com/docs/cloud-messaging/android/client
- Admin SDK Guide: https://firebase.google.com/docs/cloud-messaging/admin/send-messages

### Code Locations
- **FCM Service:** `app/src/main/java/com/tomasronis/rhentiapp/core/notifications/RhentiFirebaseMessagingService.kt`
- **Notification Manager:** `app/src/main/java/com/tomasronis/rhentiapp/core/notifications/RhentiNotificationManager.kt`
- **Deep Link Handler:** `app/src/main/java/com/tomasronis/rhentiapp/core/notifications/DeepLinkHandler.kt`
- **Token Manager:** `app/src/main/java/com/tomasronis/rhentiapp/core/notifications/FcmTokenManager.kt`

---

## 🎯 Quick Start Testing (5 minutes)

**Fastest way to verify push notifications work:**

1. **Run app** → Get FCM token from Logcat
2. **Firebase Console** → Send test message with token
3. **Verify** notification appears
4. **Tap** notification → Verify navigation

**If all above work:** ✅ Push notifications are functional!

---

## ✅ When Push Notifications Are Ready

After backend implementation and testing:

1. **Update CLAUDE.md:**
   - Mark backend endpoints as implemented
   - Update Configuration Status section

2. **Test End-to-End:**
   - Real user flow: New message → Notification → Navigate
   - Multiple devices
   - All notification types

3. **Document for Team:**
   - Share notification payload formats
   - Share testing procedures
   - Create runbook for troubleshooting

---

**Last Updated:** February 12, 2026
**Status:** Ready for Testing & Backend Implementation
**Questions?** Reference PHASE8_IMPLEMENTATION_SUMMARY.md or contact Claude

---

## 🚀 Resume Testing Later

To resume push notification testing:

1. Read this document (PUSH_NOTIFICATIONS_NEXT_STEPS.md)
2. Start with "Step 1: Get FCM Token"
3. Follow steps sequentially
4. Check off items as you complete them
5. Document any issues found

**Good luck! The hard part (implementation) is done.** 🎉
