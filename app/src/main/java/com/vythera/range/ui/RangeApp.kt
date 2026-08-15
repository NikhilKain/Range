@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.vythera.range.ui

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import com.vythera.range.ui.components.RangeMark
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.vythera.range.data.DestinationCatalog
import com.vythera.range.data.OriginCatalog
import com.vythera.range.ui.screens.DetailScreen
import com.vythera.range.ui.screens.ExploreScreen
import com.vythera.range.ui.screens.HomeScreen
import com.vythera.range.ui.screens.OnboardingScreen
import com.vythera.range.ui.screens.OriginScreen
import com.vythera.range.ui.screens.SavedScreen
import com.vythera.range.ui.screens.SettingsScreen
import com.vythera.range.ui.state.RangeViewModel

@Composable
private fun BootPane() {
    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        RangeMark(Modifier.size(96.dp))
    }
}

private object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val EXPLORE = "explore"
    const val DETAIL = "detail/{id}"
    const val SAVED = "saved"
    const val SETTINGS = "settings"
    const val ORIGIN = "origin"

    fun detail(id: String) = "detail/$id"
}

@Composable
fun RangeApp(
    viewModel: RangeViewModel = viewModel(factory = RangeViewModel.Factory),
    navController: NavHostController = rememberNavController(),
) {
    val query by viewModel.query.collectAsStateWithLifecycle()
    val filters by viewModel.filters.collectAsStateWithLifecycle()
    val explore by viewModel.explore.collectAsStateWithLifecycle()
    val currency by viewModel.currency.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val settingsLoaded by viewModel.settingsLoaded.collectAsStateWithLifecycle()
    val wishlist by viewModel.wishlist.collectAsStateWithLifecycle()
    val savedTrips by viewModel.savedTrips.collectAsStateWithLifecycle()
    val ratesUpdatedAt by viewModel.ratesUpdatedAt.collectAsStateWithLifecycle()
    val refreshing by viewModel.refreshing.collectAsStateWithLifecycle()
    val liveFares by viewModel.liveFares.collectAsStateWithLifecycle()
    val fetchingFares by viewModel.fetchingFares.collectAsStateWithLifecycle()

    val loaded = settingsLoaded
    if (loaded == null) {
        BootPane()
        return
    }

    // Fixed for the lifetime of the graph: swapping startDestination on a live
    // NavHost rebuilds it and can bounce the user back a screen.
    val start = rememberSaveable {
        if (loaded.onboarded) Routes.HOME else Routes.ONBOARDING
    }

    NavHost(
        navController = navController,
        startDestination = start,
        modifier = Modifier.fillMaxSize(),
        enterTransition = {
            slideInHorizontally(tween(360)) { it / 6 } + fadeIn(tween(260))
        },
        exitTransition = {
            slideOutHorizontally(tween(360)) { -it / 8 } + fadeOut(tween(200))
        },
        popEnterTransition = {
            slideInHorizontally(tween(340)) { -it / 8 } + fadeIn(tween(240))
        },
        popExitTransition = {
            slideOutHorizontally(tween(340)) { it / 6 } + fadeOut(tween(200))
        },
    ) {
        composable(
            Routes.ONBOARDING,
            exitTransition = { scaleOut(tween(400), targetScale = 1.08f) + fadeOut(tween(300)) },
        ) {
            OnboardingScreen(
                originId = query.originId,
                currency = currency,
                onOrigin = viewModel::setOriginWithCurrency,
                onCurrency = viewModel::setCurrency,
                onDone = {
                    viewModel.setOnboarded(true)
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.HOME) {
            HomeScreen(
                query = query,
                currency = currency,
                explore = explore,
                onQueryChange = viewModel::update,
                onToggleMode = viewModel::toggleMode,
                onOpenOrigin = { navController.navigate(Routes.ORIGIN) },
                onOpenSaved = { navController.navigate(Routes.SAVED) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onSearch = {
                    viewModel.commitSearch()
                    navController.navigate(Routes.EXPLORE)
                },
            )
        }

        composable(
            Routes.EXPLORE,
            enterTransition = { fadeIn(tween(380)) + scaleIn(tween(420), initialScale = 0.94f) },
        ) {
            ExploreScreen(
                state = explore,
                filters = filters,
                currency = currency,
                wishlist = wishlist,
                originCity = OriginCatalog.find(query.originId).city,
                originCountry = OriginCatalog.find(query.originId).country,
                budgetUsd = query.totalBudgetUsd,
                onFilters = viewModel::setFilters,
                onToggleRegion = viewModel::toggleRegion,
                onToggleVibe = viewModel::toggleVibe,
                onClearFilters = viewModel::clearFilters,
                onOpen = { navController.navigate(Routes.detail(it.destination.id)) },
                onWishlist = viewModel::toggleWishlist,
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.DETAIL) { backStack ->
            val id = backStack.arguments?.getString("id").orEmpty()
            val destination = DestinationCatalog.find(id)
            if (destination == null) {
                navController.popBackStack()
            } else {
                DetailScreen(
                    destination = destination,
                    query = query,
                    currency = currency,
                    wishlisted = id in wishlist,
                    onWishlist = { viewModel.toggleWishlist(id) },
                    onSaveTrip = { viewModel.saveTrip(it) },
                    onApplyQuery = viewModel::update,
                    onBack = { navController.popBackStack() },
                    liveFare = liveFares[id],
                    fareLoading = id in fetchingFares,
                    onRequestLiveFare = { viewModel.requestLiveFare(id) },
                )
            }
        }

        composable(Routes.SAVED) {
            SavedScreen(
                trips = savedTrips,
                wishlist = wishlist,
                currency = currency,
                onOpenDestination = { navController.navigate(Routes.detail(it)) },
                onDelete = viewModel::deleteTrip,
                onToggleWishlist = viewModel::toggleWishlist,
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.ORIGIN) {
            OriginScreen(
                currentId = query.originId,
                onPick = {
                    viewModel.setOrigin(it)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() },
            )
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                settings = settings,
                currency = currency,
                originCity = OriginCatalog.find(query.originId).city,
                originCountry = OriginCatalog.find(query.originId).country,
                onOpenOrigin = { navController.navigate(Routes.ORIGIN) },
                ratesUpdatedAt = ratesUpdatedAt,
                refreshing = refreshing,
                onRefreshRates = { viewModel.refreshRates(force = true) },
                onCurrency = { viewModel.setCurrency(it) },
                onThemeMode = viewModel::setThemeMode,
                onDynamicColor = viewModel::setDynamicColor,
                onReduceMotion = viewModel::setReduceMotion,
                onHaptics = viewModel::setHaptics,
                onLivePrices = { viewModel.setLivePrices(it) },
                livePricesAvailable = viewModel.liveFaresAvailable,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
