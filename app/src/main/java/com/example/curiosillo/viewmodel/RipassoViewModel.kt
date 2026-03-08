package com.example.curiosillo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.curiosillo.data.Curiosity
import com.example.curiosillo.firebase.FirebaseManager
import com.example.curiosillo.repository.CuriosityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RipassoUiState(
    val pillole:           List<Curiosity> = emptyList(),
    val giorniSelezionati: Int             = 0,
    val indiceCorrente:    Int             = 0,
    val isLoading:         Boolean         = true,
    val nota:              String          = ""
)

class RipassoViewModel(
    private val repo: CuriosityRepository
) : ViewModel() {

    private val _state = MutableStateFlow(RipassoUiState())
    val state: StateFlow<RipassoUiState> = _state.asStateFlow()

    // ── Commenti ──────────────────────────────────────────────────────────────
    private val _commentiState = MutableStateFlow(CommentiUiState())
    val commentiState: StateFlow<CommentiUiState> = _commentiState.asStateFlow()

    init { carica(0) }

    fun carica(giorniMin: Int) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, giorniSelezionati = giorniMin)
            val pillole = repo.getPerRipasso(giorniMin, emptySet())
            _state.value = _state.value.copy(
                pillole        = pillole,
                indiceCorrente = 0,
                isLoading      = false,
                nota           = pillole.firstOrNull()?.nota ?: ""
            )
        }
    }

    fun prossima() {
        val nuovoIndice = _state.value.indiceCorrente + 1
        val pillole     = _state.value.pillole
        _state.value = _state.value.copy(
            indiceCorrente = nuovoIndice,
            nota           = pillole.getOrNull(nuovoIndice)?.nota ?: ""
        )
        _commentiState.value = CommentiUiState() // reset commenti per la nuova pillola
    }

    fun precedente() {
        val nuovoIndice = (_state.value.indiceCorrente - 1).coerceAtLeast(0)
        val pillole     = _state.value.pillole
        _state.value = _state.value.copy(
            indiceCorrente = nuovoIndice,
            nota           = pillole.getOrNull(nuovoIndice)?.nota ?: ""
        )
        _commentiState.value = CommentiUiState()
    }

    fun salvaNota(testo: String) {
        val pillola = pilloleCorrente() ?: return
        viewModelScope.launch {
            repo.salvaNota(pillola, testo)
            val aggiornate = _state.value.pillole.toMutableList()
            aggiornate[_state.value.indiceCorrente] = pillola.copy(nota = testo)
            _state.value = _state.value.copy(pillole = aggiornate, nota = testo)
        }
    }

    fun setVoto(voto: Int?) {
        val pillola = pilloleCorrente() ?: return
        viewModelScope.launch {
            val nuovoVoto   = if (pillola.voto == voto) null else voto
            val vecchioVoto = pillola.voto
            repo.setVoto(pillola, nuovoVoto)
            // Aggiorna stato locale
            val pilloleAggiornate = _state.value.pillole.toMutableList()
            pilloleAggiornate[_state.value.indiceCorrente] = pillola.copy(voto = nuovoVoto)
            _state.value = _state.value.copy(pillole = pilloleAggiornate)
            // Sync su Firestore
            val externalId = pillola.externalId ?: return@launch
            FirebaseManager.sincronizzaVoto(externalId, vecchioVoto, nuovoVoto)
        }
    }

    fun pilloleCorrente(): Curiosity? =
        _state.value.pillole.getOrNull(_state.value.indiceCorrente)

    // ── Commenti ──────────────────────────────────────────────────────────────
    fun caricaCommenti() {
        val externalId = pilloleCorrente()?.externalId ?: return
        viewModelScope.launch {
            _commentiState.value = _commentiState.value.copy(isLoading = true)
            val commenti = FirebaseManager.caricaCommenti(externalId)
            _commentiState.value = _commentiState.value.copy(commenti = commenti, isLoading = false)
        }
    }

    fun inviaCommento(testo: String) {
        val externalId = pilloleCorrente()?.externalId ?: return
        viewModelScope.launch {
            _commentiState.value = _commentiState.value.copy(invioInCorso = true, erroreInvio = null)
            try {
                FirebaseManager.aggiungiCommento(externalId, testo)
                val commenti = FirebaseManager.caricaCommenti(externalId)
                _commentiState.value = _commentiState.value.copy(commenti = commenti, invioInCorso = false)
            } catch (e: Exception) {
                _commentiState.value = _commentiState.value.copy(
                    invioInCorso = false, erroreInvio = e.message ?: "Errore sconosciuto"
                )
            }
        }
    }

    fun eliminaCommento(commentoId: String) {
        val externalId = pilloleCorrente()?.externalId ?: return
        viewModelScope.launch {
            FirebaseManager.eliminaCommento(externalId, commentoId)
            val commenti = FirebaseManager.caricaCommenti(externalId)
            _commentiState.value = _commentiState.value.copy(commenti = commenti)
        }
    }

    fun dismissErroreCommento() {
        _commentiState.value = _commentiState.value.copy(erroreInvio = null)
    }

    class Factory(
        private val repo: CuriosityRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(c: Class<T>): T = RipassoViewModel(repo) as T
    }
}