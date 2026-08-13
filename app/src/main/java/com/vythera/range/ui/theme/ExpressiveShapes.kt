package com.vythera.range.ui.theme

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * Expressive shapes, built by hand so they can be morphed frame by frame.
 *
 * A closed Catmull-Rom spline through polar samples gives the soft "cookie" and
 * "clover" silhouettes Material 3 Expressive leans on, and because the sampler
 * takes a lobe count and an amplitude, any two shapes can be interpolated.
 */
fun smoothClosedPath(points: List<Offset>): Path {
    val path = Path()
    if (points.size < 3) return path
    val n = points.size
    path.moveTo(points[0].x, points[0].y)
    for (i in 0 until n) {
        val p0 = points[(i - 1 + n) % n]
        val p1 = points[i]
        val p2 = points[(i + 1) % n]
        val p3 = points[(i + 2) % n]
        // Catmull-Rom to cubic Bezier control points.
        val c1 = Offset(p1.x + (p2.x - p0.x) / 6f, p1.y + (p2.y - p0.y) / 6f)
        val c2 = Offset(p2.x - (p3.x - p1.x) / 6f, p2.y - (p3.y - p1.y) / 6f)
        path.cubicTo(c1.x, c1.y, c2.x, c2.y, p2.x, p2.y)
    }
    path.close()
    return path
}

/** Samples a lobed rosette: amplitude 0 is a circle, 0.18 is a plump cookie. */
fun lobedPoints(
    center: Offset,
    radius: Float,
    lobes: Int,
    amplitude: Float,
    rotationDeg: Float = 0f,
    samples: Int = 0,
): List<Offset> {
    val count = if (samples > 0) samples else lobes * 6
    val rot = rotationDeg * PI.toFloat() / 180f
    return (0 until count).map { i ->
        val t = i.toFloat() / count * 2f * PI.toFloat()
        val r = radius * (1f + amplitude * cos(lobes * (t - rot)))
        Offset(center.x + r * cos(t), center.y + r * sin(t))
    }
}

/** A lobed shape usable anywhere a Compose [Shape] is accepted. */
class LobedShape(
    private val lobes: Int,
    private val amplitude: Float,
    private val rotationDeg: Float = 0f,
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val r = minOf(size.width, size.height) / 2f / (1f + amplitude)
        val center = Offset(size.width / 2f, size.height / 2f)
        return Outline.Generic(
            smoothClosedPath(lobedPoints(center, r, lobes, amplitude, rotationDeg)),
        )
    }
}

/** Superellipse — the squircle used for tiles and thumbnails. */
class SquircleShape(private val exponent: Float = 4f) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val a = size.width / 2f
        val b = size.height / 2f
        val steps = 96
        val pts = (0 until steps).map { i ->
            val t = i.toFloat() / steps * 2f * PI
            val ct = cos(t)
            val st = sin(t)
            val x = a * ct.absPow(2f / exponent) * if (ct < 0) -1f else 1f
            val y = b * st.absPow(2f / exponent) * if (st < 0) -1f else 1f
            Offset(a + x, b + y)
        }
        return Outline.Generic(smoothClosedPath(pts))
    }
}

private fun Double.absPow(p: Float): Float = kotlin.math.abs(this).pow(p.toDouble()).toFloat()

object ExpressiveShapes {
    val cookie = LobedShape(lobes = 9, amplitude = 0.075f)
    val clover = LobedShape(lobes = 4, amplitude = 0.13f)
    val flower = LobedShape(lobes = 6, amplitude = 0.11f)
    val squircle = SquircleShape(4.2f)
    val softSquircle = SquircleShape(5.5f)
}
