package com.example.curiosillo.ui.screens.gamemodes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.SportsMartialArts
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.curiosillo.ui.components.CuriosilloBottomBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GiocaScreen(nav: NavController) {
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
        bottomBar = { CuriosilloBottomBar(nav) },
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
            Text(
                "Scegli come divertirti oggi!",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
            
            Spacer(Modifier.height(32.dp))

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