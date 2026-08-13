package com.vythera.range.data.model

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable

/** Broad world buckets used for filtering and grouping results. */
enum class Region(val label: String, val short: String) {
    INDIA("India", "IN"),
    SOUTH_ASIA("South Asia", "SA"),
    SOUTHEAST_ASIA("Southeast Asia", "SEA"),
    EAST_ASIA("East Asia", "EA"),
    MIDDLE_EAST("Middle East", "ME"),
    CENTRAL_ASIA("Central Asia", "CA"),
    EUROPE("Europe", "EU"),
    AFRICA("Africa", "AF"),
    NORTH_AMERICA("North America", "NA"),
    LATIN_AMERICA("Latin America", "LA"),
    OCEANIA("Oceania", "OC"),
}

/** Continuous landmass, used to decide whether road / rail is even possible. */
enum class Landmass { EURASIA, AFRICA, NORTH_AM, SOUTH_AM, AUSTRALIA, ISLAND }

enum class Vibe(val label: String) {
    BEACH("Beach"),
    MOUNTAIN("Mountains"),
    CITY("City"),
    HERITAGE("Heritage"),
    NATURE("Nature"),
    NIGHTLIFE("Nightlife"),
    FOOD("Food"),
    SNOW("Snow"),
    DESERT("Desert"),
    ISLAND("Island"),
    WILDLIFE("Wildlife"),
    SPIRITUAL("Spiritual"),
    ROADTRIP("Road trip"),
    SHOPPING("Shopping"),
    ADVENTURE("Adventure"),
}

enum class Tier(val label: String, val blurb: String) {
    BUDGET("Budget", "Hostels, street food, local buses"),
    MID("Comfort", "3-star stays, sit-down meals, the odd cab"),
    LUXURY("Luxury", "4/5-star, fine dining, private transfers"),
    ;

    val stayBlurb: String
        get() = when (this) {
            BUDGET -> "Hostels, guesthouses, dorms"
            MID -> "3-star hotels and good apartments"
            LUXURY -> "4–5 star, resorts, suites"
        }

    val foodBlurb: String
        get() = when (this) {
            BUDGET -> "Street food and local joints"
            MID -> "Cafes and sit-down restaurants"
            LUXURY -> "Tasting menus and rooftop bars"
        }

    val experienceBlurb: String
        get() = when (this) {
            BUDGET -> "Free walks, a few entry tickets"
            MID -> "Tours, museums, a day trip or two"
            LUXURY -> "Private guides, boats, the big-ticket stuff"
        }
}

enum class TransportMode(val label: String, val verb: String) {
    FLIGHT("Flight", "Fly"),
    TRAIN("Train", "Take the train"),
    BUS("Bus", "Take the bus"),
    TAXI("Taxi / cab", "Hire a cab"),
    OWN_CAR("Own car", "Drive"),
    NONE("Already there", "Skip travel"),
}

/** How hard the last mile is — mountain roads are far slower than plains. */
enum class Terrain(val speedFactor: Double, val label: String) {
    FLAT(1.0, "easy roads"),
    HILL(0.70, "hill roads"),
    HIGH_MOUNTAIN(0.52, "high mountain passes"),
}

enum class VisaKind(val label: String) {
    NONE("No visa needed"),
    VISA_FREE("Visa free"),
    ON_ARRIVAL("Visa on arrival"),
    E_VISA("e-Visa"),
    EMBASSY("Embassy visa"),
}

/** How a trip lands against the budget. */
enum class Verdict(val label: String) {
    EASY("Well within range"),
    FITS("Fits your budget"),
    STRETCH("A small stretch"),
    OUT("Out of range"),
}

enum class CostKey(val label: String) {
    TRANSPORT("Getting there"),
    STAY("Stay"),
    FOOD("Food"),
    LOCAL("Getting around"),
    EXPERIENCES("Things to do"),
    VISA("Visa & entry"),
    INSURANCE("Insurance"),
    BUFFER("Buffer"),
}

data class Place(
    val id: String,
    val city: String,
    val country: String,
    val iata: String,
    val lat: Double,
    val lon: Double,
    val region: Region,
    val landmass: Landmass,
)

/**
 * A destination Range can price. Daily costs are stored as a mid-tier anchor in
 * USD and scaled per tier by [Pricing], which keeps the catalog compact.
 */
data class Destination(
    val id: String,
    val city: String,
    val country: String,
    val countryCode: String,
    val iata: String,
    val lat: Double,
    val lon: Double,
    val region: Region,
    val landmass: Landmass,
    /** Relative price level of the city. 0.35 = very cheap, 2.4 = eye-watering. */
    val costIndex: Double,
    /** Route competitiveness multiplier on the fare model. */
    val fareFactor: Double,
    /** Mid-tier double room, USD per night. */
    val hotelMid: Int,
    /** Mid-tier food, USD per person per day. */
    val foodMid: Int,
    val vibes: Set<Vibe>,
    val bestMonths: Set<Int>,
    val visaKind: VisaKind,
    val visaCostUsd: Int,
    val popularity: Double,
    val blurb: String,
    val highlights: List<String>,
    val gradient: List<Color>,
    /** Is there a usable railway station at or near the destination? */
    val hasRail: Boolean,
    val terrain: Terrain,
    /** Rough "wow per rupee" rating used by the value sort. 0..1. */
    val experienceScore: Double,
) {
    val place: Place
        get() = Place(id, city, country, iata, lat, lon, region, landmass)
}

@Serializable
data class SavedTrip(
    val id: String,
    val destinationId: String,
    val originId: String,
    val departEpochDay: Long,
    val nights: Int,
    val travelers: Int,
    val transport: String,
    val stayTier: String,
    val foodTier: String,
    val totalUsd: Double,
    val budgetUsd: Double,
    val savedAtEpochMs: Long,
    val note: String = "",
)
