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
    @Query("""
        SELECT q.*, c.category FROM quiz_question q
        INNER JOIN curiosity c ON q.curiosityId = c.id
        WHERE c.isRead = 1
        ORDER BY RANDOM() LIMIT :limit
    """)
    suspend fun getRandomWithCategory(limit: Int): List<QuizQuestionWithCategory>

    @Query("""
        SELECT COUNT(*) FROM quiz_question q
        INNER JOIN curiosity c ON q.curiosityId = c.id
        WHERE c.isRead = 1
    """)
    suspend fun countAvailable(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<QuizQuestion>)
}
