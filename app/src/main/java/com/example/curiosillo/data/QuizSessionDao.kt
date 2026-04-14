package com.example.curiosillo.data

import androidx.compose.runtime.Immutable
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface QuizSessionDao {

    @Insert
    suspend fun inserisci(session: QuizSession)

    @Query("SELECT * FROM quiz_session ORDER BY playedAt DESC LIMIT 20")
    suspend fun ultime20(): List<QuizSession>

    @Query("""
        SELECT categoria as category,
               SUM(correctAnswers) as corrette,
               SUM(totalAnswers) as totale
        FROM quiz_session
        WHERE categoria != ''
        GROUP BY categoria
        ORDER BY totale DESC
    """)
    suspend fun statPerCategoria(): List<StatCategoria>
}

@Immutable
data class StatCategoria(
    val category: String,
    val corrette: Int,
    val totale:   Int
)
