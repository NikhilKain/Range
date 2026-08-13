@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.vythera.range.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.LocationCity
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vythera.range.data.DestinationCatalog
import com.vythera.range.data.OriginCatalog
import com.vythera.range.data.RangeSettings
import com.vythera.range.data.model.Place
import com.vythera.range.data.model.SavedTrip
import com.vythera.range.domain.Currency
import com.vythera.range.domain.formatMoney
import com.vythera.range.ui.components.AuroraBackground
import com.vythera.range.ui.components.ButtonGroup
import com.vythera.range.ui.components.ExpressiveLoader
import com.vythera.range.ui.components.DestinationArt
import com.vythera.range.ui.components.GlassCard
import com.vythera.range.ui.components.RangeChip
import com.vythera.range.ui.components.RangeMark
import com.vythera.range.ui.components.SectionHeader
import com.vythera.range.ui.theme.CardShape
import com.vythera.range.ui.theme.PillShape
import com.vythera.range.ui.theme.RangePalette
import com.vythera.range.ui.theme.ThemeMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val savedFormat = DateTimeFormatter.ofPattern("d MMM yyyy")

private fun relativeTime(epochMs: Long): String {
    val minutes = (System.currentTimeMillis() - epochMs) / 60_000
    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "$minutes min ago"
        minutes < 60 * 24 -> "${minutes / 60}h ago"
        else -> "${minutes / (60 * 24)}d ago"
    }
}

@Composable
private fun ScreenScaffold(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    content: @Composable () -> Unit,
) {
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    AuroraBackground(Modifier.fillMaxSize(), intensity = 0.55f) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(top = topInset + 8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(50))
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        Icons.Rounded.ArrowBack,
                        "Back",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            content()
        }
    }
}

@Composable
fun SavedScreen(
    trips: List<SavedTrip>,
    wishlist: Set<String>,
    currency: Currency,
    onOpenDestination: (String) -> Unit,
    onDelete: (String) -> Unit,
    onToggleWishlist: (String) -> Unit,
    onBack: () -> Unit,
) {
    ScreenScaffold(
        title = "Saved",
        subtitle = "${trips.size} trip${if (trips.size == 1) "" else "s"} · " +
            "${wishlist.size} on the wishlist",
        onBack = onBack,
    ) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (trips.isEmpty() && wishlist.isEmpty()) {
                item {
                    GlassCard {
                        Text(
                            "Nothing saved yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Tap the heart on any destination, or save a full costed trip from " +
                                "its detail page.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (trips.isNotEmpty()) {
                item { SectionHeader("COSTED TRIPS") }
                items(trips, key = { it.id }) { trip ->
                    val dest = DestinationCatalog.find(trip.destinationId)
                    val origin = OriginCatalog.find(trip.originId)
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(CardShape)
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CardShape)
                            .clickable { dest?.let { onOpenDestination(it.id) } }
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (dest != null) {
                            Box(
                                Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(18.dp)),
                            ) {
                                DestinationArt(dest, Modifier.fillMaxSize())
                            }
                            Spacer(Modifier.width(14.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            Text(
                                dest?.city ?: trip.destinationId,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                "from ${origin.city} · ${trip.travelers} pax · ${trip.nights} nights",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                LocalDate.ofEpochDay(trip.departEpochDay).format(savedFormat),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                formatMoney(trip.totalUsd, currency),
                                style = MaterialTheme.typography.titleMedium,
                                color = if (trip.totalUsd <= trip.budgetUsd) {
                                    RangePalette.Aurora
                                } else {
                                    RangePalette.Sand
                                },
                            )
                            Spacer(Modifier.height(4.dp))
                            Icon(
                                Icons.Rounded.DeleteOutline,
                                "Delete",
                                tint = MaterialTheme.colorScheme.outline,
                                modifier = Modifier
                                    .size(18.dp)
                                    .clickable { onDelete(trip.id) },
                            )
                        }
                    }
                }
            }

            if (wishlist.isNotEmpty()) {
                item { Spacer(Modifier.height(8.dp)) }
                item { SectionHeader("WISHLIST") }
                item {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        wishlist.forEach { id ->
                            val dest = DestinationCatalog.find(id)
                            if (dest != null) {
                                RangeChip(
                                    label = dest.city,
                                    selected = true,
                                    onClick = { onOpenDestination(id) },
                                    icon = Icons.Rounded.FavoriteBorder,
                                    accent = RangePalette.Coral,
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

@Composable
fun OriginScreen(
    currentId: String,
    onPick: (String) -> Unit,
    onBack: () -> Unit,
) {
    var search by remember { mutableStateOf("") }
    val places = remember(search) {
        OriginCatalog.all.filter {
            search.isBlank() ||
                it.city.contains(search, true) ||
                it.country.contains(search, true) ||
                it.iata.contains(search, true)
        }
    }
    ScreenScaffold(
        title = "Where from?",
        subtitle = "${OriginCatalog.all.size} cities · every price is calculated from here",
        onBack = onBack,
    ) {
        OutlinedTextField(
            value = search,
            onValueChange = { search = it },
            placeholder = { Text("Search cities") },
            leadingIcon = { Icon(Icons.Rounded.Search, null) },
            singleLine = true,
            shape = PillShape,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(14.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OriginCatalog.byRegion.forEach { (region, cities) ->
                val visible = cities.filter { it in places }
                if (visible.isNotEmpty()) {
                    item(region.name) {
                        SectionHeader(
                            region.label.uppercase(),
                            Modifier.padding(top = 10.dp, bottom = 2.dp),
                        )
                    }
                    items(visible, key = { it.id }) { place ->
                        OriginRow(place, place.id == currentId) { onPick(place.id) }
                    }
                }
            }
            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

@Composable
private fun OriginRow(place: Place, selected: Boolean, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                if (selected) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLow
                },
            )
            .then(
                if (selected) {
                    Modifier.border(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                        RoundedCornerShape(20.dp),
                    )
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Rounded.LocationCity,
            null,
            tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(18.dp),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                place.city,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                place.country,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            place.iata,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.5.sp,
        )
    }
}

@Composable
fun SettingsScreen(
    settings: RangeSettings,
    currency: Currency,
    ratesUpdatedAt: Long,
    refreshing: Boolean,
    onRefreshRates: () -> Unit,
    onCurrency: (Currency) -> Unit,
    onThemeMode: (ThemeMode) -> Unit,
    onDynamicColor: (Boolean) -> Unit,
    onReduceMotion: (Boolean) -> Unit,
    onHaptics: (Boolean) -> Unit,
    onBack: () -> Unit,
) {
    ScreenScaffold(title = "Settings", subtitle = "Range v1.0", onBack = onBack) {
        LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                GlassCard {
                    SectionHeader("THEME")
                    Spacer(Modifier.height(12.dp))
                    val current = runCatching { ThemeMode.valueOf(settings.themeMode) }
                        .getOrDefault(ThemeMode.DARK)
                    ButtonGroup(
                        options = ThemeMode.entries,
                        selected = current,
                        onSelect = onThemeMode,
                        label = { it.label },
                        icon = {
                            when (it) {
                                ThemeMode.SYSTEM -> Icons.Rounded.PhoneAndroid
                                ThemeMode.LIGHT -> Icons.Rounded.LightMode
                                ThemeMode.DARK -> Icons.Rounded.DarkMode
                            }
                        },
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Range is designed midnight-first, but the light theme keeps the same " +
                            "aurora palette over paper.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item {
                GlassCard {
                    SectionHeader("CURRENCY")
                    Spacer(Modifier.height(12.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Currency.entries.forEach { c ->
                            RangeChip(
                                label = "${c.symbol.trim()} ${c.code}",
                                selected = c == currency,
                                onClick = { onCurrency(c) },
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                if (ratesUpdatedAt > 0) "Live exchange rates" else "Offline rates",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                if (ratesUpdatedAt > 0) {
                                    "Updated ${relativeTime(ratesUpdatedAt)}"
                                } else {
                                    "Using the rates shipped with the app"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (refreshing) {
                            ExpressiveLoader(Modifier.size(26.dp))
                        } else {
                            RangeChip(
                                label = "Refresh",
                                selected = false,
                                onClick = onRefreshRates,
                                icon = Icons.Rounded.Refresh,
                            )
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Range fetches exchange rates over the internet and falls back to the " +
                            "rates it ships with when you're offline. Trip totals themselves are " +
                            "modelled on device from distance, season, booking lead time and " +
                            "local cost of living — a planning estimate, not a live fare quote.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item {
                GlassCard {
                    SectionHeader("APPEARANCE")
                    Spacer(Modifier.height(6.dp))
                    SettingToggle(
                        "Use my wallpaper colours",
                        "Material You dynamic palette",
                        settings.dynamicColor,
                        onDynamicColor,
                    )
                    SettingToggle(
                        "Reduce motion",
                        "Calms the background and radar animations",
                        settings.reduceMotion,
                        onReduceMotion,
                    )
                    SettingToggle(
                        "Haptics",
                        "Ticks on the budget ruler and chips",
                        settings.hapticsEnabled,
                        onHaptics,
                    )
                }
            }
            item {
                GlassCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RangeMark(Modifier.size(48.dp))
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(
                                "RANGE",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.W800,
                                letterSpacing = 4.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                "com.vythera.range",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        "Range models the true cost of a trip — getting there, sleeping, eating, " +
                            "moving around and doing things — for ${DestinationCatalog.all.size} " +
                            "destinations, and shows you everything your budget actually reaches. " +
                            "All of it runs on-device.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item { Spacer(Modifier.height(40.dp)) }
        }
    }
}

@Composable
private fun SettingToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
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
        Switch(checked = checked, onCheckedChange = onChange)
    }
}
