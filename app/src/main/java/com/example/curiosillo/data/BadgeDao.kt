package com.example.curiosillo.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BadgeDao {

    @Query("SELECT * FROM badge_sbloccato ORDER BY sbloccatoAt ASC")
    suspend fun getTutti(): List<BadgeSbloccato>

    @Query("SELECT * FROM badge_sbloccato ORDER BY sbloccatoAt ASC")
    fun getTuttiFlow(): Flow<List<BadgeSbloccato>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun inserisci(badge: BadgeSbloccato)

    @Query("SELECT EXISTS(SELECT 1 FROM badge_sbloccato WHERE id = :id)")
    suspend fun esiste(id: String): Boolean

    @Query("DELETE FROM badge_sbloccato")
    suspend fun resetTutti()
}
