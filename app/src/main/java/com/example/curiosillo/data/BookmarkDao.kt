package com.example.curiosillo.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface BookmarkDao {

    @Query(
        """
    SELECT * FROM curiosity 
    WHERE isBookmarked = 1
    AND (:tutte = 1 OR category IN (:categorie))
    AND (
        :query = ''
        OR title LIKE '%' || :query || '%'
        OR body  LIKE '%' || :query || '%'
    )
    ORDER BY title ASC
"""
    )
    suspend fun cerca(
        query: String = "",
        categorie: List<String>,
        tutte: Int
    ): List<Curiosity>

    @Query("SELECT * FROM curiosity WHERE isBookmarked = 1 ORDER BY title ASC")
    fun getTuttiFlow(): Flow<List<Curiosity>>

    @Query("SELECT COUNT(*) FROM curiosity WHERE isBookmarked = 1")
    suspend fun totale(): Int

    @Query("SELECT COUNT(*) FROM curiosity WHERE isBookmarked = 1")
    fun totaleFlow(): Flow<Int>

    @Query("SELECT DISTINCT category FROM curiosity WHERE isBookmarked = 1 ORDER BY category ASC")
    suspend fun categorieBookmark(): List<String>

    @Update
    suspend fun update(curiosity: Curiosity)
}
