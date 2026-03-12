package com.example.curiosillo.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.contentDataStore by preferencesDataStore(name = "content_prefs")

class ContentPreferences(private val context: Context) {

    companion object {
        private val KEY_CONTENT_VERSION        = intPreferencesKey("content_version")
        private val KEY_ULTIMA_VERSIONE_VISTA   = stringPreferencesKey("ultima_versione_changelog_vista")
    }

    /** Restituisce la versione del contenuto attualmente salvata in locale (0 = mai scaricato). */
    suspend fun getContentVersion(): Int =
        context.contentDataStore.data.first()[KEY_CONTENT_VERSION] ?: 0

    /** Salva la versione dopo un aggiornamento riuscito. */
    suspend fun setContentVersion(version: Int) {
        context.contentDataStore.edit { it[KEY_CONTENT_VERSION] = version }
    }

    /** Ultima versione del changelog già mostrata all'utente (es. "1.4"). */
    suspend fun getUltimaVersioneVista(): String =
        context.contentDataStore.data.first()[KEY_ULTIMA_VERSIONE_VISTA] ?: ""

    /** Salva la versione dopo aver mostrato il popup changelog. */
    suspend fun setUltimaVersioneVista(versione: String) {
        context.contentDataStore.edit { it[KEY_ULTIMA_VERSIONE_VISTA] = versione }
    }
}