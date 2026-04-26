package com.example.curiosillo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.curiosillo.data.AvatarCatalogo
import com.example.curiosillo.data.GamificationPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AvatarViewModel(
    private val gamifPrefs: GamificationPreferences
) : ViewModel() {

    // Osserviamo l'avatar attualmente salvato nel DataStore
    val avatarEquippato: StateFlow<String> = gamifPrefs.avatarEquippato
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "uovo")

    // Osserviamo gli XP totali per determinare i livelli di sblocco
    val xpTotali: StateFlow<Int> = gamifPrefs.xpTotali
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun selezionaAvatar(id: String) {
        viewModelScope.launch {
            gamifPrefs.impostaAvatar(id)
        }
    }
}
