@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
)

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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonGroup
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearWavyProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.vythera.range.ui.theme.lobedPoints
import com.vythera.range.ui.theme.smoothClosedPath
import kotlin.math.sin

/**
 * The Material 3 Expressive loading indicator — the real one.
 *
 * This used to be drawn by hand. Material now ships the seven-shape morphing
 * indicator itself, animated on the expressive motion scheme, so the hand-rolled
 * version was strictly worse: same idea, less correct timing, more code.
 */
@Composable
fun ExpressiveLoader(
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
) {
    LoadingIndicator(modifier = modifier, color = color)
}

/** Contained variant — sits in its own tonal container. Used on full-screen waits. */
@Composable
fun ExpressiveLoaderContained(modifier: Modifier = Modifier) {
    ContainedLoadingIndicator(modifier = modifier)
}

/**
 * Expressive wavy progress track — the squiggle.
 *
 * Material's own indicator, which animates its amplitude down to a flat line as
 * a value approaches completion. That detail is the tell of the real component
 * and is worth more than the custom sine wave it replaces.
 */
@Composable
fun WavyProgress(
    progress: Float,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    amplitude: Dp = 4.dp,
    animated: Boolean = true,
) {
    val p by animateFloatAsState(
        progress.coerceIn(0f, 1f),
        MaterialTheme.motionScheme.slowSpatialSpec(),
        label = "wavyProgress",
    )
    LinearWavyProgressIndicator(
        progress = { p },
        modifier = modifier.height(amplitude * 3 + 6.dp),
        color = color,
        trackColor = trackColor,
        // A full-budget bar has nothing left to say, so let the wave flatten as
        // it fills; a half-used budget stays lively.
        amplitude = { frac -> if (!animated) 0f else (1f - frac).coerceIn(0f, 1f) },
    )
}

/**
 * Connected button group — Material's, not ours.
 *
 * The expressive behaviour here is subtle and hard to fake: pressing a cell
 * widens it while its immediate neighbours *compress* to make room, then
 * everything springs back. `animateWidth` wires that to the interaction source
 * for us, which is why the whole hand-rolled weight/corner animation went away.
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
    val interactions = remember(options) { options.map { MutableInteractionSource() } }

    ButtonGroup(
        overflowIndicator = {},
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
    ) {
        options.forEachIndexed { index, option ->
            val active = option == selected
            customItem(
                buttonGroupContent = {
                    ToggleButton(
                        checked = active,
                        onCheckedChange = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onSelect(option)
                        },
                        modifier = Modifier
                            .weight(if (active) 1.35f else 1f)
                            .height(58.dp)
                            .animateWidth(interactions[index]),
                        // Round when idle, squarer when selected: the shape
                        // itself reports state, which is the expressive idiom.
                        shapes = ToggleButtonDefaults.shapes(
                            shape = ToggleButtonDefaults.roundShape,
                            pressedShape = ToggleButtonDefaults.pressedShape,
                            checkedShape = ToggleButtonDefaults.squareShape,
                        ),
                        colors = ToggleButtonDefaults.toggleButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            checkedContainerColor = accent,
                            checkedContentColor = MaterialTheme.colorScheme.onPrimary,
                        ),
                        interactionSource = interactions[index],
                        contentPadding = PaddingValues(horizontal = 6.dp),
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                        ) {
                            icon?.invoke(option)?.let {
                                Icon(it, null, modifier = Modifier.size(18.dp))
                            }
                            Text(
                                label(option),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (active) FontWeight.W800 else FontWeight.W600,
                                textAlign = TextAlign.Center,
                                maxLines = 1,
                            )
                        }
                    }
                },
                menuContent = {},
            )
        }
    }
}

