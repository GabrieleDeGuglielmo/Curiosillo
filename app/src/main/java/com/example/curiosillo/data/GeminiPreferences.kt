package com.example.curiosillo.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.curiosillo.firebase.FirebaseManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.geminiStore by preferencesDataStore(name = "gemini_prefs")

class GeminiPreferences(private val context: Context) {

    companion object {
        private val ULTIMO_UTILIZZO = longPreferencesKey("ultimo_utilizzo_gemini")
        private val CONTEGGIO_GIORNALIERO = intPreferencesKey("conteggio_giornaliero_gemini")
        private const val LIMITE_GIORNALIERO = 10
        private const val ADMIN_EMAIL = "gdg.gabriele@gmail.com"
        private const val MS_PER_DAY = 24L * 60 * 60 * 1000
    }

    private val dataFlow = context.geminiStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences())
            else throw exception
        }

    private fun isSpecialUser(): Boolean {
        return FirebaseManager.utenteCorrente?.email == ADMIN_EMAIL
    }

    /**
     * Verifica e incrementa atomicamente il conteggio giornaliero.
     * Restituisce true se l'uso è consentito (e il conteggio è stato incrementato),
     * false se il limite è stato raggiunto.
     */
    suspend fun canUseGemini(): Boolean {
        if (isSpecialUser()) return true

        var allowed = false
        context.geminiStore.edit { prefs ->
            val oggi = System.currentTimeMillis() / MS_PER_DAY
            val ultimoGiorno = (prefs[ULTIMO_UTILIZZO] ?: 0L) / MS_PER_DAY

            val currentCount = if (ultimoGiorno < oggi) {
                // Nuovo giorno, reset conteggio
                prefs[CONTEGGIO_GIORNALIERO] = 0
                0
            } else {
                prefs[CONTEGGIO_GIORNALIERO] ?: 0
            }

            allowed = currentCount < LIMITE_GIORNALIERO
        }
        return allowed
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

    fun getRemainingUsages(): Flow<Int> = dataFlow.map { prefs ->
        if (isSpecialUser()) return@map 999 // Indica infinito per la UI

        val oggi = System.currentTimeMillis() / MS_PER_DAY
        val ultimoGiorno = (prefs[ULTIMO_UTILIZZO] ?: 0L) / MS_PER_DAY
        if (ultimoGiorno < oggi) LIMITE_GIORNALIERO
        else {
            val count = prefs[CONTEGGIO_GIORNALIERO] ?: 0
            (LIMITE_GIORNALIERO - count).coerceAtLeast(0)
        }
    }
}
