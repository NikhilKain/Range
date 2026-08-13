package com.vythera.range.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

private val Sans = FontFamily.SansSerif

private val trim = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

private fun style(
    size: Int,
    line: Int,
    weight: FontWeight,
    tracking: Double = 0.0,
) = TextStyle(
    fontFamily = Sans,
    fontSize = size.sp,
    lineHeight = line.sp,
    fontWeight = weight,
    letterSpacing = tracking.sp,
    lineHeightStyle = trim,
)

/**
 * Expressive type: display sizes are tightened and heavier than stock M3 so the
 * numbers on the budget dial read like a headline, not a label.
 */
val RangeTypography = Typography(
    displayLarge = style(60, 64, FontWeight.W800, (-1.6)),
    displayMedium = style(46, 52, FontWeight.W800, (-1.1)),
    displaySmall = style(36, 42, FontWeight.W700, (-0.7)),
    headlineLarge = style(32, 38, FontWeight.W700, (-0.5)),
    headlineMedium = style(26, 32, FontWeight.W700, (-0.3)),
    headlineSmall = style(22, 28, FontWeight.W700, (-0.2)),
    titleLarge = style(20, 26, FontWeight.W700, (-0.1)),
    titleMedium = style(16, 22, FontWeight.W600, 0.1),
    titleSmall = style(14, 20, FontWeight.W600, 0.1),
    bodyLarge = style(16, 24, FontWeight.W400, 0.15),
    bodyMedium = style(14, 20, FontWeight.W400, 0.2),
    bodySmall = style(12, 17, FontWeight.W400, 0.25),
    labelLarge = style(14, 18, FontWeight.W700, 0.3),
    labelMedium = style(12, 16, FontWeight.W700, 0.5),
    labelSmall = style(11, 14, FontWeight.W700, 0.6),
)
