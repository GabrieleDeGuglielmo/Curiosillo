package com.example.curiosillo.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.contentDataStore by preferencesDataStore(name = "content_prefs")

class ContentPreferences(private val context: Context) {

    companion object {
        private val KEY_CONTENT_VERSION = intPreferencesKey("content_version")
        private val KEY_CLOUD_MIGRAZIONE_DONE = intPreferencesKey("cloud_migrazione_done")
        private val KEY_MOSTRA_POPUP_AR = booleanPreferencesKey("mostra_popup_ar")
        private val KEY_ULTIMA_VERSIONE_VISTA = stringPreferencesKey("ultima_versione_vista")
    }

    private val dataFlow = context.contentDataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences())
            else throw exception
        }

    /** Restituisce la versione del contenuto attualmente salvata in locale (0 = mai scaricato). */
    suspend fun getContentVersion(): Int =
        dataFlow.first()[KEY_CONTENT_VERSION] ?: 0

    /** Salva la versione dopo un aggiornamento riuscito. */
    suspend fun setContentVersion(version: Int) {
        context.contentDataStore.edit { it[KEY_CONTENT_VERSION] = version }
    }

    /** True se la migrazione one-shot verso Firebase e' gia' stata eseguita. */
    suspend fun isCloudMigrazioneDone(): Boolean =
        (dataFlow.first()[KEY_CLOUD_MIGRAZIONE_DONE] ?: 0) == 1

    suspend fun setCloudMigrazioneCompletata() {
        context.contentDataStore.edit { it[KEY_CLOUD_MIGRAZIONE_DONE] = 1 }
    }

    suspend fun resetCloudMigrazione() {
        context.contentDataStore.edit { it[KEY_CLOUD_MIGRAZIONE_DONE] = 0 }
    }

    suspend fun getUltimaVersioneVista(): String =
        dataFlow.first()[KEY_ULTIMA_VERSIONE_VISTA] ?: ""

    suspend fun setUltimaVersioneVista(versione: String) {
        context.contentDataStore.edit {
            it[KEY_ULTIMA_VERSIONE_VISTA] = versione
        }
    }

    val mostraPopupAR: Flow<Boolean> = dataFlow.map {
        it[KEY_MOSTRA_POPUP_AR] ?: true
    }

    suspend fun disabilitaPopupAR() {
        context.contentDataStore.edit { it[KEY_MOSTRA_POPUP_AR] = false }
    }
}
