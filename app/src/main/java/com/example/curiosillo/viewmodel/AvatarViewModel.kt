package com.example.curiosillo.viewmodel

import androidx.annotation.DrawableRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.curiosillo.data.AvatarCatalogo
import com.example.curiosillo.data.GamificationPreferences
import com.example.curiosillo.domain.LivelloHelper
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * UI state for a single avatar item in the selection grid.
 */
data class AvatarItemUiState(
    val id: String,
    @DrawableRes val resourceId: Int,
    val isUnlocked: Boolean,
    val isEquipped: Boolean,
    val isNew: Boolean,
    val livelloRichiesto: Int
)

class AvatarViewModel(
    private val gamifPrefs: GamificationPreferences
) : ViewModel() {

    // Current equipped avatar ID, defaulting to "0" if empty
    val avatarEquippato: StateFlow<String> = gamifPrefs.avatarEquippato
        .map { it.ifEmpty { "0" } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "0")

    // User's total XP
    val xpTotali: StateFlow<Int> = gamifPrefs.xpTotali
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    /**
     * Builds the avatar list from raw values. Used for both the combine block and the initial
     * stateIn value to guarantee the first composition frame already has correct state.
     */
    private fun buildAvatarList(
        xp: Int,
        equippedId: String,
        vistiIds: Set<Int>
    ): List<AvatarItemUiState> {
        val livelloAttuale = LivelloHelper.daXp(xp).numero
        val actualEquippedId = equippedId.ifEmpty { "0" }
        return AvatarCatalogo.lista.map { avatar ->
            val idInt = avatar.id.toIntOrNull() ?: -1
            val isUnlocked = livelloAttuale >= avatar.livelloRichiesto
            // isNew is true if unlocked AND ID is not in the already seen set
            val isNew = isUnlocked && idInt != -1 && idInt !in vistiIds
            AvatarItemUiState(
                id = avatar.id,
                resourceId = avatar.drawableRes,
                isUnlocked = isUnlocked,
                isEquipped = avatar.id == actualEquippedId,
                isNew = isNew,
                livelloRichiesto = avatar.livelloRichiesto
            )
        }
    }

    /**
     * List of avatars to display, including their unlock and "new" status.
     * An avatar is considered "new" if it's unlocked but its ID is not in the "seen" set.
     * The initial value is eagerly computed so the first composition frame already reflects
     * the correct "In uso" and "Novità" states (fixes blank selection on cold start).
     */
    val avatarItems: StateFlow<List<AvatarItemUiState>> = combine(
        gamifPrefs.xpTotali,
        gamifPrefs.avatarEquippato,
        gamifPrefs.avatarVistiIds
    ) { xp, equippedId, vistiIds ->
        buildAvatarList(xp, equippedId, vistiIds)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        // Eagerly compute initial list using the StateFlows' current defaults so the UI
        // renders correctly on the very first frame before DataStore emits.
        buildAvatarList(
            xp = xpTotali.value,
            equippedId = avatarEquippato.value,
            vistiIds = setOf(0) // mirrors GamificationPreferences.avatarVistiIds default
        )
    )

    fun selezionaAvatar(id: String) {
        viewModelScope.launch {
            gamifPrefs.impostaAvatar(id)
        }
    }

    /**
     * Marks all currently unlocked avatars as "seen" in the local preferences.
     * This should be called when the user enters the avatar selection screen.
     */
    fun onWardrobeClosed() {
        viewModelScope.launch {
            val xp = xpTotali.value
            val livelloAttuale = LivelloHelper.daXp(xp).numero
            
            val unlockedIds = AvatarCatalogo.lista
                .filter { livelloAttuale >= it.livelloRichiesto }
                .mapNotNull { it.id.toIntOrNull() }
                .toSet()
            
            if (unlockedIds.isNotEmpty()) {
                gamifPrefs.segnaAvatarComeVisti(unlockedIds)
            }
        }
    }
}
