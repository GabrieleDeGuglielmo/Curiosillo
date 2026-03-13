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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    init {
        carica(0)
        viewModelScope.launch {
            geminiPrefs.getRemainingUsages().collect {
                _geminiState.value = _geminiState.value.copy(rimanenti = it)
            }
        }
    }

    fun carica(giorni: Int) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, giorniSelezionati = giorni)
            val pillole = repo.getPerRipasso(giorni, emptySet())
            val categorie = pillole.map { it.category }.distinct().sorted()
            _state.value = _state.value.copy(
                pillole = pillole,
                categorie = categorie,
                isLoading = false
            )
            filtra()
        }
    }

    fun onQueryChange(newQuery: String) {
        _state.value = _state.value.copy(query = newQuery)
        filtra()
    }

    fun onCategoriaSelezionata(cat: String) {
        val attuali = _state.value.categorieSelezionate.toMutableSet()
        if (attuali.contains(cat)) attuali.remove(cat) else attuali.add(cat)
        _state.value = _state.value.copy(categorieSelezionate = attuali)
        filtra()
    }

    fun resetCategorie() {
        _state.value = _state.value.copy(categorieSelezionate = emptySet())
        filtra()
    }

    private fun filtra() {
        val s = _state.value
        val filtrati = s.pillole.filter { p ->
            val matchQuery = p.title.contains(s.query, ignoreCase = true) || p.body.contains(s.query, ignoreCase = true)
            val matchCat = s.categorieSelezionate.isEmpty() || s.categorieSelezionate.contains(p.category)
            matchQuery && matchCat
        }
        _state.value = s.copy(risultati = filtrati)
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
            onPillolaAggiornata = { nuovaPillola ->
                aggiornaPillolaCorrente(nuovaPillola)
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

    private fun aggiornaPillolaCorrente(nuovaPillola: Curiosity) {
        val s = _state.value
        val listaRisultati = s.risultati.toMutableList()
        val idxRisultati = listaRisultati.indexOfFirst { it.id == nuovaPillola.id }
        if (idxRisultati >= 0) {
            listaRisultati[idxRisultati] = nuovaPillola
        }
        
        val listaPillole = s.pillole.toMutableList()
        val idxPillole = listaPillole.indexOfFirst { it.id == nuovaPillola.id }
        if (idxPillole >= 0) {
            listaPillole[idxPillole] = nuovaPillola
        }

        _state.value = s.copy(
            risultati = listaRisultati, 
            pillole = listaPillole, 
            pillolaDettaglio = if (s.pillolaDettaglio?.id == nuovaPillola.id) nuovaPillola else s.pillolaDettaglio
        )
    }

    // ── Bookmarks ─────────────────────────────────────────────────────────────

    fun toggleBookmark() {
        val pillola = pilloleCorrente() ?: return
        viewModelScope.launch {
            repo.toggleBookmark(pillola)
            aggiornaPillolaCorrente(pillola.copy(isBookmarked = !pillola.isBookmarked))
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
