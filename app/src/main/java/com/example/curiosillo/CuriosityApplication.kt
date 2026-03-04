package com.example.curiosillo

import android.app.Application
import com.example.curiosillo.data.AppDatabase
import com.example.curiosillo.data.CategoryPreferences
import com.example.curiosillo.data.GamificationPreferences
import com.example.curiosillo.domain.GamificationEngine
import com.example.curiosillo.repository.CuriosityRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CuriosityApplication : Application() {
    private val scope = CoroutineScope(SupervisorJob())

    val database           by lazy { AppDatabase.getDatabase(this) }
    val repository         by lazy { CuriosityRepository(database) }
    val categoryPrefs      by lazy { CategoryPreferences(this) }
    val gamificationPrefs  by lazy { GamificationPreferences(this) }
    val gamificationEngine by lazy { GamificationEngine(gamificationPrefs, repository) }

    override fun onCreate() {
        super.onCreate()
        scope.launch { repository.initializeSeedData() }
    }
}
