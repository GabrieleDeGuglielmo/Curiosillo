package com.example.curiosillo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.curiosillo.repository.CuriosityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ProfileUiState(
    val totalCuriosità: Int    = 0,
    val curiositàImparate: Int = 0,
    val quizDisponibili: Int   = 0,
    val isLoading: Boolean     = true
)

class ProfileViewModel(private val repo: CuriosityRepository) : ViewModel() {

    private val _state = MutableStateFlow(ProfileUiState())
    val state: StateFlow<ProfileUiState> = _state.asStateFlow()

    init { caricaStatistiche() }

    fun caricaStatistiche() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            _state.value = ProfileUiState(
                totalCuriosità    = repo.totalCuriosità(),
                curiositàImparate = repo.curiositàImparate(),
                quizDisponibili   = repo.quizNonRisposti(),
                isLoading         = false
            )
        }
    }

    fun resetProgressi() {
        viewModelScope.launch {
            repo.resetProgressi()
            caricaStatistiche()
        }
    }

    class Factory(private val repo: CuriosityRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(c: Class<T>): T = ProfileViewModel(repo) as T
    }
}