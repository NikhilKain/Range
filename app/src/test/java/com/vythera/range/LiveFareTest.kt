package com.vythera.range

import com.vythera.range.data.DestinationCatalog
import com.vythera.range.data.OriginCatalog
import com.vythera.range.data.live.FareQuote
import com.vythera.range.data.live.FareRequest
import com.vythera.range.data.live.FareSource
import com.vythera.range.data.model.CostKey
import com.vythera.range.data.model.TransportMode
import com.vythera.range.domain.BudgetEngine
import com.vythera.range.domain.TripQuery
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * The live-fare path, tested where it can go wrong silently.
 *
 * None of these touch the network. The interesting failures are not "the API
 * was down" — that path just falls back and is obviously fine — but the ones
 * that produce a plausible wrong number: a fare applied after the transport
 * mode was chosen, or a per-person price treated as a per-party one.
 */
class LiveFareTest {

    private val origin = OriginCatalog.find("del")
    private val query = TripQuery(
        originId = "del",
        departDate = LocalDate.now().plusDays(45),
        nights = 5,
        travelers = 2,
        modes = setOf(TransportMode.FLIGHT),
        budgetUsd = 2_000.0,
    )

    private fun quote(usd: Double) = FareQuote(
        originIata = "DEL",
        destIata = "XXX",
        totalUsd = usd,
        source = FareSource.AMADEUS,
        fetchedAtMs = System.currentTimeMillis(),
    )

    @Test
    fun `live fare replaces the modelled transport line`() {
        val dest = DestinationCatalog.all.first { it.id != "del" && it.iata.isNotBlank() }

        val modelled = BudgetEngine.estimate(origin, dest, query)
        val live = BudgetEngine.estimate(origin, dest, query, quote(777.0))

        assertEquals(777.0, live.line(CostKey.TRANSPORT), 0.01)
        assertTrue(
            "the total must move with the fare, not just the label",
            modelled.totalUsd != live.totalUsd,
        )
    }

    @Test
    fun `a cheaper live fare can flip the chosen mode`() {
        // A destination reachable by both air and surface, so there is a real
        // choice to flip. Without one, this test would pass vacuously.
        val dest = DestinationCatalog.all.first { d ->
            d.hasRail && d.country.equals("India", ignoreCase = true) && d.id != "del"
        }
        val q = query.copy(modes = setOf(TransportMode.FLIGHT, TransportMode.TRAIN))

        val modelled = BudgetEngine.estimate(origin, dest, q)
        val trainCost = modelled.transportOptions
            .first { it.mode == TransportMode.TRAIN }.costUsd

        // Undercut the train by half. If the fare were applied after the mode
        // was picked, the estimate would still be sitting on the train.
        val live = BudgetEngine.estimate(origin, dest, q, quote(trainCost / 2))

        assertEquals(TransportMode.FLIGHT, live.mode)
        assertEquals(trainCost / 2, live.line(CostKey.TRANSPORT), 0.01)
    }

    @Test
    fun `an expensive live fare can flip the choice the other way`() {
        val dest = DestinationCatalog.all.first { d ->
            d.hasRail && d.country.equals("India", ignoreCase = true) && d.id != "del"
        }
        val q = query.copy(modes = setOf(TransportMode.FLIGHT, TransportMode.TRAIN))
        val modelled = BudgetEngine.estimate(origin, dest, q)
        val trainCost = modelled.transportOptions
            .first { it.mode == TransportMode.TRAIN }.costUsd

        val live = BudgetEngine.estimate(origin, dest, q, quote(trainCost * 10))

        assertEquals(TransportMode.TRAIN, live.mode)
        // The trip is going by train, so nothing about it is a live fare and
        // it must not claim to be.
        assertNull(live.liveFare)
        assertTrue(!live.isLive)
    }

    @Test
    fun `a flight quote is ignored when the traveller is not flying`() {
        val dest = DestinationCatalog.all.first { d ->
            d.hasRail && d.country.equals("India", ignoreCase = true) && d.id != "del"
        }
        val q = query.copy(modes = setOf(TransportMode.TRAIN))

        val modelled = BudgetEngine.estimate(origin, dest, q)
        val live = BudgetEngine.estimate(origin, dest, q, quote(1.0))

        assertEquals(modelled.totalUsd, live.totalUsd, 0.01)
        assertNull(live.liveFare)
    }

    @Test
    fun `explore applies only the fares it is given`() {
        val subset = DestinationCatalog.all.take(20).filter { it.id != "del" }
        val target = subset.first()

        val results = BudgetEngine.explore(
            origin,
            subset,
            query,
            mapOf(target.id to quote(500.0)),
        )

        val hit = results.firstOrNull { it.destination.id == target.id }
        // take(20) may include something inside the 40 km floor explore drops.
        if (hit != null) {
            assertNotNull(hit.liveFare)
            assertEquals(1, results.count { it.isLive })
        }
        assertTrue(results.none { it.destination.id != target.id && it.isLive })
    }

    @Test
    fun `the lean total uses the live fare too`() {
        val dest = DestinationCatalog.all.first { it.id != "del" && it.iata.isNotBlank() }

        val cheapFare = 40.0
        val live = BudgetEngine.estimate(origin, dest, query, quote(cheapFare))
        val modelled = BudgetEngine.estimate(origin, dest, query)

        // Otherwise the headline total would rest on a real fare while the
        // "budget tiers would make this work" nudge quietly used a different one.
        assertTrue(
            "lean total should follow the live fare down",
            live.leanTotalUsd < modelled.leanTotalUsd,
        )
    }

    @Test
    fun `request keys distinguish everything that changes the price`() {
        val base = FareRequest("DEL", "DXB", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 6), 2)

        val variants = listOf(
            base.copy(destIata = "BKK"),
            base.copy(departDate = LocalDate.of(2026, 9, 2)),
            base.copy(returnDate = LocalDate.of(2026, 9, 7)),
            base.copy(returnDate = null),
            base.copy(travelers = 3),
        )

        // A key that collides across any of these would serve a cached price
        // for a trip the traveller is not taking.
        val keys = (variants + base).map { it.key }
        assertEquals(keys.size, keys.toSet().size)
    }
}
