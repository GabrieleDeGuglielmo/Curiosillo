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
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(nav: NavController, onLogout: () -> Unit) {
    val ctx   = LocalContext.current
    val app   = ctx.applicationContext as CuriosityApplication
    val scope = rememberCoroutineScope()

    val vm: ProfileViewModel = viewModel(
        factory = ProfileViewModel.Factory(app.repository, app.gamificationPrefs)
    )
    val state by vm.state.collectAsState()

    var showResetDialog      by remember { mutableStateOf(false) }
    var showLogoutDialog     by remember { mutableStateOf(false) }
    var showEliminaDialog    by remember { mutableStateOf(false) }
    var badgeDettaglio       by remember { mutableStateOf<BadgeDefinizione?>(null) }
    var badgeSbloccatoAppena by remember { mutableStateOf<BadgeSbloccato?>(null) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSheet  by remember { mutableStateOf(false) }

    // ── BottomSheet azioni profilo ────────────────────────────────────────────
    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState       = sheetState
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text("Azioni profilo",
                    style      = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier   = Modifier.padding(bottom = 20.dp))

                // Preferiti
                AzioneItem(
                    icon  = Icons.Default.Bookmark,
                    tint  = MaterialTheme.colorScheme.primary,
                    label = "I miei preferiti (${state.totaleBookmark})",
                    sub   = "Curiosità salvate con il segnalibro"
                ) {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        showSheet = false
                        nav.navigate("preferiti")
                    }
                }

                HorizontalDivider(Modifier.padding(vertical = 8.dp))

                // Pillole nascoste
                AzioneItem(
                    icon  = Icons.Default.VisibilityOff,
                    tint  = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    label = "Pillole nascoste (${state.totaleIgnorate})",
                    sub   = "Curiosità contrassegnate come \"non mi interessa\""
                ) {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        showSheet = false
                        nav.navigate("pillole_nascoste")
                    }
                }

                if (state.isAdmin) {
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    AzioneItem(
                        icon  = Icons.Default.BarChart,
                        tint  = Color(0xFF7B2D8B),
                        label = "Admin — Voti curiosità",
                        sub   = "Visualizza like/dislike di tutte le curiosità"
                    ) {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            showSheet = false
                            nav.navigate("admin_voti")
                        }
                    }
                    HorizontalDivider(Modifier.padding(vertical = 8.dp))
                    AzioneItem(
                        icon  = Icons.Default.Edit,
                        tint  = Color(0xFF7B2D8B),
                        label = "Admin — Gestione curiosità",
                        sub   = "Aggiungi, modifica o importa curiosità"
                    ) {
                        scope.launch { sheetState.hide() }.invokeOnCompletion {
                            showSheet = false
                            nav.navigate("admin_curiosita")
                        }
                    }
                }

                HorizontalDivider(Modifier.padding(vertical = 8.dp))

                // Statistiche Quiz
                AzioneItem(
                    icon  = Icons.Default.BarChart,
                    tint  = MaterialTheme.colorScheme.secondary,
                    label = "Statistiche Quiz",
                    sub   = "Cronologia e risultati dei tuoi quiz"
                ) {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        showSheet = false
                        nav.navigate("quiz_stats")
                    }
                }

                HorizontalDivider(Modifier.padding(vertical = 8.dp))

                // Resetta progressi
                AzioneItem(
                    icon  = Icons.Default.RestartAlt,
                    tint  = Error,
                    label = "Resetta progressi",
                    sub   = "Azzera XP, streak, badge e pillole lette"
                ) {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        showSheet = false
                        showResetDialog = true
                    }
                }

                HorizontalDivider(Modifier.padding(vertical = 8.dp))

                // Elimina account
                AzioneItem(
                    icon  = Icons.Default.DeleteForever,
                    tint  = Error,
                    label = "Elimina account",
                    sub   = "Cancella account e tutti i dati in modo permanente"
                ) {
                    scope.launch { sheetState.hide() }.invokeOnCompletion {
                        showSheet = false
                        showEliminaDialog = true
                    }
                }

                Spacer(Modifier.height(8.dp))
            }
        }
    }

    // ── Dialog dettaglio badge ────────────────────────────────────────────────
    badgeDettaglio?.let { def ->
        val sbloccato = def.id in state.badgeSbloccati.map { it.id }
        AlertDialog(
            onDismissRequest = { badgeDettaglio = null },
            icon  = { Text(def.icona, fontSize = 40.sp) },
            title = {
                Text(def.nome, fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (sbloccato) {
                        Text(def.descrizione, textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        Spacer(Modifier.height(8.dp))
                        Text("✅ Badge sbloccato!", fontWeight = FontWeight.SemiBold, color = Success)
                    } else {
                        Text("Per sbloccare questo badge:", fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                            textAlign = TextAlign.Center)
                        Text(condizioneBadge(def.id), textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { badgeDettaglio = null }) { Text("Chiudi") }
            }
        )
    }

    // ── Dialog badge appena sbloccato ─────────────────────────────────────────
    badgeSbloccatoAppena?.let { badge ->
        AlertDialog(
            onDismissRequest = { badgeSbloccatoAppena = null },
            icon  = { Text(badge.icona, fontSize = 48.sp) },
            title = {
                Text("Badge sbloccato!", fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(badge.nome, style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(6.dp))
                    Text(badge.descrizione, textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
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

    // ── Dialog reset progressi ────────────────────────────────────────────────
    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            icon  = { Icon(Icons.Default.Delete, null, tint = Error) },
            title = {
                Text("Resetta i progressi?", fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            },
            text = {
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

    // ── Dialog logout ─────────────────────────────────────────────────────────
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            icon  = { Icon(Icons.Default.Logout, null, tint = Error) },
            title = {
                Text("Esci dall'account?", fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            },
            text = {
                Text("I tuoi progressi sono salvati su cloud e saranno disponibili al prossimo accesso.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            },
            confirmButton = {
                Button(onClick = { showLogoutDialog = false; vm.logout(onLogout) },
                    colors = ButtonDefaults.buttonColors(containerColor = Error)
                ) { Text("Esci", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                OutlinedButton(onClick = { showLogoutDialog = false }) { Text("Annulla") }
            }
        )
    }

    // ── Dialog elimina account ────────────────────────────────────────────────
    if (showEliminaDialog) {
        AlertDialog(
            onDismissRequest = { if (!state.isEliminazioneInCorso) showEliminaDialog = false },
            icon  = { Icon(Icons.Default.DeleteForever, null, tint = Error) },
            title = {
                Text("Elimina account?", fontWeight = FontWeight.Bold,
                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            },
            text = {
                Text("Verranno eliminati permanentemente il tuo account e tutti i dati associati: " +
                        "XP, streak, badge, pillole lette e preferiti.\n\nQuesta azione è irreversibile.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
            },
            confirmButton = {
                Button(
                    onClick = { vm.eliminaAccount(onLogout) },
                    enabled = !state.isEliminazioneInCorso,
                    colors  = ButtonDefaults.buttonColors(containerColor = Error)
                ) {
                    if (state.isEliminazioneInCorso) {
                        CircularProgressIndicator(Modifier.size(18.dp),
                            color = Color.White, strokeWidth = 2.dp)
                    } else {
                        Text("Sì, elimina tutto", fontWeight = FontWeight.Bold)
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick  = { showEliminaDialog = false },
                    enabled  = !state.isEliminazioneInCorso
                ) { Text("Annulla") }
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
                actions = {
                    IconButton(onClick = { showSheet = true }) {
                        Icon(Icons.Default.MoreVert, "Azioni profilo",
                            tint = MaterialTheme.colorScheme.onBackground)
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
        if (state.isLoading) {
            Box(Modifier.fillMaxSize().background(gradientBg).padding(pad), Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                Modifier.fillMaxSize().background(gradientBg).padding(pad)
                    .verticalScroll(rememberScrollState()).padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(Modifier.height(12.dp))

                // ── Sezione account ───────────────────────────────────────────
                if (state.isLoggato) {
                    Card(
                        modifier  = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                        shape     = RoundedCornerShape(18.dp),
                        colors    = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer),
                        elevation = CardDefaults.cardElevation(0.dp)
                    ) {
                        Row(
                            Modifier.padding(horizontal = 16.dp, vertical = 12.dp).fillMaxWidth(),
                            verticalAlignment     = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)) {
                                Box(
                                    Modifier.size(44.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        state.username.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize   = 20.sp,
                                        color      = Color.White
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(state.username.ifBlank { "Utente" },
                                        style      = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color      = MaterialTheme.colorScheme.onPrimaryContainer)
                                    if (state.email.isNotBlank()) {
                                        Text(state.email,
                                            style    = MaterialTheme.typography.bodySmall,
                                            color    = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f),
                                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                }
                            }
                            TextButton(
                                onClick = { showLogoutDialog = true },
                                colors  = ButtonDefaults.textButtonColors(contentColor = Error)
                            ) {
                                Icon(Icons.Default.Logout, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Esci", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }

                // ── Avatar + titolo ───────────────────────────────────────────
                Box(Modifier.size(90.dp).background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, null, Modifier.size(52.dp), tint = Color.White)
                }
                Spacer(Modifier.height(12.dp))
                Text("Il mio profilo", style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(20.dp))

                // Banner XP
                GamificationBanner(xpTotali = state.xpTotali, streakCorrente = state.streakCorrente)
                Spacer(Modifier.height(8.dp))

                // Streak massima + info
                Row(verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()) {
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
                                Text("Come funziona la streak?", fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                            },
                            text = {
                                Text("La streak conta i giorni consecutivi in cui hai letto almeno " +
                                        "una pillola e premuto \"Ho imparato!\"\n\nSe salti un giorno, " +
                                        "la streak riparte da 1.\n\nPiù alta è la streak, più XP bonus " +
                                        "guadagni ogni giorno!",
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            },
                            confirmButton = {
                                TextButton(onClick = { showStreakInfo = false }) { Text("Capito!") }
                            }
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))

                // ── Statistiche ───────────────────────────────────────────────
                Text("Le mie statistiche", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard(Modifier.weight(1f), "${state.curiositàImparate}",
                        "Curiosità\nimparate", MaterialTheme.colorScheme.primary)
                    StatCard(Modifier.weight(1f), "${state.totalCuriosità}",
                        "Curiosità\ntotali", MaterialTheme.colorScheme.secondary)
                    StatCard(Modifier.weight(1f), "${state.quizDisponibili}",
                        "Quiz non\nrisposti", MaterialTheme.colorScheme.tertiary)
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

                // ── Badge ─────────────────────────────────────────────────────
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

                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

// ── Componente riga azione nel BottomSheet ────────────────────────────────────
@Composable
private fun AzioneItem(
    icon:    androidx.compose.ui.graphics.vector.ImageVector,
    tint:    Color,
    label:   String,
    sub:     String,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(42.dp)
                .background(tint.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(label, fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyLarge, color = tint)
            Text(sub, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f))
        }
        TextButton(onClick = onClick) {
            Text("Vai", fontWeight = FontWeight.Bold, color = tint)
        }
    }
}

@Composable
private fun BadgeCard(def: BadgeDefinizione, sbloccato: Boolean,
                      modifier: Modifier, onClick: () -> Unit) {
    val cardBg    = if (sbloccato) Color(0xFF1A1A2E) else MaterialTheme.colorScheme.surfaceVariant
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
                color = textColor, maxLines = 2, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth())
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