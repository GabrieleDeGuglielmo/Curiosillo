package com.example.curiosillo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
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
import com.example.curiosillo.firebase.FirebaseManager
import com.example.curiosillo.repository.CuriosityRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ── Data ──────────────────────────────────────────────────────────────────────

data class VotoConTitolo(
    val externalId: String,
    val titolo:     String,
    val likes:      Long,
    val dislikes:   Long
)

data class AdminVotiUiState(
    val isLoading: Boolean             = true,
    val isAdmin:   Boolean             = false,
    val voti:      List<VotoConTitolo> = emptyList()
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

class AdminVotiViewModel(private val repo: CuriosityRepository) : ViewModel() {

    private val _state = MutableStateFlow(AdminVotiUiState())
    val state: StateFlow<AdminVotiUiState> = _state.asStateFlow()

    init { carica() }

    fun carica() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val isAdmin = FirebaseManager.isAdmin()
            if (!isAdmin) { _state.value = AdminVotiUiState(isLoading = false, isAdmin = false); return@launch }

            val votiFirestore    = FirebaseManager.caricaTuttiVoti()
            val tutteLeCuriosita = repo.getTutteLeCuriosita()
            val mappa            = tutteLeCuriosita.associateBy { it.externalId ?: "" }

            _state.value = AdminVotiUiState(
                isLoading = false,
                isAdmin   = true,
                voti      = votiFirestore.map { v ->
                    VotoConTitolo(
                        externalId = v.externalId,
                        titolo     = mappa[v.externalId]?.title ?: v.externalId,
                        likes      = v.likes,
                        dislikes   = v.dislikes
                    )
                }
            )
        }
    }

    class Factory(private val repo: CuriosityRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(c: Class<T>): T = AdminVotiViewModel(repo) as T
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminVotiScreen(nav: NavController) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as CuriosityApplication
    val vm: AdminVotiViewModel = viewModel(factory = AdminVotiViewModel.Factory(app.repository))
    val state by vm.state.collectAsState()

    val gradientBg = Brush.verticalGradient(listOf(
        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
        MaterialTheme.colorScheme.background
    ))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Voti curiosità", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Indietro")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { pad ->
        Box(Modifier.fillMaxSize().background(gradientBg).padding(pad)) {
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    CircularProgressIndicator()
                }
                !state.isAdmin -> AccessoNegatoContent()
                state.voti.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("📊", fontSize = 48.sp)
                        Spacer(Modifier.height(16.dp))
                        Text("Nessun voto ancora registrato.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f))
                    }
                }
                else -> {
                    val totLikes    = state.voti.sumOf { it.likes }
                    val totDislikes = state.voti.sumOf { it.dislikes }
                    LazyColumn(
                        contentPadding      = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape    = RoundedCornerShape(18.dp),
                                colors   = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Row(
                                    Modifier.fillMaxWidth().padding(20.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    TotaleChip("👍 $totLikes",    "Like totali",      Color(0xFF4CAF50))
                                    TotaleChip("👎 $totDislikes", "Dislike totali",   Color(0xFFF44336))
                                    TotaleChip("📊 ${state.voti.size}", "Curiosità votate", MaterialTheme.colorScheme.primary)
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                        }
                        items(state.voti, key = { it.externalId }) { voto ->
                            VotoCard(voto)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun VotoCard(voto: VotoConTitolo) {
    val totale   = voto.likes + voto.dislikes
    val likesPct = if (totale > 0) voto.likes.toFloat() / totale else 0f

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {

            // ── Titolo + contatori ────────────────────────────────────────────
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(voto.titolo,
                        style      = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines   = 2,
                        overflow   = TextOverflow.Ellipsis)
                    Text(voto.externalId,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                }
                Spacer(Modifier.width(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ThumbUp, null, Modifier.size(16.dp), tint = Color(0xFF4CAF50))
                        Spacer(Modifier.width(4.dp))
                        Text("${voto.likes}", fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ThumbDown, null, Modifier.size(16.dp), tint = Color(0xFFF44336))
                        Spacer(Modifier.width(4.dp))
                        Text("${voto.dislikes}", fontWeight = FontWeight.Bold, color = Color(0xFFF44336))
                    }
                }
            }

            // ── Barra percentuale like ────────────────────────────────────────
            if (totale > 0) {
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    LinearProgressIndicator(
                        progress   = { likesPct },
                        modifier   = Modifier.weight(1f).height(6.dp),
                        color      = Color(0xFF4CAF50),
                        trackColor = Color(0xFFF44336)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("${(likesPct * 100).toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
        }
    }
}

@Composable
fun AccessoNegatoContent() {
    Box(Modifier.fillMaxSize(), Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text("🔒", fontSize = 48.sp)
            Spacer(Modifier.height(16.dp))
            Text("Accesso negato", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Non hai i permessi per visualizzare questa sezione.",
                style     = MaterialTheme.typography.bodyMedium,
                color     = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun TotaleChip(valore: String, etichetta: String, colore: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(valore, fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = colore)
        Text(etichetta, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
    }
}