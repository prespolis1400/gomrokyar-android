package com.example.util

import java.util.Calendar
import java.util.Locale

object DateTimeHelper {

    data class JalaliDate(
        val year: Int,
        val month: Int,
        val day: Int
    ) {
        val monthName: String
            get() = PERSIAN_MONTH_NAMES.getOrElse(month - 1) { "" }

        fun toFormattedString(usePersianDigits: Boolean = true): String {
            val y = "%04d".format(Locale.US, year)
            val m = "%02d".format(Locale.US, month)
            val d = "%02d".format(Locale.US, day)
            val str = "$y/$m/$d"
            return if (usePersianDigits) toPersianDigits(str) else str
        }

        fun toFullTextString(): String {
            return "${toPersianDigits(day.toString())} $monthName ${toPersianDigits(year.toString())}"
        }
    }

    private val PERSIAN_MONTH_NAMES = arrayOf(
        "فروردین", "اردیبهشت", "خرداد",
        "تیر", "مرداد", "شهریور",
        "مهر", "آبان", "آذر",
        "دی", "بهمن", "اسفند"
    )

    private val PERSIAN_WEEK_DAYS = arrayOf(
        "یکشنبه", "دوشنبه", "سه‌شنبه", "چهارشنبه", "پنجشنبه", "جمعه", "شنبه"
    )

    fun getNowShamsiDateTimeString(
        calendar: Calendar = Calendar.getInstance(),
        includeDayOfWeek: Boolean = true,
        includeSeconds: Boolean = true
    ): String {
        val jalali = getNowJalali(calendar)
        val dayOfWeekIdx = calendar.get(Calendar.DAY_OF_WEEK) - 1
        val dayName = PERSIAN_WEEK_DAYS.getOrElse(dayOfWeekIdx) { "" }
        val h = calendar.get(Calendar.HOUR_OF_DAY)
        val m = calendar.get(Calendar.MINUTE)
        val s = calendar.get(Calendar.SECOND)
        val timeStr = if (includeSeconds) {
            "%02d:%02d:%02d".format(Locale.US, h, m, s)
        } else {
            "%02d:%02d".format(Locale.US, h, m)
        }
        val persianTime = toPersianDigits(timeStr)
        val dateText = jalali.toFullTextString()
        return if (includeDayOfWeek && dayName.isNotEmpty()) {
            "$dayName، $dateText - ساعت $persianTime"
        } else {
            "$dateText - ساعت $persianTime"
        }
    }

    fun toPersianDigits(input: String): String {
        val persianChars = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')
        val sb = StringBuilder()
        for (ch in input) {
            if (ch in '0'..'9') {
                sb.append(persianChars[ch - '0'])
            } else {
                sb.append(ch)
            }
        }
        return sb.toString()
    }

    /**
     * Converts a Gregorian date (year, month 1-12, day 1-31) to Jalali (Solar Hijri / شمسی).
     */
    fun gregorianToJalali(gy: Int, gm: Int, gd: Int): JalaliDate {
        val gDaysInMonth = intArrayOf(31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        val jDaysInMonth = intArrayOf(31, 31, 31, 31, 31, 31, 30, 30, 30, 30, 30, 29)

        val gy2 = gy - 1600
        val gm2 = gm - 1
        val gd2 = gd - 1

        var gDayNo = 365 * gy2 + (gy2 + 3) / 4 - (gy2 + 99) / 100 + (gy2 + 399) / 400
        for (i in 0 until gm2) {
            gDayNo += gDaysInMonth[i]
        }
        if (gm2 > 1 && ((gy % 4 == 0 && gy % 100 != 0) || (gy % 400 == 0))) {
            gDayNo++
        }
        gDayNo += gd2

        var jDayNo = gDayNo - 79
        val jNp = jDayNo / 12053
        jDayNo %= 12053

        var jy = 979 + 33 * jNp + 4 * (jDayNo / 1461)
        jDayNo %= 1461

        if (jDayNo >= 366) {
            jy += (jDayNo - 1) / 365
            jDayNo = (jDayNo - 1) % 365
        }

        var jm = 0
        for (i in 0..11) {
            val days = if (i == 11 && isJalaliLeapYear(jy)) 30 else jDaysInMonth[i]
            if (jDayNo < days) {
                jm = i + 1
                break
            }
            jDayNo -= days
        }
        val jd = jDayNo + 1
        return JalaliDate(jy, jm, jd)
    }

    private fun isJalaliLeapYear(jy: Int): Boolean {
        val rem = (jy + 38) * 31 % 128
        return rem < 31
    }

    fun getNowJalali(calendar: Calendar = Calendar.getInstance()): JalaliDate {
        val gy = calendar.get(Calendar.YEAR)
        val gm = calendar.get(Calendar.MONTH) + 1
        val gd = calendar.get(Calendar.DAY_OF_MONTH)
        return gregorianToJalali(gy, gm, gd)
    }

    fun getNowGregorianFormatted(calendar: Calendar = Calendar.getInstance()): String {
        val gy = calendar.get(Calendar.YEAR)
        val gm = calendar.get(Calendar.MONTH) + 1
        val gd = calendar.get(Calendar.DAY_OF_MONTH)
        return "%04d/%02d/%02d".format(Locale.US, gy, gm, gd)
    }

    fun getNowTimeFormatted(calendar: Calendar = Calendar.getInstance(), usePersianDigits: Boolean = true): String {
        val h = calendar.get(Calendar.HOUR_OF_DAY)
        val m = calendar.get(Calendar.MINUTE)
        val timeStr = "%02d:%02d".format(Locale.US, h, m)
        return if (usePersianDigits) toPersianDigits(timeStr) else timeStr
    }

    fun getDualDateStringForPdf(calendar: Calendar = Calendar.getInstance()): String {
        val jalali = getNowJalali(calendar).toFormattedString(usePersianDigits = true)
        val greg = getNowGregorianFormatted(calendar)
        val time = getNowTimeFormatted(calendar, usePersianDigits = true)
        return "تاریخ: $jalali (شمسی)  |  $greg (میلادی)  -  ساعت $time"
    }

    fun getDualDateStringForImage(calendar: Calendar = Calendar.getInstance()): String {
        val jalali = getNowJalali(calendar).toFormattedString(usePersianDigits = true)
        val greg = getNowGregorianFormatted(calendar)
        val time = getNowTimeFormatted(calendar, usePersianDigits = true)
        return "$jalali (شمسی)  •  $greg (میلادی)  •  $time"
    }

    fun getDualDateStringForShareText(calendar: Calendar = Calendar.getInstance()): String {
        val jalali = getNowJalali(calendar).toFormattedString(usePersianDigits = true)
        val jalaliFull = getNowJalali(calendar).toFullTextString()
        val greg = getNowGregorianFormatted(calendar)
        val time = getNowTimeFormatted(calendar, usePersianDigits = true)
        return "📅 تاریخ: $jalali (شمسی - $jalaliFull) | $greg (میلادی)\n⏰ ساعت: $time"
    }
}
