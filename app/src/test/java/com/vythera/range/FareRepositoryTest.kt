package com.vythera.range

import com.vythera.range.data.live.FareProvider
import com.vythera.range.data.live.FareQuote
import com.vythera.range.data.live.FareRepository
import com.vythera.range.data.live.FareRequest
import com.vythera.range.data.live.FareSource
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * The caching rules, which exist to protect a metered quota.
 *
 * Every one of these failures is invisible at runtime: nothing crashes when a
 * cache misses, it just quietly spends a request that did not need spending,
 * and a free tier of ~66 calls a day does not survive much of that.
 */
class FareRepositoryTest {

    private val request = FareRequest(
        originIata = "DEL",
        destIata = "DXB",
        departDate = LocalDate.of(2026, 9, 1),
        returnDate = LocalDate.of(2026, 9, 6),
        travelers = 2,
    )

    private class FakeProvider(
        override val source: FareSource = FareSource.AMADEUS,
        override val configured: Boolean = true,
        private val answer: (FareRequest) -> FareQuote? = { null },
    ) : FareProvider {
        var calls = 0
            private set

        override suspend fun quote(request: FareRequest): FareQuote? {
            calls++
            return answer(request)
        }
    }

    private fun quote(usd: Double, source: FareSource = FareSource.AMADEUS) = FareQuote(
        originIata = "DEL",
        destIata = "DXB",
        totalUsd = usd,
        source = source,
        fetchedAtMs = System.currentTimeMillis(),
    )

    private fun repo(
        providers: List<FareProvider>,
        now: () -> Long = System::currentTimeMillis,
        ttlMs: Long = FareRepository.TTL_MS,
    ) = FareRepository(
        persistCache = {},
        providers = providers,
        ttlMs = ttlMs,
        now = now,
    )

    @Test
    fun `a hit is served from cache rather than the provider`() = runTest {
        val provider = FakeProvider { quote(500.0) }
        val repository = repo(listOf(provider))

        repeat(5) { repository.quote(request) }

        assertEquals("only the first call should reach the network", 1, provider.calls)
    }

    @Test
    fun `a miss is cached too`() = runTest {
        // The wasteful case: a route nobody flies, re-asked on every scroll.
        val provider = FakeProvider { null }
        val repository = repo(listOf(provider))

        repeat(5) { assertNull(repository.quote(request)) }

        assertEquals(1, provider.calls)
    }

    @Test
    fun `a stale entry is refetched`() = runTest {
        var clock = 0L
        val provider = FakeProvider { quote(500.0) }
        val repository = repo(listOf(provider), now = { clock }, ttlMs = 1_000)

        repository.quote(request)
        clock += 1_500
        repository.quote(request)

        assertEquals(2, provider.calls)
    }

    @Test
    fun `the second provider is tried only when the first has nothing`() = runTest {
        val first = FakeProvider(FareSource.AMADEUS) { null }
        val second = FakeProvider(FareSource.TRAVELPAYOUTS) { quote(420.0, FareSource.TRAVELPAYOUTS) }
        val repository = repo(listOf(first, second))

        val result = repository.quote(request)

        assertNotNull(result)
        assertEquals(FareSource.TRAVELPAYOUTS, result!!.source)
        assertEquals(1, first.calls)
        assertEquals(1, second.calls)
    }

    @Test
    fun `the second provider is skipped when the first answers`() = runTest {
        val first = FakeProvider(FareSource.AMADEUS) { quote(500.0) }
        val second = FakeProvider(FareSource.TRAVELPAYOUTS) { quote(420.0, FareSource.TRAVELPAYOUTS) }
        val repository = repo(listOf(first, second))

        assertEquals(FareSource.AMADEUS, repository.quote(request)!!.source)
        assertEquals("the fallback should not have been paid for", 0, second.calls)
    }

    @Test
    fun `an unconfigured provider is never called`() = runTest {
        val unconfigured = FakeProvider(configured = false) { quote(1.0) }
        val repository = repo(listOf(unconfigured))

        assertNull(repository.quote(request))
        assertEquals(0, unconfigured.calls)
    }

    @Test
    fun `concurrent callers share one request`() = runTest {
        // Opening a destination while its card is still resolving. Two requests
        // for the same thing is exactly the waste the in-flight map prevents.
        val gate = CompletableDeferred<Unit>()
        val provider = object : FareProvider {
            override val source = FareSource.AMADEUS
            override val configured = true
            var calls = 0
                private set

            override suspend fun quote(request: FareRequest): FareQuote? {
                calls++
                gate.await()
                return quote(500.0)
            }

            private fun quote(usd: Double) = FareQuote("DEL", "DXB", usd, source, 0L)
        }
        val repository = repo(listOf(provider))

        val racers = List(4) { async { repository.quote(request) } }
        gate.complete(Unit)
        val results = racers.awaitAll()

        assertEquals(1, provider.calls)
        assertTrue("every caller should get the same answer", results.all { it != null })
    }

    @Test
    fun `a provider that throws falls back to the model instead of propagating`() = runTest {
        val exploding = object : FareProvider {
            override val source = FareSource.AMADEUS
            override val configured = true
            override suspend fun quote(request: FareRequest): FareQuote? =
                error("provider bug")
        }

        // Must not throw — a broken provider takes the live price away, not
        // the whole results screen.
        assertNull(repo(listOf(exploding)).quote(request))
    }

    @Test
    fun `cached reads the synchronous path only returns fresh hits`() = runTest {
        var clock = 0L
        val repository = repo(listOf(FakeProvider { quote(500.0) }), now = { clock }, ttlMs = 1_000)

        assertNull("nothing cached yet", repository.cached(request))
        repository.quote(request)
        assertNotNull(repository.cached(request))

        clock += 2_000
        assertNull("stale entries must not paint", repository.cached(request))
    }
}
