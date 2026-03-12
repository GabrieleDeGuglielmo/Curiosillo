package com.example.curiosillo.viewmodel

import com.example.curiosillo.firebase.FirebaseManager

/**
 * Stato UI condiviso per la sezione commenti.
 * Usato da: CuriosityViewModel, RipassoViewModel.
 */
data class CommentiUiState(
    val commenti:     List<FirebaseManager.Commento> = emptyList(),
    val isLoading:    Boolean = false,
    val erroreInvio:  String? = null,
    val invioInCorso: Boolean = false
)
