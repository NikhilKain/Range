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

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val vm: RangeViewModel = viewModel(factory = RangeViewModel.Factory)
            val settings by vm.settings.collectAsStateWithLifecycle()
            // Range is a dark-first product; the midnight identity is the point.
            RangeTheme(darkTheme = true, dynamicColor = settings.dynamicColor) {
                RangeApp(viewModel = vm)
            }
        }
    }
}
