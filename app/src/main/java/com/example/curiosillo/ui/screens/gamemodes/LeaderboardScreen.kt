package com.example.curiosillo.ui.screens.gamemodes

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.curiosillo.firebase.FirebaseManager
import com.example.curiosillo.viewmodel.LeaderboardViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(nav: NavController) {
    val vm: LeaderboardViewModel = viewModel(factory = LeaderboardViewModel.Factory())
    val state by vm.state.collectAsState()

    val brushBg = Brush.verticalGradient(
        listOf(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), MaterialTheme.colorScheme.background)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Classifiche", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Indietro")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { pad ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(brushBg)
                .padding(pad)
        ) {
            // Selettore Modalità
            TabRow(
                selectedTabIndex = if (state.mode == FirebaseManager.LeaderboardMode.SCALATA) 0 else 1,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                divider = {}
            ) {
                Tab(
                    selected = state.mode == FirebaseManager.LeaderboardMode.SCALATA,
                    onClick = { vm.setMode(FirebaseManager.LeaderboardMode.SCALATA) },
                    text = { Text("Scalata", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Timer, null) }
                )
                Tab(
                    selected = state.mode == FirebaseManager.LeaderboardMode.SOPRAVVIVENZA,
                    onClick = { vm.setMode(FirebaseManager.LeaderboardMode.SOPRAVVIVENZA) },
                    text = { Text("Sopravvivenza", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Whatshot, null) }
                )
            }

            Spacer(Modifier.height(8.dp))

            // Selettore Temporale (Settimanale / All-time)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                FilterChip(
                    selected = state.isWeekly,
                    onClick = { vm.setWeekly(true) },
                    label = { Text("Questa Settimana") },
                    shape = RoundedCornerShape(16.dp)
                )
                Spacer(Modifier.width(8.dp))
                FilterChip(
                    selected = !state.isWeekly,
                    onClick = { vm.setWeekly(false) },
                    label = { Text("Migliori di Sempre") },
                    shape = RoundedCornerShape(16.dp)
                )
            }

            Box(Modifier.fillMaxSize()) {
                if (state.isLoading && state.entries.isEmpty()) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                } else if (state.entries.isEmpty()) {
                    EmptyLeaderboardView()
                } else {
                    LeaderboardList(state.entries, state.userEntry)
                }
            }
        }
    }
}

@Composable
fun LeaderboardList(
    entries: List<FirebaseManager.LeaderboardEntry>,
    userEntry: FirebaseManager.LeaderboardEntry?
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(entries) { index, entry ->
            LeaderboardItem(entry, index + 1)
        }
        
        // Se l'utente non è nella top 50, potremmo mostrare la sua posizione qui (opzionale)
    }
}

@Composable
fun LeaderboardItem(entry: FirebaseManager.LeaderboardEntry, rank: Int) {
    val isCurrentUser = entry.uid == FirebaseManager.uid
    val bgColor = if (isCurrentUser) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    val elevation = if (isCurrentUser) 4.dp else 1.dp

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        elevation = CardDefaults.cardElevation(elevation)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank
            Box(
                modifier = Modifier.size(36.dp),
                contentAlignment = Alignment.Center
            ) {
                when (rank) {
                    1 -> Icon(Icons.Default.EmojiEvents, "Primo", tint = Color(0xFFFFD700), modifier = Modifier.size(28.dp))
                    2 -> Icon(Icons.Default.EmojiEvents, "Secondo", tint = Color(0xFFC0C0C0), modifier = Modifier.size(26.dp))
                    3 -> Icon(Icons.Default.EmojiEvents, "Terzo", tint = Color(0xFFCD7F32), modifier = Modifier.size(24.dp))
                    else -> Text("$rank", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }

            Spacer(Modifier.width(12.dp))

            // Avatar
            AsyncImage(
                model = entry.photoUrl ?: "https://api.dicebear.com/7.x/avataaars/svg?seed=${entry.username}",
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray),
                contentScale = ContentScale.Crop
            )

            Spacer(Modifier.width(12.dp))

            // Username
            Text(
                text = entry.username,
                modifier = Modifier.weight(1f),
                fontWeight = if (isCurrentUser) FontWeight.ExtraBold else FontWeight.Bold,
                maxLines = 1
            )

            // Punteggio
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${entry.score}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "punti",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
fun EmptyLeaderboardView() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("🏆", fontSize = 64.sp)
        Spacer(Modifier.height(16.dp))
        Text(
            "Ancora nessuno in classifica.\nSii il primo!",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}
