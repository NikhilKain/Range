@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.layout.ExperimentalLayoutApi::class,
)

package com.vythera.range.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vythera.range.data.OriginCatalog
import com.vythera.range.domain.Currency
import com.vythera.range.ui.components.AuroraBackground
import com.vythera.range.ui.components.Motion
import com.vythera.range.ui.components.RangeChip
import com.vythera.range.ui.components.RangeMark
import com.vythera.range.ui.components.RangeWordmark
import com.vythera.range.ui.theme.PillShape
import com.vythera.range.ui.theme.RangePalette
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    originId: String,
    currency: Currency,
    onOrigin: (String) -> Unit,
    onCurrency: (Currency) -> Unit,
    onDone: () -> Unit,
) {
    val pager = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScopeSafe()
    val markProgress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        markProgress.animateTo(1f, tween(1400))
    }

    AuroraBackground(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(28.dp)) {
            Spacer(Modifier.height(40.dp))
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(180.dp),
                contentAlignment = Alignment.Center,
            ) {
                RangeMark(
                    Modifier
                        .size(150.dp)
                        .scale(0.85f + 0.15f * markProgress.value),
                    progress = markProgress.value,
                )
            }
            Spacer(Modifier.height(8.dp))
            RangeWordmark(Modifier.align(Alignment.CenterHorizontally))

            HorizontalPager(
                state = pager,
                modifier = Modifier.weight(1f),
            ) { page ->
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(top = 36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    when (page) {
                        0 -> Pane(
                            title = "Start with the money",
                            body = "Most travel apps ask where you're going. Range asks what you " +
                                "can spend — then shows you everywhere that actually fits.",
                        )

                        1 -> Pane(
                            title = "The whole trip, not just the flight",
                            body = "Flights, trains, buses, cabs or your own car. Plus stay, food, " +
                                "getting around, things to do, visas and a buffer — priced for " +
                                "your group, at your comfort level.",
                        )

                        2 -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Pane(
                                title = "Where are you starting?",
                                body = "Every distance and price is calculated from here.",
                            )
                            Spacer(Modifier.height(20.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                OriginCatalog.all.take(10).forEach { place ->
                                    RangeChip(
                                        label = place.city,
                                        selected = place.id == originId,
                                        onClick = { onOrigin(place.id) },
                                    )
                                }
                            }
                            Spacer(Modifier.height(18.dp))
                            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(Currency.INR, Currency.USD, Currency.EUR, Currency.GBP)
                                    .forEach { c ->
                                        RangeChip(
                                            label = c.code,
                                            selected = c == currency,
                                            onClick = { onCurrency(c) },
                                            accent = RangePalette.Sky,
                                        )
                                    }
                            }
                        }
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(3) { i ->
                    val active = pager.currentPage == i
                    val w by animateFloatAsState(if (active) 22f else 7f, Motion.snappy, label = "dot")
                    Box(
                        Modifier
                            .padding(horizontal = 3.dp)
                            .width(w.dp)
                            .height(7.dp)
                            .clip(PillShape)
                            .background(
                                if (active) RangePalette.Aurora else RangePalette.InkLine,
                            ),
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Box(
                Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .clip(PillShape)
                    .background(
                        Brush.horizontalGradient(
                            listOf(RangePalette.Aurora, RangePalette.Lagoon, RangePalette.Sky),
                        ),
                    )
                    .clickable {
                        if (pager.currentPage < 2) {
                            scope.launch { pager.animateScrollToPage(pager.currentPage + 1) }
                        } else {
                            onDone()
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    if (pager.currentPage < 2) "Next" else "Find my range",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF04121B),
                    fontWeight = FontWeight.W800,
                )
            }

            Spacer(Modifier.height(10.dp))
            Text(
                "Skip",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clickable(onClick = onDone)
                    .padding(8.dp),
            )
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun Pane(title: String, body: String) {
    AnimatedVisibility(
        visible = true,
        enter = fadeIn(tween(500)) + slideInVertically { it / 4 },
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                title,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                body,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun rememberCoroutineScopeSafe() = androidx.compose.runtime.rememberCoroutineScope()
