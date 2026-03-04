package com.example.curiosillo.data

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update

@Dao
interface BookmarkDao {

    @Query("""
        SELECT * FROM curiosity 
        WHERE isBookmarked = 1
        AND (:categoria = '' OR category = :categoria)
        AND (
            :query = ''
            OR title LIKE '%' || :query || '%'
            OR body  LIKE '%' || :query || '%'
        )
        ORDER BY title ASC
    """)
    suspend fun cerca(query: String = "", categoria: String = ""): List<Curiosity>

    @Query("SELECT COUNT(*) FROM curiosity WHERE isBookmarked = 1")
    suspend fun totale(): Int

    @Query("SELECT DISTINCT category FROM curiosity WHERE isBookmarked = 1 ORDER BY category ASC")
    suspend fun categorieBookmark(): List<String>

    @Update
    suspend fun update(curiosity: Curiosity)
}