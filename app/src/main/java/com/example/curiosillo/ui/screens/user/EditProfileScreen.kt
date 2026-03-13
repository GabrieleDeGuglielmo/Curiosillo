package com.example.curiosillo.ui.screens.user

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.example.curiosillo.firebase.FirebaseManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.curiosillo.CuriosityApplication
import com.example.curiosillo.ui.theme.Success
import com.example.curiosillo.viewmodel.ProfileViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(nav: NavController) {
    val ctx = LocalContext.current
    val app = ctx.applicationContext as CuriosityApplication
    val vm: ProfileViewModel = viewModel(
        factory = ProfileViewModel.Factory(app.repository, app.gamificationPrefs)
    )
    val state by vm.state.collectAsState()

    var usernameInput by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    val coroutineScope           = rememberCoroutineScope()
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var successMsg by remember { mutableStateOf<String?>(null) }
    var isUpdating by remember { mutableStateOf(false) }
    var usernameDisponibile   by remember { mutableStateOf<Boolean?>(null) }
    var usernameCheckLoading  by remember { mutableStateOf(false) }
    var usernameCheckJob      by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(state.username) {
        if (usernameInput.isEmpty()) usernameInput = state.username
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Modifica profilo", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { nav.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { padding ->
        val gradientBg = Brush.verticalGradient(
            listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                MaterialTheme.colorScheme.background
            )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBg)
        ) {
            if (state.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    Text(
                        "Informazioni Personali",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    OutlinedTextField(
                        value = usernameInput,
                        onValueChange = { v ->
                            if (v.length <= 20) {
                                usernameInput = v
                                errorMsg = null
                                successMsg = null
                                usernameDisponibile = null
                                usernameCheckJob?.cancel()
                                val trimmed = v.trim()
                                if (trimmed.length >= 3 && trimmed != state.username) {
                                    usernameCheckLoading = true
                                    usernameCheckJob = coroutineScope.launch {
                                        delay(600)
                                        usernameDisponibile = !FirebaseManager.isUsernameOccupato(trimmed)
                                        usernameCheckLoading = false
                                    }
                                } else {
                                    usernameCheckLoading = false
                                }
                            }
                        },
                        label = { Text("Username") },
                        trailingIcon = {
                            when {
                                usernameCheckLoading -> CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                                usernameDisponibile == true  -> Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF4CAF50))
                                usernameDisponibile == false -> Icon(Icons.Default.Cancel, null, tint = MaterialTheme.colorScheme.error)
                            }
                        },
                        isError = usernameDisponibile == false,
                        supportingText = {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                when {
                                    usernameDisponibile == false -> Text("Username già in uso", color = MaterialTheme.colorScheme.error)
                                    usernameDisponibile == true  -> Text("Username disponibile", color = MaterialTheme.colorScheme.primary)
                                    usernameInput.trim().isNotEmpty() && usernameInput.trim().length < 3 -> Text("Minimo 3 caratteri")
                                    else -> Spacer(Modifier.weight(1f))
                                }
                                Text("${usernameInput.length}/20")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Spacer(Modifier.height(16.dp))

                    Button(
                        onClick = {
                            errorMsg = null
                            successMsg = null
                            if (usernameInput.isBlank()) {
                                errorMsg = "L'username non può essere vuoto"
                                return@Button
                            }
                            isUpdating = true
                            vm.cambiaUsername(usernameInput,
                                onSuccess = {
                                    isUpdating = false
                                    successMsg = "Username aggiornato con successo!"
                                },
                                onError = {
                                    isUpdating = false
                                    errorMsg = it
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        enabled = !isUpdating && usernameInput.trim().isNotEmpty() &&
                                (usernameInput.trim() == state.username || (usernameDisponibile == true && !usernameCheckLoading))
                    ) {
                        if (isUpdating) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                        else Text("Aggiorna Username")
                    }

                    if (!state.isGoogleUser) {
                        Spacer(Modifier.height(32.dp))
                        HorizontalDivider()
                        Spacer(Modifier.height(24.dp))

                        Text(
                            "Sicurezza",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        OutlinedTextField(
                            value = password,
                            onValueChange = {
                                password = it
                                errorMsg = null
                                successMsg = null
                            },
                            label = { Text("Nuova Password") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(image, null)
                                }
                            },
                            singleLine = true
                        )

                        Spacer(Modifier.height(12.dp))

                        OutlinedTextField(
                            value = confirmPassword,
                            onValueChange = {
                                confirmPassword = it
                                errorMsg = null
                                successMsg = null
                            },
                            label = { Text("Conferma Password") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            singleLine = true
                        )

                        Spacer(Modifier.height(16.dp))

                        Button(
                            onClick = {
                                errorMsg = null
                                successMsg = null
                                if (password.length < 6) {
                                    errorMsg = "La password deve essere di almeno 6 caratteri"
                                    return@Button
                                }
                                if (password != confirmPassword) {
                                    errorMsg = "Le password non coincidono"
                                    return@Button
                                }
                                isUpdating = true
                                vm.cambiaPassword(password,
                                    onSuccess = {
                                        isUpdating = false
                                        successMsg = "Password aggiornata con successo!"
                                        password = ""
                                        confirmPassword = ""
                                    },
                                    onError = {
                                        isUpdating = false
                                        errorMsg = it
                                    }
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isUpdating && password.isNotEmpty()
                        ) {
                            if (isUpdating) CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                            else Text("Cambia Password")
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    if (errorMsg != null) {
                        Text(
                            text = errorMsg!!,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    } else if (successMsg != null) {
                        Text(
                            text = successMsg!!,
                            color = Success,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}
