package com.vythera.range.domain

import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Range works in USD internally and presents in whatever the traveller thinks in.
 * Rates are static, shipped-with-the-app approximations — enough for planning,
 * and it keeps the whole app usable with no network at all.
 */
/**
 * Live rates, when we have them. [Currency.perUsd] stays as the shipped
 * fallback so the app is fully functional offline and on first launch.
 */
object Rates {
    @Volatile
    private var overrides: Map<String, Double> = emptyMap()

    @Volatile
    var updatedAtMs: Long = 0L
        private set

    val isLive: Boolean get() = overrides.isNotEmpty()

    fun apply(rates: Map<String, Double>, fetchedAtMs: Long) {
        overrides = rates
        updatedAtMs = fetchedAtMs
    }

    fun perUsd(currency: Currency): Double = overrides[currency.code] ?: currency.perUsd
}

/** The rate actually in force: live if we have it, shipped otherwise. */
val Currency.rate: Double get() = Rates.perUsd(this)

enum class Currency(
    val code: String,
    val symbol: String,
    val perUsd: Double,
    val step: Int,
) {
    INR("INR", "₹", 87.0, 5_000),
    USD("USD", "$", 1.0, 50),
    EUR("EUR", "€", 0.92, 50),
    GBP("GBP", "£", 0.78, 50),
    AED("AED", "AED ", 3.67, 200),
    SGD("SGD", "S$", 1.34, 100),
    AUD("AUD", "A$", 1.52, 100),
    CAD("CAD", "C$", 1.37, 100),
    ;

    fun fromUsd(usd: Double): Double = usd * rate
    fun toUsd(local: Double): Double = local / rate
}

private fun groupIndian(value: Long): String {
    val s = value.toString()
    if (s.length <= 3) return s
    val head = s.dropLast(3)
    val tail = s.takeLast(3)
    val chunks = mutableListOf<String>()
    var rest = head
    while (rest.length > 2) {
        chunks.add(0, rest.takeLast(2))
        rest = rest.dropLast(2)
    }
    if (rest.isNotEmpty()) chunks.add(0, rest)
    return chunks.joinToString(",") + "," + tail
}

private fun groupWestern(value: Long): String {
    val s = value.toString()
    val sb = StringBuilder()
    for ((i, c) in s.withIndex()) {
        if (i > 0 && (s.length - i) % 3 == 0) sb.append(',')
        sb.append(c)
    }
    return sb.toString()
}

fun formatMoney(usd: Double, currency: Currency, showSymbol: Boolean = true): String {
    val local = currency.fromUsd(usd).roundToLong()
    val digits = if (currency == Currency.INR) groupIndian(abs(local)) else groupWestern(abs(local))
    val sign = if (local < 0) "-" else ""
    return if (showSymbol) "$sign${currency.symbol}$digits" else "$sign$digits"
}

/** Short form for dials and chips: ₹1.2L, ₹85k, $1.2k. */
fun formatMoneyCompact(usd: Double, currency: Currency): String {
    val local = currency.fromUsd(usd)
    val sym = currency.symbol
    fun trim(v: Double): String {
        val r = (v * 10).roundToLong() / 10.0
        return if (abs(r - r.toLong()) < 0.05) r.toLong().toString() else r.toString()
    }
    return when {
        currency == Currency.INR && local >= 10_000_000 -> "$sym${trim(local / 10_000_000)}Cr"
        currency == Currency.INR && local >= 100_000 -> "$sym${trim(local / 100_000)}L"
        local >= 1_000_000 -> "$sym${trim(local / 1_000_000)}M"
        local >= 1_000 -> "$sym${trim(local / 1_000)}k"
        else -> "$sym${local.roundToLong()}"
    }
}

fun formatKm(km: Double): String = when {
    km >= 1000 -> "${(km / 100).roundToLong() / 10.0}k km"
    else -> "${km.roundToLong()} km"
}

fun formatHours(hours: Double): String {
    if (hours <= 0.01) return "—"
    val h = hours.toInt()
    val m = ((hours - h) * 60).roundToLong()
    return when {
        h >= 24 -> "${h / 24}d ${h % 24}h"
        h == 0 -> "${m}m"
        m == 0L -> "${h}h"
        else -> "${h}h ${m}m"
    }
}
