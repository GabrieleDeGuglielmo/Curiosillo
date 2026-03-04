package com.example.curiosillo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.Search
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.curiosillo.CuriosityApplication
import com.example.curiosillo.data.Curiosity
import com.example.curiosillo.ui.theme.Error
import com.example.curiosillo.ui.theme.Primary
import com.example.curiosillo.viewmodel.BookmarkViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarkScreen(nav: NavController) {
    val ctx  = LocalContext.current
    val repo = (ctx.applicationContext as CuriosityApplication).repository
    val vm: BookmarkViewModel = viewModel(factory = BookmarkViewModel.Factory(repo))
    val state by vm.state.collectAsState()

    // Bottom sheet dettaglio pillola
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    state.dettaglio?.let { pillola ->
        ModalBottomSheet(
            onDismissRequest = { vm.chiudiDettaglio() },
            sheetState       = sheetState
        ) {
            DettaglioSheet(
                pillola         = pillola,
                onRimuovi       = { vm.rimuoviBookmark(pillola) },
                onChiudi        = { vm.chiudiDettaglio() }
            )
        }
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("I miei preferiti", fontWeight = FontWeight.SemiBold) },
            navigationIcon = {
                IconButton(onClick = { nav.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, "Indietro")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )
    }) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFFFFF3E0), Color.White)))
                .padding(pad)
        ) {
            // ── Barra di ricerca ──────────────────────────────────────────────
            OutlinedTextField(
                value         = state.query,
                onValueChange = { vm.onQueryChange(it) },
                placeholder   = { Text("Cerca tra i preferiti...") },
                leadingIcon   = { Icon(Icons.Default.Search, null) },
                singleLine    = true,
                shape         = RoundedCornerShape(16.dp),
                modifier      = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // ── Filtri categoria ──────────────────────────────────────────────
            if (state.categorie.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Chip "Tutte"
                    FilterChip(
                        selected = state.categoria == "",
                        onClick  = { vm.onCategoriaSelezionata("") },
                        label    = { Text("Tutte") }
                    )
                    state.categorie.forEach { cat ->
                        FilterChip(
                            selected = state.categoria == cat,
                            onClick  = { vm.onCategoriaSelezionata(cat) },
                            label    = { Text(cat) },
                            colors   = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = coloreCategoria(cat),
                                selectedLabelColor     = Color.White
                            )
                        )
                    }
                }
            }

            Divider(modifier = Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(4.dp))

            // ── Contenuto ─────────────────────────────────────────────────────
            when {
                state.isLoading -> {
                    CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
                }
                state.risultati.isEmpty() -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("🔖", fontSize = 56.sp)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            if (state.query.isNotEmpty() || state.categoria.isNotEmpty())
                                "Nessun risultato trovato.\nProva a cambiare i filtri."
                            else
                                "Non hai ancora salvato nessuna pillola.\nPremi il segnalibro durante la lettura!",
                            style     = MaterialTheme.typography.bodyLarge,
                            color     = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier  = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                }
                else -> {
                    Text(
                        "${state.risultati.size} pillol${if (state.risultati.size == 1) "a" else "e"} salvat${if (state.risultati.size == 1) "a" else "e"}",
                        style    = MaterialTheme.typography.bodySmall,
                        color    = Color.Gray,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                    )
                    LazyColumn(
                        contentPadding        = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement   = Arrangement.spacedBy(10.dp)
                    ) {
                        items(state.risultati, key = { it.id }) { pillola ->
                            BookmarkCard(pillola) { vm.apriDettaglio(pillola) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun BookmarkCard(pillola: Curiosity, onClick: () -> Unit) {
    Card(
        onClick   = onClick,
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Pallino colorato per categoria
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(coloreCategoria(pillola.category),
                        androidx.compose.foundation.shape.CircleShape)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    pillola.title,
                    style      = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines   = 2,
                    overflow   = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    pillola.category,
                    style = MaterialTheme.typography.bodySmall,
                    color = coloreCategoria(pillola.category)
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Default.Bookmark, null,
                tint = Primary, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun DettaglioSheet(
    pillola:   Curiosity,
    onRimuovi: () -> Unit,
    onChiudi:  () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp)
    ) {
        // Categoria
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(coloreCategoria(pillola.category),
                        androidx.compose.foundation.shape.CircleShape)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                pillola.category,
                style = MaterialTheme.typography.labelLarge,
                color = coloreCategoria(pillola.category),
                fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(10.dp))

        // Titolo
        Text(
            pillola.title,
            style      = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color      = Color(0xFF1C1B1F)
        )
        Spacer(Modifier.height(14.dp))

        // Testo completo
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape    = RoundedCornerShape(14.dp),
            colors   = CardDefaults.cardColors(containerColor = Color(0xFFFFF8F0)),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Text(
                pillola.body,
                modifier   = Modifier.padding(16.dp),
                style      = MaterialTheme.typography.bodyLarge,
                lineHeight = 26.sp,
                color      = Color(0xFF3C3C3C)
            )
        }
        Spacer(Modifier.height(24.dp))

        // Pulsante rimuovi bookmark
        OutlinedButton(
            onClick  = onRimuovi,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape    = RoundedCornerShape(14.dp),
            colors   = ButtonDefaults.outlinedButtonColors(contentColor = Error),
            border   = ButtonDefaults.outlinedButtonBorder.copy(
                brush = androidx.compose.ui.graphics.SolidColor(Error)
            )
        ) {
            Icon(Icons.Default.BookmarkRemove, null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Rimuovi dai preferiti", fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(8.dp))

        // Pulsante chiudi
        TextButton(
            onClick  = onChiudi,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Chiudi", color = Color.Gray)
        }
    }
}