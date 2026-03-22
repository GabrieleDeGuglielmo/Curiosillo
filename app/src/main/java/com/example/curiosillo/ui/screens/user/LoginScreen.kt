package com.example.curiosillo.ui.screens.user

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.example.curiosillo.firebase.FirebaseManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.curiosillo.CuriosityApplication
import com.example.curiosillo.R
import com.example.curiosillo.viewmodel.AuthUiState
import com.example.curiosillo.viewmodel.AuthViewModel
import com.example.curiosillo.viewmodel.HomeViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch
import androidx.core.net.toUri

@Composable
fun LoginScreen(onLoginSuccesso: () -> Unit) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as CuriosityApplication

    val vm: AuthViewModel = viewModel(
        factory = AuthViewModel.Factory(app.repository, app.gamificationPrefs, app.contentPrefs, ctx)
    )
    val homeVm: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(
            repo         = app.repository,
            contentPrefs = app.contentPrefs,
            context      = ctx,
            database     = app.database
        )
    )

    val state     by vm.state.collectAsState()
    val homeState by homeVm.state.collectAsState()

    var showGoogleUsernameDialog by remember { mutableStateOf(false) }
    var googleUsernameInput by remember { mutableStateOf("") }

    // Naviga alla home dopo login riuscito
    LaunchedEffect(state) {
        if (state is AuthUiState.Successo) {
            onLoginSuccesso()
            vm.resetStato()
        }
        if (state is AuthUiState.RichiedeUsername) {
            googleUsernameInput = (state as AuthUiState.RichiedeUsername).suggestedUsername
            showGoogleUsernameDialog = true
        }
    }

    // Google Sign-In launcher
    val googleLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            try {
                val account = GoogleSignIn.getSignedInAccountFromIntent(result.data)
                    .getResult(ApiException::class.java)
                account.idToken?.let { vm.loginGoogle(it) }
            } catch (_: ApiException) {}
        }
    }

    var isRegistrazione   by remember { mutableStateOf(false) }
    var email             by remember { mutableStateOf("") }
    var password          by remember { mutableStateOf("") }
    var confermaPassword  by remember { mutableStateOf("") }
    val coroutineScope         = rememberCoroutineScope()
    var usernameInput         by remember { mutableStateOf("") }
    var usernameDisponibile   by remember { mutableStateOf<Boolean?>(null) }
    var usernameCheckLoading  by remember { mutableStateOf(false) }
    var usernameCheckJob      by remember { mutableStateOf<Job?>(null) }
    var mostraPassword    by remember { mutableStateOf(false) }
    var showRecuperoDialog by remember { mutableStateOf(false) }
    var emailRecupero     by remember { mutableStateOf("") }
    var showVerificaDialog by remember { mutableStateOf(false) }

    // Gestione stato verifica email
    LaunchedEffect(state) {
        if (state is AuthUiState.VerificaEmailInviata) {
            showVerificaDialog = true
        }
    }

    DisposableEffect(usernameInput) {
        onDispose {
            usernameCheckJob?.cancel()
        }
    }

    // Funzione helper per il controllo (da chiamare nell'onValueChange)
    fun checkUsernameAvailability(name: String) {
        val trimmed = name.trim()
        usernameDisponibile = null
        usernameCheckJob?.cancel()

        if (trimmed.length >= 3) {
            usernameCheckLoading = true
            usernameCheckJob = coroutineScope.launch {
                try {
                    delay(600) // Debounce per non sovraccaricare Firebase
                    usernameDisponibile = !FirebaseManager.isUsernameOccupato(trimmed)
                } catch (e: Exception) {
                    usernameDisponibile = null // In caso di errore di rete
                } finally {
                    usernameCheckLoading = false
                }
            }
        } else {
            usernameCheckLoading = false
        }
    }

    // ── Dialog per impostare lo username dopo Google ──────────────
    if (showGoogleUsernameDialog && state is AuthUiState.RichiedeUsername) {
        val s = state as AuthUiState.RichiedeUsername

        var isChecking by remember { mutableStateOf(false) }
        var isAvail    by remember { mutableStateOf<Boolean?>(null) }

        LaunchedEffect(googleUsernameInput) {
            val trimmed = googleUsernameInput.trim()
            isAvail = null
            if (trimmed.length < 3) { isChecking = false; return@LaunchedEffect }
            isChecking = true
            try {
                delay(600)
                isAvail = !FirebaseManager.isUsernameOccupato(trimmed)
            } catch (_: Exception) {
                isAvail = null
            } finally {
                isChecking = false
            }
        }

        AlertDialog(
            onDismissRequest = { /* obbligatorio scegliere */ },
            title = { Text("Scegli il tuo username", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold) },
            text = {
                val scrollState = rememberScrollState()
                Column(Modifier.verticalScroll(scrollState)) {
                    Text(
                        "Benvenuto! Come vorresti farti chiamare su Curiosillo?",
                        Modifier.padding(bottom = 16.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(0.7f)
                    )
                    OutlinedTextField(
                        value         = googleUsernameInput,
                        onValueChange = { v -> if (v.length <= 20) googleUsernameInput = v },
                        label         = { Text("Username") },
                        trailingIcon  = {
                            when {
                                isChecking       -> CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                isAvail == true  -> Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50))
                                isAvail == false -> Icon(Icons.Default.Cancel, null, tint = MaterialTheme.colorScheme.error)
                            }
                        },
                        isError        = isAvail == false,
                        supportingText = {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                when {
                                    isAvail == false -> Text("Username già in uso", color = MaterialTheme.colorScheme.error)
                                    isAvail == true  -> Text("Username disponibile", color = Color(0xFF4CAF50))
                                    googleUsernameInput.length in 1..<3 -> Text("Minimo 3 caratteri")
                                    else             -> Spacer(Modifier.weight(1f))
                                }
                                Text("${googleUsernameInput.length}/20")
                            }
                        },
                        singleLine     = true,
                        shape          = RoundedCornerShape(14.dp),
                        modifier       = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick  = {
                        vm.completaRegistrazioneGoogle(s.user, googleUsernameInput.trim())
                        showGoogleUsernameDialog = false
                    },
                    enabled = googleUsernameInput.trim().length >= 3 && isAvail == true && !isChecking,
                    modifier = Modifier.height(48.dp) // Touch target
                ) {
                    if (state is AuthUiState.Loading)
                        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary)
                    else Text("Conferma")
                }
            }
        )
    }

    // ── Dialog verifica email ────────────────────────────────────────────────
    if (showVerificaDialog) {
        AlertDialog(
            onDismissRequest = {
                showVerificaDialog = false
                vm.resetStato()
            },
            icon = { Text("✉️", fontSize = 32.sp) },
            title = { Text("Verifica la tua email", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
            text = {
                Text(
                    "Ti abbiamo inviato un'email di verifica a $email. Per favore, clicca sul link contenuto nell'email per attivare il tuo account prima di accedere.",
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            },
            confirmButton = {
                Button(onClick = {
                    showVerificaDialog = false
                    vm.resetStato()
                }, modifier = Modifier.height(48.dp)) { Text("Ho capito") }
            }
        )
    }

    // ── Dialog recupero password ──────────────────────────────────────────────
    if (showRecuperoDialog) {
        val emailInviata = state is AuthUiState.EmailRecuperoInviata
        AlertDialog(
            onDismissRequest = {
                showRecuperoDialog = false
                emailRecupero = ""
                if (emailInviata) vm.resetStato()
            },
            icon  = { Text("🔑", fontSize = 32.sp) },
            title = {
                Text(
                    if (emailInviata) "Email inviata!" else "Recupera password",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign  = TextAlign.Center,
                    modifier   = Modifier.fillMaxWidth()
                )
            },
            text = {
                if (emailInviata) {
                    Text(
                        "Controlla la tua casella email\nTi abbiamo inviato un link per reimpostare la password.",
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color     = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                } else {
                    Column {
                        Text(
                            "Inserisci la tua email e ti invieremo un link per reimpostare la password.",
                            style    = MaterialTheme.typography.bodyMedium,
                            color    = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        OutlinedTextField(
                            value           = emailRecupero,
                            onValueChange   = { emailRecupero = it },
                            label           = { Text("Email") },
                            leadingIcon     = { Icon(Icons.Default.Email, null) },
                            singleLine      = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            shape           = RoundedCornerShape(14.dp),
                            modifier        = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                if (emailInviata) {
                    Button(onClick = {
                        showRecuperoDialog = false
                        emailRecupero = ""
                        vm.resetStato()
                    }, modifier = Modifier.height(48.dp)) { Text("OK") }
                } else {
                    Button(
                        onClick  = { vm.recuperaPassword(emailRecupero) },
                        enabled  = emailRecupero.isNotBlank() && state !is AuthUiState.Loading,
                        modifier = Modifier.height(48.dp)
                    ) {
                        if (state is AuthUiState.Loading) {
                            CircularProgressIndicator(Modifier.size(18.dp),
                                color = Color.White, strokeWidth = 2.dp)
                        } else {
                            Text("Invia")
                        }
                    }
                }
            },
            dismissButton = {
                if (!emailInviata) {
                    TextButton(onClick = {
                        showRecuperoDialog = false
                        emailRecupero = ""
                    }, modifier = Modifier.height(48.dp)) { Text("Annulla") }
                }
            }
        )
    }

    // ── Dialog aggiornamento app ──────────────────────────────────────────────
    homeState.aggiornamentoApp?.let { info ->
        AlertDialog(
            onDismissRequest = { homeVm.dismissAggiornamento() },
            icon  = { Icon(Icons.Default.SystemUpdate, null,
                tint = MaterialTheme.colorScheme.primary) },
            title = {
                Text("Aggiornamento disponibile",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            },
            text = {
                Text(
                    "È disponibile la versione ${info.versione} di Curiosillo.\nVuoi scaricarla adesso?",
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            },
            confirmButton = {
                Button(onClick = {
                    val url = info.downloadUrl.ifEmpty { info.releaseUrl }
                    ctx.startActivity(Intent(Intent.ACTION_VIEW, url.toUri()))
                    homeVm.dismissAggiornamento()
                }, modifier = Modifier.height(48.dp)) { Text("Scarica") }
            },
            dismissButton = {
                TextButton(onClick = { homeVm.dismissAggiornamento() }, modifier = Modifier.height(48.dp)) { Text("Dopo") }
            }
        )
    }

    val gradientBg = Brush.verticalGradient(listOf(
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        MaterialTheme.colorScheme.background
    ))

    Box(Modifier.fillMaxSize().background(gradientBg)) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp)
                .padding(top = 80.dp, bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── Logo Curiosillo ──────────────────────────────────────────────────
            Image(
                painter = painterResource(id = R.drawable.ic_logo),
                contentDescription = "Logo Curiosillo",
                modifier = Modifier.size(80.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text("Curiosillo",
                style      = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color      = MaterialTheme.colorScheme.primary)
            Text(
                if (isRegistrazione) "Crea il tuo account" else "Benvenuto!",
                style    = MaterialTheme.typography.bodyLarge,
                color    = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.padding(top = 4.dp, bottom = 32.dp)
            )

            // Errore
            val errore = (state as? AuthUiState.Errore)?.messaggio
            AnimatedVisibility(visible = errore != null) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                    colors   = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer),
                    shape    = RoundedCornerShape(12.dp)
                ) {
                    Text(errore ?: "", Modifier.padding(12.dp),
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodyMedium)
                }
            }

            // Username (solo registrazione)
            AnimatedVisibility(visible = isRegistrazione) {
                OutlinedTextField(
                    value = usernameInput,
                    onValueChange = { v ->
                        if (v.length <= 20) {
                            usernameInput = v
                            usernameDisponibile = null
                            usernameCheckJob?.cancel()

                            val trimmed = v.trim()
                            if (trimmed.length >= 3) {
                                usernameCheckLoading = true
                                usernameCheckJob = coroutineScope.launch {
                                    try {
                                        delay(600)
                                        val occupato = FirebaseManager.isUsernameOccupato(trimmed)
                                        usernameDisponibile = !occupato
                                    } catch (e: Exception) {
                                        usernameDisponibile = null
                                    } finally {
                                        usernameCheckLoading = false
                                    }
                                }
                            } else {
                                usernameCheckLoading = false
                            }
                        }
                    },
                    label = { Text("Username") },
                    leadingIcon = { Icon(Icons.Default.Person, null) },
                    trailingIcon = {
                        when {
                            usernameCheckLoading -> CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                            usernameDisponibile == true -> Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50))
                            usernameDisponibile == false -> Icon(Icons.Default.Cancel, null, tint = MaterialTheme.colorScheme.error)
                        }
                    },
                    isError = usernameDisponibile == false,
                    supportingText = {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            when {
                                usernameDisponibile == false -> Text("Username già in uso", color = MaterialTheme.colorScheme.error)
                                usernameDisponibile == true -> Text("Username disponibile", color = Color(0xFF4CAF50))
                                usernameInput.isNotEmpty() && usernameInput.length < 3 -> Text("Minimo 3 caratteri")
                                else -> Spacer(Modifier.weight(1f))
                            }
                            Text("${usernameInput.length}/20")
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                )
            }

            OutlinedTextField(
                value           = email,
                onValueChange   = { email = it },
                label           = { Text("Email") },
                leadingIcon     = { Icon(Icons.Default.Email, null) },
                singleLine      = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                shape           = RoundedCornerShape(14.dp),
                modifier        = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value                = password,
                onValueChange        = { password = it },
                label                = { Text("Password") },
                leadingIcon          = { Icon(Icons.Default.Lock, null) },
                trailingIcon         = {
                    IconButton(onClick = { mostraPassword = !mostraPassword }) {
                        Icon(
                            if (mostraPassword) Icons.Default.VisibilityOff
                            else Icons.Default.Visibility, null
                        )
                    }
                },
                singleLine           = true,
                visualTransformation = if (mostraPassword) VisualTransformation.None
                else PasswordVisualTransformation(),
                keyboardOptions      = KeyboardOptions(keyboardType = KeyboardType.Password),
                shape                = RoundedCornerShape(14.dp),
                modifier             = Modifier.fillMaxWidth()
            )

            // Conferma Password (solo registrazione)
            AnimatedVisibility(visible = isRegistrazione) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value                = confermaPassword,
                        onValueChange        = { confermaPassword = it },
                        label                = { Text("Conferma Password") },
                        leadingIcon          = { Icon(Icons.Default.Lock, null) },
                        singleLine           = true,
                        visualTransformation = if (mostraPassword) VisualTransformation.None
                        else PasswordVisualTransformation(),
                        keyboardOptions      = KeyboardOptions(keyboardType = KeyboardType.Password),
                        isError              = isRegistrazione && confermaPassword.isNotEmpty() && confermaPassword != password,
                        supportingText       = {
                            if (isRegistrazione && confermaPassword.isNotEmpty() && confermaPassword != password) {
                                Text("Le password non coincidono", color = MaterialTheme.colorScheme.error)
                            }
                        },
                        shape                = RoundedCornerShape(14.dp),
                        modifier             = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // Link "Password dimenticata?" — Allineato a 48dp di area tocco
            AnimatedVisibility(visible = !isRegistrazione) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    Text(
                        "Password dimenticata?",
                        color      = MaterialTheme.colorScheme.primary,
                        style      = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        modifier   = Modifier
                            .minimumInteractiveComponentSize() // Assicura 48dp
                            .clickable {
                                emailRecupero = email
                                showRecuperoDialog = true
                                vm.resetStato()
                            }
                            .padding(vertical = 8.dp)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            // Pulsante principale
            Button(
                onClick = {
                    if (isRegistrazione) {
                        val finalUsername = usernameInput.trim()
                        if (finalUsername.length >= 3 && usernameDisponibile == true && password == confermaPassword) {
                            vm.registraEmail(email, password, finalUsername)
                        }
                    } else {
                        vm.loginEmail(email, password)
                    }
                },
                enabled = state !is AuthUiState.Loading &&
                        email.isNotBlank() &&
                        password.length >= 6 &&
                        (!isRegistrazione || (usernameInput.trim().length >= 3 &&
                                usernameDisponibile == true &&
                                !usernameCheckLoading &&
                                password == confermaPassword)),
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                if (state is AuthUiState.Loading) {
                    CircularProgressIndicator(Modifier.size(22.dp), color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text(
                        if (isRegistrazione) "Crea account" else "Accedi",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            // Pulsante Google
            OutlinedButton(
                onClick  = {
                    val client = vm.getGoogleSignInClient("235758737875-50kaqom32soh8n4ih7lo49nd1pn9s69u.apps.googleusercontent.com")
                    googleLauncher.launch(client.signInIntent)
                },
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape    = RoundedCornerShape(14.dp)
            ) {
                Text("🔵  Accedi con Google", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(24.dp))

            // Switch login / registrazione — Allineato a 48dp di area tocco
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text(
                    if (isRegistrazione) "Hai già un account? " else "Non hai un account? ",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
                Text(
                    if (isRegistrazione) "Accedi" else "Registrati",
                    color      = MaterialTheme.colorScheme.primary,
                    style      = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    modifier   = Modifier
                        .minimumInteractiveComponentSize()
                        .clickable {
                            isRegistrazione = !isRegistrazione
                            vm.resetStato()
                        }
                        .padding(horizontal = 4.dp)
                )
            }
        }
    }
}