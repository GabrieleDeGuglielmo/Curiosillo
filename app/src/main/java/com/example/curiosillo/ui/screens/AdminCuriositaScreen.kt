package com.example.curiosillo.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.curiosillo.CuriosityApplication
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
            val isAdmin = FirebaseManager.isAdmin()
            if (!isAdmin) {
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
                val lista = parseJsonBulk(json)
                if (lista.isEmpty()) {
                    _state.value = _state.value.copy(errore = "Nessuna curiosità trovata nel JSON.")
                    return@launch
                }
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

    /**
     * Accetta sia il formato Gist originale che il formato app:
     *
     * Gist:  risposta_corretta / risposte_errate  (snake_case)
     * App:   rispostaCorretta  / risposteErrate   (camelCase)
     *
     * Il JSON può essere l'intero oggetto { "version": N, "curiosita": [...] }
     * oppure direttamente un array [...].
     */
    private fun parseJsonBulk(json: String): List<FirebaseManager.CuriositaRemota> {
        // Accetta sia array diretto che oggetto con chiave "curiosita"
        val array = try {
            JSONArray(json)
        } catch (_: Exception) {
            org.json.JSONObject(json).getJSONArray("curiosita")
        }

        return (0 until array.length()).map { i ->
            val obj     = array.getJSONObject(i)
            val quizObj = obj.optJSONObject("quiz")

            // Supporta sia snake_case (Gist) che camelCase (formato app)
            val rispostaCorretta = quizObj?.let {
                it.optString("rispostaCorretta").ifBlank { it.optString("risposta_corretta") }
            }?.ifBlank { null }

            val risposteErrate = quizObj?.let { q ->
                val arr = q.optJSONArray("risposteErrate")
                    ?: q.optJSONArray("risposte_errate")
                arr?.let { (0 until it.length()).map { j -> it.getString(j) } }
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
        override fun <T : ViewModel> create(c: Class<T>): T = AdminCuriositaViewModel() as T
    }
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminCuriositaScreen(nav: NavController) {
    val vm: AdminCuriositaViewModel = viewModel(factory = AdminCuriositaViewModel.Factory())
    val state  by vm.state.collectAsState()
    val ctx    = LocalContext.current

    var showForm      by remember { mutableStateOf(false) }
    var showImport    by remember { mutableStateOf(false) }
    var editingItem   by remember { mutableStateOf<FirebaseManager.CuriositaRemota?>(null) }
    var deleteTarget  by remember { mutableStateOf<String?>(null) }
    var query         by remember { mutableStateOf("") }

    // File picker per JSON
    var jsonDaImportare by remember { mutableStateOf("") }
    val filePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        try {
            val stream = ctx.contentResolver.openInputStream(uri)
            jsonDaImportare = stream?.bufferedReader()?.readText() ?: ""
            stream?.close()
            showImport = true
        } catch (_: Exception) {}
    }

    val gradientBg = Brush.verticalGradient(listOf(
        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
        MaterialTheme.colorScheme.background
    ))

    // Snackbar messaggi
    state.messaggio?.let {
        LaunchedEffect(it) {
            kotlinx.coroutines.delay(2500)
            vm.dismissMessaggio()
        }
    }

    // Dialog conferma eliminazione
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

    // Dialog errore
    state.errore?.let {
        AlertDialog(
            onDismissRequest = { vm.dismissErrore() },
            title = { Text("Errore") },
            text  = { Text(it) },
            confirmButton = { TextButton(onClick = { vm.dismissErrore() }) { Text("OK") } }
        )
    }

    // Dialog import JSON
    if (showImport && jsonDaImportare.isNotBlank()) {
        AlertDialog(
            onDismissRequest = { showImport = false },
            title = { Text("Importa JSON", textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
            text  = {
                Text("Il file contiene del testo JSON. Vuoi importare le curiosità?\nVerranno aggiunte o aggiornate su Firestore.",
                    textAlign = TextAlign.Center)
            },
            confirmButton = {
                Button(onClick = { vm.importaJson(jsonDaImportare); showImport = false; jsonDaImportare = "" }) {
                    Text("Importa")
                }
            },
            dismissButton = { OutlinedButton(onClick = { showImport = false; jsonDaImportare = "" }) { Text("Annulla") } }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestione curiosità") },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Indietro")
                    }
                },
                actions = {
                    // Import da file JSON
                    IconButton(onClick = { filePicker.launch("application/json") }) {
                        Icon(Icons.Default.Upload, "Importa JSON")
                    }
                    // Nuova curiosità
                    IconButton(onClick = { editingItem = null; showForm = true }) {
                        Icon(Icons.Default.Add, "Aggiungi")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { pad ->
        Box(Modifier.fillMaxSize().background(gradientBg).padding(pad)) {

            // Messaggio successo
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
                !state.isAdmin  -> AccessoNegatoContent()
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
                        // ── Barra di ricerca ──────────────────────────────────
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

                        // ── Lista filtrata ────────────────────────────────────
                        if (filtrate.isEmpty()) {
                            item {
                                Box(
                                    Modifier.fillMaxWidth().padding(top = 48.dp),
                                    Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("🔍", fontSize = 40.sp)
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

    // Bottom sheet form aggiungi/modifica
    if (showForm) {
        CuriositaFormSheet(
            iniziale  = editingItem,
            onSalva   = { c -> vm.salva(c); showForm = false },
            onDismiss = { showForm = false }
        )
    }
}

// ── Highlight testo ricerca ───────────────────────────────────────────────────

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

// ── Card curiosità ────────────────────────────────────────────────────────────

@Composable
private fun CuriositaAdminCard(
    c:          FirebaseManager.CuriositaRemota,
    query:      String = "",
    onModifica: () -> Unit,
    onElimina:  () -> Unit
) {
    Card(
        modifier  = Modifier.fillMaxWidth(),
        shape     = RoundedCornerShape(16.dp),
        colors    = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(Modifier.padding(14.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(c.emoji.ifBlank { "📌" }, fontSize = 28.sp, modifier = Modifier.padding(end = 12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    buildHighlightedText(c.titolo, query),
                    fontWeight = FontWeight.SemiBold,
                    maxLines   = 2,
                    overflow   = TextOverflow.Ellipsis
                )
                Text(
                    buildHighlightedText("${c.externalId}  •  ${c.categoria}", query),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                )
                // Mostra anteprima corpo solo se il match è nel testo
                if (query.isNotBlank() && c.corpo.lowercase().contains(query.trim().lowercase())) {
                    val idx   = c.corpo.lowercase().indexOf(query.trim().lowercase())
                    val start = maxOf(0, idx - 20)
                    val end   = minOf(c.corpo.length, idx + query.length + 40)
                    val snip  = (if (start > 0) "…" else "") +
                            c.corpo.substring(start, end) +
                            (if (end < c.corpo.length) "…" else "")
                    Spacer(Modifier.height(3.dp))
                    Text(
                        buildHighlightedText(snip, query),
                        style   = MaterialTheme.typography.bodySmall,
                        color   = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            IconButton(onClick = onModifica) {
                Icon(Icons.Default.Edit, "Modifica", tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onElimina) {
                Icon(Icons.Default.Delete, "Elimina", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

// ── Form aggiungi/modifica ────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CuriositaFormSheet(
    iniziale:  FirebaseManager.CuriositaRemota?,
    onSalva:   (FirebaseManager.CuriositaRemota) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var externalId       by remember { mutableStateOf(iniziale?.externalId       ?: "") }
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

    val isModifica = iniziale != null
    val campiValidi = externalId.isNotBlank() && titolo.isNotBlank() && corpo.isNotBlank()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState       = sheetState
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 40.dp)
        ) {
            Text(
                if (isModifica) "Modifica curiosità" else "Nuova curiosità",
                style      = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier   = Modifier.padding(bottom = 16.dp)
            )

            // ── Campi base ────────────────────────────────────────────────────
            OutlinedTextField(
                value         = externalId,
                onValueChange = { if (!isModifica) externalId = it },
                label         = { Text("ID univoco (es. c001)") },
                enabled       = !isModifica,
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value         = emoji,
                onValueChange = { emoji = it },
                label         = { Text("Emoji") },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value         = titolo,
                onValueChange = { titolo = it },
                label         = { Text("Titolo *") },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value         = categoria,
                onValueChange = { categoria = it },
                label         = { Text("Categoria") },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value         = corpo,
                onValueChange = { corpo = it },
                label         = { Text("Testo curiosità *") },
                minLines      = 4,
                modifier      = Modifier.fillMaxWidth()
            )

            // ── Quiz (opzionale) ──────────────────────────────────────────────
            Spacer(Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))
            Text("Quiz (opzionale)", style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color      = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(
                value         = domanda,
                onValueChange = { domanda = it },
                label         = { Text("Domanda") },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value         = rispostaCorretta,
                onValueChange = { rispostaCorretta = it },
                label         = { Text("Risposta corretta") },
                singleLine    = true,
                modifier      = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = errata1, onValueChange = { errata1 = it },
                label = { Text("Risposta errata 1") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = errata2, onValueChange = { errata2 = it },
                label = { Text("Risposta errata 2") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = errata3, onValueChange = { errata3 = it },
                label = { Text("Risposta errata 3") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value         = spiegazione,
                onValueChange = { spiegazione = it },
                label         = { Text("Spiegazione risposta") },
                minLines      = 2,
                modifier      = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))
            Button(
                onClick  = {
                    onSalva(FirebaseManager.CuriositaRemota(
                        externalId       = externalId.trim(),
                        titolo           = titolo.trim(),
                        corpo            = corpo.trim(),
                        categoria        = categoria.trim(),
                        emoji            = emoji.trim(),
                        domanda          = domanda.trim().ifBlank { null },
                        rispostaCorretta = rispostaCorretta.trim().ifBlank { null },
                        risposteErrate   = listOf(errata1, errata2, errata3)
                            .map { it.trim() }.filter { it.isNotBlank() }.ifEmpty { null },
                        spiegazione      = spiegazione.trim().ifBlank { null }
                    ))
                },
                enabled  = campiValidi,
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape    = RoundedCornerShape(14.dp)
            ) {
                Text(if (isModifica) "Aggiorna" else "Pubblica",
                    fontWeight = FontWeight.Bold,
                    style      = MaterialTheme.typography.titleMedium)
            }
        }
    }
}