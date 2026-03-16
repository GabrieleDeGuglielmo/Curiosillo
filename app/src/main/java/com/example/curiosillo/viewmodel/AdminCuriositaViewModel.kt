package com.example.curiosillo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.curiosillo.firebase.FirebaseManager
import com.example.curiosillo.repository.CuriosityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject

data class AdminCuriositaUiState(
    val isLoading:   Boolean                              = true,
    val isAdmin:     Boolean                              = false,
    val curiosita:   List<FirebaseManager.CuriositaRemota> = emptyList(),
    val messaggio:   String?                              = null,
    val errore:      String?                              = null
)

class AdminCuriositaViewModel(private val repo: CuriosityRepository) : ViewModel() {

    private val _state = MutableStateFlow(AdminCuriositaUiState())
    val state: StateFlow<AdminCuriositaUiState> = _state.asStateFlow()

    init { carica() }

    fun carica() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            if (!FirebaseManager.isAdmin()) {
                _state.value = AdminCuriositaUiState(isLoading = false, isAdmin = false)
                return@launch
            }
            val lista = FirebaseManager.caricaTutteLeCuriositaRemote()
            _state.value = AdminCuriositaUiState(isLoading = false, isAdmin = true, curiosita = lista)
        }
    }

    fun salva(c: FirebaseManager.CuriositaRemota) {
        viewModelScope.launch {
            val result = FirebaseManager.salvaCuriosita(c)
            if (result.isSuccess) {
                // SINCRONIZZAZIONE LOCALE IMMEDIATA:
                // Dopo il salvataggio su Firebase, aggiorniamo Room per riflettere i cambiamenti
                repo.syncLocaleConRemoto(c)
                
                _state.value = _state.value.copy(messaggio = "✅ Curiosità salvata e sincronizzata localmente!")
                carica()
            } else {
                _state.value = _state.value.copy(errore = "Errore: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun elimina(externalId: String) {
        viewModelScope.launch {
            val result = FirebaseManager.eliminaCuriosita(externalId)
            if (result.isSuccess) {
                _state.value = _state.value.copy(messaggio = "🗑️ Curiosità eliminata.")
                carica()
            } else {
                _state.value = _state.value.copy(errore = "Errore: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun importaJson(json: String) {
        viewModelScope.launch {
            try {
                val lista = parseJson(json)
                val result = FirebaseManager.importaBulk(lista)
                if (result.isSuccess) {
                    // Sincronizzazione bulk locale (opzionale, ma utile)
                    lista.forEach { repo.syncLocaleConRemoto(it) }
                    
                    _state.value = _state.value.copy(
                        messaggio = "✅ Importate ${result.getOrDefault(0)} curiosità.")
                    carica()
                } else {
                    _state.value = _state.value.copy(errore = "Errore import: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(errore = "JSON non valido: ${e.message}")
            }
        }
    }

    fun dismissMessaggio() { _state.value = _state.value.copy(messaggio = null) }
    fun dismissErrore()    { _state.value = _state.value.copy(errore = null) }

    private fun parseJson(json: String): List<FirebaseManager.CuriositaRemota> {
        val array = if (json.trim().startsWith("[")) JSONArray(json)
        else {
            val obj = JSONObject(json)
            obj.getJSONArray("curiosita")
        }

        return (0 until array.length()).map { i ->
            val obj     = array.getJSONObject(i)
            val quizObj = obj.optJSONObject("quiz")

            val domanda = quizObj?.optString("domanda") ?: obj.optString("domanda")
            
            val rispostaCorretta = quizObj?.optString("rispostaCorretta")
                ?: quizObj?.optString("risposta_corretta")
                ?: obj.optString("rispostaCorretta")
                ?: obj.optString("risposta_corretta")
            
            val risposteErrate = quizObj?.optJSONArray("risposteErrate")?.let { arr ->
                (0 until arr.length()).map { arr.getString(it) }
            } ?: quizObj?.optJSONArray("risposte_errate")?.let { arr ->
                (0 until arr.length()).map { arr.getString(it) }
            } ?: obj.optJSONArray("risposteErrate")?.let { arr ->
                (0 until arr.length()).map { arr.getString(it) }
            } ?: obj.optJSONArray("risposte_errate")?.let { arr ->
                (0 until arr.length()).map { arr.getString(it) }
            }

            val spiegazione = quizObj?.optString("spiegazione") ?: obj.optString("spiegazione")

            FirebaseManager.CuriositaRemota(
                externalId       = obj.getString("id"),
                titolo           = obj.getString("titolo"),
                corpo            = obj.getString("corpo"),
                categoria        = obj.optString("categoria", ""),
                emoji            = obj.optString("emoji", ""),
                domanda          = domanda.ifBlank { null },
                rispostaCorretta = rispostaCorretta.ifBlank { null },
                risposteErrate   = risposteErrate,
                spiegazione      = spiegazione.ifBlank { null }
            )
        }
    }

    class Factory(private val repo: CuriosityRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = AdminCuriositaViewModel(repo) as T
    }
}
