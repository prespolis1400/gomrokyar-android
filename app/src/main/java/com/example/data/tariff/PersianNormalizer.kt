package com.example.data.tariff

object PersianNormalizer {
    private val arabicDigits = "٠١٢٣٤٥٦٧٨٩"
    private val persianDigits = "۰۱۲۳۴۵۶۷۸۹"

    fun normalize(input: String?): String {
        if (input.isNullOrBlank()) return ""

        val sb = java.lang.StringBuilder(input.length)
        for (i in input.indices) {
            val c = input[i]
            when (c) {
                // Arabic Yeh variants
                'ي', 'ى' -> sb.append('ی')
                // Arabic Kaf
                'ك' -> sb.append('ک')
                // Tatweel / Kashida, ZWNJ, Soft Hyphen, ZWJ
                'ـ', '\u200C', '\u200D', '\u00AD' -> sb.append(' ')
                // Dashes, underscores, bullets
                '-', '_', '•', '·', '―' -> sb.append(' ')
                // Arabic Diacritics (Fatha, Damma, Kasra, Tanween, Tashdeed, Sukun, etc.)
                in '\u064B'..'\u065F', '\u0670' -> { /* Skip diacritics */ }
                // Digits conversion
                in arabicDigits -> {
                    val idx = arabicDigits.indexOf(c)
                    sb.append(('0' + idx))
                }
                in persianDigits -> {
                    val idx = persianDigits.indexOf(c)
                    sb.append(('0' + idx))
                }
                else -> {
                    sb.append(c.lowercaseChar())
                }
            }
        }

        // Collapse multiple spaces
        return sb.toString().replace(Regex("\\s+"), " ").trim()
    }
}
