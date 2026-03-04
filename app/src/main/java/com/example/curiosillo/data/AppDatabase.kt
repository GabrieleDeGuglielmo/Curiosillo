package com.example.curiosillo.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Curiosity::class, QuizQuestion::class, QuizAnswer::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun curiosityDao(): CuriosityDao
    abstract fun quizQuestionDao(): QuizQuestionDao
    abstract fun userProgressDao(): UserProgressDao
    abstract fun quizAnswerDao(): QuizAnswerDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        fun getDatabase(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java, "curiosillo.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build().also { INSTANCE = it }
            }

        val MIGRATION_1_2 = object : androidx.room.migration.Migration(1, 2) {
            override fun migrate(database: androidx.sqlite.db.SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS quiz_answer (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        questionId INTEGER NOT NULL,
                        isCorrect INTEGER NOT NULL,
                        answeredAt INTEGER NOT NULL,
                        FOREIGN KEY (questionId) REFERENCES quiz_question(id) ON DELETE CASCADE
                    )
                """
                )
                database.execSQL(
                    """
                    CREATE INDEX IF NOT EXISTS index_quiz_answer_questionId 
                    ON quiz_answer(questionId)
                """
                )
            }
        }
    }
}
