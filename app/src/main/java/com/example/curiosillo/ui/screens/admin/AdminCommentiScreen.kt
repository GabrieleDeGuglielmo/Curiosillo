package com.example.curiosillo.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.curiosillo.CuriosityApplication
import com.example.curiosillo.firebase.FirebaseManager
import com.example.curiosillo.repository.CuriosityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// ── ViewModel ─────────────────────────────────────────────────────────────────

data class PillolaConCommenti(
    val externalId: String,
    val titolo:     String,
    val commenti:   List<FirebaseManager.Commento>
)

data class AdminCommentiUiState(
    val isLoading: Boolean               = true,
    val isAdmin:   Boolean               = false,
    val dati:      List<PillolaConCommenti> = emptyList()
)

class AdminCommentiViewModel(private val repo: CuriosityRepository) : ViewModel() {

    private val _state = MutableStateFlow(AdminCommentiUiState())
    val state: StateFlow<AdminCommentiUiState> = _state.asStateFlow()

    init { carica() }

    fun carica() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            if (!FirebaseManager.isAdmin()) {
                _state.value = AdminCommentiUiState(isLoading = false, isAdmin = false)
                return@launch
            }

            // OTTIMIZZAZIONE: Recuperiamo tutti i commenti in un'unica query collectionGroup
            val tuttiICommenti = FirebaseManager.caricaTuttiICommentiModerazione()
            
            // Carichiamo i titoli delle pillole dal DB locale per visualizzarli
            val tutteCuriosita = repo.getTutteLeCuriosita()
            val mappaTitoli = tutteCuriosita.associateBy({ it.externalId ?: "" }, { it.title })

            val lista = tuttiICommenti.map { (exId, comms) ->
                PillolaConCommenti(
                    externalId = exId,
                    titolo     = mappaTitoli[exId] ?: "Curiosità rimossa ($exId)",
                    commenti   = comms
                )
            }.sortedByDescending { it.commenti.size }

            _state.value = AdminCommentiUiState(
                isLoading = false,
                isAdmin   = true,
                dati      = lista
            )
        }
    }

    fun eliminaCommento(externalId: String, commentoId: String, uid: String) {
        viewModelScope.launch {
            val res = FirebaseManager.eliminaCommento(externalId, commentoId, uid)
            if (res.isSuccess) {
                _state.value = _state.value.copy(
                    dati = _state.value.dati.mapNotNull { p ->
                        if (p.externalId != externalId) p
                        else {
                            val rimasti = p.commenti.filter { it.id != commentoId }
                            if (rimasti.isEmpty()) null
                            else p.copy(commenti = rimasti)
                        }
                    }
                )
            }
        }
    }

    class Factory(private val repo: CuriosityRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = AdminCommentiViewModel(repo) as T
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCommentiScreen(nav: NavController) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as CuriosityApplication
    val vm: AdminCommentiViewModel = viewModel(factory = AdminCommentiViewModel.Factory(app.repository))
    val state by vm.state.collectAsState()

    var query by remember { mutableStateOf("") }
    var pillolaSelezionata by remember { mutableStateOf<PillolaConCommenti?>(null) }

    val gradientBg = Brush.verticalGradient(listOf(
        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
        MaterialTheme.colorScheme.background
    ))

    pillolaSelezionata?.let { pillola ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
        ModalBottomSheet(
            onDismissRequest = { pillolaSelezionata = null },
            sheetState       = sheetState
        ) {
            CommentiPillolaSheet(
                pillola    = pillola,
                onElimina  = { commentoId, uid -> vm.eliminaCommento(pillola.externalId, commentoId, uid) },
                onVedi     = { nav.navigate("admin_curiosita/${pillola.externalId}"); pillolaSelezionata = null }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Moderazione Commenti") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Indietro")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { pad ->
        Box(Modifier.fillMaxSize().background(gradientBg).padding(pad)) {
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                !state.isAdmin  -> AccessoNegatoContent()
                else -> {
                    val filtrate = remember(state.dati, query) {
                        if (query.isBlank()) state.dati
                        else {
                            val q = query.trim().lowercase()
                            state.dati.filter { 
                                it.titolo.lowercase().contains(q) || 
                                it.externalId.lowercase().contains(q) ||
                                it.commenti.any { c -> c.testo.lowercase().contains(q) || c.autore.lowercase().contains(q) }
                            }
                        }
                    }

                    LazyColumn(
                        contentPadding      = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            OutlinedTextField(
                                value         = query,
                                onValueChange = { query = it },
                                modifier      = Modifier.fillMaxWidth(),
                                placeholder   = { Text("Cerca per titolo, ID o testo commento…") },
                                leadingIcon   = { Icon(Icons.Default.Search, null) },
                                singleLine    = true,
                                shape         = RoundedCornerShape(14.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "${state.dati.sumOf { it.commenti.size }} commenti in ${state.dati.size} curiosità",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                        }

                        if (filtrate.isEmpty()) {
                            item {
                                Box(Modifier.fillMaxWidth().padding(top = 40.dp), Alignment.Center) {
                                    Text("Nessun commento trovato", color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                                }
                            }
                        } else {
                            items(filtrate, key = { it.externalId }) { item ->
                                PillolaCommentiCard(item) { pillolaSelezionata = item }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PillolaCommentiCard(item: PillolaConCommenti, onClick: () -> Unit) {
    Card(
        onClick  = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape    = RoundedCornerShape(16.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(0.4f))
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(item.titolo, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(item.externalId, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
            }
            Spacer(Modifier.width(12.dp))
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    item.commenti.size.toString(),
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    style    = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color    = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
private fun CommentiPillolaSheet(
    pillola:   PillolaConCommenti,
    onElimina: (String, String) -> Unit,
    onVedi:    () -> Unit
) {
    val fmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALY)

    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(pillola.titolo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("Moderazione commenti", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onVedi) {
                Icon(Icons.Default.Visibility, "Vedi pillola", tint = MaterialTheme.colorScheme.primary)
            }
        }
        
        Spacer(Modifier.height(16.dp))

        pillola.commenti.forEach { c ->
            CommentoModerazioneItem(c, fmt, onElimina)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun CommentoModerazioneItem(
    c:         FirebaseManager.Commento,
    fmt:       SimpleDateFormat,
    onElimina: (String, String) -> Unit
) {
    var confirmDelete by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(0.5f))
            .padding(12.dp)
    ) {
        Column {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(c.autore, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(fmt.format(Date(c.timestamp)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(0.4f))
            }
            Spacer(Modifier.height(4.dp))
            Text(c.testo, style = MaterialTheme.typography.bodyMedium)
            
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                if (confirmDelete) {
                    TextButton(onClick = { confirmDelete = false }) { Text("Annulla") }
                    Button(
                        onClick = { onElimina(c.id, c.uid); confirmDelete = false },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Elimina definitivamente", style = MaterialTheme.typography.labelSmall) }
                } else {
                    IconButton(onClick = { confirmDelete = true }) {
                        Icon(Icons.Default.Delete, "Elimina", tint = MaterialTheme.colorScheme.error.copy(0.7f))
                    }
                }
            }
        }
    }
}
