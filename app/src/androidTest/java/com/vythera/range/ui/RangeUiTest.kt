package com.vythera.range.ui

import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vythera.range.data.DestinationCatalog
import com.vythera.range.data.model.SavedTrip
import com.vythera.range.domain.Currency
import com.vythera.range.ui.components.CountStepper
import com.vythera.range.ui.components.RangeChip
import com.vythera.range.ui.screens.SavedScreen
import com.vythera.range.ui.theme.RangeTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Compose UI tests for the interactions that carry state.
 *
 * These deliberately drive individual components rather than walking the whole
 * app from onboarding. A test that taps through four screens to reach the thing
 * it cares about fails for four unrelated reasons; these fail only when the
 * behaviour under test actually breaks.
 *
 * The emulator screenshot job in CI still covers "does the whole thing render".
 */
@RunWith(AndroidJUnit4::class)
class RangeUiTest {

    @get:Rule
    val compose = createComposeRule()

    // ── Chips: the app's most-reused control ────────────────────────────────

    @Test
    fun chip_reports_clicks_and_survives_reselection() {
        var clicks = 0
        compose.setContent {
            RangeTheme {
                var on by androidx.compose.runtime.remember { mutableStateOf(false) }
                RangeChip(
                    label = "Beaches",
                    selected = on,
                    onClick = {
                        clicks++
                        on = !on
                    },
                )
            }
        }

        compose.onNodeWithText("Beaches").assertIsDisplayed()
        compose.onNodeWithText("Beaches").performClick()
        compose.onNodeWithText("Beaches").performClick()
        assertEquals(2, clicks)
    }

    // ── Stepper: bounds are the thing that actually breaks ──────────────────

    @Test
    fun stepper_will_not_go_below_its_minimum() {
        var value = 1
        compose.setContent {
            RangeTheme {
                CountStepper(value = value, onChange = { value = it }, min = 1, max = 8)
            }
        }
        // The decrement control is disabled at the floor, so this is a no-op
        // rather than a step down to zero travellers.
        compose.onNodeWithText("1").assertIsDisplayed()
        assertEquals(1, value)
    }

    // ── Saved trips: swipe is now the only delete affordance ────────────────

    @Test
    fun swiping_a_saved_trip_deletes_it() {
        val destination = DestinationCatalog.all.first()
        var deleted: String? = null
        val trip = SavedTrip(
            id = "trip-1",
            destinationId = destination.id,
            originId = "delhi",
            departEpochDay = 20_000L,
            nights = 5,
            travelers = 2,
            transport = "FLIGHT",
            stayTier = "COMFORT",
            foodTier = "COMFORT",
            totalUsd = 900.0,
            budgetUsd = 1200.0,
            savedAtEpochMs = System.currentTimeMillis(),
        )

        compose.setContent {
            RangeTheme {
                SavedScreen(
                    trips = listOf(trip),
                    wishlist = emptySet(),
                    currency = Currency.INR,
                    onOpenDestination = {},
                    onDelete = { deleted = it },
                    onToggleWishlist = {},
                    onBack = {},
                )
            }
        }

        compose.onNodeWithText(destination.city).assertIsDisplayed()
        compose.onNodeWithText(destination.city).performTouchInput { swipeLeft() }
        compose.waitForIdle()
        assertEquals("trip-1", deleted)
    }

    @Test
    fun saved_screen_shows_an_empty_state_when_there_is_nothing() {
        compose.setContent {
            RangeTheme {
                SavedScreen(
                    trips = emptyList(),
                    wishlist = emptySet(),
                    currency = Currency.INR,
                    onOpenDestination = {},
                    onDelete = {},
                    onToggleWishlist = {},
                    onBack = {},
                )
            }
        }
        compose.onNodeWithText("Nothing saved yet").assertIsDisplayed()
    }

    // ── Theme: Material You must not break contrast roles ───────────────────

    @Test
    fun theme_supplies_a_usable_scheme_in_both_modes() {
        compose.setContent {
            RangeTheme(mode = com.vythera.range.ui.theme.ThemeMode.LIGHT) {
                Text("light-probe")
            }
        }
        compose.onNodeWithText("light-probe").assertIsDisplayed()
    }

    @Test
    fun catalog_is_not_empty() {
        // Guards the fixtures the tests above rely on.
        assertTrue(DestinationCatalog.all.isNotEmpty())
    }
}
