@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.animation.ExperimentalSharedTransitionApi::class,
    androidx.compose.ui.ExperimentalComposeUiApi::class,
)

package com.vythera.range.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.material3.MaterialTheme
import com.vythera.range.ui.theme.LocalIsDark
import com.vythera.range.ui.theme.RangePalette
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * The living backdrop: two slow aurora blooms drifting against a starfield, with
 * a faint orbit arc echoing the app mark. Cheap to draw — three gradients and a
 * few dozen points — but it makes every screen feel awake.
 */
@Composable
fun AuroraBackground(
    modifier: Modifier = Modifier,
    intensity: Float = 1f,
    animated: Boolean = true,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val phase = if (animated) {
        ambientPhase(durationMs = 38_000, steps = 190, label = "aurora")
    } else {
        0.22f
    }

    val stars = remember {
        val rnd = Random(7)
        List(44) {
            Triple(rnd.nextFloat(), rnd.nextFloat(), rnd.nextFloat() * 0.7f + 0.3f)
        }
    }

    val dark = LocalIsDark.current
    val base = MaterialTheme.colorScheme.background
    val bloomA = if (dark) RangePalette.Sky else RangePalette.Lagoon
    val bloomB = RangePalette.Aurora

    Box(modifier.background(base)) {
        Canvas(Modifier.fillMaxSize()) {
            drawAurora(phase, intensity * if (dark) 1f else 0.55f, bloomA, bloomB, base, dark)
            if (dark) drawStars(stars, phase)
        }
        content()
    }
}

private fun DrawScope.drawAurora(
    phase: Float,
    intensity: Float,
    bloomA: Color,
    bloomB: Color,
    base: Color,
    dark: Boolean,
) {
    val w = size.width
    val h = size.height
    val a1 = phase * 2f * Math.PI.toFloat()
    val a2 = a1 * 0.63f + 1.4f

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                bloomA.copy(alpha = 0.28f * intensity),
                bloomA.copy(alpha = 0.10f * intensity),
                Color.Transparent,
            ),
            center = Offset(w * (0.22f + 0.14f * cos(a1)), h * (0.14f + 0.06f * sin(a1))),
            radius = w * 0.95f,
        ),
        radius = w * 0.95f,
        center = Offset(w * (0.22f + 0.14f * cos(a1)), h * (0.14f + 0.06f * sin(a1))),
    )

    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                bloomB.copy(alpha = 0.20f * intensity),
                bloomB.copy(alpha = 0.08f * intensity),
                Color.Transparent,
            ),
            center = Offset(w * (0.86f + 0.10f * sin(a2)), h * (0.30f + 0.10f * cos(a2))),
            radius = w * 0.8f,
        ),
        radius = w * 0.8f,
        center = Offset(w * (0.86f + 0.10f * sin(a2)), h * (0.30f + 0.10f * cos(a2))),
    )

    // Settle the bottom back to the page colour so content stays legible.
    drawRect(
        brush = Brush.verticalGradient(
            0f to Color.Transparent,
            0.55f to base.copy(alpha = if (dark) 0.55f else 0.75f),
            1f to base,
        ),
    )
}

private fun DrawScope.drawStars(stars: List<Triple<Float, Float, Float>>, phase: Float) {
    stars.forEachIndexed { i, (x, y, seed) ->
        val twinkle = 0.35f + 0.65f * ((sin(phase * 6.28f * (0.4f + seed) + i) + 1f) / 2f)
        drawCircle(
            color = Color.White.copy(alpha = 0.06f + 0.16f * twinkle * seed),
            radius = (0.7f + seed * 1.5f),
            center = Offset(x * size.width, y * size.height * 0.75f),
        )
    }
}
