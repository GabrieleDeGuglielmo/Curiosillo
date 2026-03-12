package com.example.curiosillo.viewmodel

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.curiosillo.data.AppDatabase
import com.example.curiosillo.data.ContentPreferences
import com.example.curiosillo.firebase.FirebaseManager
import com.example.curiosillo.network.AppUpdateService
import com.example.curiosillo.network.ChangelogService
import com.example.curiosillo.firebase.FirestoreSyncService
import com.example.curiosillo.network.VersioneChangelog
import com.example.curiosillo.repository.CuriosityRepository
import kotlinx.coroutines.delay
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
    val syncInCorso:         Boolean                              = false,
    val syncMessaggio:       String?                              = null,
    val aggiornamentoApp:    UpdateInfo?                          = null,
    val changelogDaMostrare: List<VersioneChangelog>?             = null,
    val changelogCompleto:   List<VersioneChangelog>              = emptyList(),
    val isOffline:           Boolean                              = false,
    val notifiche:           List<FirebaseManager.NotificaInApp>  = emptyList()
)

class HomeViewModel(
    private val repo:         CuriosityRepository,
    private val contentPrefs: ContentPreferences,
    private val context:      Context,
    private val dbRoom:       AppDatabase
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private val syncService      = FirestoreSyncService(repo, contentPrefs, dbRoom)
    private val updateService    = AppUpdateService()
    private val changelogService = ChangelogService()

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            _state.value = _state.value.copy(isOffline = false)
            refreshDatiCloud()
        }
        override fun onLost(network: Network) {
            _state.value = _state.value.copy(isOffline = true)
        }
    }

    init {
        val isConnected = connectivityManager.activeNetwork
            ?.let { connectivityManager.getNetworkCapabilities(it) }
            ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        _state.value = _state.value.copy(isOffline = !isConnected)

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)

        // All'avvio proviamo sempre a sincronizzare, anche con un piccolo delay 
        // per dare tempo alla connessione di stabilizzarsi
        refreshDatiCloud()
    }

    private fun refreshDatiCloud() {
        viewModelScope.launch {
            // Se il DB è vuoto, non serve aspettare la propagazione degli indici
            val dbVuoto = repo.totaleCuriosità() == 0
            if (!dbVuoto) delay(1500)
            
            syncContenuti()
            checkAggiornamentoApp()
            caricaChangelog()
            caricaNotifiche()
        }
    }

    override fun onCleared() {
        super.onCleared()
        try { connectivityManager.unregisterNetworkCallback(networkCallback) } catch (_: Exception) {}
    }

    fun syncContenuti() {
        if (_state.value.isOffline) return
        viewModelScope.launch {
            _state.value = _state.value.copy(syncInCorso = true, syncMessaggio = null)
            val result = syncService.sync()
            val msg = when (result) {
                is FirestoreSyncService.SyncResult.Success ->
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
                    .getPackageInfo(context.packageName, 0).versionName ?: "1.0"
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
                    .getPackageInfo(context.packageName, 0).versionName ?: "1.0"
                val ultimaVista = contentPrefs.getUltimaVersioneVista()
                _state.value = _state.value.copy(changelogCompleto = lista)
                if (ultimaVista != versioneCorrente) {
                    val novita = if (ultimaVista.isBlank()) listOf(lista.first())
                    else lista.takeWhile { it.versione != ultimaVista }
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
                .getPackageInfo(context.packageName, 0).versionName ?: "1.0"
            contentPrefs.setUltimaVersioneVista(versioneCorrente)
            _state.value = _state.value.copy(changelogDaMostrare = null)
        }
    }

    fun dismissAggiornamento()  { _state.value = _state.value.copy(aggiornamentoApp = null) }
    fun dismissSyncMessaggio()  { _state.value = _state.value.copy(syncMessaggio = null) }

    fun caricaNotifiche() {
        viewModelScope.launch {
            val notifiche = FirebaseManager.caricaNotifichePendenti()
            _state.value = _state.value.copy(notifiche = notifiche)
        }
    }

    fun segnaNotificaLetta(notificaId: String) {
        viewModelScope.launch {
            FirebaseManager.segnaNotificaLetta(notificaId)
            _state.value = _state.value.copy(
                notifiche = _state.value.notifiche.filter { it.id != notificaId }
            )
        }
    }

    fun segnaNotificheTutteLette() {
        viewModelScope.launch {
            FirebaseManager.segnaNotificheTutteLette(_state.value.notifiche)
            _state.value = _state.value.copy(notifiche = emptyList())
        }
    }

    fun dismissNotifiche() = segnaNotificheTutteLette()

    class Factory(
        private val repo:         CuriosityRepository,
        private val contentPrefs: ContentPreferences,
        private val context:      Context,
        private val dbRoom:       AppDatabase
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(c: Class<T>): T =
            HomeViewModel(repo, contentPrefs, context, dbRoom) as T
    }
}
