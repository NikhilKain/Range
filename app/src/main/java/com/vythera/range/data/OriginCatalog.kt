package com.vythera.range.data

import com.vythera.range.data.model.Landmass
import com.vythera.range.data.model.Place
import com.vythera.range.data.model.Region

/** Cities you can start from. Range prices every trip relative to one of these. */
object OriginCatalog {

    private fun p(
        id: String,
        city: String,
        country: String,
        iata: String,
        lat: Double,
        lon: Double,
        region: Region,
        land: Landmass = Landmass.EURASIA,
    ) = Place(id, city, country, iata, lat, lon, region, land)

    val all: List<Place> by lazy {
        listOf(
            p("del", "Delhi", "India", "DEL", 28.56, 77.10, Region.INDIA),
            p("bom", "Mumbai", "India", "BOM", 19.09, 72.87, Region.INDIA),
            p("blr", "Bengaluru", "India", "BLR", 13.20, 77.71, Region.INDIA),
            p("maa", "Chennai", "India", "MAA", 12.99, 80.17, Region.INDIA),
            p("ccu", "Kolkata", "India", "CCU", 22.65, 88.45, Region.INDIA),
            p("hyd", "Hyderabad", "India", "HYD", 17.24, 78.43, Region.INDIA),
            p("pnq", "Pune", "India", "PNQ", 18.58, 73.92, Region.INDIA),
            p("amd", "Ahmedabad", "India", "AMD", 23.07, 72.63, Region.INDIA),
            p("cok", "Kochi", "India", "COK", 10.15, 76.40, Region.INDIA),
            p("jai", "Jaipur", "India", "JAI", 26.82, 75.81, Region.INDIA),
            p("lko", "Lucknow", "India", "LKO", 26.76, 80.89, Region.INDIA),
            p("ixc", "Chandigarh", "India", "IXC", 30.67, 76.79, Region.INDIA),
            p("gau", "Guwahati", "India", "GAU", 26.11, 91.59, Region.INDIA),
            p("bbi", "Bhubaneswar", "India", "BBI", 20.24, 85.82, Region.INDIA),
            p("idr", "Indore", "India", "IDR", 22.72, 75.80, Region.INDIA),
            p("nag", "Nagpur", "India", "NAG", 21.09, 79.05, Region.INDIA),
            p("trv", "Thiruvananthapuram", "India", "TRV", 8.48, 76.92, Region.INDIA),
            p("cjb", "Coimbatore", "India", "CJB", 11.03, 77.04, Region.INDIA),
            p("pat", "Patna", "India", "PAT", 25.59, 85.09, Region.INDIA),
            p("srn", "Srinagar", "India", "SXR", 33.99, 74.77, Region.INDIA),
            p("ded", "Dehradun", "India", "DED", 30.19, 78.18, Region.INDIA),
            p("vtz", "Visakhapatnam", "India", "VTZ", 17.72, 83.22, Region.INDIA),
            p("dxb", "Dubai", "UAE", "DXB", 25.25, 55.36, Region.MIDDLE_EAST),
            p("auh", "Abu Dhabi", "UAE", "AUH", 24.43, 54.65, Region.MIDDLE_EAST),
            p("doh", "Doha", "Qatar", "DOH", 25.27, 51.61, Region.MIDDLE_EAST),
            p("sin", "Singapore", "Singapore", "SIN", 1.36, 103.99, Region.SOUTHEAST_ASIA, Landmass.ISLAND),
            p("kul", "Kuala Lumpur", "Malaysia", "KUL", 2.75, 101.71, Region.SOUTHEAST_ASIA),
            p("bkk", "Bangkok", "Thailand", "BKK", 13.69, 100.75, Region.SOUTHEAST_ASIA),
            p("cmb", "Colombo", "Sri Lanka", "CMB", 7.18, 79.88, Region.SOUTH_ASIA, Landmass.ISLAND),
            p("ktm", "Kathmandu", "Nepal", "KTM", 27.70, 85.36, Region.SOUTH_ASIA),
            p("lhr", "London", "UK", "LHR", 51.47, (-0.45), Region.EUROPE),
            p("cdg", "Paris", "France", "CDG", 49.01, 2.55, Region.EUROPE),
            p("jfk", "New York", "USA", "JFK", 40.64, (-73.78), Region.NORTH_AMERICA, Landmass.NORTH_AM),
            p("yyz", "Toronto", "Canada", "YYZ", 43.68, (-79.63), Region.NORTH_AMERICA, Landmass.NORTH_AM),
            p("syd", "Sydney", "Australia", "SYD", (-33.94), 151.18, Region.OCEANIA, Landmass.AUSTRALIA),
        )
    }

    val byId: Map<String, Place> by lazy { all.associateBy { it.id } }

    val default: Place get() = byId.getValue("del")

    fun find(id: String): Place = byId[id] ?: default
}
