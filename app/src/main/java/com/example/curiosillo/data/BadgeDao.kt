package com.example.curiosillo.data

import androidx.room.*

@Dao
interface BadgeDao {

    @Query("SELECT * FROM badge_sbloccato ORDER BY sbloccatoAt ASC")
    suspend fun getTutti(): List<BadgeSbloccato>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun inserisci(badge: BadgeSbloccato)

    @Query("SELECT COUNT(*) > 0 FROM badge_sbloccato WHERE id = :id")
    suspend fun esiste(id: String): Boolean
}
