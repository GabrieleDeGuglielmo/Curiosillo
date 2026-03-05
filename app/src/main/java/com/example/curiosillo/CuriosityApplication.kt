package com.example.curiosillo

import android.app.Application
import com.example.curiosillo.data.AppDatabase
import com.example.curiosillo.data.CategoryPreferences
import com.example.curiosillo.data.ContentPreferences
import com.example.curiosillo.data.GamificationPreferences
import com.example.curiosillo.data.ThemePreferences
import com.example.curiosillo.domain.GamificationEngine
import com.example.curiosillo.repository.CuriosityRepository
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class CuriosityApplication : Application() {

    val database          by lazy { AppDatabase.getDatabase(this) }
    val repository        by lazy { CuriosityRepository(database) }
    val categoryPrefs     by lazy { CategoryPreferences(this) }
    val gamificationPrefs by lazy { GamificationPreferences(this) }
    val themePrefs        by lazy { ThemePreferences(this) }
    val contentPrefs      by lazy { ContentPreferences(this) }

    val gamificationEngine by lazy {
        GamificationEngine(gamificationPrefs, repository)
    }

    override fun onCreate() {
        super.onCreate()
        // Inizializza Firebase — deve essere la prima cosa
        FirebaseApp.initializeApp(this)

        CoroutineScope(Dispatchers.IO).launch {
            repository.initializeSeedData()
        }
    }
}
