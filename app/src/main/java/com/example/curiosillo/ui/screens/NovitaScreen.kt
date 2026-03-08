package com.example.curiosillo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.curiosillo.CuriosityApplication
import com.example.curiosillo.network.VersioneChangelog
import com.example.curiosillo.ui.components.CuriosilloBottomBar
import com.example.curiosillo.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovitaScreen(nav: NavController) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as CuriosityApplication

    val homeVm: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(app.repository, app.contentPrefs, ctx)
    )
    val homeState by homeVm.state.collectAsState()

    val gradientBg = Brush.verticalGradient(
        listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
            MaterialTheme.colorScheme.background
        )
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("✨", fontSize = 20.sp)
                        Spacer(Modifier.width(8.dp))
                        Text("Novità", fontWeight = FontWeight.Bold)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        bottomBar      = { CuriosilloBottomBar(nav) },
        containerColor = Color.Transparent
    ) { pad ->
        when {
            homeState.changelogCompleto.isEmpty() -> {
                Box(
                    Modifier.fillMaxSize().background(gradientBg).padding(pad),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔍", fontSize = 48.sp)
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Nessuna novità disponibile.",
                            style     = MaterialTheme.typography.bodyLarge,
                            color     = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            else -> {
                val scrollState = rememberScrollState()
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(gradientBg)
                        .padding(pad)
                ) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(horizontal = 20.dp, vertical = 8.dp)
                    ) {
                        homeState.changelogCompleto.forEachIndexed { index, versione ->
                            NovitaVersioneCard(versione = versione, isLatest = index == 0)
                            Spacer(Modifier.height(16.dp))
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    // ── Barra scroll laterale non invasiva ────────────────────
                    if (scrollState.maxValue > 0) {
                        val fraction = (scrollState.value.toFloat() / scrollState.maxValue.toFloat()).coerceIn(0f, 1f)
                        // Calcola l'altezza visibile approssimativa del thumb
                        val thumbFraction = (scrollState.viewportSize.toFloat() /
                                (scrollState.viewportSize + scrollState.maxValue).toFloat()).coerceIn(0.08f, 0.5f)
                        Box(
                            Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxHeight()
                                .width(4.dp)
                                .padding(vertical = 12.dp)
                                .background(
                                    MaterialTheme.colorScheme.onBackground.copy(alpha = 0.06f),
                                    androidx.compose.foundation.shape.RoundedCornerShape(2.dp)
                                )
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(thumbFraction)
                                    .align(Alignment.TopStart)
                                    .offset(y = androidx.compose.ui.unit.Dp(fraction * (1f - thumbFraction) * 600f))
                                    .background(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                        androidx.compose.foundation.shape.RoundedCornerShape(2.dp)
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Card singola versione ─────────────────────────────────────────────────────

@Composable
private fun NovitaVersioneCard(versione: VersioneChangelog, isLatest: Boolean) {
    val colorNovita = Color(0xFF4CAF50)
    val colorFix    = Color(0xFFFF9800)

    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(20.dp),
        colors    = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(if (isLatest) 6.dp else 2.dp)
    ) {
        Column(Modifier.padding(20.dp)) {

            // ── Header versione ───────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier          = Modifier.fillMaxWidth()
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isLatest) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        "v${versione.versione}",
                        modifier   = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color      = if (isLatest) Color.White
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (isLatest) {
                    Spacer(Modifier.width(10.dp))
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = colorNovita.copy(alpha = 0.15f)
                    ) {
                        Text(
                            "Attuale",
                            modifier   = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style      = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color      = colorNovita
                        )
                    }
                }

                if (versione.data.isNotBlank()) {
                    Spacer(Modifier.weight(1f))
                    Text(
                        versione.data,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }

            // ── Novità ────────────────────────────────────────────────────────
            if (versione.novita.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                SezioneVoci(
                    etichetta = "Novità",
                    icona     = Icons.Default.Star,
                    colore    = colorNovita,
                    voci      = versione.novita
                )
            }

            // ── Fix ───────────────────────────────────────────────────────────
            if (versione.fix.isNotEmpty()) {
                Spacer(Modifier.height(if (versione.novita.isNotEmpty()) 12.dp else 16.dp))
                SezioneVoci(
                    etichetta = "Correzioni",
                    icona     = Icons.Default.BugReport,
                    colore    = colorFix,
                    voci      = versione.fix
                )
            }
        }
    }
}

// ── Sezione voci (novità o fix) ───────────────────────────────────────────────

@Composable
private fun SezioneVoci(
    etichetta: String,
    icona:     ImageVector,
    colore:    Color,
    voci:      List<String>
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier          = Modifier.padding(bottom = 8.dp)
    ) {
        Box(
            Modifier
                .size(26.dp)
                .background(colore.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icona, null, tint = colore, modifier = Modifier.size(15.dp))
        }
        Spacer(Modifier.width(8.dp))
        Text(
            etichetta,
            style      = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color      = colore
        )
    }

    voci.forEach { voce ->
        Row(
            modifier          = Modifier.padding(start = 4.dp, bottom = 6.dp),
            verticalAlignment = Alignment.Top
        ) {
            Text(
                "·  ",
                color      = colore,
                fontWeight = FontWeight.ExtraBold,
                fontSize   = 18.sp,
                lineHeight = 22.sp
            )
            Text(
                voce,
                style    = MaterialTheme.typography.bodyMedium,
                color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                modifier = Modifier.weight(1f)
            )
        }
    }
}