package com.example.curiosillo.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query

@Entity(tableName = "quiz_session")
data class QuizSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val correctAnswers: Int,
    val totalAnswers:   Int,
    val categoria:      String = "",
    val playedAt:       Long   = System.currentTimeMillis()
)

data class StatCategoria(
    val category:  String,
    val corrette:  Int,
    val totale:    Int
)

@Dao
interface QuizSessionDao {

    @Insert
    suspend fun inserisci(session: QuizSession)

    @Query("SELECT * FROM quiz_session ORDER BY playedAt DESC LIMIT 20")
    suspend fun ultime20(): List<QuizSession>

    @Query("""
        SELECT c.category as category,
               SUM(CASE WHEN qa.isCorrect = 1 THEN 1 ELSE 0 END) as corrette,
               COUNT(qa.id) as totale
        FROM quiz_answer qa
        INNER JOIN quiz_question qq ON qa.questionId = qq.id
        INNER JOIN curiosity c ON qq.curiosityId = c.id
        GROUP BY c.category
        ORDER BY corrette DESC
    """)
    suspend fun statPerCategoria(): List<StatCategoria>

    @Query("DELETE FROM quiz_session")
    suspend fun resetTutto()
}
