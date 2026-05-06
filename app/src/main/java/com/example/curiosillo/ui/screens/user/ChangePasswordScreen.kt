package com.example.curiosillo.ui.screens.user

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.curiosillo.domain.PasswordValidationResult
import com.example.curiosillo.ui.theme.Success
import com.example.curiosillo.viewmodel.ChangePasswordUiState
import com.example.curiosillo.viewmodel.ChangePasswordViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
    navController: NavController,
    viewModel: ChangePasswordViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentPassword by viewModel.currentPassword.collectAsState()
    val newPassword by viewModel.newPassword.collectAsState()
    val confirmPassword by viewModel.confirmPassword.collectAsState()
    val validation by viewModel.newPasswordValidation.collectAsState()
    val isFormValid by viewModel.isFormValid.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState) {
        if (uiState is ChangePasswordUiState.Success) {
            snackbarHostState.showSnackbar("Password cambiata con successo!")
            viewModel.resetState()
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = Success,
                    contentColor = Color.White
                )
            }
        },
        topBar = {
            TopAppBar(
                title = { Text("Cambia Password", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
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
                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                MaterialTheme.colorScheme.background
            )
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradientBg)
                .padding(padding)
        ) {
            ChangePasswordContent(
                currentPassword = currentPassword,
                newPassword = newPassword,
                confirmPassword = confirmPassword,
                validation = validation,
                isFormValid = isFormValid,
                uiState = uiState,
                onCurrentPasswordChange = viewModel::onCurrentPasswordChange,
                onNewPasswordChange = viewModel::onNewPasswordChange,
                onConfirmPasswordChange = viewModel::onConfirmPasswordChange,
                onSubmit = viewModel::changePassword
            )
        }
    }
}

@Composable
fun ChangePasswordContent(
    currentPassword: String,
    newPassword: String,
    confirmPassword: String,
    validation: PasswordValidationResult,
    isFormValid: Boolean,
    uiState: ChangePasswordUiState,
    onCurrentPasswordChange: (String) -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Inserisci le tue credenziali per aggiornare la password di sicurezza.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        PasswordField(
            value = currentPassword,
            onValueChange = onCurrentPasswordChange,
            label = "Password Attuale",
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        PasswordField(
            value = newPassword,
            onValueChange = onNewPasswordChange,
            label = "Nuova Password",
            isError = validation !is PasswordValidationResult.Valid && validation !is PasswordValidationResult.Empty,
            supportingText = {
                ValidationErrorMessage(validation)
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(16.dp))

        PasswordField(
            value = confirmPassword,
            onValueChange = onConfirmPasswordChange,
            label = "Conferma Nuova Password",
            isError = confirmPassword.isNotEmpty() && confirmPassword != newPassword,
            supportingText = {
                if (confirmPassword.isNotEmpty() && confirmPassword != newPassword) {
                    Text("Le password non coincidono", color = MaterialTheme.colorScheme.error)
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(32.dp))

        if (uiState is ChangePasswordUiState.Error) {
            Text(
                text = uiState.message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        Button(
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(12.dp),
            enabled = isFormValid && uiState !is ChangePasswordUiState.Loading
        ) {
            if (uiState is ChangePasswordUiState.Loading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Text("Aggiorna Password", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}

@Composable
fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    isError: Boolean = false,
    supportingText: @Composable (() -> Unit)? = null
) {
    var passwordVisible by remember { mutableStateOf(false) }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                Icon(image, null)
            }
        },
        isError = isError,
        supportingText = supportingText,
        singleLine = true
    )
}

@Composable
fun ValidationErrorMessage(validation: PasswordValidationResult) {
    val message = when (validation) {
        PasswordValidationResult.TooShort -> "Minimo 8 caratteri"
        PasswordValidationResult.NoUppercase -> "Almeno una lettera maiuscola"
        PasswordValidationResult.NoNumber -> "Almeno un numero"
        PasswordValidationResult.Valid -> "Password valida"
        else -> null
    }

    message?.let {
        Text(
            text = it,
            color = if (validation is PasswordValidationResult.Valid) Success else MaterialTheme.colorScheme.error
        )
    }
}
