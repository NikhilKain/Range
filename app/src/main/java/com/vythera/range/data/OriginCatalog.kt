package com.vythera.range.data

import com.vythera.range.data.model.Landmass
import com.vythera.range.data.model.Place
import com.vythera.range.data.model.Region

/**
 * Cities you can start from. Range prices every trip relative to one of these,
 * so the list spans every region — the app is just as useful from Lagos, Lima
 * or Lisbon as it is from Ludhiana.
 */
object OriginCatalog {

    private fun p(
        id: String,
        city: String,
        country: String,
        iata: String,
        lat: Double,
        lon: Double,
        region: Region,
        currency: String = "USD",
        land: Landmass = Landmass.EURASIA,
    ) = Place(id, city, country, iata, lat, lon, region, land, currency)

    val all: List<Place> by lazy {
        listOf(
            // ---- India ----
            p("del", "Delhi", "India", "DEL", 28.56, 77.10, Region.INDIA, "INR"),
            p("bom", "Mumbai", "India", "BOM", 19.09, 72.87, Region.INDIA, "INR"),
            p("blr", "Bengaluru", "India", "BLR", 13.20, 77.71, Region.INDIA, "INR"),
            p("maa", "Chennai", "India", "MAA", 12.99, 80.17, Region.INDIA, "INR"),
            p("ccu", "Kolkata", "India", "CCU", 22.65, 88.45, Region.INDIA, "INR"),
            p("hyd", "Hyderabad", "India", "HYD", 17.24, 78.43, Region.INDIA, "INR"),
            p("pnq", "Pune", "India", "PNQ", 18.58, 73.92, Region.INDIA, "INR"),
            p("amd", "Ahmedabad", "India", "AMD", 23.07, 72.63, Region.INDIA, "INR"),
            p("cok", "Kochi", "India", "COK", 10.15, 76.40, Region.INDIA, "INR"),
            p("jai", "Jaipur", "India", "JAI", 26.82, 75.81, Region.INDIA, "INR"),
            p("lko", "Lucknow", "India", "LKO", 26.76, 80.89, Region.INDIA, "INR"),
            p("ixc", "Chandigarh", "India", "IXC", 30.67, 76.79, Region.INDIA, "INR"),
            p("gau", "Guwahati", "India", "GAU", 26.11, 91.59, Region.INDIA, "INR"),
            p("bbi", "Bhubaneswar", "India", "BBI", 20.24, 85.82, Region.INDIA, "INR"),
            p("idr", "Indore", "India", "IDR", 22.72, 75.80, Region.INDIA, "INR"),
            p("nag", "Nagpur", "India", "NAG", 21.09, 79.05, Region.INDIA, "INR"),
            p("trv", "Thiruvananthapuram", "India", "TRV", 8.48, 76.92, Region.INDIA, "INR"),
            p("cjb", "Coimbatore", "India", "CJB", 11.03, 77.04, Region.INDIA, "INR"),
            p("pat", "Patna", "India", "PAT", 25.59, 85.09, Region.INDIA, "INR"),
            p("srn", "Srinagar", "India", "SXR", 33.99, 74.77, Region.INDIA, "INR"),
            p("ded", "Dehradun", "India", "DED", 30.19, 78.18, Region.INDIA, "INR"),
            p("vtz", "Visakhapatnam", "India", "VTZ", 17.72, 83.22, Region.INDIA, "INR"),

            // ---- South Asia ----
            p("cmb", "Colombo", "Sri Lanka", "CMB", 7.18, 79.88, Region.SOUTH_ASIA, "USD", Landmass.ISLAND),
            p("ktm", "Kathmandu", "Nepal", "KTM", 27.70, 85.36, Region.SOUTH_ASIA),
            p("dac", "Dhaka", "Bangladesh", "DAC", 23.84, 90.40, Region.SOUTH_ASIA),
            p("khi", "Karachi", "Pakistan", "KHI", 24.91, 67.16, Region.SOUTH_ASIA),
            p("lhe", "Lahore", "Pakistan", "LHE", 31.52, 74.40, Region.SOUTH_ASIA),

            // ---- Middle East ----
            p("dxb", "Dubai", "UAE", "DXB", 25.25, 55.36, Region.MIDDLE_EAST, "AED"),
            p("auh", "Abu Dhabi", "UAE", "AUH", 24.43, 54.65, Region.MIDDLE_EAST, "AED"),
            p("doh", "Doha", "Qatar", "DOH", 25.27, 51.61, Region.MIDDLE_EAST),
            p("ruh", "Riyadh", "Saudi Arabia", "RUH", 24.96, 46.70, Region.MIDDLE_EAST),
            p("jed", "Jeddah", "Saudi Arabia", "JED", 21.68, 39.16, Region.MIDDLE_EAST),
            p("mct", "Muscat", "Oman", "MCT", 23.59, 58.28, Region.MIDDLE_EAST),
            p("kwi", "Kuwait City", "Kuwait", "KWI", 29.23, 47.98, Region.MIDDLE_EAST),
            p("bah", "Manama", "Bahrain", "BAH", 26.27, 50.63, Region.MIDDLE_EAST),
            p("ist", "Istanbul", "Turkey", "IST", 41.26, 28.74, Region.MIDDLE_EAST, "EUR"),
            p("tlv", "Tel Aviv", "Israel", "TLV", 32.01, 34.89, Region.MIDDLE_EAST),
            p("amm", "Amman", "Jordan", "AMM", 31.72, 35.99, Region.MIDDLE_EAST),

            // ---- Southeast Asia ----
            p("sin", "Singapore", "Singapore", "SIN", 1.36, 103.99, Region.SOUTHEAST_ASIA, "SGD"),
            p("kul", "Kuala Lumpur", "Malaysia", "KUL", 2.75, 101.71, Region.SOUTHEAST_ASIA),
            p("bkk", "Bangkok", "Thailand", "BKK", 13.69, 100.75, Region.SOUTHEAST_ASIA),
            p("cgk", "Jakarta", "Indonesia", "CGK", (-6.13), 106.66, Region.SOUTHEAST_ASIA),
            p("mnl", "Manila", "Philippines", "MNL", 14.51, 121.02, Region.SOUTHEAST_ASIA, "USD", Landmass.ISLAND),
            p("sgn", "Ho Chi Minh City", "Vietnam", "SGN", 10.82, 106.66, Region.SOUTHEAST_ASIA),
            p("han", "Hanoi", "Vietnam", "HAN", 21.22, 105.81, Region.SOUTHEAST_ASIA),

            // ---- East Asia ----
            p("hnd", "Tokyo", "Japan", "HND", 35.55, 139.78, Region.EAST_ASIA, "USD", Landmass.ISLAND),
            p("icn", "Seoul", "South Korea", "ICN", 37.46, 126.44, Region.EAST_ASIA),
            p("hkg", "Hong Kong", "Hong Kong", "HKG", 22.31, 113.91, Region.EAST_ASIA),
            p("pvg", "Shanghai", "China", "PVG", 31.14, 121.80, Region.EAST_ASIA),
            p("pek", "Beijing", "China", "PEK", 40.08, 116.58, Region.EAST_ASIA),
            p("tpe", "Taipei", "Taiwan", "TPE", 25.08, 121.23, Region.EAST_ASIA, "USD", Landmass.ISLAND),

            // ---- Europe ----
            p("lhr", "London", "UK", "LHR", 51.47, (-0.45), Region.EUROPE, "GBP"),
            p("man", "Manchester", "UK", "MAN", 53.36, (-2.27), Region.EUROPE, "GBP"),
            p("cdg", "Paris", "France", "CDG", 49.01, 2.55, Region.EUROPE, "EUR"),
            p("ams", "Amsterdam", "Netherlands", "AMS", 52.31, 4.76, Region.EUROPE, "EUR"),
            p("ber", "Berlin", "Germany", "BER", 52.37, 13.50, Region.EUROPE, "EUR"),
            p("fra", "Frankfurt", "Germany", "FRA", 50.04, 8.56, Region.EUROPE, "EUR"),
            p("mad", "Madrid", "Spain", "MAD", 40.47, (-3.56), Region.EUROPE, "EUR"),
            p("bcn", "Barcelona", "Spain", "BCN", 41.30, 2.08, Region.EUROPE, "EUR"),
            p("fco", "Rome", "Italy", "FCO", 41.80, 12.24, Region.EUROPE, "EUR"),
            p("mxp", "Milan", "Italy", "MXP", 45.63, 8.72, Region.EUROPE, "EUR"),
            p("lis", "Lisbon", "Portugal", "LIS", 38.77, (-9.13), Region.EUROPE, "EUR"),
            p("dub", "Dublin", "Ireland", "DUB", 53.43, (-6.25), Region.EUROPE, "EUR"),
            p("zrh", "Zurich", "Switzerland", "ZRH", 47.46, 8.55, Region.EUROPE, "EUR"),
            p("cph", "Copenhagen", "Denmark", "CPH", 55.62, 12.66, Region.EUROPE, "EUR"),
            p("arn", "Stockholm", "Sweden", "ARN", 59.65, 17.92, Region.EUROPE, "EUR"),
            p("waw", "Warsaw", "Poland", "WAW", 52.17, 20.97, Region.EUROPE, "EUR"),
            p("ath", "Athens", "Greece", "ATH", 37.94, 23.94, Region.EUROPE, "EUR"),
            p("svo", "Moscow", "Russia", "SVO", 55.97, 37.41, Region.EUROPE),

            // ---- Africa ----
            p("cai", "Cairo", "Egypt", "CAI", 30.11, 31.41, Region.AFRICA, "USD", Landmass.AFRICA),
            p("cmn", "Casablanca", "Morocco", "CMN", 33.37, (-7.59), Region.AFRICA, "USD", Landmass.AFRICA),
            p("los", "Lagos", "Nigeria", "LOS", 6.58, 3.32, Region.AFRICA, "USD", Landmass.AFRICA),
            p("nbo", "Nairobi", "Kenya", "NBO", (-1.32), 36.93, Region.AFRICA, "USD", Landmass.AFRICA),
            p("jnb", "Johannesburg", "South Africa", "JNB", (-26.13), 28.24, Region.AFRICA, "USD", Landmass.AFRICA),
            p("cpt", "Cape Town", "South Africa", "CPT", (-33.97), 18.60, Region.AFRICA, "USD", Landmass.AFRICA),

            // ---- North America ----
            p("jfk", "New York", "USA", "JFK", 40.64, (-73.78), Region.NORTH_AMERICA, "USD", Landmass.NORTH_AM),
            p("lax", "Los Angeles", "USA", "LAX", 33.94, (-118.41), Region.NORTH_AMERICA, "USD", Landmass.NORTH_AM),
            p("sfo", "San Francisco", "USA", "SFO", 37.62, (-122.38), Region.NORTH_AMERICA, "USD", Landmass.NORTH_AM),
            p("ord", "Chicago", "USA", "ORD", 41.98, (-87.90), Region.NORTH_AMERICA, "USD", Landmass.NORTH_AM),
            p("mia", "Miami", "USA", "MIA", 25.80, (-80.29), Region.NORTH_AMERICA, "USD", Landmass.NORTH_AM),
            p("iah", "Houston", "USA", "IAH", 29.99, (-95.34), Region.NORTH_AMERICA, "USD", Landmass.NORTH_AM),
            p("yyz", "Toronto", "Canada", "YYZ", 43.68, (-79.63), Region.NORTH_AMERICA, "CAD", Landmass.NORTH_AM),
            p("yvr", "Vancouver", "Canada", "YVR", 49.19, (-123.18), Region.NORTH_AMERICA, "CAD", Landmass.NORTH_AM),
            p("mex", "Mexico City", "Mexico", "MEX", 19.44, (-99.07), Region.NORTH_AMERICA, "USD", Landmass.NORTH_AM),

            // ---- Latin America ----
            p("gru", "Sao Paulo", "Brazil", "GRU", (-23.43), (-46.47), Region.LATIN_AMERICA, "USD", Landmass.SOUTH_AM),
            p("eze", "Buenos Aires", "Argentina", "EZE", (-34.82), (-58.54), Region.LATIN_AMERICA, "USD", Landmass.SOUTH_AM),
            p("bog", "Bogota", "Colombia", "BOG", 4.70, (-74.15), Region.LATIN_AMERICA, "USD", Landmass.SOUTH_AM),
            p("lim", "Lima", "Peru", "LIM", (-12.02), (-77.11), Region.LATIN_AMERICA, "USD", Landmass.SOUTH_AM),
            p("scl", "Santiago", "Chile", "SCL", (-33.39), (-70.79), Region.LATIN_AMERICA, "USD", Landmass.SOUTH_AM),

            // ---- Oceania ----
            p("syd", "Sydney", "Australia", "SYD", (-33.94), 151.18, Region.OCEANIA, "AUD", Landmass.AUSTRALIA),
            p("mel", "Melbourne", "Australia", "MEL", (-37.67), 144.84, Region.OCEANIA, "AUD", Landmass.AUSTRALIA),
            p("per", "Perth", "Australia", "PER", (-31.94), 115.97, Region.OCEANIA, "AUD", Landmass.AUSTRALIA),
            p("akl", "Auckland", "New Zealand", "AKL", (-37.01), 174.79, Region.OCEANIA, "USD", Landmass.ISLAND),
        )
    }

    val byId: Map<String, Place> by lazy { all.associateBy { it.id } }

    /** Grouped for pickers, in a sensible reading order. */
    val byRegion: List<Pair<Region, List<Place>>> by lazy {
        Region.entries.mapNotNull { region ->
            all.filter { it.region == region }.takeIf { it.isNotEmpty() }?.let { region to it }
        }
    }

    /** A spread across the world, for the onboarding picker. */
    val popular: List<Place> by lazy {
        listOf(
            "del", "bom", "blr", "dxb", "sin", "lhr", "jfk", "syd",
            "cdg", "bkk", "hnd", "cai", "jnb", "gru", "yyz", "ams",
        ).mapNotNull { byId[it] }
    }

    val default: Place get() = byId.getValue("del")

    fun find(id: String): Place = byId[id] ?: default
}
