package com.example.curiosillo.ui.screens.curiosity

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.curiosillo.CuriosityApplication
import com.example.curiosillo.data.Curiosity
import com.example.curiosillo.firebase.FirebaseManager
import com.example.curiosillo.ui.components.CuriosilloBottomBar
import com.example.curiosillo.ui.components.NotaBottomSheet
import com.example.curiosillo.ui.screens.utils.*
import com.example.curiosillo.ui.theme.Success
import com.example.curiosillo.viewmodel.RipassoUiState
import com.example.curiosillo.viewmodel.RipassoViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun RipassoScreen(nav: NavController) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as CuriosityApplication
    val vm: RipassoViewModel = viewModel(
        factory = RipassoViewModel.Factory(app.repository, app.geminiPrefs)
    )
    val state             by vm.state.collectAsState()
    val commentiState     by vm.commentiState.collectAsState()
    val segnalazioneState by vm.segnalazioneState.collectAsState()
    val geminiState       by vm.geminiState.collectAsState()

    var mostraNota         by remember { mutableStateOf(false) }
    var mostraSelettore    by remember { mutableStateOf(false) }
    var mostraCommenti     by remember { mutableStateOf(false) }
    var mostraAzioni       by remember { mutableStateOf(false) }
    var mostraSegnalazione by remember { mutableStateOf(false) }
    var mostraGemini       by remember { mutableStateOf(false) }

    val coroutineScope = rememberCoroutineScope()

    val opzioniGiorni = listOf(
        0  to "Tutte",
        3  to "3+ giorni fa",
        7  to "7+ giorni fa",
        14 to "14+ giorni fa",
        30 to "30+ giorni fa"
    )

    LaunchedEffect(segnalazioneState) {
        if (segnalazioneState is SegnalazioneUiState.Successo) {
            mostraSegnalazione = false
        }
    }

    if (mostraSelettore) {
        AlertDialog(
            onDismissRequest = { mostraSelettore = false },
            title = { Text("Mostra pillole lette...") },
            text  = {
                Column {
                    opzioniGiorni.forEach { (giorni, etichetta) ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = state.giorniSelezionati == giorni,
                                onClick  = { vm.carica(giorni); mostraSelettore = false }
                            )
                            Text(etichetta, Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { mostraSelettore = false }) { Text("Chiudi") }
            }
        )
    }

    // Pillola corrente reattiva allo stato
    val pillola = state.pillolaDettaglio ?: state.risultati.getOrNull(state.indiceCorrente)

    if (mostraNota && pillola != null) {
        NotaBottomSheet(
            notaAttuale = pillola.nota,
            onSalva     = { vm.salvaNota(it) },
            onChiudi    = { mostraNota = false }
        )
    }

    if (mostraSegnalazione) {
        SegnalazioneBottomSheet(
            onInvia = { tipo, testo -> vm.inviaSegnalazione(tipo, testo) },
            onDismiss = { mostraSegnalazione = false },
            isLoading = segnalazioneState is SegnalazioneUiState.Loading
        )
    }

    if (mostraCommenti) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
        ModalBottomSheet(
            onDismissRequest = { mostraCommenti = false },
            sheetState       = sheetState
        ) {
            CommentiSheet(
                commentiState  = commentiState,
                currentUid     = FirebaseManager.uid,
                isLoggato      = FirebaseManager.isLoggato,
                onInvia        = { vm.inviaCommento(it) },
                onElimina      = { vm.eliminaCommento(it) },
                onDismissError = { vm.dismissErroreCommento() }
            )
        }
    }

    if (mostraGemini) {
        val sheetStateGemini = rememberModalBottomSheetState(skipPartiallyExpanded = false)
        ModalBottomSheet(
            onDismissRequest = {
                mostraGemini = false
            },
            sheetState = sheetStateGemini
        ) {
            GeminiSheet(geminiState)
        }
    }

    if (mostraAzioni && pillola != null) {
        val sheetStateAzioni = rememberModalBottomSheetState(skipPartiallyExpanded = true)

        val animateChiudiAzioni: () -> Unit = {
            coroutineScope.launch {
                sheetStateAzioni.hide()
            }.invokeOnCompletion {
                mostraAzioni = false
            }
        }

        ModalBottomSheet(
            onDismissRequest = { mostraAzioni = false },
            sheetState       = sheetStateAzioni
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    "Azioni",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier   = Modifier.padding(bottom = 16.dp)
                )

                OutlinedButton(
                    onClick  = {
                        vm.toggleBookmark()
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        imageVector = if (pillola.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                        contentDescription = "Preferito",
                        Modifier.size(20.dp),
                        tint = if (pillola.isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (pillola.isBookmarked) "Rimuovi dai preferiti" else "Aggiungi ai preferiti", fontWeight = FontWeight.SemiBold)
                }

                Spacer(Modifier.height(12.dp))

                OutlinedButton(
                    onClick  = {
                        animateChiudiAzioni()
                        coroutineScope.launch {
                            delay(300)
                            mostraNota = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        Icons.Default.EditNote, null, Modifier.size(20.dp),
                        tint = if (pillola.nota.isNotBlank()) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(if (pillola.nota.isNotBlank()) "Modifica nota" else "Aggiungi nota", fontWeight = FontWeight.SemiBold)
                }

                Spacer(Modifier.height(12.dp))

                OutlinedButton(
                    onClick  = {
                        animateChiudiAzioni()
                        coroutineScope.launch {
                            delay(300)
                            vm.caricaCommenti()
                            mostraCommenti = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.ChatBubbleOutline, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    Spacer(Modifier.width(8.dp))
                    Text("Commenti (${commentiState.commenti.size})", fontWeight = FontWeight.SemiBold)
                }

                Spacer(Modifier.height(12.dp))

                OutlinedButton(
                    onClick  = {
                        animateChiudiAzioni()
                        coroutineScope.launch {
                            delay(300)
                            mostraSegnalazione = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(14.dp),
                    border   = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Flag, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Segnala curiosità", fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ripasso") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (state.pillolaDettaglio != null) vm.chiudiDettaglio()
                        else nav.popBackStack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Indietro")
                    }
                },
                actions = {
                    if (state.pillolaDettaglio == null) {
                        TextButton(onClick = { mostraSelettore = true }) {
                            val label = opzioniGiorni.find { it.first == state.giorniSelezionati }?.second ?: "Filtra"
                            Text(label, color = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        IconButton(onClick = { mostraAzioni = true }) {
                            Icon(Icons.Default.MoreVert, "Azioni",
                                tint = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar      = { if (state.pillolaDettaglio == null) CuriosilloBottomBar(nav) },
        containerColor = Color.Transparent
    ) { pad ->
        val gradientBg = Brush.verticalGradient(listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
            MaterialTheme.colorScheme.background
        ))

        Box(Modifier.fillMaxSize().background(gradientBg).padding(pad)) {
            when {
                state.isLoading ->
                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                        CircularProgressIndicator()
                    }

                state.pillole.isEmpty() ->
                    Column(
                        Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("📚", fontSize = 56.sp)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Nessuna pillola da ripassare\ncon il filtro attuale.",
                            style     = MaterialTheme.typography.bodyLarge,
                            color     = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(16.dp))
                        TextButton(onClick = { mostraSelettore = true }) {
                            Text("Cambia filtro", color = MaterialTheme.colorScheme.primary)
                        }
                    }

                else -> {
                    AnimatedContent(
                        targetState = state.pillolaDettaglio == null,
                        transitionSpec = {
                            if (targetState) {
                                (fadeIn() + slideInHorizontally { -it }).togetherWith(fadeOut() + slideOutHorizontally { it })
                            } else {
                                (fadeIn() + slideInHorizontally { it }).togetherWith(fadeOut() + slideOutHorizontally { -it })
                            }
                        },
                        label = "RipassoTransition"
                    ) { isListView ->
                        if (isListView) {
                            // Vista Elenco con ricerca e filtri
                            Column(Modifier.fillMaxSize()) {
                                OutlinedTextField(
                                    value = state.query,
                                    onValueChange = { vm.onQueryChange(it) },
                                    placeholder = { Text("Cerca tra le pillole lette...") },
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
                                            onClick = { vm.resetCategorie() },
                                            label = { Text("Tutte") }
                                        )
                                        state.categorie.forEach { cat ->
                                            FilterChip(
                                                selected = state.categorieSelezionate.contains(cat),
                                                onClick = { vm.onCategoriaSelezionata(cat) },
                                                label = {
                                                    Text(
                                                        cat,
                                                        color = if (state.categorieSelezionate.contains(cat)) Color(0xFF1A1A1A)
                                                        else MaterialTheme.colorScheme.onSurface
                                                    )
                                                },
                                                colors = FilterChipDefaults.filterChipColors(
                                                    selectedContainerColor = coloreCategoria(cat),
                                                    selectedLabelColor = Color(0xFF1A1A1A)
                                                )
                                            )
                                        }
                                    }
                                }
                                HorizontalDivider(Modifier.padding(horizontal = 16.dp, vertical = 4.dp))

                                if (state.risultati.isEmpty()) {
                                    Box(Modifier.fillMaxSize(), Alignment.Center) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text("🔍", fontSize = 48.sp)
                                            Text("Nessun risultato", color = MaterialTheme.colorScheme.onSurface.copy(0.5f))
                                        }
                                    }
                                } else {
                                    LazyColumn(
                                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        items(state.risultati, key = { it.id }) { p ->
                                            RipassoItemCard(p) { vm.apriDettaglio(p) }
                                        }
                                    }
                                }
                            }
                        } else {
                            // Vista Card (Pager)
                            PillolePager(
                                state = state,
                                vm = vm,
                                onDimmiDiPiu = { vm.dimmiDiPiu(); mostraGemini = true }
                            )
                        }
                    }
                }
            }

            // Notifiche segnalazione
            when (val sState = segnalazioneState) {
                is SegnalazioneUiState.Successo -> {
                    LaunchedEffect(Unit) {
                        delay(3000)
                        vm.dismissSegnalazione()
                    }
                    Box(Modifier.fillMaxSize().padding(bottom = 20.dp), contentAlignment = Alignment.BottomCenter) {
                        Surface(color = Success, shape = RoundedCornerShape(12.dp), shadowElevation = 6.dp) {
                            Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, null, tint = Color.White)
                                Spacer(Modifier.width(8.dp))
                                Text("Segnalazione inviata. Grazie!", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                is SegnalazioneUiState.Errore -> {
                    LaunchedEffect(Unit) {
                        delay(3500)
                        vm.dismissSegnalazione()
                    }
                    Box(Modifier.fillMaxSize().padding(bottom = 20.dp), contentAlignment = Alignment.BottomCenter) {
                        Surface(color = MaterialTheme.colorScheme.error, shape = RoundedCornerShape(12.dp), shadowElevation = 6.dp) {
                            Text(sState.msg, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

@Composable
private fun RipassoItemCard(p: Curiosity, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(elevation = 3.dp, shape = RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Row(
            Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(10.dp).background(coloreCategoria(p.category), CircleShape))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    p.title, style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(
                    p.body, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            Icon(
                Icons.AutoMirrored.Filled.ArrowForwardIos, null,
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PillolePager(
    state: RipassoUiState,
    vm: RipassoViewModel,
    onDimmiDiPiu: () -> Unit
) {
    val pagerState = rememberPagerState(initialPage = state.indiceCorrente, pageCount = { state.risultati.size })
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(pagerState.currentPage) {
        vm.setIndice(pagerState.currentPage)
    }

    val onProssima: () -> Unit = {
        coroutineScope.launch {
            if (pagerState.currentPage < state.risultati.size - 1) {
                pagerState.animateScrollToPage(pagerState.currentPage + 1)
            }
        }
    }
    val onPrecedente: () -> Unit = {
        coroutineScope.launch {
            if (pagerState.currentPage > 0) {
                pagerState.animateScrollToPage(pagerState.currentPage - 1)
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        LinearProgressIndicator(
            progress = { (pagerState.currentPage + 1f) / state.risultati.size },
            modifier   = Modifier.fillMaxWidth().height(3.dp).align(Alignment.TopCenter),
            color      = MaterialTheme.colorScheme.secondary,
            trackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
        )

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 32.dp),
            pageSpacing = 16.dp,
            verticalAlignment = Alignment.CenterVertically
        ) { page ->
            val cur = state.risultati[page]

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .graphicsLayer {
                        val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
                        val scale = lerp(start = 0.85f, stop = 1f, fraction = 1f - pageOffset.absoluteValue.coerceIn(0f, 1f))

                        scaleX = scale
                        scaleY = scale

                        shadowElevation = 24.dp.toPx() // Un'ombra ampia e morbida
                        shape = RoundedCornerShape(24.dp)
                        clip = false // Importante: evita che l'ombra venga tagliata ai bordi

                    },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(Modifier.fillMaxSize()) {
                    Image(
                        painter = painterResource(id = categoryImage(cur.category)),
                        contentDescription = cur.category,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                        contentScale = ContentScale.Crop
                    )

                    Box(Modifier.weight(1f)) {
                        Column(
                            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)
                        ) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                SuggestionChip(
                                    onClick = {},
                                    label = { Text(emojiCategoria(cur.category) + " " + cur.category) }
                                )

                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
                                ) {
                                    Text(
                                        "${page + 1} / ${state.risultati.size}",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }

                            Spacer(Modifier.height(12.dp))
                            Text(cur.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(12.dp))
                            Text(cur.body, style = MaterialTheme.typography.bodyMedium, lineHeight = 24.sp)

                            if (cur.nota.isNotBlank()) {
                                Spacer(Modifier.height(16.dp))
                                Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.primaryContainer.copy(0.4f)) {
                                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                                        Text("📝 ", fontSize = 14.sp)
                                        Text(cur.nota, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }

                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalAlignment = Alignment.End) {
                        // Bottone Dimmi di più (piccolo)
                        val giaSvelato = cur.approfondimentoAi != null
                        OutlinedButton(
                            onClick = onDimmiDiPiu,
                            modifier = Modifier.height(36.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                        ) {
                            Icon(if (giaSvelato) Icons.Default.CheckCircle else Icons.Default.AutoAwesome, null, Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                            Text(if (giaSvelato) "Già svelato! 🐾" else "Dimmi di più", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }

                        Spacer(Modifier.height(10.dp))

                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            if (page > 0) {
                                FilledTonalIconButton(
                                    onClick = onPrecedente,
                                    modifier = Modifier.size(48.dp),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Icon(Icons.Default.ArrowBackIosNew, null, Modifier.size(18.dp))
                                }
                            } else {
                                Spacer(Modifier.size(48.dp))
                            }

                            Button(
                                onClick = onProssima,
                                enabled = page < state.risultati.size - 1,
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                Text(if (page < state.risultati.size - 1) "Prossima" else "Fine ripasso")
                                if (page < state.risultati.size - 1) {
                                    Spacer(Modifier.width(8.dp))
                                    Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}