package com.vythera.range.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf

/** Set from settings; when true every ambient animation holds still. */
val LocalReduceMotion = staticCompositionLocalOf { false }

/**
 * Ambient background art doesn't need 60 fps.
 *
 * A drifting aurora or a slowly turning blob looks identical stepped at 5–20 fps,
 * but invalidating a full-screen canvas every frame costs real battery and, on
 * weak GPUs, starves the rest of the system. This returns a phase that only
 * changes [steps] times per cycle, so the canvas redraws that often and no more.
 */
@Composable
fun ambientPhase(
    durationMs: Int,
    steps: Int,
    label: String = "ambient",
    hold: Float = 0.2f,
): Float {
    if (LocalReduceMotion.current) return hold
    val transition = rememberInfiniteTransition(label = label)
    val raw: State<Float> = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "${label}Raw",
    )
    val quantized by remember(steps) {
        derivedStateOf { (raw.value * steps).toInt() / steps.toFloat() }
    }
    return quantized
}
