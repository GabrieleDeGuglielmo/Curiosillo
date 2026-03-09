package com.example.curiosillo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.curiosillo.data.CategoryPreferences
import com.example.curiosillo.data.Curiosity
import com.example.curiosillo.domain.GamificationEngine
import com.example.curiosillo.domain.RisultatoAzione
import com.example.curiosillo.firebase.FirebaseManager
import com.example.curiosillo.repository.CuriosityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

sealed class CuriosityUiState {
    object Loading : CuriosityUiState()
    object Empty   : CuriosityUiState()
    data class Success(val curiosity: Curiosity, val readCount: Int) : CuriosityUiState()
    object Learned : CuriosityUiState()
}

sealed class SegnalazioneUiState {
    object Idle     : SegnalazioneUiState()
    object Loading  : SegnalazioneUiState()
    object Successo : SegnalazioneUiState()
    data class Errore(val msg: String) : SegnalazioneUiState()
}

class CuriosityViewModel(
    private val repo:   CuriosityRepository,
    private val prefs:  CategoryPreferences,
    private val engine: GamificationEngine
) : ViewModel() {

    private val _state = MutableStateFlow<CuriosityUiState>(CuriosityUiState.Loading)
    val state: StateFlow<CuriosityUiState> = _state.asStateFlow()

    private val _risultatoAzione = MutableStateFlow<RisultatoAzione?>(null)
    val risultatoAzione: StateFlow<RisultatoAzione?> = _risultatoAzione.asStateFlow()

    private val _commentiState = MutableStateFlow(CommentiUiState())
    val commentiState: StateFlow<CommentiUiState> = _commentiState.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = CuriosityUiState.Loading
            val categorie = prefs.categorieAttive.first()
            val c = repo.getNext(categorie)
            _state.value = if (c != null)
                CuriosityUiState.Success(c, repo.curiositàImparate())
            else CuriosityUiState.Empty
        }
    }

    fun markLearned() {
        val s = _state.value as? CuriosityUiState.Success ?: return
        viewModelScope.launch {
            repo.markAsRead(s.curiosity)
            val risultato = engine.onPillolaLetta()
            _risultatoAzione.value = risultato
            _state.value = CuriosityUiState.Learned
        }
    }

    fun toggleBookmark() {
        val s = _state.value as? CuriosityUiState.Success ?: return
        viewModelScope.launch {
            repo.toggleBookmark(s.curiosity)
            _state.value = s.copy(
                curiosity = s.curiosity.copy(isBookmarked = !s.curiosity.isBookmarked)
            )
        }
    }

    fun salvaNota(testo: String) {
        val s = _state.value as? CuriosityUiState.Success ?: return
        viewModelScope.launch {
            repo.salvaNota(s.curiosity, testo)
            _state.value = s.copy(curiosity = s.curiosity.copy(nota = testo))
        }
    }

    // ── Segnalazioni ─────────────────────────────────────────────────────────

    private val _segnalazioneState = MutableStateFlow<SegnalazioneUiState>(SegnalazioneUiState.Idle)
    val segnalazioneState: StateFlow<SegnalazioneUiState> = _segnalazioneState.asStateFlow()

    fun inviaSegnalazione(tipo: String, testo: String) {
        val s = _state.value as? CuriosityUiState.Success ?: return
        val externalId = s.curiosity.externalId ?: return
        viewModelScope.launch {
            _segnalazioneState.value = SegnalazioneUiState.Loading
            val result = FirebaseManager.inviaSegnalazione(externalId, tipo, testo)
            _segnalazioneState.value = if (result.isSuccess)
                SegnalazioneUiState.Successo
            else
                SegnalazioneUiState.Errore(result.exceptionOrNull()?.message ?: "Errore")
        }
    }

    fun dismissSegnalazione() { _segnalazioneState.value = SegnalazioneUiState.Idle }

    fun toggleIgnora() {
        val s = _state.value as? CuriosityUiState.Success ?: return
        viewModelScope.launch {
            repo.toggleIgnora(s.curiosity)
            // dopo aver ignorato, carica la prossima pillola
            load()
        }
    }

    fun caricaCommenti() {
        val s = _state.value as? CuriosityUiState.Success ?: return
        val externalId = s.curiosity.externalId ?: return
        viewModelScope.launch {
            _commentiState.value = _commentiState.value.copy(isLoading = true)
            val commenti = FirebaseManager.caricaCommenti(externalId)
            _commentiState.value = _commentiState.value.copy(commenti = commenti, isLoading = false)
        }
    }

    fun inviaCommento(testo: String) {
        val s = _state.value as? CuriosityUiState.Success ?: return
        val externalId = s.curiosity.externalId ?: return
        if (FirebaseManager.contienePaloroleVietate(testo)) {
            _commentiState.value = _commentiState.value.copy(
                erroreInvio = "Il commento contiene parole non consentite."
            )
            return
        }
        viewModelScope.launch {
            _commentiState.value = _commentiState.value.copy(invioInCorso = true, erroreInvio = null)
            val result = FirebaseManager.aggiungiCommento(externalId, testo)
            if (result.isSuccess) {
                val commenti = FirebaseManager.caricaCommenti(externalId)
                _commentiState.value = _commentiState.value.copy(commenti = commenti, invioInCorso = false)
            } else {
                _commentiState.value = _commentiState.value.copy(
                    invioInCorso = false,
                    erroreInvio  = "Errore durante l'invio. Riprova."
                )
            }
        }
    }

    fun eliminaCommento(commentoId: String) {
        val s = _state.value as? CuriosityUiState.Success ?: return
        val externalId = s.curiosity.externalId ?: return
        viewModelScope.launch {
            FirebaseManager.eliminaCommento(externalId, commentoId)
            val commenti = FirebaseManager.caricaCommenti(externalId)
            _commentiState.value = _commentiState.value.copy(commenti = commenti)
        }
    }

    fun dismissErroreCommento() {
        _commentiState.value = _commentiState.value.copy(erroreInvio = null)
    }

    fun consumaRisultato() { _risultatoAzione.value = null }

    class Factory(
        private val repo:   CuriosityRepository,
        private val prefs:  CategoryPreferences,
        private val engine: GamificationEngine
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(c: Class<T>): T =
            CuriosityViewModel(repo, prefs, engine) as T
    }
}