@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.animation.ExperimentalSharedTransitionApi::class,
    androidx.compose.ui.ExperimentalComposeUiApi::class,
)

package com.vythera.range.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Fixed brand colours, lifted off the app mark.
 *
 * These are **identity**, not theme. Since the app defaults to Material You,
 * the interface takes its colour from the user's wallpaper — but a logo that
 * changes colour per phone stops being a logo, and destination artwork is keyed
 * to a place's own character rather than to the UI around it.
 *
 * So the rule is: [RangePalette] is for the app mark and generated artwork.
 * Everything else in the interface reads [RangeAccents] or `colorScheme`
 * directly, so it follows the wallpaper.
 */
object RangePalette {
    val Aurora = Color(0xFF2FE39B)
    val AuroraBright = Color(0xFF5CF5B6)
    val AuroraDeep = Color(0xFF12A873)
    val Sky = Color(0xFF2C8FFF)
    val SkyDeep = Color(0xFF1358C8)
    val Lagoon = Color(0xFF19C6C6)
    val Ink = Color(0xFF04070F)
    val InkRaised = Color(0xFF0A1120)
    val InkCard = Color(0xFF101A2C)
    val InkLine = Color(0xFF1E2C45)
    val Mist = Color(0xFFE8F0FF)
    val MistDim = Color(0xFFA7B6D0)
    val Sand = Color(0xFFFFC46B)
    val Coral = Color(0xFFFF6B6B)
    val Violet = Color(0xFF9A6BFF)
}

/** Gradient stops for the app mark's orbit. Brand, so fixed. */
val OrbitGradient = listOf(
    RangePalette.SkyDeep,
    RangePalette.Sky,
    RangePalette.Lagoon,
    RangePalette.Aurora,
)

/**
 * The interface's semantic colours, resolved from whatever scheme is active.
 *
 * Every one of these used to be a hardcoded green, amber or coral. Pinning them
 * to scheme roles instead is what lets Material You actually reach the parts of
 * the UI that carry meaning — a trip that fits is `primary` whatever your
 * wallpaper is, and it stays legible because the scheme guarantees the contrast.
 */
object RangeAccents {
    /** Comfortably inside the budget. */
    val easy: Color @Composable get() = MaterialTheme.colorScheme.primary

    /** Inside the budget, but not by much. */
    val fits: Color @Composable get() = MaterialTheme.colorScheme.secondary

    /** Over, but within reach of a nudge. */
    val stretch: Color @Composable get() = MaterialTheme.colorScheme.tertiary

    /** Out of range — deliberately recessive. */
    val out: Color @Composable get() = MaterialTheme.colorScheme.outline

    /** Wishlist / favourite. */
    val wish: Color @Composable get() = MaterialTheme.colorScheme.error

    /** Secondary highlight for informational accents. */
    val info: Color @Composable get() = MaterialTheme.colorScheme.secondary

    /** Tertiary highlight, used for "best season" style call-outs. */
    val highlight: Color @Composable get() = MaterialTheme.colorScheme.tertiary

    /** Hairline / tick marks on instruments like the budget tape. */
    val instrument: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

    /** Dim companion to [instrument]. */
    val instrumentDim: Color
        @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
}

/**
 * The four verdict colours bundled together.
 *
 * Canvas code — the radar, the cost bars — draws inside a `DrawScope`, which is
 * not a composable scope and so cannot reach `MaterialTheme`. Rather than make
 * every drawing helper composable, the caller resolves this once and passes it
 * down.
 */
@androidx.compose.runtime.Immutable
data class VerdictColors(
    val easy: Color,
    val fits: Color,
    val stretch: Color,
    val out: Color,
)

val verdictColors: VerdictColors
    @Composable get() = VerdictColors(
        easy = RangeAccents.easy,
        fits = RangeAccents.fits,
        stretch = RangeAccents.stretch,
        out = RangeAccents.out,
    )

/**
 * Palette for the cost-breakdown bar. Five slices that must stay tellable apart
 * whatever the wallpaper is, so they walk the scheme's distinct hues rather
 * than shading one of them.
 */
@androidx.compose.runtime.Immutable
data class CostColors(
    val transport: Color,
    val stay: Color,
    val food: Color,
    val local: Color,
    val experiences: Color,
    val overhead: Color,
)

val costColors: CostColors
    @Composable get() = MaterialTheme.colorScheme.let { s ->
        CostColors(
            // The scheme's primary/secondary/tertiary are a harmonised triad —
            // they are built to sit together. The container roles are *background*
            // tones, and using them as foreground fills put an unrelated red and
            // blue next to each other in the same bar. The three biggest lines
            // get the accents; the minor ones step down through the neutrals,
            // which also reads as a visual hierarchy rather than six equals.
            transport = s.primary,
            stay = s.secondary,
            food = s.tertiary,
            local = s.onSurfaceVariant,
            experiences = s.outline,
            overhead = s.outlineVariant,
        )
    }

/** Verdict gradients, resolved from the active scheme. */
val affordableGradient: List<Color>
    @Composable get() = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.secondary,
    )

val stretchGradient: List<Color>
    @Composable get() = listOf(
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.72f),
    )

val outOfRangeGradient: List<Color>
    @Composable get() = listOf(
        MaterialTheme.colorScheme.outline,
        MaterialTheme.colorScheme.outlineVariant,
    )
