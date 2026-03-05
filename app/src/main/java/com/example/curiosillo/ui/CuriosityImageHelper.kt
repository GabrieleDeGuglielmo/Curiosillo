package com.example.curiosillo.ui

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
    else       -> R.drawable.curiosity_cibo //TODO CAMBIARE CON DEFAULT
}
