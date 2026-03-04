package com.example.curiosillo.data

import androidx.room.*

@Dao
interface CuriosityDao {
    @Query("SELECT * FROM curiosity ORDER BY isRead ASC, RANDOM() LIMIT 1")
    suspend fun getNext(): Curiosity?

    @Query("SELECT COUNT(*) FROM curiosity")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM curiosity WHERE isRead = 1")
    suspend fun countRead(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<Curiosity>): List<Long>

    @Update
    suspend fun update(curiosity: Curiosity)
}
