package com.example.curiosillo.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.curiosillo.BuildConfig
import com.example.curiosillo.data.BadgeSbloccato
import com.example.curiosillo.data.GeminiPreferences
import com.example.curiosillo.data.Scoperta
import com.example.curiosillo.domain.GamificationEngine
import com.example.curiosillo.repository.CuriosityRepository
import com.example.curiosillo.ui.screens.utils.LISTA_CATEGORIE
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.RequestOptions
import com.google.ai.client.generativeai.type.content
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject

data class ArResult(
    val titolo: String,
    val curiosita: String,
    val categoria: String
)

data class ArUiState(
    val isLoading: Boolean = false,
    val result: ArResult? = null,
    val error: String? = null,
    val remainingUsages: Int = 0,
    val badgeSbloccato: BadgeSbloccato? = null
)

class ArViewModel(
    private val repo: CuriosityRepository,
    private val geminiPrefs: GeminiPreferences,
    private val engine: GamificationEngine
) : ViewModel() {

    private val _uiState = MutableStateFlow(ArUiState())
    val uiState: StateFlow<ArUiState> = _uiState.asStateFlow()

    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash-lite",
        apiKey = BuildConfig.GEMINI_API_KEY,
        requestOptions = RequestOptions(apiVersion = "v1")
    )

    init {
        viewModelScope.launch {
            geminiPrefs.getRemainingUsages().collect {
                _uiState.value = _uiState.value.copy(remainingUsages = it)
            }
        }
    }

    fun scanImage(bitmap: Bitmap) {
        viewModelScope.launch {
            if (!geminiPrefs.canUseGemini()) {
                _uiState.value = _uiState.value.copy(error = "Hai esaurito i tuoi utilizzi giornalieri!")
                return@launch
            }

            _uiState.value = _uiState.value.copy(isLoading = true, error = null, result = null, badgeSbloccato = null)

            val categoriesPrompt = LISTA_CATEGORIE.joinToString(", ")
            val prompt = """
                Sei un divulgatore scientifico esperto e simpatico di nome Curiosillo. Usa un tono amichevole.
                Analizza questa immagine e dimmi delle informazioni riassuntive e una curiosità incredibile a riguardo in 100 parole circa. 
                Restituisci esclusivamente un JSON con i seguenti campi:
                - "titolo": un titolo breve dell'oggetto/scena
                - "curiosita": le informazioni e la curiosità (circa 100 parole totali)
                - "categoria": una tra queste: $categoriesPrompt
                
                Non aggiungere altro testo prima o dopo il JSON.
            """.trimIndent()

            try {
                val inputContent = content {
                    image(bitmap)
                    text(prompt)
                }

                val response = generativeModel.generateContent(inputContent)
                val responseText = response.text ?: ""
                val jsonString = responseText.replace("```json", "").replace("```", "").trim()
                val json = JSONObject(jsonString)
                
                val result = ArResult(
                    titolo = json.getString("titolo"),
                    curiosita = json.getString("curiosita"),
                    categoria = json.getString("categoria")
                )

                // 1. Salva la scoperta e ottieni il numero totale
                val numeroScoperte = repo.salvaScoperta(
                    Scoperta(
                        titolo = result.titolo,
                        descrizione = result.curiosita,
                        categoria = result.categoria
                    )
                )

                // 2. Notifica l'engine per XP e Badge
                val resGamif = engine.onScopertaEffettuata(numeroScoperte)

                geminiPrefs.incrementUsage()
                _uiState.value = _uiState.value.copy(
                    isLoading = false, 
                    result = result,
                    badgeSbloccato = resGamif.badgeSbloccati.firstOrNull()
                )
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false, 
                    error = "Errore durante l'analisi: ${e.localizedMessage}"
                )
            }
        }
    }

    fun resetState() {
        _uiState.value = _uiState.value.copy(result = null, error = null, isLoading = false, badgeSbloccato = null)
    }

    fun dismissBadge() {
        _uiState.value = _uiState.value.copy(badgeSbloccato = null)
    }
}

class ArViewModelFactory(
    private val repo: CuriosityRepository,
    private val geminiPrefs: GeminiPreferences,
    private val engine: GamificationEngine
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ArViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ArViewModel(repo, geminiPrefs, engine) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
