package com.vythera.range.data

import androidx.compose.ui.graphics.Color
import com.vythera.range.data.model.Vibe
import kotlin.math.absoluteValue

/**
 * Destination artwork is generated, not downloaded — every card gets a two-stop
 * wash derived from its dominant vibe, nudged by a hash of its id so no two
 * neighbours look identical. Keeps the app fully offline and visually coherent.
 */
private val ramps: Map<Vibe, List<Pair<Long, Long>>> = mapOf(
    Vibe.BEACH to listOf(0xFF0EA5A5 to 0xFF0B4F8A, 0xFF12B7C6 to 0xFF0E5FA8, 0xFF17C4A0 to 0xFF0A6C8F),
    Vibe.ISLAND to listOf(0xFF19D3C5 to 0xFF0B6FA0, 0xFF2BD6A6 to 0xFF0D5C93),
    Vibe.MOUNTAIN to listOf(0xFF4C6FD8 to 0xFF17255C, 0xFF3F7FD4 to 0xFF16204F, 0xFF5D7BE0 to 0xFF1B2B63),
    Vibe.SNOW to listOf(0xFF6EC5F5 to 0xFF1C3D75, 0xFF8FD6F7 to 0xFF23508C),
    Vibe.DESERT to listOf(0xFFE8A34A to 0xFF8A3E1E, 0xFFF0B455 to 0xFF7A3520),
    Vibe.HERITAGE to listOf(0xFFD98A5A to 0xFF6B2F45, 0xFFC97A6B to 0xFF5B2A4F, 0xFFE0A05E to 0xFF74304A),
    Vibe.CITY to listOf(0xFF7A5AF0 to 0xFF1E1A57, 0xFF6366F1 to 0xFF1B1B4B, 0xFF8B5CF6 to 0xFF23175A),
    Vibe.NIGHTLIFE to listOf(0xFFEC4899 to 0xFF3B0A56, 0xFFD946A0 to 0xFF33104F),
    Vibe.NATURE to listOf(0xFF2FBF71 to 0xFF124A3D, 0xFF37C98A to 0xFF0F4438),
    Vibe.WILDLIFE to listOf(0xFFB08A3E to 0xFF2F4A22, 0xFF9AA33C to 0xFF27401F),
    Vibe.FOOD to listOf(0xFFE2703A to 0xFF5C2233, 0xFFEE8352 to 0xFF52202F),
    Vibe.SPIRITUAL to listOf(0xFFE0B24A to 0xFF5C2E5E, 0xFFD9A03C to 0xFF4C2A5A),
    Vibe.ADVENTURE to listOf(0xFF25C2E0 to 0xFF14406B, 0xFF1FB3D6 to 0xFF123A63),
    Vibe.SHOPPING to listOf(0xFF9D7BF5 to 0xFF2A1B5E, 0xFFA88BF7 to 0xFF2E1F63),
    Vibe.ROADTRIP to listOf(0xFF5FB0E8 to 0xFF1D3A66, 0xFF6FBCEE to 0xFF20406F),
)

private val fallback = listOf(0xFF3D7BE0 to 0xFF152A55)

fun paletteFor(id: String, vibes: Set<Vibe>): List<Color> {
    val dominant = Vibe.entries.firstOrNull { it in vibes } ?: Vibe.CITY
    val options = ramps[dominant] ?: fallback
    val pick = options[(id.hashCode().absoluteValue) % options.size]
    return listOf(Color(pick.first), Color(pick.second))
}
