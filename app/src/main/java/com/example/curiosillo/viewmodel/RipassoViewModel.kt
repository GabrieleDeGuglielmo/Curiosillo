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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class RipassoUiState(
    val pillole:          List<Curiosity> = emptyList(),
    val giorniSelezionati: Int            = 7,
    val indiceCorrente:   Int             = 0,
    val isLoading:        Boolean         = true,
    val nota:             String          = ""
)

class RipassoViewModel(
    private val repo:  CuriosityRepository,
    private val prefs: CategoryPreferences
) : ViewModel() {

    private val _state = MutableStateFlow(RipassoUiState())
    val state: StateFlow<RipassoUiState> = _state.asStateFlow()

    init { carica(7) }

    fun carica(giorniMin: Int) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, giorniSelezionati = giorniMin)
            val categorie = prefs.categorieAttive.first()
            val pillole = repo.getPerRipasso(giorniMin, categorie)
            _state.value = _state.value.copy(
                pillole       = pillole,
                indiceCorrente = 0,
                isLoading     = false,
                nota          = pillole.firstOrNull()?.nota ?: ""
            )
        }
    }

    fun prossima() {
        val nuovoIndice = _state.value.indiceCorrente + 1
        val pillole     = _state.value.pillole
        _state.value = _state.value.copy(
            indiceCorrente = nuovoIndice,
            nota           = pillole.getOrNull(nuovoIndice)?.nota ?: ""
        )
    }

    fun precedente() {
        val nuovoIndice = (_state.value.indiceCorrente - 1).coerceAtLeast(0)
        val pillole     = _state.value.pillole
        _state.value = _state.value.copy(
            indiceCorrente = nuovoIndice,
            nota           = pillole.getOrNull(nuovoIndice)?.nota ?: ""
        )
    }

    fun salvaNota(testo: String) {
        val pillola = pilloleCorrente() ?: return
        viewModelScope.launch {
            repo.salvaNota(pillola, testo)
            val pilloleAggiornate = _state.value.pillole.toMutableList()
            pilloleAggiornate[_state.value.indiceCorrente] = pillola.copy(nota = testo)
            _state.value = _state.value.copy(pillole = pilloleAggiornate, nota = testo)
        }
    }

    fun pilloleCorrente(): Curiosity? =
        _state.value.pillole.getOrNull(_state.value.indiceCorrente)

    class Factory(
        private val repo:  CuriosityRepository,
        private val prefs: CategoryPreferences
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(c: Class<T>): T = RipassoViewModel(repo, prefs) as T
    }
}
