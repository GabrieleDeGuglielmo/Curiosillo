package com.example.curiosillo.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.curiosillo.BuildConfig
import com.example.curiosillo.data.GeminiPreferences
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
    val remainingUsages: Int = 0
)

class ArViewModel(private val geminiPrefs: GeminiPreferences) : ViewModel() {

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

            _uiState.value = _uiState.value.copy(isLoading = true, error = null, result = null)

            val categoriesPrompt = LISTA_CATEGORIE.joinToString(", ")
            val prompt = """
                Sei un divulgatore scientifico esperto e simpatico di nome Curiosillo. Usa un tono amichevole.
                Analizza questa immagine e dimmi delle informazioni riassuntive e una curiosità incredibile a riguardo in 100 parole massimo. 
                Restituisci esclusivamente un JSON con i seguenti campi:
                - "titolo": un titolo breve dell'oggetto/scena
                - "curiosita": le informazioni e la curiosità (max 100 parole totali)
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
                
                // Pulisci il testo se Gemini aggiunge markdown tipo ```json ... ```
                val jsonString = responseText.replace("```json", "").replace("```", "").trim()
                val json = JSONObject(jsonString)
                
                val result = ArResult(
                    titolo = json.getString("titolo"),
                    curiosita = json.getString("curiosita"),
                    categoria = json.getString("categoria")
                )

                geminiPrefs.incrementUsage()
                _uiState.value = _uiState.value.copy(isLoading = false, result = result)
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false, 
                    error = "Errore durante l'analisi: ${e.localizedMessage}"
                )
            }
        }
    }

    fun resetState() {
        _uiState.value = _uiState.value.copy(result = null, error = null, isLoading = false)
    }
}

class ArViewModelFactory(private val geminiPrefs: GeminiPreferences) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ArViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ArViewModel(geminiPrefs) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
