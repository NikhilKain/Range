package com.vythera.range.data

import android.util.Log
import com.vythera.range.domain.Currency
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Live exchange rates.
 *
 * Range prices everything in USD internally, so a fresh USD→local rate is the
 * one piece of live data that changes every figure in the app. The endpoint is
 * keyless and free; if the phone is offline, or anything at all goes wrong, the
 * shipped rates stand in and the app carries on working exactly as before.
 */
object LiveRates {

    private const val TAG = "LiveRates"
    private const val ENDPOINT = "https://api.frankfurter.app/latest?from=USD&to="
    private const val TIMEOUT_MS = 6_000

    data class Result(val rates: Map<String, Double>, val fetchedAtMs: Long)

    suspend fun fetch(): Result? = withContext(Dispatchers.IO) {
        withTimeoutOrNull(TIMEOUT_MS.toLong() + 2_000) {
            runCatching {
                val symbols = Currency.entries
                    .filter { it != Currency.USD }
                    .joinToString(",") { it.code }
                val connection = (URL(ENDPOINT + symbols).openConnection() as HttpURLConnection)
                    .apply {
                        connectTimeout = TIMEOUT_MS
                        readTimeout = TIMEOUT_MS
                        requestMethod = "GET"
                        setRequestProperty("Accept", "application/json")
                    }
                val body = connection.use { it.inputStream.bufferedReader().readText() }
                val rates = JSONObject(body).getJSONObject("rates")
                val parsed = buildMap {
                    put("USD", 1.0)
                    rates.keys().forEach { key -> put(key, rates.getDouble(key)) }
                }
                Result(parsed, System.currentTimeMillis())
            }.onFailure { Log.i(TAG, "rate refresh skipped: ${it.message}") }.getOrNull()
        }
    }

    private inline fun <T> HttpURLConnection.use(block: (HttpURLConnection) -> T): T = try {
        block(this)
    } finally {
        disconnect()
    }

    fun encode(result: Result): String =
        result.fetchedAtMs.toString() + "|" +
            result.rates.entries.joinToString(",") { "${it.key}:${it.value}" }

    fun decode(raw: String?): Result? {
        if (raw.isNullOrBlank()) return null
        return runCatching {
            val (stamp, body) = raw.split("|", limit = 2)
            val rates = body.split(",").mapNotNull {
                val parts = it.split(":")
                if (parts.size == 2) parts[0] to (parts[1].toDoubleOrNull() ?: return@mapNotNull null)
                else null
            }.toMap()
            if (rates.isEmpty()) null else Result(rates, stamp.toLong())
        }.getOrNull()
    }
}
