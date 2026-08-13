@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.vythera.range.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vythera.range.ui.theme.RangePalette
import com.vythera.range.ui.theme.lobedPoints
import com.vythera.range.ui.theme.smoothClosedPath
import kotlin.math.sin

/**
 * The Material 3 Expressive loading indicator: a filled shape that morphs
 * between lobe counts as it spins. Drawn by hand so the morph is continuous.
 */
@Composable
fun ExpressiveLoader(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    val t = rememberInfiniteTransition(label = "loader")
    val spin by t.animateFloat(
        0f,
        360f,
        infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart),
        label = "spin",
    )
    val morph by t.animateFloat(
        0f,
        1f,
        infiniteRepeatable(tween(1300, easing = LinearEasing), RepeatMode.Reverse),
        label = "morph",
    )
    Canvas(modifier) {
        val lobes = 4 + (morph * 5).toInt()
        val amp = 0.10f + 0.06f * sin(morph * Math.PI.toFloat())
        val r = minOf(size.width, size.height) / 2f / (1f + amp)
        val pts = lobedPoints(
            center = Offset(size.width / 2f, size.height / 2f),
            radius = r,
            lobes = lobes,
            amplitude = amp,
            rotationDeg = spin,
        )
        rotate(spin) { drawPath(smoothClosedPath(pts), color) }
    }
}

/** Expressive wavy progress track — the squiggle, drawn to any width. */
@Composable
fun WavyProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    amplitude: Dp = 4.dp,
    animated: Boolean = true,
) {
    val t = rememberInfiniteTransition(label = "wave")
    val phase by t.animateFloat(
        0f,
        (2 * Math.PI).toFloat(),
        infiniteRepeatable(tween(1800, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), tween(700), label = "wavyProgress")
    Canvas(modifier.height(amplitude * 3 + 6.dp)) {
        val midY = size.height / 2f
        val amp = amplitude.toPx()
        val strokeW = 5f
        val endX = size.width * p

        drawLine(
            trackColor,
            Offset(0f, midY),
            Offset(size.width, midY),
            strokeW,
            StrokeCap.Round,
        )

        if (endX > 2f) {
            val path = Path()
            path.moveTo(0f, midY)
            var x = 0f
            while (x <= endX) {
                val wave = sin(x / 22f + if (animated) phase else 0f) * amp
                path.lineTo(x, midY + wave)
                x += 3f
            }
            drawPath(
                path,
                brush = Brush.horizontalGradient(
                    listOf(color.copy(alpha = 0.75f), color),
                    endX = endX.coerceAtLeast(1f),
                ),
                style = Stroke(width = strokeW, cap = StrokeCap.Round),
            )
        }
    }
}

/**
 * Connected button group: the expressive replacement for a segmented control.
 * The selected cell swells and rounds, its neighbours squeeze to make room.
 */
@Composable
fun <T> ButtonGroup(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    icon: ((T) -> ImageVector?)? = null,
    accent: Color = MaterialTheme.colorScheme.primary,
) {
    val haptics = LocalHapticFeedback.current
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        options.forEach { option ->
            val active = option == selected
            val weight by animateFloatAsState(
                if (active) 1.45f else 1f,
                Motion.springOf(0.6f, 340f),
                label = "groupWeight",
            )
            val corner by animateFloatAsState(
                if (active) 26f else 14f,
                Motion.springOf(0.6f, 340f),
                label = "groupCorner",
            )
            val bg by animateColorAsState(
                if (active) accent else MaterialTheme.colorScheme.surfaceContainerHighest,
                tween(280),
                label = "groupBg",
            )
            val fg by animateColorAsState(
                if (active) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                tween(280),
                label = "groupFg",
            )
            Column(
                Modifier
                    .weight(weight)
                    .height(58.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(corner.dp))
                    .background(bg)
                    .clickable {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSelect(option)
                    }
                    .padding(horizontal = 6.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                icon?.invoke(option)?.let {
                    Icon(it, null, tint = fg, modifier = Modifier.size(18.dp))
                }
                Text(
                    label(option),
                    style = MaterialTheme.typography.labelLarge,
                    color = fg,
                    fontWeight = if (active) FontWeight.W800 else FontWeight.W600,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                )
            }
        }
    }
}

/** A big, friendly tile — the expressive way to show one fact you can tap. */
@Composable
fun FactTile(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    onContainer: Color = MaterialTheme.colorScheme.onSurface,
    accent: Color = MaterialTheme.colorScheme.primary,
    onClick: (() -> Unit)? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val interaction = rememberInteraction()
    Column(
        modifier
            .pressScale(interaction)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(26.dp))
            .background(container)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interaction,
                        indication = null,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(17.dp))
            androidx.compose.foundation.layout.Spacer(Modifier.size(7.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = onContainer.copy(alpha = 0.7f),
            )
        }
        androidx.compose.foundation.layout.Spacer(Modifier.height(6.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                value,
                style = MaterialTheme.typography.titleMedium,
                color = onContainer,
                fontWeight = FontWeight.W700,
                maxLines = 1,
            )
            trailing?.invoke()
        }
    }
}

/** Decorative blob used behind hero numbers to add expressive colour. */
@Composable
fun ShapeBlob(
    modifier: Modifier = Modifier,
    color: Color = RangePalette.Aurora,
    lobes: Int = 7,
    amplitude: Float = 0.1f,
    spinSeconds: Int = 26,
) {
    val t = rememberInfiniteTransition(label = "blob")
    val spin by t.animateFloat(
        0f,
        360f,
        infiniteRepeatable(tween(spinSeconds * 1000, easing = LinearEasing), RepeatMode.Restart),
        label = "blobSpin",
    )
    Canvas(modifier) {
        val r = minOf(size.width, size.height) / 2f / (1f + amplitude)
        val pts = lobedPoints(
            Offset(size.width / 2f, size.height / 2f),
            r,
            lobes,
            amplitude,
            spin,
        )
        drawPath(smoothClosedPath(pts), color)
    }
}

@Composable
fun ExpressiveDivider(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier.fillMaxWidth().height(10.dp)) {
        val path = Path()
        val midY = size.height / 2f
        path.moveTo(0f, midY)
        var x = 0f
        while (x <= size.width) {
            path.lineTo(x, midY + sin(x / 16f) * 3f)
            x += 3f
        }
        drawPath(path, color, style = Stroke(width = 2f, cap = StrokeCap.Round))
    }
}

/** Full-bleed loading state for the explore screen. */
@Composable
fun LoadingPane(message: String, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            ExpressiveLoader(Modifier.size(64.dp))
            androidx.compose.foundation.layout.Spacer(Modifier.height(18.dp))
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
