package com.vythera.range.data.live

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * The smallest HTTP client that does the job.
 *
 * Range has one dependency-light rule and no OkHttp/Retrofit, so this wraps
 * [HttpURLConnection] with the three things the fare providers actually need
 * that [com.vythera.range.data.LiveRates] does not: the status code (a 429 has
 * to be distinguishable from a network drop, because one means back off and
 * the other means retry), form POSTs for OAuth token exchange, and a hard
 * timeout ceiling so a hung socket can never wedge a coroutine.
 */
internal object Http {

    private const val TAG = "RangeHttp"
    private const val TIMEOUT_MS = 8_000
    private const val MAX_BODY_BYTES = 512 * 1024

    sealed interface Result {
        data class Ok(val body: String) : Result

        /**
         * [code] is the HTTP status, or 0 when the request never got that far
         * (no connectivity, DNS failure, timeout).
         */
        data class Failed(val code: Int, val reason: String) : Result

        val bodyOrNull: String? get() = (this as? Ok)?.body

        /** Quota or rate limit — the caller should stop asking, not retry. */
        val rateLimited: Boolean get() = this is Failed && (code == 429 || code == 403)
    }

    suspend fun get(url: String, headers: Map<String, String> = emptyMap()): Result =
        request(url, method = "GET", headers = headers, body = null)

    /** `application/x-www-form-urlencoded` POST, for OAuth token endpoints. */
    suspend fun postForm(
        url: String,
        form: Map<String, String>,
        headers: Map<String, String> = emptyMap(),
    ): Result = request(
        url = url,
        method = "POST",
        headers = headers + ("Content-Type" to "application/x-www-form-urlencoded"),
        body = form.entries.joinToString("&") { "${encode(it.key)}=${encode(it.value)}" },
    )

    fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

    private suspend fun request(
        url: String,
        method: String,
        headers: Map<String, String>,
        body: String?,
    ): Result = withContext(Dispatchers.IO) {
        // The connection's own timeouts govern the socket, but a server that
        // trickles bytes forever satisfies both. This is the backstop.
        withTimeoutOrNull(TIMEOUT_MS.toLong() * 2 + 2_000) {
            var connection: HttpURLConnection? = null
            try {
                connection = (URL(url).openConnection() as HttpURLConnection).apply {
                    requestMethod = method
                    connectTimeout = TIMEOUT_MS
                    readTimeout = TIMEOUT_MS
                    instanceFollowRedirects = true
                    setRequestProperty("Accept", "application/json")
                    setRequestProperty("Accept-Encoding", "identity")
                    headers.forEach { (k, v) -> setRequestProperty(k, v) }
                    if (body != null) {
                        doOutput = true
                        outputStream.use { it.write(body.toByteArray()) }
                    }
                }
                val code = connection.responseCode
                if (code in 200..299) {
                    Result.Ok(connection.inputStream.readCapped())
                } else {
                    // Error bodies carry the useful part of an API failure —
                    // which credential is wrong, which quota is spent.
                    val detail = connection.errorStream?.readCapped()?.take(300).orEmpty()
                    Log.i(TAG, "$method ${url.redacted()} -> $code $detail")
                    Result.Failed(code, detail)
                }
            } catch (e: IOException) {
                Log.i(TAG, "$method ${url.redacted()} failed: ${e.message}")
                Result.Failed(0, e.message ?: "network unavailable")
            } finally {
                connection?.disconnect()
            }
        } ?: Result.Failed(0, "timed out")
    }

    private fun java.io.InputStream.readCapped(): String =
        bufferedReader().use { reader ->
            val buffer = CharArray(8 * 1024)
            val out = StringBuilder()
            while (out.length < MAX_BODY_BYTES) {
                val read = reader.read(buffer)
                if (read <= 0) break
                out.appendRange(buffer, 0, read)
            }
            out.toString()
        }

    /**
     * Travelpayouts passes its token in the query string, so a raw URL in a log
     * would leak the credential to anyone holding a logcat dump.
     */
    private fun String.redacted(): String =
        Regex("(token|api_key|apikey)=[^&]*", RegexOption.IGNORE_CASE)
            .replace(this) { "${it.groupValues[1]}=***" }
}
