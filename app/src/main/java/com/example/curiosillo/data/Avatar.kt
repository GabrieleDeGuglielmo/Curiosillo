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
        Avatar("0", 1, R.drawable.avatar_0),
        Avatar("1", 2, R.drawable.avatar_1),
        Avatar("2", 5, R.drawable.avatar_2),
        Avatar("3", 7, R.drawable.avatar_3),
        Avatar("4", 8, R.drawable.avatar_4),
        Avatar("5", 9, R.drawable.avatar_5),
        Avatar("6", 10, R.drawable.avatar_6),
    )

    fun trovaPerId(id: String): Avatar = 
        lista.find { it.id == id } ?: lista.first()
}
