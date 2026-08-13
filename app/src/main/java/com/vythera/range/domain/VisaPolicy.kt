package com.vythera.range.domain

/**
 * Whether the traveller actually needs a visa.
 *
 * The catalog stores each destination's entry requirement for a visitor who
 * needs one. That is the wrong answer for someone travelling inside their own
 * free-movement area — a Londoner does not buy a Schengen visa for Paris, and a
 * Dubai resident does not need one for Muscat. This narrows the charge to trips
 * that genuinely cross a visa boundary.
 */
object VisaPolicy {

    private val freeMovementBlocs: List<Set<String>> = listOf(
        // EU / Schengen and close neighbours with reciprocal visa-free travel.
        setOf(
            "France", "Germany", "Netherlands", "Spain", "Portugal", "Italy", "Greece",
            "Austria", "Czechia", "Hungary", "Poland", "Croatia", "Slovenia", "Romania",
            "Bulgaria", "Denmark", "Sweden", "Norway", "Finland", "Switzerland", "Ireland",
            "Iceland",
        ),
        // Gulf Cooperation Council.
        setOf("UAE", "Qatar", "Oman", "Saudi Arabia", "Kuwait", "Bahrain"),
        // ASEAN.
        setOf(
            "Thailand", "Malaysia", "Singapore", "Indonesia", "Philippines", "Vietnam",
            "Cambodia", "Laos", "Myanmar",
        ),
        // Trans-Tasman.
        setOf("Australia", "New Zealand"),
    )

    /** The UK sits outside Schengen but is visa-free both ways with it. */
    private val ukVisaFreeWith = freeMovementBlocs[0] + setOf("UK")

    /** Cheap electronic authorisations rather than a full visa. */
    private val lightAuthorisation = mapOf(
        setOf("USA", "Canada") to 21,
        setOf("India", "Nepal") to 0,
        setOf("India", "Bhutan") to 0,
    )

    /**
     * Returns the per-person entry cost in USD for this pairing, or null when
     * the destination's own catalog figure should be used as-is.
     */
    fun overrideCostUsd(originCountry: String, destCountry: String): Int? {
        if (originCountry.equals(destCountry, ignoreCase = true)) return 0

        lightAuthorisation.forEach { (pair, cost) ->
            if (originCountry in pair && destCountry in pair) return cost
        }

        if (originCountry in ukVisaFreeWith && destCountry in ukVisaFreeWith) return 0

        freeMovementBlocs.forEach { bloc ->
            if (originCountry in bloc && destCountry in bloc) return 0
        }

        return null
    }

    fun isDomestic(originCountry: String, destCountry: String): Boolean =
        originCountry.equals(destCountry, ignoreCase = true)
}
