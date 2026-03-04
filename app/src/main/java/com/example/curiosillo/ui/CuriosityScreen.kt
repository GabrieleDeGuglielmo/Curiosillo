package com.example.curiosillo.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
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
import com.example.curiosillo.ui.theme.Primary
import com.example.curiosillo.ui.theme.Success
import com.example.curiosillo.viewmodel.CuriosityUiState
import com.example.curiosillo.viewmodel.CuriosityViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CuriosityScreen(nav: NavController) {
    val ctx  = LocalContext.current
    val repo = (ctx.applicationContext as CuriosityApplication).repository
    val vm: CuriosityViewModel = viewModel(factory = CuriosityViewModel.Factory(repo))
    val state by vm.state.collectAsState()

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Curiosita del Giorno", fontWeight = FontWeight.SemiBold) },
            navigationIcon = { IconButton({ nav.popBackStack() }) {
                Icon(Icons.Default.ArrowBack, "Indietro") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent))
    }) { pad ->
        Box(Modifier.fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFFFF3E0), Color.White)))
            .padding(pad)) {
            when (val s = state) {
                is CuriosityUiState.Loading ->
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                is CuriosityUiState.Empty ->
                    Text("Nessuna curiosita disponibile!\nTorna presto.",
                        Modifier.align(Alignment.Center).padding(24.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge)
                is CuriosityUiState.Success -> CuriosityContent(s) { vm.markLearned() }
                is CuriosityUiState.Learned -> LearnedContent { vm.load() }
            }
        }
    }
}

@Composable
private fun CuriosityContent(s: CuriosityUiState.Success, onLearn: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {

        // Immagine categoria — full width, nessun padding laterale
        Image(
            painter = painterResource(id = categoryImage(s.curiosity.category)),
            contentDescription = s.curiosity.category,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)),
            contentScale = ContentScale.Crop
        )

        Column(Modifier.padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(16.dp))

            // Badge categoria
            SuggestionChip({}, { Text(s.curiosity.category + "  " + s.curiosity.emoji) })
            Spacer(Modifier.height(10.dp))

            Text(s.curiosity.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = Color(0xFF1C1B1F))
            Spacer(Modifier.height(20.dp))

            Card(Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(4.dp)) {
                Text(s.curiosity.body, Modifier.padding(20.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = 27.sp, color = Color(0xFF3C3C3C))
            }
            Spacer(Modifier.height(16.dp))
            Text("Curiosità imparate: ${s.readCount}",
                style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            Spacer(Modifier.height(24.dp))

            if (!s.curiosity.isRead) {
                Button(onLearn, Modifier.fillMaxWidth().height(58.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Primary)) {
                    Text("Ho imparato!", style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold)
                }
            } else {
                OutlinedButton(onLearn, Modifier.fillMaxWidth().height(58.dp),
                    shape = RoundedCornerShape(16.dp)) {
                    Text("Prossima curiosità", style = MaterialTheme.typography.titleMedium)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun LearnedContent(onNext: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center) {
        Icon(Icons.Default.CheckCircle, null, Modifier.size(90.dp), tint = Success)
        Spacer(Modifier.height(20.dp))
        Text("Fantastico!", style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Text("Hai imparato qualcosa di nuovo!\nOra puoi fare il quiz su questa curiosità.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center, color = Color.Gray)
        Spacer(Modifier.height(32.dp))
        Button(onNext, Modifier.fillMaxWidth().height(58.dp),
            shape = RoundedCornerShape(16.dp)) {
            Text("Prossima curiosità", style = MaterialTheme.typography.titleMedium)
        }
    }
}
