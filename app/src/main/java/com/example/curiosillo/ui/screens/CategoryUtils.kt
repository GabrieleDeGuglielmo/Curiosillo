package com.example.curiosillo.ui.screens

import androidx.compose.ui.graphics.Color

val LISTA_CATEGORIE = listOf(
    "Scienza", "Storia", "Animali", "Tecnologia",
    "Cibo", "Sport", "Arte", "Geografia", "Natura", 
    "Musica", "Cinema", "Letteratura", "Lingua", "Moda", 
    "Vita quotidiana"
).sorted()

fun emojiCategoria(categoria: String): String = when (categoria.lowercase()) {
    "scienza"         -> "🔬"
    "animali"         -> "🐾"
    "storia"          -> "📜"
    "sport"           -> "⚽"
    "arte"            -> "🎨"
    "tecnologia"      -> "💻"
    "natura"          -> "🌿"
    "cibo"            -> "🍽️"
    "geografia"       -> "🌍"
    "musica"          -> "🎵"
    "cinema"          -> "🎬"
    "letteratura"     -> "📖"
    "lingua"          -> "💬"
    "moda"            -> "👗"
    "vita quotidiana" -> "🏠"
    else              -> "✨"
}

fun coloreCategoria(categoria: String): Color = when (categoria.lowercase()) {
    "scienza"         -> Color(0xFF1565C0)
    "animali"         -> Color(0xFF2E7D32)
    "storia"          -> Color(0xFF6A1B9A)
    "sport"           -> Color(0xFFE65100)
    "arte"            -> Color(0xFFAD1457)
    "tecnologia"      -> Color(0xFF00838F)
    "natura"          -> Color(0xFF558B2F)
    "cibo"            -> Color(0xFFEF6C00)
    "geografia"       -> Color(0xFF00695C)
    "musica"          -> Color(0xFF4527A0)
    "cinema"          -> Color(0xFF283593)
    "letteratura"     -> Color(0xFF4E342E)
    "lingua"          -> Color(0xFF0277BD)
    "moda"            -> Color(0xFFC2185B)
    "vita quotidiana" -> Color(0xFF37474F)
    else              -> Color(0xFF455A64)
}
