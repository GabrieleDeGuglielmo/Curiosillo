package com.example.curiosillo.ui.screens

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Share
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
import com.example.curiosillo.data.BadgeSbloccato
import com.example.curiosillo.ui.categoryImage
import com.example.curiosillo.ui.components.NotaBottomSheet
import com.example.curiosillo.ui.theme.Success
import com.example.curiosillo.viewmodel.CuriosityUiState
import com.example.curiosillo.viewmodel.CuriosityViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CuriosityScreen(nav: NavController) {
    val ctx   = LocalContext.current
    val app   = ctx.applicationContext as CuriosityApplication
    val vm: CuriosityViewModel = viewModel(
        factory = CuriosityViewModel.Factory(app.repository, app.categoryPrefs, app.gamificationEngine)
    )
    val state    by vm.state.collectAsState()
    val risultato by vm.risultatoAzione.collectAsState()

    var badgeDaMostrare by remember { mutableStateOf<BadgeSbloccato?>(null) }
    var badgeQueue      by remember { mutableStateOf<List<BadgeSbloccato>>(emptyList()) }

    LaunchedEffect(risultato) {
        risultato?.let {
            if (it.badgeSbloccati.isNotEmpty()) {
                badgeQueue      = it.badgeSbloccati
                badgeDaMostrare = badgeQueue.first()
            }
            vm.consumaRisultato()
        }
    }

    badgeDaMostrare?.let { badge ->
        AlertDialog(
            onDismissRequest = {
                val r = badgeQueue.drop(1); badgeQueue = r; badgeDaMostrare = r.firstOrNull()
            },
            icon  = { Text(badge.icona, fontSize = 48.sp) },
            title = {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("Badge sbloccato!", fontWeight = FontWeight.Bold)
                }
            },
            text  = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(badge.nome, style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Text(badge.descrizione, textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            },
            confirmButton = {
                Button(onClick = {
                    val r = badgeQueue.drop(1); badgeQueue = r; badgeDaMostrare = r.firstOrNull()
                }) { Text("Ottimo!", fontWeight = FontWeight.Bold) }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pillola", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton({ nav.popBackStack() }) { Icon(Icons.Default.ArrowBack, "Indietro") }
                },
                actions = {
                    if (state is CuriosityUiState.Success) {
                        val pillola = (state as CuriosityUiState.Success).curiosity
                        IconButton(onClick = {
                            val testo = buildString {
                                append("📚 ${pillola.title}\n\n")
                                append(pillola.body)
                                append("\n\n— Categoria: ${pillola.category}")
                                append("\nScoperto con Curiosillo 🎓")
                            }
                            ctx.startActivity(Intent.createChooser(
                                Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, testo)
                                }, "Condividi pillola"))
                        }) {
                            Icon(Icons.Default.Share, "Condividi",
                                tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent
    ) { pad ->
        val gradientBg = Brush.verticalGradient(listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
            MaterialTheme.colorScheme.background
        ))
        when (val s = state) {
            is CuriosityUiState.Loading ->
                Box(Modifier.fillMaxSize().background(gradientBg).padding(pad), Alignment.Center) {
                    CircularProgressIndicator()
                }
            is CuriosityUiState.Empty ->
                Box(Modifier.fillMaxSize().background(gradientBg).padding(pad), Alignment.Center) {
                    Text("Nessuna curiosità disponibile!\nTorna presto.",
                        Modifier.padding(24.dp), textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge)
                }
            is CuriosityUiState.Success ->
                CuriosityContent(s, pad, gradientBg,
                    onLearn     = { vm.markLearned() },
                    onBookmark  = { vm.toggleBookmark() },
                    onSalvaNota = { vm.salvaNota(it) }
                )
            is CuriosityUiState.Learned -> LearnedContent(pad, gradientBg) { vm.load() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CuriosityContent(
    s:           CuriosityUiState.Success,
    pad:         androidx.compose.foundation.layout.PaddingValues,
    gradientBg:  androidx.compose.ui.graphics.Brush,
    onLearn:     () -> Unit,
    onBookmark:  () -> Unit,
    onSalvaNota: (String) -> Unit
) {
    var mostraNota by remember { mutableStateOf(false) }
    val noteCardBg = MaterialTheme.colorScheme.surfaceVariant
    val noteTextColor = MaterialTheme.colorScheme.onSurfaceVariant

    if (mostraNota) {
        NotaBottomSheet(
            notaAttuale = s.curiosity.nota,
            onSalva     = onSalvaNota,
            onChiudi    = { mostraNota = false }
        )
    }

    Column(Modifier.fillMaxSize().background(gradientBg).padding(pad).verticalScroll(rememberScrollState())) {
        Image(
            painter            = painterResource(id = categoryImage(s.curiosity.category)),
            contentDescription = s.curiosity.category,
            modifier           = Modifier
                .fillMaxWidth().height(220.dp)
                .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)),
            contentScale = ContentScale.Crop
        )
        Column(Modifier.padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(16.dp))
            Row(
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier              = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
            ) {
                SuggestionChip(onClick = {},
                    label = { Text(emojiCategoria(s.curiosity.category) + " " + s.curiosity.category) })
                Row {
                    IconButton(onClick = { mostraNota = true }) {
                        Icon(Icons.Default.EditNote, "Nota",
                            tint = if (s.curiosity.nota.isNotBlank())
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                    }
                    IconToggleButton(checked = s.curiosity.isBookmarked,
                        onCheckedChange = { onBookmark() }) {
                        Icon(
                            imageVector = if (s.curiosity.isBookmarked)
                                Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Salva",
                            tint = if (s.curiosity.isBookmarked)
                                MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                    }
                }
            }

            if (s.curiosity.nota.isNotBlank()) {
                Card(Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    shape     = RoundedCornerShape(12.dp),
                    colors    = CardDefaults.cardColors(containerColor = noteCardBg),
                    elevation = CardDefaults.cardElevation(0.dp)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                        Text("📝 ", fontSize = 14.sp)
                        Text(s.curiosity.nota, style = MaterialTheme.typography.bodySmall,
                            color = noteTextColor)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Text(s.curiosity.title,
                style      = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign  = TextAlign.Center,
                color      = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(20.dp))

            Card(Modifier.fillMaxWidth(),
                shape     = RoundedCornerShape(18.dp),
                colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Text(s.curiosity.body, Modifier.padding(20.dp),
                    style      = MaterialTheme.typography.bodyLarge,
                    lineHeight = 27.sp,
                    color      = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.height(16.dp))
            Text("Curiosità imparate: ${s.readCount}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
            Spacer(Modifier.height(24.dp))

            if (!s.curiosity.isRead) {
                Button(onLearn, Modifier.fillMaxWidth().height(58.dp),
                    shape = RoundedCornerShape(16.dp)
                ) { Text("Ho imparato!", style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold) }
            } else {
                OutlinedButton(onLearn, Modifier.fillMaxWidth().height(58.dp),
                    shape = RoundedCornerShape(16.dp)
                ) { Text("Prossima curiosità", style = MaterialTheme.typography.titleMedium) }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun LearnedContent(
    pad:        androidx.compose.foundation.layout.PaddingValues,
    gradientBg: androidx.compose.ui.graphics.Brush,
    onNext:     () -> Unit
) {
    Column(
        Modifier.fillMaxSize().background(gradientBg).padding(pad).padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.CheckCircle, null, Modifier.size(90.dp), tint = Success)
        Spacer(Modifier.height(20.dp))
        Text("Fantastico!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Text("Hai imparato qualcosa di nuovo!\nOra puoi fare il quiz su questa curiosità.",
            style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        Spacer(Modifier.height(32.dp))
        Button(onNext, Modifier.fillMaxWidth().height(58.dp), shape = RoundedCornerShape(16.dp)) {
            Text("Prossima curiosità", style = MaterialTheme.typography.titleMedium)
        }
    }
}

fun emojiCategoria(category: String): String = when (category) {
    "Scienza"    -> "🔬"
    "Animali"    -> "🐾"
    "Storia"     -> "📜"
    "Sport"      -> "⚽"
    "Arte"       -> "🎨"
    "Tecnologia" -> "💻"
    "Natura"     -> "🌿"
    "Cibo"       -> "🍽️"
    else         -> "✨"
}
