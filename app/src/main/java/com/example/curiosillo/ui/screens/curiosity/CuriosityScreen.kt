package com.example.curiosillo.ui.screens.curiosity

import android.content.Intent
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.curiosillo.ui.screens.utils.categoryImage
import com.example.curiosillo.ui.components.NotaBottomSheet
import com.example.curiosillo.ui.screens.utils.CommentiUiState
import com.example.curiosillo.ui.screens.utils.SegnalazioneBottomSheet
import com.example.curiosillo.ui.screens.utils.SegnalazioneUiState
import com.example.curiosillo.ui.screens.utils.emojiCategoria
import com.example.curiosillo.ui.theme.Success
import com.example.curiosillo.viewmodel.CuriosityUiState
import com.example.curiosillo.viewmodel.CuriosityViewModel
import com.example.curiosillo.viewmodel.GeminiUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CuriosityScreen(nav: NavController) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as CuriosityApplication
    val vm: CuriosityViewModel = viewModel(
        factory = CuriosityViewModel.Factory(app.repository, app.categoryPrefs, app.gamificationEngine, app.geminiPrefs, app.gamificationPrefs)
    )
    val state             by vm.state.collectAsState()
    val risultato         by vm.risultatoAzione.collectAsState()
    val commentiState     by vm.commentiState.collectAsState()
    val segnalazioneState by vm.segnalazioneState.collectAsState()
    val geminiState       by vm.geminiState.collectAsState()

    val coroutineScope = rememberCoroutineScope()

    var badgeDaMostrare    by remember { mutableStateOf<BadgeSbloccato?>(null) }
    var badgeQueue         by remember { mutableStateOf<List<BadgeSbloccato>>(emptyList()) }
    var mostraCommenti     by remember { mutableStateOf(false) }
    var mostraDialogIgnora by remember { mutableStateOf(false) }
    var mostraAvvenutaAzioneIgnora by remember { mutableStateOf(false) }
    var mostraAzioni       by remember { mutableStateOf(false) }
    var mostraGemini       by remember { mutableStateOf(false) }
    var triggerSegnala     by remember { mutableStateOf(false) }

    LaunchedEffect(risultato) {
        risultato?.let {
            if (it.badgeSbloccati.isNotEmpty()) {
                badgeQueue      = it.badgeSbloccati
                badgeDaMostrare = badgeQueue.first()
            }
            vm.consumaRisultato()
        }
    }

    // --- Dialogs ---

    if (state is CuriosityUiState.Success && (state as CuriosityUiState.Success).isRecuperata) {
        AlertDialog(
            onDismissRequest = { vm.dismissRecupero() },
            icon  = { Text("🔄", style = MaterialTheme.typography.displaySmall) },
            title = { Text("Pillola recuperata", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
            text  = { Text("Questa pillola era stata modificata ma non imparata. Te la ripropongo prima di passare alle nuove categorie!", textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.fillMaxWidth()) },
            confirmButton = {
                Button(
                    onClick = { vm.dismissRecupero() },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Ho capito", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold) }
            }
        )
    }

    if (mostraAvvenutaAzioneIgnora) {
        AlertDialog(
            onDismissRequest = { mostraAvvenutaAzioneIgnora = false },
            icon  = { Text("✨", style = MaterialTheme.typography.displaySmall) },
            title = { Text("Pillola nascosta", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
            text  = { Text("Questa curiosità non ti verrà più riproposta.\n\nLa trovi nella sezione \"Pillole nascoste\" del tuo profilo.", textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.fillMaxWidth()) },
            confirmButton = {
                Button(
                    onClick = { mostraAvvenutaAzioneIgnora = false },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    shape = RoundedCornerShape(14.dp)
                ) { Text("Ho capito", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold) }
            }
        )
    }

    badgeDaMostrare?.let { badge ->
        AlertDialog(
            onDismissRequest = {
                val r = badgeQueue.drop(1); badgeQueue = r; badgeDaMostrare = r.firstOrNull()
            },
            icon  = { Text(badge.icona, style = MaterialTheme.typography.displaySmall) },
            title = { Text("Badge sbloccato!", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
            text  = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(badge.nome, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(6.dp))
                    Text(badge.descrizione, style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            },
            confirmButton = {
                Button(onClick = {
                    val r = badgeQueue.drop(1); badgeQueue = r; badgeDaMostrare = r.firstOrNull()
                }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Ottimo!", style = MaterialTheme.typography.labelLarge) }
            }
        )
    }

    if (mostraDialogIgnora) {
        AlertDialog(
            onDismissRequest = { mostraDialogIgnora = false },
            icon  = { Text("🙈", style = MaterialTheme.typography.displaySmall) },
            title = { Text("Vuoi nascondere questa pillola?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
            text  = { Text("Non la vedrai più nei quiz o durante la lettura giornaliera.\n\nPotrai ripristinarla in qualsiasi momento dal profilo.", textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.fillMaxWidth()) },
            confirmButton = {
                Button(
                    onClick = {
                        mostraDialogIgnora = false
                        vm.toggleIgnora()
                        mostraAvvenutaAzioneIgnora = true
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Outlined.VisibilityOff, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Nascondi pillola", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { mostraDialogIgnora = false }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).padding(top = 8.dp)) {
                    Text("Annulla", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
        )
    }

    // --- Sheets ---

    if (mostraCommenti) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
        ModalBottomSheet(onDismissRequest = { mostraCommenti = false }, sheetState = sheetState) {
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
        ModalBottomSheet(onDismissRequest = { mostraGemini = false }, sheetState = sheetStateGemini) {
            GeminiSheet(geminiState)
        }
    }

    if (mostraAzioni && state is CuriosityUiState.Success) {
        val pillola = (state as CuriosityUiState.Success).curiosity
        val sheetStateAzioni = rememberModalBottomSheetState(skipPartiallyExpanded = false)
        val animateChiudiAzioni: () -> Unit = {
            coroutineScope.launch { sheetStateAzioni.hide() }.invokeOnCompletion { mostraAzioni = false }
        }

        ModalBottomSheet(onDismissRequest = { mostraAzioni = false }, sheetState = sheetStateAzioni) {
            // Aggiunto scrolling al menu azioni
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text("Azioni", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 16.dp))
                OutlinedButton(
                    onClick = {
                        animateChiudiAzioni()
                        val testo = "📚 ${pillola.title}\n\n${pillola.body}\n\n— Categoria: ${pillola.category}\nScoperto con Curiosillo 🎓"
                        ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply { type = "text/plain"; putExtra(Intent.EXTRA_TEXT, testo) }, "Condividi pillola"))
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    shape    = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Share, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Condividi pillola", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        animateChiudiAzioni()
                        coroutineScope.launch { delay(300); mostraDialogIgnora = true }
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    shape    = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Outlined.VisibilityOff, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Nascondi (Non mi interessa)",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        animateChiudiAzioni()
                        coroutineScope.launch { delay(300); triggerSegnala = true }
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                    shape    = RoundedCornerShape(14.dp),
                    border   = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Flag, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "Segnala errore o imprecisione",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pillola", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = { IconButton({ nav.popBackStack() }, modifier = Modifier.minimumInteractiveComponentSize()) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Indietro") } },
                actions = {
                    if (state is CuriosityUiState.Success) {
                        IconButton(onClick = { mostraAzioni = true }, modifier = Modifier.minimumInteractiveComponentSize()) {
                            Icon(Icons.Default.MoreVert, "Azioni", tint = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { pad ->
        val gradientBg = Brush.verticalGradient(listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
            MaterialTheme.colorScheme.background
        ))

        Box(Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = state,
                transitionSpec = {
                    if (targetState is CuriosityUiState.Learned) {
                        (fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 10 }) togetherWith
                                (fadeOut(tween(200)) + slideOutVertically(tween(200)) { -it / 10 })
                    } else {
                        fadeIn(tween(200)) togetherWith fadeOut(tween(150))
                    }
                },
                contentKey = { it::class },
                label = "stateTransition"
            ) { animState ->
                when (val s = animState) {
                    is CuriosityUiState.Loading -> Box(Modifier.fillMaxSize().background(gradientBg).padding(pad), Alignment.Center) { CircularProgressIndicator() }
                    is CuriosityUiState.Empty -> Box(Modifier.fillMaxSize().background(gradientBg).padding(pad), Alignment.Center) {
                        Text("Nessuna curiosità disponibile!\nTorna presto.", Modifier.padding(24.dp), textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyLarge)
                    }
                    is CuriosityUiState.Success -> {
                        CuriosityContent(s, pad, gradientBg,
                            commentiState = commentiState,
                            segnalazioneState = segnalazioneState,
                            triggerSegnalaExternally = triggerSegnala,
                            onSegnalaReset = { triggerSegnala = false },
                            onLearn      = { vm.markLearned() },
                            onBookmark   = { vm.toggleBookmark() },
                            onSalvaNota  = { vm.salvaNota(it) },
                            onSegnala    = { tipo, testo -> vm.inviaSegnalazione(tipo, testo) },
                            onCommenti   = { vm.caricaCommenti(); mostraCommenti = true },
                            onDimmiDiPiu = { vm.dimmiDiPiu(); mostraGemini = true }
                        )
                    }
                    is CuriosityUiState.Learned -> LearnedContent(pad, gradientBg) { vm.load() }
                }
            }

            Box(Modifier.fillMaxSize().padding(bottom = 120.dp), contentAlignment = Alignment.BottomCenter) {
                AnimatedVisibility(
                    visible = segnalazioneState is SegnalazioneUiState.Successo,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    LaunchedEffect(Unit) { delay(3000); vm.dismissSegnalazione() }
                    Surface(color = Success, shape = RoundedCornerShape(12.dp), shadowElevation = 6.dp) {
                        Row(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CheckCircle, null, tint = Color.White)
                            Spacer(Modifier.width(8.dp))
                            Text("Segnalazione inviata. Grazie!", style = MaterialTheme.typography.labelLarge, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                AnimatedVisibility(
                    visible = segnalazioneState is SegnalazioneUiState.Errore,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    val sState = segnalazioneState as? SegnalazioneUiState.Errore
                    LaunchedEffect(Unit) { delay(3500); vm.dismissSegnalazione() }
                    Surface(color = MaterialTheme.colorScheme.error, shape = RoundedCornerShape(12.dp), shadowElevation = 6.dp) {
                        Text(sState?.msg ?: "", color = Color.White, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, modifier = Modifier.padding(16.dp))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CuriosityContent(
    s:                        CuriosityUiState.Success,
    pad:                      PaddingValues,
    gradientBg:               Brush,
    commentiState: CommentiUiState,
    segnalazioneState: SegnalazioneUiState,
    triggerSegnalaExternally: Boolean = false,
    onSegnalaReset:           () -> Unit = {},
    onLearn:                  () -> Unit,
    onBookmark:               () -> Unit,
    onSalvaNota:              (String) -> Unit,
    onSegnala:                (tipo: String, testo: String) -> Unit,
    onCommenti:               () -> Unit,
    onDimmiDiPiu:             () -> Unit
) {
    var mostraSegnalazione by remember { mutableStateOf(false) }
    var mostraNota         by remember { mutableStateOf(false) }

    LaunchedEffect(triggerSegnalaExternally) { if (triggerSegnalaExternally) { mostraSegnalazione = true; onSegnalaReset() } }
    LaunchedEffect(segnalazioneState) { if (segnalazioneState is SegnalazioneUiState.Successo) mostraSegnalazione = false }

    if (mostraNota) {
        NotaBottomSheet(notaAttuale = s.curiosity.nota, onSalva = onSalvaNota, onChiudi = { mostraNota = false })
    }

    Column(Modifier.fillMaxSize().background(gradientBg).padding(pad).verticalScroll(rememberScrollState())) {
        Image(
            painter            = painterResource(id = categoryImage(s.curiosity.category)),
            contentDescription = s.curiosity.category,
            modifier           = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)),
            contentScale = ContentScale.Crop
        )
        Column(Modifier.padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                SuggestionChip(onClick = {}, label = { Text(emojiCategoria(s.curiosity.category) + " " + s.curiosity.category, style = MaterialTheme.typography.labelLarge) })
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { mostraNota = true }, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(Icons.Default.EditNote, "Nota", tint = if (s.curiosity.nota.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    }
                    IconToggleButton(checked = s.curiosity.isBookmarked, onCheckedChange = { onBookmark() }, modifier = Modifier.minimumInteractiveComponentSize()) {
                        Icon(imageVector = if (s.curiosity.isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder, contentDescription = "Salva", tint = if (s.curiosity.isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    }
                }
            }

            if (s.curiosity.nota.isNotBlank()) {
                Card(Modifier.fillMaxWidth().padding(bottom = 8.dp), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                        Text("📝 ", style = MaterialTheme.typography.bodySmall)
                        Text(s.curiosity.nota, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(s.curiosity.title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(20.dp))

            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(4.dp)) {
                Text(s.curiosity.body, Modifier.padding(20.dp), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.height(16.dp))

            // FIX: Tasti con altezza dinamica e wrapping del testo (Immagine 4)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                val giaSvelato = s.curiosity.approfondimentoAi != null
                OutlinedButton(onClick = onDimmiDiPiu, modifier = Modifier.weight(1f).heightIn(min = 48.dp), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)) {
                    Icon(if (giaSvelato) Icons.Default.CheckCircle else Icons.Default.AutoAwesome, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (giaSvelato) "Già svelato! 🐾" else "Dimmi di più", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }
                OutlinedButton(onClick = onCommenti, modifier = Modifier.weight(1f).heightIn(min = 48.dp), shape = RoundedCornerShape(12.dp), border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)) {
                    Icon(Icons.Default.ChatBubbleOutline, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Commenti (${commentiState.commenti.size})", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }
            }

            if (mostraSegnalazione) SegnalazioneBottomSheet(
                onInvia = { tipo, testo ->
                    onSegnala(
                        tipo,
                        testo
                    )
                },
                onDismiss = { mostraSegnalazione = false },
                isLoading = segnalazioneState is SegnalazioneUiState.Loading
            )

            Spacer(Modifier.height(16.dp))
            AnimatedContent(
                targetState = s.curiosity.isRead,
                transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(150)) },
                label = "learnButton"
            ) { isRead ->
                if (!isRead) {
                    Button(onLearn, Modifier.fillMaxWidth().heightIn(min = 58.dp), shape = RoundedCornerShape(16.dp)) { Text("Ho imparato!", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) }
                } else {
                    OutlinedButton(onLearn, Modifier.fillMaxWidth().heightIn(min = 58.dp), shape = RoundedCornerShape(16.dp)) { Text("Prossima curiosità", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) }
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
fun GeminiSheet(state: GeminiUiState) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp).padding(bottom = 48.dp).heightIn(min = 300.dp, max = 500.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = if (state.errore != null) Icons.Default.Warning else Icons.Default.AutoAwesome, contentDescription = null, tint = if (state.errore != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(text = if (state.errore != null && state.rimanenti == 0) "Limite raggiunto" else if (state.errore != null) "Oops! Servizio non disponibile" else "Approfondimento da Curiosillo", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = if (state.errore != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
                Text(text = "Rimanenti oggi: ${state.rimanenti}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        }
        Spacer(Modifier.height(20.dp))
        if (state.isLoading) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally) { CircularProgressIndicator(); Spacer(Modifier.height(12.dp)); Text("Curiosillo sta pensando...", style = MaterialTheme.typography.bodyMedium) } }
        } else if (state.errore != null) {
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(20.dp)) {
                    Text("Al momento Curiosillo non può essere contattato. Riprova più tardi.", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onErrorContainer)
                    if (state.rimanenti > 0) { Spacer(Modifier.height(12.dp)); Text("Assicurati di avere una connessione internet attiva o riprova tra qualche minuto.\n\nGrazie della comprensione.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)) }
                }
            }
        } else {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                Text(text = state.risposta, style = MaterialTheme.typography.bodyLarge)
                if (state.isScritturaInCorso) Box(Modifier.padding(top = 4.dp).size(12.dp, 20.dp).background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)))
            }
        }
    }
}

@Composable
private fun LearnedContent(pad: PaddingValues, gradientBg: Brush, onNext: () -> Unit) {
    Column(Modifier.fillMaxSize().background(gradientBg).padding(pad).padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.CheckCircle, null, Modifier.size(90.dp), tint = Success)
        Spacer(Modifier.height(20.dp)); Text("Fantastico!", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(10.dp)); Text("Hai imparato qualcosa di nuovo!\nOra puoi fare il quiz su questa curiosità.", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        Spacer(Modifier.height(32.dp)); Button(onNext, Modifier.fillMaxWidth().heightIn(min = 58.dp), shape = RoundedCornerShape(16.dp)) { Text("Prossima curiosità", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) }
    }
}

@Composable
fun CommentiSheet(commentiState: CommentiUiState, currentUid: String?, isLoggato: Boolean, onInvia: (String) -> Unit, onElimina: (String) -> Unit, onDismissError: () -> Unit) {
    var testo by remember { mutableStateOf("") }
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 32.dp)) {
        Text("Commenti", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold); Spacer(Modifier.height(12.dp))
        commentiState.erroreInvio?.let { err -> Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) { Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) { Text(err, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f)); TextButton(onClick = onDismissError, modifier = Modifier.minimumInteractiveComponentSize()) { Text("OK", style = MaterialTheme.typography.labelLarge) } } }; Spacer(Modifier.height(8.dp)) }
        if (commentiState.isLoading) { CircularProgressIndicator(Modifier.align(Alignment.CenterHorizontally).padding(vertical = 16.dp)) }
        else if (commentiState.commenti.isEmpty()) { Text("Nessun commento ancora. Sii il primo!", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(vertical = 16.dp).fillMaxWidth(), textAlign = TextAlign.Center) }
        else {
            LazyColumn(modifier = Modifier.heightIn(max = 300.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(commentiState.commenti, key = { it.id }) { commento ->
                    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                            Column(Modifier.weight(1f)) {
                                Text(commento.autore, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.height(4.dp)); Text(commento.testo, style = MaterialTheme.typography.bodyMedium)
                            }
                            if (currentUid != null && currentUid == commento.uid) IconButton(onClick = { onElimina(commento.id) }, modifier = Modifier.size(48.dp).minimumInteractiveComponentSize()) { Icon(Icons.Default.Delete, "Elimina", modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)) }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(16.dp)); HorizontalDivider(); Spacer(Modifier.height(12.dp))
        if (isLoggato) {
            OutlinedTextField(value = testo, onValueChange = { if (it.length <= 300) testo = it }, placeholder = { Text("Scrivi un commento...", style = MaterialTheme.typography.bodyMedium) }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp), maxLines = 4, trailingIcon = { Text("${testo.length}/300", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), modifier = Modifier.padding(end = 8.dp)) })
            Spacer(Modifier.height(8.dp)); Button(onClick = { if (testo.isNotBlank()) { onInvia(testo.trim()); testo = "" } }, enabled = testo.isNotBlank() && !commentiState.invioInCorso, modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp), shape = RoundedCornerShape(12.dp)) { if (commentiState.invioInCorso) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp) else Text("Pubblica commento", style = MaterialTheme.typography.labelLarge) }
        } else { Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) { Text("Accedi per lasciare un commento.", Modifier.padding(16.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) } }
    }
}
