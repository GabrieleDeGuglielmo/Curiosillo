package com.example.curiosillo.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface QuizQuestionDao {

    @Transaction
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

    @Transaction
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

    @Transaction
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

    @Transaction
    @Query("""
    SELECT COUNT(*) FROM quiz_question qq
    INNER JOIN curiosity c ON c.id = qq.curiosityId
    LEFT JOIN quiz_answer qa ON qa.questionId = qq.id AND qa.isCorrect = 1
    WHERE qa.id IS NULL
      AND c.isRead = 1
      AND c.isIgnorata = 0
      AND (:cat = '' OR qq.category = :cat)
""")
    fun countAvailableFlow(cat: String = ""): Flow<Int>

    @Transaction
    @Query("""
    SELECT COUNT(*) FROM quiz_question qq
    INNER JOIN curiosity c ON c.id = qq.curiosityId
    LEFT JOIN quiz_answer qa ON qa.questionId = qq.id AND qa.isCorrect = 1
    WHERE qa.id IS NULL
      AND c.isRead = 1
      AND c.isIgnorata = 0
""")
    suspend fun quizNonRisposti(): Int

    @Transaction
    @Query("""
    SELECT COUNT(*) FROM quiz_question qq
    INNER JOIN curiosity c ON c.id = qq.curiosityId
    LEFT JOIN quiz_answer qa ON qa.questionId = qq.id AND qa.isCorrect = 1
    WHERE qa.id IS NULL
      AND c.isRead = 1
      AND c.isIgnorata = 0
""")
    fun quizNonRispostiFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM quiz_question")
    suspend fun countTotali(): Int

    @Query("SELECT COUNT(*) FROM quiz_question")
    fun countTotaliFlow(): Flow<Int>

    // Sync remoto
    @Query("SELECT * FROM quiz_question WHERE curiosityId = :curiosityId LIMIT 1")
    suspend fun getByCuriosityId(curiosityId: Int): QuizQuestion?

    /** Per il duello e Sopravvivenza: tutte le domande disponibili, senza filtri */
    @Query("SELECT * FROM quiz_question ORDER BY RANDOM() LIMIT :n")
    suspend fun getRandomAll(n: Int): List<QuizQuestion>

    /** Per recuperare tutte le domande disponibili senza limite prefissato (o con limite molto alto) */
    @Query("SELECT * FROM quiz_question ORDER BY RANDOM()")
    suspend fun getAllRandomly(): List<QuizQuestion>

    /** Per la modalità Scalata: solo domande da pillole già lette */
    @Transaction
    @Query("""
        SELECT qq.* FROM quiz_question qq
        INNER JOIN curiosity c ON c.id = qq.curiosityId
        WHERE c.isRead = 1 AND c.isIgnorata = 0
        ORDER BY RANDOM()
    """)
    suspend fun getDomandeLetteRandom(): List<QuizQuestion>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(items: List<QuizQuestion>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item: QuizQuestion): Long

    @Update
    suspend fun update(item: QuizQuestion)
}
