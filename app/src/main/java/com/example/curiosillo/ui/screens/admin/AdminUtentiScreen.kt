package com.example.curiosillo.ui.screens.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.curiosillo.firebase.FirebaseManager
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.QueryDocumentSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// ── Data ──────────────────────────────────────────────────────────────────────

data class UtenteAdmin(
    val uid:             String,
    val username:        String,
    val email:           String,
    val isBannato:       Boolean,
    val banMotivazione:  String?,
    val commentiRimossi: Int,
    val isAdmin:         Boolean
)

data class AdminUtentiUiState(
    val isLoading: Boolean           = true,
    val isAdmin:   Boolean           = false,
    val utenti:    List<UtenteAdmin> = emptyList()
)

class AdminUtentiViewModel : ViewModel() {

    private val _state = MutableStateFlow(AdminUtentiUiState())
    val state: StateFlow<AdminUtentiUiState> = _state.asStateFlow()

    init { carica() }

    fun carica() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            if (!FirebaseManager.isAdmin()) {
                _state.value = AdminUtentiUiState(isLoading = false, isAdmin = false)
                return@launch
            }

            try {
                val snap = FirebaseManager.db.collection("users").get().await()
                val lista = snap.documents.map { doc ->
                    UtenteAdmin(
                        uid             = doc.id,
                        username        = doc.getString("username") ?: "Anonimo",
                        email           = doc.getString("email") ?: "",
                        isBannato       = doc.getBoolean("ban") ?: false,
                        banMotivazione  = doc.getString("banMotivazione"),
                        commentiRimossi = doc.getLong("commentiRimossi")?.toInt() ?: 0,
                        isAdmin         = doc.getBoolean("isAdmin") ?: false
                    )
                }.sortedBy { it.username.lowercase() }

                _state.value = AdminUtentiUiState(isLoading = false, isAdmin = true, utenti = lista)
            } catch (e: Exception) {
                _state.value = AdminUtentiUiState(isLoading = false, isAdmin = true)
            }
        }
    }

    fun impostaBan(uid: String, ban: Boolean, motivazione: String? = null) {
        viewModelScope.launch {
            try {
                val dati = mutableMapOf<String, Any>("ban" to ban)
                if (ban) {
                    dati["banMotivazione"] = motivazione ?: "Violazione dei termini di servizio"
                } else {
                    dati["banMotivazione"] = FieldValue.delete()
                }

                FirebaseManager.db.collection("users").document(uid).update(dati).await()
                
                // Aggiorna stato locale
                _state.value = _state.value.copy(
                    utenti = _state.value.utenti.map {
                        if (it.uid == uid) it.copy(isBannato = ban, banMotivazione = motivazione)
                        else it
                    }
                )
            } catch (_: Exception) {}
        }
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUtentiScreen(nav: NavController) {
    val vm: AdminUtentiViewModel = viewModel()
    val state by vm.state.collectAsState()

    var query by remember { mutableStateOf("") }
    var utentePerBan by remember { mutableStateOf<UtenteAdmin?>(null) }

    val gradientBg = Brush.verticalGradient(listOf(
        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
        MaterialTheme.colorScheme.background
    ))

    // Dialog per motivazione ban
    utentePerBan?.let { utente ->
        var motivazione by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { utentePerBan = null },
            title = { Text(if (utente.isBannato) "Sblocca Utente" else "Banna Utente") },
            text = {
                Column {
                    Text("Stai per ${if (utente.isBannato) "riattivare" else "sospendere"} l'accesso per ${utente.username}.")
                    if (!utente.isBannato) {
                        Spacer(Modifier.height(16.dp))
                        OutlinedTextField(
                            value = motivazione,
                            onValueChange = { motivazione = it },
                            label = { Text("Motivazione ban") },
                            placeholder = { Text("es. Linguaggio inappropriato") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        vm.impostaBan(utente.uid, !utente.isBannato, motivazione.takeIf { it.isNotBlank() })
                        utentePerBan = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (utente.isBannato) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                    )
                ) {
                    Text(if (utente.isBannato) "Sblocca" else "Conferma Ban")
                }
            },
            dismissButton = {
                TextButton(onClick = { utentePerBan = null }) { Text("Annulla") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestione Utenti") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Indietro")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { pad ->
        Box(Modifier.fillMaxSize().background(gradientBg).padding(pad)) {
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                !state.isAdmin -> AccessoNegatoContent()
                else -> {
                    val filtrate = remember(state.utenti, query) {
                        if (query.isBlank()) state.utenti
                        else {
                            val q = query.trim().lowercase()
                            state.utenti.filter { 
                                it.username.lowercase().contains(q) || 
                                it.email.lowercase().contains(q) ||
                                it.uid.lowercase().contains(q)
                            }
                        }
                    }

                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            OutlinedTextField(
                                value = query,
                                onValueChange = { query = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Cerca per username, email o UID…") },
                                leadingIcon = { Icon(Icons.Default.Search, null) },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "${state.utenti.size} utenti totali (${state.utenti.count { it.isBannato }} bannati)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                        }

                        items(filtrate, key = { it.uid }) { utente ->
                            UtenteAdminCard(utente) { utentePerBan = utente }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun UtenteAdminCard(utente: UtenteAdmin, onAction: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (utente.isBannato) 
                MaterialTheme.colorScheme.errorContainer.copy(0.3f)
            else MaterialTheme.colorScheme.surfaceVariant.copy(0.4f)
        )
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        utente.username, 
                        style = MaterialTheme.typography.titleMedium, 
                        fontWeight = FontWeight.Bold,
                        maxLines = 1, 
                        overflow = TextOverflow.Ellipsis
                    )
                    if (utente.isAdmin) {
                        Spacer(Modifier.width(8.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "ADMIN", 
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Text(utente.email, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(0.6f))
                
                if (utente.commentiRimossi > 0) {
                    Text(
                        "Commenti rimossi: ${utente.commentiRimossi}", 
                        style = MaterialTheme.typography.labelSmall,
                        color = if (utente.commentiRimossi >= 3) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                    )
                }

                if (utente.isBannato && !utente.banMotivazione.isNullOrBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Motivo: ${utente.banMotivazione}", 
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            IconButton(
                onClick = onAction,
                modifier = Modifier.clip(CircleShape).background(
                    if (utente.isBannato) Color(0xFF4CAF50).copy(0.1f) else MaterialTheme.colorScheme.error.copy(0.1f)
                )
            ) {
                Icon(
                    imageVector = if (utente.isBannato) Icons.Default.CheckCircle else Icons.Default.Block,
                    contentDescription = if (utente.isBannato) "Sblocca" else "Banna",
                    tint = if (utente.isBannato) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
