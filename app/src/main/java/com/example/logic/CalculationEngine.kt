package com.example.logic

import kotlin.math.roundToLong

object Constants {
    const val INSURANCE_DEFAULT = 0.0005
    const val STANDARD_TAX = 0.0087
    const val FOOD_DRUG_TAX = 0.002
    const val ALIOLHESAB_TAX = 0.02
    const val VAT = 0.10
}

data class Box042Item(
    val name: String,
    val type: String, // "tariff", "value", "weight"
    val rate: Double,
    var isChecked: Boolean = false
)

object Box042Items {
    val items = listOf(
        Box042Item("هلال احمر", "tariff", 0.01, isChecked = true),
        Box042Item("نهاده دامی", "value", 0.01),
        Box042Item("عوارض ۱۲ در ۱۰۰۰", "value", 0.012, isChecked = true),
        Box042Item("پسماند", "value", 0.0005),
        Box042Item("گوشت قرمز", "weight", 2750.0),
        Box042Item("گوشت مرغ", "weight", 1100.0),
        Box042Item("خودرو بنزینی", "value", 0.10),
        Box042Item("خودرو سواری", "value", 0.05),
        Box042Item("کالای آسیب‌رسان", "value", 0.10),
        Box042Item("غلات/چوب/دانه", "weight", 200.0),
        Box042Item("عوارض چای", "weight", 300.0),
        Box042Item("پنبه/کنجاله", "weight", 500.0),
        Box042Item("میوه و سبزیجات", "weight", 700.0),
        Box042Item("سم کشاورزی", "weight", 14640.0)
    )
}

data class CalculationInput(
    val mode: String, // "azad" or "taavoni"
    val weight: Double,
    val amount: Double,
    val rate: Double,
    val tariffPct: Double,
    val manualIns: Long,
    val chkStandard: Boolean,
    val chkFoodDrug: Boolean,
    val activeBox042: List<Box042Item>,
    val applyDiscount: Boolean = false // For Taavoni Box049 discount
)

data class Box042Chip(val name: String, val cost: Long)

data class Box042Result(val total: Long, val chips: List<Box042Chip>)

data class CalculationResult(
    val mode: String,
    val insurance: Long,
    val cifValue: Long,
    val tariff: Long,
    val box042Total: Long,
    val box042Chips: List<Box042Chip>,
    val vat: Long,
    val tax4: Long,
    val standard: Long,
    val foodDrug: Long,
    val totalPayment: Long,
    val costPerKg: Long,
    val manualIns: Long,
    val weight: Double,
    val basePayment: Long,
    val box049: Long,
    val redCrescentAmount: Long,
    val applyDiscount: Boolean
)

object CalculationEngine {
    
    private fun calcBox042(weight: Double, totalValue: Long, tariff: Long, activeItems: List<Box042Item>): Box042Result {
        var total = 0L
        val chips = mutableListOf<Box042Chip>()
        
        for (item in activeItems) {
            val cost = when (item.type) {
                "tariff" -> (tariff * item.rate).roundToLong()
                "value" -> (totalValue * item.rate).roundToLong()
                "weight" -> (weight * item.rate).roundToLong()
                else -> 0L
            }
            total += cost
            chips.add(Box042Chip(item.name, cost))
        }
        return Box042Result(total, chips)
    }

    fun calculate(input: CalculationInput): CalculationResult {
        val insurance = if (input.manualIns > 0) {
            input.manualIns
        } else {
            (input.amount * Constants.INSURANCE_DEFAULT * input.rate).roundToLong()
        }
        val cifValue = (input.amount * input.rate).roundToLong() + insurance + 1L
        val tariff = (cifValue * (input.tariffPct / 100.0)).roundToLong()
        val box042 = calcBox042(input.weight, cifValue, tariff, input.activeBox042)
        val tax4 = ((cifValue + tariff) * Constants.ALIOLHESAB_TAX).roundToLong()

        val standard = if (input.chkStandard) ((cifValue * Constants.STANDARD_TAX) * 1.1).roundToLong() else 0L
        val foodDrug = if (input.chkFoodDrug) (cifValue * Constants.FOOD_DRUG_TAX).roundToLong() else 0L

        val redCrescentItem = box042.chips.find { it.name.contains("هلال احمر") }
        val redCrescentAmount = redCrescentItem?.cost ?: 0L

        var box049 = 0L
        if (input.mode == "taavoni") {
            val halfTariff = (tariff / 2.0).roundToLong()
            box049 = if (input.applyDiscount) {
                halfTariff + redCrescentAmount
            } else {
                tariff + redCrescentAmount
            }
        }

        val vat = if (input.mode == "taavoni") {
            if (input.applyDiscount) {
                // با فعال کردن تیک تخفیف ۵۰ درصد: ارزش گمرکی + نصف حقوق ورودی
                val halfTariff = (tariff / 2.0).roundToLong()
                ((cifValue + halfTariff) * Constants.VAT).roundToLong()
            } else {
                // بدون فعال کردن تیک تخفیف ۵۰ درصد: فقط ارزش گمرکی
                (cifValue * Constants.VAT).roundToLong()
            }
        } else {
            ((cifValue + tariff) * Constants.VAT).roundToLong()
        }

        var totalPayment = 0L
        var costPerKg = 0L
        var basePayment = 0L

        if (input.mode == "taavoni") {
            basePayment = vat + box042.total + tariff
            
            totalPayment = basePayment - box049
            
            val totalForKg = totalPayment + tax4 + standard + foodDrug
            costPerKg = if (input.weight > 0) (totalForKg / input.weight).roundToLong() else 0L
        } else {
            totalPayment = tariff + box042.total + vat
            val totalForKg = totalPayment + tax4 + standard + foodDrug
            costPerKg = if (input.weight > 0) (totalForKg / input.weight).roundToLong() else 0L
        }

        return CalculationResult(
            mode = input.mode,
            insurance = insurance,
            cifValue = cifValue,
            tariff = tariff,
            box042Total = box042.total,
            box042Chips = box042.chips,
            vat = vat,
            tax4 = tax4,
            standard = standard,
            foodDrug = foodDrug,
            totalPayment = totalPayment,
            costPerKg = costPerKg,
            manualIns = input.manualIns,
            weight = input.weight,
            basePayment = basePayment,
            box049 = box049,
            redCrescentAmount = redCrescentAmount,
            applyDiscount = input.applyDiscount
        )
    }
}
