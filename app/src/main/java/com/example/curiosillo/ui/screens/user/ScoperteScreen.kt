package com.example.curiosillo.ui.screens.user

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.curiosillo.CuriosityApplication
import com.example.curiosillo.data.Scoperta
import com.example.curiosillo.ui.screens.utils.coloreCategoria
import com.example.curiosillo.ui.screens.utils.emojiCategoria
import com.example.curiosillo.viewmodel.ScoperteViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScoperteScreen(nav: NavController) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as CuriosityApplication
    val vm: ScoperteViewModel = viewModel(factory = ScoperteViewModel.Factory(app.repository))
    val state by vm.uiState.collectAsState()

    var scopertaDettaglio by remember { mutableStateOf<Scoperta?>(null) }

    if (scopertaDettaglio != null) {
        ModalBottomSheet(
            onDismissRequest = { scopertaDettaglio = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            DettaglioScopertaSheet(scopertaDettaglio!!)
        }
    }

    val gradientBg = Brush.verticalGradient(
        listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
            MaterialTheme.colorScheme.background
        )
    )
    Box(Modifier.fillMaxSize().background(gradientBg)) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Le mie scoperte") },
                    navigationIcon = {
                        IconButton(onClick = {
                            if (nav.currentBackStackEntry?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
                                nav.popBackStack()
                            }
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Indietro")
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
                    .padding(pad)
            ) {
                if (state.isLoading && state.scoperte.isEmpty()) {
                    CircularProgressIndicator(Modifier.align(Alignment.Center))
                } else if (state.scoperte.isEmpty()) {
                    Column(
                        Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Default.AutoAwesome,
                            null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Ancora nessuna scoperta!",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                        Text(
                            "Inquadra qualcosa con la fotocamera AR\nper aggiungerlo alla tua collezione.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
                            modifier = Modifier.padding(horizontal = 32.dp, vertical = 8.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(state.scoperte) { scoperta ->
                            ScopertaCard(scoperta) { scopertaDettaglio = scoperta }
                        }
                    }
                }
            }
        }
    } // Box gradient
}

@Composable
fun ScopertaCard(scoperta: Scoperta, onClick: () -> Unit) {
    val dateStr = remember(scoperta.dataScoperta) {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.ITALY)
        sdf.format(Date(scoperta.dataScoperta))
    }

    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .size(48.dp)
                    .background(
                        coloreCategoria(scoperta.categoria).copy(alpha = 0.1f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(emojiCategoria(scoperta.categoria), fontSize = 24.sp)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    scoperta.titolo,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    scoperta.categoria,
                    style = MaterialTheme.typography.labelMedium,
                    color = coloreCategoria(scoperta.categoria)
                )
            }
            Text(
                dateStr,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }
    }
}

@Composable
fun DettaglioScopertaSheet(scoperta: Scoperta) {
    val dateStr = remember(scoperta.dataScoperta) {
        val sdf = SimpleDateFormat("EEEE dd MMMM yyyy, HH:mm", Locale.ITALY)
        sdf.format(Date(scoperta.dataScoperta))
    }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .padding(bottom = 32.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Surface(
                color = coloreCategoria(scoperta.categoria).copy(alpha = 0.1f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "${emojiCategoria(scoperta.categoria)} ${scoperta.categoria}",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = coloreCategoria(scoperta.categoria),
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                dateStr,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = scoperta.titolo,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold
        )

        Spacer(Modifier.height(12.dp))

        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Text(
                text = scoperta.descrizione,
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 26.sp
            )
        }
    }
}