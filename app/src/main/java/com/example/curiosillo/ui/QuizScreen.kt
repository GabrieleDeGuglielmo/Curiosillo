package com.example.curiosillo.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.curiosillo.CuriosityApplication
import com.example.curiosillo.ui.theme.Error
import com.example.curiosillo.ui.theme.Secondary
import com.example.curiosillo.ui.theme.Success
import com.example.curiosillo.ui.theme.Tertiary
import com.example.curiosillo.viewmodel.QuizUiState
import com.example.curiosillo.viewmodel.QuizViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuizScreen(nav: NavController) {
    val ctx = LocalContext.current
    val repo = (ctx.applicationContext as CuriosityApplication).repository
    val vm: QuizViewModel = viewModel(factory = QuizViewModel.Factory(repo))
    val state by vm.state.collectAsState()

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Quiz", fontWeight = FontWeight.SemiBold) },
            navigationIcon = {
                IconButton({ nav.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, "Indietro")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )
    }) { pad ->
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFFE8F4FD), Color.White)))
                .padding(pad)
        ) {
            when (val s = state) {
                is QuizUiState.Loading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                is QuizUiState.NoQuestions -> NoQuestionsContent { nav.popBackStack() }
                is QuizUiState.Question -> QuestionContent(s, vm::answer)
                is QuizUiState.Answered -> AnsweredContent(s, vm::next)
                is QuizUiState.Summary -> SummaryContent(s, vm::startQuiz) { nav.popBackStack() }
            }
        }
    }
}

@Composable
private fun NoQuestionsContent(onBack: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "Nessun quiz disponibile",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text("Leggi prima alcune curiosità e premi\n\"Ho imparato!\" per sbloccare il quiz.",
            textAlign = TextAlign.Center, color = Color.Gray
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onBack, Modifier
                .fillMaxWidth()
                .height(58.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("<- Torna al Menu", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun QuestionContent(s: QuizUiState.Question, onAnswer: (String) -> Unit) {
    Column(Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())) {

        // Immagine categoria — stessa logica di CuriosityScreen
        Image(
            painter = painterResource(id = categoryImage(s.question.category)),
            contentDescription = s.question.category,
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)),
            contentScale = ContentScale.Crop
        )

        Column(
            Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LinearProgressIndicator(
                progress = { s.current.toFloat() / s.total },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp), color = Secondary
            )
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "Domanda ${s.current} di ${s.total}",
                    style = MaterialTheme.typography.bodySmall, color = Color.Gray
                )
                Text(
                    "Punti: ${s.score}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            Spacer(Modifier.height(20.dp))

            Card(
                Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Secondary),
                elevation = CardDefaults.cardElevation(6.dp)
            ) {
                Text(
                    s.question.questionText, Modifier.padding(24.dp),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White, textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(24.dp))
            s.question.answers.forEach { answer ->
                AnswerButton(answer, AnswerState.Idle) { onAnswer(answer) }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun AnsweredContent(s: QuizUiState.Answered, onNext: () -> Unit) {
    Column(Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())) {

        Image(
            painter = painterResource(id = categoryImage(s.question.category)),
            contentDescription = s.question.category,
            modifier = Modifier
                .fillMaxWidth()
                .height(160.dp)
                .clip(RoundedCornerShape(bottomStart = 20.dp, bottomEnd = 20.dp)),
            contentScale = ContentScale.Crop
        )

        Column(
            Modifier.padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LinearProgressIndicator(
                progress = { s.current.toFloat() / s.total },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp), color = Secondary
            )
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "Domanda ${s.current} di ${s.total}",
                    style = MaterialTheme.typography.bodySmall, color = Color.Gray
                )
                Text(
                    "Punti: ${s.score}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            Spacer(Modifier.height(16.dp))

            Card(
                Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (s.isCorrect) Success else Error
                )
            ) {
                Text(
                    if (s.isCorrect) "Corretto!" else "Risposta errata",
                    Modifier
                        .padding(16.dp)
                        .fillMaxWidth(),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold, color = Color.White,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(Modifier.height(14.dp))

            s.question.answers.forEach { answer ->
                val st = when {
                    answer == s.question.correctAnswer -> AnswerState.Correct
                    answer == s.selectedAnswer && !s.isCorrect -> AnswerState.Wrong
                    else -> AnswerState.Idle
                }
                AnswerButton(answer, st) {}
                Spacer(Modifier.height(10.dp))
            }
            if (s.question.explanation.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Card(
                    Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1))
                ) {
                    Text(
                        "Sai che... ${s.question.explanation}", Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyMedium, color = Color(0xFF5D4037)
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            Button(
                onNext,
                Modifier
                    .fillMaxWidth()
                    .height(58.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    "Prossima domanda", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun SummaryContent(s: QuizUiState.Summary, onRestart: () -> Unit, onHome: () -> Unit) {
    val pct = if (s.total > 0) s.score * 100 / s.total else 0
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.EmojiEvents, null, Modifier.size(80.dp), tint = Tertiary)
        Spacer(Modifier.height(16.dp))
        Text(
            "Quiz completato!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))
        Card(
            Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Secondary),
            elevation = CardDefaults.cardElevation(6.dp)
        ) {
            Column(
                Modifier
                    .padding(28.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "${s.score} / ${s.total}", fontSize = 56.sp,
                    fontWeight = FontWeight.ExtraBold, color = Color.White
                )
                Text(
                    "$pct% di risposte corrette",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
        }
        Spacer(Modifier.height(32.dp))
        Button(
            onRestart,
            Modifier
                .fillMaxWidth()
                .height(58.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                "Ricomincia Quiz", style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.height(14.dp))
        OutlinedButton(
            onHome,
            Modifier
                .fillMaxWidth()
                .height(58.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text("<- Torna al Menu", style = MaterialTheme.typography.titleMedium)
        }
    }
}

private enum class AnswerState { Idle, Correct, Wrong }

@Composable
private fun AnswerButton(text: String, state: AnswerState, onClick: () -> Unit) {
    val bg by animateColorAsState(
        when (state) {
            AnswerState.Idle -> Color.White
            AnswerState.Correct -> Success
            AnswerState.Wrong -> Error
        }, animationSpec = tween(300), label = "bg"
    )
    val fg = if (state == AnswerState.Idle) Color(0xFF1C1B1F) else Color.White
    Button(
        onClick, enabled = state == AnswerState.Idle,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = bg, disabledContainerColor = bg,
            contentColor = fg, disabledContentColor = fg
        ),
        elevation = ButtonDefaults.buttonElevation(2.dp, disabledElevation = 2.dp)
    ) {
        Text(
            text, style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium, textAlign = TextAlign.Center
        )
    }
}
