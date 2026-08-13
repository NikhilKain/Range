@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.animation.ExperimentalSharedTransitionApi::class,
    androidx.compose.ui.ExperimentalComposeUiApi::class,
)

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
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.AnimationState
import androidx.compose.animation.core.animateDecay
import com.vythera.range.domain.Currency
import com.vythera.range.domain.rate
import com.vythera.range.ui.theme.PillShape
import com.vythera.range.ui.theme.RangePalette
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * A tape-measure budget input.
 *
 * The tape owns its own value while you drag it, so the ruler and the headline
 * figure track your finger at frame rate; the priced result only gets asked for
 * on a throttle and again when you let go. Tick labels are measured once and
 * cached, because measuring text inside a draw loop is what makes rulers stutter.
 */
@Composable
fun BudgetTape(
    valueUsd: Double,
    currency: Currency,
    onPreview: (Double) -> Unit,
    onCommit: (Double) -> Unit,
    modifier: Modifier = Modifier,
    onDragging: (Boolean) -> Unit = {},
    minUsd: Double = 40.0,
    maxUsd: Double = 24_000.0,
) {
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val measurer = rememberTextMeasurer()
    val scope = rememberCoroutineScope()
    val decay = rememberSplineBasedDecay<Float>()

    val stepLocal = currency.step.toDouble()
    val stepUsd = stepLocal / currency.rate
    val pxPerStep = with(density) { 26.dp.toPx() }

    var dragging by remember { mutableStateOf(false) }
    var local by remember { mutableDoubleStateOf(valueUsd) }
    var lastTick by remember { mutableDoubleStateOf(valueUsd) }
    var lastCommit by remember { mutableLongStateOf(0L) }

    // Follow the outside world whenever the user isn't the one moving it.
    if (!dragging && local != valueUsd) local = valueUsd

    val labelCache = remember(currency) { mutableMapOf<Int, TextLayoutResult>() }
    val labelColor = RangePalette.MistDim

    fun push(value: Double, force: Boolean) {
        onPreview(value)
        val now = System.currentTimeMillis()
        if (force || now - lastCommit > 180) {
            lastCommit = now
            onCommit(value)
        }
    }

    fun applyDelta(dxPx: Float) {
        val next = (local - (dxPx / pxPerStep) * stepUsd).coerceIn(minUsd, maxUsd)
        if (abs(next - lastTick) >= stepUsd * 0.98) {
            lastTick = next
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
        local = next
        push(next, force = false)
    }

    fun settle() {
        val steps = (local * currency.rate / stepLocal).roundToInt().coerceAtLeast(1)
        local = (steps * stepLocal / currency.rate).coerceIn(minUsd, maxUsd)
        push(local, force = true)
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
                    onDragStarted = {
                        dragging = true
                        onDragging(true)
                    },
                    onDragStopped = { velocity ->
                        scope.launch {
                            var last = 0f
                            AnimationState(initialValue = 0f, initialVelocity = velocity / 2.4f)
                                .animateDecay(decay) {
                                    val d = value - last
                                    last = value
                                    applyDelta(d)
                                }
                            settle()
                            dragging = false
                            onDragging(false)
                        }
                    },
                ),
        ) {
            val cx = size.width / 2f
            val localAmount = local * currency.rate
            val stepsFromZero = localAmount / stepLocal

            val firstStep = (stepsFromZero - (cx / pxPerStep) - 1).toInt()
            val lastStep = (stepsFromZero + (cx / pxPerStep) + 1).toInt()

            for (st in firstStep..lastStep) {
                if (st < 0) continue
                val x = cx + ((st - stepsFromZero) * pxPerStep).toFloat()
                val major = st % 5 == 0
                val fade = (1f - abs(x - cx) / cx).coerceIn(0f, 1f)
                val h = if (major) 28f else 14f
                drawLine(
                    color = if (major) {
                        RangePalette.Mist.copy(alpha = 0.30f + 0.65f * fade)
                    } else {
                        RangePalette.MistDim.copy(alpha = 0.18f + 0.45f * fade)
                    },
                    start = Offset(x, size.height / 2f - h / 2f),
                    end = Offset(x, size.height / 2f + h / 2f),
                    strokeWidth = if (major) 2.4f else 1.3f,
                    cap = StrokeCap.Round,
                )
                if (major && fade > 0.05f) {
                    val layout = labelCache.getOrPut(st) {
                        measurer.measure(
                            tickLabel(st * stepLocal, currency),
                            style = TextStyle(color = labelColor, fontSize = 10.sp),
                        )
                    }
                    drawText(
                        layout,
                        color = labelColor.copy(alpha = 0.35f + 0.6f * fade),
                        topLeft = Offset(x - layout.size.width / 2f, size.height / 2f + 18f),
                    )
                }
            }

            drawLine(
                brush = Brush.verticalGradient(
                    listOf(RangePalette.AuroraBright, RangePalette.Sky),
                ),
                start = Offset(cx, 2f),
                end = Offset(cx, size.height - 20f),
                strokeWidth = 4f,
                cap = StrokeCap.Round,
            )
            drawCircle(RangePalette.AuroraBright, radius = 5f, center = Offset(cx, 2f))
        }
    }
}

private fun tickLabel(amount: Double, currency: Currency): String = when {
    currency == Currency.INR && amount >= 100_000 ->
        "${trimNumber(amount / 100_000)}L"
    amount >= 1000 -> "${trimNumber(amount / 1000)}k"
    else -> amount.roundToInt().toString()
}

private fun trimNumber(v: Double): String =
    if (v % 1.0 == 0.0) v.toInt().toString() else ((v * 10).roundToInt() / 10.0).toString()

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
    val size by animateDpAsState(
        if (enabled) 36.dp else 34.dp,
        Motion.springOf(),
        label = "stepSize",
    )
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
    val scale by animateFloatAsState(if (selected) 1.03f else 1f, Motion.bouncy, label = "chipScale")
    val corner by animateFloatAsState(
        if (selected) 14f else 50f,
        Motion.springOf(0.55f, 320f),
        label = "chipCorner",
    )
    val shape = RoundedCornerShape(corner.dp)

    Row(
        modifier
            .scale(scale)
            .clip(shape)
            .background(bg)
            .border(1.dp, border, shape)
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
