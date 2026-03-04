package com.example.curiosillo.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities  = [Curiosity::class, QuizQuestion::class, QuizAnswer::class, BadgeSbloccato::class],
    version   = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun curiosityDao(): CuriosityDao
    abstract fun quizQuestionDao(): QuizQuestionDao
    abstract fun userProgressDao(): UserProgressDao
    abstract fun quizAnswerDao(): QuizAnswerDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun badgeDao(): BadgeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "curiosillo.db"
                )
                .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                .build().also { INSTANCE = it }
            }

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS quiz_answer (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        questionId INTEGER NOT NULL,
                        isCorrect INTEGER NOT NULL,
                        answeredAt INTEGER NOT NULL,
                        FOREIGN KEY (questionId) REFERENCES quiz_question(id) ON DELETE CASCADE
                    )
                """)
                database.execSQL("""
                    CREATE INDEX IF NOT EXISTS index_quiz_answer_questionId
                    ON quiz_answer(questionId)
                """)
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE curiosity ADD COLUMN isBookmarked INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("""
                    CREATE TABLE IF NOT EXISTS badge_sbloccato (
                        id TEXT PRIMARY KEY NOT NULL,
                        nome TEXT NOT NULL,
                        descrizione TEXT NOT NULL,
                        icona TEXT NOT NULL,
                        sbloccatoAt INTEGER NOT NULL
                    )
                """)
            }
        }
    }
}
