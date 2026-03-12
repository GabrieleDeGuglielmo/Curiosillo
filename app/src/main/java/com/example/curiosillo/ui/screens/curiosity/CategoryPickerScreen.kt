package com.example.curiosillo.ui.screens.curiosity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.curiosillo.CuriosityApplication
import com.example.curiosillo.ui.screens.utils.coloreCategoria
import com.example.curiosillo.ui.screens.utils.emojiCategoria
import com.example.curiosillo.viewmodel.CategoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryPickerScreen(nav: NavController, destinazione: String) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as CuriosityApplication
    val vm: CategoryViewModel = viewModel(
        factory = CategoryViewModel.Factory(app.repository, app.categoryPrefs)
    )
    val categorie       by vm.categorie.collectAsState()
    val categorieAttive by vm.categorieAttive.collectAsState()

    // Reset ad ogni apertura
    LaunchedEffect(Unit) { vm.resetCategorie() }

    // Statistiche completamento per categoria (lette/totali)
    val completamento = remember { mutableStateOf<Map<String, Pair<Int, Int>>>(emptyMap()) }
    LaunchedEffect(Unit) {
        val lette         = app.repository.getPilloleLette()
        val tutteImparate = app.repository.getTutteLeCuriosita()
        val tuttePerCat   = tutteImparate.groupBy { it.category }
        val letteSet      = lette.map { it.id }.toSet()
        
        completamento.value = tuttePerCat.mapValues { (_, lista) ->
            Pair(lista.count { it.id in letteSet }, lista.size)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scegli categoria") },
                navigationIcon = {
                    IconButton({ nav.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Indietro") }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { pad ->
        val gradientBg = Brush.verticalGradient(listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
            MaterialTheme.colorScheme.background
        ))
        Column(
            Modifier.fillMaxSize().background(gradientBg).padding(pad).padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            Text("Seleziona una o più categorie, oppure vai avanti per tutte.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                modifier = Modifier.padding(horizontal = 4.dp))
            Spacer(Modifier.height(16.dp))

            LazyVerticalGrid(
                columns               = GridCells.Fixed(2),
                verticalArrangement   = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier              = Modifier.weight(1f)
            ) {
                item {
                    CategoriaCard(nome = "Tutte", attiva = categorieAttive.isEmpty(),
                        colore = Color(0xFF607D8B), completamento = null,
                        emojiOverride = "🌐",
                        onClick = { vm.resetCategorie() })
                }
                items(categorie) { cat ->
                    CategoriaCard(nome = cat, attiva = categorieAttive.contains(cat),
                        colore = coloreCategoria(cat),
                        completamento = completamento.value[cat],
                        onClick = { vm.toggleCategoria(cat) })
                }
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick  = { nav.navigate(destinazione) { popUpTo("home") } },
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape    = RoundedCornerShape(16.dp)
            ) {
                val label = when {
                    categorieAttive.isEmpty() -> "Avanti — Tutte le categorie"
                    categorieAttive.size == 1 -> "Avanti — ${categorieAttive.first()}"
                    else                      -> "Avanti — ${categorieAttive.size} categorie"
                }
                Text(label, style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CategoriaCard(
    nome:          String,
    attiva:        Boolean,
    colore:        Color,
    completamento: Pair<Int, Int>?,
    emojiOverride: String? = null,
    onClick:       () -> Unit
) {
    val bgColore    = if (attiva) colore else colore.copy(alpha = 0.12f)
    val testoColore = if (attiva) Color.White else colore
    val emoji       = emojiOverride ?: emojiCategoria(nome)

    Card(
        onClick   = onClick,
        modifier  = Modifier.fillMaxWidth().height(90.dp),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = bgColore),
        elevation = CardDefaults.cardElevation(if (attiva) 6.dp else 0.dp)
    ) {
        Box(Modifier.fillMaxSize().padding(10.dp)) {
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "$emoji  $nome",
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color      = testoColore
                    )
                    if (attiva) {
                        Spacer(Modifier.width(6.dp))
                        Icon(Icons.Default.Check, null, Modifier.size(16.dp), tint = Color.White)
                    }
                }
                // Indicatore completamento
                if (completamento != null && completamento.second > 0) {
                    val (lette, totali) = completamento
                    val pct             = lette.toFloat() / totali
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress   = { pct },
                        modifier   = Modifier.fillMaxWidth(0.85f).height(4.dp).clip(CircleShape),
                        color      = if (attiva) Color.White.copy(alpha = 0.9f) else colore,
                        trackColor = if (attiva) Color.White.copy(alpha = 0.3f) else colore.copy(alpha = 0.2f)
                    )
                    Spacer(Modifier.height(2.dp))
                    val perc = (pct * 100).toInt()
                    Text(
                        "$perc%",
                        style    = MaterialTheme.typography.labelSmall,
                        color    = testoColore.copy(alpha = 0.7f),
                        fontSize = 9.sp
                    )
                }
            }
        }
    }
}
