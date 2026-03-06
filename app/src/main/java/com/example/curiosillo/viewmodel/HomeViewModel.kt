package com.example.curiosillo.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.curiosillo.data.ContentPreferences
import com.example.curiosillo.network.AppUpdateService
import com.example.curiosillo.network.ChangelogService
import com.example.curiosillo.network.ContentSyncService
import com.example.curiosillo.network.VersioneChangelog
import com.example.curiosillo.repository.CuriosityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class UpdateInfo(
    val versione:    String,
    val downloadUrl: String,
    val releaseUrl:  String
)

data class HomeUiState(
    val syncInCorso:         Boolean                    = false,
    val syncMessaggio:       String?                    = null,
    val aggiornamentoApp:    UpdateInfo?                = null,
    val changelogDaMostrare: List<VersioneChangelog>?   = null,
    val changelogCompleto:   List<VersioneChangelog>    = emptyList()
)

class HomeViewModel(
    private val repo:         CuriosityRepository,
    private val contentPrefs: ContentPreferences,
    private val context:      Context
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private val syncService      = ContentSyncService(repo, contentPrefs)
    private val updateService    = AppUpdateService()
    private val changelogService = ChangelogService()

    init {
        syncContenuti()
        checkAggiornamentoApp()
        caricaChangelog()
    }

    fun syncContenuti() {
        viewModelScope.launch {
            _state.value = _state.value.copy(syncInCorso = true, syncMessaggio = null)
            val result = syncService.sync()
            val msg = when (result) {
                is ContentSyncService.SyncResult.Success ->
                    if (result.nuove > 0) "🎉 ${result.nuove} nuove pillole disponibili!" else null
                else -> null
            }
            _state.value = _state.value.copy(syncInCorso = false, syncMessaggio = msg)
        }
    }

    private fun checkAggiornamentoApp() {
        viewModelScope.launch {
            try {
                val versioneCorrente = context.packageManager
                    .getPackageInfo(context.packageName, 0)
                    .versionName ?: "1.0"
                val result = updateService.checkUpdate(versioneCorrente)
                if (result is AppUpdateService.UpdateResult.AggiornamentoDisponibile) {
                    _state.value = _state.value.copy(
                        aggiornamentoApp = UpdateInfo(
                            versione    = result.versione,
                            downloadUrl = result.downloadUrl,
                            releaseUrl  = result.releaseUrl
                        )
                    )
                }
            } catch (_: Exception) {}
        }
    }

    private fun caricaChangelog() {
        viewModelScope.launch {
            try {
                val lista = changelogService.scaricaChangelog()
                if (lista.isEmpty()) return@launch

                val versioneCorrente = context.packageManager
                    .getPackageInfo(context.packageName, 0)
                    .versionName ?: "1.0"
                val ultimaVista = contentPrefs.getUltimaVersioneVista()

                _state.value = _state.value.copy(changelogCompleto = lista)

                if (ultimaVista != versioneCorrente) {
                    val novita = if (ultimaVista.isBlank()) {
                        listOf(lista.first())
                    } else {
                        lista.takeWhile { it.versione != ultimaVista }
                    }
                    if (novita.isNotEmpty()) {
                        _state.value = _state.value.copy(changelogDaMostrare = novita)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun dismissChangelog() {
        viewModelScope.launch {
            val versioneCorrente = context.packageManager
                .getPackageInfo(context.packageName, 0)
                .versionName ?: "1.0"
            contentPrefs.setUltimaVersioneVista(versioneCorrente)
            _state.value = _state.value.copy(changelogDaMostrare = null)
        }
    }

    fun dismissAggiornamento() {
        _state.value = _state.value.copy(aggiornamentoApp = null)
    }

    fun dismissSyncMessaggio() {
        _state.value = _state.value.copy(syncMessaggio = null)
    }

    class Factory(
        private val repo:         CuriosityRepository,
        private val contentPrefs: ContentPreferences,
        private val context:      Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(c: Class<T>): T =
            HomeViewModel(repo, contentPrefs, context) as T
    }
}