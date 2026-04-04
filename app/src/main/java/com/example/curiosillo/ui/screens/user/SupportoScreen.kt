package com.example.curiosillo.ui.screens.user

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.curiosillo.firebase.FirebaseManager
import com.example.curiosillo.ui.screens.utils.LISTA_CATEGORIE

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportoScreen(nav: NavController) {
    val gradientBg = Brush.verticalGradient(
        listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.background
        )
    )

    Box(Modifier.fillMaxSize().background(gradientBg)) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Supporto", color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (nav.currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
                                nav.popBackStack()
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Indietro", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            // Navbar rimossa
            containerColor = Color.Transparent
        ) { pad ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(pad)
                    .padding(24.dp)
            ) {
                Text(
                    "Come possiamo aiutarti?",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 24.dp),
                    color = Color.White
                )

                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(Modifier.padding(8.dp)) {
                        SupportOptionItem(
                            icon = Icons.Default.BugReport,
                            label = "Segnala un problema",
                            sub = "Inviaci una segnalazione tecnica"
                        ) {
                            nav.navigate("supporto_bug")
                        }

                        HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        SupportOptionItem(
                            icon = Icons.Default.AutoAwesome,
                            label = "Suggerisci una curiosità",
                            sub = "Inviaci un'idea per una nuova pillola. Aiuta Curiosillo a diventare ancora più acculturato! 🐾"
                        ) {
                            nav.navigate("supporto_curiosita")
                        }

                        HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                        SupportOptionItem(
                            icon = Icons.Default.Lightbulb,
                            label = "Suggerisci funzionalità",
                            sub = "Aiutaci a migliorare Curiosillo"
                        ) {
                            nav.navigate("supporto_suggerimento")
                        }
                    }
                }

                Spacer(Modifier.weight(1f))

                Text(
                    text = "Gabriele De Guglielmo aka GDG",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.5f),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}

@Composable
private fun SupportOptionItem(
    icon: ImageVector,
    label: String,
    sub: String,
    onClick: () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(48.dp)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), RoundedCornerShape(12.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                sub, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        Icon(
            Icons.Default.ChevronRight, null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BugReportScreen(nav: NavController) {
    val context = LocalContext.current
    val gradientBg = Brush.verticalGradient(
        listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.background
        )
    )

    var oggetto by remember { mutableStateOf("Errore generico") }
    var descrizione by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var expanded by remember { mutableStateOf(false) }

    val opzioni = listOf("Errore generico", "Problema Quiz", "Errore caricamento pillole", "Problema Profilo/XP", "Altro")

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        imageUri = uri
    }

    Box(Modifier.fillMaxSize().background(gradientBg)) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Segnala un problema", color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = { nav.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Indietro", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { pad ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(pad)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Oggetto (Dropdown)
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            OutlinedTextField(
                                value = oggetto,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Oggetto") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false }
                            ) {
                                opzioni.forEach { selectionOption ->
                                    DropdownMenuItem(
                                        text = { Text(selectionOption) },
                                        onClick = {
                                            oggetto = selectionOption
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Descrizione
                        OutlinedTextField(
                            value = descrizione,
                            onValueChange = { descrizione = it },
                            label = { Text("Descrizione") },
                            placeholder = { Text("Descrivi il problema riscontrato e invia la segnalazione tramite posta elettronica\n(Min. 10 lettere)") },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                            shape = RoundedCornerShape(12.dp)
                        )

                        // Allegato
                        if (imageUri != null) {
                            Box(Modifier.fillMaxWidth().height(200.dp)) {
                                AsyncImage(
                                    model = imageUri,
                                    contentDescription = "Screenshot",
                                    modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.05f), RoundedCornerShape(12.dp)),
                                    contentScale = ContentScale.Fit
                                )
                                IconButton(
                                    onClick = { imageUri = null },
                                    modifier = Modifier.align(Alignment.TopEnd).background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(bottomStart = 12.dp))
                                ) {
                                    Icon(Icons.Default.Close, null, tint = Color.White)
                                }
                            }
                        } else {
                            OutlinedButton(
                                onClick = { launcher.launch("image/*") },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp),
                                contentPadding = PaddingValues(16.dp)
                            ) {
                                Icon(Icons.Default.AttachFile, null)
                                Spacer(Modifier.width(8.dp))
                                Text("Allega screenshot (opzionale)")
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // Tasto invio
                        Button(
                            onClick = {
                                val user = FirebaseManager.utenteCorrente
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "message/rfc822"
                                    putExtra(Intent.EXTRA_EMAIL, arrayOf("support.curiosillo@gmail.com"))
                                    putExtra(Intent.EXTRA_SUBJECT, "BUG REPORT: $oggetto")
                                    val corpoEmail = """
                                        Oggetto: $oggetto
                                        Utente: ${user?.displayName ?: "Anonimo"} (${user?.email ?: "N/D"})
                                        UID: ${user?.uid ?: "N/D"}
                                        
                                        Descrizione:
                                        $descrizione
                                    """.trimIndent()
                                    putExtra(Intent.EXTRA_TEXT, corpoEmail)
                                    imageUri?.let {
                                        putExtra(Intent.EXTRA_STREAM, it)
                                        type = "image/*"
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                }
                                try {
                                    context.startActivity(Intent.createChooser(intent, "Invia segnalazione con:"))
                                    nav.popBackStack()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Nessuna app email trovata", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = descrizione.trim().length >= 10,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Invia segnalazione")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuggerimentoCuriositaScreen(nav: NavController) {
    val context = LocalContext.current
    val gradientBg = Brush.verticalGradient(
        listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.background
        )
    )

    var categoria by remember { mutableStateOf(LISTA_CATEGORIE.first()) }
    var descrizione by remember { mutableStateOf("") }
    var expanded by remember { mutableStateOf(false) }

    val opzioni = LISTA_CATEGORIE + "Altro"

    Box(Modifier.fillMaxSize().background(gradientBg)) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Suggerisci curiosità", color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = { nav.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Indietro", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { pad ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(pad)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        // Categoria (Dropdown)
                        ExposedDropdownMenuBox(
                            expanded = expanded,
                            onExpandedChange = { expanded = !expanded }
                        ) {
                            OutlinedTextField(
                                value = categoria,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Categoria") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                shape = RoundedCornerShape(12.dp)
                            )
                            ExposedDropdownMenu(
                                expanded = expanded,
                                onDismissRequest = { expanded = false },
                                modifier = Modifier.heightIn(max = 280.dp)
                            ) {
                                opzioni.forEach { selectionOption ->
                                    DropdownMenuItem(
                                        text = { Text(selectionOption) },
                                        onClick = {
                                            categoria = selectionOption
                                            expanded = false
                                        }
                                    )
                                }
                            }
                        }

                        // Descrizione
                        OutlinedTextField(
                            value = descrizione,
                            onValueChange = { descrizione = it },
                            label = { Text("Descrizione della curiosità") },
                            placeholder = { Text("Scrivi qui la curiosità che vuoi suggerire...") },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Button(
                            onClick = {
                                val user = FirebaseManager.utenteCorrente
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "message/rfc822"
                                    putExtra(Intent.EXTRA_EMAIL, arrayOf("support.curiosillo@gmail.com"))
                                    putExtra(Intent.EXTRA_SUBJECT, "SUGGERIMENTO CURIOSITÀ: $categoria")
                                    val corpoEmail = """
                                        Categoria: $categoria
                                        Utente: ${user?.displayName ?: "Anonimo"} (${user?.email ?: "N/D"})
                                        
                                        Idea curiosità:
                                        $descrizione
                                    """.trimIndent()
                                    putExtra(Intent.EXTRA_TEXT, corpoEmail)
                                }
                                try {
                                    context.startActivity(Intent.createChooser(intent, "Invia suggerimento con:"))
                                    nav.popBackStack()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Nessuna app email trovata", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = descrizione.trim().length >= 10,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Invia suggerimento")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuggerimentoScreen(nav: NavController) {
    val context = LocalContext.current
    val gradientBg = Brush.verticalGradient(
        listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.background
        )
    )

    var suggerimento by remember { mutableStateOf("") }

    Box(Modifier.fillMaxSize().background(gradientBg)) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Suggerisci funzionalità", color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = { nav.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Indietro", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { pad ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(pad)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                Text(
                    "Quali nuove funzioni vorresti vedere su Curiosillo?",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f))
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = suggerimento,
                            onValueChange = { suggerimento = it },
                            label = { Text("Il tuo suggerimento") },
                            placeholder = { Text("Scrivi qui la tua idea...") },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 150.dp),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Button(
                            onClick = {
                                val user = FirebaseManager.utenteCorrente
                                val intent = Intent(Intent.ACTION_SEND).apply {
                                    type = "message/rfc822"
                                    putExtra(Intent.EXTRA_EMAIL, arrayOf("support.curiosillo@gmail.com"))
                                    putExtra(Intent.EXTRA_SUBJECT, "SUGGERIMENTO FUNZIONALITÀ")
                                    val corpoEmail = """
                                        Tipo: Suggerimento
                                        Utente: ${user?.displayName ?: "Anonimo"} (${user?.email ?: "N/D"})
                                        
                                        Idea:
                                        $suggerimento
                                    """.trimIndent()
                                    putExtra(Intent.EXTRA_TEXT, corpoEmail)
                                }
                                try {
                                    context.startActivity(Intent.createChooser(intent, "Invia suggerimento con:"))
                                    nav.popBackStack()
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Nessuna app email trovata", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = suggerimento.trim().length >= 10,
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Invia suggerimento")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SupportoDetailScreen(nav: NavController, title: String) {
    val gradientBg = Brush.verticalGradient(
        listOf(
            MaterialTheme.colorScheme.primary,
            MaterialTheme.colorScheme.background
        )
    )

    Box(Modifier.fillMaxSize().background(gradientBg)) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text(title, color = Color.White) },
                    navigationIcon = {
                        IconButton(onClick = { nav.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Indietro", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { pad ->
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(pad), 
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.padding(24.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Text(
                        "Pagina $title in arrivo...", 
                        modifier = Modifier.padding(32.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
