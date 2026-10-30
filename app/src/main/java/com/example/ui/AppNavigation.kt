package com.example.ui

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.CalculatorEvent
import com.example.MainViewModel
import kotlinx.coroutines.delay

@Composable
fun AppNavigation(viewModel: MainViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "splash") {
        composable("splash") {
            val state by viewModel.state.collectAsStateWithLifecycle()

            LaunchedEffect(state.isLoadingCurrencies) {
                // When loading finishes, we transition to the initial tariff search screen
                if (!state.isLoadingCurrencies) {
                    delay(1500)
                    if (navController.currentDestination?.route == "splash") {
                        navController.navigate("tariff_search") {
                            popUpTo("splash") { inclusive = true }
                        }
                    }
                }
            }

            // Fallback in case loading is fast or already complete
            LaunchedEffect(Unit) {
                delay(3000)
                if (navController.currentDestination?.route == "splash") {
                    navController.navigate("tariff_search") {
                        popUpTo("splash") { inclusive = true }
                    }
                }
            }

            SplashScreen()
        }

        composable("tariff_search") {
            TariffSearchScreen(
                viewModel = viewModel,
                onNavigateToCalculator = {
                    navController.navigate("calculator")
                }
            )
        }

        composable("calculator") {
            CalculatorScreen(
                viewModel = viewModel,
                onNavigateToResult = {
                    navController.navigate("result")
                },
                onNavigateToSearch = {
                    navController.navigate("tariff_search")
                }
            )
        }

        composable("result") {
            val result by viewModel.calculationResult.collectAsStateWithLifecycle()
            val state by viewModel.state.collectAsStateWithLifecycle()
            val context = LocalContext.current
            
            if (result != null) {
                ResultScreen(
                    result = result!!,
                    applyDiscount = state.applyDiscount,
                    onDiscountToggle = { viewModel.onEvent(CalculatorEvent.OnApplyDiscountToggle) },
                    onNavigateBack = { navController.popBackStack() },
                    onShare = { shareText ->
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }
                        context.startActivity(Intent.createChooser(intent, "اشتراک گذاری نتایج"))
                    },
                    currentFont = state.fontFamilyChoice,
                    currentScale = state.fontSizeScale,
                    onFontChange = { viewModel.onEvent(CalculatorEvent.OnFontFamilyChange(it)) },
                    onScaleChange = { viewModel.onEvent(CalculatorEvent.OnFontSizeChange(it)) }
                )
            }
        }
    }
}
