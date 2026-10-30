cat << 'INNER_EOF' > app/src/main/java/com/example/ui/CalculatorScreen.kt
package com.example.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.CalculatorEvent
import com.example.MainViewModel
import com.example.data.CurrencyEntity
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalculatorScreen(
    viewModel: MainViewModel,
    onNavigateToResult: () -> Unit
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currencies by viewModel.currencies.collectAsStateWithLifecycle()
    val result by viewModel.calculationResult.collectAsStateWithLifecycle()
    val box042Items = viewModel.box042Items
    
    var showAboutDialog by remember { mutableStateOf(false) }

    if (showAboutDialog) {
        val context = LocalContext.current
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("درباره") },
            text = { 
                Text("برنامه نویس حیدر حیدری\nبرای سفارش برنامه نویسی های خود با ما تماس بگیرید.")
            },
            confirmButton = {
                TextButton(onClick = {
                    showAboutDialog = false
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://t.me/h31dar"))
                    context.startActivity(intent)
                }) {
                    Text("تماس")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("لغو")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("محاسبه‌گر گمرکی") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                ),
                actions = {
                    val sysTheme = isSystemInDarkTheme()
                    TextButton(onClick = { 
                        val current = state.isDarkMode ?: sysTheme
                        viewModel.onEvent(CalculatorEvent.OnThemeToggle(!current)) 
                    }) {
                        Text("تغییر تم")
                    }
                    TextButton(onClick = { showAboutDialog = true }) {
                        Text("درباره")
                    }
                    TextButton(onClick = { viewModel.onEvent(CalculatorEvent.OnReset) }) {
                        Text("ریست")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = state.weight,
                        onValueChange = { viewModel.onEvent(CalculatorEvent.OnWeightChange(it)) },
                        label = { Text("وزن خالص") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = state.amount,
                        onValueChange = { viewModel.onEvent(CalculatorEvent.OnAmountChange(it)) },
                        label = { Text("ارزش کل") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CurrencyDropdown(
                        currencies = currencies,
                        selectedCode = state.selectedCurrencyCode,
                        onSelected = { viewModel.onEvent(CalculatorEvent.OnCurrencySelected(it)) },
                        isLoading = state.isLoadingCurrencies,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = state.manualRate,
                        onValueChange = { viewModel.onEvent(CalculatorEvent.OnRateChange(it)) },
                        label = { Text("نرخ ارز") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = state.tariffPct,
                        onValueChange = { viewModel.onEvent(CalculatorEvent.OnTariffChange(it)) },
                        label = { Text("حقوق ورودی (%)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = state.manualInsurance,
                        onValueChange = { viewModel.onEvent(CalculatorEvent.OnManualInsuranceChange(it)) },
                        label = { Text("بیمه (اختیاری)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Checkbox(
                            checked = state.chkStandard,
                            onCheckedChange = { viewModel.onEvent(CalculatorEvent.OnStandardTaxToggle) }
                        )
                        Text("عوارض استاندارد")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Checkbox(
                            checked = state.chkFoodDrug,
                            onCheckedChange = { viewModel.onEvent(CalculatorEvent.OnFoodDrugTaxToggle) }
                        )
                        Text("غذا و دارو")
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("تجمیع عوارض باکس ۰۴۲", fontWeight = FontWeight.Bold)
                    val chunkedItems = box042Items.chunked(3)
                    chunkedItems.forEach { rowItems ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            rowItems.forEach { item ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { viewModel.onEvent(CalculatorEvent.OnBox042ItemToggle(item.name)) }
                                ) {
                                    Checkbox(
                                        checked = item.isChecked,
                                        onCheckedChange = { viewModel.onEvent(CalculatorEvent.OnBox042ItemToggle(item.name)) },
                                        modifier = Modifier.scale(0.8f)
                                    )
                                    Text(item.name, style = MaterialTheme.typography.bodySmall, maxLines = 1)
                                }
                            }
                            if (rowItems.size < 3) {
                                repeat(3 - rowItems.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { 
                            viewModel.onEvent(CalculatorEvent.OnCalculate("azad")) 
                            onNavigateToResult()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("محاسبه آزاد")
                    }
                    Button(
                        onClick = { 
                            viewModel.onEvent(CalculatorEvent.OnCalculate("taavoni")) 
                            onNavigateToResult()
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Text("محاسبه تعاونی")
                    }
                }
            }
        }
    }
}

@Composable
fun CurrencyDropdown(
    currencies: List<CurrencyEntity>,
    selectedCode: String?,
    onSelected: (String) -> Unit,
    isLoading: Boolean,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    
    val selectedText = if (isLoading) {
        "در حال دریافت ارزها..."
    } else {
        currencies.find { it.code == selectedCode }?.name ?: selectedCode ?: "انتخاب ارز"
    }

    // Trigger dialog
    OutlinedTextField(
        value = selectedText,
        onValueChange = {},
        readOnly = true,
        label = { Text("نوع ارز") },
        modifier = modifier.clickable { 
            if (!isLoading && currencies.isNotEmpty()) {
                showDialog = true 
            }
        },
        enabled = false, // disabled to capture clicks via clickable
        colors = OutlinedTextFieldDefaults.colors(
            disabledTextColor = MaterialTheme.colorScheme.onSurface,
            disabledBorderColor = MaterialTheme.colorScheme.outline,
            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    )

    if (showDialog) {
        Dialog(onDismissRequest = { 
            showDialog = false 
            searchQuery = ""
        }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.8f),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("جستجو...") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                        shape = RoundedCornerShape(12.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))

                    val priorityCodes = listOf("USD", "EUR", "AED", "CNY", "TRY", "GBP")
                    
                    val filteredAndSortedCurrencies = remember(currencies, searchQuery) {
                        val filtered = currencies.filter {
                            it.name.contains(searchQuery, ignoreCase = true) || 
                            it.code.contains(searchQuery, ignoreCase = true)
                        }
                        
                        // Sort so that priority ones come first
                        filtered.sortedBy { curr ->
                            val index = priorityCodes.indexOf(curr.code)
                            if (index != -1) index else priorityCodes.size
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredAndSortedCurrencies) { currency ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        onSelected(currency.code)
                                        showDialog = false
                                        searchQuery = ""
                                    }
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(currency.name)
                                Text("${formatNumber(currency.rate.toLong())} ریال")
                            }
                            HorizontalDivider()
                        }
                        
                        if (filteredAndSortedCurrencies.isEmpty()) {
                            item {
                                Text("نتیجه‌ای یافت نشد", color = Color.Gray, modifier = Modifier.padding(8.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ResultCard(
    result: com.example.logic.CalculationResult,
    applyDiscount: Boolean,
    onDiscountToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "نتایج محاسبات (${if (result.mode == "taavoni") "تعاونی" else "آزاد"})",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("بهای تمام‌شده هر کیلوگرم خالص", color = MaterialTheme.colorScheme.onPrimary)
                    Text(
                        text = "${formatNumber(result.costPerKg)} ریال",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            DetailRow("بیمه حمل", result.insurance)
            DetailRow("ارزش کل ریالی (سیف)", result.cifValue)
            DetailRow("حقوق ورودی", result.tariff)
            DetailRow("ارزش افزوده (۱۰٪)", result.vat)
            DetailRow("مالیات ۲٪ علی‌الحساب", result.tax4)
            DetailRow("عوارض استاندارد", result.standard)
            DetailRow("غذا و دارو", result.foodDrug)

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("تجمیع عوارض باکس ۰۴۲", fontWeight = FontWeight.Bold)
                        Text("${formatNumber(result.box042Total)} ریال")
                    }
                    if (result.box042Chips.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        result.box042Chips.forEach { chip ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(chip.name, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                                Text(formatNumber(chip.cost), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            if (result.mode == "taavoni") {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("باکس ۰۴۹ (تخفیف)", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            Text("- ${formatNumber(result.box049)} ریال", color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }
                    }
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                        .clickable { onDiscountToggle() }
                        .padding(8.dp)
                ) {
                    Checkbox(
                        checked = applyDiscount,
                        onCheckedChange = { onDiscountToggle() }
                    )
                    Text(
                        "اعمال تخفیف ۵۰٪ حقوق ورودی",
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            HorizontalDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (result.mode == "taavoni") "جمع پرداختی (تعاونی)" else "جمع کل پرداختی",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    "${formatNumber(result.totalPayment)} ریال",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun DetailRow(title: String, value: Long) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("${formatNumber(value)} ریال", fontWeight = FontWeight.Medium)
    }
}

fun formatNumber(number: Long): String {
    return NumberFormat.getNumberInstance(Locale.US).format(number)
}
INNER_EOF
