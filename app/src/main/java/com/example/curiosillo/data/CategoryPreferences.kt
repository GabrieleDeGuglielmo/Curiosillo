package com.example.curiosillo.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "curiosillo_prefs")

class CategoryPreferences(private val context: Context) {

    companion object {
        val CATEGORIE_ATTIVE = stringSetPreferencesKey("categorie_attive")
    }

    // Restituisce un Set<String> vuoto se nessuna categoria è selezionata (= tutte)
    val categorieAttive: Flow<Set<String>> = context.dataStore.data
        .map { prefs -> prefs[CATEGORIE_ATTIVE] ?: emptySet() }

    suspend fun toggleCategoria(categoria: String) {
        context.dataStore.edit { prefs ->
            val correnti = prefs[CATEGORIE_ATTIVE]?.toMutableSet() ?: mutableSetOf()
            if (correnti.contains(categoria)) correnti.remove(categoria)
            else correnti.add(categoria)
            prefs[CATEGORIE_ATTIVE] = correnti
        }
    }

    suspend fun resetCategorie() {
        context.dataStore.edit { prefs ->
            prefs[CATEGORIE_ATTIVE] = emptySet()
        }
    }
}