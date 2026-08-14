@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.animation.ExperimentalSharedTransitionApi::class,
    androidx.compose.ui.ExperimentalComposeUiApi::class,
)

package com.vythera.range.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
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

/**
 * Fallback schemes for devices without Material You (Android 11 and below) and
 * for anyone who turns dynamic colour off.
 *
 * Deliberately a **neutral slate** rather than the old aurora green. When the
 * app's whole colour story is "borrow the wallpaper", the fallback should read
 * as a calm absence of opinion, not as a second competing brand — and a
 * desaturated ground is also the safest backdrop for the generated destination
 * artwork, which supplies all the colour these screens actually need.
 */
private val FallbackDarkScheme = darkColorScheme(
    primary = Color(0xFF9FCAF5),
    onPrimary = Color(0xFF003354),
    primaryContainer = Color(0xFF1B4A70),
    onPrimaryContainer = Color(0xFFCFE5FF),
    secondary = Color(0xFFBAC8D8),
    onSecondary = Color(0xFF24323F),
    secondaryContainer = Color(0xFF3A4856),
    onSecondaryContainer = Color(0xFFD6E4F4),
    tertiary = Color(0xFFD6BFA6),
    onTertiary = Color(0xFF3A2A18),
    tertiaryContainer = Color(0xFF52402C),
    onTertiaryContainer = Color(0xFFF3DEC3),
    background = Color(0xFF0E1114),
    onBackground = Color(0xFFE1E3E6),
    surface = Color(0xFF0E1114),
    onSurface = Color(0xFFE1E3E6),
    surfaceVariant = Color(0xFF40484F),
    onSurfaceVariant = Color(0xFFC0C8D0),
    surfaceContainerLowest = Color(0xFF090B0E),
    surfaceContainerLow = Color(0xFF14181C),
    surfaceContainer = Color(0xFF181C21),
    surfaceContainerHigh = Color(0xFF23282D),
    surfaceContainerHighest = Color(0xFF2E3338),
    outline = Color(0xFF8A9299),
    outlineVariant = Color(0xFF40484F),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    inverseSurface = Color(0xFFE1E3E6),
    inverseOnSurface = Color(0xFF2E3134),
    inversePrimary = Color(0xFF37618E),
    scrim = Color(0xFF000000),
)

private val FallbackLightScheme = lightColorScheme(
    primary = Color(0xFF37618E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD2E4FF),
    onPrimaryContainer = Color(0xFF001D33),
    secondary = Color(0xFF535F70),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD6E3F8),
    onSecondaryContainer = Color(0xFF101C2B),
    tertiary = Color(0xFF6B5A43),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF4DEC0),
    onTertiaryContainer = Color(0xFF241A07),
    background = Color(0xFFF8F9FC),
    onBackground = Color(0xFF191C20),
    surface = Color(0xFFF8F9FC),
    onSurface = Color(0xFF191C20),
    surfaceVariant = Color(0xFFDFE2EB),
    onSurfaceVariant = Color(0xFF43474E),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF2F3F7),
    surfaceContainer = Color(0xFFECEEF2),
    surfaceContainerHigh = Color(0xFFE6E8EC),
    surfaceContainerHighest = Color(0xFFE1E2E6),
    outline = Color(0xFF73777F),
    outlineVariant = Color(0xFFC3C6CF),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    inverseSurface = Color(0xFF2E3134),
    inverseOnSurface = Color(0xFFF0F1F4),
    inversePrimary = Color(0xFFA1C9FD),
    scrim = Color(0xFF000000),
)

@Composable
fun RangeTheme(
    mode: ThemeMode = ThemeMode.DARK,
    /** Material You. On by default — see [FallbackDarkScheme] for the alternative. */
    dynamicColor: Boolean = true,
    reduceMotion: Boolean = false,
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
        dark -> FallbackDarkScheme
        else -> FallbackLightScheme
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

    CompositionLocalProvider(
        LocalIsDark provides dark,
        com.vythera.range.ui.components.LocalReduceMotion provides reduceMotion,
    ) {
        // MaterialExpressiveTheme is what makes every M3 component in the app
        // move on springs rather than duration curves: buttons squash, sheets
        // settle with a little overshoot, selection changes carry momentum.
        // Handing it a MotionScheme is the whole point — swapping to the
        // standard scheme when the user asks for reduced motion turns the
        // bounce off everywhere at once, instead of per-animation.
        MaterialExpressiveTheme(
            colorScheme = scheme,
            motionScheme = if (reduceMotion) MotionScheme.standard() else MotionScheme.expressive(),
            shapes = RangeShapes,
            typography = RangeTypography,
            content = content,
        )
    }
}

/** Convenience alias so screens can read the scheme without importing MaterialTheme. */
val colors
    @Composable get() = MaterialTheme.colorScheme
