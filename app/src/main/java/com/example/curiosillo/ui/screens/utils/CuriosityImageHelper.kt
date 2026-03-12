package com.example.curiosillo.ui.screens.utils

import androidx.annotation.DrawableRes
import com.example.curiosillo.R

@DrawableRes
fun categoryImage(category: String): Int = when (category.lowercase()) {
    "scienza"  -> R.drawable.curiosity_scienza
    "animali"  -> R.drawable.curiosity_animali
    "storia"   -> R.drawable.curiosity_storia
    "sport"    -> R.drawable.curiosity_sport
    "arte"     -> R.drawable.curiosity_arte
    "tecnologia" -> R.drawable.curiosity_tecnologia
    "natura"   -> R.drawable.curiosity_natura
    "cibo"     -> R.drawable.curiosity_cibo
    "geografia" -> R.drawable.curiosity_geografia
    "cinema" -> R.drawable.curiosity_cinema
    "letteratura" -> R.drawable.curiosity_letteratura
    "musica" -> R.drawable.curiosity_musica
    "lingua" -> R.drawable.curiosity_lingua
    "moda" -> R.drawable.curiosity_moda
    "vita quotidiana" -> R.drawable.curiosity_vq
    else       -> R.drawable.curiosity
}
