package com.example.curiosillo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.curiosillo.data.Curiosity
import com.example.curiosillo.data.GeminiPreferences
import com.example.curiosillo.firebase.FirebaseManager
import com.example.curiosillo.repository.CuriosityRepository
import com.example.curiosillo.ui.screens.utils.CommentiUiState
import com.example.curiosillo.ui.screens.utils.SegnalazioneHelper
import com.example.curiosillo.ui.screens.utils.SegnalazioneUiState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// UI State for Ripasso
data class RipassoUiState(
    val pillole: List<Curiosity> = emptyList(),
    val risultati: List<Curiosity> = emptyList(),
    val indiceCorrente: Int = 0,
    val isLoading: Boolean = false,
    val giorniSelezionati: Int = 0,
    val query: String = "",
    val categorie: List<String> = emptyList(),
    val categorieSelezionate: Set<String> = emptySet(),
    val pillolaDettaglio: Curiosity? = null
)

class RipassoViewModel(
    private val repo: CuriosityRepository,
    private val geminiPrefs: GeminiPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(RipassoUiState())
    val state: StateFlow<RipassoUiState> = _state.asStateFlow()

    private val _commentiState = MutableStateFlow(CommentiUiState())
    val commentiState: StateFlow<CommentiUiState> = _commentiState.asStateFlow()

    private val _geminiState = MutableStateFlow(GeminiUiState())
    val geminiState: StateFlow<GeminiUiState> = _geminiState.asStateFlow()

    // Shared report state
    private val _segnalazioneState = MutableStateFlow<SegnalazioneUiState>(SegnalazioneUiState.Idle)
    val segnalazioneState: StateFlow<SegnalazioneUiState> = _segnalazioneState.asStateFlow()

    // Flusso dei filtri per la reattività automatica
    private val _giorni = MutableStateFlow(0)
    private val _filtroCategorie = MutableStateFlow(emptySet<String>())

    init {
        osservaDatabase()
        viewModelScope.launch {
            geminiPrefs.getRemainingUsages().collect {
                _geminiState.value = _geminiState.value.copy(rimanenti = it)
            }
        }
    }

    /**
     * Osserva il database in tempo reale. Se un admin modifica una pillola,
     * il Flow di Room emette i nuovi dati e la UI si aggiorna istantaneamente.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun osservaDatabase() {
        combine(_giorni, _filtroCategorie) { g, cats -> Pair(g, cats) }
            .flatMapLatest { (g, cats) ->
                val soglia = if (g == 0) Long.MAX_VALUE
                else System.currentTimeMillis() - g * 24L * 60 * 60 * 1000
                
                if (cats.isEmpty()) repo.getPerRipassoFlow(soglia)
                else repo.getPerRipassoFilteredFlow(soglia, cats.toList())
            }
            .onEach { lista ->
                val catsDisponibili = lista.map { it.category }.distinct().sorted()
                _state.update { it.copy(pillole = lista, categorie = catsDisponibili, isLoading = false) }
                filtra()
            }
            .launchIn(viewModelScope)
    }

    fun carica(giorni: Int) {
        _giorni.value = giorni
        _state.update { it.copy(isLoading = true, giorniSelezionati = giorni) }
    }

    fun onQueryChange(newQuery: String) {
        _state.update { it.copy(query = newQuery) }
        filtra()
    }

    fun onCategoriaSelezionata(cat: String) {
        val attuali = _filtroCategorie.value.toMutableSet()
        if (attuali.contains(cat)) attuali.remove(cat) else attuali.add(cat)
        _filtroCategorie.value = attuali
        _state.update { it.copy(categorieSelezionate = attuali) }
        filtra()
    }

    fun resetCategorie() {
        _filtroCategorie.value = emptySet()
        _state.update { it.copy(categorieSelezionate = emptySet()) }
        filtra()
    }

    private fun filtra() {
        val s = _state.value
        val filtrati = s.pillole.filter { p ->
            val matchQuery = p.title.contains(s.query, ignoreCase = true) || p.body.contains(s.query, ignoreCase = true)
            val matchCat = s.categorieSelezionate.isEmpty() || s.categorieSelezionate.contains(p.category)
            matchQuery && matchCat
        }
        
        _state.update { currentState ->
            val nuovaPillolaDettaglio = if (currentState.pillolaDettaglio != null) {
                filtrati.find { it.id == currentState.pillolaDettaglio.id } ?: currentState.pillolaDettaglio
            } else null
            
            currentState.copy(
                risultati = filtrati,
                pillolaDettaglio = nuovaPillolaDettaglio
            )
        }
    }

    fun apriDettaglio(pillola: Curiosity) {
        val index = _state.value.risultati.indexOf(pillola)
        _state.value = _state.value.copy(pillolaDettaglio = pillola, indiceCorrente = if (index >= 0) index else 0)
        aggiornaStatoGeminiPerPillolaCorrente()
    }

    fun chiudiDettaglio() {
        _state.value = _state.value.copy(pillolaDettaglio = null)
    }

    fun pilloleCorrente(): Curiosity? {
        val s = _state.value
        if (s.risultati.isEmpty() || s.indiceCorrente !in s.risultati.indices) return null
        return s.risultati[s.indiceCorrente]
    }

    fun setIndice(indice: Int) {
        val s = _state.value
        if (indice in s.risultati.indices && indice != s.indiceCorrente) {
            val nuovaPillola = s.risultati[indice]
            _state.value = s.copy(
                indiceCorrente = indice,
                pillolaDettaglio = nuovaPillola
            )
            aggiornaStatoGeminiPerPillolaCorrente()
        }
    }

    private fun aggiornaStatoGeminiPerPillolaCorrente() {
        val cur = pilloleCorrente()
        if (cur?.approfondimentoAi != null) {
            _geminiState.value = _geminiState.value.copy(risposta = cur.approfondimentoAi, errore = null, isLoading = false)
        } else {
            resetGemini()
        }
    }

    fun dimmiDiPiu() {
        val c = pilloleCorrente() ?: return
        
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
            onPillolaAggiornata = { _ ->
                // Non serve aggiornare manualmente, osservaDatabase() se ne occupa
            },
            tagLog = "RipassoViewModel"
        )
    }

    fun resetGemini() {
        GeminiHelper.reset(_geminiState)
    }

    fun salvaNota(testo: String) {
        val pillola = pilloleCorrente() ?: return
        viewModelScope.launch {
            repo.salvaNota(pillola, testo)
        }
    }

    fun setVoto(voto: Int) {
        val pillola = pilloleCorrente() ?: return
        viewModelScope.launch {
            repo.setVoto(pillola, voto)
        }
    }

    // ── Bookmarks ─────────────────────────────────────────────────────────────

    fun toggleBookmark() {
        val pillola = pilloleCorrente() ?: return
        viewModelScope.launch {
            repo.toggleBookmark(pillola)
        }
    }

    // ── Reports (Segnalazioni) ────────────────────────────────────────────────

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

    class Factory(
        private val repo: CuriosityRepository,
        private val geminiPrefs: GeminiPreferences
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = 
            RipassoViewModel(repo, geminiPrefs) as T
    }
}
