package com.example.curiosillo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.curiosillo.data.Curiosity
import com.example.curiosillo.firebase.FirebaseManager
import com.example.curiosillo.repository.CuriosityRepository
import com.example.curiosillo.ui.screens.SegnalazioneHelper
import com.example.curiosillo.ui.screens.SegnalazioneUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// UI State for Ripasso
data class RipassoUiState(
    val pillole: List<Curiosity> = emptyList(),
    val indiceCorrente: Int = 0,
    val isLoading: Boolean = false,
    val giorniSelezionati: Int = 0
)

class RipassoViewModel(
    private val repo: CuriosityRepository
) : ViewModel() {

    private val _state = MutableStateFlow(RipassoUiState())
    val state: StateFlow<RipassoUiState> = _state.asStateFlow()

    private val _commentiState = MutableStateFlow(CommentiUiState())
    val commentiState: StateFlow<CommentiUiState> = _commentiState.asStateFlow()

    // Shared report state (AGGIUNTA)
    private val _segnalazioneState = MutableStateFlow<SegnalazioneUiState>(SegnalazioneUiState.Idle)
    val segnalazioneState: StateFlow<SegnalazioneUiState> = _segnalazioneState.asStateFlow()

    init {
        carica(0)
    }

    // LOGICA ORIGINALE MANTENUTA
    fun carica(giorni: Int) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, giorniSelezionati = giorni)
            val pillole = repo.getPerRipasso(giorni, emptySet())
            _state.value = _state.value.copy(pillole = pillole, indiceCorrente = 0, isLoading = false)
        }
    }

    fun pilloleCorrente(): Curiosity? {
        val s = _state.value
        if (s.pillole.isEmpty() || s.indiceCorrente !in s.pillole.indices) return null
        return s.pillole[s.indiceCorrente]
    }

    fun prossima() {
        val s = _state.value
        if (s.indiceCorrente < s.pillole.size - 1) {
            _state.value = s.copy(indiceCorrente = s.indiceCorrente + 1)
        }
    }

    fun precedente() {
        val s = _state.value
        if (s.indiceCorrente > 0) {
            _state.value = s.copy(indiceCorrente = s.indiceCorrente - 1)
        }
    }

    fun salvaNota(testo: String) {
        val pillola = pilloleCorrente() ?: return
        viewModelScope.launch {
            repo.salvaNota(pillola, testo)
            aggiornaPillolaCorrente(pillola.copy(nota = testo))
        }
    }

    fun setVoto(voto: Int) {
        val pillola = pilloleCorrente() ?: return
        viewModelScope.launch {
            repo.setVoto(pillola, voto)
            aggiornaPillolaCorrente(pillola.copy(voto = voto))
        }
    }

    // Helper per aggiornare la pillola corrente nella lista senza ricaricare dal DB (AGGIUNTA)
    private fun aggiornaPillolaCorrente(nuovaPillola: Curiosity) {
        val s = _state.value
        val lista = s.pillole.toMutableList()
        if (s.indiceCorrente in lista.indices) {
            lista[s.indiceCorrente] = nuovaPillola
            _state.value = s.copy(pillole = lista)
        }
    }

    // ── Bookmarks (AGGIUNTA) ──────────────────────────────────────────────────

    fun toggleBookmark() {
        val pillola = pilloleCorrente() ?: return
        viewModelScope.launch {
            repo.toggleBookmark(pillola)
            aggiornaPillolaCorrente(pillola.copy(isBookmarked = !pillola.isBookmarked))
        }
    }

    // ── Reports (Segnalazioni) (AGGIUNTA) ─────────────────────────────────────

    fun inviaSegnalazione(tipo: String, testo: String) {
        val pillola = pilloleCorrente() ?: return
        val externalId = pillola.externalId ?: return

        SegnalazioneHelper.invia(viewModelScope, _segnalazioneState, externalId, tipo, testo)
    }

    fun dismissSegnalazione() {
        SegnalazioneHelper.dismiss(_segnalazioneState)
    }

    // ── Comments (Commenti) ───────────────────────────────────────────────────

    fun caricaCommenti() {
        val pillola = pilloleCorrente() ?: return
        val externalId = pillola.externalId ?: return
        viewModelScope.launch {
            _commentiState.value = _commentiState.value.copy(isLoading = true)
            val commenti = FirebaseManager.caricaCommenti(externalId)
            _commentiState.value = _commentiState.value.copy(commenti = commenti, isLoading = false)
        }
    }

    fun inviaCommento(testo: String) {
        val pillola = pilloleCorrente() ?: return
        val externalId = pillola.externalId ?: return
        if (FirebaseManager.contienePaloroleVietate(testo)) {
            _commentiState.value = _commentiState.value.copy(erroreInvio = "Il commento contiene parole non consentite.")
            return
        }
        viewModelScope.launch {
            _commentiState.value = _commentiState.value.copy(invioInCorso = true, erroreInvio = null)
            val result = FirebaseManager.aggiungiCommento(externalId, testo)
            if (result.isSuccess) {
                val commenti = FirebaseManager.caricaCommenti(externalId)
                _commentiState.value = _commentiState.value.copy(commenti = commenti, invioInCorso = false)
            } else {
                _commentiState.value = _commentiState.value.copy(invioInCorso = false, erroreInvio = "Errore durante l'invio. Riprova.")
            }
        }
    }

    fun eliminaCommento(commentoId: String) {
        val pillola = pilloleCorrente() ?: return
        val externalId = pillola.externalId ?: return
        viewModelScope.launch {
            FirebaseManager.eliminaCommento(externalId, commentoId)
            val commenti = FirebaseManager.caricaCommenti(externalId)
            _commentiState.value = _commentiState.value.copy(commenti = commenti)
        }
    }

    fun dismissErroreCommento() {
        _commentiState.value = _commentiState.value.copy(erroreInvio = null)
    }

    class Factory(private val repo: CuriosityRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(c: Class<T>): T = RipassoViewModel(repo) as T
    }
}