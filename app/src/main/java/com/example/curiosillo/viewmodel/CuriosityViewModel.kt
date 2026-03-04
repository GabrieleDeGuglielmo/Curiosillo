package com.example.curiosillo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.curiosillo.data.CategoryPreferences
import com.example.curiosillo.data.Curiosity
import com.example.curiosillo.repository.CuriosityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first

sealed class CuriosityUiState {
    object Loading : CuriosityUiState()
    object Empty   : CuriosityUiState()
    object Learned : CuriosityUiState()
    data class Success(val curiosity: Curiosity, val readCount: Int) : CuriosityUiState()
}

class CuriosityViewModel(private val repo: CuriosityRepository,     private val prefs: CategoryPreferences) : ViewModel() {
    private val _state = MutableStateFlow<CuriosityUiState>(CuriosityUiState.Loading)
    val state: StateFlow<CuriosityUiState> = _state.asStateFlow()

    init { load() }

    fun load() {
        viewModelScope.launch {
            _state.value = CuriosityUiState.Loading
            val categorie = prefs.categorieAttive.first()
            val c = repo.getNext(categorie)
            _state.value = if (c != null)
                CuriosityUiState.Success(c, repo.curiositàImparate())
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

    fun toggleBookmark() {
        val s = _state.value as? CuriosityUiState.Success ?: return
        viewModelScope.launch {
            repo.toggleBookmark(s.curiosity)
            _state.value = s.copy(
                curiosity = s.curiosity.copy(isBookmarked = !s.curiosity.isBookmarked)
            )
        }
    }

    class Factory(
        private val repo: CuriosityRepository,
        private val prefs: CategoryPreferences
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(c: Class<T>): T =
            CuriosityViewModel(repo, prefs) as T
    }
}