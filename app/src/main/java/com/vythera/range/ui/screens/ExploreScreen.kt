@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FilterList
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.FloatingToolbarExitDirection
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.semantics.Role
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
import com.vythera.range.ui.components.ExpressiveLoader
import com.vythera.range.ui.components.GlassCard
import com.vythera.range.ui.components.Motion
import com.vythera.range.ui.components.RangeChip
import com.vythera.range.ui.components.RangeRadar
import com.vythera.range.ui.components.SectionHeader
import com.vythera.range.ui.components.pressScale
import com.vythera.range.ui.components.rememberInteraction
import com.vythera.range.ui.state.ExploreState
import com.vythera.range.ui.state.Filters
import com.vythera.range.ui.state.Scope
import com.vythera.range.ui.state.SortMode
import com.vythera.range.ui.theme.CardShape
import com.vythera.range.ui.theme.PillShape
import kotlinx.coroutines.launch

@Composable
fun ExploreScreen(
    state: ExploreState,
    filters: Filters,
    currency: Currency,
    wishlist: Set<String>,
    originCity: String,
    originCountry: String,
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
    // Collapsed by default: at full height the radar filled over half the
    // viewport, so the first actual result sat below the fold on every search.
    // The summary line above it carries the headline numbers either way.
    var radarExpanded by remember { mutableStateOf(false) }
    val topInset = androidx.compose.foundation.layout.WindowInsets.statusBars
        .asPaddingValues().calculateTopPadding()
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val toolbarScroll = FloatingToolbarDefaults.exitAlwaysScrollBehavior(
        exitDirection = FloatingToolbarExitDirection.Bottom,
    )

    AuroraBackground(modifier.fillMaxSize(), intensity = 0.7f) {
        LazyColumn(
            state = listState,
            // Feeds scroll deltas to the floating toolbar so it can hide itself.
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(toolbarScroll),
            // Horizontal padding lives on the individual items, not here, so the
            // filter rail below can run edge to edge and still scroll its last
            // chip fully into view. With padding on the list, chips were being
            // clipped at the gutter no matter how far you scrolled.
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                top = topInset + 8.dp,
                bottom = 120.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item("bar") {
                Row(
                    Modifier.padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircleButton(Icons.Rounded.ArrowBack, onBack, "Back")
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
                    // Sort and filter moved to the floating toolbar below —
                    // easier to reach one-handed, and it keeps the header to
                    // just identity and the headline number.
                }
            }

            item("summary") {
                Box(Modifier.padding(horizontal = 20.dp)) {
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
            }

            item("quickfilters") {
                // Full-bleed rail: its own gutter comes from contentPadding, so
                // chips slide right up to both screen edges instead of stopping
                // dead at a clipped boundary.
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 20.dp,
                    ),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(Scope.entries.toList(), key = { "scope_${it.name}" }) { option ->
                        RangeChip(
                            label = when (option) {
                                Scope.DOMESTIC -> "In $originCountry"
                                else -> option.label
                            },
                            selected = filters.scope == option,
                            onClick = { onFilters { it.copy(scope = option) } },
                            accent = MaterialTheme.colorScheme.primary,
                        )
                    }
                    item("onlyInRange") {
                        RangeChip(
                            label = if (filters.onlyInRange) {
                                "In range only"
                            } else {
                                "Showing everything"
                            },
                            selected = filters.onlyInRange,
                            onClick = { onFilters { it.copy(onlyInRange = !it.onlyInRange) } },
                        )
                    }
                    item("visa") {
                        RangeChip(
                            label = "Easy visa",
                            selected = filters.visaEasyOnly,
                            onClick = { onFilters { it.copy(visaEasyOnly = !it.visaEasyOnly) } },
                        )
                    }
                    items(Vibe.entries.take(7), key = { "vibe_${it.name}" }) { v ->
                        RangeChip(
                            label = v.label,
                            selected = v in filters.vibes,
                            onClick = { onToggleVibe(v) },
                            accent = MaterialTheme.colorScheme.tertiary,
                        )
                    }
                }
            }

            if (state.computing) {
                item("loading") {
                    Column(
                        Modifier.fillMaxWidth().padding(vertical = 60.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        ExpressiveLoader(Modifier.size(58.dp))
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Pricing the world…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else if (state.visible.isEmpty()) {
                item("empty") {
                    Box(Modifier.padding(horizontal = 20.dp)) {
                        EmptyState(state, currency, onClearFilters)
                    }
                }
            }

            itemsIndexed(state.visible, key = { _, e -> e.destination.id }) { index, estimate ->
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
                        .padding(horizontal = 20.dp)
                        .graphicsAlpha(alpha),
                ) {
                    DestinationCard(
                        estimate = estimate,
                        currency = currency,
                        wishlisted = estimate.destination.id in wishlist,
                        onClick = { onOpen(estimate) },
                        onWishlist = { onWishlist(estimate.destination.id) },
                        hero = index == 0,
                        rankLabel = when {
                            index == 0 && filters.sort == SortMode.BEST_VALUE -> "BEST VALUE"
                            index == 0 && filters.sort == SortMode.CHEAPEST -> "CHEAPEST"
                            index == 0 && filters.sort == SortMode.FARTHEST -> "FARTHEST"
                            index == 0 -> "TOP PICK"
                            else -> null
                        },
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
                    modifier = Modifier.padding(top = 12.dp, start = 20.dp, end = 20.dp),
                )
            }
        }

        // ── Floating toolbar ────────────────────────────────────────────────
        // The Expressive answer to a bottom app bar: a pill that hovers over the
        // content instead of walling it off. `exitAlwaysScrollBehavior` slides it
        // off-screen as you scroll down into the results and brings it straight
        // back on the first upward flick — so it never permanently covers a card,
        // and you never have to scroll back up to reach the controls.
        HorizontalFloatingToolbar(
            expanded = true,
            scrollBehavior = toolbarScroll,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp + navInset),
            colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
        ) {
            FilledIconButton(
                onClick = { showSort = true },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            ) {
                Icon(Icons.Rounded.Sort, "Sort results")
            }

            Box {
                FilledIconButton(
                    onClick = { showFilters = true },
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                ) {
                    Icon(Icons.Rounded.FilterList, "Filter results")
                }
                if (filters.activeCount > 0) {
                    Box(
                        Modifier
                            .align(Alignment.TopEnd)
                            .size(17.dp)
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.onPrimaryContainer),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "${filters.activeCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primaryContainer,
                            fontSize = 9.sp,
                        )
                    }
                }
            }

            // No count here on purpose: the reach card at the top of the list
            // already states "19 of 180", and a second, differently-scoped
            // number in the toolbar just made the two disagree.
            FilledIconButton(
                onClick = { scope.launch { listState.animateScrollToItem(0) } },
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    contentColor = MaterialTheme.colorScheme.primaryContainer,
                ),
            ) {
                Icon(Icons.Rounded.ArrowUpward, "Back to top")
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
                            accent = MaterialTheme.colorScheme.secondary,
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
                            accent = MaterialTheme.colorScheme.tertiary,
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
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        " of ${s.total} places",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 5.dp),
                    )
                }
                Text(
                    "${s.domesticInRange} at home · ${s.internationalInRange} abroad · " +
                        "${s.countries} countries" +
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
                            // An 18dp hit area is well under Material's 48dp
                            // minimum — the icon stays small, the target does not.
                            Box(
                                Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(50))
                                    .clickable(
                                        onClickLabel = "Clear selection",
                                        role = Role.Button,
                                    ) { onSelect(null) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Rounded.Close,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
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
    contentDescription: String? = null,
) {
    val interaction = rememberInteraction()
    Box(
        Modifier
            .size(48.dp)
            .pressScale(interaction)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(50))
            .clickable(
                interactionSource = interaction,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
    }
}

private fun Modifier.graphicsAlpha(a: Float) = this.alpha(a)
