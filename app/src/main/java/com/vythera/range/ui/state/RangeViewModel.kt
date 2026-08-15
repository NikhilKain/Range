@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
    androidx.compose.animation.ExperimentalSharedTransitionApi::class,
    androidx.compose.ui.ExperimentalComposeUiApi::class,
)

package com.vythera.range.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vythera.range.data.DestinationCatalog
import com.vythera.range.data.OriginCatalog
import com.vythera.range.data.RangeSettings
import com.vythera.range.data.LiveRates
import com.vythera.range.data.RangeStore
import com.vythera.range.data.live.FareQuote
import com.vythera.range.data.live.FareRepository
import com.vythera.range.data.live.FareRequest
import com.vythera.range.data.model.Region
import com.vythera.range.data.model.SavedTrip
import com.vythera.range.data.model.Tier
import com.vythera.range.data.model.TransportMode
import com.vythera.range.data.model.Vibe
import com.vythera.range.di.ServiceLocator
import com.vythera.range.domain.BudgetEngine
import com.vythera.range.domain.Currency
import com.vythera.range.domain.Rates
import com.vythera.range.domain.ReachSummary
import com.vythera.range.domain.TripEstimate
import com.vythera.range.domain.TripQuery
import com.vythera.range.ui.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.first
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

enum class Scope(val label: String) {
    ALL("Everywhere"),
    DOMESTIC("In my country"),
    INTERNATIONAL("Abroad"),
}

data class Filters(
    val regions: Set<Region> = emptySet(),
    val vibes: Set<Vibe> = emptySet(),
    val onlyInRange: Boolean = true,
    val visaEasyOnly: Boolean = false,
    val sort: SortMode = SortMode.BEST_VALUE,
    val scope: Scope = Scope.ALL,
    val search: String = "",
    val maxTravelHours: Float = 0f,
) {
    val activeCount: Int
        get() = regions.size + vibes.size + (if (visaEasyOnly) 1 else 0) +
            (if (maxTravelHours > 0f) 1 else 0) + (if (scope != Scope.ALL) 1 else 0)
}

data class ExploreState(
    val computing: Boolean = true,
    val all: List<TripEstimate> = emptyList(),
    val visible: List<TripEstimate> = emptyList(),
    val summary: ReachSummary = ReachSummary(
        total = 0,
        inRange = 0,
        countries = 0,
        farthestKm = 0.0,
        farthestCity = null,
        regions = 0,
        cheapest = null,
        bestValue = null,
    ),
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

    /**
     * Null until DataStore has actually answered. The nav graph waits for this
     * so a returning user never gets flashed the onboarding screen.
     */
    val settingsLoaded: StateFlow<RangeSettings?> = store.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val savedTrips: StateFlow<List<SavedTrip>> = store.savedTrips
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val wishlist: StateFlow<Set<String>> = store.wishlist
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    val currency: StateFlow<Currency> = store.settings
        .mapLatest { s -> Currency.entries.firstOrNull { it.code == s.currency } ?: Currency.INR }
        .stateIn(viewModelScope, SharingStarted.Eagerly, Currency.INR)

    private val fares = FareRepository(store)

    /** Real fares that have arrived, keyed by destination id. */
    private val _liveFares = MutableStateFlow<Map<String, FareQuote>>(emptyMap())
    val liveFares: StateFlow<Map<String, FareQuote>> = _liveFares.asStateFlow()

    /** Destination ids with a fetch in flight, so cards can show a spinner. */
    private val _fetchingFares = MutableStateFlow<Set<String>>(emptySet())
    val fetchingFares: StateFlow<Set<String>> = _fetchingFares.asStateFlow()

    /** False when this build carries no fare credentials — the UI hides the toggle. */
    val liveFaresAvailable: Boolean get() = fares.configured

    private val computed: StateFlow<ExploreState> = combine(
        _query.debounce(90),
        _liveFares,
    ) { q, quotes -> q to quotes }
        .mapLatest { (q, quotes) ->
            withContext(Dispatchers.Default) {
                val origin = OriginCatalog.find(q.originId)
                // Only ever *already known* fares here. This runs on every frame
                // of the budget ruler, so it stays pure — nothing in this block
                // may touch the network.
                val all = BudgetEngine.explore(origin, DestinationCatalog.all, q, quotes)
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

    private val _ratesUpdatedAt = MutableStateFlow(0L)
    val ratesUpdatedAt: StateFlow<Long> = _ratesUpdatedAt.asStateFlow()

    private val _refreshing = MutableStateFlow(false)
    val refreshing: StateFlow<Boolean> = _refreshing.asStateFlow()

    /**
     * Exchange rates are the one figure in Range that genuinely moves day to
     * day, so they come off the network when there is one, from cache when
     * there isn't, and from the shipped table on a fresh install offline.
     */
    fun refreshRates(force: Boolean = false) = viewModelScope.launch {
        if (_refreshing.value) return@launch
        val age = System.currentTimeMillis() - Rates.updatedAtMs
        if (!force && Rates.isLive && age < 6 * 60 * 60 * 1000L) return@launch
        _refreshing.value = true
        LiveRates.fetch()?.let { result ->
            Rates.apply(result.rates, result.fetchedAtMs)
            _ratesUpdatedAt.value = result.fetchedAtMs
            store.setRates(LiveRates.encode(result))
            // Re-price everything against the new rates.
            _query.value = _query.value.copy()
        }
        _refreshing.value = false
    }

    /**
     * Fetch real fares for the results the traveller is actually looking at.
     *
     * The hard rule here is that this never fans out across the catalogue. 180
     * destinations against a metered API would spend a month's quota on one
     * screen, so only the top [TOP_FARE_FETCHES] in-budget flight results get
     * a request. Everything else keeps the modelled price, which is a complete
     * answer on its own.
     */
    private suspend fun refreshTopFares(q: TripQuery) {
        if (!fares.configured || !settings.value.livePrices) return
        if (TransportMode.FLIGHT !in q.modes) return

        val origin = OriginCatalog.find(q.originId)
        val returnDate = q.departDate.plusDays(q.nights.toLong().coerceAtLeast(1))
        val wanted = explore.value.visible
            .asSequence()
            .filter { it.fits && it.mode == TransportMode.FLIGHT }
            .filter { it.destination.iata.isNotBlank() && origin.iata.isNotBlank() }
            .take(TOP_FARE_FETCHES)
            .toList()
        if (wanted.isEmpty()) return

        _fetchingFares.value = wanted.map { it.destination.id }.toSet()
        val fetched = coroutineScope {
            wanted.map { estimate ->
                async {
                    val quote = fares.quote(
                        FareRequest(
                            originIata = origin.iata,
                            destIata = estimate.destination.iata,
                            departDate = q.departDate,
                            returnDate = returnDate,
                            travelers = q.travelers,
                        ),
                    )
                    estimate.destination.id to quote
                }
            }.awaitAll()
        }
        _fetchingFares.value = emptySet()

        // One batched update rather than one per arrival: each change to this
        // map re-prices the whole catalogue, and eight sequential re-prices
        // would be visible as jank for no benefit.
        val arrived = fetched.mapNotNull { (id, quote) -> quote?.let { id to it } }
        if (arrived.isNotEmpty()) _liveFares.value = _liveFares.value + arrived
    }

    /**
     * Fetch one destination on demand — called when its detail screen opens.
     * A destination the traveller opened is worth a request even if it did not
     * make the top of the list.
     */
    fun requestLiveFare(destinationId: String) = viewModelScope.launch {
        if (!fares.configured || !settings.value.livePrices) return@launch
        if (_liveFares.value.containsKey(destinationId)) return@launch

        val q = _query.value
        if (TransportMode.FLIGHT !in q.modes) return@launch
        val origin = OriginCatalog.find(q.originId)
        val dest = DestinationCatalog.all.firstOrNull { it.id == destinationId } ?: return@launch
        if (origin.iata.isBlank() || dest.iata.isBlank()) return@launch

        _fetchingFares.value = _fetchingFares.value + destinationId
        val quote = fares.quote(
            FareRequest(
                originIata = origin.iata,
                destIata = dest.iata,
                departDate = q.departDate,
                returnDate = q.departDate.plusDays(q.nights.toLong().coerceAtLeast(1)),
                travelers = q.travelers,
            ),
        )
        _fetchingFares.value = _fetchingFares.value - destinationId
        if (quote != null) _liveFares.value = _liveFares.value + (destinationId to quote)
    }

    fun setLivePrices(on: Boolean) = viewModelScope.launch {
        store.setLivePrices(on)
        // Turning it off must actually revert the numbers on screen, not just
        // stop future fetches — otherwise the setting looks broken.
        if (!on) _liveFares.value = emptyMap()
    }

    init {
        viewModelScope.launch {
            LiveRates.decode(store.cachedRates.first())?.let {
                Rates.apply(it.rates, it.fetchedAtMs)
                _ratesUpdatedAt.value = it.fetchedAtMs
            }
            refreshRates()
        }
        viewModelScope.launch {
            fares.restore(store.cachedFares.first())
        }
        viewModelScope.launch {
            // A much longer debounce than the 90 ms used for pricing: dragging
            // the budget ruler must never cost a network request, so fares are
            // only chased once the traveller has actually settled on something.
            _query.debounce(1_500).collectLatest { q -> refreshTopFares(q) }
        }
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
                when (f.scope) {
                    Scope.ALL -> true
                    Scope.DOMESTIC -> e.domestic
                    Scope.INTERNATIONAL -> !e.domestic
                } &&
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

    fun clearFilters() = setFilters {
        Filters(sort = it.sort, onlyInRange = it.onlyInRange, scope = it.scope)
    }

    // ---- persistence ------------------------------------------------------

    fun setOrigin(id: String) {
        update { it.copy(originId = id) }
        viewModelScope.launch { store.setOrigin(id) }
    }

    /** Onboarding only: match the currency to where the traveller lives. */
    fun setOriginWithCurrency(id: String) {
        setOrigin(id)
        val place = OriginCatalog.find(id)
        Currency.entries.firstOrNull { it.code == place.currency }?.let { setCurrency(it) }
    }

    fun setCurrency(c: Currency) = viewModelScope.launch { store.setCurrency(c.code) }
    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { store.setThemeMode(mode.name) }
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
        return BudgetEngine.estimate(
            OriginCatalog.find(q.originId),
            dest,
            q,
            // Without this the detail screen would price off the model while
            // the card that opened it showed a live total — same trip, two
            // different numbers.
            _liveFares.value[destinationId],
        )
    }

    companion object {
        /**
         * How many results get a real fare request. Small on purpose: the free
         * API tiers are metered per month, and nobody reads past the first
         * screen of results before changing something anyway.
         */
        private const val TOP_FARE_FETCHES = 8

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer { RangeViewModel(ServiceLocator.store) }
        }
    }
}
