package com.example.curiosillo.ui.screens.gamemodes

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.curiosillo.CuriosityApplication
import com.example.curiosillo.viewmodel.ScalataUiState
import com.example.curiosillo.viewmodel.ScalataViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScalataScreen(nav: NavController) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as CuriosityApplication
    val vm: ScalataViewModel = viewModel(
        factory = ScalataViewModel.Factory(app.repository, app.gamificationPrefs)
    )
    val state by vm.state.collectAsState()

    var showExitDialog by remember { mutableStateOf(false) }

    // Intercetta il tasto indietro del sistema
    BackHandler(enabled = state is ScalataUiState.InCorso) {
        showExitDialog = true
    }

    if (showExitDialog) {
        AlertDialog(
            onDismissRequest = { showExitDialog = false },
            title = { 
                Text(
                    text = "Vuoi uscire?", 
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                ) 
            },
            text = { 
                Text(
                    text = "Se esci ora perderai tutti i progressi di questa partita.",
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                ) 
            },
            confirmButton = {
                TextButton(onClick = { 
                    showExitDialog = false
                    nav.popBackStack() 
                }) {
                    Text("ESCI", color = Color(0xFFB71C1C), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitDialog = false }) {
                    Text("ANNULLA")
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White
        )
    }

    // Gradiente dinamico: Rosso scuro sopra, Rosso più vivo sotto
    val inFiamme = (state as? ScalataUiState.InCorso)?.inFiamme == true
    
    val colorTop by animateColorAsState(if (inFiamme) Color(0xFFB71C1C) else Color(0xFF450000), label = "ct")
    val colorBottom by animateColorAsState(if (inFiamme) Color(0xFFFF4500) else Color(0xFF8B0000), label = "cb")
    
    val brushBg = Brush.verticalGradient(listOf(colorTop, colorBottom))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scalata Infernale", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = { 
                        if (state is ScalataUiState.InCorso) showExitDialog = true 
                        else nav.popBackStack() 
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Indietro")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color.Transparent
    ) { pad ->
        Box(modifier = Modifier.fillMaxSize().background(brushBg).padding(pad)) {
            AnimatedContent(
                targetState = state::class,
                transitionSpec = {
                    fadeIn(tween(500)) + scaleIn(initialScale = 0.9f) togetherWith fadeOut(tween(500))
                },
                label = "macroState"
            ) { _ ->
                when (val currentS = state) {
                    is ScalataUiState.Loading -> {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color.White)
                        }
                    }
                    is ScalataUiState.NoQuestions -> {
                        NoQuestionsView { nav.popBackStack() }
                    }
                    is ScalataUiState.InCorso -> {
                        ScalataGameView(currentS, onAnswer = { vm.rispondi(it) })
                    }
                    is ScalataUiState.GameOver -> {
                        GameOverView(currentS, onRetry = { vm.iniziaPartita() }, onExit = { nav.popBackStack() })
                    }
                }
            }
        }
    }
}

@Composable
fun ScalataGameView(s: ScalataUiState.InCorso, onAnswer: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp).verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(16.dp))
        
        // Punteggio Header
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("PUNTEGGIO", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                    AnimatedContent(targetState = s.punteggio, label = "score") { score ->
                        Text("$score", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black, color = Color.White)
                    }
                }
                
                if (s.inFiamme) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocalFireDepartment, null, tint = Color(0xFFFFEB3B), modifier = Modifier.size(32.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("x${s.moltiplicatore}", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Black, color = Color(0xFFFFEB3B))
                    }
                } else {
                    Column(horizontalAlignment = Alignment.End) {
                        Text("STREAK", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                        Text("${s.streak}", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // Timer
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth()) {
            val timerProgress = s.timeLeft.toFloat() / 7f
            val animatedProgress by animateFloatAsState(
                targetValue = timerProgress, 
                animationSpec = if (s.timeLeft == 7) snap() else tween(1000, easing = LinearEasing),
                label = "timer"
            )
            
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxWidth().height(16.dp).clip(CircleShape),
                color = if (s.timeLeft <= 2) Color.White else Color(0xFFFFD700),
                trackColor = Color.Black.copy(alpha = 0.2f)
            )
            
            Text("${s.timeLeft}s", fontWeight = FontWeight.Black, color = if(s.timeLeft <= 2) Color(0xFFB71C1C) else Color.White)
        }

        Spacer(Modifier.height(32.dp))

        // Domanda
        AnimatedContent(
            targetState = s.domanda.id,
            transitionSpec = {
                fadeIn(tween(300)) + slideInVertically { it / 2 } togetherWith fadeOut(tween(300)) + slideOutVertically { -it / 2 }
            },
            label = "q"
        ) { domandaId ->
            val currentDomanda = s.domanda
            val currentRisposte = s.risposte
            
            Column {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(28.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(12.dp)
                ) {
                    Text(
                        text = currentDomanda.questionText,
                        modifier = Modifier.padding(28.dp),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center,
                        color = Color(0xFF1A1A1A)
                    )
                }

                Spacer(Modifier.height(32.dp))

                currentRisposte.forEachIndexed { index, risposta ->
                    // Usiamo 'key' per stabilizzare i componenti delle risposte
                    key(domandaId, risposta) {
                        AnswerButton(risposta, index, s, onAnswer)
                    }
                }
            }
        }
        
        Spacer(Modifier.height(32.dp))
    }
}

@Composable
fun AnswerButton(risposta: String, index: Int, s: ScalataUiState.InCorso, onAnswer: (String) -> Unit) {
    val isSelected = s.rispostaSelezionata == risposta
    val isCorrect = risposta == s.domanda.correctAnswer
    
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * 60L)
        visible = true
    }

    // Animazione colore sfondo
    val targetBgColor = when {
        s.mostraCorretta && isCorrect -> Color(0xFF43A047) // Verde se corretta
        s.mostraCorretta && isSelected && !isCorrect -> Color(0xFFD32F2F) // Rosso se selezionata sbagliata
        isSelected -> Color.White.copy(alpha = 0.95f)
        else -> Color.White.copy(alpha = 0.2f)
    }
    val bgColor by animateColorAsState(targetBgColor, animationSpec = tween(400), label = "btnBg")
    
    // Animazione scala per la risposta corretta
    val scale by animateFloatAsState(
        targetValue = if (s.mostraCorretta && isCorrect) 1.05f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "btnScale"
    )
    
    val textColor = if (isSelected && !s.mostraCorretta) Color.Black else Color.White

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + expandVertically(),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
    ) {
        Card(
            onClick = { onAnswer(risposta) },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = bgColor, 
                contentColor = textColor,
                disabledContainerColor = bgColor,
                disabledContentColor = textColor
            ),
            border = if (!isSelected) BorderStroke(1.5.dp, Color.White.copy(alpha = 0.4f)) else null,
            enabled = s.rispostaSelezionata == null
        ) {
            Text(
                text = risposta,
                modifier = Modifier.padding(18.dp).fillMaxWidth(),
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun GameOverView(s: ScalataUiState.GameOver, onRetry: () -> Unit, onExit: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val titolo = if (s.completata) "SCALATA COMPLETATA!" else "GAME OVER"
        val icona = if (s.completata) "🏆" else "💀"
        
        Text(icona, style = MaterialTheme.typography.displayLarge)
        Text(
            text = titolo,
            style = MaterialTheme.typography.displaySmall,
            fontWeight = FontWeight.Black,
            color = Color.White,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        
        if (s.completata) {
            Text("Hai risposto a tutte le curiosità che conosci!", 
                textAlign = TextAlign.Center, color = Color.White.copy(0.9f), modifier = Modifier.padding(top = 8.dp).fillMaxWidth())
        }

        Spacer(Modifier.height(32.dp))
        
        Text("PUNTEGGIO FINALE", style = MaterialTheme.typography.labelLarge, color = Color.White.copy(0.7f))
        Text("${s.punteggioFinale}", style = MaterialTheme.typography.displayLarge, fontWeight = FontWeight.Black, color = Color(0xFFFFD700))
        
        if (s.nuovoRecord) {
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFD700)), shape = RoundedCornerShape(8.dp)) {
                Text("NUOVO RECORD!", modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp), fontWeight = FontWeight.Black, color = Color.Black)
            }
        } else {
            Text("RECORD PERSONALE: ${s.recordPrecedente}", color = Color.White.copy(0.6f))
        }

        Spacer(Modifier.height(48.dp))

        Button(
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFFB71C1C)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("RIPROVA LA SCALATA", fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = onExit,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            border = BorderStroke(2.dp, Color.White),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("TORNA AL MENU")
        }
    }
}

@Composable
fun NoQuestionsView(onExit: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🏜️", style = MaterialTheme.typography.displayLarge)
        Text("NESSUNA DOMANDA", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = Color.White, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Text("Hai già letto tutte le curiosità disponibili. Torna più tardi!", 
            textAlign = TextAlign.Center, color = Color.White.copy(0.7f), modifier = Modifier.padding(16.dp).fillMaxWidth())
        
        Button(onClick = onExit, colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black)) {
            Text("INDIETRO")
        }
    }
}
