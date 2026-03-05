package com.example.curiosillo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.curiosillo.CuriosityApplication
import com.example.curiosillo.ui.theme.Primary
import com.example.curiosillo.ui.theme.Secondary
import com.example.curiosillo.ui.theme.Tertiary
import com.example.curiosillo.viewmodel.QuizStatsViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizStatsScreen(nav: NavController) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as CuriosityApplication
    val vm: QuizStatsViewModel = viewModel(
        factory = QuizStatsViewModel.Factory(app.repository)
    )
    val state by vm.state.collectAsState()

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Statistiche Quiz", fontWeight = FontWeight.SemiBold) },
            navigationIcon = {
                IconButton(onClick = { nav.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, "Indietro")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )
    }) { pad ->
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFFE8F4FD), Color.White)))
                .padding(pad)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else if (state.ultime20Sessioni.isEmpty() && state.statPerCategoria.isEmpty()) {
                Column(
                    Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("🧠", fontSize = 56.sp)
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Nessuna statistica ancora.\nCompleta qualche quiz per vedere i tuoi progressi!",
                        style     = MaterialTheme.typography.bodyLarge,
                        color     = Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier  = Modifier.padding(horizontal = 32.dp)
                    )
                }
            } else {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(24.dp)
                ) {

                    // ── Andamento sessioni ─────────────────────────────────────
                    if (state.ultime20Sessioni.isNotEmpty()) {
                        Text("Andamento recente",
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color      = Color(0xFF555555))
                        Spacer(Modifier.height(12.dp))

                        Card(
                            Modifier.fillMaxWidth(),
                            shape     = RoundedCornerShape(16.dp),
                            colors    = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(3.dp)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                val sessioni = state.ultime20Sessioni.reversed().takeLast(10)
                                val maxPct   = 100f

                                Row(
                                    Modifier.fillMaxWidth().height(120.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment     = Alignment.Bottom
                                ) {
                                    sessioni.forEach { sessione ->
                                        val pct = if (sessione.totalAnswers > 0)
                                            sessione.correctAnswers.toFloat() / sessione.totalAnswers
                                        else 0f
                                        val colore = when {
                                            pct >= 0.8f -> Primary
                                            pct >= 0.5f -> Secondary
                                            else        -> Tertiary
                                        }
                                        Column(
                                            Modifier.weight(1f),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Bottom
                                        ) {
                                            Text(
                                                "${(pct * 100).toInt()}%",
                                                fontSize = 9.sp,
                                                color    = Color.Gray
                                            )
                                            Spacer(Modifier.height(2.dp))
                                            Box(
                                                Modifier
                                                    .fillMaxWidth()
                                                    .height((pct * 90).dp)
                                                    .background(colore, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Ultime ${sessioni.size} sessioni — verde ≥80%, giallo ≥50%",
                                    style    = MaterialTheme.typography.labelSmall,
                                    color    = Color.Gray,
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                    }

                    // ── Per categoria ──────────────────────────────────────────
                    if (state.statPerCategoria.isNotEmpty()) {
                        Text("Punti forti e deboli",
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color      = Color(0xFF555555))
                        Spacer(Modifier.height(12.dp))

                        state.statPerCategoria.forEach { stat ->
                            val pct = if (stat.totale > 0)
                                stat.corrette.toFloat() / stat.totale else 0f
                            val colore = when {
                                pct >= 0.8f -> Primary
                                pct >= 0.5f -> Secondary
                                else        -> Tertiary
                            }

                            Card(
                                Modifier.fillMaxWidth().padding(bottom = 10.dp),
                                shape     = RoundedCornerShape(14.dp),
                                colors    = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Row(
                                        Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment     = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            emojiCategoria(stat.category) + " " + stat.category,
                                            style      = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            "${stat.corrette}/${stat.totale} — ${(pct * 100).toInt()}%",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colore,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress   = { pct },
                                        modifier   = Modifier.fillMaxWidth().height(8.dp),
                                        color      = colore,
                                        trackColor = Color(0xFFE0E0E0)
                                    )
                                }
                            }
                        }
                    }

                    // ── Riepilogo numerico ─────────────────────────────────────
                    Spacer(Modifier.height(8.dp))
                    Text("Riepilogo",
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color      = Color(0xFF555555))
                    Spacer(Modifier.height(12.dp))

                    val totaleSessioni = state.ultime20Sessioni.size
                    val totaleRisposte = state.ultime20Sessioni.sumOf { it.totalAnswers }
                    val totaleCorrette = state.ultime20Sessioni.sumOf { it.correctAnswers }
                    val pctGlobale = if (totaleRisposte > 0)
                        (totaleCorrette.toFloat() / totaleRisposte * 100).toInt() else 0

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        RiepilogoCard(Modifier.weight(1f), "$totaleSessioni", "Quiz\ncompletati", Primary)
                        RiepilogoCard(Modifier.weight(1f), "$totaleRisposte", "Risposte\ntotali", Secondary)
                        RiepilogoCard(Modifier.weight(1f), "$pctGlobale%", "Media\nglobale", Tertiary)
                    }
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun RiepilogoCard(modifier: Modifier, valore: String, etichetta: String, colore: Color) {
    Card(
        modifier  = modifier,
        shape     = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = colore),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(
            Modifier.padding(vertical = 16.dp, horizontal = 8.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(valore, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Spacer(Modifier.height(4.dp))
            Text(etichetta, style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.9f), textAlign = TextAlign.Center)
        }
    }
}
