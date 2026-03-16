package com.example.curiosillo.ui.screens.user

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.BookmarkRemove
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.curiosillo.CuriosityApplication
import com.example.curiosillo.data.Curiosity
import com.example.curiosillo.ui.components.NotaBottomSheet
import com.example.curiosillo.ui.screens.utils.SegnalazioneBottomSheet
import com.example.curiosillo.ui.screens.utils.coloreCategoria
import com.example.curiosillo.ui.theme.Error
import com.example.curiosillo.viewmodel.BookmarkViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BookmarkScreen(nav: NavController) {
    val ctx = LocalContext.current
    val repo = (ctx.applicationContext as CuriosityApplication).repository
    val vm: BookmarkViewModel = viewModel(factory = BookmarkViewModel.Factory(repo))
    val state by vm.state.collectAsState()
    val scope = rememberCoroutineScope()
    
    var pillolaPerNota     by remember { mutableStateOf<Curiosity?>(null) }
    var mostraSegnalazione by remember { mutableStateOf<Curiosity?>(null) }
    var hintGiaMostrato    by rememberSaveable { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    pillolaPerNota?.let { pillola ->
        NotaBottomSheet(
            notaAttuale = pillola.nota,
            onSalva = { testo -> vm.salvaNota(pillola, testo) },
            onChiudi = { pillolaPerNota = null })
    }

    mostraSegnalazione?.let { pillola ->
        SegnalazioneBottomSheet(
            onInvia = { tipo, testo ->
                vm.inviaSegnalazione(pillola, tipo, testo)
                mostraSegnalazione = null
            },
            onDismiss = { mostraSegnalazione = null }
        )
    }

    state.dettaglio?.let { pillola ->
        ModalBottomSheet(
            onDismissRequest = { vm.chiudiDettaglio() }, 
            sheetState = sheetState
        ) {
            val context = LocalContext.current
            DettaglioSheet(
                pillola = pillola,
                onRimuovi = {
                    scope.launch {
                        sheetState.hide()
                        vm.chiudiDettaglio()
                        vm.rimuoviBookmark(pillola)
                    }
                },
                onNota = { pillolaPerNota = pillola },
                onSegnala = { 
                    scope.launch {
                        sheetState.hide()
                        vm.chiudiDettaglio()
                        mostraSegnalazione = pillola
                    }
                },
                onCondividi = {
                    val testo = "📚 ${pillola.title}\n\n${pillola.body}\n\n— Categoria: ${pillola.category}\nScoperto con Curiosillo 🎓"
                    val intent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, testo)
                    }
                    context.startActivity(Intent.createChooser(intent, "Condividi con..."))
                },
                onChiudi = { 
                    scope.launch {
                        sheetState.hide()
                        vm.chiudiDettaglio()
                    }
                }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("I miei preferiti") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (nav.currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
                            nav.popBackStack()
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Indietro")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
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
        Column(Modifier.fillMaxSize().background(gradientBg).padding(pad)) {
            OutlinedTextField(
                value = state.query,
                onValueChange = { vm.onQueryChange(it) },
                placeholder = { Text("Cerca tra i preferiti...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (state.categorie.isNotEmpty()) {
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = state.categorieSelezionate.isEmpty(),
                        onClick = { vm.resetCategorie() }, label = { Text("Tutte") })
                    state.categorie.forEach { cat ->
                        FilterChip(
                            selected = state.categorieSelezionate.contains(cat),
                            onClick = { vm.onCategoriaSelezionata(cat) },
                            label = {
                                Text(cat, color = if (state.categorieSelezionate.contains(cat)) Color(0xFF1A1A1A) else MaterialTheme.colorScheme.onSurface)
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = coloreCategoria(cat),
                                selectedLabelColor = Color(0xFF1A1A1A)
                            )
                        )
                    }
                }
            }

            HorizontalDivider(Modifier.padding(horizontal = 16.dp))
            Spacer(Modifier.height(4.dp))

            when {
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
                state.risultati.isEmpty() -> Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("🔖", fontSize = 56.sp)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        if (state.query.isNotEmpty() || state.categorieSelezionate.isNotEmpty()) "Nessun risultato trovato.\nProva a cambiare i filtri."
                        else "Non hai ancora salvato nessuna pillola.\nPremi il segnalibro durante la lettura!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 32.dp)
                    )
                }
                else -> {
                    Text(
                        "${state.risultati.size} pillol${if (state.risultati.size == 1) "a" else "e"} salvat${if (state.risultati.size == 1) "a" else "e"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
                    )
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(state.risultati, key = { it.id }) { pillola ->
                            val isVisible = !state.idInRimozione.contains(pillola.id)
                            val isFirst = state.risultati.firstOrNull()?.id == pillola.id
                            val offsetX = remember { Animatable(0f) }

                            if (isFirst && !hintGiaMostrato) {
                                LaunchedEffect(Unit) {
                                    delay(400)
                                    offsetX.animateTo(-28f, tween(400))
                                    offsetX.animateTo(28f, tween(400))
                                    offsetX.animateTo(0f, tween(180))
                                    hintGiaMostrato = true
                                }
                            }

                            AnimatedVisibility(
                                visible = isVisible,
                                exit = shrinkVertically(tween(280)) + fadeOut(tween(280)),
                                modifier = Modifier.animateItemPlacement()
                            ) {
                                SwipeToRemoveBookmark(onRimuovi = { vm.rimuoviBookmark(pillola) }) {
                                    BookmarkCard(
                                        pillola,
                                        modifier = if (isFirst) Modifier.graphicsLayer { translationX = offsetX.value } else Modifier
                                    ) { vm.apriDettaglio(pillola) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToRemoveBookmark(onRimuovi: () -> Unit, content: @Composable () -> Unit) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.StartToEnd || value == SwipeToDismissBoxValue.EndToStart) {
                onRimuovi()
                true
            } else false
        },
        positionalThreshold = { it * 0.4f }
    )

    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            val color by animateColorAsState(
                targetValue = when (dismissState.dismissDirection) {
                    SwipeToDismissBoxValue.StartToEnd -> Error.copy(alpha = 0.85f)
                    SwipeToDismissBoxValue.EndToStart -> Error.copy(alpha = 0.85f)
                    else -> Color.Transparent
                },
                label = "swipe_bg"
            )
            val scale by animateFloatAsState(
                targetValue = if (dismissState.dismissDirection != SwipeToDismissBoxValue.Settled) 1.2f else 0.8f,
                label = "swipe_icon_scale"
            )
            Box(
                Modifier.fillMaxSize().background(color, RoundedCornerShape(16.dp)).padding(horizontal = 24.dp),
                contentAlignment = when (dismissState.dismissDirection) {
                    SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
                    else -> Alignment.CenterStart
                }
            ) {
                Icon(Icons.Default.BookmarkRemove, null, Modifier.size((24 * scale).dp), tint = Color.White)
            }
        },
        modifier = Modifier.animateContentSize()
    ) {
        content()
    }
}

@Composable
private fun BookmarkCard(pillola: Curiosity, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(Modifier.padding(16.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).background(coloreCategoria(pillola.category), CircleShape))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(pillola.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                Surface(shape = RoundedCornerShape(6.dp), color = coloreCategoria(pillola.category).copy(alpha = 0.18f) ) {
                    Text(pillola.category, modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = Color(0xFF1A1A1A))
                }
                if (pillola.nota.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text("📝 " + pillola.nota, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Default.Bookmark, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun DettaglioSheet(
    pillola: Curiosity, onRimuovi: () -> Unit, onNota: () -> Unit,
    onSegnala: () -> Unit, onCondividi: () -> Unit, onChiudi: () -> Unit
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).background(coloreCategoria(pillola.category), CircleShape))
            Spacer(Modifier.width(8.dp))
            Text(pillola.category, style = MaterialTheme.typography.labelLarge, color = coloreCategoria(pillola.category))
        }
        Spacer(Modifier.height(10.dp))
        Text(pillola.title, style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(14.dp))
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), elevation = CardDefaults.cardElevation(0.dp)) {
            Text(pillola.body, Modifier.padding(16.dp), style = MaterialTheme.typography.bodyLarge, lineHeight = 26.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (pillola.nota.isNotBlank()) {
            Spacer(Modifier.height(12.dp))
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), elevation = CardDefaults.cardElevation(0.dp)) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                    Text("📝 ", fontSize = 14.sp)
                    Text(pillola.nota, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        OutlinedButton(onClick = onSegnala, modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))) {
            Icon(Icons.Default.Flag, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Spacer(Modifier.width(6.dp))
            Text("Segnala curiosità", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = onNota, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp)) {
            Icon(Icons.Default.EditNote, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp))
            Text(if (pillola.nota.isNotBlank()) "Modifica nota" else "Aggiungi nota")
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = onCondividi, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp)) {
            Icon(Icons.Default.Share, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)); Text("Condividi")
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = onRimuovi, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.outlinedButtonColors(contentColor = Error), border = ButtonDefaults.outlinedButtonBorder.copy(brush = SolidColor(Error))) {
            Icon(Icons.Default.BookmarkRemove, null, Modifier.size(20.dp)); Spacer(Modifier.width(8.dp))
            Text("Rimuovi dai preferiti")
        }
    }
}
