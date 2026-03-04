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

data class BookmarkUiState(
    val risultati:  List<Curiosity> = emptyList(),
    val categorie:  List<String>    = emptyList(),
    val query:      String          = "",
    val categoria:  String          = "",
    val isLoading:  Boolean         = true,
    val dettaglio:  Curiosity?      = null   // pillola aperta nel bottom sheet
)

class BookmarkViewModel(private val repo: CuriosityRepository) : ViewModel() {

    private val _state = MutableStateFlow(BookmarkUiState())
    val state: StateFlow<BookmarkUiState> = _state.asStateFlow()

    init { carica() }

    private fun carica() {
        viewModelScope.launch {
            val categorie = repo.categorieBookmark()
            val risultati = repo.cercaBookmark(
                _state.value.query,
                _state.value.categoria
            )
            _state.value = _state.value.copy(
                risultati  = risultati,
                categorie  = categorie,
                isLoading  = false
            )
        }
    }

    fun onQueryChange(nuova: String) {
        _state.value = _state.value.copy(query = nuova)
        cerca()
    }

    fun onCategoriaSelezionata(cat: String) {
        val nuova = if (_state.value.categoria == cat) "" else cat
        _state.value = _state.value.copy(categoria = nuova)
        cerca()
    }

    fun apriDettaglio(c: Curiosity) {
        _state.value = _state.value.copy(dettaglio = c)
    }

    fun chiudiDettaglio() {
        _state.value = _state.value.copy(dettaglio = null)
    }

    fun rimuoviBookmark(c: Curiosity) {
        viewModelScope.launch {
            repo.rimuoviBookmark(c)
            chiudiDettaglio()
            carica()
        }
    }

    private fun cerca() {
        viewModelScope.launch {
            val risultati = repo.cercaBookmark(
                _state.value.query,
                _state.value.categoria
            )
            _state.value = _state.value.copy(risultati = risultati)
        }
    }

    class Factory(private val repo: CuriosityRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(c: Class<T>): T = BookmarkViewModel(repo) as T
    }
}