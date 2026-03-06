package com.example.curiosillo.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.curiosillo.CuriosityApplication
import com.example.curiosillo.viewmodel.AuthUiState
import com.example.curiosillo.viewmodel.AuthViewModel
import com.example.curiosillo.viewmodel.HomeViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException

@Composable
fun LoginScreen(onLoginSuccesso: () -> Unit) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as CuriosityApplication

    val vm: AuthViewModel = viewModel(
        factory = AuthViewModel.Factory(app.repository, app.gamificationPrefs, ctx)
    )
    // HomeViewModel usato solo per il check aggiornamenti
    val homeVm: HomeViewModel = viewModel(
        factory = HomeViewModel.Factory(app.repository, app.contentPrefs, ctx)
    )

    val state     by vm.state.collectAsState()
    val homeState by homeVm.state.collectAsState()

    // Naviga alla home dopo login riuscito
    LaunchedEffect(state) {
        if (state is AuthUiState.Successo) {
            onLoginSuccesso()
            vm.resetStato()
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

    var isRegistrazione by remember { mutableStateOf(false) }
    var email           by remember { mutableStateOf("") }
    var password        by remember { mutableStateOf("") }
    var username        by remember { mutableStateOf("") }
    var mostraPassword  by remember { mutableStateOf(false) }

    // ── Dialog aggiornamento app ──────────────────────────────────────────────
    homeState.aggiornamentoApp?.let { info ->
        AlertDialog(
            onDismissRequest = { homeVm.dismissAggiornamento() },
            icon  = { Icon(Icons.Default.SystemUpdate, null,
                tint = MaterialTheme.colorScheme.primary) },
            title = {
                Text("Aggiornamento disponibile", fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            },
            text = {
                Text(
                    "È disponibile la versione ${info.versione} di Curiosillo.\nVuoi scaricarla adesso?",
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            },
            confirmButton = {
                Button(onClick = {
                    val url = if (info.downloadUrl.isNotEmpty()) info.downloadUrl else info.releaseUrl
                    ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    homeVm.dismissAggiornamento()
                }) { Text("Scarica", fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { homeVm.dismissAggiornamento() }) { Text("Dopo") }
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
            Text("🧠", fontSize = 64.sp)
            Spacer(Modifier.height(8.dp))
            Text("Curiosillo",
                style      = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.ExtraBold,
                color      = MaterialTheme.colorScheme.primary)
            Text(
                if (isRegistrazione) "Crea il tuo account" else "Bentornato!",
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
                    value         = username,
                    onValueChange = { username = it },
                    label         = { Text("Username") },
                    leadingIcon   = { Icon(Icons.Default.Person, null) },
                    singleLine    = true,
                    shape         = RoundedCornerShape(14.dp),
                    modifier      = Modifier.fillMaxWidth().padding(bottom = 12.dp)
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
            Spacer(Modifier.height(24.dp))

            // Pulsante principale
            Button(
                onClick  = {
                    if (isRegistrazione) vm.registraEmail(email, password, username)
                    else vm.loginEmail(email, password)
                },
                enabled  = state !is AuthUiState.Loading &&
                        email.isNotBlank() && password.isNotBlank() &&
                        (!isRegistrazione || username.isNotBlank()),
                modifier = Modifier.fillMaxWidth().height(54.dp),
                shape    = RoundedCornerShape(14.dp)
            ) {
                if (state is AuthUiState.Loading) {
                    CircularProgressIndicator(Modifier.size(22.dp),
                        color = Color.White, strokeWidth = 2.dp)
                } else {
                    Text(
                        if (isRegistrazione) "Crea account" else "Accedi",
                        fontWeight = FontWeight.Bold, fontSize = 16.sp
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
                Text("🔵  Accedi con Google", fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(24.dp))

            // Switch login / registrazione
            Row(horizontalArrangement = Arrangement.Center) {
                Text(
                    if (isRegistrazione) "Hai già un account? " else "Non hai un account? ",
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
                Text(
                    if (isRegistrazione) "Accedi" else "Registrati",
                    color      = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier   = Modifier.clickable {
                        isRegistrazione = !isRegistrazione
                        vm.resetStato()
                    }
                )
            }
        }
    }
}