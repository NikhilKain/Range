package com.vythera.range.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/** Expressive shape scale: generous, pill-leaning corners across the board. */
val RangeShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(30.dp),
    extraLarge = RoundedCornerShape(40.dp),
)

val PillShape = RoundedCornerShape(percent = 50)
val CardShape = RoundedCornerShape(28.dp)
val SheetShape = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp)
