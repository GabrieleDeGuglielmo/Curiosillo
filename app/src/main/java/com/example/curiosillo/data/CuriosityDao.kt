package com.example.curiosillo.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CuriosityDao {

    // ── Lettura ───────────────────────────────────────────────────────────────

    @Query("SELECT * FROM curiosity WHERE isRead = 1 AND externalId IS NOT NULL AND isIgnorata = 0")
    suspend fun getPilloleLette(): List<Curiosity>

    @Query("SELECT * FROM curiosity WHERE isRead = 0 AND isIgnorata = 0 AND externalId IS NOT NULL AND (:cat = '' OR category = :cat) ORDER BY RANDOM() LIMIT 1")
    suspend fun getNext(cat: String = ""): Curiosity?

    @Query("SELECT * FROM curiosity WHERE isRead = 0 AND isIgnorata = 0 AND externalId IS NOT NULL AND (category IN (:cats)) ORDER BY RANDOM() LIMIT 1")
    suspend fun getNextFiltered(cats: List<String>): Curiosity?

    @Query("SELECT * FROM curiosity WHERE isBookmarked = 1 ORDER BY id DESC")
    suspend fun getBookmarked(): List<Curiosity>

    @Query("""
        SELECT * FROM curiosity 
        WHERE isBookmarked = 1 
          AND (:query = '' OR title LIKE '%' || :query || '%' OR body LIKE '%' || :query || '%')
          AND (:tutte = 1 OR category IN (:cats))
        ORDER BY id DESC
    """)
    suspend fun searchBookmarked(query: String, cats: List<String>, tutte: Boolean): List<Curiosity>

    @Query("SELECT DISTINCT category FROM curiosity ORDER BY category")
    suspend fun getCategorie(): List<String>

    @Query("SELECT COUNT(*) FROM curiosity")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM curiosity WHERE isRead = 1 AND externalId IS NOT NULL AND isIgnorata = 0")
    suspend fun curiositàImparate(): Int

    @Query("SELECT COUNT(*) FROM curiosity WHERE externalId IS NOT NULL AND isIgnorata = 0")
    suspend fun totaleCuriosità(): Int

    @Query("SELECT COUNT(*) FROM curiosity WHERE isBookmarked = 1 AND externalId IS NOT NULL")
    suspend fun totaleBookmark(): Int

    @Query("SELECT COUNT(*) FROM curiosity WHERE isIgnorata = 1")
    suspend fun totaleIgnorate(): Int

    @Query("SELECT * FROM curiosity WHERE isIgnorata = 1 ORDER BY id DESC")
    suspend fun getPilloleIgnorate(): List<Curiosity>

    @Query("SELECT * FROM curiosity")
    suspend fun getTutte(): List<Curiosity>

    @Query("SELECT * FROM curiosity WHERE isRead = 1 AND isIgnorata = 0 AND (:cat = '' OR category = :cat) ORDER BY CASE WHEN readAt IS NULL THEN 1 ELSE 0 END, readAt ASC")
    suspend fun getTutteImparate(cat: String = ""): List<Curiosity>

    @Query("SELECT * FROM curiosity WHERE isRead = 1 AND isIgnorata = 0 AND (:cat = '' OR category = :cat) AND (readAt IS NULL OR readAt <= :soglia) ORDER BY CASE WHEN readAt IS NULL THEN 1 ELSE 0 END, readAt ASC")
    suspend fun getPerRipasso(soglia: Long, cat: String = ""): List<Curiosity>

    @Query("SELECT * FROM curiosity WHERE isRead = 1 AND isIgnorata = 0 AND (category IN (:cats)) AND (readAt IS NULL OR readAt <= :soglia) ORDER BY CASE WHEN readAt IS NULL THEN 1 ELSE 0 END, readAt ASC")
    suspend fun getPerRipassoFiltered(soglia: Long, cats: List<String>): List<Curiosity>

    // ── Lettura REATTIVA (per Ripasso e UI) ───────────────────────────────────

    @Query("SELECT * FROM curiosity WHERE isRead = 1 AND isIgnorata = 0 AND (:cat = '' OR category = :cat) AND (readAt IS NULL OR readAt <= :soglia) ORDER BY CASE WHEN readAt IS NULL THEN 1 ELSE 0 END, readAt ASC")
    fun getPerRipassoFlow(soglia: Long, cat: String = ""): Flow<List<Curiosity>>

    @Query("SELECT * FROM curiosity WHERE isRead = 1 AND isIgnorata = 0 AND (category IN (:cats)) AND (readAt IS NULL OR readAt <= :soglia) ORDER BY CASE WHEN readAt IS NULL THEN 1 ELSE 0 END, readAt ASC")
    fun getPerRipassoFilteredFlow(soglia: Long, cats: List<String>): Flow<List<Curiosity>>

    // ── Sync remoto ───────────────────────────────────────────────────────────

    @Query("SELECT * FROM curiosity WHERE externalId = :externalId LIMIT 1")
    suspend fun getByExternalId(externalId: String): Curiosity?

    @Query("DELETE FROM curiosity WHERE externalId NOT IN (:ids) AND externalId IS NOT NULL")
    suspend fun deleteMissing(ids: List<String>)

    @Query("DELETE FROM curiosity WHERE externalId = :externalId")
    suspend fun deleteByExternalId(externalId: String)

    // ── Scrittura ─────────────────────────────────────────────────────────────

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<Curiosity>): List<Long>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: Curiosity): Long

    @Update
    suspend fun update(item: Curiosity)

    @Query("UPDATE curiosity SET isRead = 0, isBookmarked = 0, nota = '', readAt = NULL, voto = NULL, isIgnorata = 0 WHERE 1=1")
    suspend fun resetProgressi()
}
