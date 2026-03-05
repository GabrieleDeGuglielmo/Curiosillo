package com.example.curiosillo.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface CuriosityDao {

    @Insert
    suspend fun insertAll(list: List<Curiosity>): List<Long>

    @Update
    suspend fun update(c: Curiosity)

    @Query("SELECT COUNT(*) FROM curiosity")
    suspend fun totaleCuriosità(): Int

    @Query("SELECT COUNT(*) FROM curiosity WHERE isRead = 1")
    suspend fun curiositàImparate(): Int

    @Query("SELECT COUNT(*) FROM curiosity WHERE isBookmarked = 1")
    suspend fun totaleBookmark(): Int

    @Query("""
        SELECT * FROM curiosity
        WHERE (:tutte = 1 OR category IN (:categorie))
        ORDER BY isRead ASC, RANDOM() LIMIT 1
    """)
    suspend fun getNext(categorie: List<String>, tutte: Int): Curiosity?

    @Query("SELECT * FROM curiosity WHERE isBookmarked = 1")
    suspend fun getBookmarked(): List<Curiosity>

    @Query("SELECT DISTINCT category FROM curiosity ORDER BY category ASC")
    suspend fun getCategorie(): List<String>

    // ── Ripasso ───────────────────────────────────────────────────────────────
    // Restituisce pillole lette più di minDaysAgo giorni fa, ordinate dalla più vecchia
    @Query("""
        SELECT * FROM curiosity
        WHERE isRead = 1
        AND readAt > 0
        AND readAt < :soglia
        AND (:tutte = 1 OR category IN (:categorie))
        ORDER BY readAt ASC
    """)
    suspend fun getPerRipasso(
        soglia:    Long,
        categorie: List<String>,
        tutte:     Int
    ): List<Curiosity>

    @Query("""
        SELECT * FROM curiosity
        WHERE isRead = 1
        AND (:tutte = 1 OR category IN (:categorie))
        ORDER BY readAt ASC
    """)
    suspend fun getTutteImparate(
        categorie: List<String>,
        tutte:     Int
    ): List<Curiosity>
}
