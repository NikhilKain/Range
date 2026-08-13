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
import com.vythera.range.ui.theme.RangePalette as P

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

private val RangeLightScheme = lightColorScheme(
    primary = Color(0xFF00785A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFA9F5D4),
    onPrimaryContainer = Color(0xFF00251A),
    secondary = Color(0xFF0F5FCB),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD5E4FF),
    onSecondaryContainer = Color(0xFF001B3D),
    tertiary = Color(0xFF8A5100),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFDDB6),
    onTertiaryContainer = Color(0xFF2C1600),
    background = Color(0xFFF6F9FF),
    onBackground = Color(0xFF0B1220),
    surface = Color(0xFFF6F9FF),
    onSurface = Color(0xFF0B1220),
    surfaceVariant = Color(0xFFE1E8F5),
    onSurfaceVariant = Color(0xFF44506A),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF1F5FE),
    surfaceContainer = Color(0xFFEBF1FC),
    surfaceContainerHigh = Color(0xFFE4ECF9),
    surfaceContainerHighest = Color(0xFFDDE6F6),
    outline = Color(0xFF74809A),
    outlineVariant = Color(0xFFC5CFE2),
    error = Color(0xFFB3261E),
    onError = Color.White,
)

/**
 * Range always renders its own midnight identity unless the user opts into
 * dynamic colour, in which case we hand over to the wallpaper palette.
 */
@Composable
fun RangeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val scheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        darkTheme -> RangeDarkScheme
        else -> RangeLightScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialExpressiveTheme(
        colorScheme = scheme,
        motionScheme = MotionScheme.expressive(),
        shapes = RangeShapes,
        typography = RangeTypography,
        content = content,
    )
}

/** Convenience alias so screens can read the scheme without importing MaterialTheme. */
val colors
    @Composable get() = MaterialTheme.colorScheme
