sed -i 's/import androidx.compose.material.icons.filled.WbSunny/import androidx.compose.material.icons.Icons\nimport androidx.compose.material.icons.filled.LightMode\nimport androidx.compose.material.icons.filled.DarkMode/' app/src/main/java/com/example/ui/CalculatorScreen.kt
sed -i 's/import androidx.compose.material.icons.filled.Nightlight//' app/src/main/java/com/example/ui/CalculatorScreen.kt
sed -i 's/androidx.compose.material.icons.filled.WbSunny/Icons.Default.LightMode/g' app/src/main/java/com/example/ui/CalculatorScreen.kt
sed -i 's/androidx.compose.material.icons.filled.Nightlight/Icons.Default.DarkMode/g' app/src/main/java/com/example/ui/CalculatorScreen.kt
