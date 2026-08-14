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
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vythera.range.data.model.Verdict
import com.vythera.range.domain.TripEstimate
import androidx.compose.material3.MaterialTheme
import com.vythera.range.ui.theme.VerdictColors
import com.vythera.range.ui.theme.verdictColors
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.sin

private const val SECTORS = 24

/**
 * The centrepiece: your origin at the middle, every destination plotted by its
 * true bearing and a log-scaled distance, and a soft "reach" boundary drawn
 * through the farthest thing you can afford in each direction.
 */
@Composable
fun RangeRadar(
    estimates: List<TripEstimate>,
    selectedId: String?,
    onSelect: (TripEstimate?) -> Unit,
    modifier: Modifier = Modifier,
    animated: Boolean = true,
) {
    val reveal by animateFloatAsState(
        targetValue = if (estimates.isEmpty()) 0f else 1f,
        animationSpec = tween(1100),
        label = "radarReveal",
    )
    val sweep = ambientPhase(durationMs = 9_000, steps = 150, label = "radarSweep") * 360f
    val pulse = ambientPhase(durationMs = 2_600, steps = 52, label = "radarPulse")

    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current

    val maxKm = remember(estimates) {
        max(2500.0, estimates.maxOfOrNull { it.distanceKm } ?: 2500.0)
    }

    val scheme = MaterialTheme.colorScheme
    val paint = RadarPaint(
        grid = scheme.outlineVariant,
        label = scheme.onSurfaceVariant,
        sweep = scheme.primary,
        fieldInner = scheme.primary,
        fieldOuter = scheme.secondary,
        origin = scheme.primary,
        marker = scheme.onSurface,
        verdicts = verdictColors,
    )

    Box(modifier.fillMaxWidth()) {
        Canvas(
            Modifier
                .matchParentSize()
                .pointerInput(estimates, maxKm) {
                    detectTapGestures { tap ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val radius = minOf(size.width, size.height) / 2f * 0.86f
                        val threshold = with(density) { 22.dp.toPx() }
                        val hit = estimates.minByOrNull { e ->
                            val p = plot(e, center, radius, maxKm)
                            hypot(p.x - tap.x, p.y - tap.y)
                        }
                        val distance = hit?.let {
                            val p = plot(it, center, radius, maxKm)
                            hypot(p.x - tap.x, p.y - tap.y)
                        } ?: Float.MAX_VALUE
                        onSelect(if (distance <= threshold) hit else null)
                    }
                },
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = minOf(size.width, size.height) / 2f * 0.86f

            drawGrid(center, radius, maxKm, measurer, reveal, paint)
            if (animated) drawSweep(center, radius, sweep, reveal, paint)
            drawReachBoundary(estimates, center, radius, maxKm, reveal, paint)
            drawDots(estimates, center, radius, maxKm, reveal, selectedId, pulse, paint)
            drawOrigin(center, pulse, reveal, paint)
        }
    }
}

/**
 * Every colour the radar draws with, resolved once per composition.
 *
 * The drawing below happens inside a `DrawScope`, which is not a composable
 * scope and cannot read `MaterialTheme`. Passing a resolved bundle down is what
 * lets the radar follow the Material You scheme instead of staying pinned to
 * fixed brand colours.
 */
private data class RadarPaint(
    val grid: Color,
    val label: Color,
    val sweep: Color,
    val fieldInner: Color,
    val fieldOuter: Color,
    val origin: Color,
    val marker: Color,
    val verdicts: VerdictColors,
)

private fun plot(e: TripEstimate, center: Offset, radius: Float, maxKm: Double): Offset {
    val r = radialFraction(e.distanceKm, maxKm) * radius
    val theta = Math.toRadians(e.bearing).toFloat()
    return Offset(center.x + r * sin(theta), center.y - r * cos(theta))
}

private fun radialFraction(km: Double, maxKm: Double): Float {
    val minKm = 120.0
    val v = (ln(km.coerceIn(minKm, maxKm)) - ln(minKm)) / (ln(maxKm) - ln(minKm))
    return (0.14 + 0.86 * v).toFloat()
}

private fun DrawScope.drawGrid(
    center: Offset,
    radius: Float,
    maxKm: Double,
    measurer: TextMeasurer,
    reveal: Float,
    paint: RadarPaint,
) {
    val rings = listOf(500.0, 1500.0, 4000.0, 10000.0, 18000.0).filter { it <= maxKm * 1.05 }
    rings.forEach { km ->
        val r = radialFraction(km, maxKm) * radius * reveal
        drawCircle(
            color = paint.grid.copy(alpha = 0.75f),
            radius = r,
            center = center,
            style = Stroke(
                width = 1f,
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 10f), 0f),
            ),
        )
        val label = if (km >= 1000) "${(km / 1000).toInt()}k" else "${km.toInt()}"
        val layout = measurer.measure(
            label,
            style = TextStyle(color = paint.label.copy(alpha = 0.55f), fontSize = 9.sp),
        )
        drawText(
            layout,
            topLeft = Offset(center.x + 4f, center.y - r - layout.size.height / 2f),
        )
    }
    // Compass spokes.
    listOf(0f, 45f, 90f, 135f, 180f, 225f, 270f, 315f).forEach { deg ->
        val theta = Math.toRadians(deg.toDouble()).toFloat()
        drawLine(
            color = paint.grid.copy(alpha = 0.45f),
            start = center,
            end = Offset(
                center.x + radius * reveal * sin(theta),
                center.y - radius * reveal * cos(theta),
            ),
            strokeWidth = 1f,
        )
    }
    listOf("N" to 0f, "E" to 90f, "S" to 180f, "W" to 270f).forEach { (label, deg) ->
        val theta = Math.toRadians(deg.toDouble()).toFloat()
        val layout = measurer.measure(
            label,
            style = TextStyle(color = paint.label.copy(alpha = 0.6f), fontSize = 10.sp),
        )
        drawText(
            layout,
            topLeft = Offset(
                center.x + (radius + 14f) * sin(theta) - layout.size.width / 2f,
                center.y - (radius + 14f) * cos(theta) - layout.size.height / 2f,
            ),
        )
    }
}

private fun DrawScope.drawSweep(
    center: Offset,
    radius: Float,
    angle: Float,
    reveal: Float,
    paint: RadarPaint,
) {
    rotate(angle, pivot = center) {
        drawArc(
            brush = Brush.sweepGradient(
                0f to Color.Transparent,
                0.06f to paint.sweep.copy(alpha = 0.16f * reveal),
                0.12f to Color.Transparent,
                center = center,
            ),
            startAngle = -90f,
            sweepAngle = 60f,
            useCenter = true,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
        )
    }
}

private fun DrawScope.drawReachBoundary(
    estimates: List<TripEstimate>,
    center: Offset,
    radius: Float,
    maxKm: Double,
    reveal: Float,
    paint: RadarPaint,
) {
    if (estimates.isEmpty()) return
    val affordable = estimates.filter { it.withinBudget }
    if (affordable.size < 3) return

    val sectorMax = FloatArray(SECTORS)
    affordable.forEach { e ->
        val idx = ((e.bearing / 360.0 * SECTORS).toInt()) % SECTORS
        val r = radialFraction(e.distanceKm, maxKm)
        if (r > sectorMax[idx]) sectorMax[idx] = r
    }
    // Smooth across empty sectors so the blob reads as a field, not a starburst.
    val smoothed = FloatArray(SECTORS)
    for (i in 0 until SECTORS) {
        var sum = 0f
        var weight = 0f
        for (k in -3..3) {
            val idx = ((i + k) % SECTORS + SECTORS) % SECTORS
            val w = 1f / (1f + kotlin.math.abs(k))
            sum += sectorMax[idx] * w
            weight += w
        }
        smoothed[i] = (sum / weight) * reveal
    }

    val path = Path()
    for (i in 0..SECTORS) {
        val idx = i % SECTORS
        val theta = Math.toRadians((idx * 360.0 / SECTORS)).toFloat()
        val r = smoothed[idx] * radius
        val p = Offset(center.x + r * sin(theta), center.y - r * cos(theta))
        if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
    }
    path.close()

    drawPath(
        path,
        brush = Brush.radialGradient(
            colors = listOf(
                paint.fieldInner.copy(alpha = 0.20f),
                paint.fieldOuter.copy(alpha = 0.10f),
                Color.Transparent,
            ),
            center = center,
            radius = radius,
        ),
    )
    drawPath(
        path,
        color = paint.fieldInner.copy(alpha = 0.55f * reveal),
        style = Stroke(width = 2f),
    )
}

private fun DrawScope.drawDots(
    estimates: List<TripEstimate>,
    center: Offset,
    radius: Float,
    maxKm: Double,
    reveal: Float,
    selectedId: String?,
    pulse: Float,
    paint: RadarPaint,
) {
    estimates.forEach { e ->
        val p = plot(e, center, radius, maxKm)
        val animated = Offset(
            center.x + (p.x - center.x) * reveal,
            center.y + (p.y - center.y) * reveal,
        )
        val base = paint.verdicts.of(e.verdict)
        val alpha = if (e.withinBudget) 1f else 0.42f
        val r = (2.2f + e.destination.popularity.toFloat() * 3.4f)
        if (e.withinBudget) {
            drawCircle(base.copy(alpha = 0.16f * alpha), radius = r * 2.6f, center = animated)
        }
        drawCircle(base.copy(alpha = alpha), radius = r, center = animated)

        if (e.destination.id == selectedId) {
            val ring = r + 6f + pulse * 10f
            drawCircle(
                color = base.copy(alpha = (1f - pulse) * 0.8f),
                radius = ring,
                center = animated,
                style = Stroke(width = 2f),
            )
            drawCircle(
                color = paint.marker,
                radius = r + 1.5f,
                center = animated,
                style = Stroke(width = 1.5f),
            )
        }
    }
}

private fun DrawScope.drawOrigin(
    center: Offset,
    pulse: Float,
    reveal: Float,
    paint: RadarPaint,
) {
    drawCircle(
        brush = Brush.radialGradient(
            listOf(paint.origin.copy(alpha = 0.5f), Color.Transparent),
            center = center,
            radius = 26f + pulse * 18f,
        ),
        radius = 26f + pulse * 18f,
        center = center,
    )
    drawCircle(color = paint.marker.copy(alpha = reveal), radius = 4.5f, center = center)
    drawCircle(
        color = paint.origin.copy(alpha = (1f - pulse) * reveal),
        radius = 8f + pulse * 26f,
        center = center,
        style = Stroke(width = 1.5f),
    )
}
