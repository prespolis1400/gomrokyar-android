package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val VazirmatnFontFamily = FontFamily(
    Font(R.font.vazirmatn, FontWeight.Normal),
    Font(R.font.vazirmatn, FontWeight.Medium),
    Font(R.font.vazirmatn, FontWeight.Bold)
)

val NotoSansArabicFontFamily = FontFamily(
    Font(R.font.noto_sans_arabic, FontWeight.Normal),
    Font(R.font.noto_sans_arabic, FontWeight.Bold)
)

val LalezarFontFamily = FontFamily(
    Font(R.font.lalezar, FontWeight.Normal)
)

fun getFontFamily(choice: String): FontFamily {
    return when (choice) {
        "Vazirmatn" -> VazirmatnFontFamily
        "NotoSans" -> NotoSansArabicFontFamily
        "Lalezar" -> LalezarFontFamily
        else -> FontFamily.Default
    }
}

fun createAppTypography(fontFamily: FontFamily, scale: Float = 1.0f): Typography {
    return Typography(
        displayLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = (57 * scale).sp, lineHeight = (64 * scale).sp),
        displayMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = (45 * scale).sp, lineHeight = (52 * scale).sp),
        displaySmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = (36 * scale).sp, lineHeight = (44 * scale).sp),
        headlineLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = (32 * scale).sp, lineHeight = (40 * scale).sp),
        headlineMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = (28 * scale).sp, lineHeight = (36 * scale).sp),
        headlineSmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = (24 * scale).sp, lineHeight = (32 * scale).sp),
        titleLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = (22 * scale).sp, lineHeight = (28 * scale).sp),
        titleMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = (16 * scale).sp, lineHeight = (24 * scale).sp),
        titleSmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = (14 * scale).sp, lineHeight = (20 * scale).sp),
        bodyLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = (16 * scale).sp, lineHeight = (24 * scale).sp, letterSpacing = 0.5.sp),
        bodyMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = (14 * scale).sp, lineHeight = (20 * scale).sp, letterSpacing = 0.25.sp),
        bodySmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Normal, fontSize = (12 * scale).sp, lineHeight = (16 * scale).sp, letterSpacing = 0.4.sp),
        labelLarge = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = (14 * scale).sp, lineHeight = (20 * scale).sp, letterSpacing = 0.1.sp),
        labelMedium = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = (12 * scale).sp, lineHeight = (16 * scale).sp, letterSpacing = 0.5.sp),
        labelSmall = TextStyle(fontFamily = fontFamily, fontWeight = FontWeight.Medium, fontSize = (11 * scale).sp, lineHeight = (16 * scale).sp, letterSpacing = 0.5.sp)
    )
}

val Typography = createAppTypography(VazirmatnFontFamily, 1.0f)
