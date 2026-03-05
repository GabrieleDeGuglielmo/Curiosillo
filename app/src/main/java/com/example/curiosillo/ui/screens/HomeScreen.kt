package com.example.curiosillo.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.EmojiObjects
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.curiosillo.CuriosityApplication
import com.example.curiosillo.R
import com.example.curiosillo.ui.components.GamificationBanner
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(nav: NavController) {
    val ctx        = LocalContext.current
    val app        = ctx.applicationContext as CuriosityApplication
    val prefs      = app.categoryPrefs
    val gamifPrefs = app.gamificationPrefs
    val themePrefs = app.themePrefs
    val scope      = rememberCoroutineScope()

    val categorieAttive by prefs.categorieAttive.collectAsState(initial = emptySet())
    val xpTotali        by gamifPrefs.xpTotali.collectAsState(initial = 0)
    val streakCorrente  by gamifPrefs.streakCorrente.collectAsState(initial = 0)
    val isDarkMode      by themePrefs.isDarkMode.collectAsState(initial = false)

    val bg = MaterialTheme.colorScheme.background

    Box(modifier = Modifier.fillMaxSize().background(bg)) {

        // ── Toggle dark mode ──────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = 30.dp, start = 12.dp)
                .size(42.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape)
                .clickable { scope.launch { themePrefs.setDarkMode(!isDarkMode) } },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector        = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                contentDescription = "Tema",
                modifier           = Modifier.size(22.dp),
                tint               = MaterialTheme.colorScheme.primary
            )
        }

        // ── Pulsante profilo ──────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 30.dp, end = 12.dp)
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

            if (categorieAttive.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment     = Alignment.CenterVertically
                ) {
                    Text("Filtro attivo: ",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f))
                    AssistChip(
                        onClick = { nav.navigate("category_picker/curiosity") },
                        label   = {
                            val testo = if (categorieAttive.size == 1) categorieAttive.first()
                            else "${categorieAttive.size} categorie"
                            Text(testo, fontWeight = FontWeight.SemiBold)
                        },
                        trailingIcon = {
                            Icon(Icons.Default.Close, "Rimuovi filtro", Modifier.size(14.dp))
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MenuCard(icon: ImageVector, title: String, subtitle: String, color: Color, onClick: () -> Unit) {
    Card(
        onClick   = onClick,
        modifier  = Modifier.fillMaxWidth().height(100.dp),
        shape     = RoundedCornerShape(22.dp),
        colors    = CardDefaults.cardColors(containerColor = color),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 22.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, Modifier.size(46.dp), Color.White.copy(alpha = 0.9f))
            Spacer(Modifier.width(18.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold, color = Color.White)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f))
            }
        }
    }
}
