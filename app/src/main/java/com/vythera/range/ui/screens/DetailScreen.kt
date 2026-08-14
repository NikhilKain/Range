@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.ui.ExperimentalComposeUiApi::class,
)

package com.vythera.range.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BookmarkAdded
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vythera.range.data.OriginCatalog
import com.vythera.range.data.model.CostKey
import com.vythera.range.data.model.Destination
import com.vythera.range.data.model.Tier
import com.vythera.range.data.model.TransportMode
import com.vythera.range.domain.BudgetEngine
import com.vythera.range.domain.Currency
import com.vythera.range.domain.TripEstimate
import com.vythera.range.domain.TripQuery
import com.vythera.range.domain.compassLabel
import com.vythera.range.domain.formatHours
import com.vythera.range.domain.formatKm
import com.vythera.range.domain.formatMoney
import com.vythera.range.ui.components.AnimatedNumber
import com.vythera.range.ui.components.BudgetMeter
import com.vythera.range.ui.components.DestinationArt
import com.vythera.range.ui.components.GlassCard
import com.vythera.range.ui.components.MetaChip
import com.vythera.range.ui.components.SectionHeader
import com.vythera.range.ui.components.VerdictPill
import com.vythera.range.ui.components.pressScale
import com.vythera.range.ui.components.rememberInteraction
import com.vythera.range.ui.components.transportIcon
import com.vythera.range.ui.theme.CardShape
import com.vythera.range.ui.theme.PillShape
import com.vythera.range.ui.theme.costColors
import java.time.format.TextStyle as JTextStyle
import java.util.Locale
import kotlin.math.roundToInt

@Composable
private fun costColor(key: CostKey): Color = costColors.let { c ->
    when (key) {
        CostKey.TRANSPORT -> c.transport
        CostKey.STAY -> c.stay
        CostKey.FOOD -> c.food
        CostKey.LOCAL -> c.local
        CostKey.EXPERIENCES -> c.experiences
        else -> c.overhead
    }
}

@Composable
fun DetailScreen(
    destination: Destination,
    query: TripQuery,
    currency: Currency,
    wishlisted: Boolean,
    onWishlist: () -> Unit,
    onSaveTrip: (TripEstimate) -> Unit,
    onApplyQuery: ((TripQuery) -> TripQuery) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scroll = rememberScrollState()
    val origin = remember(query.originId) { OriginCatalog.find(query.originId) }
    val estimate = remember(destination.id, query) {
        BudgetEngine.estimate(origin, destination, query)
    }
    var previewNights by remember(destination.id) { mutableStateOf(query.nights) }
    val preview = remember(destination.id, query, previewNights) {
        BudgetEngine.estimate(origin, destination, query.copy(nights = previewNights))
    }
    var saved by remember(destination.id) { mutableStateOf(false) }
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scroll),
        ) {
            // ---- hero ----
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(320.dp),
            ) {
                val parallax = (scroll.value / 6f).coerceIn(0f, 60f)
                DestinationArt(
                    destination,
                    Modifier
                        .fillMaxSize()
                        .alpha((1f - scroll.value / 900f).coerceIn(0.35f, 1f)),
                    parallax = parallax,
                    detail = true,
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.45f to Color.Transparent,
                                1f to MaterialTheme.colorScheme.background,
                            ),
                        ),
                )
                Column(
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(20.dp),
                ) {
                    VerdictPill(estimate.verdict)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        destination.city,
                        style = MaterialTheme.typography.displaySmall,
                        color = Color.White,
                        fontWeight = FontWeight.W800,
                    )
                    Text(
                        "${destination.country} · ${compassLabel(estimate.bearing)} of ${origin.city}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f),
                    )
                }
            }

            Column(Modifier.padding(horizontal = 20.dp)) {
                Text(
                    destination.blurb,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(14.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MetaChip(transportIcon(estimate.mode), formatKm(estimate.distanceKm))
                    MetaChip(
                        Icons.Rounded.Schedule,
                        formatHours(
                            estimate.transportOptions
                                .firstOrNull { it.mode == estimate.mode }?.hoursOneWay ?: 0.0,
                        ),
                    )
                    MetaChip(Icons.Rounded.CheckCircle, destination.visaKind.label)
                    destination.vibes.take(3).forEach { v ->
                        MetaChip(Icons.Rounded.Favorite, v.label)
                    }
                }

                Spacer(Modifier.height(20.dp))

                // ---- the number ----
                GlassCard {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom,
                    ) {
                        Column {
                            Text(
                                "TRIP TOTAL · ${query.travelers} PEOPLE · ${query.nights} NIGHTS",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                letterSpacing = 1.2.sp,
                            )
                            AnimatedNumber(
                                text = formatMoney(estimate.totalUsd, currency),
                                style = MaterialTheme.typography.displaySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "EACH",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                formatMoney(estimate.perPersonUsd, currency),
                                style = MaterialTheme.typography.titleLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    BudgetMeter(estimate, currency)
                    Spacer(Modifier.height(10.dp))
                    Text(
                        estimate.seasonNote,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (estimate.inSeason) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.secondary,
                    )
                }

                Spacer(Modifier.height(16.dp))

                // ---- breakdown ----
                GlassCard {
                    SectionHeader("WHERE THE MONEY GOES")
                    Spacer(Modifier.height(14.dp))
                    StackedCostBar(estimate)
                    Spacer(Modifier.height(16.dp))
                    estimate.lines.forEach { line ->
                        CostRow(line.key, line.amountUsd, line.detail, estimate.totalUsd, currency)
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ---- how long could you stay ----
                GlassCard {
                    SectionHeader("HOW LONG COULD YOU STAY?")
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        AnimatedNumber(
                            text = "$previewNights",
                            style = MaterialTheme.typography.displaySmall,
                            color = if (preview.withinBudget) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                        )
                        Text(
                            " nights · ${formatMoney(preview.totalUsd, currency)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 6.dp),
                        )
                    }
                    Slider(
                        value = previewNights.toFloat(),
                        onValueChange = { previewNights = it.roundToInt() },
                        valueRange = 1f..21f,
                        steps = 19,
                    )
                    Text(
                        if (estimate.maxNights > 0) {
                            "Your budget covers up to ${estimate.maxNights} nights here."
                        } else {
                            "Even one night is over budget at these settings."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    AnimatedVisibility(previewNights != query.nights) {
                        Row(Modifier.padding(top = 12.dp)) {
                            ActionPill("Use $previewNights nights") {
                                onApplyQuery { it.copy(nights = previewNights) }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ---- transport options ----
                GlassCard {
                    SectionHeader("WAYS TO GET THERE")
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Range picked ${estimate.mode.label.lowercase()} because it's the cheapest " +
                            "option you allowed.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(12.dp))
                    estimate.transportOptions
                        .filter { it.mode != TransportMode.NONE }
                        .sortedBy { if (it.available) it.costUsd else Double.MAX_VALUE }
                        .forEach { option ->
                            TransportRow(
                                option = option,
                                currency = currency,
                                chosen = option.mode == estimate.mode,
                                onPick = { onApplyQuery { q -> q.copy(modes = setOf(option.mode)) } },
                            )
                        }
                }

                Spacer(Modifier.height(16.dp))

                // ---- tier what-ifs ----
                GlassCard {
                    SectionHeader("CHANGE YOUR STYLE")
                    Spacer(Modifier.height(12.dp))
                    Tier.entries.forEach { tier ->
                        val alt = remember(destination.id, query, tier) {
                            BudgetEngine.estimate(
                                origin,
                                destination,
                                query.copy(stay = tier, food = tier, experience = tier),
                            )
                        }
                        TierCompareRow(
                            tier = tier,
                            estimate = alt,
                            currency = currency,
                            current = query.stay == tier && query.food == tier,
                            onApply = {
                                onApplyQuery {
                                    it.copy(stay = tier, food = tier, experience = tier)
                                }
                            },
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                // ---- highlights ----
                GlassCard {
                    SectionHeader("WHAT YOU'D ACTUALLY DO")
                    Spacer(Modifier.height(12.dp))
                    destination.highlights.forEachIndexed { i, h ->
                        Row(
                            Modifier.padding(vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier
                                    .size(26.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(destination.gradient.first().copy(alpha = 0.22f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "${i + 1}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = destination.gradient.first(),
                                )
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(
                                h,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Best months: " + destination.bestMonths.sorted().joinToString(", ") {
                            java.time.Month.of(it).getDisplayName(JTextStyle.SHORT, Locale.getDefault())
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(Modifier.height(20.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    val interaction = rememberInteraction()
                    Box(
                        Modifier
                            .weight(1f)
                            .height(56.dp)
                            .pressScale(interaction)
                            .clip(PillShape)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary),
                                ),
                            )
                            .clickable(interactionSource = interaction, indication = null) {
                                onSaveTrip(estimate)
                                saved = true
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (saved) Icons.Rounded.BookmarkAdded else Icons.Rounded.BookmarkBorder,
                                null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(19.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (saved) "Saved to trips" else "Save this trip",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.W800,
                            )
                        }
                    }
                    Box(
                        Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(50))
                            .clickable(onClick = onWishlist),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            if (wishlisted) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                            null,
                            tint = if (wishlisted) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }

                Spacer(Modifier.height(40.dp))
            }
        }

        // Floating back button over the hero.
        Box(
            Modifier
                .padding(top = topInset + 10.dp, start = 16.dp)
                .size(42.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.35f))
                .clickable(onClick = onBack),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.ArrowBack, "Back", tint = Color.White, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun StackedCostBar(estimate: TripEstimate) {
    val total = estimate.totalUsd.coerceAtLeast(1.0)
    Row(
        Modifier
            .fillMaxWidth()
            .height(16.dp)
            .clip(PillShape),
    ) {
        estimate.lines.forEach { line ->
            val fraction = (line.amountUsd / total).toFloat()
            if (fraction > 0.001f) {
                Box(
                    Modifier
                        .weight(fraction)
                        .fillMaxSize()
                        .background(costColor(line.key)),
                )
            }
        }
    }
}

@Composable
private fun CostRow(
    key: CostKey,
    amount: Double,
    detail: String,
    total: Double,
    currency: Currency,
) {
    val pct = (amount / total.coerceAtLeast(1.0) * 100).roundToInt()
    val width by animateFloatAsState(
        (amount / total.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1f),
        tween(700),
        label = "costRow",
    )
    Column(Modifier.padding(vertical = 7.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(9.dp)
                    .clip(RoundedCornerShape(50))
                    .background(costColor(key)),
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    key.label,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    formatMoney(amount, currency),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    "$pct%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .fillMaxWidth()
                .height(3.dp)
                .clip(PillShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(width)
                    .height(3.dp)
                    .clip(PillShape)
                    .background(costColor(key)),
            )
        }
    }
}

@Composable
private fun TransportRow(
    option: com.vythera.range.domain.TransportOption,
    currency: Currency,
    chosen: Boolean,
    onPick: () -> Unit,
) {
    val enabled = option.available
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (chosen) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLow
                },
            )
            .then(
                if (chosen) {
                    Modifier.border(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        RoundedCornerShape(18.dp),
                    )
                } else {
                    Modifier
                },
            )
            .clickable(enabled = enabled, onClick = onPick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            transportIcon(option.mode),
            null,
            tint = if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.outline
            },
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                option.mode.label,
                style = MaterialTheme.typography.titleSmall,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.outline
                },
            )
            Text(
                option.note,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                if (enabled) formatMoney(option.costUsd, currency) else "—",
                style = MaterialTheme.typography.titleSmall,
                color = if (enabled) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.outline
                },
            )
            if (enabled) {
                Text(
                    "${formatHours(option.hoursOneWay)} each way",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun TierCompareRow(
    tier: Tier,
    estimate: TripEstimate,
    currency: Currency,
    current: Boolean,
    onApply: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(
                if (current) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLow
                },
            )
            .clickable(onClick = onApply)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                tier.label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                tier.blurb,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                formatMoney(estimate.totalUsd, currency),
                style = MaterialTheme.typography.titleSmall,
                color = if (estimate.withinBudget) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
            )
            Text(
                if (estimate.withinBudget) "fits" else "over",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ActionPill(label: String, onClick: () -> Unit) {
    Box(
        Modifier
            .clip(PillShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), PillShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
