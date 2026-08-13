package com.vythera.range.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.vythera.range.data.model.SavedTrip
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "range_prefs")

private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

data class RangeSettings(
    val originId: String = "del",
    val currency: String = "INR",
    val themeMode: String = "DARK",
    val dynamicColor: Boolean = false,
    val reduceMotion: Boolean = false,
    val hapticsEnabled: Boolean = true,
    val onboarded: Boolean = false,
)

/** Small, boring persistence layer — settings plus the traveller's saved trips. */
class RangeStore(private val context: Context) {

    private object Keys {
        val ORIGIN = stringPreferencesKey("origin")
        val CURRENCY = stringPreferencesKey("currency")
        val DYNAMIC = booleanPreferencesKey("dynamic_color")
        val REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
        val HAPTICS = booleanPreferencesKey("haptics")
        val ONBOARDED = booleanPreferencesKey("onboarded")
        val THEME = stringPreferencesKey("theme_mode")
        val TRIPS = stringPreferencesKey("saved_trips")
        val WISHLIST = stringPreferencesKey("wishlist")
    }

    val settings: Flow<RangeSettings> = context.dataStore.data.map { p ->
        RangeSettings(
            originId = p[Keys.ORIGIN] ?: "del",
            currency = p[Keys.CURRENCY] ?: "INR",
            themeMode = p[Keys.THEME] ?: "DARK",
            dynamicColor = p[Keys.DYNAMIC] ?: false,
            reduceMotion = p[Keys.REDUCE_MOTION] ?: false,
            hapticsEnabled = p[Keys.HAPTICS] ?: true,
            onboarded = p[Keys.ONBOARDED] ?: false,
        )
    }

    val savedTrips: Flow<List<SavedTrip>> = context.dataStore.data.map { p ->
        decodeTrips(p[Keys.TRIPS])
    }

    val wishlist: Flow<Set<String>> = context.dataStore.data.map { p ->
        (p[Keys.WISHLIST] ?: "").split(',').filter { it.isNotBlank() }.toSet()
    }

    private fun decodeTrips(raw: String?): List<SavedTrip> = runCatching {
        if (raw.isNullOrBlank()) emptyList() else json.decodeFromString<List<SavedTrip>>(raw)
    }.getOrDefault(emptyList())

    suspend fun setOrigin(id: String) = context.dataStore.edit { it[Keys.ORIGIN] = id }
    suspend fun setCurrency(code: String) = context.dataStore.edit { it[Keys.CURRENCY] = code }
    suspend fun setThemeMode(mode: String) = context.dataStore.edit { it[Keys.THEME] = mode }
    suspend fun setDynamicColor(on: Boolean) = context.dataStore.edit { it[Keys.DYNAMIC] = on }
    suspend fun setReduceMotion(on: Boolean) = context.dataStore.edit { it[Keys.REDUCE_MOTION] = on }
    suspend fun setHaptics(on: Boolean) = context.dataStore.edit { it[Keys.HAPTICS] = on }
    suspend fun setOnboarded(done: Boolean) = context.dataStore.edit { it[Keys.ONBOARDED] = done }

    suspend fun toggleWishlist(destinationId: String) = context.dataStore.edit { p ->
        val current = (p[Keys.WISHLIST] ?: "").split(',').filter { it.isNotBlank() }.toMutableSet()
        if (!current.add(destinationId)) current.remove(destinationId)
        p[Keys.WISHLIST] = current.joinToString(",")
    }

    suspend fun saveTrip(trip: SavedTrip) = context.dataStore.edit { p ->
        val current = decodeTrips(p[Keys.TRIPS]).filterNot { it.id == trip.id }
        p[Keys.TRIPS] = json.encodeToString(listOf(trip) + current)
    }

    suspend fun deleteTrip(id: String) = context.dataStore.edit { p ->
        val current = decodeTrips(p[Keys.TRIPS]).filterNot { it.id == id }
        p[Keys.TRIPS] = json.encodeToString(current)
    }
}
