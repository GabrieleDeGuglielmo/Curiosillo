package com.example.curiosillo.data

import androidx.room.*

@Dao
interface QuizQuestionDao {

    @Query("""
    SELECT qq.* FROM quiz_question qq
    INNER JOIN curiosity c ON c.id = qq.curiosityId
    LEFT JOIN quiz_answer qa ON qa.questionId = qq.id
    WHERE qa.id IS NULL
      AND c.isRead = 1
      AND c.isIgnorata = 0
      AND (:cat = '' OR qq.category = :cat)
    ORDER BY RANDOM()
    LIMIT :n
""")
    suspend fun getRandomWithCategory(n: Int, cat: String = ""): List<QuizQuestion>

    @Query("""
    SELECT qq.* FROM quiz_question qq
    INNER JOIN curiosity c ON c.id = qq.curiosityId
    LEFT JOIN quiz_answer qa ON qa.questionId = qq.id
    WHERE qa.id IS NULL
      AND c.isRead = 1
      AND c.isIgnorata = 0
      AND (qq.category IN (:cats))
    ORDER BY RANDOM()
    LIMIT :n
""")
    suspend fun getRandomFiltered(n: Int, cats: List<String>): List<QuizQuestion>

    @Query("""
    SELECT COUNT(*) FROM quiz_question qq
    INNER JOIN curiosity c ON c.id = qq.curiosityId
    LEFT JOIN quiz_answer qa ON qa.questionId = qq.id
    WHERE qa.id IS NULL
      AND c.isRead = 1
      AND c.isIgnorata = 0
      AND (:cat = '' OR qq.category = :cat)
""")
    suspend fun countAvailable(cat: String = ""): Int

    @Query("""
    SELECT COUNT(*) FROM quiz_question qq
    INNER JOIN curiosity c ON c.id = qq.curiosityId
    LEFT JOIN quiz_answer qa ON qa.questionId = qq.id
    WHERE qa.id IS NULL
      AND c.isRead = 1
      AND c.isIgnorata = 0
""")
    suspend fun quizNonRisposti(): Int

    // Sync remoto
    @Query("SELECT * FROM quiz_question WHERE curiosityId = :curiosityId LIMIT 1")
    suspend fun getByCuriosityId(curiosityId: Int): QuizQuestion?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<QuizQuestion>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: QuizQuestion): Long

    @Update
    suspend fun update(item: QuizQuestion)
}