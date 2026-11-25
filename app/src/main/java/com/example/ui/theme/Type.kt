package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.R

val NotoSansArabicFamily = FontFamily(
    Font(R.font.noto_sans_arabic, FontWeight.Normal),
    Font(R.font.noto_sans_arabic, FontWeight.Medium),
    Font(R.font.noto_sans_arabic, FontWeight.SemiBold),
    Font(R.font.noto_sans_arabic, FontWeight.Bold)
)

val NotoNastaliqUrduFamily = FontFamily(
    Font(R.font.noto_nastaliq_urdu, FontWeight.Normal),
    Font(R.font.noto_nastaliq_urdu, FontWeight.Bold)
)

val AppTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = NotoNastaliqUrduFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 46.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = NotoSansArabicFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 34.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = NotoSansArabicFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 30.sp
    ),
    titleLarge = TextStyle(
        fontFamily = NotoSansArabicFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 26.sp
    ),
    titleMedium = TextStyle(
        fontFamily = NotoSansArabicFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 24.sp
    ),
    titleSmall = TextStyle(
        fontFamily = NotoSansArabicFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = NotoSansArabicFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 23.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = NotoSansArabicFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 20.sp
    ),
    bodySmall = TextStyle(
        fontFamily = NotoSansArabicFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 18.sp
    ),
    labelLarge = TextStyle(
        fontFamily = NotoSansArabicFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelMedium = TextStyle(
        fontFamily = NotoSansArabicFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp
    ),
    labelSmall = TextStyle(
        fontFamily = NotoSansArabicFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 15.sp
    )
)
