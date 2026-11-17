package com.example

import org.junit.Assert.*
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testGregorianToJalali() {
    // 2024-03-20 is 1403/01/01 (leap boundary) or 1402/12/29
    val d1 = com.example.util.DateTimeHelper.gregorianToJalali(2024, 3, 20)
    assertTrue(d1.year in 1402..1403)

    // 2024-10-27 is 1403/08/06
    val d2 = com.example.util.DateTimeHelper.gregorianToJalali(2024, 10, 27)
    assertEquals(1403, d2.year)
    assertEquals(8, d2.month)
    assertEquals(6, d2.day)
    assertEquals("آبان", d2.monthName)

    val str = d2.toFormattedString(usePersianDigits = false)
    assertEquals("1403/08/06", str)

    val persianStr = d2.toFormattedString(usePersianDigits = true)
    assertEquals("۱۴۰۳/۰۸/۰۶", persianStr)
  }

  @Test
  fun testTaavoniVatCalculation() {
    // Test Taavoni VAT without 50% discount: VAT should be 10% of CIF value only
    val inputWithoutDiscount = com.example.logic.CalculationInput(
      mode = "taavoni",
      weight = 1000.0,
      amount = 1000.0,
      rate = 500000.0, // 500,000,000 IRR
      tariffPct = 10.0, // 10% tariff
      manualIns = 0L,
      chkStandard = false,
      chkFoodDrug = false,
      activeBox042 = emptyList(),
      applyDiscount = false
    )
    val resultWithoutDiscount = com.example.logic.CalculationEngine.calculate(inputWithoutDiscount)
    val expectedVatWithout = Math.round(resultWithoutDiscount.cifValue * com.example.logic.Constants.VAT)
    assertEquals(expectedVatWithout, resultWithoutDiscount.vat)

    // Test Taavoni VAT with 50% discount: VAT should be 10% of (CIF + half of tariff)
    val inputWithDiscount = inputWithoutDiscount.copy(applyDiscount = true)
    val resultWithDiscount = com.example.logic.CalculationEngine.calculate(inputWithDiscount)
    val halfTariff = Math.round(resultWithDiscount.tariff / 2.0)
    val expectedVatWith = Math.round((resultWithDiscount.cifValue + halfTariff) * com.example.logic.Constants.VAT)
    assertEquals(expectedVatWith, resultWithDiscount.vat)
  }

  @Test
  fun testInsuranceHalfPerThousandRate() {
    val amount = 1000.0
    val rate = 500000.0 // 500,000,000 IRR
    val input = com.example.logic.CalculationInput(
      mode = "azad",
      weight = 500.0,
      amount = amount,
      rate = rate,
      tariffPct = 5.0,
      manualIns = 0L,
      chkStandard = false,
      chkFoodDrug = false,
      activeBox042 = emptyList(),
      applyDiscount = false
    )
    val result = com.example.logic.CalculationEngine.calculate(input)
    // 0.5 per 1000 (0.0005) of total amount * rate
    val expectedInsurance = Math.round(amount * 0.0005 * rate)
    assertEquals(expectedInsurance, result.insurance)
  }

  @Test
  fun testTaavoniRedCrescentAndBox049() {
    val redCrescentItem = com.example.logic.Box042Item("هلال احمر", "tariff", 0.01, isChecked = true)
    val inputBase = com.example.logic.CalculationInput(
      mode = "taavoni",
      weight = 1000.0,
      amount = 1000.0,
      rate = 500000.0,
      tariffPct = 10.0,
      manualIns = 0L,
      chkStandard = false,
      chkFoodDrug = false,
      activeBox042 = listOf(redCrescentItem),
      applyDiscount = true // With 50% discount
    )

    // 1. With 50% discount: Red Crescent is 1% of FULL tariff, and NOT in Box 049 (Box 049 is only halfTariff)
    val resultWithDiscount = com.example.logic.CalculationEngine.calculate(inputBase)
    val halfTariff = Math.round(resultWithDiscount.tariff / 2.0)
    val expectedRedCrescentWithDiscount = Math.round(resultWithDiscount.tariff * 0.01)
    val actualRedCrescentWithDiscount = resultWithDiscount.box042Chips.find { it.name.contains("هلال احمر") }?.cost
    assertEquals(expectedRedCrescentWithDiscount, actualRedCrescentWithDiscount)
    assertEquals(halfTariff, resultWithDiscount.box049)

    // 2. Without 50% discount (applyDiscount = false): Red Crescent is completely removed from calculations
    val inputWithoutDiscount = inputBase.copy(applyDiscount = false)
    val resultWithoutDiscount = com.example.logic.CalculationEngine.calculate(inputWithoutDiscount)
    val actualRedCrescentWithoutDiscount = resultWithoutDiscount.box042Chips.find { it.name.contains("هلال احمر") }
    assertNull(actualRedCrescentWithoutDiscount)
    assertEquals(0L, resultWithoutDiscount.redCrescentAmount)
    assertEquals(resultWithoutDiscount.tariff, resultWithoutDiscount.box049)
  }

  @Test
  fun testPenaltyBox047NotInVatButAddedToTotalPayment() {
    val penaltyAmount = 15_000_000L

    // Azad mode without vs with penalty
    val baseAzad = com.example.logic.CalculationInput(
      mode = "azad",
      weight = 1000.0,
      amount = 1000.0,
      rate = 500000.0,
      tariffPct = 10.0,
      manualIns = 0L,
      chkStandard = false,
      chkFoodDrug = false,
      activeBox042 = emptyList(),
      applyDiscount = false,
      manualPenalty = 0L
    )
    val resAzadNoPenalty = com.example.logic.CalculationEngine.calculate(baseAzad)
    val resAzadWithPenalty = com.example.logic.CalculationEngine.calculate(baseAzad.copy(manualPenalty = penaltyAmount))

    assertEquals(penaltyAmount, resAzadWithPenalty.box047)
    assertEquals(resAzadNoPenalty.vat, resAzadWithPenalty.vat)
    assertEquals(resAzadNoPenalty.totalPayment + penaltyAmount, resAzadWithPenalty.totalPayment)

    // Taavoni mode (with discount) without vs with penalty
    val baseTaavoni = baseAzad.copy(mode = "taavoni", applyDiscount = true)
    val resTaavoniNoPenalty = com.example.logic.CalculationEngine.calculate(baseTaavoni)
    val resTaavoniWithPenalty = com.example.logic.CalculationEngine.calculate(baseTaavoni.copy(manualPenalty = penaltyAmount))

    assertEquals(penaltyAmount, resTaavoniWithPenalty.box047)
    assertEquals(resTaavoniNoPenalty.vat, resTaavoniWithPenalty.vat)
    assertEquals(resTaavoniNoPenalty.totalPayment + penaltyAmount, resTaavoniWithPenalty.totalPayment)
  }
}
