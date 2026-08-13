@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.animation.ExperimentalSharedTransitionApi::class,
    androidx.compose.ui.ExperimentalComposeUiApi::class,
)

package com.vythera.range.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Range's palette is lifted straight off the app mark: a deep midnight plate, an
 * orbit that runs sky-blue into aurora-green, and a warm signal colour for money.
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

/** Gradient stops used for the orbit, range rings and hero washes. */
val OrbitGradient = listOf(
    RangePalette.SkyDeep,
    RangePalette.Sky,
    RangePalette.Lagoon,
    RangePalette.Aurora,
)

val AffordableGradient = listOf(RangePalette.Aurora, RangePalette.Lagoon)
val StretchGradient = listOf(RangePalette.Sand, Color(0xFFFF9A5B))
val OutOfRangeGradient = listOf(Color(0xFF4A5975), Color(0xFF35415A))
