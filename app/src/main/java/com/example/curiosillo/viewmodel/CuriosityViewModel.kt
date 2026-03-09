package com.example.curiosillo.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.curiosillo.BuildConfig
import com.example.curiosillo.data.CategoryPreferences
import com.example.curiosillo.data.Curiosity
import com.example.curiosillo.data.GeminiPreferences
import com.example.curiosillo.domain.GamificationEngine
import com.example.curiosillo.domain.RisultatoAzione
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// UI States
sealed class CuriosityUiState {
    object Loading : CuriosityUiState()
    object Empty   : CuriosityUiState()
    data class Success(val curiosity: Curiosity, val readCount: Int) : CuriosityUiState()
    object Learned : CuriosityUiState()
}

data class GeminiUiState(
    val isLoading: Boolean = false,
    val risposta:  String  = "",
    val errore:    String? = null,
    val isScritturaInCorso: Boolean = false,
    val rimanenti: Int = 2 //limite utilizzi giornaliero
)

class CuriosityViewModel(
    private val repo:   CuriosityRepository,
    private val prefs:  CategoryPreferences,
    private val engine: GamificationEngine,
    private val geminiPrefs: GeminiPreferences
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

    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash-lite",
        apiKey    = BuildConfig.GEMINI_API_KEY,
        requestOptions = RequestOptions(apiVersion = "v1")
    )

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
            val c = repo.getNext(categorie)
            _state.value = if (c != null) {
                // Se la pillola ha già un approfondimento salvato, caricalo nello stato Gemini
                if (c.approfondimentoAi != null) {
                    _geminiState.value = _geminiState.value.copy(risposta = c.approfondimentoAi, errore = null, isLoading = false)
                } else {
                    resetGemini()
                }
                CuriosityUiState.Success(c, repo.curiositàImparate())
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

                // Aggiorna la pillola nello stato UI per includere l'approfondimento
                _state.value = s.copy(curiosity = c.copy(approfondimentoAi = fullText))

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
                Log.e("CuriosityViewModel", "Errore Gemini AI: ${e.message}", e)
                _geminiState.value = _geminiState.value.copy(isLoading = false, errore = "Errore Gemini: ${e.localizedMessage}")
            }
        }
    }

    fun resetGemini() {
        _geminiState.value = _geminiState.value.copy(risposta = "", errore = null, isLoading = false, isScritturaInCorso = false)
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
        private val engine: GamificationEngine,
        private val geminiPrefs: GeminiPreferences
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            CuriosityViewModel(repo, prefs, engine, geminiPrefs) as T
    }
}
