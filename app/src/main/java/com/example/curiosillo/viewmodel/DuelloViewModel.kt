package com.example.curiosillo.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.curiosillo.data.DuelloGiocatore
import com.example.curiosillo.data.DuelloStato
import com.example.curiosillo.data.QuizQuestion
import com.example.curiosillo.firebase.FirebaseManager
import com.example.curiosillo.repository.CuriosityRepository
import com.example.curiosillo.repository.DuelloRepository
import com.example.curiosillo.repository.MatchmakingResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ── UI State ──────────────────────────────────────────────────────────────────

sealed class DuelloUiState {
    object Idle : DuelloUiState()
    object Loading : DuelloUiState()
    data class InAttesa(
        val duelloId:  String,
        val codice:    String,
        val mioNick:   String
    ) : DuelloUiState()
    data class InCountdown(
        val secondiRimasti: Int,   // 3..1
        val mioNick:        String
    ) : DuelloUiState()
    data class InCorso(
        val duello:          DuelloStato,
        val mioUid:          String,
        val mioNick:         String,
        val indiceCorrente:  Int,
        val rispostaData:    String?,
        val secondiRimasti:  Int,
        val risposteCorrentiAvversario: Int
    ) : DuelloUiState()
    data class Pausa(
        val duello:         DuelloStato,
        val mioUid:         String,
        val mioNick:        String,
        val indiceCorrente: Int,
        val miaRisposta:    String?,   // null = non ha risposto in tempo
        val rispostaCorretta: String,
        val eraCorretta:    Boolean,
        val secondiRimasti: Int        // countdown 3..0
    ) : DuelloUiState()
    data class Risultati(
        val duello:      DuelloStato,
        val mioUid:      String,
        val mioPunteggio: Int,
        val avvPunteggio: Int,
        val mioNick:     String,
        val avvNick:     String
    ) : DuelloUiState()
    data class Errore(val messaggio: String) : DuelloUiState()
}

// ── ViewModel ─────────────────────────────────────────────────────────────────

class DuelloViewModel(
    private val repo:       CuriosityRepository,
    private val duelloRepo: DuelloRepository,
    private val context:    Context
) : ViewModel() {

    private val _state = MutableStateFlow<DuelloUiState>(DuelloUiState.Idle)
    val state: StateFlow<DuelloUiState> = _state.asStateFlow()

    private var timerJob:    Job? = null
    private var osservaJob:  Job? = null
    private var duelloId:    String = ""
    private var domande:     List<QuizQuestion> = emptyList()
    private var mioUid:      String = ""
    private var mioNick:     String = ""

    private fun getNickname(): String {
        val user = FirebaseManager.utenteCorrente
        return user?.displayName?.takeIf { it.isNotBlank() }
            ?: user?.email?.substringBefore("@")?.takeIf { it.isNotBlank() }
            ?: "Giocatore${(1000..9999).random()}"
    }

    private fun getMioUid(): String =
        FirebaseManager.uid ?: "ospite_${System.currentTimeMillis()}"

    // ── Carica domande ────────────────────────────────────────────────────────

    private suspend fun caricaDomande(): List<QuizQuestion> {
        // 10 domande casuali da TUTTO il DB (non solo lette dall'utente)
        return repo.getQuizQuestionsAll(10)
    }

    // ── Crea stanza (invita amico) ────────────────────────────────────────────

    fun creaStanza() {
        mioUid  = getMioUid()
        mioNick = getNickname()
        viewModelScope.launch {
            _state.value = DuelloUiState.Loading
            try {
                domande  = caricaDomande()
                duelloId = duelloRepo.creaDuello(mioUid, mioNick, domande)
                // Ottieni il codice dal documento appena creato
                val codice = ottieniCodice(duelloId)
                _state.value = DuelloUiState.InAttesa(duelloId, codice, mioNick)
                osservaPerAvversario(duelloId)
            } catch (e: Exception) {
                _state.value = DuelloUiState.Errore("Errore creazione stanza: ${e.message}")
            }
        }
    }

    private suspend fun ottieniCodice(id: String): String {
        repeat(5) {
            val snap = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("duelli").document(id).get()
                .addOnSuccessListener {}.addOnFailureListener {}
            kotlinx.coroutines.delay(300)
        }
        // Ascolta il duello per il codice
        var codice = ""
        val job = viewModelScope.launch {
            duelloRepo.osservaDuello(id).collect { stato ->
                if (stato != null && stato.codice.isNotBlank()) {
                    codice = stato.codice
                    return@collect
                }
            }
        }
        delay(1000)
        job.cancel()
        return codice
    }

    private fun osservaPerAvversario(id: String) {
        osservaJob?.cancel()
        osservaJob = viewModelScope.launch {
            duelloRepo.osservaDuello(id).collect { stato ->
                if (stato == null) return@collect
                if (stato.isInCorso) {
                    avviaDuello(stato)
                }
            }
        }
    }

    // ── Unisciti con codice ───────────────────────────────────────────────────

    fun uniscitiConCodice(codice: String) {
        mioUid  = getMioUid()
        mioNick = getNickname()
        viewModelScope.launch {
            _state.value = DuelloUiState.Loading
            val result = duelloRepo.uniscitiConCodice(codice, mioUid, mioNick)
            result.fold(
                onSuccess = { id ->
                    duelloId = id
                    osservaJob?.cancel()
                    osservaJob = viewModelScope.launch {
                        duelloRepo.osservaDuello(id).collect { stato ->
                            if (stato == null) return@collect
                            when {
                                stato.isInCorso    -> avviaDuello(stato)
                                stato.isCompletato -> mostraRisultati(stato)
                            }
                        }
                    }
                },
                onFailure = {
                    _state.value = DuelloUiState.Errore(it.message ?: "Errore sconosciuto")
                }
            )
        }
    }

    // ── Matchmaking casuale ───────────────────────────────────────────────────

    fun cercaAvversarioCasuale() {
        mioUid  = getMioUid()
        mioNick = getNickname()
        viewModelScope.launch {
            _state.value = DuelloUiState.Loading
            try {
                domande = caricaDomande()
                duelloRepo.cercaAvversarioCasuale(mioUid, mioNick, domande).collect { result ->
                    when (result) {
                        is MatchmakingResult.InAttesa -> {
                            duelloId     = result.duelloId
                            _state.value = DuelloUiState.InAttesa(result.duelloId, "", mioNick)
                            // Listener già attivo nel repository — osserva per aggiornamenti in partita
                            osservaJob?.cancel()
                            osservaJob = viewModelScope.launch {
                                duelloRepo.osservaDuello(result.duelloId).collect { stato ->
                                    if (stato?.isInCorso == true) avviaDuello(stato)
                                }
                            }
                        }
                        is MatchmakingResult.Trovato -> {
                            duelloId = result.duelloId
                            // Carica subito lo stato e avvia
                            osservaJob?.cancel()
                            osservaJob = viewModelScope.launch {
                                duelloRepo.osservaDuello(result.duelloId).collect { stato ->
                                    if (stato == null) return@collect
                                    when {
                                        stato.isInCorso    -> avviaDuello(stato)
                                        stato.isCompletato -> mostraRisultati(stato)
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                _state.value = DuelloUiState.Errore("Errore ricerca: ${e.message}")
            }
        }
    }

    fun annullaRicerca() {
        osservaJob?.cancel()
        timerJob?.cancel()
        viewModelScope.launch {
            if (duelloId.isNotBlank()) {
                duelloRepo.annullaRicerca(mioUid, duelloId)
            }
            _state.value = DuelloUiState.Idle
        }
    }

    // ── Avvia partita ─────────────────────────────────────────────────────────

    private fun avviaDuello(stato: DuelloStato) {
        osservaJob?.cancel()
        // Countdown 3s prima di avviare la partita
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            for (s in 3 downTo 1) {
                _state.value = DuelloUiState.InCountdown(secondiRimasti = s, mioNick = mioNick)
                delay(1000)
            }
            _state.value = DuelloUiState.InCorso(
                duello          = stato,
                mioUid          = mioUid,
                mioNick         = mioNick,
                indiceCorrente  = 0,
                rispostaData    = null,
                secondiRimasti  = 10,
                risposteCorrentiAvversario = 0
            )
            avviaTimer(0, stato)
        }
        osservaDuello(stato.id)
    }

    private fun osservaDuello(id: String) {
        osservaJob?.cancel()
        osservaJob = viewModelScope.launch {
            duelloRepo.osservaDuello(id).collect { stato ->
                if (stato == null) {
                    timerJob?.cancel()
                    _state.value = DuelloUiState.Errore("L'avversario ha abbandonato il duello.")
                    return@collect
                }
                if (stato.isCompletato) { mostraRisultati(stato); return@collect }

                // Aggiorna duello in InCorso o Pausa senza sovrascrivere lo stato locale
                when (val cur = _state.value) {
                    is DuelloUiState.InCorso -> {
                        val avvUid      = stato.avversarioUid(mioUid)
                        val avvRisposte = avvUid?.let { stato.giocatori[it]?.risposte?.size } ?: 0
                        _state.value = cur.copy(
                            duello = stato,
                            risposteCorrentiAvversario = avvRisposte
                        )
                    }
                    is DuelloUiState.Pausa ->
                        _state.value = cur.copy(duello = stato)
                    else -> {}
                }
            }
        }
    }

    // ── Timer domanda ─────────────────────────────────────────────────────────

    private fun avviaTimer(indice: Int, stato: DuelloStato) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            for (s in 10 downTo 0) {
                val cur = _state.value as? DuelloUiState.InCorso ?: return@launch
                if (cur.indiceCorrente != indice) return@launch
                _state.value = cur.copy(secondiRimasti = s)
                // Se entrambi hanno risposto, esci subito dal timer
                val mioRisposte = cur.duello.giocatori[mioUid]?.risposte?.size ?: 0
                val avvUid      = cur.duello.avversarioUid(mioUid)
                val avvRisposte = avvUid?.let { cur.duello.giocatori[it]?.risposte?.size } ?: 0
                if (mioRisposte > indice && avvRisposte > indice) break
                if (s == 0) break
                delay(1000)
            }
            val cur = _state.value as? DuelloUiState.InCorso ?: return@launch
            avviaPausa(cur)
        }
    }

    private fun avviaPausa(cur: DuelloUiState.InCorso) {
        val domanda         = cur.duello.domande.getOrNull(cur.indiceCorrente) ?: return
        val rispostaCorretta = domanda.correctAnswer
        val miaRisposta     = cur.rispostaData
        val eraCorretta     = miaRisposta == rispostaCorretta

        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            for (s in 5 downTo 0) {
                _state.value = DuelloUiState.Pausa(
                    duello           = cur.duello,
                    mioUid           = cur.mioUid,
                    mioNick          = cur.mioNick,
                    indiceCorrente   = cur.indiceCorrente,
                    miaRisposta      = miaRisposta,
                    rispostaCorretta = rispostaCorretta,
                    eraCorretta      = eraCorretta,
                    secondiRimasti   = s
                )
                if (s == 0) break
                delay(1000)
            }
            // Dopo la pausa: prossima domanda o fine partita
            val pausa = _state.value as? DuelloUiState.Pausa ?: return@launch
            prossimaOFine(pausa)
        }
    }

    fun rispondi(risposta: String) {
        val cur = _state.value as? DuelloUiState.InCorso ?: return
        if (cur.rispostaData != null) return
        _state.value = cur.copy(rispostaData = risposta)
        viewModelScope.launch {
            duelloRepo.inviaRisposta(duelloId, mioUid, cur.indiceCorrente, risposta)
        }
    }

    private fun prossimaOFine(pausa: DuelloUiState.Pausa) {
        val prossimo = pausa.indiceCorrente + 1
        if (prossimo >= pausa.duello.domande.size) {
            viewModelScope.launch {
                duelloRepo.segnaCompletato(duelloId, mioUid)
            }
        } else {
            _state.value = DuelloUiState.InCorso(
                duello          = pausa.duello,
                mioUid          = pausa.mioUid,
                mioNick         = pausa.mioNick,
                indiceCorrente  = prossimo,
                rispostaData    = null,
                secondiRimasti  = 10,
                risposteCorrentiAvversario = 0
            )
            avviaTimer(prossimo, pausa.duello)
        }
    }

    private fun mostraRisultati(stato: DuelloStato) {
        timerJob?.cancel()
        osservaJob?.cancel()
        val avvUid  = stato.avversarioUid(mioUid) ?: ""
        val avvNick = stato.giocatori[avvUid]?.nickname ?: "Avversario"
        _state.value = DuelloUiState.Risultati(
            duello       = stato,
            mioUid       = mioUid,
            mioPunteggio = stato.punteggio(mioUid),
            avvPunteggio = stato.punteggio(avvUid),
            mioNick      = mioNick,
            avvNick      = avvNick
        )
    }

    fun reset() {
        timerJob?.cancel()
        osservaJob?.cancel()
        _state.value = DuelloUiState.Idle
        duelloId = ""
    }

    /** Chiamato quando l'utente esce dal duello (back fisico, back button, onCleared).
     *  Elimina il documento Firestore → l'avversario riceve null e vede "ha abbandonato". */
    fun abbandonaDuello() {
        val id = duelloId
        timerJob?.cancel()
        osservaJob?.cancel()
        _state.value = DuelloUiState.Idle
        duelloId = ""
        if (id.isBlank()) return
        viewModelScope.launch {
            duelloRepo.annullaRicerca(mioUid, id)
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        osservaJob?.cancel()
        // Se il ViewModel viene distrutto mentre si è in partita (es. navigazione di sistema)
        // elimina il duello così l'avversario viene notificato
        val id = duelloId
        if (id.isNotBlank()) {
            viewModelScope.launch {
                duelloRepo.annullaRicerca(mioUid, id)
            }
        }
    }

    class Factory(
        private val repo:       CuriosityRepository,
        private val duelloRepo: DuelloRepository,
        private val context:    Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(c: Class<T>): T =
            DuelloViewModel(repo, duelloRepo, context) as T
    }
}