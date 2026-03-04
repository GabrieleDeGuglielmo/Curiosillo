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
import com.example.curiosillo.ui.theme.Error
import com.example.curiosillo.ui.theme.Primary
import com.example.curiosillo.ui.theme.Secondary
import com.example.curiosillo.ui.theme.Tertiary
import com.example.curiosillo.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(nav: NavController) {
    val ctx  = LocalContext.current
    val repo = (ctx.applicationContext as CuriosityApplication).repository
    val vm: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory(repo))
    val state by vm.state.collectAsState()
    var showResetDialog by remember { mutableStateOf(false) }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            icon    = { Icon(Icons.Default.Delete, null, tint = Error) },
            title   = { Text("Resetta i progressi?", fontWeight = FontWeight.Bold) },
            text    = {
                Text(
                    "Tutte le curiosità torneranno a \"non lette\" e il quiz verrà bloccato. " +
                            "Questa azione non può essere annullata.",
                    textAlign = TextAlign.Center
                )
            },
            confirmButton = {
                Button(
                    onClick = { vm.resetProgressi(); showResetDialog = false },
                    colors  = ButtonDefaults.buttonColors(containerColor = Error)
                ) {
                    Text("Sì, resetta", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showResetDialog = false }) {
                    Text("Annulla")
                }
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
                        .verticalScroll(rememberScrollState())  // ← aggiunto scroll
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
                        Icon(Icons.Default.Person, null,
                            modifier = Modifier.size(52.dp),
                            tint = Color.White)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Il mio profilo",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(32.dp))

                    // ── Statistiche ───────────────────────────────────────────
                    Text("Le mie statistiche",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF555555),
                        modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        StatCard(
                            modifier  = Modifier.weight(1f),
                            valore    = "${state.curiositàImparate}",
                            etichetta = "Curiosità\nimparate",
                            colore    = Primary
                        )
                        StatCard(
                            modifier  = Modifier.weight(1f),
                            valore    = "${state.totalCuriosità}",
                            etichetta = "Curiosità\ntotali",
                            colore    = Secondary
                        )
                        StatCard(
                            modifier  = Modifier.weight(1f),
                            valore    = "${state.quizDisponibili}",
                            etichetta = "Quiz\nnon risposti",
                            colore    = Tertiary
                        )
                    }

                    // ── Barra progresso ───────────────────────────────────────
                    if (state.totalCuriosità > 0) {
                        Spacer(Modifier.height(28.dp))
                        val percentuale = state.curiositàImparate.toFloat() / state.totalCuriosità
                        Text(
                            "Progresso complessivo — ${(percentuale * 100).toInt()}%",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF555555),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress  = { percentuale },
                            modifier  = Modifier.fillMaxWidth().height(10.dp),
                            color     = Primary,
                            trackColor = Color(0xFFE0E0E0)
                        )
                    }

                    // ── Curiosità salvate ─────────────────────────────────────
                    Spacer(Modifier.height(28.dp))
                    Text("Curiosità salvate (${state.totaleBookmark})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF555555),
                        modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(10.dp))

                    if (state.salvati.isEmpty()) {
                        Text(
                            "Nessuna curiosità salvata ancora.\nPremi il segnalibro durante la lettura!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        state.salvati.forEach { curiosità ->
                            Card(
                                modifier  = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                shape     = RoundedCornerShape(14.dp),
                                colors    = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(curiosità.title,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold)
                                        Text(curiosità.category,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Gray)
                                    }
                                    Icon(Icons.Default.Bookmark, null,
                                        tint = Primary,
                                        modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(28.dp))

                    // ── Pulsante reset ────────────────────────────────────────
                    OutlinedButton(
                        onClick  = { showResetDialog = true },
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape    = RoundedCornerShape(14.dp),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = Error),
                        border   = ButtonDefaults.outlinedButtonBorder.copy(
                            brush = androidx.compose.ui.graphics.SolidColor(Error)
                        )
                    ) {
                        Icon(Icons.Default.Delete, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Resetta progressi",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier,
    valore: String,
    etichetta: String,
    colore: Color
) {
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = colore),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(vertical = 20.dp, horizontal = 8.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(valore, fontSize = 32.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White)
            Spacer(Modifier.height(4.dp))
            Text(etichetta,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = TextAlign.Center)
        }
    }
}