@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.animation.ExperimentalSharedTransitionApi::class,
    androidx.compose.ui.ExperimentalComposeUiApi::class,
)

package com.vythera.range.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.BookmarkBorder
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Hotel
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vythera.range.data.OriginCatalog
import com.vythera.range.data.model.Tier
import com.vythera.range.data.model.TransportMode
import com.vythera.range.domain.Currency
import com.vythera.range.domain.TripQuery
import com.vythera.range.domain.formatMoney
import com.vythera.range.ui.components.AnimatedNumber
import com.vythera.range.ui.components.AuroraBackground
import com.vythera.range.ui.components.BudgetTape
import com.vythera.range.ui.components.CountStepper
import com.vythera.range.ui.components.GlassCard
import com.vythera.range.ui.components.Motion
import com.vythera.range.ui.components.RangeChip
import com.vythera.range.ui.components.RangeMark
import com.vythera.range.ui.components.SectionHeader
import com.vythera.range.ui.components.SegmentedSelector
import com.vythera.range.ui.components.pressScale
import com.vythera.range.ui.components.rememberInteraction
import com.vythera.range.ui.components.transportIcon
import com.vythera.range.ui.state.ExploreState
import com.vythera.range.ui.theme.CardShape
import com.vythera.range.ui.theme.PillShape
import com.vythera.range.ui.theme.RangePalette
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val dateFormat = DateTimeFormatter.ofPattern("EEE, d MMM yyyy")

@Composable
fun HomeScreen(
    query: TripQuery,
    currency: Currency,
    explore: ExploreState,
    onQueryChange: ((TripQuery) -> TripQuery) -> Unit,
    onToggleMode: (TransportMode) -> Unit,
    onOpenOrigin: () -> Unit,
    onOpenSaved: () -> Unit,
    onOpenSettings: () -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scroll = rememberScrollState()
    var showDatePicker by remember { mutableStateOf(false) }
    var showAdvanced by remember { mutableStateOf(false) }
    val origin = OriginCatalog.find(query.originId)
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    AuroraBackground(modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scroll)
                .padding(horizontal = 20.dp)
                .padding(top = topInset + 8.dp, bottom = 120.dp),
        ) {
            // ---- top bar ----
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RangeMark(Modifier.size(40.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "RANGE",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.W800,
                        letterSpacing = 4.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        "How far can your money take you?",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconBubble(Icons.Rounded.BookmarkBorder, onOpenSaved)
                Spacer(Modifier.width(8.dp))
                IconBubble(Icons.Rounded.Settings, onOpenSettings)
            }

            Spacer(Modifier.height(22.dp))

            // ---- origin ----
            Row(
                Modifier
                    .clip(PillShape)
                    .background(MaterialTheme.colorScheme.surfaceContainerLow)
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant, PillShape)
                    .clickable(onClick = onOpenOrigin)
                    .padding(horizontal = 14.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    Icons.Rounded.MyLocation,
                    null,
                    tint = RangePalette.Aurora,
                    modifier = Modifier.size(15.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Starting from ${origin.city}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.width(6.dp))
                Icon(
                    Icons.Rounded.ExpandMore,
                    null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp),
                )
            }

            Spacer(Modifier.height(20.dp))

            // ---- budget hero ----
            GlassCard(contentPadding = 22.dp) {
                Text(
                    if (query.budgetIsPerPerson) "MY BUDGET, PER PERSON" else "MY TOTAL BUDGET",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.6.sp,
                )
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    AnimatedNumber(
                        text = formatMoney(query.budgetUsd, currency),
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.width(8.dp))
                    if (query.budgetIsPerPerson && query.travelers > 1) {
                        Text(
                            "= ${formatMoney(query.totalBudgetUsd, currency)} total",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 8.dp),
                        )
                    }
                }

                BudgetTape(
                    valueUsd = query.budgetUsd,
                    currency = currency,
                    onValueChange = { v -> onQueryChange { it.copy(budgetUsd = v) } },
                )

                Spacer(Modifier.height(6.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Drag the ruler",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "Per person",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(6.dp))
                        Switch(
                            checked = query.budgetIsPerPerson,
                            onCheckedChange = { on -> onQueryChange { it.copy(budgetIsPerPerson = on) } },
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    quickBudgets(currency).forEach { (label, usd) ->
                        RangeChip(
                            label = label,
                            selected = kotlin.math.abs(query.budgetUsd - usd) < 1.0,
                            onClick = { onQueryChange { it.copy(budgetUsd = usd) } },
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // ---- when / how long / who ----
            GlassCard {
                SectionHeader("THE TRIP")
                Spacer(Modifier.height(14.dp))

                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .clickable { showDatePicker = true }
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        Icons.Rounded.CalendarMonth,
                        null,
                        tint = RangePalette.Sky,
                        modifier = Modifier.size(19.dp),
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "DEPARTING",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            query.departDate.format(dateFormat),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    Text(
                        daysAway(query.departDate),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Spacer(Modifier.height(12.dp))
                StepperRow(
                    icon = Icons.Rounded.NightsStay,
                    title = "Nights away",
                    subtitle = "${query.nights + 1} days on the ground",
                ) {
                    CountStepper(
                        value = query.nights,
                        onChange = { n -> onQueryChange { it.copy(nights = n) } },
                        min = 0,
                        max = 30,
                    )
                }

                Spacer(Modifier.height(12.dp))
                StepperRow(
                    icon = Icons.Rounded.Groups,
                    title = "Travellers",
                    subtitle = if (query.travelers == 1) "Solo trip" else "${roomsFor(query)} room(s)",
                ) {
                    CountStepper(
                        value = query.travelers,
                        onChange = { n -> onQueryChange { it.copy(travelers = n) } },
                        min = 1,
                        max = 12,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ---- how you'll travel ----
            GlassCard {
                SectionHeader("HOW YOU'LL GET THERE")
                Spacer(Modifier.height(6.dp))
                Text(
                    "Pick everything you'd consider — Range prices each one and uses the cheapest that works.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(14.dp))
                FlowChips {
                    TransportMode.entries.forEach { mode ->
                        RangeChip(
                            label = mode.label,
                            selected = mode in query.modes,
                            onClick = { onToggleMode(mode) },
                            icon = transportIcon(mode),
                            accent = if (mode == TransportMode.NONE) {
                                RangePalette.Violet
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                        )
                    }
                }
                AnimatedVisibility(visible = TransportMode.NONE in query.modes) {
                    Text(
                        "Travel costs excluded — you're pricing the stay only.",
                        style = MaterialTheme.typography.bodySmall,
                        color = RangePalette.Violet,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // ---- comfort tiers ----
            GlassCard {
                SectionHeader("YOUR STYLE")
                Spacer(Modifier.height(14.dp))
                TierRow(
                    icon = Icons.Rounded.Hotel,
                    label = "Stay",
                    tier = query.stay,
                    onSelect = { t -> onQueryChange { it.copy(stay = t) } },
                )
                Spacer(Modifier.height(14.dp))
                TierRow(
                    icon = Icons.Rounded.Restaurant,
                    label = "Food",
                    tier = query.food,
                    onSelect = { t -> onQueryChange { it.copy(food = t) } },
                )
                Spacer(Modifier.height(14.dp))
                TierRow(
                    icon = Icons.Rounded.Tune,
                    label = "Things to do",
                    tier = query.experience,
                    onSelect = { t -> onQueryChange { it.copy(experience = t) } },
                )

                Spacer(Modifier.height(16.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clip(PillShape)
                        .clickable { showAdvanced = !showAdvanced }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Fine tuning",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(4.dp))
                    val rot by animateFloatAsState(
                        if (showAdvanced) 180f else 0f,
                        Motion.snappy,
                        label = "advRot",
                    )
                    Icon(
                        Icons.Rounded.ExpandMore,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp).rotate(rot),
                    )
                }

                AnimatedVisibility(
                    visible = showAdvanced,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    Column {
                        ToggleRow(
                            "Share rooms",
                            "${query.peoplePerRoom} per room",
                        ) {
                            CountStepper(
                                value = query.peoplePerRoom,
                                onChange = { n -> onQueryChange { it.copy(peoplePerRoom = n) } },
                                min = 1,
                                max = 4,
                            )
                        }
                        ToggleRow("Include visa & entry fees", "Where they apply") {
                            Switch(
                                checked = query.includeVisa,
                                onCheckedChange = { on -> onQueryChange { it.copy(includeVisa = on) } },
                            )
                        }
                        ToggleRow("Include travel insurance", "Recommended abroad") {
                            Switch(
                                checked = query.includeInsurance,
                                onCheckedChange = { on ->
                                    onQueryChange { it.copy(includeInsurance = on) }
                                },
                            )
                        }
                        ToggleRow("Safety buffer", "${query.bufferPercent}% on top") {
                            CountStepper(
                                value = query.bufferPercent,
                                onChange = { n -> onQueryChange { it.copy(bufferPercent = n) } },
                                min = 0,
                                max = 25,
                                suffix = "%",
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // ---- live teaser ----
            AnimatedVisibility(
                visible = !explore.computing && explore.all.isNotEmpty(),
                enter = fadeIn(tween(400)) + slideInVertically { it / 3 },
            ) {
                LiveTeaser(explore, currency)
            }

            Spacer(Modifier.height(20.dp))

            SearchButton(
                enabled = !explore.computing,
                count = explore.summary.inRange,
                onClick = onSearch,
            )
        }
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = query.departDate
                .atStartOfDay(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableYear(year: Int) = year >= LocalDate.now().year
            },
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        val picked = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.of("UTC"))
                            .toLocalDate()
                        onQueryChange { it.copy(departDate = picked) }
                    }
                    showDatePicker = false
                }) { Text("Set date") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            },
            colors = DatePickerDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
        ) {
            DatePicker(
                state = state,
                title = {
                    Text(
                        "When do you want to leave?",
                        Modifier.padding(start = 24.dp, top = 20.dp),
                        style = MaterialTheme.typography.titleMedium,
                    )
                },
            )
        }
    }
}

@Composable
private fun IconBubble(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
    val interaction = rememberInteraction()
    Box(
        Modifier
            .size(40.dp)
            .pressScale(interaction)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(50))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp),
        )
    }
}

@Composable
private fun StepperRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    control: @Composable () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = RangePalette.Lagoon, modifier = Modifier.size(19.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        control()
    }
}

@Composable
private fun TierRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tier: Tier,
    onSelect: (Tier) -> Unit,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = RangePalette.Sand, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                tier.blurb,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        Spacer(Modifier.height(8.dp))
        SegmentedSelector(
            options = Tier.entries,
            selected = tier,
            onSelect = onSelect,
            label = { it.label },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, control: @Composable () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        control()
    }
}

@Composable
private fun FlowChips(content: @Composable () -> Unit) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) { content() }
}

@Composable
private fun LiveTeaser(explore: ExploreState, currency: Currency) {
    val s = explore.summary
    GlassCard(tone = MaterialTheme.colorScheme.surfaceContainerLow) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "RIGHT NOW, THAT REACHES",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    letterSpacing = 1.4.sp,
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    AnimatedNumber(
                        text = "${s.inRange}",
                        style = MaterialTheme.typography.displaySmall,
                        color = RangePalette.Aurora,
                    )
                    Text(
                        " places · ${s.countries} countries",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 5.dp),
                    )
                }
                s.farthestCity?.let {
                    Text(
                        "Farthest: $it, ${(s.farthestKm / 100).toInt() / 10.0}k km away",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            s.bestValue?.let { best ->
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "BEST VALUE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        best.destination.city,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        formatMoney(best.totalUsd, currency),
                        style = MaterialTheme.typography.bodySmall,
                        color = RangePalette.Lagoon,
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchButton(enabled: Boolean, count: Int, onClick: () -> Unit) {
    val interaction = rememberInteraction()
    val scale by animateFloatAsState(if (enabled) 1f else 0.98f, Motion.snappy, label = "cta")
    Box(
        Modifier
            .fillMaxWidth()
            .height(62.dp)
            .pressScale(interaction)
            .clip(PillShape)
            .background(
                Brush.horizontalGradient(
                    listOf(RangePalette.Aurora, RangePalette.Lagoon, RangePalette.Sky),
                ),
            )
            .clickable(
                enabled = enabled,
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                if (count > 0) "Show me all $count" else "Find my range",
                style = MaterialTheme.typography.titleMedium,
                color = Color(0xFF04121B),
                fontWeight = FontWeight.W800,
            )
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.Rounded.ArrowForward,
                null,
                tint = Color(0xFF04121B),
                modifier = Modifier.size(20.dp).scaleBy(scale),
            )
        }
    }
}

private fun Modifier.scaleBy(v: Float) = this.then(androidx.compose.ui.draw.scale(v))

private fun quickBudgets(currency: Currency): List<Pair<String, Double>> {
    val locals = when (currency) {
        Currency.INR -> listOf(25_000.0, 50_000.0, 100_000.0, 200_000.0)
        else -> listOf(300.0, 700.0, 1500.0, 3000.0)
    }
    return locals.map { local ->
        val label = when {
            currency == Currency.INR && local >= 100_000 -> "${currency.symbol}${(local / 100_000).toInt()}L"
            local >= 1000 -> "${currency.symbol}${(local / 1000).toInt()}k"
            else -> "${currency.symbol}${local.toInt()}"
        }
        label to local / currency.perUsd
    }
}

private fun roomsFor(q: TripQuery): Int =
    kotlin.math.ceil(q.travelers / q.peoplePerRoom.coerceAtLeast(1).toDouble()).toInt()

private fun daysAway(date: LocalDate): String {
    val days = java.time.temporal.ChronoUnit.DAYS.between(LocalDate.now(), date)
    return when {
        days < 0 -> "in the past"
        days == 0L -> "today"
        days == 1L -> "tomorrow"
        days < 31 -> "in $days days"
        else -> "in ${days / 30} month${if (days / 30 == 1L) "" else "s"}"
    }
}
