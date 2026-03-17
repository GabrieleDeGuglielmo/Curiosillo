package com.example.curiosillo

import android.app.Application
import com.example.curiosillo.data.AppDatabase
import com.example.curiosillo.data.CategoryPreferences
import com.example.curiosillo.data.ContentPreferences
import com.example.curiosillo.data.GamificationPreferences
import com.example.curiosillo.data.ThemePreferences
import com.example.curiosillo.data.GeminiPreferences
import com.example.curiosillo.domain.GamificationEngine
import com.example.curiosillo.repository.CuriosityRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CuriosityApplication : Application() {

    // Scope legato al ciclo di vita dell'app: sopravvive ai ViewModel
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { CuriosityRepository(database, applicationScope) }

    val categoryPrefs    by lazy { CategoryPreferences(this) }
    val gamificationPrefs by lazy { GamificationPreferences(this) }
    val themePrefs       by lazy { ThemePreferences(this) }
    val contentPrefs     by lazy { ContentPreferences(this) }
    val geminiPrefs       by lazy { GeminiPreferences(this) }

    val gamificationEngine by lazy {
        GamificationEngine(gamificationPrefs, repository, contentPrefs)
    }

    override fun onCreate() {
        super.onCreate()
        // Al primo avvio inizializza il seed (ora vuoto — i contenuti arrivano dal remoto)
        applicationScope.launch {
            repository.initializeSeedData()
        }
    }
}