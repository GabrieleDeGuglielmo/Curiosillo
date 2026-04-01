package com.example.curiosillo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.curiosillo.data.Scoperta
import com.example.curiosillo.repository.CuriosityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class ScoperteUiState(
    val scoperte: List<Scoperta> = emptyList(),
    val isLoading: Boolean = false
)

class ScoperteViewModel(private val repo: CuriosityRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(ScoperteUiState())
    val uiState: StateFlow<ScoperteUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            // Sincronizza con Firebase all'avvio
            repo.syncScoperte()
            
            repo.getScoperteFlow().collect { list ->
                _uiState.value = _uiState.value.copy(scoperte = list, isLoading = false)
            }
        }
    }

    class Factory(private val repo: CuriosityRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(ScoperteViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return ScoperteViewModel(repo) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
