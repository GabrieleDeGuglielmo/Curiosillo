package com.example.curiosillo.ui.screens

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.curiosillo.CuriosityApplication
import com.example.curiosillo.data.BadgeSbloccato
import com.example.curiosillo.firebase.FirebaseManager
import com.example.curiosillo.ui.categoryImage
import com.example.curiosillo.ui.components.NotaBottomSheet
import com.example.curiosillo.ui.theme.DarkText
import com.example.curiosillo.ui.theme.Hidden
import com.example.curiosillo.ui.theme.Secondary
import com.example.curiosillo.ui.theme.Success
import com.example.curiosillo.viewmodel.CommentiUiState
import com.example.curiosillo.viewmodel.CuriosityUiState
import com.example.curiosillo.viewmodel.CuriosityViewModel
import com.example.curiosillo.viewmodel.SegnalazioneUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CuriosityScreen(nav: NavController) {
    val ctx   = LocalContext.current
    val app   = ctx.applicationContext as CuriosityApplication
    val vm: CuriosityViewModel = viewModel(
        factory = CuriosityViewModel.Factory(app.repository, app.categoryPrefs, app.gamificationEngine)
    )
    val state    by vm.state.collectAsState()
    val risultato by vm.risultatoAzione.collectAsState()
    val commentiState     by vm.commentiState.collectAsState()
    val segnalazioneState by vm.segnalazioneState.collectAsState()

    // Banner segnalazione inviata
    if (segnalazioneState is SegnalazioneUiState.Successo) {
        LaunchedEffect(Unit) {
            kotlinx.coroutines.delay(2500)
            vm.dismissSegnalazione()
        }
    }

    var badgeDaMostrare  by remember { mutableStateOf<BadgeSbloccato?>(null) }
    var badgeQueue       by remember { mutableStateOf<List<BadgeSbloccato>>(emptyList()) }
    var mostraCommenti     by remember { mutableStateOf(false) }
    var mostraDialogIgnora by remember { mutableStateOf(false) }
    var mostraFantastico   by remember { mutableStateOf(false) }

    LaunchedEffect(risultato) {
        risultato?.let {
            if (it.badgeSbloccati.isNotEmpty()) {
                badgeQueue      = it.badgeSbloccati
                badgeDaMostrare = badgeQueue.first()
            }
            vm.consumaRisultato()
        }
    }

    badgeDaMostrare?.let { badge ->
        AlertDialog(
            onDismissRequest = {
                val r = badgeQueue.drop(1); badgeQueue = r; badgeDaMostrare = r.firstOrNull()
            },
            icon  = { Text(badge.icona, fontSize = 48.sp) },
            title = {
                Text("Badge sbloccato!",
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            },
            text  = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(badge.nome, style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        textAlign  = TextAlign.Center,
                        modifier   = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(6.dp))
                    Text(badge.descrizione, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            },
            confirmButton = {
                Button(onClick = {
                    val r = badgeQueue.drop(1); badgeQueue = r; badgeDaMostrare = r.firstOrNull()
                }) { Text("Ottimo!") }
            }
        )
    }

    // Dialog conferma "Non mi interessa"
    if (mostraDialogIgnora) {
        AlertDialog(
            onDismissRequest = { mostraDialogIgnora = false },
            icon  = { Text("🙈", fontSize = 36.sp) },
            title = { Text("Non mi interessa") },
            text  = { Text("Questa curiosità verrà nascosta da tutti i conteggi e quiz. Potrai ripristinarla in seguito.", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
            confirmButton = {
                Button(onClick = { mostraDialogIgnora = false; vm.toggleIgnora() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Hidden
                    )) {
                    Text("Nascondi")
                }
            },
            dismissButton = {
                TextButton(onClick = { mostraDialogIgnora = false }) { Text("Annulla") }
            }
        )
    }

    // Bottom sheet commenti
    if (mostraCommenti) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
        ModalBottomSheet(
            onDismissRequest = { mostraCommenti = false },
            sheetState = sheetState
        ) {
            val s = state as? CuriosityUiState.Success
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pillola") },
                navigationIcon = {
                    IconButton({ nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Indietro") }
                },
                actions = {
                    if (state is CuriosityUiState.Success) {
                        val pillola = (state as CuriosityUiState.Success).curiosity
                        IconButton(onClick = {
                            val testo = buildString {
                                append("📚 ${pillola.title}\n\n")
                                append(pillola.body)
                                append("\n\n— Categoria: ${pillola.category}")
                                append("\nScoperto con Curiosillo 🎓")
                            }
                            ctx.startActivity(Intent.createChooser(
                                Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, testo)
                                }, "Condividi pillola"))
                        }) {
                            Icon(Icons.Default.Share, "Condividi",
                                tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent
    ) { pad ->
        val gradientBg = Brush.verticalGradient(listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
            MaterialTheme.colorScheme.background
        ))
        Box(Modifier.fillMaxSize()) {
            when (val s = state) {
                is CuriosityUiState.Loading ->
                    Box(Modifier.fillMaxSize().background(gradientBg).padding(pad), Alignment.Center) {
                        CircularProgressIndicator()
                    }
                is CuriosityUiState.Empty ->
                    Box(Modifier.fillMaxSize().background(gradientBg).padding(pad), Alignment.Center) {
                        Text("Nessuna curiosità disponibile!\nTorna presto.",
                            Modifier.padding(24.dp), textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge)
                    }
                is CuriosityUiState.Success ->
                    CuriosityContent(s, pad, gradientBg, commentiState = commentiState,
                        onLearn      = { mostraFantastico = true; vm.markLearned() },
                        onBookmark   = { vm.toggleBookmark() },
                        onSalvaNota  = { vm.salvaNota(it) },
                        onSegnala    = { tipo, testo -> vm.inviaSegnalazione(tipo, testo) },
                        onIgnora     = { mostraDialogIgnora = true },
                        onCommenti   = { vm.caricaCommenti(); mostraCommenti = true }
                    )
                is CuriosityUiState.Learned -> LearnedContent(pad, gradientBg) { vm.load() }
            }

            /* ── Overlay "Fantastico!" ─────────────────────────────────────────
            if (mostraFantastico) {
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(1400)
                    mostraFantastico = false
                }
                val pulse = rememberInfiniteTransition(label = "pulse")
                val scale by pulse.animateFloat(
                    initialValue  = 0.85f,
                    targetValue   = 1.05f,
                    animationSpec = infiniteRepeatable(
                        tween(300, easing = FastOutSlowInEasing), RepeatMode.Reverse
                    ),
                    label = "scale"
                )
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Surface(
                        shape           = RoundedCornerShape(28.dp),
                        color           = MaterialTheme.colorScheme.primary,
                        shadowElevation = 20.dp,
                        modifier        = Modifier.graphicsLayer(scaleX = scale, scaleY = scale)
                    ) {
                        Text(
                            "🎉 Fantastico!",
                            modifier   = Modifier.padding(horizontal = 40.dp, vertical = 22.dp),
                            style      = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color      = Color.White
                        )
                    }
                }
            }*/
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CuriosityContent(
    s:           CuriosityUiState.Success,
    pad:         PaddingValues,
    gradientBg:  Brush,
    commentiState: CommentiUiState,
    onLearn:     () -> Unit,
    onBookmark:  () -> Unit,
    onSalvaNota: (String) -> Unit,
    onSegnala:   (tipo: String, testo: String) -> Unit,
    onIgnora:    () -> Unit,
    onCommenti:  () -> Unit
) {
    var mostraSegnalazione by remember { mutableStateOf(false) }
    var mostraNota by remember { mutableStateOf(false) }
    val noteCardBg = MaterialTheme.colorScheme.surfaceVariant
    val noteTextColor = MaterialTheme.colorScheme.onSurfaceVariant

    if (mostraNota) {
        NotaBottomSheet(
            notaAttuale = s.curiosity.nota,
            onSalva     = onSalvaNota,
            onChiudi    = { mostraNota = false }
        )
    }

    Column(Modifier.fillMaxSize().background(gradientBg).padding(pad).verticalScroll(rememberScrollState())) {
        Image(
            painter            = painterResource(id = categoryImage(s.curiosity.category)),
            contentDescription = s.curiosity.category,
            modifier           = Modifier
                .fillMaxWidth().height(220.dp)
                .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)),
            contentScale = ContentScale.Crop
        )
        Column(Modifier.padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(16.dp))
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier              = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
            ) {
                SuggestionChip(onClick = {},
                    label = { Text(emojiCategoria(s.curiosity.category) + " " + s.curiosity.category) })

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = onIgnora,
                        modifier = Modifier.height(34.dp), // Altezza ridotta per eleganza
                        border = BorderStroke(1.dp, DarkText),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = DarkText
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.VisibilityOff,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Nascondi",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }

                    // Nota
                    IconButton(onClick = { mostraNota = true }) {
                        Icon(Icons.Default.EditNote, "Nota",
                            tint = if (s.curiosity.nota.isNotBlank())
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    }
                    // Bookmark
                    IconToggleButton(checked = s.curiosity.isBookmarked,
                        onCheckedChange = { onBookmark() }) {
                        Icon(
                            imageVector = if (s.curiosity.isBookmarked)
                                Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Salva",
                            tint = if (s.curiosity.isBookmarked)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            }

            if (s.curiosity.nota.isNotBlank()) {
                Card(Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    shape     = RoundedCornerShape(12.dp),
                    colors    = CardDefaults.cardColors(containerColor = noteCardBg),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                        Text("📝 ", fontSize = 14.sp)
                        Text(s.curiosity.nota, style = MaterialTheme.typography.bodySmall,
                            color = noteTextColor)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(s.curiosity.title,
                style      = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign  = TextAlign.Center,
                color      = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(20.dp))

            Card(Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(18.dp),
                colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Text(s.curiosity.body, Modifier.padding(20.dp),
                    style      = MaterialTheme.typography.bodyLarge,
                    lineHeight = 27.sp,
                    color      = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.height(16.dp))

            // ── Like / Dislike / Non mi interessa / Commenti ──────────────────
            Spacer(Modifier.height(16.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment     = Alignment.CenterVertically
            ) {
                // Segnala
                if (mostraSegnalazione) {
                    SegnalazioneBottomSheet(
                        onInvia   = { tipo, testo -> onSegnala(tipo, testo); mostraSegnalazione = false },
                        onDismiss = { mostraSegnalazione = false }
                    )
                }
                OutlinedButton(
                    onClick        = { mostraSegnalazione = true },
                    modifier       = Modifier.height(36.dp),
                    border         = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    shape          = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Flag, null, Modifier.size(15.dp),
                        tint = Secondary)
                    Spacer(Modifier.width(5.dp))
                    Text("Segnala",
                        style    = MaterialTheme.typography.labelMedium,
                        color    = Secondary,
                        fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.weight(1f))
                // Commenti
                OutlinedButton(onClick = onCommenti, shape = RoundedCornerShape(12.dp)) {
                    val count = commentiState.commenti.size
                    val label = "💬 Commenti ($count)"

                    Text(
                        text  = label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            if (!s.curiosity.isRead) {
                Button(onLearn, Modifier.fillMaxWidth().height(58.dp),
                    shape = RoundedCornerShape(16.dp)
                ) { Text("Ho imparato!", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold) }
            } else {
                OutlinedButton(onLearn, Modifier.fillMaxWidth().height(58.dp),
                    shape = RoundedCornerShape(16.dp)
                ) { Text("Prossima curiosità", style = MaterialTheme.typography.titleMedium) }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun LearnedContent(
    pad:        androidx.compose.foundation.layout.PaddingValues,
    gradientBg: androidx.compose.ui.graphics.Brush,
    onNext:     () -> Unit
) {
    Column(
        Modifier.fillMaxSize().background(gradientBg).padding(pad).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.CheckCircle, null, Modifier.size(90.dp), tint = Success)
        Spacer(Modifier.height(20.dp))
        Text("Fantastico!", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(10.dp))
        Text("Hai imparato qualcosa di nuovo!\nOra puoi fare il quiz su questa curiosità.",
            style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        Spacer(Modifier.height(32.dp))
        Button(onNext, Modifier.fillMaxWidth().height(58.dp), shape = RoundedCornerShape(16.dp)) {
            Text("Prossima curiosità", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun CommentiSheet(
    commentiState:  com.example.curiosillo.viewmodel.CommentiUiState,
    currentUid:     String?,
    isLoggato:      Boolean,
    onInvia:        (String) -> Unit,
    onElimina:      (String) -> Unit,
    onDismissError: () -> Unit
) {
    var testo by remember { mutableStateOf("") }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 32.dp)
    ) {
        Text("Commenti", style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))

        // Errore invio
        commentiState.erroreInvio?.let { err ->
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(err, color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    TextButton(onClick = onDismissError) { Text("OK") }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        // Lista commenti
        if (commentiState.isLoading) {
            CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally))
        } else if (commentiState.commenti.isEmpty()) {
            Text("Nessun commento ancora. Sii il primo!",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 8.dp))
        } else {
            val listState = rememberLazyListState()
            LazyColumn(
                state = listState,
                modifier = Modifier.heightIn(max = 300.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(commentiState.commenti, key = { it.id }) { commento ->
                    Card(
                        Modifier.fillMaxWidth(),
                        shape  = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                            Column(Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(commento.autore,
                                        style      = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color      = MaterialTheme.colorScheme.primary)
                                }
                                Spacer(Modifier.height(4.dp))
                                Text(commento.testo, style = MaterialTheme.typography.bodyMedium)
                            }
                            // Elimina solo il proprio commento
                            if (currentUid != null && currentUid == commento.uid) {
                                IconButton(
                                    onClick  = { onElimina(commento.id) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.Delete, "Elimina",
                                        modifier = Modifier.size(16.dp),
                                        tint     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(12.dp))

        // Campo inserimento
        if (isLoggato) {
            OutlinedTextField(
                value         = testo,
                onValueChange = { if (it.length <= 300) testo = it },
                placeholder   = { Text("Scrivi un commento...") },
                modifier      = Modifier.fillMaxWidth(),
                shape         = RoundedCornerShape(12.dp),
                maxLines      = 4,
                trailingIcon  = {
                    Text("${testo.length}/300",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                        modifier = Modifier.padding(end = 8.dp))
                }
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick  = { if (testo.isNotBlank()) { onInvia(testo.trim()); testo = "" } },
                enabled  = testo.isNotBlank() && !commentiState.invioInCorso,
                modifier = Modifier.fillMaxWidth().height(50.dp),
                shape    = RoundedCornerShape(12.dp)
            ) {
                if (commentiState.invioInCorso)
                    CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                else
                    Text("Pubblica commento")
            }
        } else {
            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Text("Accedi per lasciare un commento.",
                    Modifier.padding(16.dp),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}