@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.animation.ExperimentalSharedTransitionApi::class,
    androidx.compose.ui.ExperimentalComposeUiApi::class,
)

package com.vythera.range.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Flight
import androidx.compose.material.icons.rounded.LocalTaxi
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Train
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vythera.range.data.model.TransportMode
import com.vythera.range.domain.Currency
import com.vythera.range.domain.TripEstimate
import com.vythera.range.domain.formatHours
import com.vythera.range.domain.formatKm
import com.vythera.range.domain.formatMoney
import com.vythera.range.ui.theme.CardShape
import com.vythera.range.ui.theme.PillShape
import com.vythera.range.ui.theme.RangePalette

fun transportIcon(mode: TransportMode): ImageVector = when (mode) {
    TransportMode.FLIGHT -> Icons.Rounded.Flight
    TransportMode.TRAIN -> Icons.Rounded.Train
    TransportMode.BUS -> Icons.Rounded.DirectionsBus
    TransportMode.TAXI -> Icons.Rounded.LocalTaxi
    TransportMode.OWN_CAR -> Icons.Rounded.DirectionsCar
    TransportMode.NONE -> Icons.Rounded.Place
}

@Composable
fun DestinationCard(
    estimate: TripEstimate,
    currency: Currency,
    wishlisted: Boolean,
    onClick: () -> Unit,
    onWishlist: () -> Unit,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    val interaction = rememberInteraction()
    val d = estimate.destination
    val alpha by animateFloatAsState(
        if (estimate.withinBudget) 1f else 0.72f,
        tween(400),
        label = "cardAlpha",
    )

    Column(
        modifier
            .fillMaxWidth()
            .pressScale(interaction)
            .clip(CardShape)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = alpha), CardShape)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(if (compact) 108.dp else 132.dp),
        ) {
            DestinationArt(d, Modifier.fillMaxSize())

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                VerdictPill(estimate.verdict)
                Box(
                    Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(50))
                        .background(Color.Black.copy(alpha = 0.32f))
                        .clickable(onClick = onWishlist),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        if (wishlisted) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "Save",
                        tint = if (wishlisted) RangePalette.Coral else Color.White,
                        modifier = Modifier.size(17.dp),
                    )
                }
            }

            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Text(
                    d.city,
                    style = MaterialTheme.typography.headlineSmall,
                    color = Color.White,
                    fontWeight = FontWeight.W800,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        d.country,
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.82f),
                    )
                    Text(
                        "  •  ${formatKm(estimate.distanceKm)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color.White.copy(alpha = 0.6f),
                    )
                }
            }
        }

        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Column {
                    Text(
                        "TOTAL FOR ${estimate.query.travelers}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    AnimatedNumber(
                        text = formatMoney(estimate.totalUsd, currency),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "PER PERSON",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        formatMoney(estimate.perPersonUsd, currency),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            BudgetMeter(estimate, currency)

            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetaChip(transportIcon(estimate.mode), estimate.mode.label)
                MetaChip(
                    Icons.Rounded.Schedule,
                    formatHours(
                        estimate.transportOptions
                            .firstOrNull { it.mode == estimate.mode }?.hoursOneWay ?: 0.0,
                    ),
                )
                if (estimate.inSeason) MetaChip(Icons.Rounded.Bolt, "In season")
            }

            AnimatedVisibility(
                visible = estimate.leanRescue,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Row(
                    Modifier
                        .padding(top = 12.dp)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(RangePalette.Sand.copy(alpha = 0.12f))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.Bolt,
                        null,
                        tint = RangePalette.Sand,
                        modifier = Modifier.size(15.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Fits at ${formatMoney(estimate.leanTotalUsd, currency)} if you go budget",
                        style = MaterialTheme.typography.bodySmall,
                        color = RangePalette.Sand,
                    )
                }
            }
        }
    }
}

/** Thin bar showing how much of the budget this trip eats. */
@Composable
fun BudgetMeter(
    estimate: TripEstimate,
    currency: Currency,
    modifier: Modifier = Modifier,
) {
    val fraction by animateFloatAsState(
        estimate.ratio.coerceIn(0.0, 1.35).toFloat() / 1.35f,
        tween(700),
        label = "meter",
    )
    val color = verdictColor(estimate.verdict)
    Column(modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(7.dp)
                .clip(PillShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .height(7.dp)
                    .clip(PillShape)
                    .background(
                        Brush.horizontalGradient(listOf(color.copy(alpha = 0.65f), color)),
                    ),
            )
            // Budget line at 1.0 of the budget (0.74 of the 1.35 scale).
            Box(
                Modifier
                    .fillMaxWidth(1f / 1.35f)
                    .height(7.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Box(
                    Modifier
                        .width(2.dp)
                        .height(11.dp)
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            if (estimate.withinBudget) {
                "${formatMoney(estimate.headroomUsd, currency)} left over"
            } else {
                "${formatMoney(-estimate.headroomUsd, currency)} over budget"
            },
            style = MaterialTheme.typography.bodySmall,
            color = color,
        )
    }
}

@Composable
fun MetaChip(icon: ImageVector, label: String, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(PillShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(horizontal = 9.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(
            icon,
            null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(13.dp),
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
