package com.example.curiosillo.ui.screens.gamemodes

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.curiosillo.CuriosityApplication
import com.example.curiosillo.viewmodel.SopravvivenzaUiState
import com.example.curiosillo.viewmodel.SopravvivenzaViewModel

// Palette Hardcore
private val NeroOssidiana   = Color(0xFF121212)
private val GrigioAntracite = Color(0xFF1E1E1E)
private val RossoLava       = Color(0xFFBC4749)
private val OroElettrico    = Color(0xFFFFB703)
private val BiancoGhiaccio  = Color(0xFFEAEAEA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SopravvivenzaScreen(nav: NavController) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as CuriosityApplication
    val vm: SopravvivenzaViewModel = viewModel(
        factory = SopravvivenzaViewModel.Factory(app.repository, app.gamificationPrefs)
    )
    val state by vm.state.collectAsState()
    
    var showExitDialog by remember { mutableStateOf(false) }

    // Intercetta il tasto indietro del sistema
    BackHandler(enabled = state is SopravvivenzaUiState.InCorso) {
        showExitDialog = true
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = {
                Text(
                    text = "Abbandonare la sfida?",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold,
                    color = BiancoGhiaccio
                )
            },
            text = {
                Text(
                    text = "Se esci ora perderai tutti i progressi di questa partita.",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    color = BiancoGhiaccio
                )
            },
            confirmButton = {
                TextButton(onClick = { 
                    showExitDialog = false
                    nav.popBackStack() 
                }) {
                    Text("ESCI", color = RossoLava, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("RESTA", color = Color.White)
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = GrigioAntracite,
            titleContentColor = Color.White,
            textContentColor = Color.White.copy(alpha = 0.8f)
        )
    }

    // Gestione Musica
    val isMusicEnabled by app.themePrefs.isMusicEnabled.collectAsState(initial = true)
    DisposableEffect(Unit) {
        app.musicManager.stop()
        onDispose {
            if (isMusicEnabled) app.musicManager.start()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sopravvivenza", color = BiancoGhiaccio, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { 
                        if (state is SopravvivenzaUiState.InCorso) showExitDialog = true
                        else nav.popBackStack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Esci", tint = BiancoGhiaccio)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = NeroOssidiana
    ) { pad ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(pad)
        ) {
            AnimatedContent(
                targetState = state,
                transitionSpec = {
                    fadeIn(animationSpec = tween(400)) togetherWith fadeOut(animationSpec = tween(400))
                },
                contentKey = { it::class },
                label = "state_transition"
            ) { targetState ->
                when (targetState) {
                    is SopravvivenzaUiState.Loading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                        CircularProgressIndicator(color = RossoLava)
                    }
                    is SopravvivenzaUiState.InCorso -> GameContent(
                        s = targetState, 
                        onRisposta = { vm.rispondi(it) },
                        onAvanti = { vm.vaiAvanti() }
                    )
                    is SopravvivenzaUiState.GameOver -> GameOverContent(
                        s = targetState, 
                        onRiprova = { vm.iniziaPartita() }, 
                        onEsci = { nav.popBackStack() }
                    )
                }
            }
        }
    }
}

@Composable
private fun GameContent(
    s: SopravvivenzaUiState.InCorso, 
    onRisposta: (String) -> Unit,
    onAvanti: () -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Vite e Statistiche
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Vite
            Row {
                repeat(3) { i ->
                    val active = i < s.vite
                    Icon(
                        imageVector = if (active) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = if (active) RossoLava else BiancoGhiaccio.copy(alpha = 0.2f),
                        modifier = Modifier.size(34.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                }
            }
            
            // Streak e Record
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(horizontalAlignment = Alignment.End) {
                    Text("SERIE", color = BiancoGhiaccio.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall)
                    Text("${s.streak}", color = OroElettrico, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black)
                }
                Spacer(Modifier.width(16.dp))
                Column(horizontalAlignment = Alignment.End) {
                    Text("RECORD", color = BiancoGhiaccio.copy(alpha = 0.5f), style = MaterialTheme.typography.labelSmall)
                    Text("${s.recordPersonale}", color = BiancoGhiaccio, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(40.dp))

        AnimatedContent(
            targetState = s.domanda,
            transitionSpec = {
                (slideInHorizontally { width -> width } + fadeIn()).togetherWith(slideOutHorizontally { width -> -width } + fadeOut())
            },
            label = "question_transition"
        ) { currentDomanda ->
            Column(
                Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = GrigioAntracite),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Text(
                        currentDomanda.questionText,
                        modifier = Modifier.padding(24.dp),
                        style = MaterialTheme.typography.titleLarge,
                        color = BiancoGhiaccio,
                        textAlign = TextAlign.Center,
                        lineHeight = 32.sp
                    )
                }

                Spacer(Modifier.height(32.dp))

                Column(
                    Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                ) {
                    s.risposte.forEach { risposta ->
                        val isSelezionata = s.rispostaSelezionata == risposta
                        val isCorretta = risposta == currentDomanda.correctAnswer
                        
                        val bgColor by animateColorAsState(
                            targetValue = when {
                                s.mostraCorretta && isCorretta -> Color(0xFF2D6A4F)
                                isSelezionata && !isCorretta -> RossoLava
                                else -> GrigioAntracite.copy(alpha = 0.7f)
                            },
                            animationSpec = tween(300),
                            label = "bgColor"
                        )
                        
                        val borderColor by animateColorAsState(
                            targetValue = when {
                                s.mostraCorretta && isCorretta -> Color(0xFF52B788)
                                isSelezionata && !isCorretta -> Color.Red
                                else -> BiancoGhiaccio.copy(alpha = 0.1f)
                            },
                            animationSpec = tween(300),
                            label = "borderColor"
                        )

                        val scale by animateFloatAsState(
                            targetValue = if (isSelezionata) 1.05f else 1f,
                            animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            label = "scale"
                        )

                        OutlinedButton(
                            onClick = { onRisposta(risposta) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .heightIn(min = 64.dp)
                                .graphicsLayer(scaleX = scale, scaleY = scale),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = bgColor,
                                contentColor = BiancoGhiaccio,
                                disabledContainerColor = bgColor,
                                disabledContentColor = BiancoGhiaccio
                            ),
                            border = BorderStroke(2.dp, borderColor),
                            enabled = s.rispostaSelezionata == null
                        ) {
                            Text(
                                risposta,
                                textAlign = TextAlign.Center,
                                color = BiancoGhiaccio,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = if (isSelezionata) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                // Tasto Avanti
                AnimatedVisibility(
                    visible = s.rispostaSelezionata != null,
                    enter = slideInVertically { it } + fadeIn(),
                    exit = slideOutVertically { it } + fadeOut()
                ) {
                    Button(
                        onClick = onAvanti,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                            .height(64.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OroElettrico),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("AVANTI", color = NeroOssidiana, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                            Spacer(Modifier.width(8.dp))
                            Icon(Icons.Default.ArrowForward, null, tint = NeroOssidiana)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GameOverContent(s: SopravvivenzaUiState.GameOver, onRiprova: () -> Unit, onEsci: () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "skull")
        val scale by infiniteTransition.animateFloat(
            initialValue = 1f,
            targetValue = 1.2f,
            animationSpec = infiniteRepeatable(tween(1000), RepeatMode.Reverse),
            label = "scale"
        )

        Text("💀", fontSize = 90.sp, modifier = Modifier.graphicsLayer(scaleX = scale, scaleY = scale))
        Spacer(Modifier.height(24.dp))
        Text("FINE CORSA", style = MaterialTheme.typography.displayMedium, fontWeight = FontWeight.Black, color = RossoLava)
        Spacer(Modifier.height(16.dp))
        Text("SERIE FINALE", color = BiancoGhiaccio.copy(alpha = 0.6f), style = MaterialTheme.typography.labelLarge)
        Text("${s.streakFinale}", style = MaterialTheme.typography.displayLarge, color = OroElettrico, fontWeight = FontWeight.Black)

        if (s.nuovoRecord) {
            Surface(color = RossoLava, shape = RoundedCornerShape(12.dp), modifier = Modifier.padding(top = 12.dp)) {
                Text("NUOVO RECORD PERSONALE!", modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp), color = BiancoGhiaccio, fontWeight = FontWeight.ExtraBold)
            }
        } else {
             Text("Miglior serie di sempre: ${s.recordPrecedente}", color = BiancoGhiaccio.copy(alpha = 0.4f), style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(Modifier.height(56.dp))
        Button(onClick = onRiprova, modifier = Modifier.fillMaxWidth().height(60.dp), colors = ButtonDefaults.buttonColors(containerColor = RossoLava), shape = RoundedCornerShape(18.dp)) {
            Text("SFIDA DI NUOVO IL DESTINO", fontWeight = FontWeight.Black, color = BiancoGhiaccio)
        }
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onEsci, modifier = Modifier.fillMaxWidth().height(56.dp)) {
            Text("TORNA AI GIOCHI", color = BiancoGhiaccio.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
        }
    }
}
