package com.vythera.range.data.live

import com.vythera.range.BuildConfig
import com.vythera.range.data.model.Destination
import com.vythera.range.data.model.Place
import com.vythera.range.domain.TripQuery
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * One-tap handoffs to the sites that actually sell the thing.
 *
 * Range cannot pull a bookable price *in* — the sites that have them render
 * client-side behind bot protection, and a sideloaded app cannot ship a fix
 * when that breaks, because nobody auto-updates an APK from GitHub. So instead
 * of guessing at their numbers, this sends the traveller *out* to them with
 * the search already filled in.
 *
 * A URL with query parameters is a public interface these sites maintain on
 * purpose; their internal JSON endpoints are not. That difference is the whole
 * reason this file exists and a scraper does not.
 *
 * Every link is built from the trip already on screen: same route, same dates,
 * same party. Nothing about the traveller is attached.
 */
data class BookingLink(
    val name: String,
    val url: String,
    /** Regional sites are only worth showing to travellers they serve. */
    val regional: Boolean = false,
)

object BookingLinks {

    private val iso: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val slashed: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.US)
    private val compact: DateTimeFormatter = DateTimeFormatter.ofPattern("yyMMdd", Locale.US)
    private val dayMonth: DateTimeFormatter = DateTimeFormatter.ofPattern("ddMM", Locale.US)

    private fun encode(value: String): String = Http.encode(value)

    private fun returnDate(query: TripQuery): LocalDate =
        query.departDate.plusDays(query.nights.toLong().coerceAtLeast(1))

    /**
     * Flight searches, pre-filled.
     *
     * Ordered by how likely the result is to be useful rather than
     * alphabetically: metasearch first, because it spans the others.
     */
    fun flights(origin: Place, dest: Destination, query: TripQuery): List<BookingLink> {
        val from = origin.iata.uppercase()
        val to = dest.iata.uppercase()
        if (from.isBlank() || to.isBlank()) return emptyList()

        val out = query.departDate
        val back = returnDate(query)
        val adults = query.travelers.coerceAtLeast(1)
        val indian = origin.country.equals("India", ignoreCase = true)

        return buildList {
            // Natural-language query rather than the tfs= protobuf: the encoded
            // form is undocumented and version-specific, this one is stable.
            add(
                BookingLink(
                    "Google Flights",
                    "https://www.google.com/travel/flights?q=" +
                        encode("Flights from $from to $to on ${out.format(iso)} through ${back.format(iso)}"),
                ),
            )
            add(
                BookingLink(
                    "Skyscanner",
                    "https://www.skyscanner.net/transport/flights/" +
                        "${from.lowercase()}/${to.lowercase()}/" +
                        "${out.format(compact)}/${back.format(compact)}/" +
                        "?adults=$adults",
                ),
            )
            add(
                BookingLink(
                    "Kayak",
                    "https://www.kayak.com/flights/$from-$to/" +
                        "${out.format(iso)}/${back.format(iso)}/${adults}adults",
                ),
            )
            add(
                BookingLink(
                    "Momondo",
                    "https://www.momondo.com/flight-search/$from-$to/" +
                        "${out.format(iso)}/${back.format(iso)}/${adults}adults",
                ),
            )
            add(
                BookingLink(
                    "Kiwi.com",
                    "https://www.kiwi.com/en/search/results/$from/$to/" +
                        "${out.format(iso)}/${back.format(iso)}",
                ),
            )
            // Carries the affiliate marker when one is configured, so a booking
            // made through this row pays back. Harmless without it.
            add(BookingLink("Aviasales", aviasales(from, to, out, back, adults)))

            if (indian) {
                add(
                    BookingLink(
                        "MakeMyTrip",
                        "https://www.makemytrip.com/flight/search" +
                            "?itinerary=$from-$to-${out.format(slashed)}_$to-$from-${back.format(slashed)}" +
                            "&tripType=R&paxType=A-${adults}_C-0_I-0&cabinClass=E",
                        regional = true,
                    ),
                )
                add(
                    BookingLink(
                        "Cleartrip",
                        "https://www.cleartrip.com/flights/results" +
                            "?from=$from&to=$to" +
                            "&depart_date=${encode(out.format(slashed))}" +
                            "&return_date=${encode(back.format(slashed))}" +
                            "&adults=$adults&childs=0&infants=0&class=Economy",
                        regional = true,
                    ),
                )
                add(
                    BookingLink(
                        "Ixigo",
                        "https://www.ixigo.com/search/result/flight" +
                            "?from=$from&to=$to" +
                            "&date=${encode(out.format(slashed))}" +
                            "&returnDate=${encode(back.format(slashed))}" +
                            "&adults=$adults&children=0&infants=0&class=e&round=t",
                        regional = true,
                    ),
                )
            }
        }
    }

    /**
     * Stay searches for the destination city.
     *
     * Worth having next to the flights: lodging is the second-largest line in
     * most Range trips, and it is the one the model is least certain about
     * because a "3-star in Bali" spans an enormous range.
     */
    fun stays(origin: Place, dest: Destination, query: TripQuery): List<BookingLink> {
        val city = "${dest.city}, ${dest.country}"
        val checkIn = query.departDate
        val checkOut = returnDate(query)
        val adults = query.travelers.coerceAtLeast(1)
        val rooms = Math.ceil(adults / query.peoplePerRoom.coerceAtLeast(1).toDouble())
            .toInt().coerceAtLeast(1)
        val indian = origin.country.equals("India", ignoreCase = true)

        return buildList {
            add(
                BookingLink(
                    "Booking.com",
                    "https://www.booking.com/searchresults.html" +
                        "?ss=${encode(city)}" +
                        "&checkin=${checkIn.format(iso)}&checkout=${checkOut.format(iso)}" +
                        "&group_adults=$adults&no_rooms=$rooms&group_children=0",
                ),
            )
            add(
                BookingLink(
                    "Agoda",
                    "https://www.agoda.com/search" +
                        "?city=${encode(dest.city)}" +
                        "&checkIn=${checkIn.format(iso)}&checkOut=${checkOut.format(iso)}" +
                        "&adults=$adults&rooms=$rooms",
                ),
            )
            add(
                BookingLink(
                    "Hostelworld",
                    "https://www.hostelworld.com/search?search_keywords=${encode(city)}" +
                        "&date_from=${checkIn.format(iso)}&date_to=${checkOut.format(iso)}" +
                        "&number_of_guests=$adults",
                ),
            )
            add(
                BookingLink(
                    "Airbnb",
                    "https://www.airbnb.com/s/${encode(city)}/homes" +
                        "?checkin=${checkIn.format(iso)}&checkout=${checkOut.format(iso)}" +
                        "&adults=$adults",
                ),
            )
            add(
                BookingLink(
                    "Hotels.com",
                    "https://www.hotels.com/Hotel-Search" +
                        "?destination=${encode(city)}" +
                        "&startDate=${checkIn.format(iso)}&endDate=${checkOut.format(iso)}" +
                        "&adults=$adults",
                ),
            )
            if (indian) {
                add(
                    BookingLink(
                        "MakeMyTrip",
                        "https://www.makemytrip.com/hotels/hotel-listing/" +
                            "?checkin=${checkIn.format(iso)}&checkout=${checkOut.format(iso)}" +
                            "&roomStayQualifier=${adults}e0e&city=${encode(dest.city)}" +
                            "&country=${encode(dest.countryCode)}",
                        regional = true,
                    ),
                )
                add(
                    BookingLink(
                        "Goibibo",
                        "https://www.goibibo.com/hotels/find-hotels-in-${slug(dest.city)}/" +
                            "?checkin=${checkIn.format(iso)}&checkout=${checkOut.format(iso)}" +
                            "&roomString=1-$adults-0",
                        regional = true,
                    ),
                )
            }
        }
    }

    /**
     * Aviasales encodes the whole search into the path: origin, outbound as
     * DDMM, destination, return as DDMM, then the passenger count.
     */
    private fun aviasales(
        from: String,
        to: String,
        out: LocalDate,
        back: LocalDate,
        adults: Int,
    ): String {
        val path = "https://www.aviasales.com/search/" +
            "$from${out.format(dayMonth)}$to${back.format(dayMonth)}${adults.coerceIn(1, 9)}"
        val marker = BuildConfig.TRAVELPAYOUTS_MARKER
        return if (marker.isBlank()) path else "$path?marker=${encode(marker)}"
    }

    /** Goibibo builds city pages from a hyphenated slug. */
    private fun slug(city: String): String =
        city.lowercase()
            .replace(Regex("[^a-z0-9]+"), "-")
            .trim('-')
}
