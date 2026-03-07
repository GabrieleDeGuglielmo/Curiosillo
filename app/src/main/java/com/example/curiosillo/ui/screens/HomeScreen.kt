package com.example.curiosillo.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.EmojiObjects
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.curiosillo.CuriosityApplication
import com.example.curiosillo.R
import com.example.curiosillo.firebase.FirebaseManager
import com.example.curiosillo.network.VersioneChangelog
import com.example.curiosillo.ui.components.GamificationBanner
import com.example.curiosillo.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(nav: NavController) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as CuriosityApplication

    val homeVm: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(app.repository, app.contentPrefs, ctx)
    )
    val homeState by homeVm.state.collectAsState()

    val xpTotali       by app.gamificationPrefs.xpTotali.collectAsState(initial = 0)
    val streakCorrente by app.gamificationPrefs.streakCorrente.collectAsState(initial = 0)

    var showChangelogCompleto by remember { mutableStateOf(false) }

    // ── Popup changelog automatico ────────────────────────────────────────────
    homeState.changelogDaMostrare?.let { versioni ->
        ChangelogDialog(
            versioni  = versioni,
            titolo    = "🎉 Cosa c'è di nuovo",
            onDismiss = { homeVm.dismissChangelog() }
        )
    }

    // ── Dialog changelog completo ─────────────────────────────────────────────
    if (showChangelogCompleto && homeState.changelogCompleto.isNotEmpty()) {
        ChangelogDialog(
            versioni  = homeState.changelogCompleto,
            titolo    = "📋 Storico aggiornamenti",
            onDismiss = { showChangelogCompleto = false }
        )
    }

    // ── Dialog aggiornamento app ──────────────────────────────────────────────
    val isLoggato = FirebaseManager.utenteCorrente != null
    if (isLoggato) {
        homeState.aggiornamentoApp?.let { info ->
            AlertDialog(
                onDismissRequest = { homeVm.dismissAggiornamento() },
                icon  = { Icon(Icons.Default.SystemUpdate, null,
                    tint = MaterialTheme.colorScheme.primary) },
                title = {
                    Text("Aggiornamento disponibile", fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                },
                text = {
                    Text(
                        "È disponibile la versione ${info.versione} di Curiosillo.\nVuoi scaricarla adesso?",
                        textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        val url = if (info.downloadUrl.isNotEmpty()) info.downloadUrl else info.releaseUrl
                        ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        homeVm.dismissAggiornamento()
                    }) { Text("Scarica", fontWeight = FontWeight.Bold) }
                },
                dismissButton = {
                    TextButton(onClick = { homeVm.dismissAggiornamento() }) { Text("Dopo") }
                }
            )
        }
    }

    val gradientBg = Brush.verticalGradient(listOf(
        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
        MaterialTheme.colorScheme.background
    ))

    Box(modifier = Modifier.fillMaxSize().background(gradientBg)) {

        // ── Banner offline ────────────────────────────────────────────────────
        AnimatedVisibility(
            visible = homeState.isOffline,
            // Animazione dall'alto verso il basso più fluida
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier
                .statusBarsPadding()          // Padding per evitare la notch/status bar
                .padding(top = 8.dp)          // Piccolo distacco dal bordo superiore
                .align(Alignment.TopCenter)   // Centrato orizzontalmente
        ) {
            Surface(
                // Rimosso fillMaxWidth() per farlo diventare una pillola
                shape = CircleShape,          // Forma a pillola (molto più moderna)
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.95f), // Leggera trasparenza
                tonalElevation = 4.dp,        // Un po' di profondità
                shadowElevation = 6.dp        // Ombra per staccarlo dallo sfondo
            ) {
                Row(
                    Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp), // Padding interno bilanciato
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.WifiOff,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp), // Icona leggermente più piccola
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Nessuna connessione",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }

        // ── Contenuto principale ──────────────────────────────────────────────
        val screenHeight = LocalConfiguration.current.screenHeightDp
        val isSmallScreen = screenHeight < 700

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp)
                .padding(top = 16.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // ── Riga profilo + novità in cima ─────────────────────────────────
            Row(
                Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                // Spazio vuoto a sinistra per bilanciare
                Spacer(Modifier.size(42.dp))

                // Pulsante Novità — visibile solo se changelog disponibile
                if (homeState.changelogCompleto.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                RoundedCornerShape(20.dp)
                            )
                            .clickable { showChangelogCompleto = true }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AutoAwesome, "Novità",
                            modifier = Modifier.size(16.dp),
                            tint     = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(5.dp))
                        Text("Novità",
                            style      = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color      = MaterialTheme.colorScheme.primary)
                    }
                } else {
                    Spacer(Modifier.weight(1f))
                }

                // Pulsante Profilo
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape)
                        .clickable { nav.navigate("profile") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Person, "Profilo",
                        modifier = Modifier.size(26.dp),
                        tint     = MaterialTheme.colorScheme.primary)
                }
            }
            Image(
                painter            = painterResource(id = R.drawable.ic_logo),
                contentDescription = "Logo Curiosillo",
                modifier           = Modifier
                    .size(if (isSmallScreen) 72.dp else 110.dp)
                    .clip(CircleShape)
                    .shadow(8.dp, CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.height(if (isSmallScreen) 8.dp else 16.dp))
            Text("Curiosillo",
                style      = if (isSmallScreen) MaterialTheme.typography.headlineLarge
                else MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.ExtraBold,
                color      = MaterialTheme.colorScheme.primary)

            AnimatedVisibility(
                visible = homeState.syncMessaggio != null,
                enter   = fadeIn() + expandVertically(),
                exit    = fadeOut() + shrinkVertically()
            ) {
                homeState.syncMessaggio?.let { msg ->
                    LaunchedEffect(msg) {
                        kotlinx.coroutines.delay(7000)
                        homeVm.dismissSyncMessaggio()
                    }
                    Card(
                        modifier = Modifier.padding(top = 6.dp),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(msg, Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Text("Impara qualcosa di nuovo ogni giorno",
                style     = MaterialTheme.typography.bodyLarge,
                color     = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                textAlign = TextAlign.Center,
                modifier  = Modifier.padding(top = 6.dp, bottom = if (isSmallScreen) 12.dp else 20.dp))

            GamificationBanner(
                xpTotali       = xpTotali,
                streakCorrente = streakCorrente,
                modifier       = Modifier.padding(bottom = if (isSmallScreen) 12.dp else 20.dp)
            )

            MenuCard(Icons.Default.EmojiObjects,
                "Scopri una Curiosità", "Leggi e impara qualcosa di sorprendente",
                MaterialTheme.colorScheme.primary) { nav.navigate("category_picker/curiosity") }
            Spacer(Modifier.height(12.dp))
            MenuCard(Icons.Default.Quiz,
                "Fai un Quiz", "Metti alla prova le tue conoscenze",
                MaterialTheme.colorScheme.secondary) { nav.navigate("category_picker/quiz") }
            Spacer(Modifier.height(12.dp))
            MenuCard(Icons.Default.Refresh,
                "Ripasso", "Rileggi le pillole già imparate",
                MaterialTheme.colorScheme.tertiary) { nav.navigate("ripasso") }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Dialog changelog ──────────────────────────────────────────────────────────
@Composable
private fun ChangelogDialog(
    versioni:  List<VersioneChangelog>,
    titolo:    String,
    onDismiss: () -> Unit
) {
    val colorNovita = Color(0xFF4CAF50)   // verde
    val colorFix    = Color(0xFFFF9800)   // arancione

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(titolo, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        },
        text = {
            val scrollState = rememberScrollState()
            val showScrollHint by remember {
                derivedStateOf { scrollState.maxValue > 0 && scrollState.value < scrollState.maxValue }
            }

            Box(Modifier.fillMaxWidth()) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(scrollState)
                        .padding(end = 10.dp) // spazio per la barra
                ) {
                    versioni.forEachIndexed { index, versione ->
                        if (index > 0) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 16.dp),
                                color    = MaterialTheme.colorScheme.outlineVariant
                            )
                        }

                        // ── Intestazione versione ─────────────────────────────────
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = MaterialTheme.colorScheme.primary
                            ) {
                                Text(
                                    "v${versione.versione}",
                                    modifier   = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style      = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.ExtraBold,
                                    color      = Color.White
                                )
                            }
                            if (versione.data.isNotBlank()) {
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    versione.data,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                                )
                            }
                        }

                        // ── Nuove funzionalità ────────────────────────────────────
                        if (versione.novita.isNotEmpty()) {
                            SezioneChangelog(
                                etichetta = "Novità",
                                icona     = Icons.Default.Star,
                                colore    = colorNovita,
                                voci      = versione.novita
                            )
                        }

                        // ── Bug fix ───────────────────────────────────────────────
                        if (versione.fix.isNotEmpty()) {
                            if (versione.novita.isNotEmpty()) Spacer(Modifier.height(12.dp))
                            SezioneChangelog(
                                etichetta = "Correzioni",
                                icona     = Icons.Default.BugReport,
                                colore    = colorFix,
                                voci      = versione.fix
                            )
                        }
                    }
                }

                // ── Barra scroll laterale ─────────────────────────────────────
                if (scrollState.maxValue > 0) {
                    val fraction = scrollState.value.toFloat() / scrollState.maxValue.toFloat()
                    Box(
                        Modifier
                            .align(Alignment.CenterEnd)
                            .width(4.dp)
                            .heightIn(max = 420.dp)
                            .fillMaxHeight()
                            .background(
                                MaterialTheme.colorScheme.outlineVariant,
                                RoundedCornerShape(2.dp)
                            )
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(0.25f)
                                .align(Alignment.TopStart)
                                .offset(y = androidx.compose.ui.unit.Dp(fraction * 315))
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(2.dp)
                                )
                        )
                    }
                }

                // ── Freccia "scorri" che scompare quando sei in fondo ─────────
                androidx.compose.animation.AnimatedVisibility(
                    visible  = showScrollHint,
                    enter    = fadeIn(),
                    exit     = fadeOut(),
                    modifier = Modifier.align(Alignment.BottomCenter)
                ) {
                    Text("▾",
                        color    = MaterialTheme.colorScheme.primary,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(bottom = 2.dp))
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Ottimo!", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun SezioneChangelog(
    etichetta: String,
    icona:     ImageVector,
    colore:    Color,
    voci:      List<String>
) {
    // Intestazione sezione (es. "✨ Novità" o "🐛 Correzioni")
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 6.dp)
    ) {
        Box(
            Modifier
                .size(24.dp)
                .background(colore.copy(alpha = 0.15f), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icona, null, tint = colore, modifier = Modifier.size(14.dp))
        }
        Spacer(Modifier.width(7.dp))
        Text(
            etichetta,
            style      = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color      = colore
        )
    }

    // Voci
    voci.forEach { voce ->
        Row(
            modifier          = Modifier.padding(start = 4.dp, bottom = 5.dp),
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
                style     = MaterialTheme.typography.bodyMedium,
                color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                modifier  = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun MenuCard(
    icon: ImageVector, title: String, subtitle: String,
    color: Color, onClick: () -> Unit
) {
    Card(
        onClick   = onClick,
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(22.dp),
        colors    = CardDefaults.cardColors(containerColor = color),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, Modifier.size(46.dp), Color.White.copy(alpha = 0.9f))
            Spacer(Modifier.width(18.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold, color = Color.White)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f), maxLines = 2)
            }
        }
    }
}