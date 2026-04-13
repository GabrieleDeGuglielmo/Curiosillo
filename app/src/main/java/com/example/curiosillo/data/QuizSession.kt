package com.example.curiosillo.data

import androidx.compose.runtime.Immutable
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "quiz_session")
@Immutable
data class QuizSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val correctAnswers: Int,
    val totalAnswers:   Int,
    val categoria:      String = "",
    val playedAt:       Long   = System.currentTimeMillis()
)

@Immutable
data class StatCategoria(
    val category: String,
    val corrette: Int,
    val totale:   Int
)

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
