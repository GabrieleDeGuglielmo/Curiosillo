package com.example.curiosillo.ui.screens.user

import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import com.example.curiosillo.ui.theme.Success
import com.example.curiosillo.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BadgeScreen(nav: NavController) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as CuriosityApplication

    val vm: ProfileViewModel = viewModel(
        factory = ProfileViewModel.Factory(app.repository, app.gamificationPrefs, app.contentPrefs)
    )
    val state by vm.state.collectAsState()

    LaunchedEffect(Unit) {
        vm.caricaStatistiche()
    }

    var badgeDettaglio by remember { mutableStateOf<BadgeDefinizione?>(null) }

    val gradientBg = Brush.verticalGradient(
        listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
            MaterialTheme.colorScheme.background
        )
    )

    Box(Modifier.fillMaxSize().background(gradientBg)) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("I miei badge") },
                    navigationIcon = {
                        IconButton(onClick = { nav.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Indietro")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            }
        ) { pad ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(pad)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "In questa sezione puoi vedere tutti i traguardi che hai raggiunto e quelli ancora da sbloccare!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                Text(
                    "Sbloccati: ${state.badgeSbloccati.size} / ${BadgeCatalogo.tutti.size}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                val idSbloccati = state.badgeSbloccati.map { it.id }.toSet()
                BadgeCatalogo.tutti.chunked(3).forEach { riga ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        riga.forEach { def ->
                            BadgeCard(def, def.id in idSbloccati, Modifier.weight(1f)) {
                                badgeDettaglio = def
                            }
                        }
                        repeat(3 - riga.size) { Spacer(Modifier.weight(1f)) }
                    }
                    Spacer(Modifier.height(10.dp))
                }
            }
        }

    } // Box gradient

    badgeDettaglio?.let { def ->
        val sbloccato = def.id in state.badgeSbloccati.map { it.id }
        AlertDialog(
            onDismissRequest = { badgeDettaglio = null },
            icon = { Text(def.icona, fontSize = 40.sp) },
            title = {
                Text(
                    def.nome,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (sbloccato) {
                        Text(
                            def.descrizione, textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("✅ Badge sbloccato!", color = Success)
                    } else {
                        Text(
                            "Per sbloccare questo badge:",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp),
                            textAlign = TextAlign.Center
                        )
                        Text(
                            condizioneBadge(def.id), textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth(),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { badgeDettaglio = null }) { Text("Chiudi") }
            }
        )
    }
}

@Composable
private fun BadgeCard(
    def: BadgeDefinizione, sbloccato: Boolean,
    modifier: Modifier, onClick: () -> Unit
) {
    val isLegendaryUnlocked = def.isLeggendario && sbloccato

    val cardBg = when {
        isLegendaryUnlocked -> Color(0xFF4A148C)
        sbloccato -> Color(0xFF1A1A2E)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }

    val textColor =
        if (sbloccato) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)

    val border = if (isLegendaryUnlocked) {
        androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFFFD700))
    } else null

    val cardShape = RoundedCornerShape(14.dp)
    val cardHeight = if (isLegendaryUnlocked) 120.dp else 110.dp

    // ── Legendary 3D animations ──
    val infiniteTransition = if (isLegendaryUnlocked) {
        rememberInfiniteTransition(label = "legendary_${def.id}")
    } else null

    val rotationY by if (isLegendaryUnlocked && infiniteTransition != null) {
        infiniteTransition.animateFloat(
            initialValue = -8f,
            targetValue = 8f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 3000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "rotationY_${def.id}"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    val shimmerAlpha by if (isLegendaryUnlocked && infiniteTransition != null) {
        infiniteTransition.animateFloat(
            initialValue = 0.15f,
            targetValue = 0.5f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2000, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "shimmer_${def.id}"
        )
    } else {
        remember { mutableFloatStateOf(0f) }
    }

    val density = LocalDensity.current.density

    val cardModifier = if (isLegendaryUnlocked) {
        modifier
            .height(cardHeight)
            .shadow(
                elevation = 12.dp,
                shape = cardShape,
                ambientColor = Color(0xFF7B1FA2),
                spotColor = Color(0xFFE040FB)
            )
            .graphicsLayer {
                this.rotationY = rotationY
                this.cameraDistance = 12f * density
            }
    } else {
        modifier.height(cardHeight)
    }

    Card(
        onClick = onClick, modifier = cardModifier,
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = border,
        elevation = CardDefaults.cardElevation(
            if (isLegendaryUnlocked) 8.dp else if (sbloccato) 4.dp else 0.dp
        )
    ) {
        Box(Modifier.fillMaxSize()) {
            // ── Holographic gradient + shimmer for legendary unlocked ──
            if (isLegendaryUnlocked) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF4A148C),
                                    Color(0xFFAB47BC),
                                    Color(0xFF4A148C)
                                ),
                                start = Offset.Zero,
                                end = Offset.Infinite
                            )
                        )
                        .drawBehind {
                            drawRect(Color(0xFFFFD700).copy(alpha = shimmerAlpha))
                        }
                )
            }

            // ── Badge content ──
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    if (sbloccato) def.icona else "🔒",
                    fontSize = if (isLegendaryUnlocked) 32.sp else 28.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    def.nome, style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                    color = textColor, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
                if (isLegendaryUnlocked) {
                    Text(
                        "Leggendario", fontSize = 8.sp, color = Color(0xFFFFD700),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

private fun condizioneBadge(id: String): String = when (id) {
    "prima_pillola" -> "Leggi la tua prima curiosità e premi \"Ho imparato!\""
    "prima_risposta" -> "Completa il tuo primo quiz"
    "streak_3" -> "Leggi almeno una pillola al giorno per 3 giorni consecutivi"
    "streak_7" -> "Leggi almeno una pillola al giorno per 7 giorni consecutivi"
    "streak_30" -> "Leggi almeno una pillola al giorno per 30 giorni consecutivi"
    "perfetto_5" -> "Rispondi correttamente a 5 domande di fila senza sbagliare"
    "pillole_10" -> "Leggi e impara 10 pillole"
    "pillole_20" -> "Leggi e impara 20 pillole"
    "pillole_50" -> "Leggi e impara 50 pillole"
    "livello_3" -> "Accumula abbastanza XP per raggiungere il livello 3 (250 XP)"
    "livello_5" -> "Accumula abbastanza XP per raggiungere il livello 5 (1000 XP)"
    "preferiti_5" -> "Salva 5 pillole nei preferiti usando il segnalibro"
    "scoperta_1" -> "Fai la tua prima scoperta AR!"
    "scoperte_10" -> "Fai 10 scoperte AR!"
    "scoperte_50" -> "Fai 50 scoperte AR!"
    else -> "Continua a usare l'app per scoprire come sbloccare questo badge!"
}