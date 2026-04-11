package com.example.curiosillo.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.curiosillo.data.ContentPreferences
import com.example.curiosillo.data.GamificationPreferences
import com.example.curiosillo.firebase.FirebaseManager
import com.example.curiosillo.firebase.SyncManager
import com.example.curiosillo.repository.CuriosityRepository
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class AuthUiState {
    object Idle                              : AuthUiState()
    object Loading                           : AuthUiState()
    object EmailRecuperoInviata              : AuthUiState()
    object VerificaEmailInviata              : AuthUiState()
    data class Successo(val user: FirebaseUser, val isNuovoUtente: Boolean) : AuthUiState()
    data class RichiedeUsername(val user: FirebaseUser, val suggestedUsername: String) : AuthUiState()
    data class Errore(val messaggio: String) : AuthUiState()
}

class AuthViewModel(
    private val repo:         CuriosityRepository,
    private val gamifPrefs:   GamificationPreferences,
    private val contentPrefs: ContentPreferences,
    private val context:      Context
) : ViewModel() {

    private val _state = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    private val syncManager = SyncManager(repo, gamifPrefs, contentPrefs)

    // ── Google Sign-In client ─────────────────────────────────────────────────

    fun getGoogleSignInClient(webClientId: String): GoogleSignInClient {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(webClientId)
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context, gso)
    }

    // ── Email / Password ──────────────────────────────────────────────────────

    fun loginEmail(email: String, password: String) {
        viewModelScope.launch {
            _state.value = AuthUiState.Loading
            val result = FirebaseManager.loginEmail(email, password)
            result.fold(
                onSuccess = { user ->
                    if (!user.isEmailVerified) {
                        user.sendEmailVerification().await()
                        FirebaseManager.logout()
                        _state.value = AuthUiState.VerificaEmailInviata
                    } else {
                        sincronizzaDopoLogin(user, isNuovo = false)
                    }
                },
                onFailure = {
                    _state.value = AuthUiState.Errore(messaggioErrore(it.message))
                }
            )
        }
    }

    fun recuperaPassword(email: String) {
        viewModelScope.launch {
            _state.value = AuthUiState.Loading
            val result = FirebaseManager.recuperaPassword(email)
            _state.value = result.fold(
                onSuccess = { AuthUiState.EmailRecuperoInviata },
                onFailure = { AuthUiState.Errore(messaggioErrore(it.message)) }
            )
        }
    }

    fun registraEmail(email: String, password: String, username: String) {
        viewModelScope.launch {
            _state.value = AuthUiState.Loading
            val result = FirebaseManager.registraEmail(email, password, username)
            result.fold(
                onSuccess = { user ->
                    try {
                        user.sendEmailVerification().await()
                        FirebaseManager.logout()
                        _state.value = AuthUiState.VerificaEmailInviata
                    } catch (e: Exception) {
                        _state.value = AuthUiState.Errore("Errore invio email verifica: ${e.message}")
                    }
                },
                onFailure = {
                    _state.value = AuthUiState.Errore(messaggioErrore(it.message))
                }
            )
        }
    }

    // ── Google ────────────────────────────────────────────────────────────────

    fun loginGoogle(idToken: String) {
        viewModelScope.launch {
            _state.value = AuthUiState.Loading
            val result = FirebaseManager.loginGoogle(idToken)
            result.fold(
                onSuccess = { pair ->
                    val user    = pair.first
                    val isNuovo = pair.second
                    if (isNuovo) {
                        _state.value = AuthUiState.RichiedeUsername(user, user.displayName ?: "")
                    } else {
                        sincronizzaDopoLogin(user, isNuovo = false)
                    }
                },
                onFailure = {
                    _state.value = AuthUiState.Errore(messaggioErrore(it.message))
                }
            )
        }
    }

    fun completaRegistrazioneGoogle(user: FirebaseUser, username: String) {
        viewModelScope.launch {
            _state.value = AuthUiState.Loading
            try {
                FirebaseManager.creaProfiloSeNonEsiste(user.uid, username, user.email ?: "")
                sincronizzaDopoLogin(user, isNuovo = true)
            } catch (e: Exception) {
                _state.value = AuthUiState.Errore(messaggioErrore(e.message))
            }
        }
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    fun logout() {
        FirebaseManager.logout()
        // Scolleghiamo anche Google per permettere la scelta dell'account al prossimo login
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build()
        val googleClient = GoogleSignIn.getClient(context, gso)
        googleClient.signOut()
        _state.value = AuthUiState.Idle
    }

    fun resetStato() {
        _state.value = AuthUiState.Idle
    }

    // ── Sync ──────────────────────────────────────────────────────────────────

    private suspend fun sincronizzaDopoLogin(user: FirebaseUser, isNuovo: Boolean) {
        // Controllo Ban
        val profilo = FirebaseManager.caricaProfilo(user.uid)
        if (profilo?.get("ban") == true) {
            val motivazione = profilo["banMotivazione"] as? String ?: "Sospensione per violazione dei termini."
            FirebaseManager.logout()
            _state.value = AuthUiState.Errore("Account sospeso.\nMotivazione: $motivazione")
            return
        }

        try {
            if (isNuovo) {
                syncManager.migraLocaleVersoCloud(user.uid)
            } else {
                // Migrazione one-shot per utenti esistenti che aggiornano l'app
                syncManager.migraLocaleVersoCloudSeNecessario(user.uid)
                syncManager.ripristinaCloudVersoLocale(user.uid)
            }
        } catch (_: Exception) {
            // sync fallita — l'utente è loggato ugualmente
        }
        _state.value = AuthUiState.Successo(user, isNuovo)
    }

    private fun messaggioErrore(raw: String?): String = when {
        raw == null                                 -> "Errore sconosciuto"
        "email address is already" in raw           -> "Email già registrata"
        "password is invalid" in raw                -> "Credenziali errate"
        "no user record" in raw                     -> "Credenziali errate"
        "invalid-credential" in raw                 -> "Credenziali errate"
        "badly formatted" in raw                    -> "Email non valida"
        "incorrect, malformed" in raw               -> "Credenziali errate"
        "at least 6 characters" in raw              -> "La password deve avere almeno 6 caratteri"
        "network error" in raw.lowercase()          -> "Errore di rete — controlla la connessione"
        "Username già in uso" in raw                -> "Username già in uso"
        "too-many-requests" in raw.lowercase()      -> "Troppi tentativi falliti. Riprova più tardi."
        else                                        -> "Errore: $raw"
    }

    class Factory(
        private val repo:         CuriosityRepository,
        private val gamifPrefs:   GamificationPreferences,
        private val contentPrefs: ContentPreferences,
        private val context:      Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(c: Class<T>): T =
            AuthViewModel(repo, gamifPrefs, contentPrefs, context) as T
    }
}