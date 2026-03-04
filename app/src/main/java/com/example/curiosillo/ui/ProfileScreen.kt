package com.example.curiosillo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.curiosillo.CuriosityApplication
import com.example.curiosillo.data.BadgeCatalogo
import com.example.curiosillo.data.BadgeDefinizione
import com.example.curiosillo.data.BadgeSbloccato
import com.example.curiosillo.ui.theme.Error
import com.example.curiosillo.ui.theme.Primary
import com.example.curiosillo.ui.theme.Secondary
import com.example.curiosillo.ui.theme.Tertiary
import com.example.curiosillo.ui.theme.Success
import com.example.curiosillo.ui.components.GamificationBanner
import com.example.curiosillo.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(nav: NavController) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as CuriosityApplication
    val vm: ProfileViewModel = viewModel(
        factory = ProfileViewModel.Factory(app.repository, app.gamificationPrefs)
    )
    val state by vm.state.collectAsState()
    var showResetDialog by remember { mutableStateOf(false) }
    var badgeDettaglio by remember { mutableStateOf<BadgeDefinizione?>(null) }
    var badgeSbloccatoAppena by remember { mutableStateOf<BadgeSbloccato?>(null) }

    // Dialog dettaglio badge (click sulla griglia)
    badgeDettaglio?.let { def ->
        val sbloccato = def.id in state.badgeSbloccati.map { it.id }
        AlertDialog(
            onDismissRequest = { badgeDettaglio = null },
            icon = { Text(def.icona, fontSize = 40.sp) },
            title = { Text(def.nome, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (sbloccato) {
                        Text(
                            def.descrizione,
                            textAlign = TextAlign.Center,
                            color = Color.Gray
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "✅ Badge sbloccato!",
                            fontWeight = FontWeight.SemiBold,
                            color = Success
                        )
                    } else {
                        Text(
                            "Per sbloccare questo badge:",
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Text(
                            condizioneBadge(def.id),
                            textAlign = TextAlign.Center,
                            color = Color.Gray
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { badgeDettaglio = null }) {
                    Text("Chiudi")
                }
            }
        )
    }

// Dialog sblocco badge appena guadagnato
    badgeSbloccatoAppena?.let { badge ->
        AlertDialog(
            onDismissRequest = { badgeSbloccatoAppena = null },
            icon = { Text(badge.icona, fontSize = 48.sp) },
            title = {
                Text(
                    "Badge sbloccato!", fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        badge.nome,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        badge.descrizione,
                        textAlign = TextAlign.Center,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {
                Button(onClick = { badgeSbloccatoAppena = null }) {
                    Text("Ottimo!", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            icon = { Icon(Icons.Default.Delete, null, tint = Error) },
            title = { Text("Resetta i progressi?", fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Tutte le curiosità torneranno a \"non lette\", il quiz verrà bloccato " +
                            "e XP, streak e badge verranno azzerati. Questa azione non può essere annullata.",
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = { vm.resetProgressi(); showResetDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Error)
                ) { Text("Sì, resetta", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                OutlinedButton(onClick = { showResetDialog = false }) { Text("Annulla") }
            }
        )
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Il mio profilo", fontWeight = FontWeight.SemiBold) },
            navigationIcon = {
                IconButton(onClick = { nav.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, "Indietro")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )
    }) { pad ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFFFFF3E0), Color.White)))
                .padding(pad)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(Modifier.height(12.dp))

                    // ── Avatar ────────────────────────────────────────────────
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .background(Primary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Person, null,
                            modifier = Modifier.size(52.dp), tint = Color.White
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Il mio profilo",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(20.dp))

// ── Banner gamification ───────────────────────────────────────────
                    GamificationBanner(
                        xpTotali       = state.xpTotali,
                        streakCorrente = state.streakCorrente
                    )
                    Spacer(Modifier.height(8.dp))

// Streak massima con icona informativa
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Streak massima: ${state.streakMassima} ${if (state.streakMassima == 1) "giorno" else "giorni"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        Spacer(Modifier.width(6.dp))
                        var showStreakInfo by remember { mutableStateOf(false) }
                        IconButton(
                            onClick  = { showStreakInfo = true },
                            modifier = Modifier.size(18.dp)
                        ) {
                            Icon(Icons.Default.Info, "Info streak",
                                modifier = Modifier.size(14.dp),
                                tint     = Color.Gray)
                        }
                        if (showStreakInfo) {
                            AlertDialog(
                                onDismissRequest = { showStreakInfo = false },
                                icon  = { Text("🔥", fontSize = 32.sp) },
                                title = {
                                    Box(
                                        modifier = Modifier.fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "Come funziona la streak?",
                                            fontWeight = FontWeight.Bold,
                                            textAlign  = TextAlign.Center
                                        )
                                    }
                                },
                                text  = {
                                    Text(
                                        "La streak conta i giorni consecutivi in cui hai letto almeno una pillola " +
                                                "e premuto \"Ho imparato!\"\n\n" +
                                                "Se salti un giorno, la streak riparte da 1.\n\n" +
                                                "Più alta è la streak, più XP bonus guadagni ogni giorno!",
                                        textAlign = TextAlign.Center,
                                        color     = Color.Gray,
                                        modifier   = Modifier.fillMaxWidth()
                                    )
                                },
                                confirmButton = {
                                    TextButton(onClick = { showStreakInfo = false }) { Text("Capito!") }
                                }
                            )
                        }
                    }
                    Spacer(Modifier.height(24.dp))

                    // ── Statistiche ───────────────────────────────────────────
                    Text(
                        "Le mie statistiche",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF555555),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            Modifier.weight(1f),
                            "${state.curiositàImparate}",
                            "Curiosità\nimparate",
                            Primary
                        )
                        StatCard(
                            Modifier.weight(1f),
                            "${state.totalCuriosità}",
                            "Curiosità\ntotali",
                            Secondary
                        )
                        StatCard(
                            Modifier.weight(1f),
                            "${state.quizDisponibili}",
                            "Quiz non\nrisposti",
                            Tertiary
                        )
                    }

                    // ── Barra progresso ───────────────────────────────────────
                    if (state.totalCuriosità > 0) {
                        Spacer(Modifier.height(20.dp))
                        val percentuale = state.curiositàImparate.toFloat() / state.totalCuriosità
                        Text(
                            "Progresso complessivo — ${(percentuale * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF555555),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { percentuale },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp),
                            color = Primary,
                            trackColor = Color(0xFFE0E0E0)
                        )
                    }

                    // ── Badge ─────────────────────────────────────────────────
                    Spacer(Modifier.height(28.dp))
                    Text(
                        "Badge (${state.badgeSbloccati.size}/${BadgeCatalogo.tutti.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF555555),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))

                    val idSbloccati = state.badgeSbloccati.map { it.id }.toSet()
                    BadgeCatalogo.tutti.chunked(3).forEach { riga ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            riga.forEach { def ->
                                BadgeCard(
                                    definizione = def,
                                    sbloccato   = def.id in idSbloccati,
                                    modifier    = Modifier.weight(1f),
                                    onClick     = { badgeDettaglio = def }   // ← aggiunto
                                )
                            }
                            repeat(3 - riga.size) { Spacer(Modifier.weight(1f)) }
                        }
                        Spacer(Modifier.height(10.dp))
                    }

                    // ── Preferiti ─────────────────────────────────────────────
                    Spacer(Modifier.height(28.dp))
                    Button(
                        onClick = { nav.navigate("preferiti") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Primary)
                    ) {
                        Icon(Icons.Default.Bookmark, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            "I miei preferiti (${state.totaleBookmark})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // ── Reset ─────────────────────────────────────────────────
                    OutlinedButton(
                        onClick = { showResetDialog = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Error),
                        border = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(Error)
                        )
                    ) {
                        Icon(Icons.Default.Delete, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Resetta progressi",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun BadgeCard(
    definizione: BadgeDefinizione,
    sbloccato:   Boolean,
    modifier:    Modifier,
    onClick:     () -> Unit
) {
    Card(
        onClick   = onClick,
        modifier  = modifier.height(110.dp),
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(
            containerColor = if (sbloccato) Color(0xFF1A1A2E) else Color(0xFFF0F0F0)
        ),
        elevation = CardDefaults.cardElevation(if (sbloccato) 4.dp else 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(if (sbloccato) definizione.icona else "🔒", fontSize = 28.sp)
            Spacer(Modifier.height(4.dp))
            Text(
                definizione.nome,
                style      = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                textAlign  = TextAlign.Center,
                color      = if (sbloccato) Color.White else Color(0xFFAAAAAA),
                maxLines   = 2,
                overflow   = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StatCard(modifier: Modifier, valore: String, etichetta: String, colore: Color) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = colore),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 20.dp, horizontal = 8.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(valore, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Spacer(Modifier.height(4.dp))
            Text(
                etichetta,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center
            )
        }
    }
}

private fun condizioneBadge(id: String): String = when (id) {
    "prima_pillola"  -> "Leggi la tua prima curiosità e premi \"Ho imparato!\""
    "prima_risposta" -> "Completa il tuo primo quiz"
    "streak_3"       -> "Leggi almeno una pillola al giorno per 3 giorni consecutivi"
    "streak_7"       -> "Leggi almeno una pillola al giorno per 7 giorni consecutivi"
    "streak_30"      -> "Leggi almeno una pillola al giorno per 30 giorni consecutivi"
    "perfetto_5"     -> "Rispondi correttamente a 5 domande di fila senza sbagliare"
    "pillole_10"     -> "Leggi e impara 10 pillole"
    "pillole_20"     -> "Leggi e impara 20 pillole"
    "pillole_50"     -> "Leggi e impara 50 pillole"
    "livello_3"      -> "Accumula abbastanza XP per raggiungere il livello 3 (250 XP)"
    "livello_5"      -> "Accumula abbastanza XP per raggiungere il livello 5 (1000 XP)"
    "preferiti_5"    -> "Salva 5 pillole nei preferiti usando il segnalibro"
    else             -> "Continua a usare l'app per scoprire come sbloccare questo badge!"
}
