@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.animation.ExperimentalSharedTransitionApi::class,
    androidx.compose.ui.ExperimentalComposeUiApi::class,
)

package com.vythera.range.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import com.vythera.range.data.model.Destination
import com.vythera.range.data.model.Vibe
import kotlin.math.abs
import kotlin.math.sin
import kotlin.random.Random

/**
 * Every destination gets hand-drawn-looking artwork generated from its vibe and
 * a seed off its id — skylines for cities, layered peaks for mountains, dunes
 * for deserts. No image assets, no network, and no two cards alike.
 */
@Composable
fun DestinationArt(
    destination: Destination,
    modifier: Modifier = Modifier,
    parallax: Float = 0f,
    detail: Boolean = false,
) {
    Canvas(modifier) {
        val g = destination.gradient
        drawRect(
            brush = Brush.verticalGradient(
                listOf(g.first(), g.last(), g.last().copy(alpha = 0.92f)),
            ),
        )
        val seed = destination.id.hashCode()
        val scene = sceneFor(destination.vibes)
        drawScene(scene, seed, g, parallax, detail)
    }
}

private enum class Scene { COAST, PEAKS, SKYLINE, DUNES, DOMES, HILLS }

/**
 * Scene and palette must agree, so both resolve the destination's dominant vibe
 * the same way: by declaration order in [Vibe].
 */
private fun sceneFor(vibes: Set<Vibe>): Scene {
    val dominant = Vibe.entries.firstOrNull { it in vibes } ?: Vibe.CITY
    return when (dominant) {
        Vibe.BEACH, Vibe.ISLAND -> Scene.COAST
        Vibe.MOUNTAIN, Vibe.SNOW -> Scene.PEAKS
        Vibe.DESERT -> Scene.DUNES
        Vibe.HERITAGE, Vibe.SPIRITUAL -> Scene.DOMES
        Vibe.NATURE, Vibe.WILDLIFE -> Scene.HILLS
        Vibe.ROADTRIP, Vibe.ADVENTURE -> Scene.PEAKS
        else -> Scene.SKYLINE
    }
}

private fun DrawScope.drawScene(
    scene: Scene,
    seed: Int,
    gradient: List<Color>,
    parallax: Float,
    detail: Boolean,
) {
    @Suppress("NAME_SHADOWING")
    val rnd = Random(seed)
    val w = size.width
    val h = size.height
    val ink = androidx.compose.ui.graphics.lerp(gradient.last(), Color(0xFF04070F), 0.62f)

    // A sun / moon disc, always present, drifting with parallax.
    val sunX = w * (0.18f + 0.6f * rnd.nextFloat()) + parallax * 18f
    val sunY = h * (0.24f + 0.14f * rnd.nextFloat())
    val sunR = h * (if (detail) 0.16f else 0.14f)
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0.06f), Color.Transparent),
            center = Offset(sunX, sunY),
            radius = sunR * 2.6f,
        ),
        radius = sunR * 2.6f,
        center = Offset(sunX, sunY),
    )
    drawCircle(Color.White.copy(alpha = 0.82f), radius = sunR * 0.52f, center = Offset(sunX, sunY))

    when (scene) {
        Scene.COAST -> {
            val waterTop = h * 0.62f
            drawRect(
                brush = Brush.verticalGradient(
                    listOf(gradient.last().copy(alpha = 0.1f), ink.copy(alpha = 0.55f)),
                    startY = waterTop,
                    endY = h,
                ),
                topLeft = Offset(0f, waterTop),
                size = Size(w, h - waterTop),
            )
            for (i in 0 until 6) {
                val y = waterTop + (h - waterTop) * (i + 1) / 7f
                val path = Path()
                path.moveTo(0f, y)
                var x = 0f
                while (x <= w) {
                    val amp = 3f + i * 1.1f
                    path.lineTo(x, y + sin((x / w) * 12f + i + parallax) * amp)
                    x += 8f
                }
                drawPath(path, Color.White.copy(alpha = 0.10f + i * 0.015f), style = androidx.compose.ui.graphics.drawscope.Stroke(1.4f))
            }
            // A distant headland.
            val land = Path().apply {
                moveTo(w * 0.68f, waterTop)
                lineTo(w * 0.80f, waterTop - h * 0.16f)
                lineTo(w * 0.92f, waterTop - h * 0.05f)
                lineTo(w, waterTop - h * 0.12f)
                lineTo(w, waterTop)
                close()
            }
            drawPath(land, ink.copy(alpha = 0.5f))
        }

        Scene.PEAKS -> {
            repeat(3) { layer ->
                val base = h * (0.95f - layer * 0.06f)
                val height = h * (0.36f + layer * 0.14f)
                val alpha = 0.28f + layer * 0.24f
                val path = Path()
                path.moveTo(-w * 0.1f, base)
                var x = -w * 0.1f
                var up = true
                val peaks = 3 + layer
                val stepX = (w * 1.2f) / (peaks * 2f)
                repeat(peaks * 2) {
                    x += stepX
                    val y = if (up) base - height * (0.6f + 0.4f * rnd.nextFloat()) else base - height * 0.12f
                    path.lineTo(x - parallax * (layer + 1) * 4f, y)
                    up = !up
                }
                path.lineTo(w * 1.2f, h)
                path.lineTo(-w * 0.1f, h)
                path.close()
                drawPath(path, ink.copy(alpha = alpha))
            }
        }

        Scene.SKYLINE -> {
            val base = h * 0.98f
            repeat(2) { layer ->
                var x = -10f
                val alpha = if (layer == 0) 0.35f else 0.62f
                val maxH = h * (if (layer == 0) 0.42f else 0.62f)
                while (x < w + 10f) {
                    val bw = w * (0.05f + 0.06f * rnd.nextFloat())
                    val bh = maxH * (0.35f + 0.65f * rnd.nextFloat())
                    drawRect(
                        color = ink.copy(alpha = alpha),
                        topLeft = Offset(x - parallax * (layer + 1) * 3f, base - bh),
                        size = Size(bw, bh),
                    )
                    if (layer == 1) {
                        var wy = base - bh + 8f
                        while (wy < base - 8f) {
                            var wx = x + 5f
                            while (wx < x + bw - 5f) {
                                if (rnd.nextFloat() > 0.55f) {
                                    drawRect(
                                        color = Color(0xFFFFD98A).copy(alpha = 0.5f),
                                        topLeft = Offset(wx - parallax * 6f, wy),
                                        size = Size(2.4f, 3.4f),
                                    )
                                }
                                wx += 7f
                            }
                            wy += 9f
                        }
                    }
                    x += bw + w * 0.012f
                }
            }
        }

        Scene.DUNES -> {
            repeat(3) { layer ->
                val base = h * (0.72f + layer * 0.1f)
                val path = Path()
                path.moveTo(0f, h)
                path.lineTo(0f, base)
                var x = 0f
                while (x <= w) {
                    val y = base + sin((x / w) * (2.2f + layer) + layer * 1.7f + parallax * 0.2f) * h * 0.09f
                    path.lineTo(x, y)
                    x += 10f
                }
                path.lineTo(w, h)
                path.close()
                drawPath(path, ink.copy(alpha = 0.25f + layer * 0.2f))
            }
        }

        Scene.DOMES -> {
            val base = h * 0.95f
            val count = 4
            for (i in 0 until count) {
                val cx = w * ((i + 0.5f) / count) + (rnd.nextFloat() - 0.5f) * w * 0.05f - parallax * 3f
                val r = h * (0.12f + 0.08f * rnd.nextFloat())
                val towerH = h * (0.22f + 0.2f * rnd.nextFloat())
                drawRect(
                    color = ink.copy(alpha = 0.55f),
                    topLeft = Offset(cx - r, base - towerH),
                    size = Size(r * 2, towerH),
                )
                drawArc(
                    color = ink.copy(alpha = 0.55f),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = Offset(cx - r, base - towerH - r),
                    size = Size(r * 2, r * 2),
                )
                drawLine(
                    color = ink.copy(alpha = 0.55f),
                    start = Offset(cx, base - towerH - r),
                    end = Offset(cx, base - towerH - r - h * 0.07f),
                    strokeWidth = 2.2f,
                )
            }
        }

        Scene.HILLS -> {
            repeat(3) { layer ->
                val base = h * (0.66f + layer * 0.12f)
                val path = Path()
                path.moveTo(0f, h)
                path.lineTo(0f, base)
                var x = 0f
                while (x <= w) {
                    val y = base - abs(sin((x / w) * (3f + layer) + layer)) * h * 0.14f
                    path.lineTo(x - parallax * (layer + 1) * 2f, y)
                    x += 12f
                }
                path.lineTo(w, h)
                path.close()
                drawPath(path, ink.copy(alpha = 0.22f + layer * 0.2f))
                if (layer == 2) {
                    repeat(5) {
                        val tx = w * rnd.nextFloat()
                        val ty = base - h * 0.02f
                        drawLine(ink.copy(alpha = 0.7f), Offset(tx, ty), Offset(tx, ty - h * 0.08f), 2f)
                        drawCircle(ink.copy(alpha = 0.7f), h * 0.035f, Offset(tx, ty - h * 0.1f))
                    }
                }
            }
        }
    }

    // Vignette so text always sits on something dark enough.
    drawRect(
        brush = Brush.verticalGradient(
            0f to Color.Transparent,
            0.55f to Color.Black.copy(alpha = 0.18f),
            1f to Color.Black.copy(alpha = 0.62f),
        ),
    )
}
