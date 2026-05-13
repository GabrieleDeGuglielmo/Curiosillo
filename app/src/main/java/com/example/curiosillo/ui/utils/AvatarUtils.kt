package com.example.curiosillo.ui.utils

import androidx.annotation.DrawableRes
import com.example.curiosillo.R

/**
 * Returns the local drawable resource ID associated with a numerical avatar ID.
 * This is used for displaying user avatars in rankings and profiles.
 *
 * @param avatarId The numerical ID of the avatar (0 to 6).
 * @return The corresponding drawable resource ID from R.drawable.
 */
@DrawableRes
fun getAvatarResourceById(avatarId: Int): Int {
    return when (avatarId) {
        0 -> R.drawable.avatar_0
        1 -> R.drawable.avatar_1
        2 -> R.drawable.avatar_2
        3 -> R.drawable.avatar_3
        4 -> R.drawable.avatar_4
        5 -> R.drawable.avatar_5
        6 -> R.drawable.avatar_6
        else -> R.drawable.avatar_0 // Fallback to the first avatar
    }
}
