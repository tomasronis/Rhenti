# Rhenti Android App - Project Context & Requirements

**Last Updated:** February 1, 2026 (Evening - Authentication Fixed)
**Current Phase:** Phase 2 Complete (Authentication - Working)
**Next Phase:** Phase 3 (UI/Navigation - Main Features - Starting Now)

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

### 🚧 Phase 3: UI/Navigation - Main Features (NEXT)
**Duration:** 8-9 days

**Requirements:**

#### 3.1 Bottom Tab Navigation
- **Tabs:** Chats, Contacts, Calls, Profile
- Material 3 NavigationBar with icons
- Persist selected tab across sessions
- Badge support for unread counts

#### 3.2 Chat Hub Screen
- List of threads (conversations)
- Thread preview: last message, timestamp, unread count
- Pull-to-refresh functionality
- Filter by property/booking
- Search threads
- Swipe actions (archive, delete)
- Navigate to thread detail on tap

#### 3.3 Thread Detail Screen
- Message list with pagination
- Send text messages
- Image upload support
- Booking cards (pending, confirmed)
- Booking actions (approve, decline)
- Alternative time proposals
- Typing indicators (future)
- Read receipts (future)

#### 3.4 Design Specifications
- Material 3 components throughout
- Adaptive layouts (phone/tablet)
- Dark mode support
- Consistent spacing (8dp grid)
- Primary color: From theme
- Error states with retry
- Empty states with illustrations

**API Endpoints to Implement:**
- All Chat Hub endpoints listed above

**Database:**
- Cache threads and messages in Room
- Sync strategy: Fetch from API, update cache
- Offline viewing of cached data

---

### 📅 Phase 4: Contacts (Future)
**Duration:** 5-6 days

**Requirements:**
- Contacts list with search
- Contact detail view
- Contact profile with property info
- Initiate chat from contact
- Initiate call from contact
- Contact sync

**API Endpoints:**
- `/phone-tracking/getContacts/{superAccountId}`
- `/phone-tracking/getContactProfile`

**Database:**
- `CachedContact` entity already created
- `ContactDao` already created

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

### 📅 Phase 8: Push Notifications (Future)
**Duration:** 4-5 days

**Requirements:**
- Firebase Cloud Messaging integration
- Notification channels
- Handle notification taps
- Badge counts
- Background notifications

**Dependencies to Add:**
- Firebase BOM
- Firebase Messaging
- Firebase Analytics

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

### Known Limitations (Phase 2)
1. Google OAuth requires manual credential setup
2. Microsoft MSAL requires signature hash configuration
3. No biometric authentication yet
4. Basic error messages (no specific API error codes)
5. No retry mechanism for failed requests

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
- Phase 1: Foundation (networking, database, security)
- Phase 2: Authentication (email, Google, Microsoft, registration) ✨ **WORKING!**

**🚧 In Progress:**
- Phase 3: UI/Navigation - Main Features (starting now)

**📋 Next Up:**
- Phase 3.1: Bottom Tab Navigation
- Phase 3.2: Chat Hub Screen
- Phase 3.3: Thread Detail Screen

**⚙️ Configuration Status:**
- ✅ API Configuration: Production (`api.rhenti.com`)
- ✅ White Label: `rhenti_mobile`
- ✅ Internet Permissions: Added
- ⚠️ Google OAuth Web Client ID: Needs real credentials (currently placeholder)
- ⚠️ Microsoft MSAL signature hash: Needs configuration

**🔗 Repository:**
- GitHub: `https://github.com/tomasronis/Rhenti`
- Branch: `master`
- Last Commit: Fix authentication to work with production API (Feb 1, 2026)

---

**End of Context Document**

*This document should be updated at the end of each major phase or when significant architectural decisions are made.*
