# Device Registration Implementation - Compliance Summary

## Overview
This document outlines how the Android app implements the push notification device registration spec from `push-notification-registration.md`.

## Implementation Status: ✅ COMPLETE

### 1. ✅ Notification Permission
**Requirement:** Request `POST_NOTIFICATIONS` permission on Android 13+

**Implementation:**
- File: `MainActivity.kt`
- Permission requested after successful login
- Graceful handling when denied
- Only proceeds to token retrieval if granted

### 2. ✅ FCM Token Management
**Requirement:** Obtain FCM token, persist it, handle token refresh

**Implementation:**
- **Token Fetch:** `FcmTokenManager.refreshToken()` - called on app launch and after login
- **Token Storage:** Persisted in DataStore under key `"fcm_token"` (via `PreferencesManager`)
- **Token Refresh:** `RhentiFirebaseMessagingService.onNewToken()` - automatically called by Firebase
- **Deferred Registration:** Token stored locally if auth not available; registration triggered on login

**Files:**
- `FcmTokenManager.kt` - main token lifecycle manager
- `RhentiFirebaseMessagingService.kt` - Firebase messaging service
- `PreferencesManager.kt` - secure local storage

### 3. ✅ Device Registration Endpoint
**Requirement:** `POST /devices/unauthorized` with full device metadata

**Implementation:**
- **Endpoint:** `POST /devices/unauthorized`
- **File:** `ApiClient.kt` line ~180
- **Method:** `NotificationsRepositoryImpl.registerDevice()`

**Request Body Fields (all included):**
```kotlin
{
  "device_id": Settings.Secure.ANDROID_ID,
  "account": superAccountId (from TokenManager),
  "childAccount": userId (from TokenManager),
  "manufacturer": Build.MANUFACTURER,
  "model": Build.MODEL,
  "version": Build.VERSION.RELEASE,
  "app_version": BuildConfig.VERSION_NAME,
  "name": Build.DEVICE,
  "token": FCM push token,
  "ablepush": true,
  "brand": Build.BRAND,
  "os": "Android"
}
```

**Implementation Location:**
- `FcmTokenManager.syncTokenWithBackend()` - collects all device metadata
- `NotificationsRepositoryImpl.registerDevice()` - makes API call
- `DeviceRegistrationRequest` data class - request model

### 4. ✅ Rate Limiting
**Requirement:** No more than one registration per 5 seconds

**Implementation:**
- **File:** `NotificationsRepositoryImpl.kt`
- **Constant:** `RATE_LIMIT_MS = 5000L`
- **Logic:** Tracks `lastRegistrationTime`, skips calls within 5-second window
- **Behavior:** Returns success without making API call if rate-limited

### 5. ✅ Device Sign-Out
**Requirement:** `PUT /devices/{device_id}/signout` on logout

**Implementation:**
- **Endpoint:** `PUT /devices/{device_id}/signout`
- **File:** `ApiClient.kt` line ~183
- **Method:** `NotificationsRepositoryImpl.signoutDevice()`
- **Triggered:** `FcmTokenManager.unregisterToken()` called on logout
- **Cleanup:** Clears local token after successful sign-out

## Flow Diagram

```
App Launch
    │
    ▼
MainActivity.onCreate()
    │
    ├─► Check notification permission
    │   ├─ NOT GRANTED ──► Request permission
    │   │                   ├─ DENIED  ──► Stop
    │   │                   └─ GRANTED ──► Continue
    │   └─ ALREADY GRANTED ──► Continue
    │
    └─► FcmTokenManager.refreshToken()
            │
            ▼
        Firebase.getToken()
            │
            ├─► Persist to DataStore (key: "fcm_token")
            │
            └─► Is user authenticated?
                ├─ YES ──► syncTokenWithBackend()
                │           │
                │           ├─► Collect device metadata
                │           ├─► Check rate limit (5 sec)
                │           └─► POST /devices/unauthorized
                │
                └─ NO  ──► Wait for login
                            │
                            └─► On login → syncTokenWithBackend()

Token Refresh (Firebase event)
    │
    └─► RhentiFirebaseMessagingService.onNewToken()
            │
            ├─► Update DataStore
            └─► syncTokenWithBackend()

User Logout
    │
    └─► FcmTokenManager.unregisterToken()
            │
            ├─► PUT /devices/{device_id}/signout
            └─► Clear local token
```

## Key Files

### Core Logic
1. **FcmTokenManager.kt** - Token lifecycle management, registration coordination
2. **RhentiFirebaseMessagingService.kt** - Firebase messaging service, token refresh
3. **NotificationsRepositoryImpl.kt** - API calls, rate limiting
4. **PreferencesManager.kt** - Token persistence

### Data Models
5. **DeviceRegistrationRequest.kt** - Device registration request model
6. **DeviceRegistrationModels.kt** - Registration response models

### API Layer
7. **ApiClient.kt** - Retrofit endpoints definition

### UI Layer
8. **MainActivity.kt** - Permission handling

## Testing Checklist

- [ ] App launch → FCM token fetched and stored
- [ ] User login → Device registered with backend (all metadata fields)
- [ ] Token refresh → Re-registration triggered
- [ ] Rate limiting → Prevents multiple registrations within 5 seconds
- [ ] User logout → Device signed out from backend
- [ ] Permission denied → No registration attempted
- [ ] No auth available → Token stored, registration deferred
- [ ] Auth becomes available → Registration triggered with stored token

## Additional Notes

### Token Storage Key
- **Spec suggests:** `phoneCallToken`
- **Implementation uses:** `fcm_token`
- **Rationale:** Descriptive naming, using DataStore (better than SharedPreferences)

### Legacy Endpoints
The following legacy endpoints are maintained for backward compatibility but are not used by the new device registration flow:
- `POST /fcm/register` (old FCM registration)
- `POST /fcm/unregister` (old unregistration)

### Error Handling
- All network calls wrapped in try-catch
- Failures logged but don't crash the app
- User can continue using app even if registration fails
- Registration will retry on next token refresh or app restart

## Compliance Summary

| Requirement | Status | Implementation |
|------------|--------|----------------|
| Notification Permission | ✅ | MainActivity.kt |
| FCM Token Fetch | ✅ | FcmTokenManager.kt |
| Token Persistence | ✅ | PreferencesManager.kt |
| Token Refresh Handling | ✅ | RhentiFirebaseMessagingService.kt |
| Deferred Registration | ✅ | FcmTokenManager.kt (checks auth) |
| POST /devices/unauthorized | ✅ | ApiClient.kt, NotificationsRepositoryImpl.kt |
| All Device Metadata | ✅ | FcmTokenManager.syncTokenWithBackend() |
| Rate Limiting (5 sec) | ✅ | NotificationsRepositoryImpl.kt |
| PUT /devices/{id}/signout | ✅ | ApiClient.kt, NotificationsRepositoryImpl.kt |
| Error Handling | ✅ | All repository methods |

**Status: 100% Compliant with Specification** ✅
