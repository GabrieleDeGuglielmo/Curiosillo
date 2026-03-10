package com.example.curiosillo.ui.screens

import android.content.Intent
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.curiosillo.CuriosityApplication
import com.example.curiosillo.ui.components.CuriosilloBottomBar
import com.example.curiosillo.repository.DuelloRepository
import com.example.curiosillo.viewmodel.DuelloUiState
import com.example.curiosillo.viewmodel.DuelloViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DuelloScreen(nav: NavController) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as CuriosityApplication
    val vm: DuelloViewModel = viewModel(
        factory = DuelloViewModel.Factory(app.repository, DuelloRepository(), ctx)
    )
    val state by vm.state.collectAsState()

    val inPartita = state is DuelloUiState.InCorso
            || state is DuelloUiState.Pausa
            || state is DuelloUiState.InCountdown
    val inAttesa  = state is DuelloUiState.InAttesa

    var showAbbandonaDialog by remember { mutableStateOf(false) }

    // Back fisico: durante partita/countdown chiede conferma, altrimenti annulla e torna indietro
    BackHandler(enabled = inPartita || inAttesa) {
        if (inPartita) showAbbandonaDialog = true
        else { vm.abbandonaDuello(); nav.popBackStack() }
    }

    val gradientBg = Brush.verticalGradient(
        listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
            MaterialTheme.colorScheme.background
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Duello") },
                navigationIcon = {
                    IconButton(onClick = {
                        val inP = state is DuelloUiState.InCorso
                                || state is DuelloUiState.Pausa
                                || state is DuelloUiState.InCountdown
                        if (inP) showAbbandonaDialog = true
                        else { vm.annullaRicerca(); nav.popBackStack() }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Indietro")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar = {
            val mostraBar = state is DuelloUiState.Idle || state is DuelloUiState.Risultati
            if (mostraBar) CuriosilloBottomBar(nav)
        },
        containerColor = Color.Transparent
    ) { pad ->
        Box(
            Modifier
                .fillMaxSize()
                .background(gradientBg)
                .padding(pad)
        ) {
            // ── Contenuto principale ──────────────────────────────────────────
            when (val s = state) {
                is DuelloUiState.Idle -> LobbyContent(
                    onCasuale = { vm.cercaAvversarioCasuale() },
                    onAmico   = { vm.creaStanza() },
                    onCodice  = { codice -> vm.uniscitiConCodice(codice) }
                )
                is DuelloUiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(16.dp))
                        Text("Preparazione duello...", style = MaterialTheme.typography.bodyLarge)
                    }
                }
                is DuelloUiState.InAttesa -> AttesaContent(
                    stato     = s,
                    context   = ctx,
                    onAnnulla = { vm.annullaRicerca(); nav.popBackStack() }
                )
                is DuelloUiState.InCountdown -> CountdownContent(stato = s)
                is DuelloUiState.InCorso -> PartitaContent(
                    stato      = s,
                    onRisposta = { vm.rispondi(it) }
                )
                is DuelloUiState.Pausa -> PausaContent(stato = s)
                is DuelloUiState.Risultati -> RisultatiContent(
                    stato   = s,
                    onRivai = { vm.reset() },
                    onEsci  = { vm.reset(); nav.popBackStack() }
                )
                is DuelloUiState.Errore -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Text("⚠️", fontSize = 48.sp)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            s.messaggio,
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(Modifier.height(24.dp))
                        Button(onClick = { vm.reset() }) { Text("Torna indietro") }
                    }
                }
            }

            // ── Dialog conferma abbandono ─────────────────────────────────────
            if (showAbbandonaDialog) {
                AlertDialog(
                    onDismissRequest = { showAbbandonaDialog = false },
                    title = { Text("Abbandona il duello?") },
                    text  = { Text("Se esci ora, il duello verrà annullato e l'avversario sarà avvisato.") },
                    confirmButton = {
                        Button(
                            onClick = {
                                showAbbandonaDialog = false
                                vm.abbandonaDuello()
                                nav.popBackStack()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) { Text("Abbandona") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showAbbandonaDialog = false }) { Text("Resta") }
                    }
                )
            }


        }
    }
}

// ── Lobby ─────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LobbyContent(
    onCasuale: () -> Unit,
    onAmico:   () -> Unit,
    onCodice:  (String) -> Unit
) {
    var mostraInputCodice by remember { mutableStateOf(false) }
    var codiceInput       by remember { mutableStateOf("") }

    Column(
        Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("⚔️", fontSize = 64.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            "Modalità Duello",
            style      = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            textAlign  = TextAlign.Center
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "10 domande • 10 secondi a domanda",
            style     = MaterialTheme.typography.bodyMedium,
            color     = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(40.dp))

        Button(
            onClick  = onCasuale,
            modifier = Modifier.fillMaxWidth().height(58.dp),
            shape    = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Search, null, Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Text("Cerca avversario casuale", style = MaterialTheme.typography.titleMedium)
        }

        Spacer(Modifier.height(14.dp))

        OutlinedButton(
            onClick  = onAmico,
            modifier = Modifier.fillMaxWidth().height(58.dp),
            shape    = RoundedCornerShape(16.dp)
        ) {
            Icon(Icons.Default.Person, null, Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
            Text("Crea stanza per un amico", style = MaterialTheme.typography.titleMedium)
        }

        Spacer(Modifier.height(14.dp))

        OutlinedButton(
            onClick  = { mostraInputCodice = !mostraInputCodice },
            modifier = Modifier.fillMaxWidth().height(58.dp),
            shape    = RoundedCornerShape(16.dp)
        ) {
            Text("Inserisci codice stanza", style = MaterialTheme.typography.titleMedium)
        }

        AnimatedVisibility(visible = mostraInputCodice) {
            Column(Modifier.fillMaxWidth()) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value         = codiceInput,
                    onValueChange = { codiceInput = it.uppercase().take(6) },
                    placeholder   = { Text("Es. ABCDEF") },
                    label         = { Text("Codice stanza") },
                    singleLine    = true,
                    modifier      = Modifier.fillMaxWidth(),
                    shape         = RoundedCornerShape(14.dp)
                )
                Spacer(Modifier.height(10.dp))
                Button(
                    onClick  = { if (codiceInput.length == 6) onCodice(codiceInput) },
                    enabled  = codiceInput.length == 6,
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    shape    = RoundedCornerShape(14.dp)
                ) {
                    Text("Unisciti")
                }
            }
        }
    }
}

// ── Attesa avversario ─────────────────────────────────────────────────────────

@Composable
private fun AttesaContent(
    stato:     DuelloUiState.InAttesa,
    context:   android.content.Context,
    onAnnulla: () -> Unit
) {
    val clipboard = LocalClipboardManager.current
    val isCasuale = stato.codice.isBlank()

    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "attesa")
        val alpha by infiniteTransition.animateFloat(
            initialValue  = 0.3f,
            targetValue   = 1f,
            animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse),
            label         = "alpha"
        )

        Text("⏳", fontSize = 56.sp)
        Spacer(Modifier.height(20.dp))
        Text(
            if (isCasuale) "In attesa di un avversario..." else "In attesa che l'amico si unisca...",
            style      = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign  = TextAlign.Center,
            color      = MaterialTheme.colorScheme.onBackground.copy(alpha = alpha)
        )
        Spacer(Modifier.height(8.dp))
        CircularProgressIndicator(Modifier.size(32.dp))

        if (!isCasuale) {
            Spacer(Modifier.height(32.dp))
            Text(
                "Codice stanza",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
            Spacer(Modifier.height(10.dp))
            Card(
                shape  = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Text(
                    stato.codice,
                    modifier      = Modifier.padding(horizontal = 32.dp, vertical = 16.dp),
                    fontSize      = 38.sp,
                    fontWeight    = FontWeight.ExtraBold,
                    letterSpacing = 8.sp,
                    color         = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = { clipboard.setText(AnnotatedString(stato.codice)) },
                    shape   = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.ContentCopy, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Copia")
                }
                OutlinedButton(
                    onClick = {
                        val intent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(
                                Intent.EXTRA_TEXT,
                                "Sfidami su Curiosillo! Codice stanza: ${stato.codice} ⚔️"
                            )
                        }
                        context.startActivity(Intent.createChooser(intent, "Condividi codice"))
                    },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Share, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Condividi")
                }
            }
        }

        Spacer(Modifier.height(40.dp))
        TextButton(onClick = onAnnulla) {
            Text("Annulla", color = MaterialTheme.colorScheme.error)
        }
    }
}

// ── Partita ───────────────────────────────────────────────────────────────────

@Composable
private fun PartitaContent(
    stato:      DuelloUiState.InCorso,
    onRisposta: (String) -> Unit
) {
    val domanda     = stato.duello.domande.getOrNull(stato.indiceCorrente) ?: return
    val avvUid      = stato.duello.avversarioUid(stato.mioUid)
    val avvUser     = avvUid?.let { stato.duello.giocatori[it]?.username } ?: "Avversario"
    val avvRisposto = stato.risposteCorrentiAvversario > stato.indiceCorrente

    val seed     = (stato.duello.id + stato.indiceCorrente).hashCode().toLong()
    val risposte = remember(stato.indiceCorrente, stato.duello.id) {
        domanda.risposteShuffled(seed)
    }

    val timerColor = when {
        stato.secondiRimasti > 6 -> Color(0xFF4CAF50)
        stato.secondiRimasti > 3 -> Color(0xFFFF9800)
        else                     -> Color(0xFFF44336)
    }

    Column(
        Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Domanda ${stato.indiceCorrente + 1} di ${stato.duello.domande.size}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
        )
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress   = { (stato.indiceCorrente + 1f) / stato.duello.domande.size },
            modifier   = Modifier.fillMaxWidth().height(6.dp),
            color      = Color(0xFFFF9800),
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Spacer(Modifier.height(10.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.CenterVertically
        ) {
            PunteggioChip(
                user      = stato.mioUser,
                punteggio = stato.duello.punteggio(stato.mioUid),
                isMe      = true
            )
            Box(
                Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(timerColor.copy(alpha = 0.15f))
                    .border(2.dp, timerColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("${stato.secondiRimasti}", fontSize = 22.sp, color = timerColor)
            }
            PunteggioChip(
                user      = avvUser,
                punteggio = stato.duello.punteggio(avvUid ?: ""),
                isMe      = false
            )
        }

        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            AnimatedVisibility(visible = avvRisposto) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        "✓ $avvUser ha risposto",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style    = MaterialTheme.typography.labelSmall,
                        color    = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Card(
            Modifier.fillMaxWidth(),
            shape     = RoundedCornerShape(20.dp),
            colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Text(
                domanda.questionText,
                modifier   = Modifier.padding(20.dp),
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                textAlign  = TextAlign.Center,
                color      = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(Modifier.height(20.dp))
        risposte.forEach { risposta ->
            val isSelezionata = stato.rispostaData == risposta
            val isCorretta    = risposta == domanda.correctAnswer
            val haRisposto    = stato.rispostaData != null

            val bgColor = when {
                !haRisposto                  -> MaterialTheme.colorScheme.surface
                isSelezionata && isCorretta  -> Color(0xFF4CAF50)
                isSelezionata && !isCorretta -> Color(0xFFF44336)
                haRisposto && isCorretta     -> Color(0xFF4CAF50).copy(alpha = 0.3f)
                else                         -> MaterialTheme.colorScheme.surface
            }

            Card(
                onClick   = { if (!haRisposto) onRisposta(risposta) },
                modifier  = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                shape     = RoundedCornerShape(14.dp),
                colors    = CardDefaults.cardColors(containerColor = bgColor),
                elevation = CardDefaults.cardElevation(if (haRisposto) 0.dp else 2.dp)
            ) {
                Text(
                    risposta,
                    modifier   = Modifier
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                        .fillMaxWidth(),
                    style      = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelezionata) FontWeight.Bold else FontWeight.Normal,
                    color      = when {
                        isSelezionata || (haRisposto && isCorretta) -> Color.White
                        else -> MaterialTheme.colorScheme.onSurface
                    }
                )
            }
        }
    }
}

@Composable
private fun PunteggioChip(user: String, punteggio: Int, isMe: Boolean) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (isMe) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.secondaryContainer
    ) {
        Column(
            Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                user,
                style      = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color      = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSecondaryContainer,
                maxLines   = 1
            )
            Text(
                "$punteggio pt",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color      = if (isMe) MaterialTheme.colorScheme.onPrimaryContainer
                else MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

// ── Pausa tra domande ─────────────────────────────────────────────────────────

@Composable
private fun PausaContent(stato: DuelloUiState.Pausa) {
    val isCorretta    = stato.eraCorretta
    val nonHaRisposto = stato.miaRisposta == null

    val bgColor   = when {
        nonHaRisposto -> MaterialTheme.colorScheme.surfaceVariant
        isCorretta    -> Color(0xFF1B5E20)
        else          -> Color(0xFFB71C1C)
    }
    val textColor = if (nonHaRisposto) Color.Black else Color.White
    val emoji     = when {
        nonHaRisposto -> "⏰"
        isCorretta    -> "✅"
        else          -> "❌"
    }
    val titolo    = when {
        nonHaRisposto -> "Tempo scaduto!"
        isCorretta    -> "Risposta corretta!"
        else          -> "Risposta errata"
    }

    Box(
        Modifier.fillMaxSize().background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Text(emoji, fontSize = 64.sp)
            Spacer(Modifier.height(16.dp))
            Text(
                titolo,
                style      = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.ExtraBold,
                color      = textColor,
                textAlign  = TextAlign.Center
            )
            Spacer(Modifier.height(20.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape    = RoundedCornerShape(16.dp),
                colors   = CardDefaults.cardColors(
                    containerColor = if (nonHaRisposto) Color.Black.copy(alpha = 0.05f)
                    else Color.White.copy(alpha = 0.15f)
                )
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Risposta corretta:",
                        style = MaterialTheme.typography.labelLarge,
                        color = textColor.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        stato.rispostaCorretta,
                        style      = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        color      = textColor
                    )
                }
            }

            if (!nonHaRisposto && !isCorretta) {
                Spacer(Modifier.height(10.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape    = RoundedCornerShape(16.dp),
                    colors   = CardDefaults.cardColors(
                        containerColor = Color.White.copy(alpha = 0.10f)
                    )
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "La tua risposta:",
                            style = MaterialTheme.typography.labelLarge,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            stato.miaRisposta ?: "",
                            style      = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color      = Color.White.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))
            Text(
                "Prossima domanda tra ${stato.secondiRimasti}…",
                style     = MaterialTheme.typography.bodyMedium,
                color     = textColor.copy(alpha = 0.65f),
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── Risultati ─────────────────────────────────────────────────────────────────

@Composable
private fun RisultatiContent(
    stato:   DuelloUiState.Risultati,
    onRivai: () -> Unit,
    onEsci:  () -> Unit
) {
    val haVinto  = stato.mioPunteggio > stato.avvPunteggio
    val pareggio = stato.mioPunteggio == stato.avvPunteggio

    Column(
        Modifier.fillMaxSize().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            when { pareggio -> "🤝"; haVinto -> "🏆"; else -> "😔" },
            fontSize = 72.sp
        )
        Spacer(Modifier.height(16.dp))
        Text(
            when { pareggio -> "Pareggio!"; haVinto -> "Hai vinto!"; else -> "Hai perso!" },
            style      = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.ExtraBold,
            textAlign  = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            RisultatoCard(
                modifier  = Modifier.weight(1f),
                user      = stato.mioUser,
                punteggio = stato.mioPunteggio,
                totale    = stato.duello.domande.size,
                isMe      = true,
                haVinto   = haVinto
            )
            RisultatoCard(
                modifier  = Modifier.weight(1f),
                user      = stato.avvUser,
                punteggio = stato.avvPunteggio,
                totale    = stato.duello.domande.size,
                isMe      = false,
                haVinto   = !haVinto && !pareggio
            )
        }

        Spacer(Modifier.height(36.dp))
        Button(
            onClick  = onRivai,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape    = RoundedCornerShape(16.dp)
        ) {
            Text(
                "Gioca ancora",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick  = onEsci,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape    = RoundedCornerShape(16.dp)
        ) {
            Text("Esci", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun RisultatoCard(
    modifier:  Modifier,
    user:      String,
    punteggio: Int,
    totale:    Int,
    isMe:      Boolean,
    haVinto:   Boolean
) {
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(18.dp),
        colors    = CardDefaults.cardColors(
            containerColor = if (haVinto) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(if (haVinto) 6.dp else 2.dp)
    ) {
        Column(
            Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (haVinto) Text("🏆", fontSize = 24.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                user,
                style      = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color      = if (haVinto) Color.White
                else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines   = 1,
                textAlign  = TextAlign.Center
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "$punteggio/$totale",
                fontSize   = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                color      = if (haVinto) Color.White
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                if (isMe) "Tu" else "Avversario",
                style = MaterialTheme.typography.bodySmall,
                color = if (haVinto) Color.White.copy(alpha = 0.8f)
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

// ── Countdown pre-partita ─────────────────────────────────────────────────────

@Composable
private fun CountdownContent(stato: DuelloUiState.InCountdown) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            shape     = RoundedCornerShape(24.dp),
            colors    = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ),
            elevation = CardDefaults.cardElevation(12.dp),
            modifier  = Modifier.padding(32.dp)
        ) {
            Column(
                Modifier.padding(horizontal = 48.dp, vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("⚔️", fontSize = 52.sp)
                Spacer(Modifier.height(12.dp))
                Text(
                    "Avversario trovato!",
                    style      = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color      = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Il duello inizia tra",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    "${stato.secondiRimasti}",
                    fontSize   = 72.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color      = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}