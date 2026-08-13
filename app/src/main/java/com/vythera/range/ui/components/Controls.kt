package com.vythera.range.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.rememberSplineBasedDecay
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.animateDecay
import com.vythera.range.domain.Currency
import com.vythera.range.ui.theme.PillShape
import com.vythera.range.ui.theme.RangePalette
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * A tape-measure budget input. Drag it and the ruler scrolls with a tick of
 * haptics at every notch — far more satisfying than a slider, and far more
 * precise than a stepper.
 */
@Composable
fun BudgetTape(
    valueUsd: Double,
    currency: Currency,
    onValueChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
    minUsd: Double = 40.0,
    maxUsd: Double = 24_000.0,
) {
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val measurer = rememberTextMeasurer()
    val scope = rememberCoroutineScope()
    val decay = rememberSplineBasedDecay<Float>()

    val stepLocal = currency.step.toDouble()
    val stepUsd = stepLocal / currency.perUsd
    val pxPerStep = with(density) { 26.dp.toPx() }

    var lastTick by remember { mutableFloatStateOf(valueUsd.toFloat()) }

    fun applyDelta(dxPx: Float) {
        val deltaUsd = -(dxPx / pxPerStep) * stepUsd
        val next = (valueUsd + deltaUsd).coerceIn(minUsd, maxUsd)
        if (abs(next - lastTick) >= stepUsd * 0.98) {
            lastTick = next.toFloat()
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        onValueChange(next)
    }

    val dragState = rememberDraggableState { delta -> applyDelta(delta) }

    Column(modifier) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(62.dp)
                .draggable(
                    state = dragState,
                    orientation = Orientation.Horizontal,
                    onDragStopped = { velocity ->
                        scope.launch {
                            var last = 0f
                            AnimationState(initialValue = 0f, initialVelocity = velocity / 2.2f)
                                .animateDecay(decay) {
                                    val d = value - last
                                    last = value
                                    applyDelta(d)
                                }
                        }
                    },
                ),
        ) {
            val cx = size.width / 2f
            val localValue = valueUsd * currency.perUsd
            val stepsFromZero = localValue / stepLocal

            val firstStep = (stepsFromZero - (cx / pxPerStep) - 1).toInt()
            val lastStep = (stepsFromZero + (cx / pxPerStep) + 1).toInt()

            for (s in firstStep..lastStep) {
                if (s < 0) continue
                val x = cx + ((s - stepsFromZero) * pxPerStep).toFloat()
                val major = s % 5 == 0
                val distance = abs(x - cx) / cx
                val fade = (1f - distance).coerceIn(0f, 1f)
                val h = if (major) 26f else 14f
                drawLine(
                    color = if (major) {
                        RangePalette.Mist.copy(alpha = 0.20f + 0.55f * fade)
                    } else {
                        RangePalette.MistDim.copy(alpha = 0.10f + 0.30f * fade)
                    },
                    start = Offset(x, size.height / 2f - h / 2f),
                    end = Offset(x, size.height / 2f + h / 2f),
                    strokeWidth = if (major) 2.2f else 1.2f,
                    cap = StrokeCap.Round,
                )
                if (major && fade > 0.25f) {
                    val amount = (s * stepLocal)
                    val label = when {
                        currency == Currency.INR && amount >= 100_000 ->
                            "${(amount / 100_000).let { if (it % 1.0 == 0.0) it.toInt().toString() else String.format("%.1f", it) }}L"
                        amount >= 1000 ->
                            "${(amount / 1000).let { if (it % 1.0 == 0.0) it.toInt().toString() else String.format("%.1f", it) }}k"
                        else -> amount.roundToInt().toString()
                    }
                    val layout = measurer.measure(
                        label,
                        style = TextStyle(
                            color = RangePalette.MistDim.copy(alpha = 0.25f + 0.6f * fade),
                            fontSize = 10.sp,
                        ),
                    )
                    drawText(
                        layout,
                        topLeft = Offset(x - layout.size.width / 2f, size.height / 2f + 18f),
                    )
                }
            }

            // Centre marker.
            drawLine(
                brush = Brush.verticalGradient(
                    listOf(RangePalette.AuroraBright, RangePalette.Sky),
                ),
                start = Offset(cx, 4f),
                end = Offset(cx, size.height - 22f),
                strokeWidth = 3.5f,
                cap = StrokeCap.Round,
            )
            drawCircle(RangePalette.AuroraBright, radius = 4.5f, center = Offset(cx, 4f))
        }
    }
}

/** Sliding-pill selector used for Budget / Comfort / Luxury choices. */
@Composable
fun <T> SegmentedSelector(
    options: List<T>,
    selected: T,
    onSelect: (T) -> Unit,
    label: (T) -> String,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.primary,
) {
    val index = options.indexOf(selected).coerceAtLeast(0)
    val haptics = LocalHapticFeedback.current
    Box(
        modifier
            .clip(PillShape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, PillShape)
            .padding(4.dp),
    ) {
        Row(Modifier.fillMaxWidth()) {
            options.forEachIndexed { i, option ->
                val active = i == index
                val bg by animateColorAsState(
                    if (active) accent.copy(alpha = 0.18f) else Color.Transparent,
                    tween(260),
                    label = "segBg",
                )
                val fg by animateColorAsState(
                    if (active) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                    tween(260),
                    label = "segFg",
                )
                val scale by animateFloatAsState(if (active) 1f else 0.97f, Motion.snappy, label = "segScale")
                Box(
                    Modifier
                        .weight(1f)
                        .clip(PillShape)
                        .background(bg)
                        .then(
                            if (active) {
                                Modifier.border(1.dp, accent.copy(alpha = 0.5f), PillShape)
                            } else {
                                Modifier
                            },
                        )
                        .clickable {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSelect(option)
                        }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label(option),
                        style = MaterialTheme.typography.labelLarge,
                        color = fg,
                        fontWeight = if (active) FontWeight.W800 else FontWeight.W600,
                        modifier = Modifier.scale(scale),
                    )
                }
            }
        }
    }
}

/** Big, satisfying +/- control for travellers and nights. */
@Composable
fun CountStepper(
    value: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    min: Int = 1,
    max: Int = 12,
    suffix: String = "",
) {
    val haptics = LocalHapticFeedback.current
    Row(
        modifier
            .clip(PillShape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, PillShape)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        StepButton(Icons.Rounded.Remove, enabled = value > min) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onChange((value - 1).coerceAtLeast(min))
        }
        AnimatedNumber(
            text = "$value$suffix",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.width(58.dp),
            color = MaterialTheme.colorScheme.onSurface,
        )
        StepButton(Icons.Rounded.Add, enabled = value < max) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onChange((value + 1).coerceAtMost(max))
        }
    }
}

@Composable
private fun StepButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val size by animateDpAsState(if (enabled) 36.dp else 34.dp, Motion.snappy, label = "stepSize")
    val tint by animateColorAsState(
        if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        label = "stepTint",
    )
    Box(
        Modifier
            .size(size)
            .clip(RoundedCornerShape(50))
            .background(
                if (enabled) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                } else {
                    Color.Transparent
                },
            )
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
    }
}

/** Chip with an optional leading icon; animates its fill and border on select. */
@Composable
fun RangeChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    accent: Color = MaterialTheme.colorScheme.primary,
    enabled: Boolean = true,
) {
    val haptics = LocalHapticFeedback.current
    val bg by animateColorAsState(
        when {
            !enabled -> Color.Transparent
            selected -> accent.copy(alpha = 0.18f)
            else -> MaterialTheme.colorScheme.surfaceContainerLow
        },
        tween(240),
        label = "chipBg",
    )
    val border by animateColorAsState(
        when {
            !enabled -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            selected -> accent.copy(alpha = 0.65f)
            else -> MaterialTheme.colorScheme.outlineVariant
        },
        tween(240),
        label = "chipBorder",
    )
    val fg by animateColorAsState(
        when {
            !enabled -> MaterialTheme.colorScheme.outline
            selected -> accent
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        },
        tween(240),
        label = "chipFg",
    )
    val scale by animateFloatAsState(if (selected) 1.02f else 1f, Motion.bouncy, label = "chipScale")

    Row(
        modifier
            .scale(scale)
            .clip(PillShape)
            .background(bg)
            .border(1.dp, border, PillShape)
            .clickable(enabled = enabled) {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                onClick()
            }
            .padding(horizontal = 14.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(16.dp))
        }
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = fg,
            fontWeight = if (selected) FontWeight.W800 else FontWeight.W600,
        )
    }
}
