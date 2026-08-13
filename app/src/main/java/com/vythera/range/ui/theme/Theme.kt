@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.animation.ExperimentalSharedTransitionApi::class,
    androidx.compose.ui.ExperimentalComposeUiApi::class,
)

package com.vythera.range.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import com.vythera.range.ui.theme.RangePalette as P

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark"),
}

/** Screens read this to pick artwork and washes that suit the current scheme. */
val LocalIsDark = staticCompositionLocalOf { true }

private val RangeDarkScheme = darkColorScheme(
    primary = P.Aurora,
    onPrimary = Color(0xFF00301F),
    primaryContainer = Color(0xFF0E5B41),
    onPrimaryContainer = Color(0xFFB6FFDF),
    secondary = P.Sky,
    onSecondary = Color(0xFF00224C),
    secondaryContainer = Color(0xFF11386E),
    onSecondaryContainer = Color(0xFFCFE2FF),
    tertiary = P.Sand,
    onTertiary = Color(0xFF3F2600),
    tertiaryContainer = Color(0xFF5E3A00),
    onTertiaryContainer = Color(0xFFFFE0B4),
    background = P.Ink,
    onBackground = P.Mist,
    surface = P.Ink,
    onSurface = P.Mist,
    surfaceVariant = P.InkCard,
    onSurfaceVariant = P.MistDim,
    surfaceContainerLowest = Color(0xFF02040A),
    surfaceContainerLow = Color(0xFF070C17),
    surfaceContainer = P.InkRaised,
    surfaceContainerHigh = P.InkCard,
    surfaceContainerHighest = Color(0xFF16223A),
    outline = Color(0xFF44536F),
    outlineVariant = P.InkLine,
    error = P.Coral,
    onError = Color(0xFF4A0000),
    inverseSurface = P.Mist,
    inverseOnSurface = P.Ink,
    inversePrimary = P.AuroraDeep,
    scrim = Color(0xFF000000),
)

/**
 * The light scheme keeps the same aurora identity — teal primary, sky secondary
 * — over warm paper rather than midnight, so the brand survives the switch.
 */
private val RangeLightScheme = lightColorScheme(
    primary = Color(0xFF00705A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF8FF3D2),
    onPrimaryContainer = Color(0xFF00201A),
    secondary = Color(0xFF0F5FCB),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD3E3FF),
    onSecondaryContainer = Color(0xFF001A3D),
    tertiary = Color(0xFF8A5100),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDCB4),
    onTertiaryContainer = Color(0xFF2B1600),
    background = Color(0xFFF4F7FD),
    onBackground = Color(0xFF0C1421),
    surface = Color(0xFFF4F7FD),
    onSurface = Color(0xFF0C1421),
    surfaceVariant = Color(0xFFDFE7F3),
    onSurfaceVariant = Color(0xFF4A5872),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFEDF2FA),
    surfaceContainerHighest = Color(0xFFE1EAF7),
    outline = Color(0xFF74809A),
    outlineVariant = Color(0xFFD3DCEA),
    error = Color(0xFFB3261E),
    onError = Color.White,
    inverseSurface = Color(0xFF16202F),
    inverseOnSurface = Color(0xFFF0F4FB),
    inversePrimary = P.Aurora,
    scrim = Color(0xFF000000),
)

@Composable
fun RangeTheme(
    mode: ThemeMode = ThemeMode.DARK,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val dark = when (mode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val context = LocalContext.current
    val scheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> RangeDarkScheme
        else -> RangeLightScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !dark
            controller.isAppearanceLightNavigationBars = !dark
        }
    }

    CompositionLocalProvider(LocalIsDark provides dark) {
        MaterialTheme(
            colorScheme = scheme,
            shapes = RangeShapes,
            typography = RangeTypography,
            content = content,
        )
    }
}

/** Convenience alias so screens can read the scheme without importing MaterialTheme. */
val colors
    @Composable get() = MaterialTheme.colorScheme
