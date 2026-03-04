package com.example.curiosillo.data

import androidx.room.Dao
import androidx.room.Query

@Dao
interface UserProgressDao {

    @Query("SELECT COUNT(*) FROM curiosity")
    suspend fun totalCuriosità(): Int

    @Query("SELECT COUNT(*) FROM curiosity WHERE isRead = 1")
    suspend fun curiositàImparate(): Int

    @Query("UPDATE curiosity SET isRead = 0")
    suspend fun resetProgressi()
}