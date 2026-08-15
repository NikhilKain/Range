package com.vythera.range.data.live

import java.time.LocalDate

/** Where a live price came from. Shown to the user, so these are brand names. */
enum class FareSource(val label: String) {
    AMADEUS("Amadeus"),
    TRAVELPAYOUTS("Aviasales"),
}

/**
 * One route, one set of dates, one party.
 *
 * [returnDate] being null means a one-way search, which Range never issues
 * today — every modelled trip is a return — but the providers support it and
 * leaving the door open costs nothing.
 */
data class FareRequest(
    val originIata: String,
    val destIata: String,
    val departDate: LocalDate,
    val returnDate: LocalDate?,
    val travelers: Int,
) {
    /** Stable cache identity. Deliberately excludes anything not sent upstream. */
    val key: String
        get() = "$originIata-$destIata-$departDate-${returnDate ?: "ow"}-$travelers"
}

/**
 * A real fare, normalised to the basis Range prices in.
 *
 * **[totalUsd] is round trip, for the whole party, in USD** — the same basis as
 * `TransportOption.costUsd`, which multiplies its per-person figure by
 * `query.travelers`. Every provider has to convert into this before returning,
 * because the two APIs disagree: Amadeus `grandTotal` is already the whole
 * party, Travelpayouts quotes one seat. Getting this wrong does not throw, it
 * just prices a family holiday like a solo trip, so each provider states its
 * conversion explicitly at the call site.
 */
data class FareQuote(
    val originIata: String,
    val destIata: String,
    val totalUsd: Double,
    val source: FareSource,
    val fetchedAtMs: Long,
    /** Where to actually book it. Carries the affiliate marker where there is one. */
    val deepLink: String? = null,
) {
    fun ageMs(now: Long = System.currentTimeMillis()): Long = now - fetchedAtMs
}

/**
 * A source of live fares.
 *
 * Implementations must never throw and must never block indefinitely: a null
 * return means "no answer, use the model", which is a normal outcome rather
 * than an error. Offline, out of quota, unconfigured and route-not-found all
 * collapse to the same null on purpose — the caller's behaviour is identical
 * in every case.
 */
internal interface FareProvider {
    val source: FareSource

    /** False when the build carries no credentials for this provider. */
    val configured: Boolean

    suspend fun quote(request: FareRequest): FareQuote?
}
