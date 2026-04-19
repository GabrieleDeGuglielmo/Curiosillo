package com.example.curiosillo.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.gamificationStore by preferencesDataStore(name = "gamification_prefs")

class GamificationPreferences(private val context: Context) {

    companion object {
        private val XP_TOTALI = intPreferencesKey("xp_totali")
        private val STREAK_CORRENTE = intPreferencesKey("streak_corrente")
        private val STREAK_MASSIMA = intPreferencesKey("streak_massima")
        private val ULTIMO_ACCESSO = longPreferencesKey("ultimo_accesso")
        private val RISPOSTE_FILA = intPreferencesKey("risposte_fila")
        private val RECORD_SOPRAVVIVENZA = intPreferencesKey("record_sopravvivenza")
        private val PARTITE_SOPRAVVIVENZA = intPreferencesKey("partite_sopravvivenza")
        private val RECORD_SCALATA = intPreferencesKey("record_scalata")
        private val PARTITE_SCALATA = intPreferencesKey("partite_scalata")
        private val LAST_INTERACTED_EXT_ID = stringPreferencesKey("last_interacted_ext_id")
    }

    private val dataFlow: Flow<Preferences> = context.gamificationStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences())
            else throw exception
        }

    val xpTotali: Flow<Int> = dataFlow.map { it[XP_TOTALI] ?: 0 }
    val streakCorrente: Flow<Int> = dataFlow.map { it[STREAK_CORRENTE] ?: 0 }
    val streakMassima: Flow<Int> = dataFlow.map { it[STREAK_MASSIMA] ?: 0 }
    val risposteFila: Flow<Int> = dataFlow.map { it[RISPOSTE_FILA] ?: 0 }
    val recordSopravvivenza: Flow<Int> = dataFlow.map { it[RECORD_SOPRAVVIVENZA] ?: 0 }
    val partiteSopravvivenza: Flow<Int> = dataFlow.map { it[PARTITE_SOPRAVVIVENZA] ?: 0 }
    val recordScalata: Flow<Int> = dataFlow.map { it[RECORD_SCALATA] ?: 0 }
    val partiteScalata: Flow<Int> = dataFlow.map { it[PARTITE_SCALATA] ?: 0 }

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

    suspend fun incrementaPartiteSopravvivenza() {
        delay(100)
        context.gamificationStore.edit { prefs ->
            prefs[PARTITE_SOPRAVVIVENZA] = (prefs[PARTITE_SOPRAVVIVENZA] ?: 0) + 1
        }
    }

    suspend fun salvaRecordScalata(nuovoRecord: Int) {
        context.gamificationStore.edit { prefs ->
            val attuale = prefs[RECORD_SCALATA] ?: 0
            if (nuovoRecord > attuale) {
                prefs[RECORD_SCALATA] = nuovoRecord
            }
        }
    }

    suspend fun incrementaPartiteScalata() {
        context.gamificationStore.edit { prefs ->
            prefs[PARTITE_SCALATA] = (prefs[PARTITE_SOPRAVVIVENZA] ?: 0) + 1
        }
    }

    suspend fun registraLettura(): Boolean {
        var streakAumentata = false
        context.gamificationStore.edit { prefs ->
            val oggiMs = System.currentTimeMillis()
            val oggi = oggiMs / (24L * 60 * 60 * 1000)
            val ieri = oggi - 1
            val ultimoGiorno = prefs[ULTIMO_ACCESSO] ?: -1L

            val nuovaStreak = when (ultimoGiorno) {
                oggi -> prefs[STREAK_CORRENTE] ?: 1
                ieri -> {
                    streakAumentata = true
                    (prefs[STREAK_CORRENTE] ?: 0) + 1
                }
                else -> {
                    streakAumentata = true
                    1
                }
            }
            prefs[STREAK_CORRENTE] = nuovaStreak
            prefs[STREAK_MASSIMA] = maxOf(prefs[STREAK_MASSIMA] ?: 0, nuovaStreak)
            prefs[ULTIMO_ACCESSO] = oggi
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
            prefs[STREAK_MASSIMA] = maxOf(prefs[STREAK_MASSIMA] ?: 0, streakMassima)
        }
    }

    suspend fun getLastInteractedExternalId(): String? =
        dataFlow.first()[LAST_INTERACTED_EXT_ID]?.takeIf { it.isNotBlank() }

    suspend fun setLastInteractedExternalId(externalId: String) {
        context.gamificationStore.edit { it[LAST_INTERACTED_EXT_ID] = externalId }
    }

    suspend fun clearLastInteractedExternalId() {
        context.gamificationStore.edit { it[LAST_INTERACTED_EXT_ID] = "" }
    }

    suspend fun reset() {
        context.gamificationStore.edit { prefs ->
            prefs[XP_TOTALI] = 0
            prefs[STREAK_CORRENTE] = 0
            prefs[STREAK_MASSIMA] = 0
            prefs[ULTIMO_ACCESSO] = -1L
            prefs[RISPOSTE_FILA] = 0
            prefs[RECORD_SOPRAVVIVENZA] = 0
            prefs[PARTITE_SOPRAVVIVENZA] = 0
            prefs[RECORD_SCALATA] = 0
            prefs[PARTITE_SCALATA] = 0
            prefs[LAST_INTERACTED_EXT_ID] = ""
        }
    }
}
