package com.example.curiosillo.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.contentDataStore by preferencesDataStore(name = "content_prefs")

class ContentPreferences(private val context: Context) {

    companion object {
        private val KEY_CONTENT_VERSION       = intPreferencesKey("content_version")
        private val KEY_CLOUD_MIGRAZIONE_DONE = intPreferencesKey("cloud_migrazione_done")
    }

    /** Restituisce la versione del contenuto attualmente salvata in locale (0 = mai scaricato). */
    suspend fun getContentVersion(): Int =
        context.contentDataStore.data.first()[KEY_CONTENT_VERSION] ?: 0

    /** Salva la versione dopo un aggiornamento riuscito. */
    suspend fun setContentVersion(version: Int) {
        context.contentDataStore.edit { it[KEY_CONTENT_VERSION] = version }
    }
    /** True se la migrazione one-shot verso Firebase e' gia' stata eseguita. */
    suspend fun isCloudMigrazioneDone(): Boolean =
        (context.contentDataStore.data.first()[KEY_CLOUD_MIGRAZIONE_DONE] ?: 0) == 1

    suspend fun setCloudMigrazioneCompletata() {
        context.contentDataStore.edit { it[KEY_CLOUD_MIGRAZIONE_DONE] = 1 }
    }

    suspend fun getUltimaVersioneVista(): String =
        context.contentDataStore.data.first()[androidx.datastore.preferences.core.stringPreferencesKey("ultima_versione_vista")] ?: ""

    suspend fun setUltimaVersioneVista(versione: String) {
        context.contentDataStore.edit {
            it[androidx.datastore.preferences.core.stringPreferencesKey("ultima_versione_vista")] = versione
        }
    }
}