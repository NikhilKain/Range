package com.vythera.range.data.live

import android.util.Log
import com.vythera.range.BuildConfig
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject

/**
 * Live fares from Amadeus Self-Service.
 *
 * Amadeus sits on real GDS inventory, so its numbers are the closest thing to
 * what a booking site would show. The free tier is metered per month, which
 * shapes the whole design here: nothing speculative is ever fetched, a spent
 * quota backs off rather than hammering, and every failure degrades to the
 * modelled price instead of surfacing an error.
 *
 * Auth is OAuth2 client-credentials. Tokens last ~30 minutes; re-requesting one
 * per fare would double the request count against a quota that is already the
 * binding constraint, so the token is cached and refreshed slightly early.
 */
internal class AmadeusProvider(
    private val clientId: String = BuildConfig.AMADEUS_CLIENT_ID,
    private val clientSecret: String = BuildConfig.AMADEUS_CLIENT_SECRET,
    private val host: String = BuildConfig.AMADEUS_HOST,
) : FareProvider {

    override val source = FareSource.AMADEUS

    override val configured: Boolean
        get() = clientId.isNotBlank() && clientSecret.isNotBlank()

    private val tokenLock = Mutex()
    private var token: String? = null
    private var tokenExpiresAtMs = 0L

    /**
     * Set briefly after a failed token exchange.
     *
     * Without it, a batch of eight destination fetches queues eight separate
     * auth attempts behind [tokenLock] — each one waiting for the previous to
     * time out. Observed on a device with no DNS: the whole batch spent its
     * time retrying an exchange that could not succeed. One failure is enough
     * to know the next few seconds are not worth trying.
     */
    private var tokenRetryAfterMs = 0L

    /**
     * Set when the API says we are out of quota or rate limited. Until it
     * passes, this provider reports nothing and costs nothing — without it a
     * spent monthly quota would mean one wasted round-trip per card scrolled.
     */
    @Volatile
    private var backoffUntilMs = 0L

    override suspend fun quote(request: FareRequest): FareQuote? {
        if (!configured) return null
        if (System.currentTimeMillis() < backoffUntilMs) return null

        val bearer = token() ?: return null

        val url = buildString {
            append("https://").append(host).append("/v2/shopping/flight-offers")
            append("?originLocationCode=").append(request.originIata)
            append("&destinationLocationCode=").append(request.destIata)
            append("&departureDate=").append(request.departDate)
            request.returnDate?.let { append("&returnDate=").append(it) }
            append("&adults=").append(request.travelers.coerceIn(1, 9))
            // Priced in USD at source so no FX hop can drift the figure away
            // from the modelled line it is replacing.
            append("&currencyCode=USD")
            // Range shows one number, so one offer is all that is needed, and
            // a smaller response is a faster one on a phone.
            append("&max=1")
        }

        val result = Http.get(url, mapOf("Authorization" to "Bearer $bearer"))
        if (result.rateLimited) {
            backOff()
            return null
        }
        val body = result.bodyOrNull ?: return null

        return runCatching {
            val offers = JSONObject(body).optJSONArray("data")
            // An empty array is the normal answer for a route nobody flies,
            // not a failure. Falling through to the model is correct.
            if (offers == null || offers.length() == 0) return null

            val price = offers.getJSONObject(0).getJSONObject("price")
            // grandTotal covers every traveller in `adults` and includes taxes,
            // so it is already Range's basis — no multiplication here. That is
            // the opposite of Travelpayouts, hence the explicit note.
            val total = price.optString("grandTotal")
                .ifBlank { price.optString("total") }
                .toDoubleOrNull() ?: return null

            if (total <= 0) return null

            FareQuote(
                originIata = request.originIata,
                destIata = request.destIata,
                totalUsd = total,
                source = source,
                fetchedAtMs = System.currentTimeMillis(),
            )
        }.onFailure { Log.i(TAG, "unparseable offer: ${it.message}") }.getOrNull()
    }

    private suspend fun token(): String? = tokenLock.withLock {
        val now = System.currentTimeMillis()
        token?.let { if (now < tokenExpiresAtMs) return@withLock it }
        if (now < tokenRetryAfterMs) return@withLock null

        val result = Http.postForm(
            url = "https://$host/v1/security/oauth2/token",
            form = mapOf(
                "grant_type" to "client_credentials",
                "client_id" to clientId,
                "client_secret" to clientSecret,
            ),
        )
        if (result.rateLimited) backOff()
        val body = result.bodyOrNull ?: run {
            tokenRetryAfterMs = now + TOKEN_RETRY_MS
            return@withLock null
        }

        runCatching {
            val json = JSONObject(body)
            val fresh = json.getString("access_token")
            // Refresh a minute early — a token that expires mid-flight costs a
            // retry, and the clock on a phone is not the clock on their server.
            val ttlSeconds = json.optLong("expires_in", 1_800L)
            token = fresh
            tokenExpiresAtMs = now + (ttlSeconds - 60).coerceAtLeast(60) * 1_000
            fresh
        }.onFailure { Log.i(TAG, "token exchange failed: ${it.message}") }.getOrNull()
    }

    private fun backOff() {
        backoffUntilMs = System.currentTimeMillis() + BACKOFF_MS
        Log.i(TAG, "rate limited or out of quota — pausing live fares for 15 min")
    }

    private companion object {
        const val TAG = "Amadeus"
        const val BACKOFF_MS = 15 * 60 * 1000L

        /**
         * Short enough that stepping back into signal recovers on the next
         * query, long enough that one batch cannot retry auth eight times.
         */
        const val TOKEN_RETRY_MS = 30 * 1000L
    }
}
