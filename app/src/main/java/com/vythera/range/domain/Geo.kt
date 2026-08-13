package com.vythera.range.domain

import com.vythera.range.data.model.Place
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val EARTH_RADIUS_KM = 6371.0

private fun rad(deg: Double) = deg * Math.PI / 180.0

fun haversineKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val dLat = rad(lat2 - lat1)
    val dLon = rad(lon2 - lon1)
    val a = sin(dLat / 2) * sin(dLat / 2) +
        cos(rad(lat1)) * cos(rad(lat2)) * sin(dLon / 2) * sin(dLon / 2)
    return 2 * EARTH_RADIUS_KM * asin(sqrt(a.coerceIn(0.0, 1.0)))
}

/** Initial great-circle bearing from A to B, in degrees clockwise from north. */
fun bearingDeg(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val dLon = rad(lon2 - lon1)
    val y = sin(dLon) * cos(rad(lat2))
    val x = cos(rad(lat1)) * sin(rad(lat2)) - sin(rad(lat1)) * cos(rad(lat2)) * cos(dLon)
    val deg = Math.toDegrees(atan2(y, x))
    return (deg + 360.0) % 360.0
}

fun distanceKm(from: Place, toLat: Double, toLon: Double): Double =
    haversineKm(from.lat, from.lon, toLat, toLon)

/**
 * Road/rail distance is always longer than the great circle. This is the rough
 * detour factor Range applies for surface travel.
 */
fun surfaceKm(greatCircleKm: Double): Double = greatCircleKm * 1.28

fun compassLabel(bearing: Double): String {
    val points = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
    val idx = ((bearing + 22.5) / 45.0).toInt() % 8
    return points[abs(idx)]
}
