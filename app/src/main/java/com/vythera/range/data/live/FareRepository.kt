package com.vythera.range.data.live

import android.util.Log
import com.vythera.range.data.RangeStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.concurrent.ConcurrentHashMap

/**
 * The one place the rest of the app asks for a live fare.
 *
 * Three jobs, in order of how much they matter:
 *
 * 1. **Spend the quota carefully.** The Amadeus free tier is roughly 66 calls
 *    a day. Re-fetching a route because the user scrolled back up would empty
 *    a month in an afternoon, so answers are cached on disk for [TTL_MS] and
 *    "no fare exists on this route" is cached too — a negative result is an
 *    answer, and re-asking it every time is the most wasteful thing this class
 *    could do.
 * 2. **Never make the app worse.** Every failure path returns null, and null
 *    means the caller quietly keeps the modelled price. Offline, no
 *    credentials, spent quota and unflown route are deliberately
 *    indistinguishable to the caller.
 * 3. **Never fetch the same thing twice at once.** Opening a destination while
 *    its card is still resolving used to be two identical requests; concurrent
 *    callers now share one.
 */
class FareRepository internal constructor(
    /**
     * Where the cache is written. A lambda rather than the store itself so the
     * caching rules — TTL, negative entries, request de-duplication — can be
     * tested without an Android Context. They are the part of this class most
     * likely to be wrong, so they are the part that must be reachable.
     */
    private val persistCache: suspend (String) -> Unit,
    private val providers: List<FareProvider>,
    private val ttlMs: Long = TTL_MS,
    private val missTtlMs: Long = MISS_TTL_MS,
    private val now: () -> Long = System::currentTimeMillis,
) {

    constructor(store: RangeStore) : this(
        persistCache = store::setFares,
        // Amadeus first: real GDS inventory. Travelpayouts second: looser
        // limits and better India coverage, so it catches what Amadeus's test
        // environment does not carry.
        providers = listOf(AmadeusProvider(), TravelpayoutsProvider()),
    )

    @Serializable
    private data class Entry(
        val totalUsd: Double,
        val source: String,
        val fetchedAtMs: Long,
        val deepLink: String? = null,
        /** False means "asked, and there is genuinely no fare here". */
        val found: Boolean = true,
    )

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val cache = ConcurrentHashMap<String, Entry>()
    private val inFlight = ConcurrentHashMap<String, CompletableDeferred<FareQuote?>>()
    private val writeLock = Mutex()

    @Volatile
    private var restored = false

    /** False when this build ships no credentials at all — the UI hides live affordances. */
    val configured: Boolean = providers.any { it.configured }

    /** Load the persisted cache. Safe to call repeatedly; only the first does work. */
    suspend fun restore(encoded: String?) {
        if (restored) return
        restored = true
        if (encoded.isNullOrBlank()) return
        runCatching {
            json.decodeFromString<Map<String, Entry>>(encoded)
                .filterValues { !it.isStale() }
                .forEach { (k, v) -> cache[k] = v }
        }.onFailure {
            // A cache that will not parse is a cache worth throwing away.
            Log.i(TAG, "dropping unreadable fare cache: ${it.message}")
        }
    }

    /**
     * A cached answer if one is fresh, without touching the network.
     *
     * Synchronous on purpose: the results list needs to paint live prices it
     * already knows on the first frame, not one recomposition later.
     */
    fun cached(request: FareRequest): FareQuote? =
        cache[request.key]?.takeIf { !it.isStale() && it.found }?.toQuote(request)

    /**
     * Fetch a fare, or return the cached one. Null means "use the model".
     */
    suspend fun quote(request: FareRequest): FareQuote? {
        if (!configured) return null

        cache[request.key]?.let { entry ->
            if (!entry.isStale()) return if (entry.found) entry.toQuote(request) else null
        }

        // Join an identical request already in progress rather than starting
        // a second one. putIfAbsent is the atomic half of this; the loser of
        // the race awaits the winner's result.
        val pending = CompletableDeferred<FareQuote?>()
        inFlight.putIfAbsent(request.key, pending)?.let { return it.await() }

        val quote = try {
            providers.asSequence()
                .filter { it.configured }
                .fold(null as FareQuote?) { found, provider ->
                    found ?: provider.quote(request)
                }
        } catch (e: Exception) {
            // A provider that throws is a bug in the provider, not a reason to
            // take the screen down. Log it, price it from the model.
            Log.w(TAG, "provider threw for ${request.key}", e)
            null
        } finally {
            inFlight.remove(request.key)
        }

        // Stamped with *this* clock, not the provider's. Freshness is a property
        // of the cache, and trusting an upstream timestamp means a provider that
        // reports when its own snapshot was taken — Travelpayouts serves cached
        // searches — marks the entry stale the moment it arrives, silently
        // disabling caching and spending the quota on every repeat request.
        cache[request.key] = quote?.let {
            Entry(it.totalUsd, it.source.name, now(), it.deepLink, found = true)
        } ?: Entry(0.0, "", now(), found = false)

        persist()
        pending.complete(quote)
        return quote
    }

    private suspend fun persist() = writeLock.withLock {
        runCatching {
            val live = cache.filterValues { !it.isStale() }
            // Unbounded growth would eventually make every DataStore write
            // expensive, and the oldest entries are the least useful.
            val trimmed = if (live.size <= MAX_ENTRIES) live else {
                live.entries.sortedByDescending { it.value.fetchedAtMs }
                    .take(MAX_ENTRIES)
                    .associate { it.key to it.value }
            }
            persistCache(json.encodeToString(trimmed))
        }.onFailure { Log.i(TAG, "fare cache not persisted: ${it.message}") }
    }

    private fun Entry.isStale(): Boolean =
        now() - fetchedAtMs > (if (found) ttlMs else missTtlMs)

    private fun Entry.toQuote(request: FareRequest) = FareQuote(
        originIata = request.originIata,
        destIata = request.destIata,
        totalUsd = totalUsd,
        source = FareSource.entries.firstOrNull { it.name == source } ?: FareSource.AMADEUS,
        fetchedAtMs = fetchedAtMs,
        deepLink = deepLink,
    )

    internal companion object {
        const val TAG = "FareRepository"

        /**
         * Six hours. Fares do drift within a day, but not enough to change
         * which destinations fit a budget, and a shorter window would not
         * survive the free tier.
         */
        const val TTL_MS = 6 * 60 * 60 * 1000L

        /** Routes with no fares stay unflown for longer than fares stay fresh. */
        const val MISS_TTL_MS = 24 * 60 * 60 * 1000L

        const val MAX_ENTRIES = 400
    }
}
