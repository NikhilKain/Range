@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.vythera.range.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.navigationBars
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
import androidx.compose.material.icons.rounded.DirectionsBus
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Hotel
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.vythera.range.ui.components.ButtonGroup
import com.vythera.range.ui.components.CountStepper
import com.vythera.range.ui.components.ExpressiveLoader
import com.vythera.range.ui.components.Motion
import com.vythera.range.ui.components.RangeChip
import com.vythera.range.ui.components.RangeMark
import com.vythera.range.ui.components.ShapeBlob
import com.vythera.range.ui.components.pressScale
import com.vythera.range.ui.components.rememberInteraction
import com.vythera.range.ui.components.transportIcon
import com.vythera.range.ui.state.ExploreState
import com.vythera.range.ui.theme.ExpressiveShapes
import com.vythera.range.ui.theme.PillShape
import com.vythera.range.ui.theme.RangePalette
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

private val dateFormat = DateTimeFormatter.ofPattern("EEE d MMM")

/**
 * The composer. Four plain questions — how much, when and who, how you'd get
 * there, how comfortable — and a permanent answer bar at the bottom. Anything
 * fiddly hides behind "More options".
 */
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
    var splitTiers by remember { mutableStateOf(false) }
    var showMore by remember { mutableStateOf(false) }
    val origin = OriginCatalog.find(query.originId)
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(modifier.fillMaxSize()) {
        AuroraBackground(Modifier.fillMaxSize()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(scroll)
                    .padding(horizontal = 18.dp)
                    .padding(top = topInset + 8.dp, bottom = bottomInset + 122.dp),
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    RangeMark(Modifier.size(38.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(
                        "RANGE",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.W800,
                        letterSpacing = 4.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                    IconBubble(Icons.Rounded.BookmarkBorder, "Saved", onOpenSaved)
                    Spacer(Modifier.width(8.dp))
                    IconBubble(Icons.Rounded.Settings, "Settings", onOpenSettings)
                }

                Spacer(Modifier.height(22.dp))

                // ---------- 1. the money ----------
                StepLabel(1, "How much can you spend?")
                Spacer(Modifier.height(6.dp))
                Box(contentAlignment = Alignment.Center) {
                    ShapeBlob(
                        Modifier
                            .size(230.dp)
                            .scale(1f, 0.58f),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        lobes = 7,
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        AnimatedNumber(
                            text = formatMoney(query.budgetUsd, currency),
                            style = MaterialTheme.typography.displayMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            if (query.budgetIsPerPerson) {
                                "each · ${formatMoney(query.totalBudgetUsd, currency)} in total"
                            } else {
                                "total, everything included"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                BudgetTape(
                    valueUsd = query.budgetUsd,
                    currency = currency,
                    onValueChange = { v -> onQueryChange { it.copy(budgetUsd = v) } },
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    quickBudgets(currency).forEach { (label, usd) ->
                        RangeChip(
                            label = label,
                            selected = kotlin.math.abs(query.budgetUsd - usd) < 1.0,
                            onClick = { onQueryChange { it.copy(budgetUsd = usd) } },
                        )
                    }
                    RangeChip(
                        label = "Per person",
                        selected = query.budgetIsPerPerson,
                        onClick = {
                            onQueryChange { it.copy(budgetIsPerPerson = !it.budgetIsPerPerson) }
                        },
                        accent = RangePalette.Sky,
                    )
                }

                Spacer(Modifier.height(30.dp))

                // ---------- 2. when and who ----------
                StepLabel(2, "When, and who's coming?")
                Spacer(Modifier.height(12.dp))
                BigTile(
                    icon = Icons.Rounded.CalendarMonth,
                    label = "LEAVING",
                    value = query.departDate.format(dateFormat),
                    hint = daysAway(query.departDate),
                    container = MaterialTheme.colorScheme.secondaryContainer,
                    onContainer = MaterialTheme.colorScheme.onSecondaryContainer,
                    onClick = { showDatePicker = true },
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    StepperTile(
                        icon = Icons.Rounded.NightsStay,
                        label = "NIGHTS",
                        value = query.nights,
                        min = 0,
                        max = 30,
                        onChange = { n -> onQueryChange { it.copy(nights = n) } },
                        modifier = Modifier.weight(1f),
                    )
                    StepperTile(
                        icon = Icons.Rounded.Groups,
                        label = "TRAVELLERS",
                        value = query.travelers,
                        min = 1,
                        max = 12,
                        onChange = { n -> onQueryChange { it.copy(travelers = n) } },
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    "${query.nights + 1} days on the ground · " +
                        roomsFor(query).let { "$it room${if (it == 1) "" else "s"}" } +
                        " · from ${origin.city}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Spacer(Modifier.height(30.dp))

                // ---------- 3. how you'd travel ----------
                StepLabel(3, "How would you travel?")
                Spacer(Modifier.height(4.dp))
                Text(
                    "Pick anything you'd consider. Range uses the cheapest one that works.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
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
                        "Travel costs excluded — pricing the stay only.",
                        style = MaterialTheme.typography.bodySmall,
                        color = RangePalette.Violet,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }

                Spacer(Modifier.height(30.dp))

                // ---------- 4. comfort ----------
                StepLabel(4, "How comfortable?")
                Spacer(Modifier.height(12.dp))

                AnimatedVisibility(
                    visible = !splitTiers,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    Column {
                        ButtonGroup(
                            options = Tier.entries,
                            selected = query.stay,
                            onSelect = { t ->
                                onQueryChange { it.copy(stay = t, food = t, experience = t) }
                            },
                            label = { it.label },
                            icon = {
                                when (it) {
                                    Tier.BUDGET -> Icons.Rounded.DirectionsBus
                                    Tier.MID -> Icons.Rounded.Hotel
                                    Tier.LUXURY -> Icons.Rounded.Star
                                }
                            },
                        )
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "${query.stay.stayBlurb} · ${query.stay.foodBlurb.lowercase()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                AnimatedVisibility(
                    visible = splitTiers,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    Column {
                        TierBlock("Stay", Icons.Rounded.Hotel, query.stay, query.stay.stayBlurb) { t ->
                            onQueryChange { it.copy(stay = t) }
                        }
                        Spacer(Modifier.height(16.dp))
                        TierBlock(
                            "Food",
                            Icons.Rounded.Restaurant,
                            query.food,
                            query.food.foodBlurb,
                        ) { t -> onQueryChange { it.copy(food = t) } }
                        Spacer(Modifier.height(16.dp))
                        TierBlock(
                            "Things to do",
                            Icons.Rounded.Tune,
                            query.experience,
                            query.experience.experienceBlurb,
                        ) { t -> onQueryChange { it.copy(experience = t) } }
                    }
                }

                Spacer(Modifier.height(10.dp))
                TextLink(
                    if (splitTiers) {
                        "Use one level for everything"
                    } else {
                        "Set stay, food and activities separately"
                    },
                ) {
                    if (splitTiers) onQueryChange { it.copy(food = it.stay, experience = it.stay) }
                    splitTiers = !splitTiers
                }

                Row(
                    Modifier
                        .clip(PillShape)
                        .clickable { showMore = !showMore }
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "More options",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    val rot by animateFloatAsState(
                        if (showMore) 180f else 0f,
                        Motion.snappy,
                        label = "moreRot",
                    )
                    Icon(
                        Icons.Rounded.ExpandMore,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .size(18.dp)
                            .rotate(rot),
                    )
                }

                AnimatedVisibility(
                    visible = showMore,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(26.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .padding(16.dp),
                    ) {
                        OptionRow("Starting city", origin.city) {
                            TextLink("Change", onOpenOrigin)
                        }
                        OptionRow("People per room", "Shared rooms cost less") {
                            CountStepper(
                                value = query.peoplePerRoom,
                                onChange = { n -> onQueryChange { it.copy(peoplePerRoom = n) } },
                                min = 1,
                                max = 4,
                            )
                        }
                        OptionRow("Safety buffer", "${query.bufferPercent}% on top of everything") {
                            CountStepper(
                                value = query.bufferPercent,
                                onChange = { n -> onQueryChange { it.copy(bufferPercent = n) } },
                                min = 0,
                                max = 25,
                                suffix = "%",
                            )
                        }
                        OptionRow("Visa & entry fees", "Where they apply") {
                            Switch(
                                checked = query.includeVisa,
                                onCheckedChange = { on ->
                                    onQueryChange { it.copy(includeVisa = on) }
                                },
                            )
                        }
                        OptionRow("Travel insurance", "Recommended abroad") {
                            Switch(
                                checked = query.includeInsurance,
                                onCheckedChange = { on ->
                                    onQueryChange { it.copy(includeInsurance = on) }
                                },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
            }
        }

        ResultBar(
            explore = explore,
            onSearch = onSearch,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 18.dp)
                .padding(bottom = bottomInset + 14.dp),
        )
    }

    if (showDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = query.departDate
                .atStartOfDay(ZoneId.of("UTC"))
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
            DatePicker(state = state)
        }
    }
}

@Composable
private fun StepLabel(number: Int, title: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier
                .size(28.dp)
                .clip(ExpressiveShapes.cookie)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                "$number",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.W800,
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun BigTile(
    icon: ImageVector,
    label: String,
    value: String,
    hint: String,
    container: Color,
    onContainer: Color,
    onClick: () -> Unit,
) {
    val interaction = rememberInteraction()
    Row(
        Modifier
            .fillMaxWidth()
            .pressScale(interaction)
            .clip(RoundedCornerShape(28.dp))
            .background(container)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = onContainer, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = onContainer.copy(alpha = 0.75f),
                letterSpacing = 1.2.sp,
            )
            Text(value, style = MaterialTheme.typography.headlineSmall, color = onContainer)
        }
        Text(
            hint,
            style = MaterialTheme.typography.labelMedium,
            color = onContainer.copy(alpha = 0.75f),
        )
    }
}

@Composable
private fun StepperTile(
    icon: ImageVector,
    label: String,
    value: Int,
    min: Int,
    max: Int,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(horizontal = 12.dp, vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(15.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                letterSpacing = 1.1.sp,
            )
        }
        Spacer(Modifier.height(8.dp))
        CountStepper(value = value, onChange = onChange, min = min, max = max)
    }
}

@Composable
private fun TierBlock(
    label: String,
    icon: ImageVector,
    tier: Tier,
    blurb: String,
    onSelect: (Tier) -> Unit,
) {
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                icon,
                null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                label,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                blurb,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
        Spacer(Modifier.height(8.dp))
        ButtonGroup(
            options = Tier.entries,
            selected = tier,
            onSelect = onSelect,
            label = { it.label },
        )
    }
}

@Composable
private fun OptionRow(title: String, subtitle: String, control: @Composable () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyLarge,
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
private fun TextLink(text: String, onClick: () -> Unit) {
    Text(
        text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .clip(PillShape)
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 4.dp),
    )
}

@Composable
private fun IconBubble(icon: ImageVector, description: String, onClick: () -> Unit) {
    val interaction = rememberInteraction()
    Box(
        Modifier
            .size(42.dp)
            .pressScale(interaction)
            .clip(ExpressiveShapes.squircle)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            description,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(19.dp),
        )
    }
}

/**
 * The answer, always on screen: how many places the current budget reaches, and
 * the way through to them. No hunting for a submit button.
 */
@Composable
private fun ResultBar(
    explore: ExploreState,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = rememberInteraction()
    val s = explore.summary
    Row(
        modifier
            .fillMaxWidth()
            .pressScale(interaction)
            .clip(RoundedCornerShape(34.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(RangePalette.Aurora, RangePalette.Lagoon, RangePalette.Sky),
                ),
            )
            .clickable(
                enabled = !explore.computing,
                interactionSource = interaction,
                indication = null,
                onClick = onSearch,
            )
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (explore.computing) {
            ExpressiveLoader(Modifier.size(26.dp), color = Color(0xFF04121B))
            Spacer(Modifier.width(14.dp))
            Text(
                "Working out your range…",
                style = MaterialTheme.typography.titleSmall,
                color = Color(0xFF04121B),
                fontWeight = FontWeight.W700,
            )
        } else {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    AnimatedNumber(
                        text = "${s.inRange}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color(0xFF04121B),
                    )
                    Text(
                        " places in reach",
                        style = MaterialTheme.typography.titleSmall,
                        color = Color(0xFF04121B),
                        fontWeight = FontWeight.W700,
                        modifier = Modifier.padding(bottom = 3.dp),
                    )
                }
                Text(
                    if (s.inRange > 0) {
                        "${s.countries} countries · best value: " +
                            (s.bestValue?.destination?.city ?: "—")
                    } else {
                        "Nothing fits yet — try more budget or fewer nights"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF04121B).copy(alpha = 0.78f),
                    maxLines = 1,
                )
            }
            Spacer(Modifier.width(12.dp))
            Box(
                Modifier
                    .size(46.dp)
                    .clip(ExpressiveShapes.cookie)
                    .background(Color(0xFF04121B).copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.ArrowForward,
                    "See them all",
                    tint = Color(0xFF04121B),
                    modifier = Modifier.size(21.dp),
                )
            }
        }
    }
}

private fun quickBudgets(currency: Currency): List<Pair<String, Double>> {
    val locals = when (currency) {
        Currency.INR -> listOf(25_000.0, 50_000.0, 100_000.0, 200_000.0)
        else -> listOf(300.0, 700.0, 1500.0, 3000.0)
    }
    return locals.map { local ->
        val label = when {
            currency == Currency.INR && local >= 100_000 ->
                "${currency.symbol}${(local / 100_000).toInt()}L"
            local >= 1000 -> "${currency.symbol}${(local / 1000).toInt()}k"
            else -> "${currency.symbol}${local.toInt()}"
        }
        label to local / currency.perUsd
    }
}

private fun roomsFor(q: TripQuery): Int =
    kotlin.math.ceil(q.travelers / q.peoplePerRoom.coerceAtLeast(1).toDouble()).toInt()

private fun daysAway(date: LocalDate): String {
    val days = ChronoUnit.DAYS.between(LocalDate.now(), date)
    return when {
        days < 0 -> "past"
        days == 0L -> "today"
        days == 1L -> "tomorrow"
        days < 31 -> "in $days days"
        else -> "in ${days / 30} month${if (days / 30 == 1L) "" else "s"}"
    }
}
