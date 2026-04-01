package com.example.curiosillo.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ScopertaDao {
    @Query("SELECT * FROM scoperte ORDER BY dataScoperta DESC")
    fun getTutteFlow(): Flow<List<Scoperta>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun inserisci(scoperta: Scoperta): Long

    @Query("DELETE FROM scoperte")
    suspend fun resetTutte()
}
