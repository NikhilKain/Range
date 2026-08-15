package com.vythera.range.data.live

import android.util.Log
import com.vythera.range.BuildConfig
import org.json.JSONObject

/**
 * Live fares from Travelpayouts (Aviasales).
 *
 * Sits behind Amadeus as the fallback, and earns its place for two reasons:
 * the free tier is far less tightly metered, and its India coverage — the
 * default origin set for Range — is better than most aggregators. It is also
 * an affiliate programme, so [FareQuote.deepLink] carries a marker and a
 * booking made through it pays back.
 *
 * The prices are cached search results rather than a live GDS query, so they
 * can lag reality by a few hours. That is still enormously better than a
 * model, and it is why this provider is second rather than first.
 */
internal class TravelpayoutsProvider(
    private val token: String = BuildConfig.TRAVELPAYOUTS_TOKEN,
    private val marker: String = BuildConfig.TRAVELPAYOUTS_MARKER,
) : FareProvider {

    override val source = FareSource.TRAVELPAYOUTS

    override val configured: Boolean
        get() = token.isNotBlank()

    @Volatile
    private var backoffUntilMs = 0L

    override suspend fun quote(request: FareRequest): FareQuote? {
        if (!configured) return null
        if (System.currentTimeMillis() < backoffUntilMs) return null

        val url = buildString {
            append("https://api.travelpayouts.com/aviasales/v3/prices_for_dates")
            append("?origin=").append(request.originIata)
            append("&destination=").append(request.destIata)
            append("&departure_at=").append(request.departDate)
            request.returnDate?.let { append("&return_at=").append(it) }
            append("&one_way=").append(request.returnDate == null)
            append("&currency=usd")
            append("&sorting=price")
            append("&limit=1")
            append("&token=").append(Http.encode(token))
        }

        val result = Http.get(url)
        if (result.rateLimited) {
            backoffUntilMs = System.currentTimeMillis() + BACKOFF_MS
            return null
        }
        val body = result.bodyOrNull ?: return null

        return runCatching {
            val json = JSONObject(body)
            if (!json.optBoolean("success", true)) return null
            val data = json.optJSONArray("data")
            if (data == null || data.length() == 0) return null

            val cheapest = data.getJSONObject(0)
            val perPerson = cheapest.optDouble("price", -1.0)
            if (perPerson <= 0) return null

            // Travelpayouts quotes ONE seat. Amadeus quotes the whole party.
            // Range's basis is the whole party, so this multiplies and Amadeus
            // does not — the single most breakable line in this file, and the
            // one that fails silently by pricing a family trip as a solo one.
            val partyTotal = perPerson * request.travelers.coerceAtLeast(1)

            FareQuote(
                originIata = request.originIata,
                destIata = request.destIata,
                totalUsd = partyTotal,
                source = source,
                fetchedAtMs = System.currentTimeMillis(),
                deepLink = cheapest.optString("link").takeIf { it.isNotBlank() }?.let(::bookingUrl),
            )
        }.onFailure { Log.i(TAG, "unparseable response: ${it.message}") }.getOrNull()
    }

    /** Relative paths come back from the API; the marker is what earns the commission. */
    private fun bookingUrl(path: String): String {
        val base = if (path.startsWith("http")) path else "https://www.aviasales.com$path"
        if (marker.isBlank()) return base
        return base + (if ("?" in base) "&" else "?") + "marker=" + Http.encode(marker)
    }

    private companion object {
        const val TAG = "Travelpayouts"
        const val BACKOFF_MS = 15 * 60 * 1000L
    }
}
