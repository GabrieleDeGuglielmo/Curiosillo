package com.example.curiosillo.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.gamificationStore by preferencesDataStore(name = "gamification_prefs")

class GamificationPreferences(private val context: Context) {

    companion object {
        val XP_TOTALI       = intPreferencesKey("xp_totali")
        val STREAK_CORRENTE = intPreferencesKey("streak_corrente")
        val STREAK_MASSIMA  = intPreferencesKey("streak_massima")
        val ULTIMO_ACCESSO  = longPreferencesKey("ultimo_accesso")
        val RISPOSTE_FILA   = intPreferencesKey("risposte_fila")
    }

    val xpTotali:       Flow<Int> = context.gamificationStore.data.map { it[XP_TOTALI]       ?: 0 }
    val streakCorrente: Flow<Int> = context.gamificationStore.data.map { it[STREAK_CORRENTE] ?: 0 }
    val streakMassima:  Flow<Int> = context.gamificationStore.data.map { it[STREAK_MASSIMA]  ?: 0 }
    val risposteFila:   Flow<Int> = context.gamificationStore.data.map { it[RISPOSTE_FILA]   ?: 0 }

    suspend fun aggiungiXp(quantita: Int) {
        context.gamificationStore.edit { prefs ->
            prefs[XP_TOTALI] = (prefs[XP_TOTALI] ?: 0) + quantita
        }
    }

    // Ritorna true se la streak è aumentata (prima lettura del giorno)
    suspend fun registraLettura(): Boolean {
        var streakAumentata = false
        context.gamificationStore.edit { prefs ->
            val oggiMs       = System.currentTimeMillis()
            val oggi         = oggiMs / (24L * 60 * 60 * 1000)
            val ieri         = oggi - 1
            val ultimoGiorno = prefs[ULTIMO_ACCESSO] ?: -1L

            val nuovaStreak = when (ultimoGiorno) {
                oggi -> prefs[STREAK_CORRENTE] ?: 1
                ieri -> {
                    streakAumentata = true
                    (prefs[STREAK_CORRENTE] ?: 0) + 1
                }
                else -> {
                    // Al primo avvio assoluto (ultimoGiorno == -1L), 
                    // impostiamo la streak a 1 e consideriamo l'aumento vero.
                    streakAumentata = true
                    1
                }
            }
            prefs[STREAK_CORRENTE] = nuovaStreak
            prefs[STREAK_MASSIMA]  = maxOf(prefs[STREAK_MASSIMA] ?: 0, nuovaStreak)
            prefs[ULTIMO_ACCESSO]  = oggi
        }
        return streakAumentata
    }

    suspend fun aggiornaRisposteFila(corretta: Boolean): Int {
        var nuovoValore = 0
        context.gamificationStore.edit { prefs ->
            nuovoValore = if (corretta) (prefs[RISPOSTE_FILA] ?: 0) + 1 else 0
            prefs[RISPOSTE_FILA] = nuovoValore
        }
        return nuovoValore
    }

    suspend fun reset() {
        context.gamificationStore.edit { prefs ->
            prefs[XP_TOTALI]       = 0
            prefs[STREAK_CORRENTE] = 0
            prefs[STREAK_MASSIMA]  = 0
            prefs[ULTIMO_ACCESSO]  = -1L
            prefs[RISPOSTE_FILA]   = 0
        }
    }
}
