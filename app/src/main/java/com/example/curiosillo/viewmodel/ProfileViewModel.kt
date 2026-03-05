package com.example.curiosillo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.curiosillo.data.BadgeSbloccato
import com.example.curiosillo.data.GamificationPreferences
import com.example.curiosillo.firebase.FirebaseManager
import com.example.curiosillo.repository.CuriosityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class ProfileUiState(
    val totalCuriosità:    Int                  = 0,
    val curiositàImparate: Int                  = 0,
    val quizDisponibili:   Int                  = 0,
    val totaleBookmark:    Int                  = 0,
    val xpTotali:          Int                  = 0,
    val streakCorrente:    Int                  = 0,
    val streakMassima:     Int                  = 0,
    val badgeSbloccati:    List<BadgeSbloccato> = emptyList(),
    val username:          String               = "",
    val email:             String               = "",
    val isLoggato:         Boolean              = false,
    val isLoading:         Boolean              = true
)

class ProfileViewModel(
    private val repo:       CuriosityRepository,
    private val gamifPrefs: GamificationPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init { caricaStatistiche() }

    fun caricaStatistiche() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            val user     = FirebaseManager.utenteCorrente
            val username = when {
                user == null         -> ""
                user.displayName != null -> user.displayName!!
                else -> {
                    // prova a caricare username da Firestore
                    val profilo = FirebaseManager.caricaProfilo(user.uid)
                    (profilo?.get("username") as? String) ?: ""
                }
            }

            _state.value = ProfileUiState(
                totalCuriosità    = repo.totaleCuriosità(),
                curiositàImparate = repo.curiositàImparate(),
                quizDisponibili   = repo.quizNonRisposti(),
                totaleBookmark    = repo.totaleBookmark(),
                xpTotali          = gamifPrefs.xpTotali.first(),
                streakCorrente    = gamifPrefs.streakCorrente.first(),
                streakMassima     = gamifPrefs.streakMassima.first(),
                badgeSbloccati    = repo.badgeSbloccati(),
                username          = username,
                email             = user?.email ?: "",
                isLoggato         = user != null,
                isLoading         = false
            )
        }
    }

    fun resetProgressi() {
        viewModelScope.launch {
            repo.resetProgressi()
            gamifPrefs.reset()
            caricaStatistiche()
        }
    }

    fun logout(onLogout: () -> Unit) {
        FirebaseManager.logout()
        onLogout()
    }

    class Factory(
        private val repo:       CuriosityRepository,
        private val gamifPrefs: GamificationPreferences
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(c: Class<T>): T =
            ProfileViewModel(repo, gamifPrefs) as T
    }
}
