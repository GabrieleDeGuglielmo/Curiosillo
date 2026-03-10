package com.example.curiosillo.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
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
import org.json.JSONArray

// ── ViewModel ─────────────────────────────────────────────────────────────────

data class AdminCuriositaUiState(
    val isLoading:   Boolean                              = true,
    val isAdmin:     Boolean                              = false,
    val curiosita:   List<FirebaseManager.CuriositaRemota> = emptyList(),
    val messaggio:   String?                              = null,
    val errore:      String?                              = null
)

class AdminCuriositaViewModel : ViewModel() {

    private val _state = MutableStateFlow(AdminCuriositaUiState())
    val state: StateFlow<AdminCuriositaUiState> = _state.asStateFlow()

    init { carica() }

    fun carica() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            if (!FirebaseManager.isAdmin()) {
                _state.value = AdminCuriositaUiState(isLoading = false, isAdmin = false)
                return@launch
            }
            val lista = FirebaseManager.caricaTutteLeCuriositaRemote()
            _state.value = AdminCuriositaUiState(isLoading = false, isAdmin = true, curiosita = lista)
        }
    }

    fun salva(c: FirebaseManager.CuriositaRemota) {
        viewModelScope.launch {
            val result = FirebaseManager.salvaCuriosita(c)
            if (result.isSuccess) {
                _state.value = _state.value.copy(messaggio = "✅ Curiosità salvata!")
                carica()
            } else {
                _state.value = _state.value.copy(errore = "Errore: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun elimina(externalId: String) {
        viewModelScope.launch {
            val result = FirebaseManager.eliminaCuriosita(externalId)
            if (result.isSuccess) {
                _state.value = _state.value.copy(messaggio = "🗑️ Curiosità eliminata.")
                carica()
            } else {
                _state.value = _state.value.copy(errore = "Errore: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    fun importaJson(json: String) {
        viewModelScope.launch {
            try {
                val lista = parseJson(json)
                val result = FirebaseManager.importaBulk(lista)
                if (result.isSuccess) {
                    _state.value = _state.value.copy(
                        messaggio = "✅ Importate ${result.getOrDefault(0)} curiosità.")
                    carica()
                } else {
                    _state.value = _state.value.copy(errore = "Errore import: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(errore = "JSON non valido: ${e.message}")
            }
        }
    }

    fun dismissMessaggio() { _state.value = _state.value.copy(messaggio = null) }
    fun dismissErrore()    { _state.value = _state.value.copy(errore = null) }

    private fun parseJson(json: String): List<FirebaseManager.CuriositaRemota> {
        val array = if (json.trim().startsWith("[")) JSONArray(json)
        else {
            val obj = org.json.JSONObject(json)
            obj.getJSONArray("curiosita")
        }

        return (0 until array.length()).map { i ->
            val obj     = array.getJSONObject(i)
            val quizObj = obj.optJSONObject("quiz")

            val rispostaCorretta = quizObj?.optString("rispostaCorretta")
                ?: quizObj?.optString("risposta_corretta")
            
            val risposteErrate = quizObj?.optJSONArray("risposteErrate")?.let { arr ->
                (0 until arr.length()).map { arr.getString(it) }
            } ?: quizObj?.optJSONArray("risposte_errate")?.let { arr ->
                (0 until arr.length()).map { arr.getString(it) }
            }

            FirebaseManager.CuriositaRemota(
                externalId       = obj.getString("id"),
                titolo           = obj.getString("titolo"),
                corpo            = obj.getString("corpo"),
                categoria        = obj.optString("categoria", ""),
                emoji            = obj.optString("emoji", ""),
                domanda          = quizObj?.optString("domanda")?.ifBlank { null },
                rispostaCorretta = rispostaCorretta,
                risposteErrate   = risposteErrate,
                spiegazione      = quizObj?.let {
                    it.optString("spiegazione").ifBlank { null }
                }
            )
        }
    }

    class Factory : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = AdminCuriositaViewModel() as T
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCuriositaScreen(nav: NavController, apriModificaId: String? = null) {
    val vm: AdminCuriositaViewModel = viewModel(factory = AdminCuriositaViewModel.Factory())
    val state  by vm.state.collectAsState()
    val ctx    = LocalContext.current
    val scope = rememberCoroutineScope()

    var showForm      by remember { mutableStateOf(false) }
    var editingItem   by remember { mutableStateOf<FirebaseManager.CuriositaRemota?>(null) }
    var deleteTarget  by remember { mutableStateOf<String?>(null) }
    var query         by remember { mutableStateOf("") }
    var isMigrating   by remember { mutableStateOf(false) }

    LaunchedEffect(apriModificaId, state.curiosita) {
        if (apriModificaId != null && state.curiosita.isNotEmpty()) {
            val target = state.curiosita.find { it.externalId == apriModificaId }
            if (target != null && !showForm) {
                editingItem = target
                showForm = true
            }
        }
    }

    val jsonLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            val stream = ctx.contentResolver.openInputStream(it)
            val text = stream?.bufferedReader()?.readText() ?: ""
            stream?.close()
            if (text.isNotBlank()) {
                vm.importaJson(text)
            }
        }
    }

    val gradientBg = Brush.verticalGradient(listOf(
        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
        MaterialTheme.colorScheme.background
    ))

    state.messaggio?.let {
        LaunchedEffect(it) {
            kotlinx.coroutines.delay(3000)
            vm.dismissMessaggio()
        }
    }

    deleteTarget?.let { exId ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            icon  = { Icon(Icons.Default.DeleteForever, null, tint = MaterialTheme.colorScheme.error) },
            title = { Text("Elimina curiosità?", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
            text  = { Text("Questa azione è irreversibile e rimuove la curiosità per tutti gli utenti.", textAlign = TextAlign.Center) },
            confirmButton = {
                Button(onClick = { vm.elimina(exId); deleteTarget = null },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Elimina") }
            },
            dismissButton = { OutlinedButton(onClick = { deleteTarget = null }) { Text("Annulla") } }
        )
    }

    state.errore?.let {
        AlertDialog(
            onDismissRequest = { vm.dismissErrore() },
            title = { Text("Errore") },
            text  = { Text(it) },
            confirmButton = { TextButton(onClick = { vm.dismissErrore() }) { Text("OK") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestione curiosità") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Indietro")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        scope.launch {
                            isMigrating = true
                            FirebaseManager.eseguiMigrazioneQuizPiatta()
                            vm.carica()
                            isMigrating = false
                        }
                    }, enabled = !isMigrating) {
                        if (isMigrating) CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                        else Icon(Icons.Default.Refresh, "Migra")
                    }
                    IconButton(onClick = { jsonLauncher.launch(arrayOf("application/json")) }) {
                        Icon(Icons.Default.FileUpload, "Importa JSON")
                    }
                    IconButton(onClick = { editingItem = null; showForm = true }) {
                        Icon(Icons.Default.Add, "Nuova")
                    }
                }
            )
        },
        containerColor = Color.Transparent
    ) { pad ->
        Box(Modifier.fillMaxSize().background(gradientBg).padding(pad)) {

            state.messaggio?.let { msg ->
                Surface(
                    modifier = Modifier.align(Alignment.TopCenter).padding(top = 8.dp),
                    shape    = RoundedCornerShape(20.dp),
                    color    = Color(0xFF4CAF50)
                ) {
                    Text(msg, modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        color = Color.White)
                }
            }

            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                !state.isAdmin  -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🚫", fontSize = 64.sp)
                        Spacer(Modifier.height(16.dp))
                        Text("Accesso Negato", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("Non hai i permessi per accedere a questa sezione.", textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
                    }
                }
                else -> {
                    val filtrate = remember(state.curiosita, query) {
                        if (query.isBlank()) state.curiosita
                        else {
                            val q = query.trim().lowercase()
                            state.curiosita.filter { c ->
                                c.externalId.lowercase().contains(q) ||
                                        c.titolo.lowercase().contains(q)     ||
                                        c.corpo.lowercase().contains(q)      ||
                                        c.categoria.lowercase().contains(q)
                            }
                        }
                    }

                    LazyColumn(
                        contentPadding      = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        item {
                            OutlinedTextField(
                                value         = query,
                                onValueChange = { query = it },
                                modifier      = Modifier.fillMaxWidth(),
                                placeholder   = { Text("Cerca per ID, titolo, testo, categoria…") },
                                leadingIcon   = { Icon(Icons.Default.Search, null) },
                                trailingIcon  = {
                                    if (query.isNotBlank()) {
                                        IconButton(onClick = { query = "" }) {
                                            Icon(Icons.Default.Close, "Cancella")
                                        }
                                    }
                                },
                                singleLine = true,
                                shape      = RoundedCornerShape(14.dp)
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                if (query.isBlank()) "${state.curiosita.size} curiosità su Firestore"
                                else "${filtrate.size} risultati su ${state.curiosita.size}",
                                style    = MaterialTheme.typography.bodySmall,
                                color    = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                            )
                        }

                        if (filtrate.isEmpty()) {
                            item {
                                Box(
                                    Modifier.fillMaxWidth().padding(top = 48.dp),
                                    Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("🔎", fontSize = 48.sp)
                                        Spacer(Modifier.height(12.dp))
                                        Text(
                                            "Nessun risultato per \"$query\"",
                                            style     = MaterialTheme.typography.bodyMedium,
                                            color     = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        } else {
                            items(filtrate, key = { it.externalId }) { c ->
                                CuriositaAdminCard(
                                    c          = c,
                                    query      = query,
                                    onModifica = { editingItem = c; showForm = true },
                                    onElimina  = { deleteTarget = c.externalId }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showForm) {
        CuriositaFormSheet(
            iniziale       = editingItem,
            tutteCuriosita = state.curiosita,
            onSalva        = { c -> vm.salva(c); showForm = false },
            onDismiss      = { showForm = false }
        )
    }
}

@Composable
private fun buildHighlightedText(text: String, query: String): AnnotatedString {
    if (query.isBlank()) return AnnotatedString(text)
    return buildAnnotatedString {
        val lower = text.lowercase()
        val q     = query.trim().lowercase()
        var cursor = 0
        while (cursor < text.length) {
            val idx = lower.indexOf(q, cursor)
            if (idx < 0) { append(text.substring(cursor)); break }
            append(text.substring(cursor, idx))
            withStyle(SpanStyle(
                background = Color(0xFFFFEB3B).copy(alpha = 0.5f),
                fontWeight = FontWeight.Bold
            )) {
                append(text.substring(idx, idx + q.length))
            }
            cursor = idx + q.length
        }
    }
}

@Composable
private fun CuriositaAdminCard(
    c:          FirebaseManager.CuriositaRemota,
    query:      String = "",
    onModifica: () -> Unit,
    onElimina:  () -> Unit
) {
    val colore    = coloreCategoria(c.categoria)
    val bgColore  = colore.copy(alpha = 0.13f)
    val accentColor = colore

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(bgColore)
            .clickable { onModifica() }
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Text(c.emoji.ifBlank { "📌" }, fontSize = 22.sp)
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    buildHighlightedText(c.titolo, query),
                    style      = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines   = 2,
                    overflow   = TextOverflow.Ellipsis,
                    color      = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(accentColor.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            c.categoria.ifBlank { "—" },
                            style    = MaterialTheme.typography.labelSmall,
                            color    = accentColor,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        buildHighlightedText(c.externalId, query),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f)
                    )
                }
                if (query.isNotBlank() && c.corpo.lowercase().contains(query.trim().lowercase())) {
                    val idx   = c.corpo.lowercase().indexOf(query.trim().lowercase())
                    val start = maxOf(0, idx - 20)
                    val end   = minOf(c.corpo.length, idx + query.length + 40)
                    val snip  = (if (start > 0) "…" else "") +
                            c.corpo.substring(start, end) +
                            (if (end < c.corpo.length) "…" else "")
                    Spacer(Modifier.height(4.dp))
                    Text(
                        buildHighlightedText(snip, query),
                        style    = MaterialTheme.typography.bodySmall,
                        color    = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.55f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            IconButton(onClick = onElimina) {
                Icon(Icons.Default.Delete, "Elimina", tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
            }
        }
    }
}

// ── Form Sheet ───────────────────────────────────────────────────────────

private val CATEGORIE_DISPONIBILI = listOf("Scienza", "Storia", "Spazio", "Animali", "Tecnologia", "Cibo", "Curiosità", "Sport", "Arte", "Geografia")

private fun isEmoji(s: String): Boolean = s.isNotEmpty() && s.length <= 8

private fun generaId(tutte: List<FirebaseManager.CuriositaRemota>): String {
    val maxId = tutte.mapNotNull { it.externalId.removePrefix("c").toIntOrNull() }.maxOrNull() ?: 0
    return "c" + (maxId + 1).toString().padStart(3, '0')
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CuriositaFormSheet(
    iniziale:       FirebaseManager.CuriositaRemota?,
    tutteCuriosita: List<FirebaseManager.CuriositaRemota>,
    onSalva:        (FirebaseManager.CuriositaRemota) -> Unit,
    onDismiss:      () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val isModifica = iniziale != null
    val externalId = remember { iniziale?.externalId ?: generaId(tutteCuriosita) }

    var titolo           by remember { mutableStateOf(iniziale?.titolo           ?: "") }
    var corpo            by remember { mutableStateOf(iniziale?.corpo            ?: "") }
    var categoria        by remember { mutableStateOf(iniziale?.categoria        ?: "") }
    var emoji            by remember { mutableStateOf(iniziale?.emoji            ?: "") }
    var domanda          by remember { mutableStateOf(iniziale?.domanda          ?: "") }
    var rispostaCorretta by remember { mutableStateOf(iniziale?.rispostaCorretta ?: "") }
    var errata1          by remember { mutableStateOf(iniziale?.risposteErrate?.getOrElse(0) { "" } ?: "") }
    var errata2          by remember { mutableStateOf(iniziale?.risposteErrate?.getOrElse(1) { "" } ?: "") }
    var errata3          by remember { mutableStateOf(iniziale?.risposteErrate?.getOrElse(2) { "" } ?: "") }
    var spiegazione      by remember { mutableStateOf(iniziale?.spiegazione      ?: "") }

    var tentato by remember { mutableStateOf(false) }
    var mostraCatDropdown by remember { mutableStateOf(false) }

    val campiValidi = titolo.isNotBlank() && corpo.isNotBlank() && categoria.isNotBlank() && emoji.isNotBlank()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp).padding(bottom = 32.dp)
        ) {
            Text(if (isModifica) "Modifica curiosità" else "Nuova curiosità", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            
            OutlinedTextField(value = externalId, onValueChange = {}, label = { Text("ID univoco") }, enabled = false, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = emoji, onValueChange = { emoji = it }, label = { Text("Emoji") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = titolo, onValueChange = { titolo = it }, label = { Text("Titolo") }, modifier = Modifier.fillMaxWidth())
            
            ExposedDropdownMenuBox(expanded = mostraCatDropdown, onExpandedChange = { mostraCatDropdown = it }) {
                OutlinedTextField(value = categoria, onValueChange = {}, readOnly = true, label = { Text("Categoria") }, 
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = mostraCatDropdown) }, 
                    modifier = Modifier.fillMaxWidth().menuAnchor())
                ExposedDropdownMenu(expanded = mostraCatDropdown, onDismissRequest = { mostraCatDropdown = false }) {
                    CATEGORIE_DISPONIBILI.forEach { cat ->
                        DropdownMenuItem(text = { Text(cat) }, onClick = { categoria = cat; mostraCatDropdown = false })
                    }
                }
            }
            
            OutlinedTextField(value = corpo, onValueChange = { corpo = it }, label = { Text("Testo") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
            
            Spacer(Modifier.height(16.dp))
            Text("Quiz", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            OutlinedTextField(value = domanda, onValueChange = { domanda = it }, label = { Text("Domanda") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = rispostaCorretta, onValueChange = { rispostaCorretta = it }, label = { Text("Risposta corretta") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = errata1, onValueChange = { errata1 = it }, label = { Text("Errata 1") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = errata2, onValueChange = { errata2 = it }, label = { Text("Errata 2") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = errata3, onValueChange = { errata3 = it }, label = { Text("Errata 3") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = spiegazione, onValueChange = { spiegazione = it }, label = { Text("Spiegazione") }, modifier = Modifier.fillMaxWidth())

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = {
                    tentato = true
                    if (campiValidi) {
                        onSalva(FirebaseManager.CuriositaRemota(
                            externalId = externalId, titolo = titolo, corpo = corpo, categoria = categoria, emoji = emoji,
                            domanda = domanda.ifBlank { null }, rispostaCorretta = rispostaCorretta.ifBlank { null },
                            risposteErrate = listOf(errata1, errata2, errata3).filter { it.isNotBlank() }.ifEmpty { null },
                            spiegazione = spiegazione.ifBlank { null }
                        ))
                    }
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(if (isModifica) "Aggiorna" else "Pubblica", fontWeight = FontWeight.Bold)
            }
        }
    }
}
