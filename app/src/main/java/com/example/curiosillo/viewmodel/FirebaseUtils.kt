package com.example.curiosillo.viewmodel

import com.example.curiosillo.firebase.FirebaseManager
import com.example.curiosillo.ui.screens.utils.CommentiUiState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

/**
 * Helper per gestire la logica dei commenti e di Firebase
 * in modo centralizzato tra i vari ViewModel.
 */
object FirebaseHelper {

    /**
     * Carica i commenti da Firebase per una specifica pillola.
     */
    fun caricaCommenti(
        scope: CoroutineScope,
        commentiState: MutableStateFlow<CommentiUiState>,
        externalId: String
    ) {
        scope.launch {
            commentiState.value = commentiState.value.copy(isLoading = true)
            val commenti = FirebaseManager.caricaCommenti(externalId)
            commentiState.value = commentiState.value.copy(commenti = commenti, isLoading = false)
        }
    }

    /**
     * Invia un nuovo commento a Firebase.
     */
    fun inviaCommento(
        scope: CoroutineScope,
        commentiState: MutableStateFlow<CommentiUiState>,
        externalId: String,
        testo: String
    ) {
        if (FirebaseManager.contienePaloroleVietate(testo)) {
            commentiState.value = commentiState.value.copy(
                erroreInvio = "Il commento contiene parole non consentite."
            )
            return
        }

        scope.launch {
            commentiState.value = commentiState.value.copy(invioInCorso = true, erroreInvio = null)
            val result = FirebaseManager.aggiungiCommento(externalId, testo)
            if (result.isSuccess) {
                val commenti = FirebaseManager.caricaCommenti(externalId)
                commentiState.value = commentiState.value.copy(commenti = commenti, invioInCorso = false)
            } else {
                commentiState.value = commentiState.value.copy(
                    invioInCorso = false,
                    erroreInvio  = "Errore durante l'invio. Riprova."
                )
            }
        }
    }

    /**
     * Elimina un commento esistente.
     */
    fun eliminaCommento(
        scope: CoroutineScope,
        commentiState: MutableStateFlow<CommentiUiState>,
        externalId: String,
        commentoId: String
    ) {
        scope.launch {
            FirebaseManager.eliminaCommento(externalId, commentoId)
            val commenti = FirebaseManager.caricaCommenti(externalId)
            commentiState.value = commentiState.value.copy(commenti = commenti)
        }
    }
}
