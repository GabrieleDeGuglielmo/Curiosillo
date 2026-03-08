package com.example.curiosillo.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.EmojiObjects
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SportsMartialArts
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.curiosillo.CuriosityApplication
import com.example.curiosillo.R
import com.example.curiosillo.firebase.FirebaseManager
import com.example.curiosillo.network.VersioneChangelog
import com.example.curiosillo.ui.components.CuriosilloBottomBar
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

    homeState.changelogDaMostrare?.let { versioni ->
        ChangelogDialog(versioni = versioni, titolo = "🎉 Cosa c'è di nuovo",
            onDismiss = { homeVm.dismissChangelog() })
    }
    if (homeState.notifiche.isNotEmpty()) {
        NotificheDialog(notifiche = homeState.notifiche, onDismiss = { homeVm.dismissNotifiche() })
    }

    val isLoggato = FirebaseManager.utenteCorrente != null
    val versioneAttuale = try {
        ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName
    } catch (e: Exception) { "N/D" }

    if (isLoggato) {
        homeState.aggiornamentoApp?.let { info ->
            AlertDialog(
                onDismissRequest = { homeVm.dismissAggiornamento() },
                icon  = { Icon(Icons.Default.SystemUpdate, null, tint = MaterialTheme.colorScheme.primary) },
                title = { Text("Aggiornamento disponibile", fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
                text  = {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("La tua versione: $versioneAttuale",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Spacer(Modifier.height(12.dp))
                        Text("È disponibile la versione ${info.versione} di Curiosillo.\nVuoi scaricarla adesso?",
                            textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                        Spacer(Modifier.height(16.dp))
                        Text("Controlla tutte le novità se la tua versione è abbastanza datata!",
                            style = MaterialTheme.typography.labelSmall, textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f), fontWeight = FontWeight.Medium)
                    }
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

    Scaffold(
        bottomBar = { CuriosilloBottomBar(nav) },
        containerColor = Color.Transparent
    ) { scaffoldPad ->
        Box(modifier = Modifier.fillMaxSize().background(gradientBg).padding(scaffoldPad)) {

            AnimatedVisibility(
                visible  = homeState.isOffline,
                enter    = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit     = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier.statusBarsPadding().padding(top = 8.dp).align(Alignment.TopCenter)
            ) {
                Surface(shape = CircleShape,
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.95f),
                    tonalElevation = 4.dp, shadowElevation = 6.dp
                ) {
                    Row(Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.WifiOff, null, Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(Modifier.width(8.dp))
                        Text("Nessuna connessione", style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                    }
                }
            }

            val screenHeight  = LocalConfiguration.current.screenHeightDp
            val isSmallScreen = screenHeight < 700

            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                    .padding(horizontal = 28.dp).padding(top = 50.dp, bottom = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Top
            ) {
                Image(painter = painterResource(id = R.drawable.ic_logo),
                    contentDescription = "Logo Curiosillo",
                    modifier = Modifier.size(if (isSmallScreen) 72.dp else 110.dp))

                Spacer(Modifier.height(if (isSmallScreen) 8.dp else 16.dp))
                Text("Curiosillo",
                    style = if (isSmallScreen) MaterialTheme.typography.headlineLarge
                    else MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary)

                AnimatedVisibility(visible = homeState.syncMessaggio != null,
                    enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
                    homeState.syncMessaggio?.let { msg ->
                        LaunchedEffect(msg) {
                            kotlinx.coroutines.delay(7000)
                            homeVm.dismissSyncMessaggio()
                        }
                        Card(modifier = Modifier.padding(top = 6.dp), shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary)) {
                            Text(msg, Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.White, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Text("Impara qualcosa di nuovo ogni giorno",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 6.dp, bottom = if (isSmallScreen) 12.dp else 20.dp))

                GamificationBanner(xpTotali = xpTotali, streakCorrente = streakCorrente,
                    modifier = Modifier.padding(bottom = if (isSmallScreen) 12.dp else 20.dp))

                MenuCard(Icons.Default.EmojiObjects, "Scopri una Curiosità",
                    "Leggi e impara qualcosa di sorprendente", MaterialTheme.colorScheme.primary) {
                    nav.navigate("category_picker/curiosity") }
                Spacer(Modifier.height(12.dp))
                MenuCard(Icons.Default.Quiz, "Fai un Quiz",
                    "Metti alla prova le tue conoscenze", MaterialTheme.colorScheme.secondary) {
                    nav.navigate("category_picker/quiz") }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

// ── Dialog changelog ──────────────────────────────────────────────────────────
@Composable
private fun ChangelogDialog(versioni: List<VersioneChangelog>, titolo: String, onDismiss: () -> Unit) {
    val colorNovita = Color(0xFF4CAF50)
    val colorFix    = Color(0xFFFF9800)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(titolo, fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        text  = {
            val scrollState    = rememberScrollState()
            val isScrollable   by remember { derivedStateOf { scrollState.maxValue > 0 } }
            val isAtBottom     by remember { derivedStateOf { scrollState.value >= scrollState.maxValue } }
            val showScrollHint by remember { derivedStateOf { isScrollable && !isAtBottom } }

            // Anima l'opacità del bounce hint per richiamare l'attenzione all'apertura
            val bounceAlpha by animateFloatAsState(
                targetValue   = if (showScrollHint) 1f else 0f,
                animationSpec = tween(300),
                label         = "bounceAlpha"
            )
            val infiniteBounce = rememberInfiniteTransition(label = "bounce")
            val bounceOffset by infiniteBounce.animateFloat(
                initialValue  = 0f,
                targetValue   = 6f,
                animationSpec = infiniteRepeatable(
                    tween(500, easing = FastOutSlowInEasing),
                    RepeatMode.Reverse
                ),
                label = "bounceOffset"
            )

            Box(Modifier.fillMaxWidth()) {
                // ── Contenuto scrollabile ──────────────────────────────────────
                Column(
                    Modifier
                        .fillMaxWidth()
                        .heightIn(max = 420.dp)
                        .verticalScroll(scrollState)
                        .padding(end = 14.dp) // spazio per la scrollbar
                ) {
                    versioni.forEachIndexed { index, versione ->
                        if (index > 0) HorizontalDivider(Modifier.padding(vertical = 16.dp),
                            color = MaterialTheme.colorScheme.outlineVariant)
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                            Surface(shape = RoundedCornerShape(10.dp), color = MaterialTheme.colorScheme.primary) {
                                Text("v${versione.versione}", modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold, color = Color.White)
                            }
                            if (versione.data.isNotBlank()) {
                                Spacer(Modifier.width(10.dp))
                                Text(versione.data, style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f))
                            }
                        }
                        if (versione.novita.isNotEmpty()) SezioneChangelog("Novità", Icons.Default.Star, colorNovita, versione.novita)
                        if (versione.fix.isNotEmpty()) {
                            if (versione.novita.isNotEmpty()) Spacer(Modifier.height(12.dp))
                            SezioneChangelog("Correzioni", Icons.Default.BugReport, colorFix, versione.fix)
                        }
                    }
                }

                // ── Barra scroll laterale (più spessa e visibile) ──────────────
                if (isScrollable) {
                    val fraction = (scrollState.value.toFloat() / scrollState.maxValue.toFloat()).coerceIn(0f, 1f)
                    val trackHeight = 420
                    val thumbFraction = 0.22f
                    Box(
                        Modifier
                            .align(Alignment.CenterEnd)
                            .width(6.dp)
                            .heightIn(max = trackHeight.dp)
                            .fillMaxHeight()
                            .background(
                                MaterialTheme.colorScheme.outlineVariant,
                                RoundedCornerShape(3.dp)
                            )
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(thumbFraction)
                                .align(Alignment.TopStart)
                                .offset(y = androidx.compose.ui.unit.Dp(fraction * trackHeight * (1f - thumbFraction)))
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(3.dp)
                                )
                        )
                    }
                }

                // ── Hint "scorri" con pill + freccia animata ───────────────────
                Box(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .offset(y = androidx.compose.ui.unit.Dp(bounceOffset))
                        .graphicsLayer(alpha = bounceAlpha)
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    ) {
                        Row(
                            Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.KeyboardArrowDown, null,
                                Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                "Scorri per vedere di più",
                                style      = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color      = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("Ottimo!", fontWeight = FontWeight.Bold) } }
    )
}

@Composable
private fun SezioneChangelog(etichetta: String, icona: ImageVector, colore: Color, voci: List<String>) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 6.dp)) {
        Box(Modifier.size(24.dp).background(colore.copy(alpha = 0.15f), RoundedCornerShape(6.dp)), contentAlignment = Alignment.Center) {
            Icon(icona, null, tint = colore, modifier = Modifier.size(14.dp))
        }
        Spacer(Modifier.width(7.dp))
        Text(etichetta, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = colore)
    }
    voci.forEach { voce ->
        Row(modifier = Modifier.padding(start = 4.dp, bottom = 5.dp), verticalAlignment = Alignment.Top) {
            Text("·  ", color = colore, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, lineHeight = 22.sp)
            Text(voce, style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f), modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun MenuCard(icon: ImageVector, title: String, subtitle: String, color: Color, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = color), elevation = CardDefaults.cardElevation(8.dp)) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(38.dp), Color.White.copy(alpha = 0.9f))
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.85f), maxLines = 1)
            }
        }
    }
}

@Composable
private fun NotificheDialog(notifiche: List<FirebaseManager.NotificaInApp>, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon  = { Text("🔔", fontSize = 32.sp) },
        title = {
            Text(if (notifiche.size == 1) "Hai una novità" else "Hai ${notifiche.size} novità",
                fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        },
        text  = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                notifiche.forEach { n ->
                    Card(shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)) {
                        Column(Modifier.padding(12.dp)) {
                            Text(n.titolo, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(4.dp))
                            Text(n.corpo, style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f))
                        }
                    }
                }
            }
        },
        confirmButton = { Button(onClick = onDismiss) { Text("Ho capito", fontWeight = FontWeight.Bold) } }
    )
}