package com.tomasronis.rhentiapp.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val RhentiLightColorScheme = lightColorScheme(
    primary = LightPrimary,
    onPrimary = LightOnPrimary,
    primaryContainer = LightPrimaryContainer,
    onPrimaryContainer = LightOnPrimaryContainer,
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = LightTertiary,
    onTertiary = LightOnTertiary,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,
    error = LightError,
    onError = LightOnError,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = Color(0xFFF2F2F7),  // Light gray for cards
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceContainer = Color.White,  // Container surfaces (navigation bar, etc)
    surfaceContainerHigh = Color(0xFFFAFAFA),  // Slightly elevated surfaces
    surfaceContainerHighest = Color(0xFFF5F5F5),  // Most elevated surfaces
    inverseSurface = Color(0xFF2F3033),  // Inverse for tooltips, etc
    inverseOnSurface = Color(0xFFF1F0F4),  // Text on inverse surface
    outline = LightOutline,
    outlineVariant = DividerLight,
    scrim = Color(0xFF000000),  // Scrim for dialogs
    surfaceTint = RhentiCoral,  // Tint color for elevated surfaces
)

private val RhentiDarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkOnPrimary,
    primaryContainer = DarkPrimaryContainer,
    onPrimaryContainer = DarkOnPrimaryContainer,
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = DarkTertiary,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,
    background = DarkBackground,  // Pure black
    onBackground = DarkOnBackground,
    surface = DarkSurface,  // Dark cards
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,  // Lighter dark cards
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceContainer = DarkSurface,  // Container surfaces (navigation bar, etc)
    surfaceContainerHigh = Color(0xFF242427),  // Slightly elevated surfaces
    surfaceContainerHighest = DarkSurfaceVariant,  // Most elevated surfaces
    inverseSurface = Color(0xFFE4E2E6),  // Inverse for tooltips, etc
    inverseOnSurface = Color(0xFF2F3033),  // Text on inverse surface
    outline = DarkOutline,
    outlineVariant = Divider,
    scrim = Color(0xFF000000),  // Scrim for dialogs
    surfaceTint = RhentiCoral,  // Tint color for elevated surfaces
)

// Ocean Theme Color Schemes
private val OceanLightColorScheme = lightColorScheme(
    primary = OceanBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1E4FF),
    onPrimaryContainer = Color(0xFF001D36),
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = RhentiCoral,  // Use coral as tertiary
    onTertiary = Color.White,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,
    error = LightError,
    onError = LightOnError,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = Color(0xFFF2F2F7),
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = DividerLight,
)

private val OceanDarkColorScheme = darkColorScheme(
    primary = OceanBlue,
    onPrimary = Color.White,
    primaryContainer = OceanBlueDark,
    onPrimaryContainer = Color(0xFFD1E4FF),
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = RhentiCoral,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = Divider,
)

// Earth Theme Color Schemes
private val EarthLightColorScheme = lightColorScheme(
    primary = EarthTeal,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB8F5E0),
    onPrimaryContainer = Color(0xFF002114),
    secondary = LightSecondary,
    onSecondary = LightOnSecondary,
    secondaryContainer = LightSecondaryContainer,
    onSecondaryContainer = LightOnSecondaryContainer,
    tertiary = OceanBlue,  // Use blue as tertiary
    onTertiary = Color.White,
    tertiaryContainer = LightTertiaryContainer,
    onTertiaryContainer = LightOnTertiaryContainer,
    error = LightError,
    onError = LightOnError,
    errorContainer = LightErrorContainer,
    onErrorContainer = LightOnErrorContainer,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = Color(0xFFF2F2F7),
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = DividerLight,
)

private val EarthDarkColorScheme = darkColorScheme(
    primary = EarthTeal,
    onPrimary = Color(0xFF003828),
    primaryContainer = EarthTealDark,
    onPrimaryContainer = Color(0xFFB8F5E0),
    secondary = DarkSecondary,
    onSecondary = DarkOnSecondary,
    secondaryContainer = DarkSecondaryContainer,
    onSecondaryContainer = DarkOnSecondaryContainer,
    tertiary = OceanBlueLight,
    onTertiary = DarkOnTertiary,
    tertiaryContainer = DarkTertiaryContainer,
    onTertiaryContainer = DarkOnTertiaryContainer,
    error = DarkError,
    onError = DarkOnError,
    errorContainer = DarkErrorContainer,
    onErrorContainer = DarkOnErrorContainer,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = Divider,
)

/**
 * Main theme for the Rhenti App.
 *
 * Features Rhenti's brand identity with the signature coral accent (#E8998D) and
 * modern, professional design aesthetic matching the iOS app design.
 *
 * Supports:
 * - Light theme with light gray backgrounds (#F5F5F5)
 * - Dark theme with pure black background (#000000) and dark cards (#1C1C1E)
 * - System theme that follows device settings
 * - Coral primary color for buttons and highlights
 * - Blue accent for informational elements
 * - Dynamic color on Android 12+ (Material You) - optional
 *
 * @param darkTheme Whether to use dark theme
 * @param dynamicColor Whether to use dynamic colors (Android 12+) - defaults to false to maintain brand consistency
 * @param content The composable content to theme
 */
@Composable
fun RhentiAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Disabled by default to maintain Rhenti branding
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> RhentiDarkColorScheme
        else -> RhentiLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
