package com.example.curiosillo.ui.screens.curiosity

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.curiosillo.CuriosityApplication
import com.example.curiosillo.firebase.FirebaseManager
import com.example.curiosillo.ui.screens.utils.categoryImage
import com.example.curiosillo.ui.components.CuriosilloBottomBar
import com.example.curiosillo.ui.components.NotaBottomSheet
import com.example.curiosillo.ui.screens.utils.SegnalazioneBottomSheet
import com.example.curiosillo.ui.screens.utils.SegnalazioneUiState
import com.example.curiosillo.ui.screens.utils.emojiCategoria
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

    val pillola = vm.pilloleCorrente()

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
                        // Il menu NON si chiude quando si preme preferiti, come richiesto
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
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Indietro")
                    }
                },
                actions = {
                    TextButton(onClick = { mostraSelettore = true }) {
                        val label = opzioniGiorni.find { it.first == state.giorniSelezionati }?.second ?: "Filtra"
                        Text(label, color = MaterialTheme.colorScheme.primary)
                    }
                    if (state.pillole.isNotEmpty()) {
                        IconButton(onClick = { mostraAzioni = true }) {
                            Icon(Icons.Default.MoreVert, "Azioni",
                                tint = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar      = { CuriosilloBottomBar(nav) },
        containerColor = Color.Transparent
    ) { pad ->
        val gradientBg = Brush.verticalGradient(listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
            MaterialTheme.colorScheme.background
        ))

        Box(Modifier.fillMaxSize()) {
            when {
                state.isLoading ->
                    Box(Modifier.fillMaxSize().background(gradientBg).padding(pad), Alignment.Center) {
                        CircularProgressIndicator()
                    }

                state.pillole.isEmpty() ->
                    Column(
                        Modifier.fillMaxSize().background(gradientBg).padding(pad),
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
                    PillolePager(
                        state = state,
                        gradientBg = gradientBg,
                        pad = pad,
                        vm = vm,
                        onDimmiDiPiu = { vm.dimmiDiPiu(); mostraGemini = true }
                    )
                }
            }

            when (val sState = segnalazioneState) {
                is SegnalazioneUiState.Successo -> {
                    LaunchedEffect(Unit) {
                        delay(3000)
                        vm.dismissSegnalazione()
                    }
                    Box(Modifier.fillMaxSize().padding(bottom = 120.dp), contentAlignment = Alignment.BottomCenter) {
                        Surface(
                            color = Success,
                            shape = RoundedCornerShape(12.dp),
                            shadowElevation = 6.dp
                        ) {
                            Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Success", tint = Color.White)
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
                    Box(Modifier.fillMaxSize().padding(bottom = 120.dp), contentAlignment = Alignment.BottomCenter) {
                        Surface(
                            color = MaterialTheme.colorScheme.error,
                            shape = RoundedCornerShape(12.dp),
                            shadowElevation = 6.dp
                        ) {
                            Text(sState.msg, color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))
                        }
                    }
                }
                else -> {}
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun PillolePager(
    state:      RipassoUiState,
    gradientBg: Brush,
    pad:        PaddingValues,
    vm:         RipassoViewModel,
    onDimmiDiPiu: () -> Unit
) {
    val pagerState = rememberPagerState(initialPage = state.indiceCorrente, pageCount = { state.pillole.size })
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(pagerState.currentPage) {
        vm.setIndice(pagerState.currentPage)
    }

    val onProssima: () -> Unit = {
        coroutineScope.launch {
            if (pagerState.currentPage < state.pillole.size - 1) {
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

    Box(Modifier.fillMaxSize().background(gradientBg).padding(pad)) {
        LinearProgressIndicator(
            progress = { (pagerState.currentPage + 1f) / state.pillole.size },
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
            val cur = state.pillole[page]

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f)
                    .graphicsLayer {
                        val pageOffset = (
                                (pagerState.currentPage - page) + pagerState
                                    .currentPageOffsetFraction
                                ).absoluteValue

                        alpha = lerp(
                            start = 0.5f,
                            stop = 1f,
                            fraction = 1f - pageOffset.coerceIn(0f, 1f)
                        )

                        scaleY = lerp(
                            start = 0.85f,
                            stop = 1f,
                            fraction = 1f - pageOffset.coerceIn(0f, 1f)
                        )
                    },
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
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
                            Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(20.dp)
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
                                        "${page + 1} / ${state.pillole.size}",
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.secondary
                                    )
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            Text(
                                cur.title,
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground)

                            Spacer(Modifier.height(12.dp))

                            Text(
                                cur.body,
                                style = MaterialTheme.typography.bodyMedium,
                                lineHeight = 24.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            if (cur.nota.isNotBlank()) {
                                Spacer(Modifier.height(16.dp))
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                                ) {
                                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                                        Text("📝 ", fontSize = 14.sp)
                                        Text(
                                            cur.nota,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    } // Box weight

                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        horizontalAlignment = Alignment.End
                    ) {
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
                                enabled = page < state.pillole.size - 1,
                                modifier = Modifier.weight(1f).height(48.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary
                                )
                            ) {
                                Text(if (page < state.pillole.size - 1) "Prossima" else "Fine ripasso")
                                if (page < state.pillole.size - 1) {
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