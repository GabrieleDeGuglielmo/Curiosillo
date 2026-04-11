package com.example.curiosillo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.curiosillo.data.GamificationPreferences
import com.example.curiosillo.data.QuizQuestion
import com.example.curiosillo.repository.CuriosityRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class SopravvivenzaUiState {
    object Loading : SopravvivenzaUiState()
    data class InCorso(
        val domanda: QuizQuestion,
        val risposte: List<String>,
        val vite: Int,
        val streak: Int,
        val recordPersonale: Int,
        val rispostaSelezionata: String? = null,
        val mostraCorretta: Boolean = false
    ) : SopravvivenzaUiState()
    data class GameOver(
        val streakFinale: Int,
        val nuovoRecord: Boolean,
        val recordPrecedente: Int
    ) : SopravvivenzaUiState()
}

class SopravvivenzaViewModel(
    private val repo: CuriosityRepository,
    private val prefs: GamificationPreferences
) : ViewModel() {

    private val _state = MutableStateFlow<SopravvivenzaUiState>(SopravvivenzaUiState.Loading)
    val state: StateFlow<SopravvivenzaUiState> = _state.asStateFlow()

    private var vite = 3
    private var streak = 0
    private var domandeDisponibili = mutableListOf<QuizQuestion>()
    private var recordAttuale = 0

    init {
        viewModelScope.launch {
            prefs.recordSopravvivenza.collect { recordAttuale = it }
        }
        iniziaPartita()
    }

    fun iniziaPartita() {
        viewModelScope.launch {
            _state.value = SopravvivenzaUiState.Loading
            vite = 3
            streak = 0
            caricaDomande()
            prossimaDomanda()
        }
    }

    private suspend fun caricaDomande() {
        domandeDisponibili = repo.getQuizQuestionsAll(50).toMutableList()
    }

    private suspend fun prossimaDomanda() {
        if (domandeDisponibili.isEmpty()) {
            caricaDomande()
        }
        val domanda = domandeDisponibili.removeAt(0)
        val risposte = listOf(
            domanda.correctAnswer,
            domanda.wrongAnswer1,
            domanda.wrongAnswer2,
            domanda.wrongAnswer3
        ).shuffled()
        
        _state.value = SopravvivenzaUiState.InCorso(domanda, risposte, vite, streak, recordAttuale)
    }

    fun rispondi(risposta: String) {
        val s = _state.value as? SopravvivenzaUiState.InCorso ?: return
        if (s.rispostaSelezionata != null) return

        val corretta = risposta == s.domanda.correctAnswer
        
        if (corretta) {
            streak++
        } else {
            vite--
        }

        _state.value = s.copy(
            rispostaSelezionata = risposta, 
            mostraCorretta = true,
            vite = vite,
            streak = streak
        )
    }

    fun vaiAvanti() {
        val s = _state.value as? SopravvivenzaUiState.InCorso ?: return
        if (s.rispostaSelezionata == null) return

        viewModelScope.launch {
            if (vite <= 0) {
                val isRecord = streak > recordAttuale
                if (isRecord) prefs.salvaRecordSopravvivenza(streak)
                _state.value = SopravvivenzaUiState.GameOver(streak, isRecord, recordAttuale)
            } else {
                prossimaDomanda()
            }
        }
    }

    class Factory(
        private val repo: CuriosityRepository,
        private val prefs: GamificationPreferences
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            SopravvivenzaViewModel(repo, prefs) as T
    }
}