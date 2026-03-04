package com.example.curiosillo.data

import androidx.room.*

@Dao
interface QuizQuestionDao {
    @Query("""
        SELECT q.* FROM quiz_question q
        INNER JOIN curiosity c ON q.curiosityId = c.id
        WHERE c.isRead = 1
        ORDER BY RANDOM() LIMIT :limit
    """)
    suspend fun getRandomFromRead(limit: Int): List<QuizQuestion>

    @Query("""
        SELECT COUNT(*) FROM quiz_question q
        INNER JOIN curiosity c ON q.curiosityId = c.id
        WHERE c.isRead = 1
    """)
    suspend fun countAvailable(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<QuizQuestion>)
}
