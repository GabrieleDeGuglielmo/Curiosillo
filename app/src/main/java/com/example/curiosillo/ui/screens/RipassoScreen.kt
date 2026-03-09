package com.example.curiosillo.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.curiosillo.CuriosityApplication
import com.example.curiosillo.firebase.FirebaseManager
import com.example.curiosillo.ui.categoryImage
import com.example.curiosillo.ui.components.CuriosilloBottomBar
import com.example.curiosillo.ui.components.NotaBottomSheet
import com.example.curiosillo.ui.theme.Success
import com.example.curiosillo.viewmodel.RipassoViewModel
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RipassoScreen(nav: NavController) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as CuriosityApplication
    val vm: RipassoViewModel = viewModel(
        factory = RipassoViewModel.Factory(app.repository)
    )
    val state             by vm.state.collectAsState()
    val commentiState     by vm.commentiState.collectAsState()

    val segnalazioneState by vm.segnalazioneState.collectAsState()

    var mostraNota         by remember { mutableStateOf(false) }
    var mostraSelettore    by remember { mutableStateOf(false) }
    var mostraCommenti     by remember { mutableStateOf(false) }
    var mostraAzioni       by remember { mutableStateOf(false) }
    var mostraSegnalazione by remember { mutableStateOf(false) }

    val opzioniGiorni = listOf(
        0  to "Tutte",
        3  to "3+ giorni fa",
        7  to "7+ giorni fa",
        14 to "14+ giorni fa",
        30 to "30+ giorni fa"
    )

    // Automatically close the report sheet on success
    LaunchedEffect(segnalazioneState) {
        if (segnalazioneState is SegnalazioneUiState.Successo) {
            mostraSegnalazione = false
        }
    }

    // ── Dialog filtro ─────────────────────────────────────────────────────────
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

    // ── Nota bottom sheet ─────────────────────────────────────────────────────
    if (mostraNota && pillola != null) {
        NotaBottomSheet(
            notaAttuale = pillola.nota,
            onSalva     = { vm.salvaNota(it) },
            onChiudi    = { mostraNota = false }
        )
    }

    // ── Segnalazione bottom sheet ─────────────────────────────────────────────
    if (mostraSegnalazione) {
        SegnalazioneBottomSheet(
            onInvia   = { tipo, testo -> vm.inviaSegnalazione(tipo, testo) },
            onDismiss = { mostraSegnalazione = false },
            isLoading = segnalazioneState is SegnalazioneUiState.Loading
        )
    }

    // ── Commenti bottom sheet ─────────────────────────────────────────────────
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

    // ── Menu azioni (bottom sheet) ────────────────────────────────────────────
    if (mostraAzioni && pillola != null) {
        val sheetStateAzioni = rememberModalBottomSheetState(skipPartiallyExpanded = true)
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

                // ── Bookmark ─────────────────────────────────────────────────
                OutlinedButton(
                    onClick  = { mostraAzioni = false; vm.toggleBookmark() },
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

                // ── Nota ─────────────────────────────────────────────────────
                OutlinedButton(
                    onClick  = { mostraAzioni = false; mostraNota = true },
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

                // ── Commenti ─────────────────────────────────────────────────
                OutlinedButton(
                    onClick  = {
                        mostraAzioni = false
                        vm.caricaCommenti()
                        mostraCommenti = true
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.ChatBubbleOutline, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    Spacer(Modifier.width(8.dp))
                    Text("Commenti (${commentiState.commenti.size})", fontWeight = FontWeight.SemiBold)
                }

                Spacer(Modifier.height(12.dp))

                // ── Segnala ─────────────────────────────────────────────────────
                OutlinedButton(
                    onClick  = { mostraAzioni = false; mostraSegnalazione = true },
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

    // ── Scaffold ──────────────────────────────────────────────────────────────
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

                else -> PilloleCarousel(
                    state         = state,
                    gradientBg    = gradientBg,
                    pad           = pad,
                    onProssima    = { vm.prossima() },
                    onPrecedente  = { vm.precedente() }
                )
            }

            // ── Banner Notifiche Segnalazione ─────────────────────────────
            when (val sState = segnalazioneState) {
                is SegnalazioneUiState.Successo -> {
                    LaunchedEffect(Unit) {
                        kotlinx.coroutines.delay(3000)
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
                        kotlinx.coroutines.delay(3500)
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

// ── Carousel con swipe + peek card successiva ─────────────────────────────────

@Composable
private fun PilloleCarousel(
    state:        com.example.curiosillo.viewmodel.RipassoUiState,
    gradientBg:   Brush,
    pad:          PaddingValues,
    onProssima:   () -> Unit,
    onPrecedente: () -> Unit
) {
    // ── Swipe state ───────────────────────────────────────────────────────────
    var rawOffset    by remember { mutableFloatStateOf(0f) }
    val animSpec     = spring<Float>(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
    val animOffset   by animateFloatAsState(targetValue = rawOffset, animationSpec = animSpec, label = "offset")

    val swipeThreshold = 100f
    val indice = state.indiceCorrente

    LaunchedEffect(indice) { rawOffset = 0f }

    val cur           = state.pillole[indice]
    val hasProssima   = indice < state.pillole.size - 1
    val hasPrecedente = indice > 0
    val noteCardBg    = MaterialTheme.colorScheme.surfaceVariant
    val noteTextColor = MaterialTheme.colorScheme.onSurfaceVariant

    val rotation = (animOffset / 25f).coerceIn(-8f, 8f)

    Box(
        Modifier
            .fillMaxSize()
            .background(gradientBg)
            .padding(pad)
            .pointerInput(indice) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        when {
                            rawOffset < -swipeThreshold && hasProssima   -> onProssima()
                            rawOffset > swipeThreshold  && hasPrecedente -> onPrecedente()
                            else -> rawOffset = 0f
                        }
                    },
                    onDragCancel = { rawOffset = 0f },
                    onHorizontalDrag = { _, delta -> rawOffset += delta }
                )
            }
    ) {
        LinearProgressIndicator(
            progress   = { (indice + 1f) / state.pillole.size },
            modifier   = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .align(Alignment.TopCenter),
            color      = MaterialTheme.colorScheme.secondary,
            trackColor = MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
        )

        if (hasProssima) {
            val nextPeekOffset = (animOffset * 0.12f)
            Box(
                Modifier
                    .align(Alignment.CenterEnd)
                    .offset { IntOffset((80 + nextPeekOffset).roundToInt(), 0) }
                    .graphicsLayer(alpha = 0.55f, rotationZ = 3f, scaleX = 0.93f, scaleY = 0.93f)
            ) {
                val nextCur = state.pillole[indice + 1]
                Card(
                    modifier  = Modifier.width(280.dp),
                    shape     = RoundedCornerShape(24.dp),
                    colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Image(
                        painter            = painterResource(id = categoryImage(nextCur.category)),
                        contentDescription = null,
                        modifier           = Modifier.fillMaxWidth().height(100.dp),
                        contentScale       = ContentScale.Crop
                    )
                    Text(
                        nextCur.title,
                        modifier   = Modifier.padding(12.dp),
                        style      = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines   = 2,
                        color      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }

        Column(
            Modifier
                .fillMaxSize()
                .offset { IntOffset(animOffset.roundToInt(), 0) }
                .graphicsLayer(rotationZ = rotation)
                .verticalScroll(rememberScrollState())
        ) {
            Image(
                painter            = painterResource(id = categoryImage(cur.category)),
                contentDescription = cur.category,
                modifier           = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)),
                contentScale = ContentScale.Crop
            )

            Column(Modifier.padding(horizontal = 24.dp)) {
                Spacer(Modifier.height(16.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    SuggestionChip(
                        onClick = {},
                        label   = { Text(emojiCategoria(cur.category) + " " + cur.category) }
                    )

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.secondary
                    ) {
                        Row(
                            Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                "${indice + 1}",
                                style      = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color      = Color.White
                            )
                            Text(
                                "/ ${state.pillole.size}",
                                style  = MaterialTheme.typography.bodySmall,
                                color  = Color.White.copy(alpha = 0.75f)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                Text(
                    cur.title,
                    style      = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color      = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(16.dp))

                Card(
                    Modifier.fillMaxWidth(),
                    shape     = RoundedCornerShape(18.dp),
                    colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Text(
                        cur.body,
                        Modifier.padding(20.dp),
                        style      = MaterialTheme.typography.bodyLarge,
                        lineHeight = 27.sp,
                        color      = MaterialTheme.colorScheme.onSurface
                    )
                }

                if (cur.nota.isNotBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Card(
                        Modifier.fillMaxWidth(),
                        shape     = RoundedCornerShape(12.dp),
                        colors    = CardDefaults.cardColors(containerColor = noteCardBg),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                            Text("📝 ", fontSize = 14.sp)
                            Text(cur.nota, style = MaterialTheme.typography.bodySmall, color = noteTextColor)
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    val hintAlpha by animateFloatAsState(
                        targetValue   = if (rawOffset.absoluteValue > 10f) 0f else 0.4f,
                        animationSpec = tween(200),
                        label         = "hintAlpha"
                    )
                    if (hasPrecedente) {
                        Icon(Icons.Default.ArrowBackIosNew, null,
                            Modifier.size(12.dp).graphicsLayer(alpha = hintAlpha),
                            tint = MaterialTheme.colorScheme.onBackground)
                        Spacer(Modifier.width(4.dp))
                    }
                    Text(
                        "scorri per cambiare pillola",
                        style    = MaterialTheme.typography.labelSmall,
                        color    = MaterialTheme.colorScheme.onBackground.copy(alpha = hintAlpha),
                    )
                    if (hasProssima) {
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForwardIos, null,
                            Modifier.size(12.dp).graphicsLayer(alpha = hintAlpha),
                            tint = MaterialTheme.colorScheme.onBackground)
                    }
                }

                Spacer(Modifier.height(12.dp))

                // ── Navigazione pulsanti ────────────────────
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick  = onPrecedente,
                        enabled  = hasPrecedente,
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape    = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.ArrowBackIosNew, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Precedente")
                    }
                    Button(
                        onClick  = onProssima,
                        enabled  = hasProssima,
                        modifier = Modifier.weight(1f).height(52.dp),
                        shape    = RoundedCornerShape(14.dp),
                        colors   = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text("Prossima", color = Color.White)
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.AutoMirrored.Filled.ArrowForwardIos, null, Modifier.size(16.dp), tint = Color.White)
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}