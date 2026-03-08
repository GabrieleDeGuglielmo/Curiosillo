package com.example.curiosillo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.curiosillo.CuriosityApplication
import com.example.curiosillo.data.Curiosity
import com.example.curiosillo.repository.CuriosityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ── ViewModel ─────────────────────────────────────────────────────────────────

data class PilloleNascosteUiState(
    val pillole:   List<Curiosity> = emptyList(),
    val isLoading: Boolean         = true
)

class PilloleNascosteViewModel(private val repo: CuriosityRepository) : ViewModel() {

    private val _state = MutableStateFlow(PilloleNascosteUiState())
    val state: StateFlow<PilloleNascosteUiState> = _state.asStateFlow()

    init { carica() }

    private fun carica() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            _state.value = PilloleNascosteUiState(
                pillole   = repo.getPilloleIgnorate(),
                isLoading = false
            )
        }
    }

    fun ripristina(c: Curiosity) {
        viewModelScope.launch {
            repo.ripristinaIgnorata(c)
            carica()
        }
    }

    fun ripristinaTutte() {
        viewModelScope.launch {
            _state.value.pillole.forEach { repo.ripristinaIgnorata(it) }
            carica()
        }
    }

    class Factory(private val repo: CuriosityRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(c: Class<T>): T =
            PilloleNascosteViewModel(repo) as T
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PilloleNascosteScreen(nav: NavController) {
    val ctx  = LocalContext.current
    val repo = (ctx.applicationContext as CuriosityApplication).repository
    val vm: PilloleNascosteViewModel = viewModel(factory = PilloleNascosteViewModel.Factory(repo))
    val state by vm.state.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope             = rememberCoroutineScope()

    var showConfirmRipristinaTutte by remember { mutableStateOf(false) }

    if (showConfirmRipristinaTutte) {
        AlertDialog(
            onDismissRequest = { showConfirmRipristinaTutte = false },
            icon  = { Icon(Icons.Default.RestartAlt, null, tint = MaterialTheme.colorScheme.primary) },
            title = { Text("Ripristina tutte?", fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
            text  = { Text("Tutte le ${state.pillole.size} pillole nascoste torneranno nella coda di lettura.",
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)) },
            confirmButton = {
                Button(onClick = {
                    showConfirmRipristinaTutte = false
                    vm.ripristinaTutte()
                    scope.launch { snackbarHostState.showSnackbar("Tutte le pillole sono state ripristinate!") }
                }) { Text("Ripristina tutte", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                OutlinedButton(onClick = { showConfirmRipristinaTutte = false }) { Text("Annulla") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pillole nascoste", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Indietro")
                    }
                },
                actions = {
                    if (state.pillole.isNotEmpty()) {
                        IconButton(onClick = { showConfirmRipristinaTutte = true }) {
                            Icon(Icons.Default.RestartAlt, "Ripristina tutte",
                                tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { pad ->
        val gradientBg = Brush.verticalGradient(listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
            MaterialTheme.colorScheme.background
        ))

        Column(Modifier.fillMaxSize().background(gradientBg).padding(pad)) {
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.pillole.isEmpty() -> Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("🙈", fontSize = 56.sp)
                    Spacer(Modifier.height(16.dp))
                    Text("Nessuna pillola nascosta.\nLe curiosità che nascondi appariranno qui.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                        textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
                }
                else -> {
                    Text(
                        "${state.pillole.size} pillol${if (state.pillole.size == 1) "a nascosta" else "e nascoste"}",
                        style    = MaterialTheme.typography.bodySmall,
                        color    = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                    )
                    LazyColumn(
                        contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(state.pillole, key = { it.id }) { pillola ->
                            PilolaNascostaCard(pillola) {
                                vm.ripristina(pillola)
                                scope.launch { snackbarHostState.showSnackbar("Pillola ripristinata!") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PilolaNascostaCard(pillola: Curiosity, onRipristina: () -> Unit) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).background(
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f), CircleShape))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(pillola.title, style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Spacer(Modifier.height(2.dp))
                Text(pillola.category, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
            }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = onRipristina, shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)) {
                Icon(Icons.Default.Visibility, null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Ripristina", style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}