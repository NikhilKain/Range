@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import com.vythera.range.ui.theme.RangePalette
import kotlin.math.cos
import kotlin.math.sin

/**
 * The app mark, redrawn in Compose so it can animate: the orbit sweeps in, the
 * plane rides it, and the pin drops with a bounce.
 */
@Composable
fun RangeMark(
    modifier: Modifier = Modifier,
    progress: Float = 1f,
    orbiting: Boolean = true,
) {
    val transition = rememberInfiniteTransition(label = "mark")
    val orbit by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(7000, easing = LinearEasing), RepeatMode.Restart),
        label = "orbit",
    )
    val planeT = if (orbiting) orbit else 0.14f

    Canvas(modifier) {
        val r = minOf(size.width, size.height) / 2f * 0.82f
        val c = Offset(size.width / 2f, size.height / 2f)

        // Globe body.
        drawCircle(
            brush = Brush.radialGradient(
                listOf(
                    RangePalette.SkyDeep.copy(alpha = 0.35f * progress),
                    RangePalette.Ink.copy(alpha = 0.9f * progress),
                ),
                center = c,
                radius = r * 0.86f,
            ),
            radius = r * 0.78f,
            center = c,
        )

        // Latitude hints.
        for (i in 1..3) {
            val ry = r * 0.78f * (i / 4f)
            drawArc(
                color = RangePalette.Sky.copy(alpha = 0.18f * progress),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(c.x - r * 0.78f, c.y - ry),
                size = Size(r * 1.56f, ry * 2),
                style = Stroke(width = 1f),
            )
        }

        // The orbit ring, drawn on with progress.
        rotate(-24f, pivot = c) {
            drawArc(
                brush = Brush.sweepGradient(
                    0f to RangePalette.Sky,
                    0.35f to RangePalette.Lagoon,
                    0.62f to RangePalette.Aurora,
                    1f to RangePalette.Sky,
                    center = c,
                ),
                startAngle = 130f,
                sweepAngle = 330f * progress,
                useCenter = false,
                topLeft = Offset(c.x - r, c.y - r),
                size = Size(r * 2, r * 2),
                style = Stroke(width = r * 0.13f, cap = StrokeCap.Round),
            )
        }

        // Plane riding the orbit.
        if (progress > 0.6f) {
            val angle = Math.toRadians((130.0 + 330.0 * planeT - 24.0))
            val px = c.x + r * cos(angle).toFloat()
            val py = c.y + r * sin(angle).toFloat()
            val heading = Math.toDegrees(angle).toFloat() + 90f
            rotate(heading, pivot = Offset(px, py)) {
                val s = r * 0.26f
                val plane = Path().apply {
                    moveTo(px, py - s)
                    lineTo(px + s * 0.62f, py + s * 0.45f)
                    lineTo(px, py + s * 0.18f)
                    lineTo(px - s * 0.62f, py + s * 0.45f)
                    close()
                }
                drawPath(plane, Color.White.copy(alpha = progress))
            }
        }

        // Pin.
        val pinScale = progress
        val pinR = r * 0.30f * pinScale
        val pinCenter = Offset(c.x, c.y - r * 0.30f)
        val pin = Path().apply {
            moveTo(pinCenter.x, pinCenter.y + pinR * 2.1f)
            cubicTo(
                pinCenter.x - pinR * 1.35f, pinCenter.y + pinR * 0.55f,
                pinCenter.x - pinR * 1.15f, pinCenter.y - pinR * 1.15f,
                pinCenter.x, pinCenter.y - pinR * 1.1f,
            )
            cubicTo(
                pinCenter.x + pinR * 1.15f, pinCenter.y - pinR * 1.15f,
                pinCenter.x + pinR * 1.35f, pinCenter.y + pinR * 0.55f,
                pinCenter.x, pinCenter.y + pinR * 2.1f,
            )
            close()
        }
        drawPath(
            pin,
            brush = Brush.verticalGradient(
                listOf(RangePalette.AuroraBright, RangePalette.Aurora),
                startY = pinCenter.y - pinR,
                endY = pinCenter.y + pinR * 2f,
            ),
        )
        drawCircle(RangePalette.Ink, radius = pinR * 0.42f, center = Offset(pinCenter.x, pinCenter.y))
        drawCircle(
            RangePalette.Lagoon.copy(alpha = progress),
            radius = r * 0.055f,
            center = Offset(c.x, c.y + r * 0.16f),
        )
    }
}

/** Wordmark: RANGE with the A drawn as the brand chevron. */
@Composable
fun RangeWordmark(modifier: Modifier = Modifier, tint: Color = RangePalette.Mist) {
    Canvas(modifier) {
        val h = size.height
        val strokeW = h * 0.11f
        val gap = h * 0.30f
        var x = 0f
        val letters = 5
        val letterW = (size.width - gap * (letters - 1)) / letters

        fun line(x1: Float, y1: Float, x2: Float, y2: Float, color: Color = tint) {
            drawLine(color, Offset(x1, y1), Offset(x2, y2), strokeW, StrokeCap.Round)
        }

        // R
        line(x, h, x, 0f)
        drawArc(
            color = tint,
            startAngle = -90f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(x, 0f),
            size = Size(letterW, h * 0.52f),
            style = Stroke(strokeW, cap = StrokeCap.Round),
        )
        line(x + letterW * 0.35f, h * 0.52f, x + letterW, h)
        x += letterW + gap

        // A — brand chevron in gradient.
        val chevron = Brush.verticalGradient(listOf(RangePalette.Aurora, RangePalette.Sky))
        drawLine(chevron, Offset(x, h), Offset(x + letterW / 2f, 0f), strokeW, StrokeCap.Round)
        drawLine(chevron, Offset(x + letterW / 2f, 0f), Offset(x + letterW, h), strokeW, StrokeCap.Round)
        x += letterW + gap

        // N
        line(x, h, x, 0f)
        line(x, 0f, x + letterW, h)
        line(x + letterW, h, x + letterW, 0f)
        x += letterW + gap

        // G
        drawArc(
            color = tint,
            startAngle = -35f,
            sweepAngle = 290f,
            useCenter = false,
            topLeft = Offset(x, 0f),
            size = Size(letterW, h),
            style = Stroke(strokeW, cap = StrokeCap.Round),
        )
        line(x + letterW * 0.55f, h * 0.55f, x + letterW, h * 0.55f)
        line(x + letterW, h * 0.55f, x + letterW, h * 0.82f)
        x += letterW + gap

        // E
        line(x, 0f, x, h)
        line(x, 0f, x + letterW, 0f)
        line(x, h * 0.5f, x + letterW * 0.82f, h * 0.5f)
        line(x, h, x + letterW, h)
    }
}
