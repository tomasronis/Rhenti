# Rhenti Android App - Project Context & Requirements

**Last Updated:** February 14, 2026 (Messages Refresh Fix + Production Testing)
**Current Phase:** Phase 8 Complete + Incoming Call Polish + Production Testing
**Next Phase:** Phase 9 (Background Sync & Polish) or Production Release

**Recent Updates (Feb 14, 2026):**
- ✅ **MESSAGES TAB REFRESH FIX:** Messages now always refresh on notification tap
  - Added `messagesRefreshTrigger` counter in MainTabViewModel
  - ChatsTabContent observes trigger and calls refreshThreads() on change
  - Works for notification deep links, tab clicks, and cross-tab navigation
- ✅ **INCOMING CALL FULL-SCREEN NOTIFICATIONS:** Reliable in ALL device states
- ✅ Full-screen incoming call activity works: screen off, screen on, app foreground/background/closed
- ✅ `SYSTEM_ALERT_WINDOW` permission for reliable screen-on activity launch
- ✅ No heads-up notification flash (silent notification on screen-on, CallStyle only on screen-off)
- ✅ Single decline action dismisses everything (activity + service + notification)
- ✅ No duplicate call screen (MainTabScreen no longer shows ActiveCallScreen for Ringing state)
- ✅ Auto-prompt overlay permission after login
- ✅ Settings: "Display Over Apps" toggle, simplified VoIP Status (Ready/Not Ready)
- ✅ Settings: Removed debug items (Re-register VoIP, Last FCM, Token Grants)

**Known Issues (Production Testing - Feb 14, 2026):**
- ⚠️ **Contacts not loading on large production accounts**: `/phone-tracking/getContacts/{superAccountId}` endpoint takes very long (or never responds) for accounts with ~98K threads. Logcat shows 4 redundant identical requests fired simultaneously with no response within capture window. Messages (chat-hub/threads) load fine. Needs investigation: add better error logging, reduce redundant API calls, check if server-side pagination is needed.

**Previous Updates (Feb 12, 2026):**
- ✅ **PHASE 8 COMPLETE:** Firebase Cloud Messaging push notifications fully implemented
- ✅ FCM integration with Firebase project setup
- ✅ 4 notification types: Messages, Viewings, Applications, Calls
- ✅ Deep linking to threads, contacts, and calls from notifications
- ✅ FCM token management with backend sync
- ✅ Android 13+ notification permission handling
- ✅ 6 notification channels (including Phase 7 VoIP channels)
- ✅ Notification styling with Rhenti branding (coral color, proper icons)
- ✅ App package updated to `com.rhentimobile` to match Firebase config

**Previous Updates (Feb 8, 2026):**
- ✅ **Property Picker Update:** Switched to `/getAddressesForChatHub` endpoint to match iOS
- ✅ Added ChatHub property models (ChatHubBuilding, ChatHubUnit, ChatHubProperty)
- ✅ Updated PropertiesRepository to parse hierarchical building+unit structure
- ✅ Properties now properly flattened for multi-unit buildings
- ✅ Enhanced debug logging for property fetching
- ✅ Backward compatible with existing UI and caching

**Previous Updates (Feb 5, 2026):**
- ✅ **MAJOR REDESIGN:** Updated entire Android app to match iOS design specifications
- ✅ Messages screen: Enhanced with platform tags, property addresses, improved badges
- ✅ Login screen: Complete redesign with gradient background and glassmorphic styling
- ✅ Settings screen: New comprehensive design with theme selector and organized sections
- ✅ Calls screen: iOS-style layout with color-coded icons and proper grouping
- ✅ Booking cards: Circular action buttons matching iOS design
- ✅ Added iOS design reference images to repository
- ✅ All changes committed and pushed to GitHub

**Previous Updates (Feb 3, 2026):**
- ✅ Fixed search functionality on all tabs (Chat, Contacts, Calls)
- ✅ Fixed call logs API parsing error (wrapper object handling)
- ✅ Committed all Phase 5-7 code to git
- ✅ Added comprehensive documentation

---

## 🎨 iOS Design Parity (NEW - February 5, 2026)

The Android app has been completely redesigned to match the iOS app design specifications. All major screens now feature iOS-inspired layouts, styling, and interactions while maintaining Material 3 principles.

### Design Reference
- **Location:** `App Design Guidance/` folder contains iOS screenshots and design images
- **Source:** iPhone 17 Pro screenshots from iOS app (January 30, 2026)

### Redesigned Screens

#### 1. **Messages Screen**
- Large "Messages" title (iOS-style)
- Always-visible search bar with gray background
- Hamburger menu icon in top right
- **Enhanced Thread Cards:**
  - Larger avatars (60dp) with badge icons
  - Property address with location pin icon below name
  - Blue pill platform tags (Rhenti-powered, Facebook, Kijiji, Zumper, rhenti)
  - Circular dark unread badges with white text
  - Chevron icons for navigation
  - Proper dividers between items

#### 2. **Login Screen**
- Dark navy/blue gradient background (#2C3E50 → #34495E)
- Large centered "rhenti" logo
- **Glassmorphic card** with semi-transparent white background
- Email and password fields with leading icons
- Coral-colored "Sign In" button (#E8998D)
- "Forgot Password?" text button
- "or continue with" divider
- **Circular SSO buttons** for Google and Microsoft

#### 3. **Settings Screen** (Completely New)
- Large "Settings" title
- **Profile card** at top (avatar, name, email)
- **Preferences section:**
  - Theme color selector (Rhenti coral, Ocean blue, Earth teal)
  - Visual color circles with checkmark on selected
- **Storage section:**
  - Keep Media, Messages per Chat, Storage Used, Clear Cache
  - Each item with icon, label, and value
- **About section:**
  - Organization, Version, Connection info
- **Help & Legal:**
  - Help & Support, Privacy Policy, Terms of Service
- Coral-colored "Sign Out" button
- "Made with ❤️ in Canada" footer

#### 4. **Calls Screen**
- Centered "Recent Calls" title (iOS CenterAlignedTopAppBar)
- "Done" button in top right (when filtering/searching)
- Date headers in format "January 30, 2026"
- **Call log items:**
  - Color-coded call type icons (green incoming, blue outgoing, red missed)
  - Name and time on left
  - Duration on right
  - Chevron for navigation
  - Dividers between items

#### 5. **Thread Detail / Booking Cards**
- **iOS-style circular action buttons:**
  - Accept (green background, checkmark icon)
  - Alter (orange background, clock icon)
  - Decline (red background, X icon)
  - Labels below each button
  - Spacious layout with proper spacing

### Design System

#### Colors
- **Primary:** Rhenti Coral (#E8998D)
- **Success:** Green (#34C759) for incoming calls, accept actions
- **Warning:** Orange (#FF9500) for pending status, alter actions
- **Error:** Red (#FF3B30) for missed calls, decline actions
- **Accent Blue:** (#007AFF) for outgoing calls, info chips

#### Typography
- Large bold titles (DisplaySmall) for screen headers
- Proper font weights (SemiBold, Medium, Normal)
- Consistent spacing and line heights

#### Components
- Circular action buttons with icons
- Glassmorphic effects on login screen
- Platform tags as rounded pills
- Proper shadows and elevations
- Material 3 with iOS-inspired layouts

### Files Changed
- `ThreadListScreen.kt` - Messages screen redesign
- `ThreadCard.kt` - Enhanced thread cards with badges and tags
- `LoginScreen.kt` - Complete redesign with gradient background
- `SettingsScreen.kt` - New comprehensive settings screen
- `ProfileTab.kt` - Simplified to use new settings screen
- `CallsScreen.kt` - iOS-style title and date grouping
- `CallLogCard.kt` - Color-coded icons and simplified layout
- `BookingMessageCard.kt` - Circular action buttons

---

## 📋 Project Overview

**Rhenti** is a property management mobile application for Android that enables property owners and managers to communicate with tenants, handle bookings, manage contacts, make VoIP calls, and streamline property-related operations.

### Target Users
- **Primary:** Property owners and managers
- **Secondary:** Tenants (future scope)

### Platform
- **Android:** Minimum SDK 24 (Android 7.0), Target SDK 36 (Android 14)
- **Language:** Kotlin
- **UI Framework:** Jetpack Compose with Material 3

---

## 🏗️ Architecture & Design Patterns

### Core Architecture
- **Pattern:** MVVM (Model-View-ViewModel) with Clean Architecture principles
- **Navigation:** Single Activity with Jetpack Compose Navigation
- **State Management:** StateFlow/Flow for reactive state
- **Dependency Injection:** Hilt (Dagger)

### Layer Structure
```
presentation/          # UI Layer (Composables, ViewModels)
├── auth/             # Authentication screens
├── main/             # Main app screens
├── navigation/       # Navigation graphs
└── theme/            # Material 3 theming

data/                 # Data Layer
├── auth/             # Authentication data
├── repository/       # Repository implementations
└── models/           # Data models

core/                 # Core Infrastructure
├── network/          # API client, interceptors
├── database/         # Room database
├── security/         # Encryption, token management
└── di/               # Dependency injection modules
```

### Key Design Decisions

1. **Repository Pattern:** All data access goes through repositories
2. **Sealed Classes:** Type-safe error handling (`NetworkResult`, `AuthError`)
3. **Coroutines:** All async operations use Kotlin coroutines
4. **Encrypted Storage:** Sensitive data stored with EncryptedSharedPreferences
5. **Network First:** Fetch from API, cache locally with Room
6. **Reactive UI:** UI updates automatically via StateFlow observation

---

## 🛠️ Technology Stack

### Core Dependencies
- **Kotlin:** 2.0.21
- **Gradle:** 9.1.0
- **Android Gradle Plugin:** 8.9.1

### Jetpack Compose & UI
- **Compose BOM:** 2025.01.00
- **Material 3:** Latest from BOM
- **Material Icons Extended:** For expanded icon set
- **Navigation Compose:** 2.8.5
- **Coil:** 2.7.0 (Image loading)

### Networking
- **Retrofit:** 2.11.0
- **OkHttp:** 4.12.0 (with logging interceptor)
- **Moshi:** 1.15.1 (JSON serialization)

### Local Storage
- **Room:** 2.6.1 (SQLite abstraction)
- **DataStore Preferences:** 1.1.1
- **Security Crypto:** 1.1.0-alpha06 (EncryptedSharedPreferences)

### Authentication
- **Play Services Auth:** 21.2.0
- **Google ID:** 1.1.1 (Credential Manager)
- **Credentials:** 1.5.0
- **Credentials Play Services:** 1.5.0
- **Microsoft MSAL:** 4.0.0

### Dependency Injection
- **Hilt:** 2.54
- **Hilt Navigation Compose:** 1.2.0

### Future Dependencies (Not Yet Added)
- **Twilio Voice:** 6.4.1 (Phase 7 - VoIP Calling)
- **Firebase BOM:** 33.7.0 (Phase 8 - Push Notifications)
- **WorkManager:** 2.10.0 (Phase 9 - Background Sync)

---

## 🌐 Backend API Integration

### Base URLs
- **Production:** `https://api.rhenti.com`
- **UAT:** `https://uatapi.rhenti.com` (current default for debug builds)

### Image Storage
- **Production:** `https://upploader.rhenti.com/images/`
- **UAT:** `https://uatimgs.rhenti.com/images/`

### White Label
- **Identifier:** `rhenti_mobile` (sent in all requests)

### Authentication Flow
1. User logs in → Backend returns `{ token, user_id, super_account_id, white_label, user }`
2. Token stored in EncryptedSharedPreferences via TokenManager
3. Token attached to all subsequent requests via AuthInterceptor
4. User data cached in Room database

### API Endpoints (Implemented)

#### Authentication
- `POST /login` - Email/password login
- `POST /register` - New user registration
- `POST /forgot` - Password reset request
- `POST /integrations/sso/mobile/login` - SSO login (Google/Microsoft)

#### Chat Hub (Phase 3)
- `POST /chat-hub/threads` - Get chat threads with filters
- `GET /chat-hub/threads/{threadId}` - Get single thread
- `GET /chat-hub/messages/{threadId}` - Get messages (with pagination)
- `POST /message/{senderId}` - Send message
- `PUT /chat-hub/threads/{threadId}/badge` - Clear unread badge
- `POST /chat-hub/bookings/{bookingId}` - Handle booking action
- `POST /chat-hub/alternatives` - Propose alternative times

#### Contacts (Phase 4)
- `GET /phone-tracking/getContacts/{superAccountId}` - Get contacts list
- `POST /phone-tracking/getContactProfile` - Get contact details

#### Calls (Phase 7)
- `POST /phone-tracking/accessToken` - Get Twilio access token
- `GET /phone-tracking/ownercontactlogs/{superAccountId}` - Get call logs
- `POST /phone-tracking/callLog` - Record call log

#### Users
- `GET /users/{userId}` - Get user profile
- `PUT /users` - Update user profile
- `POST /users` - Batch get users

#### Properties
- `GET /getAddressesForChatHub` - Get buildings with units for chat property picker (hierarchical: buildings → units)
- `GET /properties/{propertyId}` - Get property details

#### App Version
- `GET /appVersion` - Check for updates

---

## 📱 Implementation Phases

### ✅ Phase 1: Foundation Layer (COMPLETE)
**Duration:** 3-4 days

**Completed Items:**
- Project setup with latest dependencies
- Gradle build system configuration
- Hilt dependency injection
- Retrofit + OkHttp + Moshi networking
- Room database with DAOs for all entities
- EncryptedSharedPreferences for secure storage
- TokenManager for auth token management
- AuthInterceptor for API authentication
- NetworkMonitor for connectivity checks
- Environment configuration (UAT/Production)
- Material 3 theme setup

**Key Files:**
- `core/network/` - API client and networking
- `core/database/` - Room database and entities
- `core/security/` - Encryption and token management
- `core/di/` - Hilt modules

---

### ✅ Phase 2: Authentication (COMPLETE)
**Duration:** 10-11 days

**Completed Items:**
- Email/password login with validation
- Google Sign-In (Credential Manager API)
- Microsoft authentication (MSAL with web view)
- User registration with validation
- Forgot password functionality
- Session management with auto-restore
- AuthViewModel with state management
- Login, Registration, ForgotPassword screens
- Navigation graph (auth → main)
- Secure token persistence
- User data caching
- Observable auth state

**Key Files:**
- `data/auth/` - Auth models, services, repository
- `presentation/auth/` - Auth screens and ViewModel
- `presentation/navigation/` - Navigation graph
- `core/di/AuthModule.kt` - Auth dependency injection

**Configuration Required:**
- Google OAuth credentials (Web Client ID)
- Microsoft MSAL signature hash
- See `QUICK_START_AUTH.md` for setup instructions

**Technical Notes:**
- Google uses modern Credential Manager API (not legacy GoogleSignInClient)
- Microsoft uses MSAL with web view (matches iOS implementation)
- Client ID: `affcec03-e96d-490f-b869-c334d0b0835b`
- SSO tokens exchanged immediately, not persisted

**Authentication Fixes (Feb 1, 2026):**
- ✅ Added INTERNET permission to AndroidManifest (was missing!)
- ✅ Added `x-white-label: rhenti_mobile` header to all requests including auth endpoints
- ✅ Fixed API response parsing to match production API format:
  * API returns camelCase: `userId`, `whiteLabel` (not snake_case)
  * User data in `profile` field (not `user`)
  * Dates as ISO 8601 strings (not Unix timestamps)
- ✅ Configured for production API: `https://api.rhenti.com`
- ✅ Login now works with demo@rhenti.com credentials
- ✅ White label configured as `rhenti_mobile` (matching iOS)

---

### ✅ Phase 3: UI/Navigation - Main Features (COMPLETE)
**Duration:** 1 day (Feb 1, 2026)
**Actual:** ~8 hours of focused implementation

**Completed Items:**

#### 3.1 Bottom Tab Navigation ✅
- 4-tab NavigationBar (Chats, Contacts, Calls, Profile)
- Material 3 icons and styling
- Tab persistence with DataStore
- Badge support for unread counts
- Placeholder screens for Contacts, Calls, Profile
- Logout functionality on Profile tab

#### 3.2 Chat Hub Screen ✅
- Thread list with LazyColumn
- ThreadCard with avatar, name, last message, timestamp
- Pull-to-refresh functionality
- Search with real-time filtering
- Swipe actions (pin/unpin, delete)
- Unread badge display
- Pin indicator (star icon)
- Empty state view
- Error state with retry
- Delete confirmation dialog

#### 3.3 Thread Detail Screen ✅
- Message list with pagination
- Text message sending with optimistic updates
- Image upload via image picker
- Image display with full-screen view
- Booking cards with approve/decline actions
- Alternative time picker bottom sheet
- Message status indicators (sending/sent/failed)
- Owner/renter message styling
- Auto-scroll to bottom
- Empty state view

#### 3.4 Data Layer ✅
- ChatHubRepository with API + Room integration
- ChatHubViewModel with StateFlow
- Domain models: ChatThread, ChatMessage, MessageMetadata
- API response parsing
- Offline caching and reactive updates
- Optimistic message sending
- Error handling with NetworkResult

**Key Files Created:**
- `data/chathub/` - Models, repository, implementation (5 files)
- `presentation/main/chathub/` - Screens and ViewModel (3 files)
- `presentation/main/chathub/components/` - UI components (9 files)
- `core/preferences/PreferencesManager.kt` - Tab persistence
- `core/di/ChatHubModule.kt` - Dependency injection
- `presentation/main/MainTabViewModel.kt` - Tab state
- `presentation/main/tabs/` - Placeholder screens (4 files)

**Technical Highlights:**
- MVVM architecture with clean separation
- Repository pattern with API-first, cache fallback
- Reactive UI with StateFlow/Flow
- Material 3 design system throughout
- Dark mode support
- Offline-first with Room caching
- Optimistic UI updates
- Image to base64 conversion
- Pull-to-refresh with Material 3 APIs
- Swipe-to-dismiss for thread actions

**API Endpoints Implemented:**
- `POST /chat-hub/threads` - Get threads
- `GET /chat-hub/messages/{threadId}` - Get messages with pagination
- `POST /message/{senderId}` - Send text/image messages
- `PUT /chat-hub/threads/{threadId}/badge` - Clear unread
- `POST /chat-hub/bookings/{bookingId}` - Handle booking actions
- `POST /chat-hub/alternatives` - Propose alternative times

**Database:**
- Threads and messages cached in Room
- Reactive Flow updates from database
- Offline viewing of cached data
- Pin/unpin thread persistence

---

### ✅ Phase 4: Contacts (COMPLETE)
**Duration:** 1 day (Feb 1, 2026)

**Completed Items:**

#### 4.1 Contacts List Screen ✅
- Grouped contact list (alphabetical sections)
- Search functionality with real-time filtering
- Pull-to-refresh
- Contact avatars (images or initials)
- Activity stats (message/call counts, last activity)
- Empty state view
- Error state with retry

#### 4.2 Contact Detail Screen ✅
- Full contact profile with avatar
- Contact information (email, phone)
- Activity statistics
- Property associations list
- Contact notes (if available)
- Action buttons (Message, Call)
- Navigation to chat from contact

#### 4.3 Data Layer ✅
- ContactsRepository with API + Room integration
- ContactsViewModel with StateFlow
- Domain models: Contact, ContactProfile, ContactProperty
- API response parsing (supports snake_case and camelCase)
- Offline caching with reactive updates
- Search functionality
- Error handling with NetworkResult

**API Endpoints Implemented:**
- `GET /phone-tracking/getContacts/{superAccountId}` - Fetches contact list
- `POST /phone-tracking/getContactProfile` - Fetches detailed profile

**Key Files Created/Updated:**
- `data/contacts/` - Models, repository, implementation (3 files)
- `presentation/main/contacts/` - Screens and ViewModel (3 files)
- `presentation/main/contacts/components/` - UI components (2 files)
- `presentation/main/tabs/ContactsPlaceholderScreen.kt` - Tab navigation wrapper

**Technical Highlights:**
- MVVM architecture with clean separation
- Repository pattern with API-first, cache fallback
- Reactive UI with StateFlow/Flow
- Material 3 design system
- Dark mode support
- Grouped list with section headers
- Avatar display with initials fallback
- Smooth navigation between list and detail
- Integration with chat (navigate to Chats tab from contact)
- Placeholder for call functionality (Phase 7)

**Database:**
- Contact data cached in Room
- Reactive Flow updates from database
- Offline viewing of cached contacts
- Search across cached contacts

---

### 📅 Phase 5: User Profile (Future)
**Duration:** 3-4 days

**Requirements:**
- View profile screen
- Edit profile (name, email, phone, photo)
- Upload profile photo
- Change password
- Logout functionality
- App settings (notifications, theme)

**API Endpoints:**
- `GET /users/{userId}`
- `PUT /users`

---

### 📅 Phase 6: Calls UI (Future)
**Duration:** 4-5 days

**Requirements:**
- Call logs list
- Call detail view
- Filter by date, type, contact
- Search call logs
- Initiate call from log

**API Endpoints:**
- `GET /phone-tracking/ownercontactlogs/{superAccountId}`
- `POST /phone-tracking/callLog`

**Database:**
- `CachedCallLog` entity already created
- `CallLogDao` already created

---

### 📅 Phase 7: VoIP Calling (Future)
**Duration:** 8-10 days

**Requirements:**
- Twilio integration
- Incoming call handling
- Outgoing call UI
- Active call screen (mute, speaker, keypad)
- Call notifications
- Foreground service for calls

**Dependencies to Add:**
- Twilio Voice SDK

**API Endpoints:**
- `POST /phone-tracking/accessToken`
- `POST /phone-tracking/callLog`

---

### ✅ Phase 8: Push Notifications (COMPLETE)
**Duration:** February 12, 2026 (1 day implementation)

**Completed Items:**

#### 8.1 Firebase Setup ✅
- Firebase project configured (rhenti-chat)
- google-services.json added to project
- Firebase BOM 33.7.0 integrated
- Application package updated to `com.rhentimobile`
- Notification icon drawable created

#### 8.2 Core Infrastructure ✅
- 4 new notification channels: Messages, Viewings, Applications, General
- NotificationPayload data classes with type-safe parsing
- DeepLinkHandler for navigation routing (supports rhenti:// URIs)
- FcmTokenManager for token lifecycle management
- PreferencesManager extended with FCM token storage

#### 8.3 Firebase Messaging Service ✅
- RhentiFirebaseMessagingService implemented
- onMessageReceived() parses and displays notifications
- onNewToken() syncs tokens with backend
- Service registered in AndroidManifest with MESSAGING_EVENT intent filter

#### 8.4 Notification Display ✅
- RhentiNotificationManager for building notifications
- BigTextStyle notifications with Rhenti coral branding
- PendingIntents for deep linking with notification data
- Image loading support for notification large icons
- Proper notification priorities and categories

#### 8.5 Deep Linking ✅
- Deep link intent filters in MainActivity (rhenti://thread, contact, call, etc.)
- handleDeepLink() method parses intent data
- onNewIntent() override for notification taps when app is open
- Integration with MainTabViewModel for navigation
- Supports all app states: foreground, background, killed

#### 8.6 Backend Integration ✅
- NotificationsRepository with FCM token sync
- POST /fcm/register endpoint integration
- POST /fcm/unregister endpoint integration
- NotificationsModule for Hilt DI
- RhentiApplication initializes FCM on startup

#### 8.7 Permissions ✅
- POST_NOTIFICATIONS permission in AndroidManifest
- Permission launcher with ActivityResultContracts
- requestNotificationPermission() in MainActivity
- Permission requested after successful login
- Graceful handling of denied permissions

**Key Files Created (17 files):**
- `core/notifications/RhentiFirebaseMessagingService.kt`
- `core/notifications/RhentiNotificationManager.kt`
- `core/notifications/NotificationPayload.kt`
- `core/notifications/DeepLinkHandler.kt`
- `core/notifications/FcmTokenManager.kt`
- `data/notifications/models/NotificationModels.kt`
- `data/notifications/repository/NotificationsRepository.kt`
- `data/notifications/repository/NotificationsRepositoryImpl.kt`
- `core/di/NotificationsModule.kt`
- `res/drawable/ic_notification.xml`

**Key Files Modified (7 files):**
- `app/build.gradle.kts` - Firebase dependencies, package name
- `AndroidManifest.xml` - Service, permissions, deep links
- `NotificationChannels.kt` - 4 new channels
- `PreferencesManager.kt` - FCM token storage
- `MainActivity.kt` - Deep linking, permissions
- `MainTabScreen.kt` - ViewModel reference passing
- `NavGraph.kt` - Permission request after login
- `ApiClient.kt` - FCM endpoints
- `RhentiApplication.kt` - FCM initialization

**Technical Highlights:**
- Notification types: MESSAGE, VIEWING, APPLICATION, CALL, GENERAL
- Deep link URIs: rhenti://thread/{id}, rhenti://contact/{id}, etc.
- FCM token automatically synced with backend on new token
- Device ID (Android ID) sent with FCM registration
- Supports Android 7.0+ (API 24+), notification permission on API 33+
- No token logging in production builds
- Graceful degradation when permission denied

**API Endpoints:**
- `POST /fcm/register` - Register FCM token
- `POST /fcm/unregister` - Unregister on logout

**Testing:**
- Firebase Console test messaging ready
- Deep links testable via ADB: `adb shell am start -W -a android.intent.action.VIEW -d "rhenti://thread/123"`
- All notification channels visible in Settings > Apps > Rhenti > Notifications

---

### ✅ Incoming Call Full-Screen Notifications (Feb 14, 2026)
**Duration:** 1 day of iterative refinement

**Problem Solved:**
Incoming VoIP calls need to show a full-screen activity reliably in ALL device states — screen off/locked, screen on with app closed, screen on with app in background, and screen on with app in foreground. Samsung Android 16 has strict restrictions on background activity launches.

**Architecture:**

```
FCM Push → Voice.handleMessage() → onCallInvite()
    → IncomingCallService.start(context, callInvite)
        → startForeground() with notification
        → startRinging() + acquireWakeLock()
        → 300ms delay → startActivity(IncomingCallActivity)
```

**Components:**

| Component | File | Role |
|---|---|---|
| IncomingCallService | `core/voip/IncomingCallService.kt` | Foreground service — owns ringing lifecycle (ringtone, vibration, wake lock, 45s timeout, notification) |
| IncomingCallActivity | `presentation/calls/active/IncomingCallActivity.kt` | Full-screen UI with `setShowWhenLocked(true)` + `setTurnScreenOn(true)` |
| IncomingCallReceiver | `core/voip/IncomingCallReceiver.kt` | BroadcastReceiver — fast `callInvite.reject()` from notification decline button |
| ActiveCallViewModel | `presentation/calls/active/ActiveCallViewModel.kt` | ViewModel for accept/decline from full-screen UI, stops service on decline |

**Screen-State Strategy:**

| Screen State | Notification Type | Activity Launch | Requires |
|---|---|---|---|
| Screen OFF/locked | Full CallStyle (PRIORITY_MAX + fullScreenIntent) | System fires fullScreenIntent | `USE_FULL_SCREEN_INTENT` permission |
| Screen ON | Silent (PRIORITY_LOW, no fullScreenIntent) | `startActivity()` after 300ms delay | `SYSTEM_ALERT_WINDOW` permission |
| Fallback (startActivity fails) | Upgrades to full CallStyle | User taps heads-up notification | Nothing extra |

**Key Design Decisions:**
1. **Silent notification on screen-on**: Prevents heads-up flash when activity is about to launch
2. **300ms postDelayed**: Gives system time to process foreground promotion before startActivity()
3. **demoteNotification()**: If screen was off and fullScreenIntent fires, IncomingCallActivity calls `onActivityShown()` to replace PRIORITY_MAX notification with silent version
4. **Decline flow**: ActiveCallViewModel calls `IncomingCallService.decline(context)` which stops service + removes notification in one action
5. **No Ringing in MainTabScreen**: `MainTabScreen` only shows ActiveCallScreen for `Active`/`Dialing` states — IncomingCallActivity exclusively handles `Ringing`
6. **singleTop launch mode**: Prevents duplicate activities when both fullScreenIntent and startActivity() fire

**Permissions:**
- `SYSTEM_ALERT_WINDOW` — "Display over other apps" (auto-prompted after login)
- `USE_FULL_SCREEN_INTENT` — Full-screen notifications (toggle in Settings)
- `FOREGROUND_SERVICE_PHONE_CALL` — Foreground service type
- `WAKE_LOCK` — Wake screen on incoming call

**Notification Channel:** `incoming_calls_v4` (HIGH importance, vibration ON, no sound — MediaPlayer handles ringtone)

**State Transitions:**
```
Idle → Ringing (IncomingCallService starts)
    ├→ User Accepts → Active (CallService takes over) → Ended → Idle
    ├→ User Declines → Idle (receiver/ViewModel rejects, service stops)
    ├→ Remote Cancels → Idle (onCancelledCallInvite)
    └→ 45s Timeout → Idle (auto-reject)
```

**Settings UI:**
- "VoIP Status" — Ready (green) / Not Ready (red)
- "Full-Screen Calls" — Shows `canUseFullScreenIntent()` status, tap to enable (Android 14+)
- "Display Over Apps" — Shows `canDrawOverlays()` status, tap to enable

**Key Files Modified:**
- `AndroidManifest.xml` — SYSTEM_ALERT_WINDOW permission
- `IncomingCallService.kt` — Silent vs CallStyle notification strategy, demoteNotification()
- `IncomingCallActivity.kt` — Calls onActivityShown() on create
- `ActiveCallViewModel.kt` — Decline calls IncomingCallService.decline()
- `MainTabScreen.kt` — Excluded Ringing from showActiveCallScreen
- `MainActivity.kt` — Auto-prompt overlay permission after login
- `SettingsScreen.kt` — Display Over Apps toggle, simplified VoIP Status
- `SettingsViewModel.kt` — canDrawOverlays state

**Tested On:** Samsung S24 Ultra, Android 16

---

### 📅 Phase 9: Background Sync & Polish (Future)
**Duration:** 5-6 days

**Requirements:**
- WorkManager for periodic sync
- Conflict resolution
- App version checking
- Error handling improvements
- Performance optimization
- Accessibility improvements
- Analytics tracking

**Dependencies to Add:**
- WorkManager

---

## 🔐 Security & Best Practices

### Security Measures
1. **Token Storage:** EncryptedSharedPreferences (AES-256)
2. **HTTPS Only:** All API calls over HTTPS
3. **No Token Logging:** All logs guarded with `BuildConfig.DEBUG`
4. **SSO Token Handling:** Tokens used immediately, not persisted
5. **Input Validation:** Client-side validation on all forms
6. **ProGuard:** Enabled for release builds

### Code Quality Standards
1. **No Magic Numbers:** Use named constants
2. **Sealed Classes:** For type-safe state/errors
3. **Null Safety:** Leverage Kotlin's null safety
4. **Coroutines:** No blocking calls on main thread
5. **Resource Management:** Use `use {}` for closeable resources
6. **Comments:** Only where logic isn't self-evident

### Git Workflow
1. **Commit Messages:** Descriptive, imperative mood
2. **Co-Authored-By:** Include Claude attribution
3. **Branch:** Work on feature branches, merge to master
4. **No Force Push:** To master branch

---

## 📊 Database Schema

### Entities (All Created)

#### CachedUser
```kotlin
- id: String (PK)
- email: String
- firstName: String?
- lastName: String?
- phone: String?
- profilePhotoUri: String?
- createdAt: Long
- updatedAt: Long
```

#### CachedThread
```kotlin
- id: String (PK)
- propertyId: String?
- bookingId: String?
- participants: String (JSON array)
- lastMessage: String?
- lastMessageTimestamp: Long?
- unreadCount: Int
- createdAt: Long
- updatedAt: Long
```

#### CachedMessage
```kotlin
- id: String (PK)
- threadId: String (FK)
- senderId: String
- content: String?
- imageUri: String?
- messageType: String
- timestamp: Long
- isRead: Boolean
```

#### CachedContact
```kotlin
- id: String (PK)
- name: String
- email: String?
- phone: String?
- propertyId: String?
- role: String?
- createdAt: Long
```

#### CachedCallLog
```kotlin
- id: String (PK)
- contactId: String?
- phoneNumber: String
- type: String (incoming/outgoing/missed)
- duration: Int (seconds)
- timestamp: Long
```

### DAOs (All Created)
- `UserDao` - User CRUD operations
- `ThreadDao` - Thread and message CRUD
- `MessageDao` - Message operations
- `ContactDao` - Contact operations
- `CallLogDao` - Call log operations

---

## 🎨 UI/UX Guidelines

### Material 3 Design System
- **Color Scheme:** Dynamic color with theme support
- **Typography:** Material 3 type scale
- **Components:** Use Material 3 components only
- **Icons:** Material Icons Extended
- **Spacing:** 8dp grid system

### Screen Patterns
1. **Loading States:** Show CircularProgressIndicator
2. **Error States:** Show error message + retry button
3. **Empty States:** Show illustration + helpful text
4. **Success States:** Brief confirmation (Snackbar)

### Navigation Patterns
1. **Top Level:** Bottom Navigation (4 tabs)
2. **Hierarchical:** Back button in TopAppBar
3. **Modal:** Full-screen dialogs or bottom sheets
4. **Deep Links:** Support for notifications

### Accessibility
1. **Content Descriptions:** All icons and images
2. **Minimum Touch Targets:** 48dp
3. **Contrast Ratios:** WCAG AA compliant
4. **Font Scaling:** Support dynamic type

---

## 🔧 Development Setup

### Prerequisites
1. Android Studio Ladybug or newer
2. JDK 17
3. Android SDK 24-36
4. Git configured with credentials

### First-Time Setup
1. Clone repository
2. Configure OAuth credentials (see `QUICK_START_AUTH.md`)
3. Sync Gradle files
4. Run on emulator or device

### Build Variants
- **Debug:** Points to UAT API
- **Release:** Points to Production API

### Running the App
```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Run tests
./gradlew test
```

---

## 📝 Important Notes

### iOS Parity
- Microsoft authentication config matches iOS implementation
- SSO flows designed to match iOS behavior
- White label identifier consistent with iOS

### Known Limitations (Current)
1. **OAuth Configuration:** Google OAuth and Microsoft MSAL require manual credential setup
2. **Image Upload:** No image compression - large images may impact performance
3. **Alternative Times:** Time picker uses mock data - needs proper date/time picker
4. **Search:** No debounce on search input (triggers immediately)
5. **Error Messages:** Generic error messages - could parse API errors for specifics
6. **Biometric Auth:** Not implemented yet (planned for future)
7. **Retry Logic:** Failed requests don't auto-retry (manual retry only)
8. **Contacts on large accounts:** `/phone-tracking/getContacts/` endpoint slow/unresponsive for accounts with ~98K threads. 4 redundant concurrent calls fired on startup. Needs: reduce to single call, add pagination or timeout handling, better error logging
9. **Redundant API calls on startup:** ContactsViewModel.init(), MainTabViewModel.preloadDataForSync(), and MainTabScreen LaunchedEffect all independently call refreshContacts() — should deduplicate

### Future Enhancements (Backlog)
- Biometric authentication (fingerprint/face)
- Apple Sign-In
- Social login (Facebook)
- Email verification flow
- Account linking (merge SSO with email)
- Password strength indicator
- Remember me functionality
- Multi-language support
- Tablet-optimized layouts

---

## 🐛 Troubleshooting Common Issues

### Build Issues
1. **Microsoft dependency not found:**
   - Ensure Microsoft Maven repository is in `settings.gradle.kts`
   - URL: `https://pkgs.dev.azure.com/MicrosoftDeviceSDK/DuoSDK-Public/_packaging/Duo-SDK-Feed/maven/v1`

2. **Visibility icons not found:**
   - Ensure `compose-material-icons-extended` dependency is added

3. **Google Sign-In fails with error 10:**
   - SHA-1 fingerprint mismatch
   - Regenerate fingerprint and update Google Cloud Console

4. **MSAL redirect URI mismatch:**
   - Signature hash not URL-encoded correctly
   - Ensure special characters: `+` → `%2B`, `/` → `%2F`, `=` → `%3D`

### Runtime Issues
1. **Network errors:**
   - Check network connectivity
   - Verify API endpoints are accessible
   - Check auth token is valid

2. **Database errors:**
   - Clear app data and reinstall
   - Check for schema migrations needed

---

## 📚 Resources & References

### Documentation
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Material 3 Design](https://m3.material.io/)
- [Hilt Dependency Injection](https://dagger.dev/hilt/)
- [Retrofit](https://square.github.io/retrofit/)
- [Room Database](https://developer.android.com/training/data-storage/room)
- [Google Credential Manager](https://developer.android.com/training/sign-in/credential-manager)
- [Microsoft MSAL Android](https://learn.microsoft.com/en-us/entra/identity-platform/msal-android-single-sign-on)

### Project Documentation
- `PHASE2_IMPLEMENTATION_SUMMARY.md` - Phase 2 complete details
- `QUICK_START_AUTH.md` - OAuth setup guide
- `README.md` - Project overview (if exists)

---

## 🎯 Current State Summary

**✅ Completed:**
- Phase 1: Foundation (networking, database, security) ✨
- Phase 2: Authentication (email, Google, Microsoft, registration) ✨
- Phase 3: UI/Navigation - Main Features (bottom tabs, chat threads, messaging, bookings) ✨
- Phase 4: Contacts (list, detail, search, chat navigation) ✨
- Phase 5: User Profile (view, edit, password change, settings) ✨
- Phase 6: Calls UI (call logs, filters, search) ✨
- Phase 7: VoIP Calling (Twilio, active calls, audio management) ✨
- Phase 8: Push Notifications (FCM, deep linking, token management) ✨
- Incoming Call Full-Screen Notifications (all device states, Samsung S24 Ultra tested) ✨

**🚧 In Progress:**
- None - Ready for Phase 9 or Production Testing!

**📋 Next Up:**
- Phase 9: Background Sync & Polish (WorkManager, optimization)
- Production Release Candidate Testing

**⚙️ Configuration Status:**
- ✅ API Configuration: Production (`api.rhenti.com`)
- ✅ White Label: `rhenti_mobile`
- ✅ App Package: `com.rhentimobile` (matches Firebase)
- ✅ Internet Permissions: Added
- ✅ Notification Permission: POST_NOTIFICATIONS (Android 13+)
- ✅ Overlay Permission: SYSTEM_ALERT_WINDOW (auto-prompted after login)
- ✅ Full-Screen Intent: USE_FULL_SCREEN_INTENT (toggle in Settings)
- ✅ Firebase Configuration: google-services.json in place
- ✅ Bottom Tab Navigation: Implemented with persistence
- ✅ Chat Hub: Full thread list and detail screens
- ✅ Message Sending: Text and image support
- ✅ Booking Management: Approve/decline/alternative times
- ✅ Contacts: List with search, detail view, chat integration
- ✅ API Parsing: Supports both snake_case and camelCase responses
- ✅ Push Notifications: FCM fully integrated
- ⚠️ Google OAuth Web Client ID: Needs real credentials (currently placeholder)
- ⚠️ Microsoft MSAL signature hash: Needs configuration
- ⚠️ Backend FCM Endpoints: Needs verification (/fcm/register, /fcm/unregister)

**📊 Feature Status:**
- ✅ Login/Registration/Logout
- ✅ Thread List with Search (FIXED Feb 3)
- ✅ Thread Detail with Messaging
- ✅ Image Upload and Display
- ✅ Booking Cards with Actions
- ✅ Pull-to-Refresh
- ✅ Swipe Actions (Pin/Delete)
- ✅ Offline Support (Room caching)
- ✅ Dark Mode Support
- ✅ Contacts List with Search (FIXED Feb 3)
- ✅ Contact Detail with Properties
- ✅ Navigate to Chat from Contact
- ✅ User Profile (view, edit, settings)
- ✅ Call Logs with Search (FIXED Feb 3)
- ✅ Call Filtering (type, date range)
- ✅ VoIP Calling (Twilio integration)
- ✅ Active Call Controls (mute, speaker, keypad)
- ✅ Push Notifications (Firebase FCM, deep linking)
- ✅ Notification Channels (6 total: calls, messages, viewings, applications, general)
- ✅ FCM Token Management (auto-sync with backend)
- ✅ Deep Linking (rhenti:// URIs for navigation)
- ✅ Incoming Call Full-Screen (all device states, no flash, single decline)
- ✅ Overlay Permission Auto-Prompt (after login)
- ⏳ Background Sync (Phase 9)

**🔗 Repository:**
- GitHub: `https://github.com/tomasronis/Rhenti`
- Branch: `master`
- Last Commit: Fix messages tab not refreshing on notification tap (Feb 14, 2026)
- Recent Commits:
  - Fix messages tab not refreshing on notification tap
  - Document incoming call full-screen notification architecture
  - Fix incoming call: no heads-up flash, single decline, overlay permission, clean settings
  - Fix incoming call: single activity launch, reliable decline, notification cleanup
  - Add IncomingCallService foreground service for reliable incoming call ringing
  - Show incoming call screen over lock screen without requiring unlock

---

**End of Context Document**

*This document should be updated at the end of each major phase or when significant architectural decisions are made.*
