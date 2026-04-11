package com.example.curiosillo.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first

private val Context.gamificationStore by preferencesDataStore(name = "gamification_prefs")

class GamificationPreferences(private val context: Context) {

    companion object {
        val XP_TOTALI       = intPreferencesKey("xp_totali")
        val STREAK_CORRENTE = intPreferencesKey("streak_corrente")
        val STREAK_MASSIMA  = intPreferencesKey("streak_massima")
        val ULTIMO_ACCESSO  = longPreferencesKey("ultimo_accesso")
        val RISPOSTE_FILA          = intPreferencesKey("risposte_fila")
        val RECORD_SOPRAVVIVENZA   = intPreferencesKey("record_sopravvivenza")
        val LAST_INTERACTED_EXT_ID = stringPreferencesKey("last_interacted_ext_id")
    }

    val xpTotali:       Flow<Int> = context.gamificationStore.data.map { it[XP_TOTALI]       ?: 0 }
    val streakCorrente: Flow<Int> = context.gamificationStore.data.map { it[STREAK_CORRENTE] ?: 0 }
    val streakMassima:  Flow<Int> = context.gamificationStore.data.map { it[STREAK_MASSIMA]  ?: 0 }
    val risposteFila:   Flow<Int> = context.gamificationStore.data.map { it[RISPOSTE_FILA]   ?: 0 }
    val recordSopravvivenza: Flow<Int> = context.gamificationStore.data.map { it[RECORD_SOPRAVVIVENZA] ?: 0 }

    suspend fun aggiungiXp(quantita: Int) {
        context.gamificationStore.edit { prefs ->
            prefs[XP_TOTALI] = (prefs[XP_TOTALI] ?: 0) + quantita
        }
    }

    suspend fun salvaRecordSopravvivenza(nuovoRecord: Int) {
        context.gamificationStore.edit { prefs ->
            val attuale = prefs[RECORD_SOPRAVVIVENZA] ?: 0
            if (nuovoRecord > attuale) {
                prefs[RECORD_SOPRAVVIVENZA] = nuovoRecord
            }
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

    suspend fun setStreakDaCloud(streakCorrente: Int, streakMassima: Int) {
        context.gamificationStore.edit { prefs ->
            prefs[STREAK_CORRENTE] = streakCorrente
            prefs[STREAK_MASSIMA]  = maxOf(prefs[STREAK_MASSIMA] ?: 0, streakMassima)
        }
    }

    suspend fun getLastInteractedExternalId(): String? =
        context.gamificationStore.data.first()[LAST_INTERACTED_EXT_ID]?.takeIf { it.isNotBlank() }

    suspend fun setLastInteractedExternalId(externalId: String) {
        context.gamificationStore.edit { it[LAST_INTERACTED_EXT_ID] = externalId }
    }

    suspend fun clearLastInteractedExternalId() {
        context.gamificationStore.edit { it[LAST_INTERACTED_EXT_ID] = "" }
    }

    suspend fun reset() {
        context.gamificationStore.edit { prefs ->
            prefs[XP_TOTALI]       = 0
            prefs[STREAK_CORRENTE] = 0
            prefs[STREAK_MASSIMA]  = 0
            prefs[ULTIMO_ACCESSO]  = -1L
            prefs[RISPOSTE_FILA]          = 0
            prefs[RECORD_SOPRAVVIVENZA]   = 0
            prefs[LAST_INTERACTED_EXT_ID] = ""
        }
    }
}