package com.example.curiosillo.ui.screens.user

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.curiosillo.CuriosityApplication
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(nav: NavController) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as CuriosityApplication
    val scope = rememberCoroutineScope()

    val isDarkMode by app.themePrefs.isDarkMode.collectAsState(initial = false)
    val isMusicEnabled by app.themePrefs.isMusicEnabled.collectAsState(initial = true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Impostazioni", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text(
                text = "Aspetto e Suoni",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column {
                    /* Dark Mode
                    ListItem(
                        headlineContent = { Text("Tema Scuro") },
                        supportingContent = { Text("Attiva o disattiva il tema scuro") },
                        leadingContent = { Icon(Icons.Default.DarkMode, contentDescription = null) },
                        trailingContent = {
                            Switch(
                                checked = isDarkMode,
                                onCheckedChange = { scope.launch { app.themePrefs.setDarkMode(it) } }
                            )
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    */
                    // Music
                    ListItem(
                        headlineContent = { Text("Musica di Sottofondo") },
                        supportingContent = { Text("Riproduci musica durante l'uso dell'app") },
                        leadingContent = { Icon(Icons.Default.MusicNote, contentDescription = null) },
                        trailingContent = {
                            Switch(
                                checked = isMusicEnabled,
                                onCheckedChange = { scope.launch { app.themePrefs.setMusicEnabled(it) } }
                            )
                        }
                    )
                }
            }
        }
    }
}
