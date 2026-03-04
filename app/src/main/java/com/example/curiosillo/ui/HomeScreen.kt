package com.example.curiosillo.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiObjects
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.curiosillo.R
import com.example.curiosillo.ui.theme.Primary
import com.example.curiosillo.ui.theme.Secondary

@Composable
fun HomeScreen(nav: NavController) {
    Box(modifier = Modifier.fillMaxSize().background(
        Brush.verticalGradient(listOf(Color(0xFFFFF3E0), Color.White)))) {
        Column(
            modifier = Modifier.fillMaxSize().padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo
            Image(
                painter = painterResource(id = R.drawable.ic_logo),
                contentDescription = "Logo Curiosillo",
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .shadow(8.dp, CircleShape),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.height(16.dp))
            Text("Curiosillo",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.ExtraBold,
                color = Primary)
            Text("Impara qualcosa di nuovo ogni giorno",
                style = MaterialTheme.typography.bodyLarge,
                color = Color(0xFF888888),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp, bottom = 56.dp))

            MenuCard(Icons.Default.EmojiObjects,
                "Scopri una Curiosità",
                "Leggi e impara qualcosa di sorprendente",
                Primary) { nav.navigate("curiosity") }
            Spacer(Modifier.height(20.dp))
            MenuCard(Icons.Default.Quiz,
                "Fai un Quiz",
                "Metti alla prova le tue conoscenze",
                Secondary) { nav.navigate("quiz") }
        }
    }
}

@Composable
private fun MenuCard(icon: ImageVector, title: String, subtitle: String,
                     color: Color, onClick: () -> Unit) {
    Card(onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(110.dp),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        elevation = CardDefaults.cardElevation(8.dp)) {
        Row(Modifier.fillMaxSize().padding(horizontal = 26.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, Modifier.size(52.dp), Color.White.copy(alpha = 0.9f))
            Spacer(Modifier.width(20.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold, color = Color.White)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f))
            }
        }
    }
}
