# Quick Start - Authentication Setup

## ⚠️ REQUIRED: Configure Before Running

The app will **NOT build** until you complete these steps:

---

## Step 1: Google OAuth Setup (5 minutes)

### 1.1 Get Your SHA-1 Fingerprint

**Windows (PowerShell/CMD):**
```powershell
keytool -list -v -keystore %USERPROFILE%\.android\debug.keystore -alias androiddebugkey -storepass android -keypass android
```

**Mac/Linux:**
```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

**Look for:** `SHA1: AB:CD:EF:12:34:...`

### 1.2 Create Google Cloud Project

1. Go to https://console.cloud.google.com/
2. Create a new project (or select existing)
3. Navigate to **APIs & Services > Credentials**

### 1.3 Create Android OAuth Client

1. Click **"+ CREATE CREDENTIALS" > "OAuth client ID"**
2. Select **"Android"**
3. Fill in:
   - **Name:** Rhenti Android App
   - **Package name:** `com.tomasronis.rhentiapp`
   - **SHA-1 certificate fingerprint:** (paste from Step 1.1)
4. Click **"Create"**

### 1.4 Create Web OAuth Client

1. Click **"+ CREATE CREDENTIALS" > "OAuth client ID"** again
2. Select **"Web application"**
3. Fill in:
   - **Name:** Rhenti Web Client
4. Click **"Create"**
5. **COPY the Client ID** (looks like: `123456789012-abc...xyz.apps.googleusercontent.com`)

### 1.5 Update app/build.gradle.kts

**Find lines 29-30 and replace placeholders:**

```kotlin
// BEFORE (lines 29-30):
manifestPlaceholders["googleClientId"] = "YOUR_WEB_CLIENT_ID.apps.googleusercontent.com"
buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"YOUR_WEB_CLIENT_ID\"")

// AFTER (example - use YOUR actual client ID):
manifestPlaceholders["googleClientId"] = "123456789012-abc123xyz.apps.googleusercontent.com"
buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"123456789012-abc123xyz\"")
```

**Note:** Use ONLY the part before `.apps.googleusercontent.com` in the second line.

---

## Step 2: Microsoft MSAL Setup (3 minutes)

### 2.1 Generate Signature Hash

**Windows (requires OpenSSL - install from https://slproweb.com/products/Win32OpenSSL.html):**
```powershell
keytool -exportcert -alias androiddebugkey -keystore %USERPROFILE%\.android\debug.keystore -storepass android -keypass android | openssl sha1 -binary | openssl base64
```

**Mac/Linux:**
```bash
keytool -exportcert -alias androiddebugkey -keystore ~/.android/debug.keystore -storepass android -keypass android | openssl sha1 -binary | openssl base64
```

**Example output:** `ho+CF0JlXe+R84PzJSLnUC3aGo0=`

### 2.2 URL-Encode the Hash

Replace special characters:
- `+` → `%2B`
- `/` → `%2F`
- `=` → `%3D`

**Example:**
- Original: `ho+CF0JlXe+R84PzJSLnUC3aGo0=`
- Encoded: `ho%2BCF0JlXe%2BR84PzJSLnUC3aGo0%3D`

### 2.3 Update app/src/main/res/raw/msal_config.json

**Find line 4 and replace `SIGNATURE_HASH_PLACEHOLDER`:**

```json
// BEFORE (line 4):
"redirect_uri": "msauth://com.tomasronis.rhentiapp/SIGNATURE_HASH_PLACEHOLDER",

// AFTER (example - use YOUR encoded hash):
"redirect_uri": "msauth://com.tomasronis.rhentiapp/ho%2BCF0JlXe%2BR84PzJSLnUC3aGo0%3D",
```

### 2.4 Update app/src/main/AndroidManifest.xml

**Find line 34 and replace `SIGNATURE_HASH_PLACEHOLDER`:**

```xml
<!-- BEFORE (line 34): -->
android:path="/SIGNATURE_HASH_PLACEHOLDER"

<!-- AFTER (example - use YOUR encoded hash): -->
android:path="/ho%2BCF0JlXe%2BR84PzJSLnUC3aGo0%3D"
```

---

## Step 3: Build and Run

**Note:** The Microsoft Maven repository has already been configured in `settings.gradle.kts` to resolve MSAL dependencies.

### 3.1 Sync Gradle
In Android Studio:
1. Click **"File > Sync Project with Gradle Files"**
2. Wait for sync to complete

### 3.2 Rebuild Project
1. Click **"Build > Rebuild Project"**
2. Wait for build to complete (should succeed with no errors)

### 3.3 Run on Device/Emulator
1. Connect device or start emulator
2. Click **"Run > Run 'app'"**
3. App should launch and show login screen

---

## 🎯 Quick Validation

After setup, verify:

- [ ] **app/build.gradle.kts** has real Google Client ID (not "YOUR_WEB_CLIENT_ID")
- [ ] **msal_config.json** has URL-encoded signature hash (not "SIGNATURE_HASH_PLACEHOLDER")
- [ ] **AndroidManifest.xml** has URL-encoded signature hash (not "SIGNATURE_HASH_PLACEHOLDER")
- [ ] Gradle sync completes without errors
- [ ] Build completes without errors
- [ ] App launches successfully

---

## 🐛 Troubleshooting

### "GOOGLE_WEB_CLIENT_ID not found"
→ You didn't update `app/build.gradle.kts` lines 29-30

### "Failed to build MSAL app"
→ Check `msal_config.json` and `AndroidManifest.xml` have the same signature hash

### "Google Sign-In fails with 10"
→ SHA-1 fingerprint mismatch. Re-generate fingerprint and update Google Cloud Console

### "MSAL redirect URI mismatch"
→ Signature hash not URL-encoded correctly. Check `+` → `%2B`, `/` → `%2F`, `=` → `%3D`

---

## 📝 Configuration Summary

After completing setup, you should have modified:

1. **app/build.gradle.kts** - Lines 29-30 (Google OAuth)
2. **app/src/main/res/raw/msal_config.json** - Line 4 (MSAL redirect URI)
3. **app/src/main/AndroidManifest.xml** - Line 34 (MSAL path)

**Total changes: 3 lines in 3 files**

---

## ✅ All Set!

Once configured, you can:
- Sign in with email/password
- Sign in with Google
- Sign in with Microsoft
- Register new accounts
- Reset passwords

For detailed testing steps, see [PHASE2_IMPLEMENTATION_SUMMARY.md](PHASE2_IMPLEMENTATION_SUMMARY.md)
