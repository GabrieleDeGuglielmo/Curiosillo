package com.example.curiosillo.data

import androidx.room.*

data class QuizQuestionWithCategory(
    val id: Int,
    val curiosityId: Int,
    val questionText: String,
    val correctAnswer: String,
    val wrongAnswer1: String,
    val wrongAnswer2: String,
    val wrongAnswer3: String,
    val explanation: String,
    val category: String
)

@Dao
interface QuizQuestionDao {

    @Query(
        """
    SELECT q.*, c.category FROM quiz_question q
    INNER JOIN curiosity c ON q.curiosityId = c.id
    WHERE c.isRead = 1
    AND (:tutte = 1 OR c.category IN (:categorie))
    ORDER BY RANDOM() LIMIT :limit
"""
    )
    suspend fun getRandomWithCategory(
        limit: Int,
        categorie: List<String>,
        tutte: Int
    ): List<QuizQuestionWithCategory>

    @Query(
        """
    SELECT COUNT(*) FROM quiz_question q
    INNER JOIN curiosity c ON q.curiosityId = c.id
    WHERE c.isRead = 1
    AND (:tutte = 1 OR c.category IN (:categorie))
"""
    )
    suspend fun countAvailable(categorie: List<String>, tutte: Int): Int

    @Query(
        """
    SELECT COUNT(*) FROM quiz_question q
    INNER JOIN curiosity c ON q.curiosityId = c.id
    WHERE c.isRead = 1
    AND q.id NOT IN (SELECT DISTINCT questionId FROM quiz_answer)
    AND (:tutte = 1 OR c.category IN (:categorie))
"""
    )
    suspend fun quizNonRisposti(categorie: List<String>, tutte: Int): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<QuizQuestion>)
}
