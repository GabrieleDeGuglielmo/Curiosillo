package com.example.curiosillo.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.curiosillo.data.BadgeSbloccato
import com.example.curiosillo.data.ContentPreferences
import com.example.curiosillo.data.GamificationPreferences
import com.example.curiosillo.firebase.FirebaseManager
import com.example.curiosillo.repository.CuriosityRepository
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.userProfileChangeRequest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class ProfileUiState(
        val totalCuriosità: Int = 0,
        val curiositàImparate: Int = 0,
        val quizDisponibili: Int = 0,
        val totaleBookmark: Int = 0,
        val totaleIgnorate: Int = 0,
        val isAdmin: Boolean = false,
        val xpTotali: Int = 0,
        val streakCorrente: Int = 0,
        val streakMassima: Int = 0,
        val badgeSbloccati: List<BadgeSbloccato> = emptyList(),
        val username: String = "",
        val email: String = "",
        val photoUrl: String? = null,
        val avatarEquippato: String = "uovo",
        val isLoggato: Boolean = false,
        val isGoogleUser: Boolean = false,
        val isEliminazioneInCorso: Boolean = false,
        val isLoading: Boolean = true
)

class ProfileViewModel(
        private val repo: CuriosityRepository,
        private val gamifPrefs: GamificationPreferences,
        private val contentPrefs: ContentPreferences,
        private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    fun caricaStatistiche() {
        viewModelScope.launch {
            try {
                val user = FirebaseManager.utenteCorrente
                val username =
                        when {
                            user == null -> ""
                            user.displayName != null -> user.displayName!!
                            else -> {
                                val profilo = FirebaseManager.caricaProfilo(user.uid)
                                (profilo?.get("username") as? String) ?: ""
                            }
                        }

                val newState =
                        ProfileUiState(
                                totalCuriosità = repo.totaleCuriosità(),
                                curiositàImparate = repo.curiositàImparate(),
                                quizDisponibili = repo.quizNonRisposti(),
                                totaleBookmark = repo.totaleBookmark(),
                                totaleIgnorate = repo.totaleIgnorate(),
                                isAdmin = FirebaseManager.isAdmin(),
                                xpTotali = gamifPrefs.xpTotali.first(),
                                streakCorrente = gamifPrefs.streakCorrente.first(),
                                streakMassima = gamifPrefs.streakMassima.first(),
                                badgeSbloccati = repo.badgeSbloccati(),
                                username = username,
                                email = user?.email ?: "",
                                photoUrl = user?.photoUrl?.toString(),
                                avatarEquippato = gamifPrefs.avatarEquippato.first(),
                                isLoggato = user != null,
                                isGoogleUser = FirebaseManager.isGoogleUser(),
                                isLoading = false
                        )
                _state.value = newState
            } catch (e: Exception) {
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
                val msg =
                        when {
                            "recent login" in raw.lowercase() ->
                                    "Per motivi di sicurezza, questa operazione richiede un accesso recente. Disconnettiti e rientra prima di riprovare."
                            "at least 6 characters" in raw.lowercase() ->
                                    "La password deve avere almeno 6 caratteri."
                            else -> "Errore nel cambio password: $raw"
                        }
                onError(msg)
            }
        }
    }

    fun aggiornaFotoProfilo(uri: Uri, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val user = FirebaseManager.utenteCorrente ?: throw Exception("Utente non loggato")
                val profileUpdates = userProfileChangeRequest { photoUri = uri }
                user.updateProfile(profileUpdates).await()
                _state.value = _state.value.copy(photoUrl = uri.toString())
                onSuccess()
            } catch (e: Exception) {
                onError(e.message ?: "Errore aggiornamento foto")
            }
        }
    }

    fun selezionaAvatar(id: String) {
        _state.value = _state.value.copy(avatarEquippato = id)
        viewModelScope.launch { gamifPrefs.impostaAvatar(id) }
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
        viewModelScope.launch {
            FirebaseManager.logout()
            repo.resetProgressi()
            gamifPrefs.reset()
            contentPrefs.resetCloudMigrazione()
            // Scolleghiamo Google per forzare la scelta dell'account al prossimo accesso
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
            GoogleSignIn.getClient(context, gso).signOut()
            onLogout()
        }
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
                contentPrefs.resetCloudMigrazione()
                FirebaseManager.eliminaAccount()
                // Scolleghiamo anche Google in caso di eliminazione
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
                GoogleSignIn.getClient(context, gso).signOut()
            } catch (_: Exception) {
                FirebaseManager.logout()
            }
            _state.value = _state.value.copy(isEliminazioneInCorso = false)
            onEliminato()
        }
    }

    class Factory(
            private val repo: CuriosityRepository,
            private val gamifPrefs: GamificationPreferences,
            private val contentPrefs: ContentPreferences,
            private val context: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(c: Class<T>): T =
                ProfileViewModel(repo, gamifPrefs, contentPrefs, context) as T
    }
}
