@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.animation.ExperimentalSharedTransitionApi::class,
    androidx.compose.ui.ExperimentalComposeUiApi::class,
)

package com.vythera.range.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import com.vythera.range.data.live.FareQuote
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vythera.range.data.model.Verdict
import com.vythera.range.ui.theme.CardShape
import com.vythera.range.ui.theme.LocalIsDark
import com.vythera.range.ui.theme.PillShape
import com.vythera.range.ui.theme.VerdictColors
import com.vythera.range.ui.theme.verdictColors

/** Spring vocabulary — every interactive surface in Range moves on one of these. */
object Motion {
    val snappy = spring<Float>(dampingRatio = 0.62f, stiffness = Spring.StiffnessMediumLow)
    val gentle = spring<Float>(dampingRatio = 0.85f, stiffness = Spring.StiffnessLow)
    val bouncy = spring<Float>(dampingRatio = 0.42f, stiffness = 320f)
    val slow = tween<Float>(durationMillis = 680)
    fun <T> springOf(damping: Float = 0.7f, stiffness: Float = 300f) =
        spring<T>(dampingRatio = damping, stiffness = stiffness)
}

/** Press-scale that reads as physical without being cartoonish. */
@Composable
fun Modifier.pressScale(
    interactionSource: MutableInteractionSource,
    pressed: Float = 0.965f,
): Modifier {
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) pressed else 1f,
        animationSpec = Motion.snappy,
        label = "pressScale",
    )
    return this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/**
 * The app's surface primitive. On dark it keeps a hairline top-light edge so
 * cards separate from the midnight ground; on light it drops the outline and
 * leans on the container tone instead, which is how M3 wants it.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = CardShape,
    tone: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
    borderTint: Color = MaterialTheme.colorScheme.outlineVariant,
    contentPadding: Dp = 20.dp,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    val dark = LocalIsDark.current
    Column(
        modifier
            .clip(shape)
            .background(tone)
            .then(
                if (dark) {
                    Modifier.border(
                        BorderStroke(
                            1.dp,
                            Brush.verticalGradient(
                                listOf(
                                    borderTint.copy(alpha = 0.85f),
                                    borderTint.copy(alpha = 0.20f),
                                ),
                            ),
                        ),
                        shape,
                    )
                } else {
                    Modifier
                },
            )
            .padding(contentPadding),
        content = content,
    )
}

@Composable
fun SectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.4.sp,
        )
        trailing?.invoke()
    }
}

@Composable
fun verdictColor(v: Verdict): Color = verdictColors.of(v)

/** Non-composable lookup, for `DrawScope` code that was handed the colours. */
fun VerdictColors.of(v: Verdict): Color = when (v) {
    Verdict.EASY -> easy
    Verdict.FITS -> fits
    Verdict.STRETCH -> stretch
    Verdict.OUT -> out
}

@Composable
fun VerdictPill(
    verdict: Verdict,
    modifier: Modifier = Modifier,
    label: String = verdict.label,
) {
    val c = verdictColor(verdict)
    Box(
        modifier
            .clip(PillShape)
            .background(c.copy(alpha = 0.16f))
            .border(1.dp, c.copy(alpha = 0.45f), PillShape)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = c,
            fontWeight = FontWeight.W700,
        )
    }
}

/**
 * Marks a total that rests on a real fare rather than the model.
 *
 * The distinction matters more than it looks: a modelled number is a planning
 * estimate the user should sanity-check, a live one is close to bookable. The
 * badge names its source and its age because a six-hour-old quote and a
 * six-second-old one deserve different amounts of trust, and only the app knows
 * which this is.
 */
@Composable
fun LiveFareBadge(
    quote: FareQuote,
    modifier: Modifier = Modifier,
    showSource: Boolean = false,
) {
    val accent = MaterialTheme.colorScheme.primary
    Row(
        modifier
            .clip(PillShape)
            .background(accent.copy(alpha = 0.16f))
            .border(1.dp, accent.copy(alpha = 0.45f), PillShape)
            .padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Dot(accent, 6.dp)
        Text(
            if (showSource) "LIVE · ${quote.source.label}" else "LIVE",
            style = MaterialTheme.typography.labelSmall,
            color = accent,
            fontWeight = FontWeight.W700,
        )
    }
}

/** "just now", "2h ago" — deliberately vague, because the precision is fake. */
fun fareAgeLabel(quote: FareQuote, now: Long = System.currentTimeMillis()): String {
    val minutes = quote.ageMs(now) / 60_000
    return when {
        minutes < 2 -> "checked just now"
        minutes < 60 -> "checked ${minutes}m ago"
        else -> "checked ${minutes / 60}h ago"
    }
}

/** Number that rolls when it changes — used for every money figure in the app. */
@Composable
fun AnimatedNumber(
    text: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.displaySmall,
    color: Color = MaterialTheme.colorScheme.onSurface,
    upwards: Boolean = true,
    animate: Boolean = true,
) {
    if (!animate) {
        // While a value is being scrubbed, a transition per frame is pure jank.
        Text(text, style = style, color = color, textAlign = TextAlign.Center, maxLines = 1, modifier = modifier)
        return
    }
    // Rolls on the expressive motion scheme rather than a fixed tween, so the
    // headline figure carries the same spring as everything around it. It was
    // the one prominent element still moving to a duration curve.
    // Resolved outside the transition lambda: `transitionSpec` is not a
    // composable scope, so MaterialTheme cannot be read from inside it.
    val enterSlide = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
    val exitSlide = MaterialTheme.motionScheme.fastSpatialSpec<IntOffset>()
    val enterFade = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()
    val exitFade = MaterialTheme.motionScheme.fastEffectsSpec<Float>()
    AnimatedContent(
        targetState = text,
        transitionSpec = {
            val dir = if (upwards) 1 else -1
            (
                slideInVertically(animationSpec = enterSlide) { h -> dir * h / 2 } +
                    fadeIn(enterFade)
                ) togetherWith (
                slideOutVertically(animationSpec = exitSlide) { h -> -dir * h / 2 } +
                    fadeOut(exitFade)
                )
        },
        label = "animatedNumber",
        modifier = modifier,
    ) { value ->
        Text(value, style = style, color = color, textAlign = TextAlign.Center, maxLines = 1)
    }
}

/** Soft coloured glow behind hero elements. */
fun Modifier.glow(color: Color, radius: Dp = 60.dp, alpha: Float = 0.45f): Modifier =
    this.drawBehind {
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(color.copy(alpha = alpha), Color.Transparent),
                center = Offset(size.width / 2f, size.height / 2f),
                radius = radius.toPx(),
            ),
            radius = radius.toPx(),
        )
    }

@Composable
fun Dot(color: Color, size: Dp = 6.dp) {
    Box(Modifier.size(size).clip(RoundedCornerShape(50)).background(color))
}

@Composable
fun rememberInteraction(): MutableInteractionSource = remember { MutableInteractionSource() }
