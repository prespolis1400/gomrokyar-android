sed -i 's/IconButton(onClick = { showAboutDialog = false })/TextButton(onClick = { showAboutDialog = false })/g' app/src/main/java/com/example/ui/CalculatorScreen.kt
sed -i 's/IconButton(onClick = { showAboutDialog = true })/TextButton(onClick = { showAboutDialog = true })/g' app/src/main/java/com/example/ui/CalculatorScreen.kt
sed -i 's/IconButton(onClick = { viewModel.onEvent(CalculatorEvent.OnReset) })/TextButton(onClick = { viewModel.onEvent(CalculatorEvent.OnReset) })/g' app/src/main/java/com/example/ui/CalculatorScreen.kt
