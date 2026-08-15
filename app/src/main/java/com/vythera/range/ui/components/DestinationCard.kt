@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
)

package com.vythera.range.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.material3.FilledIconToggleButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vythera.range.data.model.TransportMode
import com.vythera.range.data.model.Verdict
import com.vythera.range.domain.Currency
import com.vythera.range.domain.TripEstimate
import com.vythera.range.domain.formatHours
import com.vythera.range.domain.formatKm
import com.vythera.range.domain.formatMoney
import com.vythera.range.ui.theme.ExpressiveShapes
import com.vythera.range.ui.theme.PillShape

fun transportIcon(mode: TransportMode): ImageVector = when (mode) {
    TransportMode.FLIGHT -> Icons.Rounded.Flight
    TransportMode.TRAIN -> Icons.Rounded.Train
    TransportMode.BUS -> Icons.Rounded.DirectionsBus
    TransportMode.TAXI -> Icons.Rounded.LocalTaxi
    TransportMode.OWN_CAR -> Icons.Rounded.DirectionsCar
    TransportMode.NONE -> Icons.Rounded.Place
}

/**
 * Expressive result card: inset media with its own generous radius, the price
 * carried in a tonal container rather than plain text, and a wavy meter showing
 * how much of the budget this trip eats.
 */
@Composable
fun DestinationCard(
    estimate: TripEstimate,
    currency: Currency,
    wishlisted: Boolean,
    onClick: () -> Unit,
    onWishlist: () -> Unit,
    modifier: Modifier = Modifier,
    hero: Boolean = false,
    rankLabel: String? = null,
) {
    val interaction = rememberInteraction()
    val d = estimate.destination
    val affordable = estimate.withinBudget

    val priceContainer = when (estimate.verdict) {
        Verdict.EASY, Verdict.FITS -> MaterialTheme.colorScheme.primaryContainer
        Verdict.STRETCH -> MaterialTheme.colorScheme.tertiaryContainer
        Verdict.OUT -> MaterialTheme.colorScheme.surfaceContainerHighest
    }
    val onPriceContainer = when (estimate.verdict) {
        Verdict.EASY, Verdict.FITS -> MaterialTheme.colorScheme.onPrimaryContainer
        Verdict.STRETCH -> MaterialTheme.colorScheme.onTertiaryContainer
        Verdict.OUT -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        modifier
            .fillMaxWidth()
            .pressScale(interaction)
            .clip(RoundedCornerShape(32.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClickLabel = "Open ${d.city}",
                role = Role.Button,
                onClick = onClick,
            )
            .padding(10.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(if (hero) 196.dp else 142.dp)
                .clip(RoundedCornerShape(24.dp)),
        ) {
            DestinationArt(d, Modifier.fillMaxSize())

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    rankLabel?.let {
                        // A lobed shape derives its radius from the *shorter*
                        // side, so on a wide badge it collapses to a circle and
                        // eats the label — "BEST VALUE" was rendering as "T VA".
                        // Wide badges get the pill; the lobed shapes are for
                        // square marks only.
                        Box(
                            Modifier
                                .clip(PillShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                        ) {
                            Text(
                                it,
                                style = MaterialTheme.typography.labelSmall,
                                // onPrimary, not a fixed near-black: the badge
                                // sits on `primary`, which is now wallpaper-derived
                                // and can land light or dark.
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.W800,
                            )
                        }
                    }
                    if (!affordable) {
                        Box(
                            Modifier
                                .clip(PillShape)
                                .background(Color.Black.copy(alpha = 0.42f))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                        ) {
                            Text(
                                "Just over",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary,
                            )
                        }
                    }
                }
                // Expressive toggle: the container itself morphs from a rounded
                // square to a circle as it's checked, and squashes under the
                // finger. Saving a place should feel like a small event.
                FilledIconToggleButton(
                    checked = wishlisted,
                    onCheckedChange = { onWishlist() },
                    modifier = Modifier.size(38.dp),
                    shapes = IconButtonDefaults.toggleableShapes(),
                    colors = IconButtonDefaults.filledIconToggleButtonColors(
                        containerColor = Color.Black.copy(alpha = 0.34f),
                        contentColor = Color.White,
                        checkedContainerColor = MaterialTheme.colorScheme.error,
                        checkedContentColor = MaterialTheme.colorScheme.onError,
                    ),
                ) {
                    Icon(
                        if (wishlisted) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = "Save ${d.city}",
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            ) {
                Text(
                    d.city,
                    style = if (hero) {
                        MaterialTheme.typography.displaySmall
                    } else {
                        MaterialTheme.typography.headlineSmall
                    },
                    color = Color.White,
                    fontWeight = FontWeight.W800,
                )
                Text(
                    "${d.country}  ·  ${formatKm(estimate.distanceKm)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.85f),
                )
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, start = 6.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(priceContainer)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Text(
                    formatMoney(estimate.totalUsd, currency),
                    style = MaterialTheme.typography.headlineSmall,
                    color = onPriceContainer,
                    fontWeight = FontWeight.W800,
                )
                Text(
                    "total · ${formatMoney(estimate.perPersonUsd, currency)} each",
                    style = MaterialTheme.typography.labelSmall,
                    color = onPriceContainer.copy(alpha = 0.82f),
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // The live badge displaces the duration chip rather than
                    // joining it: three chips overflow the column at the narrow
                    // widths this row hits on a small phone, and "this price is
                    // real" is worth more than "9h flight".
                    estimate.liveFare?.let { LiveFareBadge(it) }
                    MetaChip(transportIcon(estimate.mode), estimate.mode.label)
                    if (estimate.liveFare == null) {
                        MetaChip(
                            Icons.Rounded.Schedule,
                            formatHours(
                                estimate.transportOptions
                                    .firstOrNull { it.mode == estimate.mode }?.hoursOneWay ?: 0.0,
                            ),
                        )
                    }
                }
                if (estimate.inSeason) {
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.Bolt,
                            null,
                            tint = MaterialTheme.colorScheme.tertiary,
                            modifier = Modifier.size(13.dp),
                        )
                        Text(
                            " best season",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(10.dp))
        BudgetMeter(estimate, currency, Modifier.padding(horizontal = 6.dp))

        AnimatedVisibility(
            visible = !affordable && estimate.leanRescue,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Row(
                Modifier
                    .padding(top = 10.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.tertiaryContainer)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.Bolt,
                    null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                    modifier = Modifier.size(15.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Fits at ${formatMoney(estimate.leanTotalUsd, currency)} on budget tiers",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                )
            }
        }

        Spacer(Modifier.height(4.dp))
    }
}

/** Wavy meter: how much of the budget this trip eats. */
@Composable
fun BudgetMeter(
    estimate: TripEstimate,
    currency: Currency,
    modifier: Modifier = Modifier,
) {
    val color = verdictColor(estimate.verdict)
    val fraction = (estimate.ratio.coerceIn(0.0, 1.35) / 1.35).toFloat()
    Column(modifier.fillMaxWidth()) {
        WavyProgress(
            progress = fraction,
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            modifier = Modifier.fillMaxWidth(),
        )
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                if (estimate.withinBudget) {
                    "${formatMoney(estimate.headroomUsd, currency)} left over"
                } else {
                    "${formatMoney(-estimate.headroomUsd, currency)} over budget"
                },
                style = MaterialTheme.typography.bodySmall,
                color = color,
            )
            Text(
                "${(estimate.ratio * 100).toInt()}% of budget",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun MetaChip(icon: ImageVector, label: String, modifier: Modifier = Modifier) {
    Row(
        modifier
            .clip(PillShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .padding(horizontal = 10.dp, vertical = 6.dp),
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
