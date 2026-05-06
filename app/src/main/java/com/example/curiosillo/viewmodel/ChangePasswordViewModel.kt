package com.example.curiosillo.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.curiosillo.domain.PasswordValidationResult
import com.example.curiosillo.domain.PasswordValidator
import com.example.curiosillo.firebase.FirebaseManager
import com.google.firebase.auth.EmailAuthProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class ChangePasswordUiState {
    object Idle : ChangePasswordUiState()
    object Loading : ChangePasswordUiState()
    object Success : ChangePasswordUiState()
    data class Error(val message: String) : ChangePasswordUiState()
}

class ChangePasswordViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<ChangePasswordUiState>(ChangePasswordUiState.Idle)
    val uiState: StateFlow<ChangePasswordUiState> = _uiState.asStateFlow()

    private val _currentPassword = MutableStateFlow("")
    val currentPassword: StateFlow<String> = _currentPassword.asStateFlow()

    private val _newPassword = MutableStateFlow("")
    val newPassword: StateFlow<String> = _newPassword.asStateFlow()

    private val _confirmPassword = MutableStateFlow("")
    val confirmPassword: StateFlow<String> = _confirmPassword.asStateFlow()

    val newPasswordValidation: StateFlow<PasswordValidationResult> = combine(_newPassword) { (password) ->
        PasswordValidator.validate(password)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PasswordValidationResult.Empty)

    val isFormValid: StateFlow<Boolean> = combine(
        _currentPassword,
        _newPassword,
        _confirmPassword,
        newPasswordValidation
    ) { current, new, confirm, validation ->
        current.isNotEmpty() && 
        new.isNotEmpty() && 
        confirm == new && 
        validation is PasswordValidationResult.Valid
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun onCurrentPasswordChange(value: String) {
        _currentPassword.value = value
    }

    fun onNewPasswordChange(value: String) {
        _newPassword.value = value
    }

    fun onConfirmPasswordChange(value: String) {
        _confirmPassword.value = value
    }

    fun changePassword() {
        val user = FirebaseManager.auth.currentUser ?: return
        val email = user.email ?: return

        viewModelScope.launch {
            _uiState.value = ChangePasswordUiState.Loading
            try {
                // Re-authentication
                val credential = EmailAuthProvider.getCredential(email, _currentPassword.value)
                user.reauthenticate(credential).await()
                
                // Update password
                user.updatePassword(_newPassword.value).await()
                
                _uiState.value = ChangePasswordUiState.Success
                // Reset form
                _currentPassword.value = ""
                _newPassword.value = ""
                _confirmPassword.value = ""
            } catch (e: Exception) {
                _uiState.value = ChangePasswordUiState.Error(e.localizedMessage ?: "Errore durante il cambio password")
            }
        }
    }

    fun resetState() {
        _uiState.value = ChangePasswordUiState.Idle
    }
}
