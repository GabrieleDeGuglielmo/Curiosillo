package com.example.curiosillo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
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
import com.example.curiosillo.ui.components.NotaBottomSheet
import com.example.curiosillo.ui.screens.coloreCategoria
import com.example.curiosillo.ui.theme.Error
import com.example.curiosillo.viewmodel.BookmarkViewModel
import android.content.Intent
import androidx.compose.material.icons.filled.Share

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarkScreen(nav: NavController) {
    val ctx = LocalContext.current
    val repo = (ctx.applicationContext as CuriosityApplication).repository
    val vm: BookmarkViewModel = viewModel(factory = BookmarkViewModel.Factory(repo))
    val state by vm.state.collectAsState()
    var pillolaPerNota by remember { mutableStateOf<Curiosity?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    pillolaPerNota?.let { pillola ->
        NotaBottomSheet(
            notaAttuale = pillola.nota,
            onSalva = { testo -> vm.salvaNota(pillola, testo) },
            onChiudi = { pillolaPerNota = null }
        )
    }

    state.dettaglio?.let { pillola ->
        ModalBottomSheet(onDismissRequest = { vm.chiudiDettaglio() }, sheetState = sheetState) {
            val context = LocalContext.current

            DettaglioSheet(
                pillola     = pillola,
                onRimuovi   = { vm.rimuoviBookmark(pillola) },
                onNota      = { pillolaPerNota = pillola },
                onCondividi = {
                    val testo = "🧠 ${pillola.title}\n\n${pillola.body}\n\n— Curiosillo"
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, testo)
                    }
                    context.startActivity(Intent.createChooser(intent, "Condividi con..."))
                },
                onChiudi    = { vm.chiudiDettaglio() }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("I miei preferiti", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Indietro")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent
    ) { pad ->
        val gradientBg = Brush.verticalGradient(
            listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                MaterialTheme.colorScheme.background
            )
        )
        Column(Modifier
            .fillMaxSize()
            .background(gradientBg)
            .padding(pad)) {
            OutlinedTextField(
                value = state.query,
                onValueChange = { vm.onQueryChange(it) },
                placeholder = { Text("Cerca tra i preferiti...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (state.categorie.isNotEmpty()) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = state.categorieSelezionate.isEmpty(),
                        onClick = { vm.resetCategorie() }, label = { Text("Tutte") })
                    state.categorie.forEach { cat ->
                        FilterChip(
                            selected = state.categorieSelezionate.contains(cat),
                            onClick = { vm.onCategoriaSelezionata(cat) },
                            label = { Text(cat) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = coloreCategoria(cat),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }

            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(4.dp))

            when {
                state.isLoading ->
                    CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))

                state.risultati.isEmpty() -> {
                    Column(
                        Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("🔖", fontSize = 56.sp)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            if (state.query.isNotEmpty() || state.categorieSelezionate.isNotEmpty())
                                "Nessun risultato trovato.\nProva a cambiare i filtri."
                            else "Non hai ancora salvato nessuna pillola.\nPremi il segnalibro durante la lettura!",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(horizontal = 32.dp)
                        )
                    }
                }

                else -> {
                    Text(
                        "${state.risultati.size} pillol${if (state.risultati.size == 1) "a" else "e"} " +
                                "salvat${if (state.risultati.size == 1) "a" else "e"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                    )
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
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
        onClick = onClick, modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Row(
            Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier
                .size(10.dp)
                .background(coloreCategoria(pillola.category), CircleShape))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    pillola.title, style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    pillola.category, style = MaterialTheme.typography.bodySmall,
                    color = coloreCategoria(pillola.category)
                )
                if (pillola.nota.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "📝 " + pillola.nota,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.Default.Bookmark, null,
                tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun DettaglioSheet(
    pillola: Curiosity,
    onRimuovi: () -> Unit,
    onNota: () -> Unit,
    onCondividi: () -> Unit,
    onChiudi: () -> Unit
) {
    val noteCardBg = MaterialTheme.colorScheme.surfaceVariant
    val noteTextColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(Modifier
        .fillMaxWidth()
        .padding(horizontal = 24.dp)
        .padding(bottom = 32.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier
                .size(10.dp)
                .background(coloreCategoria(pillola.category), CircleShape))
            Spacer(Modifier.width(8.dp))
            Text(
                pillola.category, style = MaterialTheme.typography.labelLarge,
                color = coloreCategoria(pillola.category), fontWeight = FontWeight.SemiBold
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            pillola.title, style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(14.dp))
        Card(
            Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Text(
                pillola.body, Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyLarge, lineHeight = 26.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (pillola.nota.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Card(
                Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = noteCardBg),
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                    Text("📝 ", fontSize = 14.sp)
                    Text(
                        pillola.nota,
                        style = MaterialTheme.typography.bodySmall,
                        color = noteTextColor
                    )
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        OutlinedButton(
            onClick = onNota, modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.EditNote, null, Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (pillola.nota.isNotBlank()) "Modifica nota" else "Aggiungi nota")
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick  = onCondividi,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape    = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.Share, null, Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Condividi")
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(
            onClick = onRimuovi, modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Error),
            border = ButtonDefaults.outlinedButtonBorder.copy(brush = SolidColor(Error))
        ) {
            Icon(Icons.Default.BookmarkRemove, null, Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("Rimuovi dai preferiti", fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(8.dp))
        TextButton(onClick = onChiudi, modifier = Modifier.fillMaxWidth()) {
            Text("Chiudi", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
    }
}
