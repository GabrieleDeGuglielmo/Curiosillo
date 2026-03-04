package com.example.curiosillo.data

import androidx.room.*

@Dao
interface BadgeDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun sblocca(badge: BadgeSbloccato)

    @Query("SELECT * FROM badge_sbloccato ORDER BY sbloccatoAt ASC")
    suspend fun tutti(): List<BadgeSbloccato>

    @Query("SELECT id FROM badge_sbloccato")
    suspend fun idSbloccati(): List<String>

    @Query("DELETE FROM badge_sbloccato")
    suspend fun resetTutto()
}
