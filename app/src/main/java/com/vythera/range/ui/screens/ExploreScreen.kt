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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vythera.range.data.model.Region
import com.vythera.range.data.model.Vibe
import com.vythera.range.domain.Currency
import com.vythera.range.domain.TripEstimate
import com.vythera.range.domain.formatMoney
import com.vythera.range.ui.components.AnimatedNumber
import com.vythera.range.ui.components.AuroraBackground
import com.vythera.range.ui.components.DestinationCard
import com.vythera.range.ui.components.GlassCard
import com.vythera.range.ui.components.Motion
import com.vythera.range.ui.components.RangeChip
import com.vythera.range.ui.components.RangeRadar
import com.vythera.range.ui.components.SectionHeader
import com.vythera.range.ui.components.pressScale
import com.vythera.range.ui.components.rememberInteraction
import com.vythera.range.ui.state.ExploreState
import com.vythera.range.ui.state.Filters
import com.vythera.range.ui.state.SortMode
import com.vythera.range.ui.theme.CardShape
import com.vythera.range.ui.theme.PillShape
import com.vythera.range.ui.theme.RangePalette
import kotlinx.coroutines.launch

@Composable
fun ExploreScreen(
    state: ExploreState,
    filters: Filters,
    currency: Currency,
    wishlist: Set<String>,
    originCity: String,
    budgetUsd: Double,
    onFilters: ((Filters) -> Filters) -> Unit,
    onToggleRegion: (Region) -> Unit,
    onToggleVibe: (Vibe) -> Unit,
    onClearFilters: () -> Unit,
    onOpen: (TripEstimate) -> Unit,
    onWishlist: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var selected by remember { mutableStateOf<String?>(null) }
    var showFilters by remember { mutableStateOf(false) }
    var showSort by remember { mutableStateOf(false) }
    var radarExpanded by remember { mutableStateOf(true) }
    val topInset = androidx.compose.foundation.layout.WindowInsets.statusBars
        .asPaddingValues().calculateTopPadding()

    AuroraBackground(modifier.fillMaxSize(), intensity = 0.7f) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = topInset + 8.dp,
                bottom = 40.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item("bar") {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircleButton(Icons.Rounded.ArrowBack, onBack)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "YOUR RANGE FROM ${originCity.uppercase()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            letterSpacing = 1.4.sp,
                        )
                        Text(
                            formatMoney(budgetUsd, currency),
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                    CircleButton(Icons.Rounded.Sort) { showSort = true }
                    Spacer(Modifier.width(8.dp))
                    Box {
                        CircleButton(Icons.Rounded.FilterList) { showFilters = true }
                        if (filters.activeCount > 0) {
                            Box(
                                Modifier
                                    .align(Alignment.TopEnd)
                                    .size(16.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(RangePalette.Aurora),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    "${filters.activeCount}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF04121B),
                                    fontSize = 9.sp,
                                )
                            }
                        }
                    }
                }
            }

            item("summary") {
                ReachCard(
                    state = state,
                    currency = currency,
                    expanded = radarExpanded,
                    selected = selected,
                    onToggle = { radarExpanded = !radarExpanded },
                    onSelect = { est ->
                        selected = est?.destination?.id
                        if (est != null) {
                            val idx = state.visible.indexOfFirst { it.destination.id == est.destination.id }
                            if (idx >= 0) scope.launch { listState.animateScrollToItem(idx + 3) }
                        }
                    },
                )
            }

            item("quickfilters") {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    RangeChip(
                        label = if (filters.onlyInRange) "In range only" else "Showing everything",
                        selected = filters.onlyInRange,
                        onClick = { onFilters { it.copy(onlyInRange = !it.onlyInRange) } },
                    )
                    RangeChip(
                        label = filters.sort.label,
                        selected = true,
                        onClick = { showSort = true },
                        icon = Icons.Rounded.Sort,
                        accent = RangePalette.Sky,
                    )
                    RangeChip(
                        label = "Easy visa",
                        selected = filters.visaEasyOnly,
                        onClick = { onFilters { it.copy(visaEasyOnly = !it.visaEasyOnly) } },
                    )
                    Vibe.entries.take(7).forEach { v ->
                        RangeChip(
                            label = v.label,
                            selected = v in filters.vibes,
                            onClick = { onToggleVibe(v) },
                            accent = RangePalette.Violet,
                        )
                    }
                }
            }

            if (state.visible.isEmpty()) {
                item("empty") { EmptyState(state, currency, onClearFilters) }
            }

            items(state.visible, key = { it.destination.id }) { estimate ->
                var appeared by remember(estimate.destination.id) { mutableStateOf(false) }
                LaunchedEffect(estimate.destination.id) { appeared = true }
                val alpha by animateFloatAsState(
                    if (appeared) 1f else 0f,
                    tween(420),
                    label = "itemAlpha",
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .graphicsAlpha(alpha),
                ) {
                    DestinationCard(
                        estimate = estimate,
                        currency = currency,
                        wishlisted = estimate.destination.id in wishlist,
                        onClick = { onOpen(estimate) },
                        onWishlist = { onWishlist(estimate.destination.id) },
                    )
                }
            }

            item("footer") {
                Text(
                    "Prices are Range's own model — a realistic planning estimate built from " +
                        "distance, season, how far ahead you're booking and local cost of living. " +
                        "Not a live fare quote.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }

    if (showSort) {
        val sheet = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { showSort = false },
            sheetState = sheet,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
                SectionHeader("SORT BY")
                Spacer(Modifier.height(12.dp))
                SortMode.entries.forEach { mode ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                onFilters { it.copy(sort = mode) }
                                showSort = false
                            }
                            .padding(vertical = 14.dp, horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            mode.label,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (mode == filters.sort) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                        )
                    }
                }
            }
        }
    }

    if (showFilters) {
        val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showFilters = false },
            sheetState = sheet,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(
                Modifier
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 36.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Filters",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        "Clear all",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { onClearFilters() },
                    )
                }
                Spacer(Modifier.height(20.dp))
                SectionHeader("WHERE")
                Spacer(Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Region.entries.forEach { r ->
                        RangeChip(
                            label = r.label,
                            selected = r in filters.regions,
                            onClick = { onToggleRegion(r) },
                            accent = RangePalette.Sky,
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
                SectionHeader("VIBE")
                Spacer(Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Vibe.entries.forEach { v ->
                        RangeChip(
                            label = v.label,
                            selected = v in filters.vibes,
                            onClick = { onToggleVibe(v) },
                            accent = RangePalette.Violet,
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Only what I can afford",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            "Turn off to see near-misses too",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = filters.onlyInRange,
                        onCheckedChange = { on -> onFilters { it.copy(onlyInRange = on) } },
                    )
                }
            }
        }
    }
}

@Composable
private fun ReachCard(
    state: ExploreState,
    currency: Currency,
    expanded: Boolean,
    selected: String?,
    onToggle: () -> Unit,
    onSelect: (TripEstimate?) -> Unit,
) {
    val s = state.summary
    GlassCard(contentPadding = 18.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Bottom) {
                    AnimatedNumber(
                        text = "${s.inRange}",
                        style = MaterialTheme.typography.displaySmall,
                        color = RangePalette.Aurora,
                    )
                    Text(
                        " of ${s.total} places",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 5.dp),
                    )
                }
                Text(
                    "${s.countries} countries · ${s.regions} regions" +
                        (s.farthestCity?.let { " · as far as $it" } ?: ""),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            val rot by animateFloatAsState(if (expanded) 180f else 0f, Motion.snappy, label = "radarRot")
            Box(
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(50))
                    .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                    .clickable(onClick = onToggle),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Rounded.ExpandMore,
                    null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp).rotate(rot),
                )
            }
        }

        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Column {
                Spacer(Modifier.height(8.dp))
                RangeRadar(
                    estimates = state.all,
                    selectedId = selected,
                    onSelect = onSelect,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                )
                val chosen = state.all.firstOrNull { it.destination.id == selected }
                AnimatedVisibility(chosen != null) {
                    chosen?.let {
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "${it.destination.city}, ${it.destination.country}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                                Text(
                                    "${formatMoney(it.totalUsd, currency)} · " +
                                        "${(it.distanceKm / 100).toInt() / 10.0}k km ${it.mode.label.lowercase()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Icon(
                                Icons.Rounded.Close,
                                null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { onSelect(null) },
                            )
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    "Tap a dot to find it in the list",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                )
            }
        }
    }
}

@Composable
private fun EmptyState(state: ExploreState, currency: Currency, onClear: () -> Unit) {
    val nearest = state.all.minByOrNull { it.totalUsd }
    GlassCard {
        Text(
            "Nothing lands inside that yet",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))
        nearest?.let {
            Text(
                "The closest you get is ${it.destination.city} at " +
                    "${formatMoney(it.totalUsd, currency)} — " +
                    "about ${formatMoney(it.totalUsd - it.budgetUsd, currency)} more than your budget.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "Try fewer nights, a budget tier, or adding trains and buses to how you'd travel.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(14.dp))
        RangeChip(label = "Clear filters", selected = false, onClick = onClear, icon = Icons.Rounded.Tune)
    }
}

@Composable
private fun CircleButton(
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

private fun Modifier.graphicsAlpha(a: Float) = this.then(
    androidx.compose.ui.draw.alpha(a),
)
