package com.example.curiosillo.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "curiosillo_prefs")

class CategoryPreferences(private val context: Context) {

    companion object {
        val CATEGORIA_ATTIVA = stringPreferencesKey("categoria_attiva")
    }

    val categoriaAttiva: Flow<String> = context.dataStore.data
        .map { prefs -> prefs[CATEGORIA_ATTIVA] ?: "" }

    suspend fun setCategoria(categoria: String) {
        context.dataStore.edit { prefs ->
            prefs[CATEGORIA_ATTIVA] = categoria
        }
    }

    suspend fun resetCategoria() {
        context.dataStore.edit { prefs ->
            prefs[CATEGORIA_ATTIVA] = ""
        }
    }
}