@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vythera.range.data.model.Verdict
import com.vythera.range.ui.theme.CardShape
import com.vythera.range.ui.theme.PillShape
import com.vythera.range.ui.theme.RangePalette

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

/** Frosted card used everywhere: subtle top-light border and a raised surface. */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    shape: Shape = CardShape,
    tone: Color = MaterialTheme.colorScheme.surfaceContainer,
    borderTint: Color = MaterialTheme.colorScheme.outlineVariant,
    contentPadding: Dp = 20.dp,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Column(
        modifier
            .clip(shape)
            .background(tone)
            .border(
                BorderStroke(
                    1.dp,
                    Brush.verticalGradient(
                        listOf(borderTint.copy(alpha = 0.9f), borderTint.copy(alpha = 0.25f)),
                    ),
                ),
                shape,
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

fun verdictColor(v: Verdict): Color = when (v) {
    Verdict.EASY -> RangePalette.Aurora
    Verdict.FITS -> RangePalette.Lagoon
    Verdict.STRETCH -> RangePalette.Sand
    Verdict.OUT -> Color(0xFF7C8AA5)
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

/** Number that rolls when it changes — used for every money figure in the app. */
@Composable
fun AnimatedNumber(
    text: String,
    modifier: Modifier = Modifier,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.displaySmall,
    color: Color = MaterialTheme.colorScheme.onSurface,
    upwards: Boolean = true,
) {
    AnimatedContent(
        targetState = text,
        transitionSpec = {
            val dir = if (upwards) 1 else -1
            (
                slideInVertically(animationSpec = tween(320)) { h -> dir * h / 2 } +
                    fadeIn(tween(220))
                ) togetherWith (
                slideOutVertically(animationSpec = tween(320)) { h -> -dir * h / 2 } +
                    fadeOut(tween(160))
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
