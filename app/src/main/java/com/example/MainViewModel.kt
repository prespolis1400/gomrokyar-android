package com.example

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.CurrencyEntity
import com.example.data.CurrencyRepository
import com.example.logic.Box042Item
import com.example.logic.Box042Items
import com.example.logic.CalculationEngine
import com.example.logic.CalculationInput
import com.example.logic.CalculationResult
import com.example.data.NetworkClient
import com.example.data.tariff.TariffItemDto
import com.example.data.tariff.TariffRepository
import com.example.util.DateTimeHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CalculatorState(
    val weight: String = "",
    val amount: String = "",
    val selectedCurrencyCode: String? = null,
    val manualRate: String = "",
    val tariffPct: String = "5",
    val manualInsurance: String = "",
    val chkStandard: Boolean = true,
    val chkFoodDrug: Boolean = true,
    val applyDiscount: Boolean = false,
    val isLoadingCurrencies: Boolean = false,
    val isDarkMode: Boolean? = null,
    val fontFamilyChoice: String = "Vazirmatn",
    val fontSizeScale: Float = 1.0f,
    // Tariff search state
    val tariffSearchQuery: String = "",
    val isSearchingTariff: Boolean = false,
    val tariffResults: List<TariffItemDto> = emptyList(),
    val tariffSearchError: String? = null,
    val hasSearchedTariff: Boolean = false,
    val selectedTariffCode: String? = null,
    val selectedTariffTitle: String? = null,
    val tariffSearchTimestamp: String = DateTimeHelper.getNowShamsiDateTimeString()
)

class MainViewModel(
    private val repository: CurrencyRepository,
    private val tariffRepository: TariffRepository
) : ViewModel() {
    val currencies: StateFlow<List<CurrencyEntity>> = repository.allCurrencies
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private val _state = MutableStateFlow(CalculatorState())
    val state = _state.asStateFlow()

    private val _calculationResult = MutableStateFlow<CalculationResult?>(null)
    val calculationResult = _calculationResult.asStateFlow()

    val box042Items = mutableStateListOf<Box042Item>()

    init {
        box042Items.addAll(Box042Items.items.map { it.copy() })
        
        // Preload offline tariffs
        viewModelScope.launch {
            tariffRepository.preloadIfNeeded()
            val initial = tariffRepository.getInitialItems(25)
            if (_state.value.tariffResults.isEmpty() && !_state.value.hasSearchedTariff) {
                _state.update { it.copy(tariffResults = initial) }
            }
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoadingCurrencies = true) }
            repository.refreshCurrenciesIfNeeded()
            _state.update { it.copy(isLoadingCurrencies = false) }
        }

        viewModelScope.launch {
            currencies.collect { list ->
                if (list.isNotEmpty()) {
                    if (_state.value.isLoadingCurrencies) {
                        _state.update { it.copy(isLoadingCurrencies = false) }
                    }
                    if (_state.value.selectedCurrencyCode == null) {
                        val usd = list.find { it.code == "USD" }
                        if (usd != null) {
                            _state.update { 
                                it.copy(
                                    selectedCurrencyCode = "USD",
                                    manualRate = usd.rate.toLong().toString()
                                )
                            }
                        } else {
                            val first = list.first()
                            _state.update { 
                                it.copy(
                                    selectedCurrencyCode = first.code,
                                    manualRate = first.rate.toLong().toString()
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    fun onEvent(event: CalculatorEvent) {
        when (event) {
            is CalculatorEvent.OnWeightChange -> updateState { copy(weight = event.weight) }
            is CalculatorEvent.OnAmountChange -> updateState { copy(amount = event.amount) }
            is CalculatorEvent.OnCurrencySelected -> {
                val currency = currencies.value.find { it.code == event.code }
                updateState { 
                     copy(
                        selectedCurrencyCode = event.code,
                        manualRate = currency?.rate?.toLong()?.toString() ?: manualRate
                    )
                }
            }
            is CalculatorEvent.OnRateChange -> updateState { copy(manualRate = event.rate) }
            is CalculatorEvent.OnTariffChange -> updateState { copy(tariffPct = event.tariff) }
            is CalculatorEvent.OnManualInsuranceChange -> updateState { copy(manualInsurance = event.insurance) }
            is CalculatorEvent.OnStandardTaxToggle -> updateState { copy(chkStandard = !chkStandard) }
            is CalculatorEvent.OnFoodDrugTaxToggle -> updateState { copy(chkFoodDrug = !chkFoodDrug) }
            is CalculatorEvent.OnApplyDiscountToggle -> {
                updateState { copy(applyDiscount = !applyDiscount) }
                // Recalculate if result exists
                if (_calculationResult.value?.mode == "taavoni") {
                    calculate("taavoni")
                }
            }
            is CalculatorEvent.OnBox042ItemToggle -> {
                val index = box042Items.indexOfFirst { it.name == event.name }
                if (index != -1) {
                    box042Items[index] = box042Items[index].copy(isChecked = !box042Items[index].isChecked)
                }
            }
            CalculatorEvent.OnReset -> reset()
            is CalculatorEvent.OnTariffSearchQueryChange -> onTariffSearchQueryChanged(event.query)
            is CalculatorEvent.OnSearchTariff -> searchTariff(event.query ?: state.value.tariffSearchQuery)
            is CalculatorEvent.OnSelectTariffItem -> {
                val duty = event.item.importDuty
                val formattedDuty = if (duty != null) {
                    if (duty % 1.0 == 0.0) duty.toInt().toString() else duty.toString()
                } else {
                    _state.value.tariffPct
                }
                updateState {
                    copy(
                        tariffPct = formattedDuty,
                        selectedTariffCode = event.item.tariffCode,
                        selectedTariffTitle = event.item.tariffTitle
                    )
                }
            }
            CalculatorEvent.OnClearTariffSearch -> clearTariffSearch()
            is CalculatorEvent.OnThemeToggle -> updateState { copy(isDarkMode = event.isDark) }
            is CalculatorEvent.OnFontFamilyChange -> updateState { copy(fontFamilyChoice = event.font) }
            is CalculatorEvent.OnFontSizeChange -> updateState { copy(fontSizeScale = event.scale) }
            CalculatorEvent.OnRefreshCurrencies -> {
                viewModelScope.launch {
                    _state.update { it.copy(isLoadingCurrencies = true) }
                    repository.forceRefreshCurrencies()
                    _state.update { it.copy(isLoadingCurrencies = false) }
                }
            }
            CalculatorEvent.OnClearResult -> _calculationResult.value = null
            is CalculatorEvent.OnCalculate -> calculate(event.mode)
        }
        
        // Hide result on input change except non-input changes and discount toggle
        if (event !is CalculatorEvent.OnCalculate && 
            event !is CalculatorEvent.OnApplyDiscountToggle &&
            event !is CalculatorEvent.OnThemeToggle &&
            event !is CalculatorEvent.OnFontFamilyChange &&
            event !is CalculatorEvent.OnFontSizeChange &&
            event !is CalculatorEvent.OnRefreshCurrencies &&
            event !is CalculatorEvent.OnTariffSearchQueryChange &&
            event !is CalculatorEvent.OnSearchTariff &&
            event !is CalculatorEvent.OnClearTariffSearch) {
            _calculationResult.value = null
        }
    }

    private fun updateState(update: CalculatorState.() -> CalculatorState) {
        _state.update(update)
    }

    private var tariffSearchJob: Job? = null

    private fun onTariffSearchQueryChanged(query: String) {
        _state.update { it.copy(tariffSearchQuery = query) }
        tariffSearchJob?.cancel()
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            tariffSearchJob = viewModelScope.launch {
                val initial = tariffRepository.getInitialItems(25)
                _state.update {
                    it.copy(
                        isSearchingTariff = false,
                        tariffResults = initial,
                        tariffSearchError = null,
                        hasSearchedTariff = false,
                        tariffSearchTimestamp = DateTimeHelper.getNowShamsiDateTimeString()
                    )
                }
            }
        } else {
            tariffSearchJob = viewModelScope.launch {
                delay(80)
                _state.update {
                    it.copy(
                        isSearchingTariff = true,
                        tariffSearchError = null,
                        hasSearchedTariff = true
                    )
                }
                try {
                    val items = tariffRepository.search(trimmed, limit = 80)
                    _state.update {
                        it.copy(
                            isSearchingTariff = false,
                            tariffResults = items,
                            tariffSearchError = if (items.isEmpty()) "هیچ موردی با عبارت «$trimmed» در تعرفه‌های سال ۱۴۰۵ یافت نشد" else null,
                            tariffSearchTimestamp = DateTimeHelper.getNowShamsiDateTimeString()
                        )
                    }
                } catch (e: Exception) {
                    _state.update {
                        it.copy(
                            isSearchingTariff = false,
                            tariffSearchError = "خطا در جستجوی فایل تعرفه: ${e.message}",
                            tariffSearchTimestamp = DateTimeHelper.getNowShamsiDateTimeString()
                        )
                    }
                }
            }
        }
    }

    private fun clearTariffSearch() {
        tariffSearchJob?.cancel()
        viewModelScope.launch {
            val initial = tariffRepository.getInitialItems(25)
            _state.update {
                it.copy(
                    tariffSearchQuery = "",
                    tariffResults = initial,
                    tariffSearchError = null,
                    hasSearchedTariff = false,
                    tariffSearchTimestamp = DateTimeHelper.getNowShamsiDateTimeString()
                )
            }
        }
    }

    fun searchTariff(query: String) {
        tariffSearchJob?.cancel()
        val trimmed = query.trim()
        viewModelScope.launch {
            _state.update {
                it.copy(
                    tariffSearchQuery = trimmed,
                    isSearchingTariff = true,
                    tariffSearchError = null,
                    hasSearchedTariff = trimmed.isNotEmpty()
                )
            }
            try {
                val items = if (trimmed.isEmpty()) {
                    tariffRepository.getInitialItems(30)
                } else {
                    tariffRepository.search(trimmed, limit = 80)
                }
                _state.update {
                    it.copy(
                        isSearchingTariff = false,
                        tariffResults = items,
                        tariffSearchError = if (items.isEmpty()) "هیچ موردی با عبارت «$trimmed» در تعرفه‌های سال ۱۴۰۵ یافت نشد" else null,
                        tariffSearchTimestamp = DateTimeHelper.getNowShamsiDateTimeString()
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isSearchingTariff = false,
                        tariffSearchError = "خطا در جستجوی فایل تعرفه: ${e.message}",
                        tariffSearchTimestamp = DateTimeHelper.getNowShamsiDateTimeString()
                    )
                }
            }
        }
    }

    private fun calculate(mode: String) {
        val s = state.value
        val weightVal = s.weight.replace(",", "").toDoubleOrNull() ?: 0.0
        val amountVal = s.amount.replace(",", "").toDoubleOrNull() ?: 0.0
        val rateVal = s.manualRate.replace(",", "").toDoubleOrNull() ?: 0.0
        val tariffVal = s.tariffPct.replace(",", "").toDoubleOrNull() ?: 0.0
        val manualInsVal = s.manualInsurance.replace(",", "").toLongOrNull() ?: 0L

        if (weightVal == 0.0 || amountVal == 0.0 || rateVal == 0.0) {
            return // Need these basic values
        }

        val input = CalculationInput(
            mode = mode,
            weight = weightVal,
            amount = amountVal,
            rate = rateVal,
            tariffPct = tariffVal,
            manualIns = manualInsVal,
            chkStandard = s.chkStandard,
            chkFoodDrug = s.chkFoodDrug,
            activeBox042 = box042Items.filter { it.isChecked },
            applyDiscount = s.applyDiscount
        )

        _calculationResult.value = CalculationEngine.calculate(input)
    }

    private fun reset() {
        val currentDark = _state.value.isDarkMode
        val currentFont = _state.value.fontFamilyChoice
        val currentFontSize = _state.value.fontSizeScale

        // Create new state preserving theme, font family, font size, and setting isLoadingCurrencies = false
        _state.value = CalculatorState(
            isDarkMode = currentDark,
            fontFamilyChoice = currentFont,
            fontSizeScale = currentFontSize,
            isLoadingCurrencies = false
        )
        _calculationResult.value = null
        box042Items.clear()
        box042Items.addAll(Box042Items.items.map { it.copy() })
        
        // Restore currency and rate from loaded currencies
        val list = currencies.value
        if (list.isNotEmpty()) {
            val usd = list.find { it.code == "USD" }
            if (usd != null) {
                _state.update { 
                    it.copy(
                        selectedCurrencyCode = "USD",
                        manualRate = usd.rate.toLong().toString()
                    )
                }
            } else {
                val first = list.first()
                _state.update { 
                    it.copy(
                        selectedCurrencyCode = first.code,
                        manualRate = first.rate.toLong().toString()
                    )
                }
            }
        }
    }
}

sealed class CalculatorEvent {
    data class OnWeightChange(val weight: String) : CalculatorEvent()
    data class OnAmountChange(val amount: String) : CalculatorEvent()
    data class OnCurrencySelected(val code: String) : CalculatorEvent()
    data class OnRateChange(val rate: String) : CalculatorEvent()
    data class OnTariffChange(val tariff: String) : CalculatorEvent()
    data class OnManualInsuranceChange(val insurance: String) : CalculatorEvent()
    object OnStandardTaxToggle : CalculatorEvent()
    object OnFoodDrugTaxToggle : CalculatorEvent()
    object OnApplyDiscountToggle : CalculatorEvent()
    data class OnBox042ItemToggle(val name: String) : CalculatorEvent()
    object OnReset : CalculatorEvent()
    object OnClearResult : CalculatorEvent()
    data class OnCalculate(val mode: String) : CalculatorEvent()
    data class OnThemeToggle(val isDark: Boolean) : CalculatorEvent()
    data class OnFontFamilyChange(val font: String) : CalculatorEvent()
    data class OnFontSizeChange(val scale: Float) : CalculatorEvent()
    object OnRefreshCurrencies : CalculatorEvent()
    data class OnTariffSearchQueryChange(val query: String) : CalculatorEvent()
    data class OnSearchTariff(val query: String? = null) : CalculatorEvent()
    data class OnSelectTariffItem(val item: TariffItemDto) : CalculatorEvent()
    object OnClearTariffSearch : CalculatorEvent()
}

class MainViewModelFactory(
    private val repository: CurrencyRepository,
    private val tariffRepository: TariffRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository, tariffRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
