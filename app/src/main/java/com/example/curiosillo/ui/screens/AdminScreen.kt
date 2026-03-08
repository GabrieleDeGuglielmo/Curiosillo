package com.example.curiosillo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
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

enum class FiltroVoti { TUTTI, SOLO_NEGATIVI }

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

    var query   by remember { mutableStateOf("") }
    var filtro  by remember { mutableStateOf(FiltroVoti.TUTTI) }

    val gradientBg = Brush.verticalGradient(listOf(
        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
        MaterialTheme.colorScheme.background
    ))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Voti curiosità") },
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
                    // Applica filtri in memoria
                    val filtrati = remember(state.voti, query, filtro) {
                        var lista = state.voti
                        if (filtro == FiltroVoti.SOLO_NEGATIVI)
                            lista = lista.filter { it.dislikes > 0 }
                        if (query.isNotBlank()) {
                            val q = query.trim().lowercase()
                            lista = lista.filter {
                                it.titolo.lowercase().contains(q) ||
                                        it.externalId.lowercase().contains(q)
                            }
                        }
                        lista
                    }

                    val totLikes    = state.voti.sumOf { it.likes }
                    val totDislikes = state.voti.sumOf { it.dislikes }

                    LazyColumn(
                        contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // ── Card riepilogo globale ────────────────────────────
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
                            Spacer(Modifier.height(8.dp))
                        }

                        // ── Barra ricerca ─────────────────────────────────────
                        item {
                            OutlinedTextField(
                                value         = query,
                                onValueChange = { query = it },
                                modifier      = Modifier.fillMaxWidth(),
                                placeholder   = { Text("Cerca per ID o titolo…") },
                                leadingIcon   = { Icon(Icons.Default.Search, null) },
                                trailingIcon  = {
                                    if (query.isNotBlank()) {
                                        IconButton(onClick = { query = "" }) {
                                            Icon(Icons.Default.Close, "Cancella")
                                        }
                                    }
                                },
                                singleLine = true,
                                shape      = RoundedCornerShape(14.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                        }

                        // ── Chip filtro ───────────────────────────────────────
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = filtro == FiltroVoti.TUTTI,
                                    onClick  = { filtro = FiltroVoti.TUTTI },
                                    label    = { Text("Tutte") }
                                )
                                FilterChip(
                                    selected = filtro == FiltroVoti.SOLO_NEGATIVI,
                                    onClick  = { filtro = FiltroVoti.SOLO_NEGATIVI },
                                    label    = { Text("👎 Con almeno un dislike") }
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                if (query.isBlank() && filtro == FiltroVoti.TUTTI)
                                    "${state.voti.size} curiosità votate"
                                else "${filtrati.size} risultati",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                        }

                        // ── Lista filtrata ────────────────────────────────────
                        if (filtrati.isEmpty()) {
                            item {
                                Box(
                                    Modifier.fillMaxWidth().padding(top = 48.dp),
                                    Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("🔍", fontSize = 40.sp)
                                        Spacer(Modifier.height(12.dp))
                                        Text(
                                            "Nessun risultato",
                                            style     = MaterialTheme.typography.bodyMedium,
                                            color     = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        } else {
                            items(filtrati, key = { it.externalId }) { voto ->
                                VotoCard(voto, query)
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── Highlight helper ──────────────────────────────────────────────────────────

private fun highlight(text: String, query: String): AnnotatedString {
    if (query.isBlank()) return AnnotatedString(text)
    return buildAnnotatedString {
        val lower  = text.lowercase()
        val q      = query.trim().lowercase()
        var cursor = 0
        while (cursor < text.length) {
            val idx = lower.indexOf(q, cursor)
            if (idx < 0) { append(text.substring(cursor)); break }
            append(text.substring(cursor, idx))
            withStyle(SpanStyle(
                background = Color(0xFFFFEB3B).copy(alpha = 0.5f),
                fontWeight = FontWeight.Bold
            )) { append(text.substring(idx, idx + q.length)) }
            cursor = idx + q.length
        }
    }
}

// ── VotoCard ──────────────────────────────────────────────────────────────────

@Composable
private fun VotoCard(voto: VotoConTitolo, query: String = "") {
    val totale   = voto.likes + voto.dislikes
    val likesPct = if (totale > 0) voto.likes.toFloat() / totale else 0f
    val isNegativo = voto.dislikes > voto.likes

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(
            containerColor = if (isNegativo)
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(highlight(voto.titolo, query),
                        style      = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines   = 2,
                        overflow   = TextOverflow.Ellipsis)
                    Text(highlight(voto.externalId, query),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                }
                Spacer(Modifier.width(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ThumbUp, null, Modifier.size(16.dp), tint = Color(0xFF4CAF50))
                        Spacer(Modifier.width(4.dp))
                        Text("${voto.likes}", color = Color(0xFF4CAF50))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.ThumbDown, null, Modifier.size(16.dp), tint = Color(0xFFF44336))
                        Spacer(Modifier.width(4.dp))
                        Text("${voto.dislikes}", color = Color(0xFFF44336))
                    }
                }
            }
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
            Text("Accesso negato", style = MaterialTheme.typography.headlineSmall)
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
        Text(valore, fontSize = 20.sp, color = colore)
        Text(etichetta, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
    }
}