# Rhenti Logo & Animated Loading Screen

## 📱 App Logo

### Logo Design
- **Background:** Navy blue (#2C3E50) - matches login screen gradient
- **Icon:** Coral "r" letter (#E8998D) - Rhenti brand color
- **Style:** Modern, clean, minimal

### Logo Files
- `logo_rhenti.xml` - Main app logo (200x200dp vector)
- `ic_app_logo.xml` - Alternative version (512x512dp vector)
- `ic_rhenti_logo.xml` - Simple circular version

### Usage

#### In Composables
```kotlin
Image(
    painter = painterResource(id = R.drawable.logo_rhenti),
    contentDescription = "Rhenti Logo",
    modifier = Modifier.size(120.dp)
)
```

#### As App Icon
Update `AndroidManifest.xml`:
```xml
<application
    android:icon="@drawable/logo_rhenti"
    android:roundIcon="@drawable/logo_rhenti"
    ...>
```

---

## 🎬 Animated Loading Screen

### Features
- **Bouncing Logo Animation** - Logo bounces up and down with squash/stretch effect
- **Fading Text** - "rhenti" text pulses with fade animation
- **Loading Dots** - Three dots animate in sequence
- **Gradient Background** - Navy blue gradient matching login screen
- **Alternative Cat Animation** - Playful cat emoji bouncing (inspired by loading cat designs)

### Animation Types

#### 1. Main Loading Screen (Recommended)
```kotlin
LoadingScreen(
    onLoadingComplete = {
        // Navigate to main screen
        navController.navigate("main")
    }
)
```

**Includes:**
- Bouncing Rhenti logo
- Fading "rhenti" text
- Animated loading dots
- Auto-completes after 2 seconds (configurable)

#### 2. Cat Animation (Alternative)
```kotlin
CuteCatLoadingAnimation()
```

**Includes:**
- Bouncing cat emoji 🐱
- "Loading..." text
- Animated dots
- Playful rotation effect

---

## 🚀 Integration Guide

### Option 1: Add to Navigation (Recommended)

**Update `NavGraph.kt`:**

```kotlin
@Composable
fun RhentiNavHost(
    navController: NavHostController,
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val authState by authViewModel.uiState.collectAsState()

    NavHost(
        navController = navController,
        startDestination = "splash" // Start with splash screen
    ) {
        // Splash screen
        composable("splash") {
            LoadingScreen(
                onLoadingComplete = {
                    // Determine destination based on auth state
                    if (authState.isAuthenticated) {
                        navController.navigate("main") {
                            popUpTo("splash") { inclusive = true }
                        }
                    } else {
                        navController.navigate("auth") {
                            popUpTo("splash") { inclusive = true }
                        }
                    }
                }
            )
        }

        // Rest of your navigation...
        navigation(startDestination = "login", route = "auth") {
            // Auth screens...
        }

        composable("main") {
            MainTabScreen(authViewModel = authViewModel)
        }
    }
}
```

### Option 2: Android Splash Screen API (Android 12+)

**Update `AndroidManifest.xml`:**

```xml
<application
    android:theme="@style/Theme.Rhenti.Splash"
    ...>
    <activity
        android:name=".MainActivity"
        android:theme="@style/Theme.Rhenti.Splash"
        ...>
```

**The splash theme is already configured in `splash_theme.xml`:**
- Background: Navy blue (#2C3E50)
- Icon: Rhenti logo
- Duration: 1000ms
- Post-splash theme: Main app theme

### Option 3: Manual Integration

```kotlin
@Composable
fun App() {
    var isLoading by remember { mutableStateOf(true) }

    if (isLoading) {
        LoadingScreen(
            onLoadingComplete = { isLoading = false }
        )
    } else {
        // Your main app content
        RhentiNavHost(...)
    }
}
```

---

## 🎨 Customization

### Adjust Animation Duration

**In `LoadingScreen.kt`:**

```kotlin
LaunchedEffect(Unit) {
    delay(3000) // Change to desired duration (milliseconds)
    onLoadingComplete()
}
```

### Modify Bounce Speed

```kotlin
val bounce by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = -30f,
    animationSpec = infiniteRepeatable(
        animation = tween(
            durationMillis = 800, // Change this (default: 600)
            easing = EaseInOutCubic
        ),
        repeatMode = RepeatMode.Reverse
    ),
    label = "bounce"
)
```

### Change Loading Dots Color

```kotlin
Box(
    modifier = Modifier
        .size(12.dp)
        .scale(scale)
        .background(
            color = Color.White, // Change from RhentiCoral
            shape = CircleShape
        )
)
```

### Use Different Background

```kotlin
Box(
    modifier = modifier
        .fillMaxSize()
        .background(Color(0xFF2C3E50)) // Solid color
        // OR
        .background(
            brush = Brush.radialGradient(...) // Different gradient
        )
)
```

---

## 🎯 Animation Details

### Bouncing Logo
- **Effect:** Squash and stretch with vertical movement
- **Duration:** 600ms per cycle
- **Easing:** EaseInOutCubic for smooth motion
- **Movement:** -30dp up and down
- **Scale:** 1.0 to 1.1

### Fading Text
- **Effect:** Opacity pulse
- **Duration:** 1000ms per cycle
- **Alpha:** 0.5 to 1.0
- **Easing:** Linear

### Loading Dots
- **Effect:** Sequential scale animation
- **Count:** 3 dots
- **Delay:** 200ms between each dot
- **Duration:** 1200ms total cycle
- **Scale:** 0.5 to 1.2

---

## 📝 Notes

### Performance
- All animations use `rememberInfiniteTransition` for efficient, non-blocking animations
- Compose-based animations are GPU-accelerated
- No bitmap resources loaded, all vector graphics

### Accessibility
- Consider adding `contentDescription` for screen readers
- Provide option to skip/reduce animations for users with motion sensitivity

### Best Practices
- Keep splash duration between 1-3 seconds
- Use splash time for actual initialization (loading data, checking auth)
- Don't block user interaction unnecessarily

---

## 🔧 Troubleshooting

### Logo not showing
- Ensure `logo_rhenti.xml` is in `res/drawable/`
- Check that R.drawable.logo_rhenti is accessible
- Verify vector drawable is properly formatted

### Animation not smooth
- Check device performance
- Reduce animation complexity if needed
- Use `animationSpec` with appropriate duration

### Build errors
- Ensure all imports are correct
- Check that Compose dependencies are up to date
- Verify Hilt annotations on ViewModel

---

## 🎨 Color Reference

```kotlin
Navy Blue (Background):  #2C3E50
Rhenti Coral (Primary):  #E8998D
White (Text):            #FFFFFF
```

---

## 📸 Preview

To preview in Android Studio:
1. Open `LoadingScreen.kt`
2. Click "Split" or "Design" view
3. Add `@Preview` annotation:

```kotlin
@Preview(showBackground = true)
@Composable
fun LoadingScreenPreview() {
    RhentiAppTheme {
        LoadingScreen(onLoadingComplete = {})
    }
}
```

---

**Created:** February 5, 2026
**Status:** Ready to use
**Animations:** Bouncing logo, fading text, sequential dots, optional cat animation
