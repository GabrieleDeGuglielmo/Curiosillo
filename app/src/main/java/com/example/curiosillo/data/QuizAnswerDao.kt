package com.example.curiosillo.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface QuizAnswerDao {

    @Insert
    suspend fun inserisci(answer: QuizAnswer)

    // Domande su curiosità lette ma mai risposte
    @Query("""
        SELECT COUNT(*) FROM quiz_question q
        INNER JOIN curiosity c ON q.curiosityId = c.id
        WHERE c.isRead = 1
        AND q.id NOT IN (SELECT DISTINCT questionId FROM quiz_answer)
    """)
    suspend fun quizNonRisposti(): Int

    // Usato dal reset: cancella tutta la storia delle risposte
    @Query("DELETE FROM quiz_answer")
    suspend fun resetTutto()
}