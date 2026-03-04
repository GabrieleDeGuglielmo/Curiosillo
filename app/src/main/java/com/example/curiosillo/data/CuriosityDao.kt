package com.example.curiosillo.data

import androidx.room.*

@Dao
interface CuriosityDao {
    @Query("""
        SELECT * FROM curiosity 
        WHERE (:category = '' OR category = :category)
        ORDER BY isRead ASC, RANDOM() LIMIT 1
    """)
    suspend fun getNext(category: String = ""): Curiosity?

    @Query("SELECT COUNT(*) FROM curiosity")
    suspend fun totaleCuriosità(): Int

    @Query("SELECT COUNT(*) FROM curiosity WHERE isRead = 1")
    suspend fun curiositàImparate(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<Curiosity>): List<Long>

    @Update
    suspend fun update(curiosity: Curiosity)

    @Query("SELECT * FROM curiosity WHERE isBookmarked = 1 ORDER BY title ASC")
    suspend fun getBookmarked(): List<Curiosity>

    @Query("SELECT COUNT(*) FROM curiosity WHERE isBookmarked = 1")
    suspend fun totaleBookmark(): Int

    @Query("SELECT DISTINCT category FROM curiosity ORDER BY category ASC")
    suspend fun getCategorie(): List<String>
}
