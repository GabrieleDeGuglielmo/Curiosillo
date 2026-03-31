package com.example.curiosillo.viewmodel

import android.util.Log
import com.example.curiosillo.BuildConfig
import com.example.curiosillo.data.Curiosity
import com.example.curiosillo.data.GeminiPreferences
import com.example.curiosillo.repository.CuriosityRepository
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.RequestOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * Stato UI condiviso per l'integrazione con Gemini AI.
 */
data class GeminiUiState(
    val isLoading: Boolean = false,
    val risposta:  String  = "",
    val errore:    String? = null,
    val isScritturaInCorso: Boolean = false,
    val rimanenti: Int = 5 // Limite utilizzi giornaliero di default
)

/**
 * Helper per gestire la logica di generazione contenuti con Gemini AI
 * in modo centralizzato tra i vari ViewModel (Curiosity, Ripasso, ecc.).
 */
object GeminiHelper {

    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash-lite",
        apiKey    = BuildConfig.GEMINI_API_KEY,
        requestOptions = RequestOptions(apiVersion = "v1")
    )

    /**
     * Avvia la generazione dell'approfondimento per una pillola.
     * Gestisce: limiti giornalieri, salvataggio su DB, effetto macchina da scrivere e aggiornamento stato.
     */
    fun generaApprofondimento(
        scope: CoroutineScope,
        geminiState: MutableStateFlow<GeminiUiState>,
        pillola: Curiosity,
        repo: CuriosityRepository,
        geminiPrefs: GeminiPreferences,
        onPillolaAggiornata: (Curiosity) -> Unit,
        tagLog: String = "GeminiHelper"
    ) {
        scope.launch {
            if (!geminiPrefs.canUseGemini()) {
                geminiState.value = geminiState.value.copy(
                    errore = "Hai esaurito i tuoi 5 approfondimenti giornalieri. Torna domani!"
                )
                return@launch
            }

            geminiState.value = geminiState.value.copy(isLoading = true, errore = null, risposta = "")
            
            try {
                val prompt = "Sei un divulgatore scientifico esperto e simpatico di nome Curiosillo. " +
                        "Approfondisci questa curiosità fornendo dettagli storici, scientifici o aneddoti interessanti in massimo 150 parole. " +
                        "Usa un tono amichevole. Non usare grassetti o formattazioni markdown pesanti.\n" +
                        "Titolo: ${pillola.title}\nContenuto: ${pillola.body}\nCategoria: ${pillola.category}"
                
                val response = generativeModel.generateContent(prompt)
                val fullText = response.text ?: "Uhm, non sono riuscito a trovare altre informazioni al momento."
                
                // 1. Salva nel database locale
                repo.salvaApprofondimentoAi(pillola, fullText)
                
                // 2. Incrementa l'uso nelle preferenze
                geminiPrefs.incrementUsage()

                // 3. Notifica il ViewModel che la pillola è cambiata
                onPillolaAggiornata(pillola.copy(approfondimentoAi = fullText))

                // 4. Effetto macchina da scrivere per la UI
                geminiState.value = geminiState.value.copy(isLoading = false, isScritturaInCorso = true)
                var currentText = ""
                
                fullText.split(" ").forEach { word ->
                    currentText += "$word "
                    geminiState.value = geminiState.value.copy(risposta = currentText)
                    delay(30) 
                }
                geminiState.value = geminiState.value.copy(isScritturaInCorso = false)
                
            } catch (e: Exception) {
                Log.e(tagLog, "Errore Gemini AI: ${e.message}", e)
                geminiState.value = geminiState.value.copy(
                    isLoading = false, 
                    errore = "Errore Gemini: ${e.localizedMessage}"
                )
            }
        }
    }

    /**
     * Resetta lo stato Gemini ai valori iniziali (tranne i rimanenti).
     */
    fun reset(geminiState: MutableStateFlow<GeminiUiState>) {
        geminiState.value = geminiState.value.copy(
            risposta = "", 
            errore = null, 
            isLoading = false, 
            isScritturaInCorso = false
        )
    }
}
