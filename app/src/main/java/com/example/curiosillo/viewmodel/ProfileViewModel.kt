package com.example.curiosillo.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.curiosillo.data.BadgeSbloccato
import com.example.curiosillo.data.ContentPreferences
import com.example.curiosillo.data.GamificationPreferences
import com.example.curiosillo.firebase.FirebaseManager
import com.example.curiosillo.repository.CuriosityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class ProfileUiState(
    val totalCuriosità:      Int                  = 0,
    val curiositàImparate:   Int                  = 0,
    val quizDisponibili:     Int                  = 0,
    val totaleBookmark:      Int                  = 0,
    val totaleIgnorate:      Int                  = 0,
    val isAdmin:             Boolean              = false,
    val xpTotali:            Int                  = 0,
    val streakCorrente:      Int                  = 0,
    val streakMassima:       Int                  = 0,
    val badgeSbloccati:      List<BadgeSbloccato> = emptyList(),
    val username:            String               = "",
    val email:               String               = "",
    val isLoggato:           Boolean              = false,
    val isGoogleUser:        Boolean              = false,
    val isEliminazioneInCorso: Boolean            = false,
    val isLoading:           Boolean              = true
)

class ProfileViewModel(
    private val repo:         CuriosityRepository,
    private val gamifPrefs:   GamificationPreferences,
    private val contentPrefs: ContentPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    fun caricaStatistiche() {
        Log.d("ProfileVM", "caricaStatistiche() chiamata")
        viewModelScope.launch {
            try {
                // Mostra spinner solo al primo caricamento (quando non ci sono dati)
                val primoCaricamento = _state.value.totalCuriosità == 0 && !_state.value.isLoggato
                if (primoCaricamento) {
                    Log.d("ProfileVM", "Primo caricamento, imposto isLoading = true")
                    _state.value = _state.value.copy(isLoading = true)
                }

                val user     = FirebaseManager.utenteCorrente
                Log.d("ProfileVM", "User: ${user?.uid ?: "null"}")
                
                val username = when {
                    user == null             -> ""
                    user.displayName != null -> user.displayName!!
                    else -> {
                        Log.d("ProfileVM", "displayName null, carico profilo da Firestore")
                        val profilo = FirebaseManager.caricaProfilo(user.uid)
                        (profilo?.get("username") as? String) ?: ""
                    }
                }
                
                Log.d("ProfileVM", "Username ottenuto: $username")

                val newState = ProfileUiState(
                    totalCuriosità    = repo.totaleCuriosità(),
                    curiositàImparate = repo.curiositàImparate(),
                    quizDisponibili   = repo.quizNonRisposti(),
                    totaleBookmark    = repo.totaleBookmark(),
                    totaleIgnorate    = repo.totaleIgnorate(),
                    isAdmin           = FirebaseManager.isAdmin(),
                    xpTotali          = gamifPrefs.xpTotali.first(),
                    streakCorrente    = gamifPrefs.streakCorrente.first(),
                    streakMassima     = gamifPrefs.streakMassima.first(),
                    badgeSbloccati    = repo.badgeSbloccati(),
                    username          = username,
                    email             = user?.email ?: "",
                    isLoggato         = user != null,
                    isGoogleUser      = FirebaseManager.isGoogleUser(),
                    isLoading         = false
                )
                _state.value = newState
                Log.d("ProfileVM", "Statistiche caricate con successo")
                
                Log.d("SyncDebug", "quizNonRisposti: ${repo.quizNonRisposti()}")
                Log.d("SyncDebug", "quiz_question count: ${repo.countTotaliQuiz()}")
            } catch (e: Exception) {
                Log.e("ProfileVM", "Errore caricamento statistiche", e)
                _state.value = _state.value.copy(isLoading = false)
            }
        }
    }

    fun cambiaUsername(nuovoUser: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val res = FirebaseManager.cambiaUsername(nuovoUser)
            if (res.isSuccess) {
                _state.value = _state.value.copy(username = nuovoUser)
                onSuccess()
            } else {
                onError(res.exceptionOrNull()?.message ?: "Errore nel cambio username")
            }
        }
    }

    fun cambiaPassword(nuovaPass: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val res = FirebaseManager.cambiaPassword(nuovaPass)
            if (res.isSuccess) {
                onSuccess()
            } else {
                val raw = res.exceptionOrNull()?.message ?: ""
                val msg = when {
                    "recent login" in raw.lowercase() -> "Per motivi di sicurezza, questa operazione richiede un accesso recente. Disconnettiti e rientra prima di riprovare."
                    "at least 6 characters" in raw.lowercase() -> "La password deve avere almeno 6 caratteri."
                    else -> "Errore nel cambio password: $raw"
                }
                onError(msg)
            }
        }
    }

    fun resetProgressi() {
        viewModelScope.launch {
            repo.resetProgressi()
            gamifPrefs.reset()
            FirebaseManager.uid?.let { FirebaseManager.resetProgressiUtente(it) }
            contentPrefs.resetCloudMigrazione()
            caricaStatistiche()
        }
    }

    fun logout(onLogout: () -> Unit) {
        FirebaseManager.logout()
        onLogout()
    }

    fun eliminaAccount(onEliminato: () -> Unit) {
        val uid = FirebaseManager.uid ?: return
        viewModelScope.launch {
            _state.value = _state.value.copy(isEliminazioneInCorso = true)
            try {
                FirebaseManager.anonimizzaCommentiUtente(uid)
                FirebaseManager.eliminaDatiUtente(uid)
                repo.resetProgressi()
                gamifPrefs.reset()
                FirebaseManager.eliminaAccount()
            } catch (_: Exception) {
                FirebaseManager.logout()
            }
            _state.value = _state.value.copy(isEliminazioneInCorso = false)
            onEliminato()
        }
    }

    class Factory(
        private val repo:         CuriosityRepository,
        private val gamifPrefs:   GamificationPreferences,
        private val contentPrefs: ContentPreferences
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(c: Class<T>): T =
            ProfileViewModel(repo, gamifPrefs, contentPrefs) as T
    }
}