package com.example.curiosillo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.curiosillo.data.GamificationPreferences
import com.example.curiosillo.data.QuizQuestion
import com.example.curiosillo.repository.CuriosityRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

sealed class ScalataUiState {
    object Loading : ScalataUiState()
    object NoQuestions : ScalataUiState()
    data class InCorso(
        val domanda: QuizQuestion,
        val risposte: List<String>,
        val punteggio: Int,
        val streak: Int,
        val recordPersonale: Int,
        val timeLeft: Int,
        val inFiamme: Boolean = false,
        val moltiplicatore: Int = 1,
        val rispostaSelezionata: String? = null,
        val mostraCorretta: Boolean = false
    ) : ScalataUiState()
    data class GameOver(
        val punteggioFinale: Int,
        val streakMassima: Int,
        val nuovoRecord: Boolean,
        val recordPrecedente: Int,
        val completata: Boolean = false
    ) : ScalataUiState()
}

class ScalataViewModel(
    private val repo: CuriosityRepository,
    private val prefs: GamificationPreferences
) : ViewModel() {

    private val _state = MutableStateFlow<ScalataUiState>(ScalataUiState.Loading)
    val state: StateFlow<ScalataUiState> = _state.asStateFlow()

    private var punteggio = 0
    private var streak = 0
    private var maxStreakInPartita = 0
    private var domandeDisponibili = mutableListOf<QuizQuestion>()
    private var recordAttuale = 0
    private var timerJob: Job? = null

    private val TIMER_SECONDS = 7

    init {
        viewModelScope.launch {
            prefs.recordScalata.collect { recordAttuale = it }
        }
        iniziaPartita()
    }

    fun iniziaPartita() {
        viewModelScope.launch {
            _state.value = ScalataUiState.Loading
            punteggio = 0
            streak = 0
            maxStreakInPartita = 0

            prefs.incrementaPartiteScalata()

            caricaDomande()
            if (domandeDisponibili.isEmpty()) {
                _state.value = ScalataUiState.NoQuestions
            } else {
                prossimaDomanda()
            }
        }
    }

    private suspend fun caricaDomande() {
        domandeDisponibili = repo.getQuizQuestionsLetteRandom().toMutableList()
    }

    private suspend fun prossimaDomanda() {
        if (domandeDisponibili.isEmpty()) {
            finePartita(completata = true)
            return
        }

        val domanda = domandeDisponibili.removeAt(0)
        val risposte = listOf(
            domanda.correctAnswer,
            domanda.wrongAnswer1,
            domanda.wrongAnswer2,
            domanda.wrongAnswer3
        ).shuffled()

        val inFiamme = streak >= 5
        val moltiplicatore = if (inFiamme) 2 else 1

        _state.value = ScalataUiState.InCorso(
            domanda = domanda,
            risposte = risposte,
            punteggio = punteggio,
            streak = streak,
            recordPersonale = recordAttuale,
            timeLeft = TIMER_SECONDS,
            inFiamme = inFiamme,
            moltiplicatore = moltiplicatore
        )

        startTimer()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            // Ciclo fino a 1
            for (i in TIMER_SECONDS downTo 1) {
                _state.update { s ->
                    if (s is ScalataUiState.InCorso) s.copy(timeLeft = i) else s
                }
                delay(1000)
            }
            
            // Arrivo a 0
            _state.update { s ->
                if (s is ScalataUiState.InCorso) s.copy(timeLeft = 0) else s
            }
            
            // Aspetto l'ultimo secondo di animazione visiva della barra
            delay(1000)
            
            gestisciFineTempo()
        }
    }

    private fun gestisciFineTempo() {
        val s = _state.value as? ScalataUiState.InCorso ?: return
        if (s.rispostaSelezionata != null) return
        finePartita()
    }

    fun rispondi(risposta: String) {
        val s = _state.value as? ScalataUiState.InCorso ?: return
        if (s.rispostaSelezionata != null) return

        timerJob?.cancel()
        val corretta = risposta == s.domanda.correctAnswer

        if (corretta) {
            streak++
            maxStreakInPartita = maxOf(maxStreakInPartita, streak)

            val bonusVelocita = s.timeLeft * 10
            val puntiGuadagnati = (100 + bonusVelocita) * s.moltiplicatore
            punteggio += puntiGuadagnati

            _state.update { currentState ->
                if (currentState is ScalataUiState.InCorso) {
                    currentState.copy(
                        rispostaSelezionata = risposta,
                        mostraCorretta = true,
                        punteggio = punteggio,
                        streak = streak,
                        inFiamme = streak >= 5
                    )
                } else currentState
            }

            viewModelScope.launch {
                delay(2000) // Aspetta 2 secondi con il feedback visibile
                prossimaDomanda()
            }
        } else {
            // Se sbagli, feedback immediato (rosso) e poi fine
            _state.update { currentState ->
                if (currentState is ScalataUiState.InCorso) {
                    currentState.copy(
                        rispostaSelezionata = risposta,
                        mostraCorretta = true
                    )
                } else currentState
            }
            viewModelScope.launch {
                delay(1000) // Un secondo di rosso prima del game over
                finePartita()
            }
        }
    }

    private fun finePartita(completata: Boolean = false) {
        viewModelScope.launch {
            val isRecord = punteggio > recordAttuale
            if (isRecord) {
                prefs.salvaRecordScalata(punteggio)
            }
            _state.value = ScalataUiState.GameOver(punteggio, maxStreakInPartita, isRecord, recordAttuale, completata)
        }
    }

    class Factory(
        private val repo: CuriosityRepository,
        private val prefs: GamificationPreferences
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ScalataViewModel(repo, prefs) as T
    }
}
