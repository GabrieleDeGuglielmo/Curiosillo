package com.example.curiosillo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.curiosillo.CuriosityApplication
import com.example.curiosillo.ui.theme.Primary
import com.example.curiosillo.ui.theme.Secondary
import com.example.curiosillo.viewmodel.CategoryViewModel

// destination: "category_picker/curiosity" oppure "category_picker/quiz"
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryPickerScreen(nav: NavController, destinazione: String) {
    val ctx       = LocalContext.current
    val app       = ctx.applicationContext as CuriosityApplication
    val vm: CategoryViewModel = viewModel(
        factory = CategoryViewModel.Factory(app.repository, app.categoryPrefs)
    )
    val categorie       by vm.categorie.collectAsState()
    val categorieAttive by vm.categorieAttive.collectAsState()

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Scegli categoria", fontWeight = FontWeight.SemiBold) },
            navigationIcon = { IconButton({ nav.popBackStack() }) {
                Icon(Icons.Default.ArrowBack, "Indietro") } },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )
    }) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFFFFF3E0), Color.White)))
                .padding(pad)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            Text("La selezione viene ricordata per le sessioni future.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 4.dp))
            Spacer(Modifier.height(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                verticalArrangement   = Arrangement.spacedBy(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Prima card: "Tutte"
                item {
                    CategoriaCard(
                        nome    = "Tutte",
                        attiva  = categorieAttive.isEmpty(),
                        colore  = Color(0xFF607D8B),
                        onClick = { vm.resetCategorie() }
                    )
                }
                items(categorie) { cat ->
                    CategoriaCard(
                        nome    = cat,
                        attiva  = categorieAttive.contains(cat),
                        colore  = coloreCategoria(cat),
                        onClick = { vm.toggleCategoria(cat) }
                    )
                }
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick  = { nav.navigate(destinazione) { popUpTo("home") } },
                modifier = Modifier.fillMaxWidth().height(58.dp),
                shape    = RoundedCornerShape(16.dp)
            ) {
                val label = when {
                    categorieAttive.isEmpty()    -> "Avanti — Tutte le categorie"
                    categorieAttive.size == 1    -> "Avanti — ${categorieAttive.first()}"
                    else                         -> "Avanti — ${categorieAttive.size} categorie"
                }
                Text(label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun CategoriaCard(nome: String, attiva: Boolean, colore: Color, onClick: () -> Unit) {
    Card(
        onClick   = onClick,
        modifier  = Modifier.fillMaxWidth().height(80.dp),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(
            containerColor = if (attiva) colore else colore.copy(alpha = 0.15f)
        ),
        elevation = CardDefaults.cardElevation(if (attiva) 6.dp else 2.dp)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Row(verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center) {
                Text(nome,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (attiva) Color.White else colore)
                if (attiva) {
                    Spacer(Modifier.width(6.dp))
                    Icon(Icons.Default.Check, null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.White)
                }
            }
        }
    }
}

// Colore associato a ogni categoria — aggiungine quante vuoi
fun coloreCategoria(categoria: String): Color = when (categoria.lowercase()) {
    "scienza"     -> Color(0xFF1565C0)
    "animali"     -> Color(0xFF2E7D32)
    "storia"      -> Color(0xFF6A1B9A)
    "sport"       -> Color(0xFFE65100)
    "arte"        -> Color(0xFFAD1457)
    "tecnologia"  -> Color(0xFF00838F)
    "natura"      -> Color(0xFF558B2F)
    "cibo"        -> Color(0xFFEF6C00)
    "geografia"   -> Color(0xFF00695C)
    "musica"      -> Color(0xFF4527A0)
    "cinema"      -> Color(0xFF283593)
    "letteratura" -> Color(0xFF4E342E)
    else          -> Color(0xFF455A64)
}