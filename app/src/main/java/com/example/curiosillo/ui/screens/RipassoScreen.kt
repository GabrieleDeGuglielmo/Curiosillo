package com.example.curiosillo.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.EditNote
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
import com.example.curiosillo.ui.components.NotaBottomSheet
import com.example.curiosillo.ui.theme.Primary
import com.example.curiosillo.ui.theme.Secondary
import com.example.curiosillo.viewmodel.RipassoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RipassoScreen(nav: NavController) {
    val ctx  = LocalContext.current
    val app  = ctx.applicationContext as CuriosityApplication
    val vm: RipassoViewModel = viewModel(
        factory = RipassoViewModel.Factory(app.repository, app.categoryPrefs)
    )
    val state by vm.state.collectAsState()
    var mostraNota       by remember { mutableStateOf(false) }
    var mostraSelettore  by remember { mutableStateOf(false) }

    val opzioniGiorni = listOf(0 to "Tutte", 3 to "3+ giorni fa",
        7 to "7+ giorni fa", 14 to "14+ giorni fa", 30 to "30+ giorni fa")

    // Dialog selezione giorni
    if (mostraSelettore) {
        AlertDialog(
            onDismissRequest = { mostraSelettore = false },
            title = { Text("Mostra pillole lette...", fontWeight = FontWeight.Bold) },
            text  = {
                Column {
                    opzioniGiorni.forEach { (giorni, etichetta) ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = state.giorniSelezionati == giorni,
                                onClick  = { vm.carica(giorni); mostraSelettore = false }
                            )
                            Text(etichetta, Modifier.padding(start = 8.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { mostraSelettore = false }) { Text("Chiudi") }
            }
        )
    }

    // Bottom sheet nota
    val pillola = vm.pilloleCorrente()
    if (mostraNota && pillola != null) {
        NotaBottomSheet(
            notaAttuale = pillola.nota,
            onSalva     = { vm.salvaNota(it) },
            onChiudi    = { mostraNota = false }
        )
    }

    Scaffold(topBar = {
        TopAppBar(
            title = { Text("Ripasso", fontWeight = FontWeight.SemiBold) },
            navigationIcon = {
                IconButton(onClick = { nav.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, "Indietro")
                }
            },
            actions = {
                TextButton(onClick = { mostraSelettore = true }) {
                    val label = opzioniGiorni.find { it.first == state.giorniSelezionati }?.second ?: "Filtra"
                    Text(label, color = Primary, fontWeight = FontWeight.SemiBold)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
        )
    }) { pad ->
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFFE8F5E9), Color.White)))
                .padding(pad)
        ) {
            when {
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))

                state.pillole.isEmpty() -> {
                    Column(
                        Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("📚", fontSize = 56.sp)
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Nessuna pillola da ripassare\ncon il filtro attuale.",
                            style     = MaterialTheme.typography.bodyLarge,
                            color     = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(16.dp))
                        TextButton(onClick = { mostraSelettore = true }) {
                            Text("Cambia filtro", color = Primary)
                        }
                    }
                }

                else -> {
                    val cur = state.pillole[state.indiceCorrente]
                    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {

                        // Immagine categoria
                        Image(
                            painter            = painterResource(id = categoryImage(cur.category)),
                            contentDescription = cur.category,
                            modifier           = Modifier
                                .fillMaxWidth()
                                .height(180.dp)
                                .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp)),
                            contentScale = ContentScale.Crop
                        )

                        Column(Modifier.padding(horizontal = 24.dp)) {
                            Spacer(Modifier.height(16.dp))

                            // Contatore + chip categoria
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment     = Alignment.CenterVertically
                            ) {
                                SuggestionChip(
                                    onClick = {},
                                    label   = { Text(emojiCategoria(cur.category) + " " + cur.category) }
                                )
                                Text(
                                    "${state.indiceCorrente + 1} / ${state.pillole.size}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.Gray
                                )
                            }
                            Spacer(Modifier.height(10.dp))

                            Text(cur.title,
                                style      = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color      = Color(0xFF1C1B1F))
                            Spacer(Modifier.height(16.dp))

                            Card(Modifier.fillMaxWidth(),
                                shape     = RoundedCornerShape(18.dp),
                                colors    = CardDefaults.cardColors(containerColor = Color.White),
                                elevation = CardDefaults.cardElevation(4.dp)
                            ) {
                                Text(cur.body, Modifier.padding(20.dp),
                                    style      = MaterialTheme.typography.bodyLarge,
                                    lineHeight = 27.sp,
                                    color      = Color(0xFF3C3C3C))
                            }

                            // Nota esistente
                            if (cur.nota.isNotBlank()) {
                                Spacer(Modifier.height(12.dp))
                                Card(
                                    Modifier.fillMaxWidth(),
                                    shape     = RoundedCornerShape(12.dp),
                                    colors    = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0)),
                                    elevation = CardDefaults.cardElevation(0.dp)
                                ) {
                                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.Top) {
                                        Text("📝 ", fontSize = 14.sp)
                                        Text(cur.nota,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFF5D4037))
                                    }
                                }
                            }

                            Spacer(Modifier.height(24.dp))

                            // Pulsante nota
                            OutlinedButton(
                                onClick  = { mostraNota = true },
                                modifier = Modifier.fillMaxWidth().height(52.dp),
                                shape    = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.EditNote, null, Modifier.size(18.dp),
                                    tint = if (cur.nota.isNotBlank()) Primary else Color.Gray)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    if (cur.nota.isNotBlank()) "Modifica nota" else "Aggiungi nota",
                                    color = if (cur.nota.isNotBlank()) Primary else Color.Gray
                                )
                            }

                            Spacer(Modifier.height(12.dp))

                            // Navigazione precedente/prossima
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedButton(
                                    onClick  = { vm.precedente() },
                                    enabled  = state.indiceCorrente > 0,
                                    modifier = Modifier.weight(1f).height(52.dp),
                                    shape    = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(Icons.Default.ArrowBackIosNew, null, Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Precedente")
                                }
                                Button(
                                    onClick  = { vm.prossima() },
                                    enabled  = state.indiceCorrente < state.pillole.size - 1,
                                    modifier = Modifier.weight(1f).height(52.dp),
                                    shape    = RoundedCornerShape(14.dp),
                                    colors   = ButtonDefaults.buttonColors(containerColor = Secondary)
                                ) {
                                    Text("Prossima")
                                    Spacer(Modifier.width(6.dp))
                                    Icon(Icons.Default.ArrowForwardIos, null, Modifier.size(16.dp))
                                }
                            }
                            Spacer(Modifier.height(24.dp))
                        }
                    }
                }
            }
        }
    }
}
