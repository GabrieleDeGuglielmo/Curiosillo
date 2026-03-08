package com.example.curiosillo.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.curiosillo.R

// Definiamo la famiglia con tutti i pesi necessari
val Nunito = FontFamily(
    Font(R.font.nunito_regular, FontWeight.Normal),
    Font(R.font.nunito_medium, FontWeight.Medium),
    Font(R.font.nunito_semibold, FontWeight.SemiBold),
    Font(R.font.nunito_bold, FontWeight.Bold),
    Font(R.font.nunito_extrabold, FontWeight.ExtraBold)
)

// Uno stile base per non ripeterci
private val defaultTextStyle = TextStyle(
    fontFamily = Nunito,
    color      = TestoScuro,
    platformStyle = PlatformTextStyle(includeFontPadding = false)
)

val AppTypography = Typography(
    // ── DISPLAY (Titoli giganti, es: "Curiosillo") ──────────────────────
    displayLarge = defaultTextStyle.copy(fontSize = 57.sp, fontWeight = FontWeight.ExtraBold),
    displayMedium = defaultTextStyle.copy(fontSize = 45.sp, fontWeight = FontWeight.ExtraBold),
    displaySmall = defaultTextStyle.copy(fontSize = 36.sp, fontWeight = FontWeight.Bold),

    // ── HEADLINE (Titoli di sezioni o dialog) ──────────────────────────
    headlineLarge = defaultTextStyle.copy(fontSize = 32.sp, fontWeight = FontWeight.Bold),
    headlineMedium = defaultTextStyle.copy(fontSize = 28.sp, fontWeight = FontWeight.Bold),
    headlineSmall = defaultTextStyle.copy(fontSize = 24.sp, fontWeight = FontWeight.Bold),

    // ── TITLE (Titoli delle Card, TopAppBar) ───────────────────────────
    titleLarge = defaultTextStyle.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold),
    titleMedium = defaultTextStyle.copy(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
    titleSmall = defaultTextStyle.copy(fontSize = 14.sp, fontWeight = FontWeight.Medium),

    // ── BODY (Il cuore dell'app: le pillole) ────────────────────────────
    bodyLarge = defaultTextStyle.copy(
        fontSize   = 18.sp,
        lineHeight = 27.sp,
        fontWeight = FontWeight.Normal
    ),
    bodyMedium = defaultTextStyle.copy(
        fontSize   = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Normal
    ),
    bodySmall = defaultTextStyle.copy(
        fontSize   = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Normal
    ),

    // ── LABEL (Tasti, etichette barra in basso, piccoli tag) ───────────
    labelLarge = defaultTextStyle.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
    labelMedium = defaultTextStyle.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium),
    labelSmall = defaultTextStyle.copy(fontSize = 11.sp, fontWeight = FontWeight.Medium)
)