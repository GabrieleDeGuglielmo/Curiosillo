package com.example.curiosillo.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

private val Context.themeStore by preferencesDataStore(name = "theme_prefs")

class ThemePreferences(private val context: Context) {

    companion object {
        private val DARK_MODE = booleanPreferencesKey("dark_mode")
        private val MUSIC_ENABLED = booleanPreferencesKey("music_enabled")
    }

    val isDarkMode: Flow<Boolean> = context.themeStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences())
            else throw exception
        }
        .map { prefs -> prefs[DARK_MODE] ?: false }

    val isMusicEnabled: Flow<Boolean> = context.themeStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences())
            else throw exception
        }
        .map { prefs -> prefs[MUSIC_ENABLED] ?: true }

    suspend fun setDarkMode(enabled: Boolean) {
        context.themeStore.edit { prefs ->
            prefs[DARK_MODE] = enabled
        }
    }

    suspend fun setMusicEnabled(enabled: Boolean) {
        context.themeStore.edit { prefs ->
            prefs[MUSIC_ENABLED] = enabled
        }
    }
}
