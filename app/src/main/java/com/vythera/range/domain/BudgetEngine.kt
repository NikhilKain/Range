package com.vythera.range.domain

import com.vythera.range.data.model.CostKey
import com.vythera.range.data.model.Destination
import com.vythera.range.data.model.Place
import com.vythera.range.data.model.Tier
import com.vythera.range.data.model.TransportMode
import com.vythera.range.data.model.Verdict
import com.vythera.range.data.model.VisaKind
import java.time.LocalDate
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/** Everything the traveller told us. One immutable object drives the whole app. */
data class TripQuery(
    val originId: String = "del",
    val departDate: LocalDate = LocalDate.now().plusDays(30),
    val nights: Int = 5,
    val travelers: Int = 2,
    val modes: Set<TransportMode> = setOf(TransportMode.FLIGHT),
    val stay: Tier = Tier.MID,
    val food: Tier = Tier.MID,
    val experience: Tier = Tier.MID,
    val budgetUsd: Double = 700.0,
    val budgetIsPerPerson: Boolean = false,
    val peoplePerRoom: Int = 2,
    val includeVisa: Boolean = true,
    val includeInsurance: Boolean = true,
    val bufferPercent: Int = 8,
) {
    val days: Int get() = nights + 1
    val totalBudgetUsd: Double
        get() = if (budgetIsPerPerson) budgetUsd * travelers else budgetUsd
}

data class CostLine(
    val key: CostKey,
    val amountUsd: Double,
    val detail: String,
)

data class TransportOption(
    val mode: TransportMode,
    val costUsd: Double,
    val hoursOneWay: Double,
    val available: Boolean,
    val note: String,
)

/**
 * One priced trip. [maxNights] and [leanTotalUsd] are deliberately lazy: pricing
 * a whole catalogue happens on every keystroke of the budget ruler, and those
 * two answers are only ever read for the handful of cards actually on screen.
 */
data class TripEstimate(
    val destination: Destination,
    val origin: Place,
    val distanceKm: Double,
    val bearing: Double,
    val mode: TransportMode,
    val transportOptions: List<TransportOption>,
    val lines: List<CostLine>,
    val totalUsd: Double,
    val budgetUsd: Double,
    val query: TripQuery,
    val seasonNote: String,
    val inSeason: Boolean,
) {
    /** Longest trip this budget supports at the chosen tiers, in nights. */
    val maxNights: Int by lazy(LazyThreadSafetyMode.NONE) {
        BudgetEngine.longestStay(origin, destination, query, budgetUsd)
    }

    /** Total if every tier drops to Budget — powers the "it fits if…" nudge. */
    val leanTotalUsd: Double by lazy(LazyThreadSafetyMode.NONE) {
        BudgetEngine.leanTotal(origin, destination, query)
    }

    val perPersonUsd: Double get() = totalUsd / query.travelers.coerceAtLeast(1)
    val perDayUsd: Double get() = totalUsd / query.days.coerceAtLeast(1)
    val ratio: Double get() = if (budgetUsd <= 0) 99.0 else totalUsd / budgetUsd
    val headroomUsd: Double get() = budgetUsd - totalUsd

    val verdict: Verdict
        get() = when {
            ratio <= 0.82 -> Verdict.EASY
            ratio <= 1.0 -> Verdict.FITS
            ratio <= 1.18 -> Verdict.STRETCH
            else -> Verdict.OUT
        }

    val fits: Boolean get() = verdict != Verdict.OUT
    val withinBudget: Boolean get() = ratio <= 1.0

    /** "Budget tiers would make this work" — only worth surfacing when true. */
    val leanRescue: Boolean get() = !withinBudget && leanTotalUsd <= budgetUsd

    fun line(key: CostKey): Double = lines.firstOrNull { it.key == key }?.amountUsd ?: 0.0
}

object BudgetEngine {

    // ---- tier multipliers -------------------------------------------------

    private fun stayMult(t: Tier) = when (t) {
        Tier.BUDGET -> 0.42
        Tier.MID -> 1.0
        Tier.LUXURY -> 2.85
    }

    private fun foodMult(t: Tier) = when (t) {
        Tier.BUDGET -> 0.45
        Tier.MID -> 1.0
        Tier.LUXURY -> 2.60
    }

    private fun localMult(t: Tier) = when (t) {
        Tier.BUDGET -> 0.55
        Tier.MID -> 1.0
        Tier.LUXURY -> 2.20
    }

    private fun experienceDaily(t: Tier) = when (t) {
        Tier.BUDGET -> 7.0
        Tier.MID -> 17.0
        Tier.LUXURY -> 38.0
    }

    private fun classMult(t: Tier) = when (t) {
        Tier.BUDGET -> 0.62
        Tier.MID -> 1.0
        Tier.LUXURY -> 2.4
    }

    // ---- seasonality ------------------------------------------------------

    /** Airfares and hotels both move with demand; this is one shared signal. */
    fun seasonMultiplier(dest: Destination, date: LocalDate): Double {
        val month = date.monthValue
        val peak = month in dest.bestMonths
        var m = if (peak) 1.16 else 0.93
        // Global holiday spikes.
        if (month == 12 && date.dayOfMonth >= 18) m *= 1.22
        if (month == 1 && date.dayOfMonth <= 4) m *= 1.18
        if (month == 5 || month == 6) m *= 1.06 // school holidays out of India
        return m
    }

    fun leadTimeMultiplier(depart: LocalDate, today: LocalDate = LocalDate.now()): Double {
        val days = java.time.temporal.ChronoUnit.DAYS.between(today, depart).toInt()
        return when {
            days < 0 -> 1.0
            days <= 3 -> 1.55
            days <= 7 -> 1.34
            days <= 14 -> 1.18
            days <= 30 -> 1.05
            days <= 90 -> 1.0
            days <= 180 -> 0.95
            else -> 0.93
        }
    }

    // ---- transport --------------------------------------------------------

    private fun flightRoundTripPerPerson(
        km: Double,
        dest: Destination,
        origin: Place,
        query: TripQuery,
    ): Double {
        val base = 28.0 + 0.062 * km.pow(0.98)
        val domestic = origin.country.equals(dest.country, ignoreCase = true)
        val domesticMult = if (domestic) 0.72 else 1.0
        val season = seasonMultiplier(dest, query.departDate)
        val lead = leadTimeMultiplier(query.departDate)
        val cabin = when (query.stay) {
            Tier.LUXURY -> 1.85 // premium economy / front of the bus
            Tier.BUDGET -> 0.92
            else -> 1.0
        }
        return base * dest.fareFactor * domesticMult * season * lead * cabin * 1.9
    }

    private fun sameLandmass(origin: Place, dest: Destination) = origin.landmass == dest.landmass

    fun transportOptions(origin: Place, dest: Destination, query: TripQuery): List<TransportOption> {
        val gcKm = haversineKm(origin.lat, origin.lon, dest.lat, dest.lon)
        val roadKm = surfaceKm(gcKm)
        val people = query.travelers.coerceAtLeast(1)
        val land = sameLandmass(origin, dest)
        val cls = classMult(query.stay)

        val flight = TransportOption(
            mode = TransportMode.FLIGHT,
            costUsd = flightRoundTripPerPerson(gcKm, dest, origin, query) * people,
            hoursOneWay = gcKm / 780.0 + 2.2,
            available = gcKm > 250,
            note = if (gcKm > 250) "Return economy, ${people}×" else "Too close to bother flying",
        )

        val trainPerKm = when (query.stay) {
            Tier.BUDGET -> 0.007
            Tier.MID -> 0.017
            Tier.LUXURY -> 0.032
        }
        val train = TransportOption(
            mode = TransportMode.TRAIN,
            costUsd = (3.0 + roadKm * trainPerKm) * 2 * people,
            hoursOneWay = roadKm / 62.0 + 1.0,
            available = land && roadKm in 60.0..3200.0,
            note = when {
                !land -> "No rail link — there's water in the way"
                roadKm > 3200 -> "Too far for a sane rail trip"
                else -> "Return tickets, ${people}×"
            },
        )

        val busPerKm = when (query.stay) {
            Tier.BUDGET -> 0.011
            Tier.MID -> 0.019
            Tier.LUXURY -> 0.030
        }
        val bus = TransportOption(
            mode = TransportMode.BUS,
            costUsd = (2.0 + roadKm * busPerKm) * 2 * people,
            hoursOneWay = roadKm / 46.0 + 1.0,
            available = land && roadKm in 40.0..1900.0,
            note = when {
                !land -> "No road link from here"
                roadKm > 1900 -> "That's two days on a bus each way"
                else -> "Return seats, ${people}×"
            },
        )

        val cars = ceil(people / 4.0)
        val taxi = TransportOption(
            mode = TransportMode.TAXI,
            costUsd = (roadKm * 2 * 0.22 * cars) + (12.0 * cars * query.days) * cls,
            hoursOneWay = roadKm / 54.0 + 0.5,
            available = land && roadKm in 20.0..1100.0,
            note = when {
                !land -> "No road link from here"
                roadKm > 1100 -> "A cab this far costs more than flying"
                else -> "${cars.toInt()} cab(s), return + driver"
            },
        )

        val ownCars = ceil(people / 5.0)
        val ownCar = TransportOption(
            mode = TransportMode.OWN_CAR,
            costUsd = (roadKm * 2 * 0.090 * ownCars) + (4.0 * query.days * ownCars),
            hoursOneWay = roadKm / 58.0 + 0.5,
            available = land && roadKm in 20.0..2600.0,
            note = when {
                !land -> "You can't drive there"
                roadKm > 2600 -> "That's a very long drive"
                else -> "Fuel + tolls + parking, ${ownCars.toInt()} car(s)"
            },
        )

        val none = TransportOption(
            mode = TransportMode.NONE,
            costUsd = 0.0,
            hoursOneWay = 0.0,
            available = true,
            note = "Costing the stay only",
        )

        return listOf(flight, train, bus, taxi, ownCar, none)
    }

    // ---- full estimate ----------------------------------------------------

    fun estimate(origin: Place, dest: Destination, query: TripQuery): TripEstimate {
        val gcKm = haversineKm(origin.lat, origin.lon, dest.lat, dest.lon)
        val bearing = bearingDeg(origin.lat, origin.lon, dest.lat, dest.lon)
        val options = transportOptions(origin, dest, query)

        val wantsNoTravel = query.modes == setOf(TransportMode.NONE)
        val allowed = options.filter { it.available && it.mode in query.modes }
        val chosen = when {
            wantsNoTravel -> options.first { it.mode == TransportMode.NONE }
            allowed.isNotEmpty() -> allowed.minByOrNull { it.costUsd }!!
            // Nothing you picked can serve this route — usually a hop that is
            // too short to fly. Price the cheapest thing that actually works.
            else -> options
                .filter { it.available && it.mode != TransportMode.NONE }
                .minByOrNull { it.costUsd }
                ?: options.first { it.mode == TransportMode.NONE }
        }

        val lines = costLines(origin, dest, query, chosen)
        val total = lines.sumOf { it.amountUsd }

        val budget = query.totalBudgetUsd
        val month = query.departDate.monthValue
        val inSeason = month in dest.bestMonths

        return TripEstimate(
            destination = dest,
            origin = origin,
            distanceKm = gcKm,
            bearing = bearing,
            mode = chosen.mode,
            transportOptions = options,
            lines = lines,
            totalUsd = total,
            budgetUsd = budget,
            query = query,
            seasonNote = seasonNote(dest, query.departDate, inSeason),
            inSeason = inSeason,
        )
    }

    private fun seasonNote(dest: Destination, date: LocalDate, inSeason: Boolean): String {
        val monthName = date.month.name.lowercase().replaceFirstChar { it.uppercase() }
        return if (inSeason) {
            "$monthName is peak season here — prices reflect it."
        } else {
            "$monthName is off-peak in ${dest.city}. Cheaper, quieter, slightly riskier weather."
        }
    }

    private fun costLines(
        origin: Place,
        dest: Destination,
        query: TripQuery,
        transport: TransportOption,
    ): List<CostLine> {
        val people = query.travelers.coerceAtLeast(1)
        val nights = query.nights.coerceAtLeast(0)
        val days = (nights + 1).coerceAtLeast(1)
        val season = seasonMultiplier(dest, query.departDate)
        val international = !origin.country.equals(dest.country, ignoreCase = true)

        val rooms = ceil(people / query.peoplePerRoom.coerceAtLeast(1).toDouble()).toInt().coerceAtLeast(1)
        val stayNightly = dest.hotelMid * stayMult(query.stay) * (0.85 + 0.15 * season)
        val stay = stayNightly * rooms * nights

        val food = dest.foodMid * foodMult(query.food) * people * days
        val local = dest.costIndex * 5.5 * localMult(query.food) * people * days
        val experiences = dest.costIndex * experienceDaily(query.experience) * people * days

        val visa = if (query.includeVisa && international && dest.visaKind != VisaKind.NONE) {
            dest.visaCostUsd.toDouble() * people
        } else {
            0.0
        }
        val insurance = if (query.includeInsurance) {
            (if (international) 1.9 else 0.5) * people * days
        } else {
            0.0
        }

        val subtotal = transport.costUsd + stay + food + local + experiences + visa + insurance
        val buffer = subtotal * (query.bufferPercent.coerceIn(0, 30) / 100.0)

        val stayLabel = when (query.stay) {
            Tier.BUDGET -> "hostel / guesthouse"
            Tier.MID -> "3-star"
            Tier.LUXURY -> "4–5 star"
        }
        val foodLabel = when (query.food) {
            Tier.BUDGET -> "street & local"
            Tier.MID -> "cafes & restaurants"
            Tier.LUXURY -> "fine dining"
        }

        return buildList {
            if (transport.costUsd > 0 || transport.mode != TransportMode.NONE) {
                val substituted = transport.mode !in query.modes &&
                    query.modes != setOf(TransportMode.NONE)
                val detail = if (substituted) {
                    "${transport.mode.label} instead — ${transport.note.lowercase()}"
                } else {
                    "${transport.mode.label} · ${transport.note}"
                }
                add(CostLine(CostKey.TRANSPORT, transport.costUsd, detail))
            }
            if (nights > 0) {
                add(CostLine(CostKey.STAY, stay, "$rooms × $stayLabel × $nights night${if (nights == 1) "" else "s"}"))
            }
            add(CostLine(CostKey.FOOD, food, "$foodLabel · $people × $days day${if (days == 1) "" else "s"}"))
            add(CostLine(CostKey.LOCAL, local, "Metro, autos, day cabs"))
            add(CostLine(CostKey.EXPERIENCES, experiences, "Entries, tours, the odd splurge"))
            if (visa > 0) add(CostLine(CostKey.VISA, visa, "${dest.visaKind.label} · $people ×"))
            if (insurance > 0) add(CostLine(CostKey.INSURANCE, insurance, "Cover for $people for $days days"))
            if (buffer > 0) add(CostLine(CostKey.BUFFER, buffer, "${query.bufferPercent}% for the unexpected"))
        }
    }

    internal fun leanTotal(origin: Place, dest: Destination, query: TripQuery): Double {
        val lean = query.copy(stay = Tier.BUDGET, food = Tier.BUDGET, experience = Tier.BUDGET)
        val options = transportOptions(origin, dest, lean)
        val transport = options.filter { it.available && it.mode in lean.modes }
            .minByOrNull { it.costUsd }
            ?: options.first { it.mode == TransportMode.NONE }
        return costLines(origin, dest, lean, transport).sumOf { it.amountUsd }
    }

    /** How many nights this budget stretches to, holding every other choice fixed. */
    internal fun longestStay(origin: Place, dest: Destination, query: TripQuery, budget: Double): Int {
        var best = 0
        for (n in 1..30) {
            val q = query.copy(nights = n)
            val opts = transportOptions(origin, dest, q)
            val t = opts.filter { it.available && it.mode in q.modes }.minByOrNull { it.costUsd }
                ?: opts.first { it.mode == TransportMode.NONE }
            val total = costLines(origin, dest, q, t).sumOf { it.amountUsd }
            if (total <= budget) best = n else break
        }
        return best
    }

    // ---- exploration ------------------------------------------------------

    fun explore(
        origin: Place,
        destinations: List<Destination>,
        query: TripQuery,
    ): List<TripEstimate> = destinations
        .asSequence()
        .filter { it.id != origin.id }
        .map { estimate(origin, it, query) }
        .filter { it.distanceKm > 40 }
        .toList()

    /** The single number the home screen teases: how much of the world opens up. */
    fun reachSummary(estimates: List<TripEstimate>): ReachSummary {
        val inRange = estimates.filter { it.withinBudget }
        val countries = inRange.map { it.destination.country }.distinct().size
        val farthest = inRange.maxByOrNull { it.distanceKm }
        return ReachSummary(
            total = estimates.size,
            inRange = inRange.size,
            countries = countries,
            farthestKm = farthest?.distanceKm ?: 0.0,
            farthestCity = farthest?.destination?.city,
            regions = inRange.map { it.destination.region }.distinct().size,
            cheapest = inRange.minByOrNull { it.totalUsd },
            bestValue = inRange.maxByOrNull { valueScore(it) },
        )
    }

    /** Wow-per-rupee: experience quality against how much of the budget it eats. */
    fun valueScore(e: TripEstimate): Double {
        val spend = max(0.15, min(1.4, e.ratio))
        val season = if (e.inSeason) 1.12 else 0.98
        val reach = 1.0 + min(0.35, e.distanceKm / 30_000.0)
        return (e.destination.experienceScore * 0.7 + e.destination.popularity * 0.3) *
            season * reach / spend
    }
}

data class ReachSummary(
    val total: Int,
    val inRange: Int,
    val countries: Int,
    val farthestKm: Double,
    val farthestCity: String?,
    val regions: Int,
    val cheapest: TripEstimate?,
    val bestValue: TripEstimate?,
)
