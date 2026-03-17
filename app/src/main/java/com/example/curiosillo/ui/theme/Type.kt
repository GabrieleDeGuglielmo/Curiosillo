package com.example.curiosillo.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.curiosillo.R

val Nunito = FontFamily(
    Font(R.font.nunito_regular, FontWeight.Normal),
    Font(R.font.nunito_medium, FontWeight.Medium),
    Font(R.font.nunito_semibold, FontWeight.SemiBold),
    Font(R.font.nunito_bold, FontWeight.Bold),
    Font(R.font.nunito_extrabold, FontWeight.ExtraBold)
)

private val defaultTextStyle = TextStyle(
    fontFamily = Nunito,
    color      = DarkText,
    platformStyle = PlatformTextStyle(includeFontPadding = false)
)

val AppTypography = Typography(
    displayLarge = defaultTextStyle.copy(fontSize = 57.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 64.sp),
    displayMedium = defaultTextStyle.copy(fontSize = 45.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 52.sp),
    displaySmall = defaultTextStyle.copy(fontSize = 36.sp, fontWeight = FontWeight.Bold, lineHeight = 44.sp),

    // Headlines: 22sp - 28sp
    headlineLarge = defaultTextStyle.copy(fontSize = 28.sp, fontWeight = FontWeight.Bold, lineHeight = 40.sp),
    headlineMedium = defaultTextStyle.copy(fontSize = 26.sp, fontWeight = FontWeight.Bold, lineHeight = 36.sp),
    headlineSmall = defaultTextStyle.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold, lineHeight = 34.sp),

    titleLarge = defaultTextStyle.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold, lineHeight = 30.sp),
    titleMedium = defaultTextStyle.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold, lineHeight = 26.sp),
    titleSmall = defaultTextStyle.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium, lineHeight = 20.sp),

    // Body: 16sp - 18sp. Line Height: ~150%
    bodyLarge = defaultTextStyle.copy(fontSize = 18.sp, lineHeight = 27.sp, fontWeight = FontWeight.Normal),
    bodyMedium = defaultTextStyle.copy(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Normal),
    bodySmall = defaultTextStyle.copy(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Normal),

    // Captions/Buttons: 12sp - 14sp
    labelLarge = defaultTextStyle.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp),
    labelMedium = defaultTextStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium, lineHeight = 16.sp),
    labelSmall = defaultTextStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium, lineHeight = 16.sp)
)