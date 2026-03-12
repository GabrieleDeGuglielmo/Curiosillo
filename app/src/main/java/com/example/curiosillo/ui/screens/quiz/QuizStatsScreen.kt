package com.example.curiosillo.ui.screens.quiz

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.curiosillo.ui.screens.utils.emojiCategoria
import com.example.curiosillo.viewmodel.QuizStatsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizStatsScreen(nav: NavController) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as CuriosityApplication
    val vm: QuizStatsViewModel = viewModel(factory = QuizStatsViewModel.Factory(app.repository))
    val state by vm.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Statistiche Quiz") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Indietro") }
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
        when {
            state.isLoading ->
                Box(Modifier.fillMaxSize().background(gradientBg).padding(pad), Alignment.Center) { CircularProgressIndicator() }

            state.ultime20Sessioni.isEmpty() && state.statPerCategoria.isEmpty() ->
                Column(Modifier.fillMaxSize().background(gradientBg).padding(pad),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center) {
                    Text("🧠", fontSize = 56.sp)
                    Spacer(Modifier.height(16.dp))
                    Text("Nessuna statistica ancora.\nCompleta qualche quiz per vedere i tuoi progressi!",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                        textAlign = TextAlign.Center,
                        modifier  = Modifier.padding(horizontal = 32.dp))
                }

            else ->
                Column(Modifier.fillMaxSize().background(gradientBg).padding(pad).verticalScroll(rememberScrollState()).padding(24.dp)) {

                    // ── Andamento sessioni ─────────────────────────────────────
                    if (state.ultime20Sessioni.isNotEmpty()) {
                        Text("Andamento recente", style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                        Spacer(Modifier.height(12.dp))
                        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                            colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(3.dp)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                val sessioni = state.ultime20Sessioni.reversed().takeLast(10)
                                Row(Modifier.fillMaxWidth().height(120.dp),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalAlignment     = Alignment.Bottom) {
                                    sessioni.forEach { sessione ->
                                        val pct = if (sessione.totalAnswers > 0)
                                            sessione.correctAnswers.toFloat() / sessione.totalAnswers else 0f
                                        val colore = when {
                                            pct >= 0.8f -> MaterialTheme.colorScheme.primary
                                            pct >= 0.5f -> MaterialTheme.colorScheme.secondary
                                            else        -> MaterialTheme.colorScheme.tertiary
                                        }
                                        Column(Modifier.weight(1f),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Bottom) {
                                            Text("${(pct * 100).toInt()}%", fontSize = 9.sp,
                                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                                            Spacer(Modifier.height(2.dp))
                                            Box(Modifier.fillMaxWidth().height((pct * 90).dp)
                                                .background(colore, RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)))
                                        }
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                Text("Ultime ${sessioni.size} sessioni — primario ≥80%, secondario ≥50%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                    }

                    // ── Per categoria ──────────────────────────────────────────
                    if (state.statPerCategoria.isNotEmpty()) {
                        Text("Punti forti e deboli", style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                        Spacer(Modifier.height(12.dp))
                        state.statPerCategoria.forEach { stat ->
                            val pct = if (stat.totale > 0) stat.corrette.toFloat() / stat.totale else 0f
                            val colore = when {
                                pct >= 0.8f -> MaterialTheme.colorScheme.primary
                                pct >= 0.5f -> MaterialTheme.colorScheme.secondary
                                else        -> MaterialTheme.colorScheme.tertiary
                            }
                            Card(Modifier.fillMaxWidth().padding(bottom = 10.dp),
                                shape     = RoundedCornerShape(14.dp),
                                colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Column(Modifier.padding(16.dp)) {
                                    Row(Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment     = Alignment.CenterVertically) {
                                        Text(
                                            emojiCategoria(stat.category) + " " + stat.category,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface)
                                        Text("${stat.corrette}/${stat.totale} — ${(pct * 100).toInt()}%",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colore)
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    LinearProgressIndicator(
                                        progress   = { pct },
                                        modifier   = Modifier.fillMaxWidth().height(8.dp),
                                        color      = colore,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // ── Riepilogo ─────────────────────────────────────────────
                    Spacer(Modifier.height(8.dp))
                    Text("Riepilogo", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                    Spacer(Modifier.height(12.dp))

                    val totaleSessioni = state.ultime20Sessioni.size
                    val totaleRisposte = state.ultime20Sessioni.sumOf { it.totalAnswers }
                    val totaleCorrette = state.ultime20Sessioni.sumOf { it.correctAnswers }
                    val pctGlobale = if (totaleRisposte > 0)
                        (totaleCorrette.toFloat() / totaleRisposte * 100).toInt() else 0

                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        RiepilogoCard(Modifier.weight(1f), "$totaleSessioni", "Quiz\ncompletati",
                            MaterialTheme.colorScheme.primary)
                        RiepilogoCard(Modifier.weight(1f), "$totaleRisposte", "Risposte\ntotali",
                            MaterialTheme.colorScheme.secondary)
                        RiepilogoCard(Modifier.weight(1f), "$pctGlobale%", "Media\nglobale",
                            MaterialTheme.colorScheme.tertiary)
                    }
                    Spacer(Modifier.height(16.dp))
                }
        }
    }
}

@Composable
private fun RiepilogoCard(modifier: Modifier, valore: String, etichetta: String, colore: Color) {
    Card(modifier = modifier, shape = RoundedCornerShape(14.dp),
        colors    = CardDefaults.cardColors(containerColor = colore),
        elevation = CardDefaults.cardElevation(3.dp)
    ) {
        Column(Modifier.padding(vertical = 16.dp, horizontal = 8.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text(valore, fontSize = 26.sp, color = Color.White)
            Spacer(Modifier.height(4.dp))
            Text(etichetta, style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.9f), textAlign = TextAlign.Center)
        }
    }
}
