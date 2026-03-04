package com.example.curiosillo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.curiosillo.data.Curiosity
import com.example.curiosillo.repository.CuriosityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class CuriosityUiState {
    object Loading : CuriosityUiState()
    object Empty   : CuriosityUiState()
    object Learned : CuriosityUiState()
    data class Success(val curiosity: Curiosity, val readCount: Int) : CuriosityUiState()
}

class CuriosityViewModel(private val repo: CuriosityRepository) : ViewModel() {
    private val _state = MutableStateFlow<CuriosityUiState>(CuriosityUiState.Loading)
    val state: StateFlow<CuriosityUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = CuriosityUiState.Loading
            val c = repo.getNext()
            _state.value = if (c != null) CuriosityUiState.Success(c, repo.countRead())
                           else CuriosityUiState.Empty
        }
    }

    fun markLearned() {
        val s = _state.value as? CuriosityUiState.Success ?: return
        viewModelScope.launch {
            repo.markAsRead(s.curiosity)
            _state.value = CuriosityUiState.Learned
        }
    }

    class Factory(private val repo: CuriosityRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(c: Class<T>): T = CuriosityViewModel(repo) as T
    }
}
