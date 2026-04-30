package com.example.curiosillo.data

import androidx.annotation.DrawableRes
import androidx.compose.runtime.Immutable
import com.example.curiosillo.R

@Immutable
data class Avatar(
    val id: String,
    val livelloRichiesto: Int,
    @DrawableRes val drawableRes: Int
)

object AvatarCatalogo {
    val lista = listOf(
        Avatar("uovo", 1, R.drawable.avatar_uovo),
        Avatar("topo", 3, R.drawable.avatar_topo),
        Avatar("esploratore", 5, R.drawable.avatar_esploratore_uomo),
        Avatar("scienziato", 7, R.drawable.avatar_scienziato_uomo)
    )

    fun trovaPerId(id: String): Avatar = 
        lista.find { it.id == id } ?: lista.first()
}
