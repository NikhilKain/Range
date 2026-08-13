package com.vythera.range.domain

import com.vythera.range.data.model.Destination
import com.vythera.range.data.model.Landmass
import com.vythera.range.data.model.Place

/**
 * Whether you can actually get somewhere overland.
 *
 * "Same continent" is not the same question: Delhi and Dubai share Eurasia, but
 * there is no train, bus or drive between them. So instead of geography, Range
 * groups places into blocs that are genuinely connected by usable road and rail
 * for a traveller, and only offers surface transport inside a bloc.
 */
object SurfaceReach {

    private val blocByCountry: Map<String, String> = buildMap {
        // Indian subcontinent — open land borders with road and rail links.
        listOf("India", "Nepal", "Bhutan", "Bangladesh").forEach { put(it, "IN") }

        // Mainland Southeast Asia, joined by the Asian Highway network.
        listOf("Thailand", "Malaysia", "Singapore", "Vietnam", "Cambodia", "Laos", "Myanmar")
            .forEach { put(it, "SEA") }

        // Greater China.
        listOf("China", "Hong Kong").forEach { put(it, "CN") }

        // Pakistan shares a land border with India, but it is closed to
        // through travel — a bloc of its own is the honest model.
        put("Pakistan", "PK")
        put("Indonesia", "ID")
        put("Philippines", "PH")
        put("Israel", "ME")

        // Effectively sealed by sea or by a closed border.
        put("Japan", "JP")
        put("South Korea", "KR")
        put("Taiwan", "TW")

        // Gulf and Levant road network.
        listOf("UAE", "Qatar", "Oman", "Saudi Arabia", "Jordan", "Kuwait", "Bahrain")
            .forEach { put(it, "ME") }

        // Europe, Turkey and the Caucasus are one continuous overland region.
        listOf(
            "UK", "France", "Netherlands", "Germany", "Czechia", "Hungary", "Austria",
            "Italy", "Spain", "Portugal", "Greece", "Switzerland", "Norway", "Denmark",
            "Sweden", "Finland", "Ireland", "Poland", "Croatia", "Romania", "Slovenia",
            "Russia", "Turkey", "Georgia", "Armenia", "Azerbaijan", "Bulgaria", "Serbia",
        ).forEach { put(it, "EU") }

        // Central Asia.
        listOf("Kazakhstan", "Uzbekistan", "Kyrgyzstan", "Tajikistan").forEach { put(it, "CA") }

        // Africa, split by the practical road networks.
        listOf("Egypt", "Morocco", "Tunisia", "Algeria", "Libya").forEach { put(it, "AF_N") }
        listOf("Kenya", "Tanzania", "Uganda", "Rwanda", "Ethiopia").forEach { put(it, "AF_E") }
        listOf("South Africa", "Zimbabwe", "Botswana", "Namibia", "Zambia")
            .forEach { put(it, "AF_S") }
        listOf("Nigeria", "Ghana", "Senegal").forEach { put(it, "AF_W") }

        // The Americas.
        listOf("USA", "Canada", "Mexico").forEach { put(it, "NAM") }
        listOf("Brazil", "Argentina", "Peru", "Chile", "Colombia", "Bolivia", "Ecuador")
            .forEach { put(it, "SAM") }

        put("Australia", "AU")
        put("New Zealand", "NZ")
    }

    /** Islands get a bloc of their own, so nothing drives or rails to them. */
    fun blocOf(country: String, landmass: Landmass, id: String): String =
        if (landmass == Landmass.ISLAND) "ISLAND:$id" else blocByCountry[country] ?: "X:$country"

    fun blocOf(place: Place): String = blocOf(place.country, place.landmass, place.id)

    fun blocOf(destination: Destination): String =
        blocOf(destination.country, destination.landmass, destination.id)

    /** True only when road and rail could realistically join the two. */
    fun connected(origin: Place, destination: Destination): Boolean {
        val a = blocOf(origin)
        val b = blocOf(destination)
        if (a.startsWith("X:") || b.startsWith("X:")) return a == b
        return a == b
    }

    fun reasonNotConnected(origin: Place, destination: Destination): String = when {
        destination.landmass == Landmass.ISLAND -> "It's an island — you have to fly"
        origin.landmass == Landmass.ISLAND -> "No road or rail off ${origin.country}"
        else -> "No usable road or rail route from ${origin.country}"
    }
}
