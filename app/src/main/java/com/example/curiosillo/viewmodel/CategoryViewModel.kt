package com.example.curiosillo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.curiosillo.data.CategoryPreferences
import com.example.curiosillo.repository.CuriosityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CategoryViewModel(
    private val repo: CuriosityRepository,
    private val prefs: CategoryPreferences
) : ViewModel() {

    private val _categorie = MutableStateFlow<List<String>>(emptyList())
    val categorie: StateFlow<List<String>> = _categorie.asStateFlow()

    val categoriaAttiva: StateFlow<String> = prefs.categoriaAttiva
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    init {
        viewModelScope.launch {
            _categorie.value = repo.getCategorie()
        }
    }

    fun setCategoria(categoria: String) {
        viewModelScope.launch { prefs.setCategoria(categoria) }
    }

    class Factory(
        private val repo: CuriosityRepository,
        private val prefs: CategoryPreferences
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(c: Class<T>): T =
            CategoryViewModel(repo, prefs) as T
    }
}