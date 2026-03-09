package com.example.curiosillo.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.curiosillo.BuildConfig
import com.example.curiosillo.data.Curiosity
import com.example.curiosillo.data.GeminiPreferences
import com.example.curiosillo.firebase.FirebaseManager
import com.example.curiosillo.repository.CuriosityRepository
import com.example.curiosillo.ui.screens.SegnalazioneHelper
import com.example.curiosillo.ui.screens.SegnalazioneUiState
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.RequestOptions
import kotlinx.coroutines.delay
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

    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash-lite",
        apiKey    = BuildConfig.GEMINI_API_KEY,
        requestOptions = RequestOptions(apiVersion = "v1")
    )

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
            _state.value = _state.value.copy(pillole = pillole, indiceCorrente = 0, isLoading = false)
            aggiornaStatoGeminiPerPillolaCorrente()
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
            aggiornaStatoGeminiPerPillolaCorrente()
        }
    }

    fun precedente() {
        val s = _state.value
        if (s.indiceCorrente > 0) {
            _state.value = s.copy(indiceCorrente = s.indiceCorrente - 1)
            aggiornaStatoGeminiPerPillolaCorrente()
        }
    }

    fun setIndice(indice: Int) {
        val s = _state.value
        if (indice in s.pillole.indices && indice != s.indiceCorrente) {
            _state.value = s.copy(indiceCorrente = indice)
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
        
        // Se abbiamo già l'approfondimento nel database, usiamolo direttamente
        if (c.approfondimentoAi != null) {
            _geminiState.value = _geminiState.value.copy(risposta = c.approfondimentoAi, isLoading = false, errore = null)
            return
        }

        viewModelScope.launch {
            if (!geminiPrefs.canUseGemini()) {
                _geminiState.value = _geminiState.value.copy(errore = "Hai esaurito i tuoi 3 approfondimenti giornalieri. Torna domani!")
                return@launch
            }

            _geminiState.value = _geminiState.value.copy(isLoading = true, errore = null, risposta = "")
            try {
                val prompt = "Sei un divulgatore scientifico esperto e simpatico di nome Curiosillo. " +
                        "Approfondisci questa curiosità fornendo dettagli storici, scientifici o aneddoti interessanti in circa 150 parole. " +
                        "Usa un tono amichevole. Non usare grassetti o formattazioni markdown pesanti.\n" +
                        "Titolo: ${c.title}\nContenuto: ${c.body}\nCategoria: ${c.category}"
                
                val response = generativeModel.generateContent(prompt)
                val fullText = response.text ?: "Uhm, non sono riuscito a trovare altre informazioni al momento."
                
                // Salva nel database
                repo.salvaApprofondimentoAi(c, fullText)
                geminiPrefs.incrementUsage()

                // Aggiorna la pillola nella lista corrente del ViewModel
                aggiornaPillolaCorrente(c.copy(approfondimentoAi = fullText))

                // Effetto macchina da scrivere
                _geminiState.value = _geminiState.value.copy(isLoading = false, isScritturaInCorso = true)
                var currentText = ""
                
                fullText.split(" ").forEach { word ->
                    currentText += "$word "
                    _geminiState.value = _geminiState.value.copy(risposta = currentText)
                    delay(30) 
                }
                _geminiState.value = _geminiState.value.copy(isScritturaInCorso = false)
                
            } catch (e: Exception) {
                Log.e("RipassoViewModel", "Errore Gemini AI: ${e.message}", e)
                _geminiState.value = _geminiState.value.copy(isLoading = false, errore = "Errore Gemini: ${e.localizedMessage}")
            }
        }
    }

    fun resetGemini() {
        _geminiState.value = _geminiState.value.copy(risposta = "", errore = null, isLoading = false, isScritturaInCorso = false)
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
        val lista = s.pillole.toMutableList()
        if (s.indiceCorrente in lista.indices) {
            lista[s.indiceCorrente] = nuovaPillola
            _state.value = s.copy(pillole = lista)
        }
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
