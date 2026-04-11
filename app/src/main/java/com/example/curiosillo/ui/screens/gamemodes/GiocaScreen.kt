package com.example.curiosillo.ui.screens.gamemodes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.SportsMartialArts
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.navigation.NavController
import com.example.curiosillo.CuriosityApplication

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GiocaScreen(nav: NavController) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as CuriosityApplication
    val prefs = app.gamificationPrefs
    
    val record by prefs.recordSopravvivenza.collectAsState(initial = 0)
    val partite by prefs.partiteSopravvivenza.collectAsState(initial = 0)

    val gradientBg = Brush.verticalGradient(
        listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
            MaterialTheme.colorScheme.background
        )
    )

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Area Giochi", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBg)
                .padding(pad)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            // Banner Statistiche Sopravvivenza
            if (partite > 0) {
                HardcoreStatsBanner(record, partite)
                Spacer(Modifier.height(24.dp))
            }

            Text(
                "Scegli come divertirti oggi!",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
            
            Spacer(Modifier.height(24.dp))

            GiocaCard(
                title    = "Quiz Standard",
                subtitle = "Rispondi a domande su varie categorie e scala la classifica.",
                icon     = Icons.Default.Quiz,
                color    = MaterialTheme.colorScheme.secondary,
                onClick  = { nav.navigate("category_picker/quiz") }
            )

            Spacer(Modifier.height(20.dp))

            GiocaCard(
                title    = "Sopravvivenza",
                subtitle = "Hardcore! 3 vite, domande a raffica. Quanto resisterai?",
                icon     = Icons.Default.Whatshot,
                color    = Color(0xFF121212),
                onClick  = { nav.navigate("sopravvivenza") }
            )

            Spacer(Modifier.height(20.dp))

            GiocaCard(
                title    = "Modalità Duello",
                subtitle = "Sfida un amico o un avversario casuale in tempo reale.",
                icon     = Icons.Default.SportsMartialArts,
                color    = MaterialTheme.colorScheme.primary,
                onClick  = { nav.navigate("duello_lobby") }
            )
            
            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun HardcoreStatsBanner(record: Int, partite: Int) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF1E1E1E),
        tonalElevation = 4.dp,
        border = BorderStroke(1.dp, Color(0xFFBC4749).copy(alpha = 0.5f))
    ) {
        Row(
            Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.BarChart, null, tint = Color(0xFFFFB703), modifier = Modifier.size(32.dp))
            Spacer(Modifier.width(16.dp))
            Column {
                Text("STATISTICHE HARDCORE", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f), fontWeight = FontWeight.Bold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("RECORD:", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.width(6.dp))
                    Text("$record", color = Color(0xFFFFB703), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black)
                    Spacer(Modifier.width(16.dp))
                    Text("PARTITE:", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.width(6.dp))
                    Text("$partite", color = Color.White, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun GiocaCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(56.dp)
                    .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, Modifier.size(32.dp), Color.White)
            }
            
            Spacer(Modifier.width(20.dp))
            
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f),
                    lineHeight = MaterialTheme.typography.bodySmall.lineHeight * 1.2
                )
            }
        }
    }
}
