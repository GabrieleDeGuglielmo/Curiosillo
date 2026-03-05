package com.example.curiosillo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.example.curiosillo.data.BadgeCatalogo
import com.example.curiosillo.data.BadgeDefinizione
import com.example.curiosillo.data.BadgeSbloccato
import com.example.curiosillo.ui.components.GamificationBanner
import com.example.curiosillo.ui.theme.Error
import com.example.curiosillo.ui.theme.Success
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
    var showResetDialog      by remember { mutableStateOf(false) }
    var badgeDettaglio       by remember { mutableStateOf<BadgeDefinizione?>(null) }
    var badgeSbloccatoAppena by remember { mutableStateOf<BadgeSbloccato?>(null) }

    // Dialog dettaglio badge
    badgeDettaglio?.let { def ->
        val sbloccato = def.id in state.badgeSbloccati.map { it.id }
        AlertDialog(
            onDismissRequest = { badgeDettaglio = null },
            icon  = { Text(def.icona, fontSize = 40.sp) },
            title = {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text(def.nome, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }
            },
            text  = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (sbloccato) {
                        Text(def.descrizione, textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Spacer(Modifier.height(8.dp))
                        Text("✅ Badge sbloccato!", fontWeight = FontWeight.SemiBold, color = Success)
                    } else {
                        Text("Per sbloccare questo badge:", fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(bottom = 6.dp))
                        Text(condizioneBadge(def.id), textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { badgeDettaglio = null }) { Text("Chiudi") }
            }
        )
    }

    // Dialog badge appena sbloccato
    badgeSbloccatoAppena?.let { badge ->
        AlertDialog(
            onDismissRequest = { badgeSbloccatoAppena = null },
            icon  = { Text(badge.icona, fontSize = 48.sp) },
            title = {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Badge sbloccato!", fontWeight = FontWeight.Bold)
                }
            },
            text  = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(badge.nome, style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text(badge.descrizione, textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            },
            confirmButton = {
                Button(onClick = { badgeSbloccatoAppena = null }) {
                    Text("Ottimo!", fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    // Dialog reset
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            icon  = { Icon(Icons.Default.Delete, null, tint = Error) },
            title = { Text("Resetta i progressi?", fontWeight = FontWeight.Bold) },
            text  = {
                Text("Tutte le curiosità torneranno a \"non lette\", il quiz verrà bloccato " +
                    "e XP, streak e badge verranno azzerati. Questa azione non può essere annullata.",
                    textAlign = TextAlign.Center)
            },
            confirmButton = {
                Button(onClick = { vm.resetProgressi(); showResetDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Error)
                ) { Text("Sì, resetta", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                OutlinedButton(onClick = { showResetDialog = false }) { Text("Annulla") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Il mio profilo", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Indietro")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { pad ->
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().padding(pad), Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                Modifier.fillMaxSize().padding(pad).verticalScroll(rememberScrollState()).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(12.dp))

                // Avatar
                Box(Modifier.size(90.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, null, Modifier.size(52.dp), tint = Color.White)
                }
                Spacer(Modifier.height(12.dp))
                Text("Il mio profilo", style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(20.dp))

                // Banner
                GamificationBanner(xpTotali = state.xpTotali, streakCorrente = state.streakCorrente)
                Spacer(Modifier.height(8.dp))

                // Streak massima + info
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("Streak massima: ${state.streakMassima} ${if (state.streakMassima == 1) "giorno" else "giorni"}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f))
                    Spacer(Modifier.width(6.dp))
                    var showStreakInfo by remember { mutableStateOf(false) }
                    IconButton(onClick = { showStreakInfo = true }, modifier = Modifier.size(18.dp)) {
                        Icon(Icons.Default.Info, "Info streak", Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f))
                    }
                    if (showStreakInfo) {
                        AlertDialog(
                            onDismissRequest = { showStreakInfo = false },
                            icon  = { Text("🔥", fontSize = 32.sp) },
                            title = {
                                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                    Text("Come funziona la streak?", fontWeight = FontWeight.Bold)
                                }
                            },
                            text  = {
                                Text("La streak conta i giorni consecutivi in cui hai letto almeno " +
                                    "una pillola e premuto \"Ho imparato!\"\n\nSe salti un giorno, " +
                                    "la streak riparte da 1.\n\nPiù alta è la streak, più XP bonus " +
                                    "guadagni ogni giorno!",
                                    textAlign = TextAlign.Center,
                                    color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            },
                            confirmButton = {
                                TextButton(onClick = { showStreakInfo = false }) { Text("Capito!") }
                            }
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))

                // Statistiche
                Text("Le mie statistiche", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(Modifier.weight(1f), "${state.curiositàImparate}", "Curiosità\nimparate",
                        MaterialTheme.colorScheme.primary)
                    StatCard(Modifier.weight(1f), "${state.totalCuriosità}", "Curiosità\ntotali",
                        MaterialTheme.colorScheme.secondary)
                    StatCard(Modifier.weight(1f), "${state.quizDisponibili}", "Quiz non\nrisposti",
                        MaterialTheme.colorScheme.tertiary)
                }

                if (state.totalCuriosità > 0) {
                    Spacer(Modifier.height(20.dp))
                    val pct = state.curiositàImparate.toFloat() / state.totalCuriosità
                    Text("Progresso complessivo — ${(pct * 100).toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                        modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress   = { pct },
                        modifier   = Modifier.fillMaxWidth().height(10.dp),
                        color      = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }

                // Badge
                Spacer(Modifier.height(28.dp))
                Text("Badge (${state.badgeSbloccati.size}/${BadgeCatalogo.tutti.size})",
                    style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))

                val idSbloccati = state.badgeSbloccati.map { it.id }.toSet()
                BadgeCatalogo.tutti.chunked(3).forEach { riga ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        riga.forEach { def ->
                            BadgeCard(def, def.id in idSbloccati, Modifier.weight(1f)) {
                                badgeDettaglio = def
                            }
                        }
                        repeat(3 - riga.size) { Spacer(Modifier.weight(1f)) }
                    }
                    Spacer(Modifier.height(10.dp))
                }

                // Pulsanti
                Spacer(Modifier.height(28.dp))
                Button(onClick = { nav.navigate("preferiti") },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape    = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Bookmark, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("I miei preferiti (${state.totaleBookmark})",
                        style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(12.dp))
                Button(onClick = { nav.navigate("quiz_stats") },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                ) {
                    Icon(Icons.Default.BarChart, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Statistiche Quiz",
                        style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(16.dp))
                OutlinedButton(onClick = { showResetDialog = true },
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape    = RoundedCornerShape(14.dp),
                    colors   = ButtonDefaults.outlinedButtonColors(contentColor = Error),
                    border   = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(Error))
                ) {
                    Icon(Icons.Default.Delete, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Resetta progressi", style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun BadgeCard(def: BadgeDefinizione, sbloccato: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val cardBg = if (sbloccato) Color(0xFF1A1A2E) else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (sbloccato) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    Card(onClick = onClick, modifier = modifier.height(110.dp),
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(if (sbloccato) 4.dp else 0.dp)
    ) {
        Column(Modifier.fillMaxSize().padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(if (sbloccato) def.icona else "🔒", fontSize = 28.sp)
            Spacer(Modifier.height(4.dp))
            Text(def.nome, style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                color = textColor, maxLines = 2,
                overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun StatCard(modifier: Modifier, valore: String, etichetta: String, colore: Color) {
    Card(modifier = modifier, shape = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = colore),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(Modifier.padding(vertical = 20.dp, horizontal = 8.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text(valore, fontSize = 32.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Spacer(Modifier.height(4.dp))
            Text(etichetta, style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.9f), textAlign = TextAlign.Center)
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
