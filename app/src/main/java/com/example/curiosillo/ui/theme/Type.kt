package com.example.curiosillo.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.curiosillo.R

// Default Material 3 typography styles
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    )
)

val Nunito = FontFamily(
    Font(R.font.nunito_regular, FontWeight.Normal),
    Font(R.font.nunito_semibold, FontWeight.SemiBold),
    Font(R.font.nunito_bold, FontWeight.Bold),
    Font(R.font.nunito_extrabold, FontWeight.ExtraBold) // Fondamentale per il titolo!
)

// Type.kt

private val defaultNunitoStyle = TextStyle(
    fontFamily = Nunito,
    color      = TestoScuro
)

val AppTypography = Typography(
    // Titoli delle MenuCard
    titleMedium = defaultNunitoStyle.copy(
        fontSize   = 18.sp,
        fontWeight = FontWeight.Bold
    ),

    // Sottotitoli delle MenuCard
    bodySmall = defaultNunitoStyle.copy(
        fontSize   = 14.sp,
        fontWeight = FontWeight.Normal,
        color      = TestoScuro.copy(alpha = 0.8f)
    ),

    // Corpo delle pillole (18px / 1.5 interlinea come richiesto)
    bodyLarge = defaultNunitoStyle.copy(
        fontSize   = 18.sp,
        lineHeight = 27.sp,
        fontWeight = FontWeight.Normal
    ),

    // Titolo principale "Curiosillo"
    displayMedium = defaultNunitoStyle.copy(
        fontSize   = 45.sp,
        fontWeight = FontWeight.ExtraBold
    ),
    headlineLarge = defaultNunitoStyle.copy(
        fontSize   = 32.sp,
        fontWeight = FontWeight.ExtraBold
    ),

    // Etichette barra in basso
    labelSmall = defaultNunitoStyle.copy(
        fontSize   = 12.sp,
        fontWeight = FontWeight.Medium
    )
)