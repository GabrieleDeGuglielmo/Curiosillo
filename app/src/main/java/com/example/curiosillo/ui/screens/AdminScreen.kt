package com.example.curiosillo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Search
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
import androidx.lifecycle.Lifecycle
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
import java.util.Date
import java.util.Locale

// ── Data ──────────────────────────────────────────────────────────────────────

data class SegnalazioneConInfo(
    val externalId: String,
    val titolo:     String,
    val segnalazioni: List<FirebaseManager.SegnalazioneItem>
)

enum class FiltroSegnalazioni { TUTTE, NON_LETTE }

data class AdminSegnalazioniUiState(
    val isLoading:    Boolean                       = true,
    val isAdmin:      Boolean                       = false,
    val segnalazioni: List<SegnalazioneConInfo>     = emptyList()
)

// ── ViewModel ─────────────────────────────────────────────────────────────────

class AdminVotiViewModel(private val repo: CuriosityRepository) : ViewModel() {

    private val _state = MutableStateFlow(AdminSegnalazioniUiState())
    val state: StateFlow<AdminSegnalazioniUiState> = _state.asStateFlow()

    init { carica() }

    fun carica() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val isAdmin = FirebaseManager.isAdmin()
            if (!isAdmin) {
                _state.value = AdminSegnalazioniUiState(isLoading = false, isAdmin = false)
                return@launch
            }

            val tutteCuriosita = repo.getTutteLeCuriosita()
            val mappa = tutteCuriosita.associateBy { it.externalId ?: "" }

            // Carica tutti i documenti radice segnalazioni
            val radici = try {
                FirebaseManager.caricaTutteSegnalazioni()
            } catch (_: Exception) { emptyList() }

            val lista = radici.mapNotNull { radice ->
                val items = FirebaseManager.caricaSegnalazioniPerCuriosita(radice.externalId)
                if (items.isEmpty()) return@mapNotNull null
                SegnalazioneConInfo(
                    externalId   = radice.externalId,
                    titolo       = mappa[radice.externalId]?.title ?: radice.externalId,
                    segnalazioni = items
                )
            }.sortedByDescending { segnalazione ->
                segnalazione.segnalazioni.count { !it.letta }
            }

            _state.value = AdminSegnalazioniUiState(
                isLoading    = false,
                isAdmin      = true,
                segnalazioni = lista
            )
        }
    }

    fun segnaLetta(externalId: String, segnalazioneId: String) {
        viewModelScope.launch {
            FirebaseManager.segnaSegnalazioneLetta(externalId, segnalazioneId)
            _state.value = _state.value.copy(
                segnalazioni = _state.value.segnalazioni.map { s ->
                    if (s.externalId != externalId) s
                    else s.copy(segnalazioni = s.segnalazioni.map { item ->
                        if (item.id == segnalazioneId) item.copy(letta = true) else item
                    })
                }.filter { it.segnalazioni.any { item -> !item.letta } ||
                        it.segnalazioni.isNotEmpty() }
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

    var query  by remember { mutableStateOf("") }
    var filtro by remember { mutableStateOf(FiltroSegnalazioni.NON_LETTE) }
    var aperta by remember { mutableStateOf<SegnalazioneConInfo?>(null) }

    val gradientBg = Brush.verticalGradient(listOf(
        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
        MaterialTheme.colorScheme.background
    ))

    // Detail bottom sheet
    aperta?.let { info ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
        ModalBottomSheet(
            onDismissRequest = { aperta = null },
            sheetState       = sheetState
        ) {
            SegnalazioniDetailSheet(
                info     = info,
                onSegnaLetta = { id -> vm.segnaLetta(info.externalId, id) },
                onChiudi     = { aperta = null }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Segnalazioni", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = {
                        if (nav.currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
                            nav.popBackStack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Indietro")
                    }
                },
                actions = {
                    IconButton(onClick = { vm.carica() }) {
                        Icon(Icons.Default.Flag, "Aggiorna",
                            tint = MaterialTheme.colorScheme.primary)
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
                else -> {
                    val filtrate = remember(state.segnalazioni, query, filtro) {
                        var lista = state.segnalazioni
                        if (filtro == FiltroSegnalazioni.NON_LETTE)
                            lista = lista.filter { s -> s.segnalazioni.any { !it.letta } }
                        if (query.isNotBlank()) {
                            val q = query.trim().lowercase()
                            lista = lista.filter {
                                it.titolo.lowercase().contains(q) ||
                                        it.externalId.lowercase().contains(q)
                            }
                        }
                        lista
                    }

                    val totNonLette = state.segnalazioni.sumOf { s ->
                        s.segnalazioni.count { !it.letta }
                    }
                    val totTutte = state.segnalazioni.sumOf { it.segnalazioni.size }

                    LazyColumn(
                        contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // ── Card riepilogo ────────────────────────────────────
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
                                    TotaleChip(
                                        "🚩 $totNonLette",
                                        "Non lette",
                                        MaterialTheme.colorScheme.error
                                    )
                                    TotaleChip(
                                        "📋 $totTutte",
                                        "Totale",
                                        MaterialTheme.colorScheme.primary
                                    )
                                    TotaleChip(
                                        "📚 ${state.segnalazioni.size}",
                                        "Curiosità",
                                        Color(0xFF7B2D8B)
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }

                        // ── Ricerca ───────────────────────────────────────────
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

                        // ── Filtri ────────────────────────────────────────────
                        item {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = filtro == FiltroSegnalazioni.NON_LETTE,
                                    onClick  = { filtro = FiltroSegnalazioni.NON_LETTE },
                                    label    = { Text("🚩 Non lette") }
                                )
                                FilterChip(
                                    selected = filtro == FiltroSegnalazioni.TUTTE,
                                    onClick  = { filtro = FiltroSegnalazioni.TUTTE },
                                    label    = { Text("Tutte") }
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${filtrate.size} curiosità con segnalazioni",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                        }

                        // ── Lista ─────────────────────────────────────────────
                        if (filtrate.isEmpty()) {
                            item {
                                Box(
                                    Modifier.fillMaxWidth().padding(top = 48.dp),
                                    Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(if (totNonLette == 0) "🎉" else "🔍", fontSize = 40.sp)
                                        Spacer(Modifier.height(12.dp))
                                        Text(
                                            if (totNonLette == 0 && filtro == FiltroSegnalazioni.NON_LETTE)
                                                "Nessuna segnalazione non letta"
                                            else "Nessun risultato",
                                            style     = MaterialTheme.typography.bodyMedium,
                                            color     = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        } else {
                            items(filtrate, key = { it.externalId }) { info ->
                                SegnalazioneCard(
                                    info    = info,
                                    query   = query,
                                    onClick = { aperta = info }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ── SegnalazioneCard ──────────────────────────────────────────────────────────

@Composable
private fun SegnalazioneCard(
    info:    SegnalazioneConInfo,
    query:   String,
    onClick: () -> Unit
) {
    val nonLette = info.segnalazioni.count { !it.letta }
    val hasNonLette = nonLette > 0

    Card(
        modifier  = Modifier.fillMaxWidth().clickable { onClick() },
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(
            containerColor = if (hasNonLette)
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
            else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(if (hasNonLette) 3.dp else 1.dp)
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    highlight(info.titolo, query),
                    style      = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines   = 2,
                    overflow   = TextOverflow.Ellipsis
                )
                Text(
                    highlight(info.externalId, query),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
                if (info.segnalazioni.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    // Mostra i tipi unici delle segnalazioni
                    val tipi = info.segnalazioni.map { it.tipo }.distinct().take(3)
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        tipi.forEach { tipo ->
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Text(
                                    tipo,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    style    = MaterialTheme.typography.labelSmall,
                                    color    = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.width(12.dp))
            // Badge contatore non lette
            if (hasNonLette) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.error
                ) {
                    Text(
                        nonLette.toString(),
                        modifier   = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        color      = Color.White,
                        fontWeight = FontWeight.Bold,
                        style      = MaterialTheme.typography.labelMedium
                    )
                }
            } else {
                Text(
                    "${info.segnalazioni.size}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }
    }
}

// ── Detail sheet ──────────────────────────────────────────────────────────────

@Composable
private fun SegnalazioniDetailSheet(
    info:         SegnalazioneConInfo,
    onSegnaLetta: (String) -> Unit,
    onChiudi:     () -> Unit
) {
    val fmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.ITALY)

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp)
    ) {
        // Header
        Text(
            info.titolo,
            style      = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            maxLines   = 2,
            overflow   = TextOverflow.Ellipsis
        )
        Text(
            info.externalId,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        val nonLette = info.segnalazioni.filter { !it.letta }
        val lette    = info.segnalazioni.filter { it.letta }

        if (nonLette.isNotEmpty()) {
            Text(
                "🚩 Da leggere (${nonLette.size})",
                style      = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.error,
                modifier   = Modifier.padding(bottom = 8.dp)
            )
            nonLette.forEach { item ->
                SegnalazioneItemCard(item = item, fmt = fmt, onSegnaLetta = onSegnaLetta)
                Spacer(Modifier.height(8.dp))
            }
        }

        if (lette.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Text(
                "✓ Già lette (${lette.size})",
                style      = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                modifier   = Modifier.padding(bottom = 8.dp)
            )
            lette.forEach { item ->
                SegnalazioneItemCard(item = item, fmt = fmt, onSegnaLetta = null)
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun SegnalazioneItemCard(
    item:         FirebaseManager.SegnalazioneItem,
    fmt:          SimpleDateFormat,
    onSegnaLetta: ((String) -> Unit)?
) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(12.dp),
        colors    = CardDefaults.cardColors(
            containerColor = if (!item.letta)
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.Top
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        item.tipo,
                        modifier   = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style      = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color      = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Text(
                    fmt.format(Date(item.creatoAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
            if (item.testo.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    item.testo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
            if (onSegnaLetta != null) {
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(
                        onClick       = { onSegnaLetta(item.id) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text("Segna come letta", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

// ── Helpers ───────────────────────────────────────────────────────────────────

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
            Text(
                "Non hai i permessi per visualizzare questa sezione.",
                style     = MaterialTheme.typography.bodyMedium,
                color     = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun TotaleChip(valore: String, etichetta: String, colore: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(valore, fontSize = 20.sp, color = colore, fontWeight = FontWeight.Bold)
        Text(
            etichetta,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}