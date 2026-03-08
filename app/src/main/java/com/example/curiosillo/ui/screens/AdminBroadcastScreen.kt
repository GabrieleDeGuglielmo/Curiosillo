package com.example.curiosillo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.curiosillo.firebase.FirebaseManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// ── ViewModel ─────────────────────────────────────────────────────────────────

data class AdminBroadcastUiState(
    val isLoading:  Boolean = false,
    val isAdmin:    Boolean = false,
    val messaggio:  String? = null,
    val errore:     String? = null
)

class AdminBroadcastViewModel : ViewModel() {

    private val _state = MutableStateFlow(AdminBroadcastUiState())
    val state: StateFlow<AdminBroadcastUiState> = _state.asStateFlow()

    init { verificaAdmin() }

    private fun verificaAdmin() {
        viewModelScope.launch {
            val isAdmin = FirebaseManager.isAdmin()
            _state.value = _state.value.copy(isAdmin = isAdmin)
        }
    }

    fun inviaBroadcast(titolo: String, corpo: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, errore = null)
            val result = FirebaseManager.inviaBroadcast(titolo, corpo)
            if (result.isSuccess) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    messaggio = "📢 Messaggio inviato a tutti gli utenti!"
                )
            } else {
                _state.value = _state.value.copy(
                    isLoading = false,
                    errore = result.exceptionOrNull()?.message ?: "Errore sconosciuto"
                )
            }
        }
    }

    fun dismissMessaggio() { _state.value = _state.value.copy(messaggio = null) }
    fun dismissErrore()    { _state.value = _state.value.copy(errore = null) }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(c: Class<T>): T = AdminBroadcastViewModel() as T
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminBroadcastScreen(nav: NavController) {
    val vm: AdminBroadcastViewModel = viewModel(factory = AdminBroadcastViewModel.Factory())
    val state by vm.state.collectAsState()

    var titolo  by remember { mutableStateOf("") }
    var corpo   by remember { mutableStateOf("") }
    var tentato by remember { mutableStateOf(false) }

    val errTitolo = tentato && titolo.isBlank()
    val errCorpo  = tentato && corpo.isBlank()

    // Auto-dismiss messaggio successo
    state.messaggio?.let {
        LaunchedEffect(it) {
            kotlinx.coroutines.delay(3000)
            vm.dismissMessaggio()
        }
    }

    // Dialog errore
    state.errore?.let {
        AlertDialog(
            onDismissRequest = { vm.dismissErrore() },
            title = { Text("Errore") },
            text  = { Text(it) },
            confirmButton = { TextButton(onClick = { vm.dismissErrore() }) { Text("OK") } }
        )
    }

    val gradientBg = Brush.verticalGradient(listOf(
        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
        MaterialTheme.colorScheme.background
    ))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Comunicazioni", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Indietro")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { pad ->
        Box(Modifier.fillMaxSize().background(gradientBg).padding(pad)) {

            // Banner successo
            state.messaggio?.let { msg ->
                Surface(
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp),
                    shape    = RoundedCornerShape(20.dp),
                    color    = Color(0xFF4CAF50)
                ) {
                    Text(
                        msg,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        color    = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (!state.isAdmin) {
                Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Text(
                        "Accesso negato",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            } else {
                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    // ── Intestazione ──────────────────────────────────────────
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier          = Modifier.padding(bottom = 8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Box(
                                Modifier.size(48.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Campaign, null,
                                    Modifier.size(28.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(
                                "Broadcast",
                                style      = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Il messaggio apparirà sotto la 🔔 di tutti gli utenti",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // ── Campi ─────────────────────────────────────────────────
                    OutlinedTextField(
                        value         = titolo,
                        onValueChange = { titolo = it },
                        label         = { Text("Titolo") },
                        singleLine    = true,
                        modifier      = Modifier.fillMaxWidth(),
                        isError       = errTitolo,
                        supportingText = if (errTitolo) {
                            { Text("Il titolo è obbligatorio") }
                        } else null
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value         = corpo,
                        onValueChange = { corpo = it },
                        label         = { Text("Messaggio") },
                        minLines      = 5,
                        modifier      = Modifier.fillMaxWidth(),
                        isError       = errCorpo,
                        supportingText = if (errCorpo) {
                            { Text("Il messaggio è obbligatorio") }
                        } else null
                    )

                    Spacer(Modifier.height(8.dp))

                    // ── Anteprima ─────────────────────────────────────────────
                    if (titolo.isNotBlank() || corpo.isNotBlank()) {
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Anteprima",
                            style      = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                            modifier   = Modifier.padding(bottom = 8.dp)
                        )
                        Card(
                            modifier  = Modifier.fillMaxWidth(),
                            shape     = RoundedCornerShape(14.dp),
                            colors    = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            ),
                            elevation = CardDefaults.cardElevation(0.dp)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                if (titolo.isNotBlank()) {
                                    Text(
                                        titolo,
                                        style      = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Spacer(Modifier.height(4.dp))
                                }
                                if (corpo.isNotBlank()) {
                                    Text(
                                        corpo,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(32.dp))

                    // ── Tasto invia ───────────────────────────────────────────
                    Button(
                        onClick = {
                            tentato = true
                            if (titolo.isNotBlank() && corpo.isNotBlank()) {
                                vm.inviaBroadcast(titolo.trim(), corpo.trim())
                                titolo = ""
                                corpo  = ""
                                tentato = false
                            }
                        },
                        enabled  = !state.isLoading,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape    = RoundedCornerShape(14.dp)
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color    = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Send, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Invia a tutti gli utenti",
                                fontWeight = FontWeight.Bold,
                                style      = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                }
            }
        }
    }
}
