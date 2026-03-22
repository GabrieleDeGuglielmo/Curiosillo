package com.example.curiosillo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.curiosillo.data.CategoryPreferences
import com.example.curiosillo.data.GamificationPreferences
import com.example.curiosillo.data.Curiosity
import com.example.curiosillo.data.GeminiPreferences
import com.example.curiosillo.domain.GamificationEngine
import com.example.curiosillo.domain.RisultatoAzione
import com.example.curiosillo.repository.CuriosityRepository
import com.example.curiosillo.ui.screens.utils.CommentiUiState
import com.example.curiosillo.ui.screens.utils.SegnalazioneHelper
import com.example.curiosillo.ui.screens.utils.SegnalazioneUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// UI States
sealed class CuriosityUiState {
    object Loading : CuriosityUiState()
    object Empty   : CuriosityUiState()
    data class Success(val curiosity: Curiosity, val readCount: Int, val isRecuperata: Boolean = false) : CuriosityUiState()
    object Learned : CuriosityUiState()
}

class CuriosityViewModel(
    private val repo:        CuriosityRepository,
    private val prefs:       CategoryPreferences,
    private val engine:      GamificationEngine,
    private val geminiPrefs: GeminiPreferences,
    private val gamifPrefs:  GamificationPreferences
) : ViewModel() {

    private val _state = MutableStateFlow<CuriosityUiState>(CuriosityUiState.Loading)
    val state: StateFlow<CuriosityUiState> = _state.asStateFlow()

    private val _risultatoAzione = MutableStateFlow<RisultatoAzione?>(null)
    val risultatoAzione: StateFlow<RisultatoAzione?> = _risultatoAzione.asStateFlow()

    private val _commentiState = MutableStateFlow(CommentiUiState())
    val commentiState: StateFlow<CommentiUiState> = _commentiState.asStateFlow()

    private val _segnalazioneState = MutableStateFlow<SegnalazioneUiState>(SegnalazioneUiState.Idle)
    val segnalazioneState: StateFlow<SegnalazioneUiState> = _segnalazioneState.asStateFlow()

    private val _geminiState = MutableStateFlow(GeminiUiState())
    val geminiState: StateFlow<GeminiUiState> = _geminiState.asStateFlow()

    init {
        load()
        viewModelScope.launch {
            geminiPrefs.getRemainingUsages().collect {
                _geminiState.value = _geminiState.value.copy(rimanenti = it)
            }
        }
    }

    fun load() {
        viewModelScope.launch {
            _state.value = CuriosityUiState.Loading
            val categorie = prefs.categorieAttive.first()

            // Prioritizza la curiosita' con cui l'utente ha interagito (bookmark/nota/approfondimento AI)
            // se non ancora imparata
            val lastExtId = gamifPrefs.getLastInteractedExternalId()
            var isRecuperata = false
            
            val c = if (lastExtId != null) {
                val candidate = repo.getByExternalId(lastExtId)
                if (candidate != null && !candidate.isRead && !candidate.isIgnorata) {
                    // Se la pillola non appartiene alle categorie correnti, segnaliamo il recupero
                    if (categorie.isNotEmpty() && candidate.category !in categorie) {
                        isRecuperata = true
                    }
                    candidate
                } else {
                    gamifPrefs.clearLastInteractedExternalId()
                    repo.getNext(categorie)
                }
            } else {
                repo.getNext(categorie)
            }

            _state.value = if (c != null) {
                // Se la pillola ha già un approfondimento salvato, caricalo nello stato Gemini
                if (c.approfondimentoAi != null) {
                    _geminiState.value = _geminiState.value.copy(risposta = c.approfondimentoAi, errore = null, isLoading = false)
                } else {
                    resetGemini()
                }
                CuriosityUiState.Success(c, repo.curiositàImparate(), isRecuperata = isRecuperata)
            } else CuriosityUiState.Empty
        }
    }

    fun dimmiDiPiu() {
        val s = _state.value as? CuriosityUiState.Success ?: return
        val c = s.curiosity

        // Se abbiamo già l'approfondimento nel database, usiamolo direttamente
        if (c.approfondimentoAi != null) {
            _geminiState.value = _geminiState.value.copy(risposta = c.approfondimentoAi, isLoading = false, errore = null)
            return
        }

        GeminiHelper.generaApprofondimento(
            scope = viewModelScope,
            geminiState = _geminiState,
            pillola = c,
            repo = repo,
            geminiPrefs = geminiPrefs,
            onPillolaAggiornata = { nuovaPillola ->
                _state.value = s.copy(curiosity = nuovaPillola)
                // Se l'approfondimento è stato generato con successo, salviamo l'interazione
                if (nuovaPillola.approfondimentoAi != null && !nuovaPillola.isRead) {
                    viewModelScope.launch {
                        nuovaPillola.externalId?.let { gamifPrefs.setLastInteractedExternalId(it) }
                    }
                }
            },
            tagLog = "CuriosityViewModel"
        )
    }

    fun resetGemini() {
        GeminiHelper.reset(_geminiState)
    }

    fun markLearned() {
        val s = _state.value as? CuriosityUiState.Success ?: return
        viewModelScope.launch {
            repo.markAsRead(s.curiosity)
            gamifPrefs.clearLastInteractedExternalId()
            val risultato = engine.onPillolaLetta()
            _risultatoAzione.value = risultato
            _state.value = CuriosityUiState.Learned
        }
    }

    fun toggleBookmark() {
        val s = _state.value as? CuriosityUiState.Success ?: return
        viewModelScope.launch {
            repo.toggleBookmark(s.curiosity)
            val nuovoStato = !s.curiosity.isBookmarked
            _state.value = s.copy(curiosity = s.curiosity.copy(isBookmarked = nuovoStato))
            if (nuovoStato && !s.curiosity.isRead) {
                s.curiosity.externalId?.let { gamifPrefs.setLastInteractedExternalId(it) }
            } else if (!nuovoStato && s.curiosity.nota.isBlank() && s.curiosity.approfondimentoAi == null) {
                gamifPrefs.clearLastInteractedExternalId()
            }
        }
    }

    fun salvaNota(testo: String) {
        val s = _state.value as? CuriosityUiState.Success ?: return
        viewModelScope.launch {
            repo.salvaNota(s.curiosity, testo)
            _state.value = s.copy(curiosity = s.curiosity.copy(nota = testo))
            if (testo.isNotBlank() && !s.curiosity.isRead) {
                s.curiosity.externalId?.let { gamifPrefs.setLastInteractedExternalId(it) }
            } else if (testo.isBlank() && !s.curiosity.isBookmarked && s.curiosity.approfondimentoAi == null) {
                 gamifPrefs.clearLastInteractedExternalId()
            }
        }
    }

    fun toggleIgnora() {
        val s = _state.value as? CuriosityUiState.Success ?: return
        viewModelScope.launch {
            repo.toggleIgnora(s.curiosity)
            load()
        }
    }

    // ── Reports (Segnalazioni) ──────────────────────────────────────────────────

    fun inviaSegnalazione(tipo: String, testo: String) {
        val s = _state.value as? CuriosityUiState.Success ?: return
        val externalId = s.curiosity.externalId ?: return
        SegnalazioneHelper.invia(viewModelScope, _segnalazioneState, externalId, tipo, testo)
    }

    fun dismissSegnalazione() {
        SegnalazioneHelper.dismiss(_segnalazioneState)
    }

    // ── Comments (Commenti) ─────────────────────────────────────────────────────

    fun caricaCommenti() {
        val s = _state.value as? CuriosityUiState.Success ?: return
        val externalId = s.curiosity.externalId ?: return
        FirebaseHelper.caricaCommenti(viewModelScope, _commentiState, externalId)
    }

    fun inviaCommento(testo: String) {
        val s = _state.value as? CuriosityUiState.Success ?: return
        val externalId = s.curiosity.externalId ?: return
        FirebaseHelper.inviaCommento(viewModelScope, _commentiState, externalId, testo)
    }

    fun dismissRecupero() {
        val s = _state.value as? CuriosityUiState.Success ?: return
        _state.value = s.copy(isRecuperata = false)
    }

    fun eliminaCommento(commentoId: String) {
        val s = _state.value as? CuriosityUiState.Success ?: return
        val externalId = s.curiosity.externalId ?: return
        FirebaseHelper.eliminaCommento(viewModelScope, _commentiState, externalId, commentoId)
    }

    fun dismissErroreCommento() {
        _commentiState.value = _commentiState.value.copy(erroreInvio = null)
    }

    fun consumaRisultato() { _risultatoAzione.value = null }

    class Factory(
        private val repo:        CuriosityRepository,
        private val prefs:       CategoryPreferences,
        private val engine:      GamificationEngine,
        private val geminiPrefs: GeminiPreferences,
        private val gamifPrefs:  GamificationPreferences
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            CuriosityViewModel(repo, prefs, engine, geminiPrefs, gamifPrefs) as T
    }
}