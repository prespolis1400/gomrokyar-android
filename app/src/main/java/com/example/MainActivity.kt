package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.data.CurrencyRepository
import com.example.data.NetworkClient
import com.example.data.tariff.TariffRepository
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.getFontFamily

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "currency-database"
        ).build()
        
        val repository = CurrencyRepository(db.currencyDao(), NetworkClient.currencyApi)
        val tariffRepository = TariffRepository(applicationContext)
        val factory = MainViewModelFactory(repository, tariffRepository)
        setContent {
            val viewModel: MainViewModel = viewModel(factory = factory)
            val state by viewModel.state.collectAsStateWithLifecycle()
            val isDarkTheme = state.isDarkMode ?: isSystemInDarkTheme()
            val fontFamily = getFontFamily(state.fontFamilyChoice)
            val fontSizeScale = state.fontSizeScale
            
            MyApplicationTheme(
                darkTheme = isDarkTheme,
                fontFamily = fontFamily,
                fontSizeScale = fontSizeScale
            ) {
                CompositionLocalProvider(androidx.compose.ui.platform.LocalLayoutDirection provides androidx.compose.ui.unit.LayoutDirection.Rtl) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        com.example.ui.AppNavigation(viewModel = viewModel)
                    }
                }
            }
        }
    }
}
