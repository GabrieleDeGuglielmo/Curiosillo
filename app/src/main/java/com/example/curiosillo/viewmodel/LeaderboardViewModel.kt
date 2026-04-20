package com.example.curiosillo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.curiosillo.firebase.FirebaseManager
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

data class LeaderboardUiState(
    val entries: List<FirebaseManager.LeaderboardEntry> = emptyList(),
    val isLoading: Boolean = false,
    val isWeekly: Boolean = true,
    val mode: FirebaseManager.LeaderboardMode = FirebaseManager.LeaderboardMode.SCALATA,
    val userRank: Int? = null,
    val userEntry: FirebaseManager.LeaderboardEntry? = null
)

class LeaderboardViewModel : ViewModel() {

    private val _state = MutableStateFlow(LeaderboardUiState())
    val state: StateFlow<LeaderboardUiState> = _state.asStateFlow()

    private var lastDocument: DocumentSnapshot? = null

    init {
        caricaLeaderboard()
    }

    fun setMode(mode: FirebaseManager.LeaderboardMode) {
        if (_state.value.mode != mode) {
            _state.value = _state.value.copy(mode = mode, entries = emptyList())
            lastDocument = null
            caricaLeaderboard()
        }
    }

    fun setWeekly(weekly: Boolean) {
        if (_state.value.isWeekly != weekly) {
            _state.value = _state.value.copy(isWeekly = weekly, entries = emptyList())
            lastDocument = null
            caricaLeaderboard()
        }
    }

    fun caricaLeaderboard() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            
            val period = if (_state.value.isWeekly) getPeriodoCorrente() else null
            val snapshots = FirebaseManager.caricaLeaderboard(
                modeId = _state.value.mode.id,
                period = period,
                limit = 50
            )

            val newEntries = snapshots.mapIndexed { index, doc ->
                doc.toObject(FirebaseManager.LeaderboardEntry::class.java)!!.copy(rank = index + 1)
            }

            lastDocument = snapshots.lastOrNull()
            
            // Cerchiamo la posizione dell'utente corrente (approssimata nella top 50)
            val currentUid = FirebaseManager.uid
            val userEntry = newEntries.find { it.uid == currentUid }
            
            _state.value = _state.value.copy(
                entries = newEntries,
                isLoading = false,
                userEntry = userEntry,
                userRank = userEntry?.rank
            )
        }
    }

    private fun getPeriodoCorrente(): String {
        val cal = Calendar.getInstance()
        val year = cal.get(Calendar.YEAR)
        val week = cal.get(Calendar.WEEK_OF_YEAR)
        return String.format(Locale.US, "%d-W%02d", year, week)
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = LeaderboardViewModel() as T
    }
}
