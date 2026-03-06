package com.example.curiosillo.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiObjects
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.curiosillo.CuriosityApplication
import com.example.curiosillo.R
import com.example.curiosillo.firebase.FirebaseManager
import com.example.curiosillo.ui.components.GamificationBanner
import com.example.curiosillo.viewmodel.HomeViewModel

@Composable
fun HomeScreen(nav: NavController) {
    val ctx        = LocalContext.current
    val app        = ctx.applicationContext as CuriosityApplication
    //val scope      = rememberCoroutineScope()

    // ViewModel sync/update
    val homeVm: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(app.repository, app.contentPrefs, ctx)
    )
    val homeState by homeVm.state.collectAsState()

    // Prefs esistenti
    val categorieAttive by app.categoryPrefs.categorieAttive.collectAsState(initial = emptySet())
    val xpTotali        by app.gamificationPrefs.xpTotali.collectAsState(initial = 0)
    val streakCorrente  by app.gamificationPrefs.streakCorrente.collectAsState(initial = 0)
    //val isDarkMode      by app.themePrefs.isDarkMode.collectAsState(initial = false)

    // Dialog aggiornamento — solo se l'utente è loggato
    // (se non è loggato, il check lo fa LoginScreen)
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

        /* ── Toggle dark mode ──────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 24.dp, start = 12.dp)
                .size(42.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape)
                .clickable { scope.launch { app.themePrefs.setDarkMode(!isDarkMode) } },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                contentDescription = "Tema",
                modifier           = Modifier.size(22.dp),
                tint               = MaterialTheme.colorScheme.primary
            )
        }
        */

        // ── Pulsante profilo ──────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 35.dp, end = 12.dp)
                .size(42.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape)
                .clickable { nav.navigate("profile") },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Person, "Profilo",
                modifier = Modifier.size(26.dp),
                tint     = MaterialTheme.colorScheme.primary)
        }

        Column(
            modifier = Modifier.fillMaxSize().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter            = painterResource(id = R.drawable.ic_logo),
                contentDescription = "Logo Curiosillo",
                modifier           = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .shadow(8.dp, CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.height(16.dp))
            Text("Curiosillo",
                style      = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.ExtraBold,
                color      = MaterialTheme.colorScheme.primary)

            // Messaggio sync (sparisce dopo 3 secondi)
            AnimatedVisibility(
                visible = homeState.syncMessaggio != null,
                enter   = fadeIn() + expandVertically(),
                exit    = fadeOut() + shrinkVertically()
            ) {
                homeState.syncMessaggio?.let { msg ->
                    LaunchedEffect(msg) {
                        kotlinx.coroutines.delay(3000)
                        homeVm.dismissSyncMessaggio()
                    }
                    Card(
                        modifier = Modifier.padding(top = 6.dp),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text(msg, Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            style      = MaterialTheme.typography.bodySmall,
                            color      = Color.White,
                            fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            Text("Impara qualcosa di nuovo ogni giorno",
                style     = MaterialTheme.typography.bodyLarge,
                color     = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                textAlign = TextAlign.Center,
                modifier  = Modifier.padding(top = 6.dp, bottom = 20.dp))

            GamificationBanner(
                xpTotali       = xpTotali,
                streakCorrente = streakCorrente,
                modifier       = Modifier.padding(bottom = 20.dp)
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
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 2)
            }
        }
    }
}