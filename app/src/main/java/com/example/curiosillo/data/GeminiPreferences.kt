package com.example.curiosillo.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.curiosillo.firebase.FirebaseManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.geminiStore by preferencesDataStore(name = "gemini_prefs")

class GeminiPreferences(private val context: Context) {

    companion object {
        private val ULTIMO_UTILIZZO = longPreferencesKey("ultimo_utilizzo_gemini")
        private val CONTEGGIO_GIORNALIERO = intPreferencesKey("conteggio_giornaliero_gemini")
        private const val LIMITE_GIORNALIERO = 5
        private const val ADMIN_EMAIL = "gdg.gabriele@gmail.com"
    }

    private fun isSpecialUser(): Boolean {
        return FirebaseManager.utenteCorrente?.email == ADMIN_EMAIL
    }

    suspend fun canUseGemini(): Boolean {
        if (isSpecialUser()) return true

        val oggi = System.currentTimeMillis() / (24L * 60 * 60 * 1000)
        val prefs = context.geminiStore.data.first()
        val ultimoGiorno = (prefs[ULTIMO_UTILIZZO] ?: 0L) / (24L * 60 * 60 * 1000)
        
        val currentCount = prefs[CONTEGGIO_GIORNALIERO] ?: 0
        
        return if (ultimoGiorno < oggi) {
            // Nuovo giorno, reset conteggio
            context.geminiStore.edit { it[CONTEGGIO_GIORNALIERO] = 0 }
            true
        } else {
            currentCount < LIMITE_GIORNALIERO
        }
    }

    suspend fun incrementUsage() {
        if (isSpecialUser()) return

        context.geminiStore.edit { prefs ->
            val oggi = System.currentTimeMillis()
            val conteggio = prefs[CONTEGGIO_GIORNALIERO] ?: 0
            prefs[CONTEGGIO_GIORNALIERO] = conteggio + 1
            prefs[ULTIMO_UTILIZZO] = oggi
        }
    }

    fun getRemainingUsages(): Flow<Int> = context.geminiStore.data.map { prefs ->
        if (isSpecialUser()) return@map 999 // Indica infinito per la UI

        val oggi = System.currentTimeMillis() / (24L * 60 * 60 * 1000)
        val ultimoGiorno = (prefs[ULTIMO_UTILIZZO] ?: 0L) / (24L * 60 * 60 * 1000)
        if (ultimoGiorno < oggi) LIMITE_GIORNALIERO
        else {
            val count = prefs[CONTEGGIO_GIORNALIERO] ?: 0
            (LIMITE_GIORNALIERO - count).coerceAtLeast(0)
        }
    }
}
