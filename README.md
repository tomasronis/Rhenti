# Rhenti - Property Management Android App

<div align="center">

![Android](https://img.shields.io/badge/Platform-Android-green.svg)
![Kotlin](https://img.shields.io/badge/Language-Kotlin%202.0.21-blue.svg)
![Min SDK](https://img.shields.io/badge/Min%20SDK-24-orange.svg)
![Target SDK](https://img.shields.io/badge/Target%20SDK-36-orange.svg)
![License](https://img.shields.io/badge/License-Proprietary-red.svg)

**Modern property management application for Android**

[Features](#-features) • [Tech Stack](#-tech-stack) • [Getting Started](#-getting-started) • [Architecture](#-architecture) • [Contributing](#-contributing)

</div>

---

## 📋 Overview

Rhenti is a comprehensive property management mobile application that enables property owners and managers to:
- Communicate with tenants via in-app messaging
- Manage contacts and property relationships
- Make VoIP calls with integrated Twilio support
- Handle bookings and scheduling
- Track calls and conversations
- Receive real-time notifications

Built with **Jetpack Compose** and **Material 3**, Rhenti delivers a modern, professional user experience that matches the design language of [rhenti.com](https://rhenti.com).

---

## ✨ Features

### ✅ Implemented (Phase 1 & 2)

#### Authentication
- 📧 **Email/Password Login** - Secure authentication with validation
- 🔐 **Google Sign-In** - Modern Credential Manager API integration
- 🏢 **Microsoft SSO** - MSAL authentication for enterprise users
- 📝 **User Registration** - Complete signup flow with phone number
- 🔄 **Password Reset** - Email-based password recovery
- 💾 **Session Management** - Automatic login with encrypted token storage

#### Foundation
- 🏗️ **MVVM Architecture** - Clean, maintainable code structure
- 🎨 **Material 3 Design** - Modern UI with Rhenti brand colors (#4D65FF)
- 🔌 **RESTful API Integration** - Retrofit + OkHttp + Moshi
- 💿 **Local Database** - Room for offline data caching
- 🔒 **Secure Storage** - EncryptedSharedPreferences for sensitive data
- 🌐 **Network Monitoring** - Connectivity awareness
- 🎯 **Dependency Injection** - Hilt for clean architecture

### 🚧 In Development (Phase 3)

- 💬 **Chat Hub** - Thread-based messaging system
- 📱 **Contacts Management** - Property and tenant contacts
- 📞 **Call Logs** - VoIP call history and management

### 📅 Planned Features

- 📞 **VoIP Calling** - Twilio integration for in-app calls
- 🔔 **Push Notifications** - Firebase Cloud Messaging
- 📊 **Analytics** - Usage tracking and insights
- 🌍 **Multi-language Support** - Internationalization
- 📱 **Tablet Optimization** - Adaptive layouts

---

## 🛠️ Tech Stack

### Core Technologies
- **Language:** Kotlin 2.0.21
- **Build System:** Gradle 9.1.0
- **Min SDK:** 24 (Android 7.0)
- **Target SDK:** 36 (Android 14)

### Jetpack & UI
- **UI Framework:** Jetpack Compose (BOM 2025.01.00)
- **Material Design:** Material 3
- **Navigation:** Compose Navigation 2.8.5
- **Lifecycle:** ViewModel, LiveData, StateFlow
- **Image Loading:** Coil 2.7.0

### Networking & Data
- **HTTP Client:** Retrofit 2.11.0 + OkHttp 4.12.0
- **JSON:** Moshi 1.15.1
- **Local Database:** Room 2.6.1
- **Preferences:** DataStore + EncryptedSharedPreferences

### Authentication
- **Google Sign-In:** Credential Manager API + Play Services Auth 21.2.0
- **Microsoft SSO:** MSAL 4.0.0
- **Security:** Security Crypto 1.1.0-alpha06

### Dependency Injection
- **DI Framework:** Hilt 2.54

### Future Integrations
- **VoIP:** Twilio Voice 6.4.1 (Phase 7)
- **Push Notifications:** Firebase (Phase 8)
- **Background Jobs:** WorkManager 2.10.0 (Phase 9)

---

## 🚀 Getting Started

### Prerequisites

1. **Android Studio** Ladybug or newer
2. **JDK 17**
3. **Android SDK** with API levels 24-36
4. **Git** configured with your credentials

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/tomasronis/Rhenti.git
   cd Rhenti
   ```

2. **Configure OAuth Credentials**

   The app requires Google and Microsoft OAuth setup. Follow the detailed guide in [`QUICK_START_AUTH.md`](QUICK_START_AUTH.md):

   - **Google OAuth:** Create Web Client ID in Google Cloud Console
   - **Microsoft MSAL:** Generate signature hash and configure redirect URI

   Update these files:
   - `app/build.gradle.kts` (lines 29-30) - Google Web Client ID
   - `app/src/main/res/raw/msal_config.json` (line 4) - Microsoft redirect URI
   - `app/src/main/AndroidManifest.xml` (line 34) - Microsoft path

3. **Sync and Build**
   ```bash
   # Open in Android Studio
   # File > Sync Project with Gradle Files
   # Build > Rebuild Project
   ```

4. **Run**
   ```bash
   # Connect device/emulator
   # Run > Run 'app'
   ```

### Configuration

#### API Endpoints
- **Production:** `https://api.rhenti.com`
- **UAT (Debug):** `https://uatapi.rhenti.com` _(default for debug builds)_

#### Environment
Switch between UAT and Production by changing build variant:
- **Debug:** Uses UAT API
- **Release:** Uses Production API

---

## 🏗️ Architecture

### Project Structure

```
app/src/main/java/com/tomasronis/rhentiapp/
├── core/
│   ├── network/         # Retrofit, OkHttp, API client
│   ├── database/        # Room database, DAOs, entities
│   ├── security/        # Encryption, token management
│   └── di/              # Hilt dependency injection modules
├── data/
│   └── auth/            # Authentication data layer
│       ├── models/      # Data models
│       ├── services/    # Google & Microsoft auth services
│       └── repository/  # Repository pattern implementation
└── presentation/
    ├── auth/            # Authentication screens
    ├── main/            # Main app screens
    ├── navigation/      # Navigation graphs
    └── theme/           # Material 3 theming
```

### Design Patterns
- **MVVM (Model-View-ViewModel)** - Clear separation of concerns
- **Repository Pattern** - Abstract data sources
- **Single Activity** - Compose Navigation
- **Dependency Injection** - Hilt throughout
- **Reactive State** - StateFlow/Flow for UI updates

### Key Architectural Decisions
- **Sealed Classes** for type-safe errors and state
- **Coroutines** for all async operations
- **Encrypted Storage** for sensitive data
- **Network First** with local caching strategy
- **Material 3** components exclusively

---

## 🎨 Design System

### Brand Colors
- **Primary:** `#4D65FF` (Rhenti Blue)
- **Primary Light:** `#7A8FFF`
- **Primary Dark:** `#3A4FCC`

### Typography
- **Font Family:** Poppins (matching rhenti.com)
- **Weights:** 300, 400, 500, 600, 700, 800

### UI Principles
- ✨ Modern, professional, minimalist aesthetic
- 🎯 8dp spacing grid system
- ♿ WCAG AA accessibility compliance
- 🌓 Full dark mode support
- 📱 Responsive layouts (phone & tablet)

---

## 🧪 Testing

```bash
# Unit Tests
./gradlew test

# Instrumentation Tests (requires device/emulator)
./gradlew connectedAndroidTest

# Build all variants
./gradlew assembleDebug assembleRelease
```

---

## 📚 Documentation

- **[QUICK_START_AUTH.md](QUICK_START_AUTH.md)** - OAuth setup guide
- **[PHASE2_IMPLEMENTATION_SUMMARY.md](PHASE2_IMPLEMENTATION_SUMMARY.md)** - Detailed implementation notes
- **[claude.md](claude.md)** - Complete project context for development

---

## 🔐 Security

- ✅ All sensitive data encrypted with `EncryptedSharedPreferences`
- ✅ HTTPS-only API communication
- ✅ No tokens logged in production
- ✅ SSO tokens used immediately, not persisted
- ✅ ProGuard enabled for release builds
- ✅ Input validation on all forms

---

## 🗺️ Roadmap

### Phase 1: Foundation ✅ (Complete)
- Project setup, networking, database, security

### Phase 2: Authentication ✅ (Complete)
- Email/password, Google, Microsoft SSO, registration

### Phase 3: UI/Navigation 🚧 (In Progress)
- Chat hub, contacts, call logs, tab navigation

### Phase 4: Contacts 📅 (Planned)
- Contact management, profiles, search

### Phase 5: User Profile 📅 (Planned)
- Profile editing, settings, logout

### Phase 6: Calls UI 📅 (Planned)
- Call logs, filters, search

### Phase 7: VoIP Calling 📅 (Planned)
- Twilio integration, active call UI

### Phase 8: Push Notifications 📅 (Planned)
- Firebase messaging, notification channels

### Phase 9: Polish & Launch 📅 (Planned)
- Background sync, analytics, performance

---

## 👥 Contributing

This is a proprietary project. For bug reports or feature requests, please contact the development team.

---

## 📄 License

Proprietary - All rights reserved.

---

## 🙏 Acknowledgments

- Built with ❤️ using **Jetpack Compose** and **Material Design 3**
- Integrations: **Google**, **Microsoft**, **Twilio** (coming soon)
- Design inspired by [rhenti.com](https://rhenti.com)
- Co-developed with **Claude AI** by Anthropic

---

## 📞 Support

For questions or support, please contact:
- **Email:** tomas.ronis@live.ca
- **Repository:** [github.com/tomasronis/Rhenti](https://github.com/tomasronis/Rhenti)

---

<div align="center">

**Made with Jetpack Compose 🚀**

_Last updated: February 2026_

</div>
