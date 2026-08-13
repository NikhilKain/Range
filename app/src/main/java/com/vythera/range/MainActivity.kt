package com.vythera.range

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.vythera.range.ui.RangeApp
import com.vythera.range.ui.state.RangeViewModel
import com.vythera.range.ui.theme.RangeTheme
import com.vythera.range.ui.theme.ThemeMode

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val vm: RangeViewModel = viewModel(factory = RangeViewModel.Factory)
            val settings by vm.settings.collectAsStateWithLifecycle()
            val mode = runCatching { ThemeMode.valueOf(settings.themeMode) }
                .getOrDefault(ThemeMode.DARK)
            RangeTheme(mode = mode, dynamicColor = settings.dynamicColor) {
                RangeApp(viewModel = vm)
            }
        }
    }
}
