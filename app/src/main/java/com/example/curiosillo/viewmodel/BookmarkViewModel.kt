package com.example.curiosillo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.curiosillo.data.Curiosity
import com.example.curiosillo.firebase.FirebaseManager
import com.example.curiosillo.repository.CuriosityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class BookmarkUiState(
    val risultati:  List<Curiosity> = emptyList(),
    val categorie:  List<String>    = emptyList(),
    val query:      String          = "",
    val categorieSelezionate: Set<String> = emptySet(),
    val isLoading:  Boolean         = true,
    val dettaglio:  Curiosity?      = null,  // pillola aperta nel bottom sheet
    val idInRimozione: Set<Int>     = emptySet() // card in uscita con animazione
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
                _state.value.categorieSelezionate
            )
            _state.value = _state.value.copy(
                risultati  = risultati,
                categorie  = categorie,
                isLoading  = false
            )
        }
    }

    fun salvaNota(pillola: Curiosity, testo: String) {
        viewModelScope.launch {
            repo.salvaNota(pillola, testo)
            carica()
        }
    }

    fun onQueryChange(nuova: String) {
        _state.value = _state.value.copy(query = nuova)
        cerca()
    }

    fun onCategoriaSelezionata(cat: String) {
        val correnti = _state.value.categorieSelezionate.toMutableSet()
        if (correnti.contains(cat)) correnti.remove(cat) else correnti.add(cat)
        _state.value = _state.value.copy(categorieSelezionate = correnti)
        cerca()
    }

    fun resetCategorie() {
        _state.value = _state.value.copy(categorieSelezionate = emptySet())
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
            // Segna la card come in uscita per avviare AnimatedVisibility
            _state.value = _state.value.copy(
                idInRimozione = _state.value.idInRimozione + c.id
            )
            delay(280) // attende la fine dell'animazione
            repo.rimuoviBookmark(c)
            carica()
            _state.value = _state.value.copy(
                idInRimozione = _state.value.idInRimozione - c.id
            )
        }
    }

    fun inviaSegnalazione(pillola: Curiosity, tipo: String, testo: String) {
        val externalId = pillola.externalId ?: return
        viewModelScope.launch {
            FirebaseManager.inviaSegnalazione(externalId, tipo, testo)
        }
    }

    private fun cerca() {
        viewModelScope.launch {
            val risultati = repo.cercaBookmark(
                _state.value.query,
                _state.value.categorieSelezionate
            )
            _state.value = _state.value.copy(risultati = risultati)
        }
    }

    class Factory(private val repo: CuriosityRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(c: Class<T>): T = BookmarkViewModel(repo) as T
    }
}
