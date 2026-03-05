package com.example.curiosillo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.curiosillo.data.QuizSession
import com.example.curiosillo.data.StatCategoria
import com.example.curiosillo.repository.CuriosityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class QuizStatsUiState(
    val statPerCategoria: List<StatCategoria> = emptyList(),
    val ultime20Sessioni: List<QuizSession>   = emptyList(),
    val isLoading:        Boolean             = true
)

class QuizStatsViewModel(private val repo: CuriosityRepository) : ViewModel() {

    private val _state = MutableStateFlow(QuizStatsUiState())
    val state: StateFlow<QuizStatsUiState> = _state.asStateFlow()

    init { carica() }

    private fun carica() {
        viewModelScope.launch {
            _state.value = QuizStatsUiState(
                statPerCategoria = repo.statPerCategoria(),
                ultime20Sessioni = repo.ultime20Sessioni(),
                isLoading        = false
            )
        }
    }

    class Factory(private val repo: CuriosityRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(c: Class<T>): T = QuizStatsViewModel(repo) as T
    }
}
