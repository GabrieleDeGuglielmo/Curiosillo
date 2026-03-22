package com.example.curiosillo.ui.screens.utils

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.curiosillo.firebase.FirebaseManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

// ── Shared UI State for Reports ──────────────────────────────────────────────

sealed class SegnalazioneUiState {
    object Idle    : SegnalazioneUiState()
    object Loading : SegnalazioneUiState()
    object Successo: SegnalazioneUiState()
    data class Errore(val msg: String) : SegnalazioneUiState()
}

// ── Shared Helper Logic for ViewModels ───────────────────────────────────────

object SegnalazioneHelper {

    // Sends the report to Firebase and updates the state flow automatically
    fun invia(
        scope: CoroutineScope,
        stateFlow: MutableStateFlow<SegnalazioneUiState>,
        externalId: String,
        tipo: String,
        testo: String
    ) {
        scope.launch {
            stateFlow.value = SegnalazioneUiState.Loading
            val result = FirebaseManager.inviaSegnalazione(externalId, tipo, testo)
            stateFlow.value = if (result.isSuccess) {
                SegnalazioneUiState.Successo
            } else {
                SegnalazioneUiState.Errore(result.exceptionOrNull()?.message ?: "Errore")
            }
        }
    }

    // Resets the state back to Idle
    fun dismiss(stateFlow: MutableStateFlow<SegnalazioneUiState>) {
        stateFlow.value = SegnalazioneUiState.Idle
    }
}

// ── Tipi di segnalazione ──────────────────────────────────────────────────────

enum class TipoSegnalazione(val etichetta: String, val emoji: String, val descrizione: String) {
    ERRORE_DIGITAZIONE("Errore di digitazione",        "✏️", "Parola sbagliata, punteggiatura, spazi ecc."),
    INFO_FALSE(        "Informazioni false o imprecise","⚠️", "Il contenuto non è accurato o è fuorviante"),
    CATEGORIA_ERRATA("Categoria errata",           "📅", "La categoria assegnata alla curiosità è errata"),
    CURIOSITA_RIPETUTA("Curiosità ripetuta", "🔂", "Questa curiosità è ripetuta più volte"),
    ALTRO(             "Altro",                        "💬", "Qualcosa che non rientra nelle categorie precedenti")
}

// ── Bottom sheet segnalazione (riutilizzabile in tutti gli screen) ─────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SegnalazioneBottomSheet(
    onInvia:   (tipo: String, testo: String) -> Unit,
    onDismiss: () -> Unit,
    isLoading: Boolean = false
) {
    // skipPartiallyExpanded = true permette al foglio di aprirsi tutto subito, 
    // fondamentale quando c'è un campo di testo per evitare conflitti con la tastiera.
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var tipoScelto by remember { mutableStateOf<TipoSegnalazione?>(null) }
    var testo      by remember { mutableStateOf("") }
    var tentato    by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState,
        dragHandle       = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                // imePadding() sposta il contenuto verso l'alto quando appare la tastiera
                .imePadding()
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("🚩", fontSize = 22.sp)
                Spacer(Modifier.width(10.dp))
                Column {
                    Text(
                        "Segnala curiosità",
                        style      = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Aiutaci a migliorare i contenuti",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            Text(
                "Tipo di segnalazione",
                style      = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                modifier   = Modifier.padding(bottom = 10.dp)
            )

            if (tentato && tipoScelto == null) {
                Text(
                    "Seleziona un tipo di segnalazione",
                    style    = MaterialTheme.typography.labelSmall,
                    color    = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            TipoSegnalazione.values().forEach { tipo ->
                val selezionato = tipoScelto == tipo
                Surface(
                    onClick   = { tipoScelto = tipo },
                    modifier  = Modifier.fillMaxWidth().padding(bottom = 6.dp),
                    shape     = RoundedCornerShape(12.dp),
                    color     = if (selezionato)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    border    = if (selezionato)
                        BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                    else null
                ) {
                    Row(
                        Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(tipo.emoji, fontSize = 18.sp)
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                tipo.etichetta,
                                style      = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (selezionato) FontWeight.SemiBold else FontWeight.Normal
                            )
                            Text(
                                tipo.descrizione,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                            )
                        }
                        if (selezionato) {
                            Icon(
                                Icons.Default.CheckCircle, null,
                                Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            OutlinedTextField(
                value         = testo,
                onValueChange = { testo = it },
                label         = { Text("Descrizione (opzionale)") },
                placeholder   = { Text("Descrivi il problema in modo più dettagliato…") },
                minLines      = 3,
                modifier      = Modifier.fillMaxWidth(),
                shape         = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = {
                    tentato = true
                    val t = tipoScelto ?: return@Button
                    onInvia(t.etichetta, testo.trim())
                },
                enabled  = !isLoading,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape    = RoundedCornerShape(14.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Default.Send, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Invia segnalazione", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
