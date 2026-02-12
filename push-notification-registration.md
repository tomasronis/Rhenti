# Push Notification Device Registration — Kotlin Implementation Spec

## Goal

Implement a push notification device registration system in native Kotlin (Android). The app must request notification permissions, obtain an FCM push token, collect device metadata, and register the device with our backend API so it can receive push notifications.

## Requirements

### 1. Request Notification Permission

- On Android 13+ (API 33+), the `POST_NOTIFICATIONS` runtime permission is required.
- On app launch, check if notification permission has been granted.
  - **If NOT granted** → request it from the user.
  - **If denied** → do not attempt token retrieval or registration. Optionally guide the user to settings.
  - **If granted** → proceed to obtain the FCM token.

### 2. Obtain FCM Push Token

- Use Firebase Cloud Messaging (FCM) to obtain a push token.
- Persist the token locally (e.g. `SharedPreferences` or `DataStore`) under the key `phoneCallToken`.
- The token may arrive **before or after** the user has authenticated. Handle both cases:
  - **If auth is available** at the time the token arrives → call `registerDevice()` immediately.
  - **If auth is NOT available** → store the token and defer registration. When auth becomes available later, call `registerDevice()` using the stored token.
- Listen for token refresh events and re-register when the token changes.

### 3. Register Device with Backend

Make an HTTP `POST` request to register the device.

**Endpoint:**

```
POST {BASE_URL}/devices/unauthorized
```

**Headers:** Include the standard auth headers your app uses (Bearer token, etc).

**Request Body (JSON):**

```json
{
  "device_id": "<unique device ID>",
  "account": "<super_account_id from auth>",
  "childAccount": "<userId from auth>",
  "manufacturer": "<device manufacturer, e.g. Samsung>",
  "model": "<device model, e.g. Pixel 7>",
  "version": "<Android OS version, e.g. 14>",
  "app_version": "<app version name from BuildConfig>",
  "name": "<user-facing device name>",
  "token": "<FCM push token>",
  "ablepush": true,
  "brand": "<device brand>",
  "os": "<base OS, e.g. Android>"
}
```

**Field sources (Android/Kotlin equivalents):**

| Field           | How to obtain                                        |
|-----------------|------------------------------------------------------|
| `device_id`     | `Settings.Secure.ANDROID_ID` or equivalent unique ID |
| `account`       | From your auth/session manager (`superAccountId`)    |
| `childAccount`  | From your auth/session manager (`userId`)            |
| `manufacturer`  | `Build.MANUFACTURER`                                 |
| `model`         | `Build.MODEL`                                        |
| `version`       | `Build.VERSION.RELEASE`                              |
| `app_version`   | `BuildConfig.VERSION_NAME`                           |
| `name`          | `Build.DEVICE` or `BluetoothAdapter.getDefaultAdapter()?.name` |
| `token`         | The FCM token obtained in step 2                     |
| `ablepush`      | Always `true`                                        |
| `brand`         | `Build.BRAND`                                        |
| `os`            | `"Android"` or `Build.VERSION.BASE_OS`               |

### 4. Rate Limiting

- Do not call `registerDevice()` more than once every **5 seconds**. Track the timestamp of the last attempt and skip if called again within the cooldown window.

### 5. Device Sign-Out

When the user logs out, call:

```
PUT {BASE_URL}/devices/{device_id}/signout
```

with the same auth headers. This unregisters the device from push notifications.

## Expected Flow

```
App Launch
    │
    ▼
Check notification permission
    │
    ├─ NOT GRANTED ──► Request permission from user
    │                      │
    │                      ├─ DENIED  ──► Stop (no registration)
    │                      └─ GRANTED ──► ▼
    │
    ├─ ALREADY GRANTED ──► ▼
    │
    ▼
Initialize FCM → request push token
    │
    ▼
Token received (onNewToken or getToken)
    │
    ├──► Persist token to local storage
    │
    └──► Is user authenticated?
          ├─ YES ──► registerDevice() → POST /devices/unauthorized
          └─ NO  ──► Wait. When auth completes → registerDevice()

Token refresh event
    │
    └──► Update stored token → registerDevice()

User logs out
    │
    └──► PUT /devices/{device_id}/signout
```

## Constraints

- Use coroutines for async work (no blocking the main thread).
- Handle network errors gracefully — log failures but don't crash.
- The FCM token can arrive at any point in the app lifecycle. The implementation must not assume a fixed ordering between token arrival and user authentication.
