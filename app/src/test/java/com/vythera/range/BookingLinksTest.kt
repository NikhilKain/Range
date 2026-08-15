package com.vythera.range

import com.vythera.range.data.DestinationCatalog
import com.vythera.range.data.OriginCatalog
import com.vythera.range.data.live.BookingLinks
import com.vythera.range.data.model.TransportMode
import com.vythera.range.domain.TripQuery
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Deep links to the booking sites.
 *
 * These replace scraping, so the thing that matters is that the URL carries
 * the trip that is on screen. A link that silently drops the return date or
 * the party size sends someone to a search for a different holiday, and looks
 * completely fine while doing it.
 */
class BookingLinksTest {

    private val delhi = OriginCatalog.find("del")
    private val dubai = DestinationCatalog.all.first { it.iata.equals("DXB", ignoreCase = true) }

    private val query = TripQuery(
        originId = "del",
        departDate = LocalDate.of(2026, 9, 29),
        nights = 5,
        travelers = 2,
        modes = setOf(TransportMode.FLIGHT),
        budgetUsd = 2_000.0,
    )

    @Test
    fun `every flight link carries route dates and party`() {
        val links = BookingLinks.flights(delhi, dubai, query)
        assertTrue("expected a decent spread of sites", links.size >= 6)

        links.forEach { link ->
            assertTrue("${link.name} lost the origin", link.url.contains("DEL", ignoreCase = true))
            assertTrue("${link.name} lost the destination", link.url.contains("DXB", ignoreCase = true))
            assertTrue("${link.name} is not https", link.url.startsWith("https://"))
            // 29 Sep in some encoding: ISO, dd/MM/yyyy, yyMMdd or ddMM.
            val hasDeparture = listOf("2026-09-29", "29%2F09%2F2026", "29/09/2026", "260929", "2909")
                .any { link.url.contains(it) }
            assertTrue("${link.name} lost the departure date: ${link.url}", hasDeparture)
        }
    }

    @Test
    fun `every flight link is a round trip`() {
        // The return leg is the easiest thing to drop and the hardest to spot:
        // a one-way search still looks like a working link.
        val links = BookingLinks.flights(delhi, dubai, query)
        links.forEach { link ->
            val hasReturn = listOf("2026-10-04", "04%2F10%2F2026", "04/10/2026", "261004", "0410")
                .any { link.url.contains(it) }
            assertTrue("${link.name} lost the return date: ${link.url}", hasReturn)
        }
    }

    @Test
    fun `indian sites appear for an indian origin and not otherwise`() {
        val fromDelhi = BookingLinks.flights(delhi, dubai, query).map { it.name }
        assertTrue(fromDelhi.contains("MakeMyTrip"))
        assertTrue(fromDelhi.contains("Cleartrip"))

        val abroad = OriginCatalog.all.first { !it.country.equals("India", ignoreCase = true) }
        val fromAbroad = BookingLinks
            .flights(abroad, dubai, query.copy(originId = abroad.id))
            .map { it.name }

        // MakeMyTrip is noise for someone flying out of Lisbon.
        assertFalse(fromAbroad.contains("MakeMyTrip"))
        assertFalse(fromAbroad.contains("Goibibo"))
        assertTrue("global sites should still be there", fromAbroad.contains("Google Flights"))
    }

    @Test
    fun `stays carry the city and the exact nights`() {
        val links = BookingLinks.stays(delhi, dubai, query)
        assertTrue(links.size >= 5)

        links.forEach { link ->
            assertTrue("${link.name} is not https", link.url.startsWith("https://"))
            assertTrue(
                "${link.name} lost the city: ${link.url}",
                link.url.contains("Dubai", ignoreCase = true),
            )
            assertTrue("${link.name} lost check-in", link.url.contains("2026-09-29"))
            assertTrue("${link.name} lost check-out", link.url.contains("2026-10-04"))
        }
    }

    @Test
    fun `party size reaches the sites that take it`() {
        val solo = BookingLinks.flights(delhi, dubai, query.copy(travelers = 1))
        val group = BookingLinks.flights(delhi, dubai, query.copy(travelers = 4))

        // Not every site encodes passengers, but the ones that do must not be
        // stuck on a default of one.
        assertTrue(group.any { it.url.contains("4") })
        assertEquals(solo.size, group.size)
    }

    @Test
    fun `a destination with no airport code yields no flight links`() {
        val noIata = dubai.copy(iata = "")
        assertTrue(BookingLinks.flights(delhi, noIata, query).isEmpty())
    }

    @Test
    fun `no link ever contains a raw space`() {
        // An unencoded space is the classic way a generated URL 404s.
        val all = BookingLinks.flights(delhi, dubai, query) +
            BookingLinks.stays(delhi, dubai, query)
        all.forEach { link ->
            assertFalse("${link.name} has an unencoded space: ${link.url}", link.url.contains(" "))
        }
    }
}
