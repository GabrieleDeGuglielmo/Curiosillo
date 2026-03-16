package com.example.curiosillo.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update


@Dao
interface QuizQuestionDao {

    @Query("""
    SELECT qq.* FROM quiz_question qq
    INNER JOIN curiosity c ON c.id = qq.curiosityId
    LEFT JOIN quiz_answer qa ON qa.questionId = qq.id AND qa.isCorrect = 1
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
    LEFT JOIN quiz_answer qa ON qa.questionId = qq.id AND qa.isCorrect = 1
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
    LEFT JOIN quiz_answer qa ON qa.questionId = qq.id AND qa.isCorrect = 1
    WHERE qa.id IS NULL
      AND c.isRead = 1
      AND c.isIgnorata = 0
      AND (:cat = '' OR qq.category = :cat)
""")
    suspend fun countAvailable(cat: String = ""): Int

    @Query("""
    SELECT COUNT(*) FROM quiz_question qq
    INNER JOIN curiosity c ON c.id = qq.curiosityId
    LEFT JOIN quiz_answer qa ON qa.questionId = qq.id AND qa.isCorrect = 1
    WHERE qa.id IS NULL
      AND c.isRead = 1
      AND c.isIgnorata = 0
""")
    suspend fun quizNonRisposti(): Int

    @Query("SELECT COUNT(*) FROM quiz_question")
    suspend fun countTotali(): Int

    // Sync remoto
    @Query("SELECT * FROM quiz_question WHERE curiosityId = :curiosityId LIMIT 1")
    suspend fun getByCuriosityId(curiosityId: Int): QuizQuestion?

    /** Per il duello: tutte le domande disponibili, senza filtri */
    @Query("SELECT * FROM quiz_question ORDER BY RANDOM() LIMIT :n")
    suspend fun getRandomAll(n: Int): List<QuizQuestion>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<QuizQuestion>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: QuizQuestion): Long

    @Update
    suspend fun update(item: QuizQuestion)
}