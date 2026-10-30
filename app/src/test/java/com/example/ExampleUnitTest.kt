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
}
