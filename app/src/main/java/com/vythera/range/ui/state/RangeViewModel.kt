package com.vythera.range.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vythera.range.data.DestinationCatalog
import com.vythera.range.data.OriginCatalog
import com.vythera.range.data.RangeSettings
import com.vythera.range.data.RangeStore
import com.vythera.range.data.model.Region
import com.vythera.range.data.model.SavedTrip
import com.vythera.range.data.model.Tier
import com.vythera.range.data.model.TransportMode
import com.vythera.range.data.model.Vibe
import com.vythera.range.di.ServiceLocator
import com.vythera.range.domain.BudgetEngine
import com.vythera.range.domain.Currency
import com.vythera.range.domain.ReachSummary
import com.vythera.range.domain.TripEstimate
import com.vythera.range.domain.TripQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.UUID

enum class SortMode(val label: String) {
    BEST_VALUE("Best value"),
    CHEAPEST("Cheapest"),
    FARTHEST("Farthest reach"),
    NEAREST("Closest"),
    POPULAR("Most loved"),
    LONGEST("Longest stay"),
}

data class Filters(
    val regions: Set<Region> = emptySet(),
    val vibes: Set<Vibe> = emptySet(),
    val onlyInRange: Boolean = true,
    val visaEasyOnly: Boolean = false,
    val sort: SortMode = SortMode.BEST_VALUE,
    val search: String = "",
    val maxTravelHours: Float = 0f,
) {
    val activeCount: Int
        get() = regions.size + vibes.size + (if (visaEasyOnly) 1 else 0) +
            (if (maxTravelHours > 0f) 1 else 0)
}

data class ExploreState(
    val computing: Boolean = true,
    val all: List<TripEstimate> = emptyList(),
    val visible: List<TripEstimate> = emptyList(),
    val summary: ReachSummary = ReachSummary(0, 0, 0, 0.0, null, 0, null, null),
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class RangeViewModel(private val store: RangeStore) : ViewModel() {

    private val _query = MutableStateFlow(
        TripQuery(
            originId = "del",
            departDate = LocalDate.now().plusDays(45),
            nights = 5,
            travelers = 2,
            modes = setOf(TransportMode.FLIGHT),
            budgetUsd = 690.0,
        ),
    )
    val query: StateFlow<TripQuery> = _query.asStateFlow()

    private val _filters = MutableStateFlow(Filters())
    val filters: StateFlow<Filters> = _filters.asStateFlow()

    /** Set when the user commits a search, so Explore can play its reveal once. */
    private val _searchToken = MutableStateFlow(0)
    val searchToken: StateFlow<Int> = _searchToken.asStateFlow()

    val settings: StateFlow<RangeSettings> = store.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, RangeSettings())

    val savedTrips: StateFlow<List<SavedTrip>> = store.savedTrips
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val wishlist: StateFlow<Set<String>> = store.wishlist
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val currency: StateFlow<Currency> = store.settings
        .mapLatest { s -> Currency.entries.firstOrNull { it.code == s.currency } ?: Currency.INR }
        .stateIn(viewModelScope, SharingStarted.Eagerly, Currency.INR)

    private val computed: StateFlow<ExploreState> = _query
        .debounce(90)
        .mapLatest { q ->
            withContext(Dispatchers.Default) {
                val origin = OriginCatalog.find(q.originId)
                val all = BudgetEngine.explore(origin, DestinationCatalog.all, q)
                ExploreState(
                    computing = false,
                    all = all,
                    visible = all,
                    summary = BudgetEngine.reachSummary(all),
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.Eagerly, ExploreState())

    val explore: StateFlow<ExploreState> = combine(computed, _filters) { state, f ->
        state.copy(visible = applyFilters(state.all, f))
    }.stateIn(viewModelScope, SharingStarted.Eagerly, ExploreState())

    init {
        viewModelScope.launch {
            val first = store.settings
            first.collect { s ->
                if (_query.value.originId != s.originId) {
                    _query.value = _query.value.copy(originId = s.originId)
                }
            }
        }
    }

    private fun applyFilters(all: List<TripEstimate>, f: Filters): List<TripEstimate> {
        val term = f.search.trim().lowercase()
        val filtered = all.filter { e ->
            val d = e.destination
            (!f.onlyInRange || e.fits) &&
                (f.regions.isEmpty() || d.region in f.regions) &&
                (f.vibes.isEmpty() || d.vibes.any { it in f.vibes }) &&
                (!f.visaEasyOnly || d.visaKind.ordinal <= 2) &&
                (f.maxTravelHours <= 0f || travelHours(e) <= f.maxTravelHours) &&
                (term.isEmpty() || d.city.lowercase().contains(term) ||
                    d.country.lowercase().contains(term) ||
                    d.vibes.any { it.label.lowercase().contains(term) })
        }
        return when (f.sort) {
            SortMode.BEST_VALUE -> filtered.sortedByDescending { BudgetEngine.valueScore(it) }
            SortMode.CHEAPEST -> filtered.sortedBy { it.totalUsd }
            SortMode.FARTHEST -> filtered.sortedByDescending { it.distanceKm }
            SortMode.NEAREST -> filtered.sortedBy { it.distanceKm }
            SortMode.POPULAR -> filtered.sortedByDescending { it.destination.popularity }
            SortMode.LONGEST -> filtered.sortedByDescending { it.maxNights }
        }
    }

    private fun travelHours(e: TripEstimate): Double =
        e.transportOptions.firstOrNull { it.mode == e.mode }?.hoursOneWay ?: 0.0

    // ---- query mutations --------------------------------------------------

    fun update(transform: (TripQuery) -> TripQuery) {
        _query.value = transform(_query.value)
    }

    fun setBudgetUsd(v: Double) = update { it.copy(budgetUsd = v.coerceAtLeast(20.0)) }
    fun setTravelers(n: Int) = update { it.copy(travelers = n.coerceIn(1, 12)) }
    fun setNights(n: Int) = update { it.copy(nights = n.coerceIn(0, 30)) }
    fun setDate(d: LocalDate) = update { it.copy(departDate = d) }
    fun setStay(t: Tier) = update { it.copy(stay = t) }
    fun setFood(t: Tier) = update { it.copy(food = t) }
    fun setExperience(t: Tier) = update { it.copy(experience = t) }
    fun setPerPerson(on: Boolean) = update { it.copy(budgetIsPerPerson = on) }
    fun setRoomSharing(n: Int) = update { it.copy(peoplePerRoom = n.coerceIn(1, 4)) }
    fun setBuffer(pct: Int) = update { it.copy(bufferPercent = pct.coerceIn(0, 25)) }
    fun setInsurance(on: Boolean) = update { it.copy(includeInsurance = on) }
    fun setVisa(on: Boolean) = update { it.copy(includeVisa = on) }

    fun toggleMode(mode: TransportMode) = update { q ->
        val next = q.modes.toMutableSet()
        if (!next.add(mode)) next.remove(mode)
        if (next.isEmpty()) next.add(TransportMode.NONE)
        // "Already there" is exclusive — it means no travel cost at all.
        val cleaned = if (mode == TransportMode.NONE && TransportMode.NONE in next) {
            setOf(TransportMode.NONE)
        } else {
            next - TransportMode.NONE
        }
        q.copy(modes = cleaned.ifEmpty { setOf(TransportMode.FLIGHT) })
    }

    fun commitSearch() {
        _searchToken.value = _searchToken.value + 1
    }

    // ---- filters ----------------------------------------------------------

    fun setFilters(transform: (Filters) -> Filters) {
        _filters.value = transform(_filters.value)
    }

    fun toggleRegion(r: Region) = setFilters { f ->
        f.copy(regions = f.regions.toMutableSet().also { if (!it.add(r)) it.remove(r) })
    }

    fun toggleVibe(v: Vibe) = setFilters { f ->
        f.copy(vibes = f.vibes.toMutableSet().also { if (!it.add(v)) it.remove(v) })
    }

    fun clearFilters() = setFilters { Filters(sort = it.sort, onlyInRange = it.onlyInRange) }

    // ---- persistence ------------------------------------------------------

    fun setOrigin(id: String) {
        update { it.copy(originId = id) }
        viewModelScope.launch { store.setOrigin(id) }
    }

    fun setCurrency(c: Currency) = viewModelScope.launch { store.setCurrency(c.code) }
    fun setDynamicColor(on: Boolean) = viewModelScope.launch { store.setDynamicColor(on) }
    fun setReduceMotion(on: Boolean) = viewModelScope.launch { store.setReduceMotion(on) }
    fun setHaptics(on: Boolean) = viewModelScope.launch { store.setHaptics(on) }
    fun setOnboarded(done: Boolean) = viewModelScope.launch { store.setOnboarded(done) }
    fun toggleWishlist(id: String) = viewModelScope.launch { store.toggleWishlist(id) }

    fun saveTrip(estimate: TripEstimate, note: String = "") = viewModelScope.launch {
        store.saveTrip(
            SavedTrip(
                id = UUID.randomUUID().toString(),
                destinationId = estimate.destination.id,
                originId = estimate.origin.id,
                departEpochDay = estimate.query.departDate.toEpochDay(),
                nights = estimate.query.nights,
                travelers = estimate.query.travelers,
                transport = estimate.mode.name,
                stayTier = estimate.query.stay.name,
                foodTier = estimate.query.food.name,
                totalUsd = estimate.totalUsd,
                budgetUsd = estimate.budgetUsd,
                savedAtEpochMs = System.currentTimeMillis(),
                note = note,
            ),
        )
    }

    fun deleteTrip(id: String) = viewModelScope.launch { store.deleteTrip(id) }

    fun estimateFor(destinationId: String): TripEstimate? {
        val q = _query.value
        val dest = DestinationCatalog.find(destinationId) ?: return null
        return BudgetEngine.estimate(OriginCatalog.find(q.originId), dest, q)
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { RangeViewModel(ServiceLocator.store) }
        }
    }
}
