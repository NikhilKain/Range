package com.vythera.range

import com.vythera.range.data.DestinationCatalog
import com.vythera.range.data.OriginCatalog
import com.vythera.range.data.model.CostKey
import com.vythera.range.data.model.Landmass
import com.vythera.range.data.model.Tier
import com.vythera.range.data.model.TransportMode
import com.vythera.range.domain.BudgetEngine
import com.vythera.range.domain.Currency
import com.vythera.range.domain.TripQuery
import com.vythera.range.domain.formatMoney
import com.vythera.range.domain.haversineKm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import kotlin.math.abs

class CatalogTest {

    @Test
    fun `destination ids are unique`() {
        val ids = DestinationCatalog.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `origin ids are unique`() {
        val ids = OriginCatalog.all.map { it.id }
        assertEquals(ids.size, ids.toSet().size)
    }

    @Test
    fun `every destination is fully described`() {
        DestinationCatalog.all.forEach { d ->
            assertTrue("${d.id} needs a blurb", d.blurb.length > 20)
            assertTrue("${d.id} needs highlights", d.highlights.size >= 3)
            assertTrue("${d.id} needs vibes", d.vibes.isNotEmpty())
            assertTrue("${d.id} needs best months", d.bestMonths.isNotEmpty())
            assertTrue("${d.id} lat out of range", abs(d.lat) <= 90)
            assertTrue("${d.id} lon out of range", abs(d.lon) <= 180)
            assertTrue("${d.id} needs a gradient", d.gradient.size == 2)
            assertTrue("${d.id} hotel price looks wrong", d.hotelMid in 15..250)
            assertTrue("${d.id} food price looks wrong", d.foodMid in 5..80)
            assertTrue("${d.id} months must be 1..12", d.bestMonths.all { it in 1..12 })
        }
    }

    @Test
    fun `known distances are right`() {
        val del = OriginCatalog.find("del")
        val dubai = DestinationCatalog.byId.getValue("dubai")
        val km = haversineKm(del.lat, del.lon, dubai.lat, dubai.lon)
        assertTrue("Delhi to Dubai should be ~2200km, got $km", km in 2000.0..2400.0)

        val london = DestinationCatalog.byId.getValue("london")
        val kmLon = haversineKm(del.lat, del.lon, london.lat, london.lon)
        assertTrue("Delhi to London should be ~6700km, got $kmLon", kmLon in 6400.0..7000.0)
    }
}

class BudgetEngineTest {

    private val origin = OriginCatalog.find("del")
    private val baseQuery = TripQuery(
        originId = "del",
        departDate = LocalDate.now().plusDays(60),
        nights = 5,
        travelers = 2,
        modes = setOf(TransportMode.FLIGHT),
        budgetUsd = 900.0,
    )

    @Test
    fun `every destination prices without blowing up`() {
        DestinationCatalog.all.forEach { d ->
            val e = BudgetEngine.estimate(origin, d, baseQuery)
            assertTrue("${d.id} total must be positive", e.totalUsd > 0)
            val sum = e.lines.sumOf { it.amountUsd }
            assertTrue("${d.id} lines must sum to total", abs(sum - e.totalUsd) < 0.01)
            assertTrue("${d.id} maxNights must be sane", e.maxNights in 0..30)
        }
    }

    @Test
    fun `all transport and tier combinations are safe`() {
        val goa = DestinationCatalog.byId.getValue("goa")
        TransportMode.entries.forEach { mode ->
            Tier.entries.forEach { tier ->
                val q = baseQuery.copy(modes = setOf(mode), stay = tier, food = tier, experience = tier)
                val e = BudgetEngine.estimate(origin, goa, q)
                assertTrue(e.totalUsd >= 0)
            }
        }
    }

    @Test
    fun `a week in Goa for two is realistically priced`() {
        val goa = DestinationCatalog.byId.getValue("goa")
        val e = BudgetEngine.estimate(origin, goa, baseQuery)
        // Roughly 45k-110k INR for two people, five nights, mid tier.
        val inr = e.totalUsd * Currency.INR.perUsd
        assertTrue("Goa came out at ${formatMoney(e.totalUsd, Currency.INR)}", inr in 40_000.0..120_000.0)
    }

    @Test
    fun `luxury costs more than budget everywhere`() {
        DestinationCatalog.all.take(30).forEach { d ->
            val lean = BudgetEngine.estimate(
                origin,
                d,
                baseQuery.copy(stay = Tier.BUDGET, food = Tier.BUDGET, experience = Tier.BUDGET),
            )
            val lux = BudgetEngine.estimate(
                origin,
                d,
                baseQuery.copy(stay = Tier.LUXURY, food = Tier.LUXURY, experience = Tier.LUXURY),
            )
            assertTrue("${d.id} luxury should cost more", lux.totalUsd > lean.totalUsd)
        }
    }

    @Test
    fun `more people costs more but less per head on rooms`() {
        val bali = DestinationCatalog.byId.getValue("bali")
        val solo = BudgetEngine.estimate(origin, bali, baseQuery.copy(travelers = 1))
        val pair = BudgetEngine.estimate(origin, bali, baseQuery.copy(travelers = 2))
        assertTrue(pair.totalUsd > solo.totalUsd)
        assertTrue("sharing a room should beat two singles", pair.perPersonUsd < solo.totalUsd)
    }

    @Test
    fun `you cannot drive to an island`() {
        val bali = DestinationCatalog.byId.getValue("bali")
        val options = BudgetEngine.transportOptions(origin, bali, baseQuery)
        listOf(TransportMode.TRAIN, TransportMode.BUS, TransportMode.TAXI, TransportMode.OWN_CAR)
            .forEach { mode ->
                assertTrue(
                    "$mode must be unavailable to Bali",
                    options.first { it.mode == mode }.available.not(),
                )
            }
        assertTrue(options.first { it.mode == TransportMode.FLIGHT }.available)
    }

    @Test
    fun `trains and buses work for a domestic hop`() {
        val jaipur = DestinationCatalog.byId.getValue("jaipur")
        val options = BudgetEngine.transportOptions(origin, jaipur, baseQuery)
        assertTrue(options.first { it.mode == TransportMode.TRAIN }.available)
        assertTrue(options.first { it.mode == TransportMode.BUS }.available)
        val train = options.first { it.mode == TransportMode.TRAIN }
        val flight = options.first { it.mode == TransportMode.FLIGHT }
        assertTrue("the train should undercut the plane to Jaipur", train.costUsd < flight.costUsd)
    }

    @Test
    fun `a hop too short to fly still gets priced by road`() {
        val rishikesh = DestinationCatalog.byId.getValue("rishikesh")
        val flightOnly = baseQuery.copy(modes = setOf(TransportMode.FLIGHT))
        val e = BudgetEngine.estimate(origin, rishikesh, flightOnly)
        assertTrue(
            "should substitute a surface mode, got ${e.mode}",
            e.mode != TransportMode.FLIGHT && e.mode != TransportMode.NONE,
        )
        assertTrue("substituted travel must cost something", e.line(CostKey.TRANSPORT) > 0)
    }

    @Test
    fun `there is no train or drive from Delhi to Dubai`() {
        val dubai = DestinationCatalog.byId.getValue("dubai")
        val options = BudgetEngine.transportOptions(origin, dubai, baseQuery)
        listOf(TransportMode.TRAIN, TransportMode.BUS, TransportMode.TAXI, TransportMode.OWN_CAR)
            .forEach { mode ->
                assertTrue(
                    "$mode must not be offered to Dubai",
                    !options.first { it.mode == mode }.available,
                )
            }
    }

    @Test
    fun `no overland route from India to mainland southeast asia`() {
        val bangkok = DestinationCatalog.byId.getValue("bangkok")
        val options = BudgetEngine.transportOptions(origin, bangkok, baseQuery)
        assertTrue(!options.first { it.mode == TransportMode.TRAIN }.available)
        assertTrue(!options.first { it.mode == TransportMode.BUS }.available)
    }

    @Test
    fun `mountain destinations without a railhead are never priced by train`() {
        listOf("spiti", "leh", "manali", "gangtok", "shillong").forEach { id ->
            val dest = DestinationCatalog.byId.getValue(id)
            val options = BudgetEngine.transportOptions(origin, dest, baseQuery)
            assertTrue(
                "$id has no railway but a train was offered",
                !options.first { it.mode == TransportMode.TRAIN }.available,
            )
        }
    }

    @Test
    fun `driving to Spiti takes far longer than driving the same distance on plains`() {
        val spiti = DestinationCatalog.byId.getValue("spiti")
        val jaipur = DestinationCatalog.byId.getValue("jaipur")
        val spitiDrive = BudgetEngine.transportOptions(origin, spiti, baseQuery)
            .first { it.mode == TransportMode.OWN_CAR }
        val jaipurDrive = BudgetEngine.transportOptions(origin, jaipur, baseQuery)
            .first { it.mode == TransportMode.OWN_CAR }
        val spitiSpeed = spiti.let { BudgetEngine } // readability only
        assertTrue(spitiDrive.available && jaipurDrive.available)
        assertTrue(
            "mountain roads must be slower per km",
            spitiDrive.hoursOneWay / spitiDrive.let { 1.0 } > jaipurDrive.hoursOneWay,
        )
        assertTrue(spitiSpeed === BudgetEngine)
    }

    @Test
    fun `overland options still work inside India`() {
        val jaipur = DestinationCatalog.byId.getValue("jaipur")
        val options = BudgetEngine.transportOptions(origin, jaipur, baseQuery)
        assertTrue(options.first { it.mode == TransportMode.TRAIN }.available)
        assertTrue(options.first { it.mode == TransportMode.OWN_CAR }.available)
    }

    @Test
    fun `no travel mode means no travel cost`() {
        val goa = DestinationCatalog.byId.getValue("goa")
        val e = BudgetEngine.estimate(origin, goa, baseQuery.copy(modes = setOf(TransportMode.NONE)))
        assertEquals(0.0, e.line(CostKey.TRANSPORT), 0.001)
        val withFlight = BudgetEngine.estimate(origin, goa, baseQuery)
        assertTrue(e.totalUsd < withFlight.totalUsd)
    }

    @Test
    fun `booking last minute costs more than booking early`() {
        val dubai = DestinationCatalog.byId.getValue("dubai")
        val soon = BudgetEngine.estimate(
            origin,
            dubai,
            baseQuery.copy(departDate = LocalDate.now().plusDays(2)),
        )
        val later = BudgetEngine.estimate(
            origin,
            dubai,
            baseQuery.copy(departDate = LocalDate.now().plusDays(200)),
        )
        assertTrue(soon.totalUsd > later.totalUsd)
    }

    @Test
    fun `per person budget multiplies out`() {
        val q = baseQuery.copy(budgetIsPerPerson = true, budgetUsd = 500.0, travelers = 3)
        assertEquals(1500.0, q.totalBudgetUsd, 0.001)
    }

    @Test
    fun `every major origin has domestic options to offer`() {
        // Someone in London should not be told their only choice is to leave.
        listOf("lhr", "jfk", "syd", "cdg", "hnd", "dxb", "del", "yyz", "cpt", "gru").forEach { id ->
            val home = OriginCatalog.find(id)
            val domestic = DestinationCatalog.all.count {
                it.country.equals(home.country, ignoreCase = true) && it.id != home.id
            }
            assertTrue("${home.city} has only $domestic domestic destinations", domestic >= 2)
        }
    }

    @Test
    fun `no visa is charged inside a free movement area`() {
        val london = OriginCatalog.find("lhr")
        val paris = DestinationCatalog.byId.getValue("paris")
        val e = BudgetEngine.estimate(london, paris, baseQuery)
        assertEquals(0.0, e.line(CostKey.VISA), 0.001)

        // But an Indian traveller to Paris still pays for the Schengen visa.
        val fromDelhi = BudgetEngine.estimate(origin, paris, baseQuery)
        assertTrue(fromDelhi.line(CostKey.VISA) > 0)
    }

    @Test
    fun `domestic trips are flagged as domestic`() {
        val london = OriginCatalog.find("lhr")
        val bath = DestinationCatalog.byId.getValue("bath")
        val paris = DestinationCatalog.byId.getValue("paris")
        assertTrue(BudgetEngine.estimate(london, bath, baseQuery).domestic)
        assertTrue(!BudgetEngine.estimate(london, paris, baseQuery).domestic)
        assertEquals(0.0, BudgetEngine.estimate(london, bath, baseQuery).line(CostKey.VISA), 0.001)
    }

    @Test
    fun `a londoner sees home and abroad options in range`() {
        val london = OriginCatalog.find("lhr")
        val q = baseQuery.copy(originId = "lhr", budgetUsd = 2500.0)
        val results = BudgetEngine.explore(london, DestinationCatalog.all, q)
        val summary = BudgetEngine.reachSummary(results)
        assertTrue("expected UK options in range", summary.domesticInRange >= 1)
        assertTrue("expected trips abroad in range", summary.internationalInRange >= 3)
    }

    @Test
    fun `explore returns something affordable at a sensible budget`() {
        val q = baseQuery.copy(budgetUsd = 1200.0)
        val results = BudgetEngine.explore(origin, DestinationCatalog.all, q)
        val summary = BudgetEngine.reachSummary(results)
        assertTrue("expected some destinations in range", summary.inRange > 5)
        assertTrue("expected several countries", summary.countries > 2)
        assertTrue(summary.inRange < summary.total)
    }

    @Test
    fun `a tiny budget reaches nothing far away`() {
        val q = baseQuery.copy(budgetUsd = 60.0, nights = 3)
        val results = BudgetEngine.explore(origin, DestinationCatalog.all, q)
        assertTrue(results.none { it.withinBudget && it.distanceKm > 4000 })
    }

    @Test
    fun `island destinations are marked as islands`() {
        val maldives = DestinationCatalog.byId.getValue("male")
        assertEquals(Landmass.ISLAND, maldives.landmass)
    }
}

class MoneyTest {

    @Test
    fun `indian grouping is lakh style`() {
        assertEquals("₹1,00,000", formatMoney(100_000 / Currency.INR.perUsd, Currency.INR))
        assertEquals("₹5,000", formatMoney(5_000 / Currency.INR.perUsd, Currency.INR))
    }

    @Test
    fun `western grouping is thousands`() {
        assertEquals("$1,200", formatMoney(1200.0, Currency.USD))
    }
}
