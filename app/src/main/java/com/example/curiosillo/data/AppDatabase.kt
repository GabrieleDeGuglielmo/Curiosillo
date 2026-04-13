package com.example.curiosillo.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        Curiosity::class,
        QuizQuestion::class,
        QuizAnswer::class,
        BadgeSbloccato::class,
        QuizSession::class,
        Scoperta::class
    ],
    version = 9,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun curiosityDao(): CuriosityDao
    abstract fun quizQuestionDao(): QuizQuestionDao
    abstract fun quizAnswerDao(): QuizAnswerDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun badgeDao(): BadgeDao
    abstract fun quizSessionDao(): QuizSessionDao
    abstract fun scopertaDao(): ScopertaDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "curiosillo.db"
                )
                    .addMigrations(
                        MIGRATION_1_2,
                        MIGRATION_2_3,
                        MIGRATION_3_4,
                        MIGRATION_4_5,
                        MIGRATION_5_6,
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                        MIGRATION_8_9
                    )
                    .fallbackToDestructiveMigrationOnDowngrade() // Protezione extra
                    .build()
                INSTANCE = instance
                instance
            }
        }

        // ── Storiche ─────────────────────────────────────────────────────────

        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
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
                    "CREATE INDEX IF NOT EXISTS index_quiz_answer_questionId ON quiz_answer(questionId)"
                )
            }
        }

        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    "ALTER TABLE curiosity ADD COLUMN isBookmarked INTEGER NOT NULL DEFAULT 0"
                )
            }
        }

        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS badge_sbloccato (
                        id TEXT PRIMARY KEY NOT NULL,
                        nome TEXT NOT NULL,
                        descrizione TEXT NOT NULL,
                        icona TEXT NOT NULL,
                        sbloccatoAt INTEGER NOT NULL
                    )
                """
                )
            }
        }

        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE curiosity ADD COLUMN nota TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE curiosity ADD COLUMN readAt INTEGER NOT NULL DEFAULT 0")
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS quiz_session (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        correctAnswers INTEGER NOT NULL,
                        totalAnswers INTEGER NOT NULL,
                        categoria TEXT NOT NULL DEFAULT '',
                        playedAt INTEGER NOT NULL
                    )
                """
                )
            }
        }

        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
            CREATE TABLE curiosity_new (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                externalId TEXT,
                title TEXT NOT NULL,
                body TEXT NOT NULL,
                category TEXT NOT NULL,
                emoji TEXT NOT NULL,
                isRead INTEGER NOT NULL,
                isBookmarked INTEGER NOT NULL,
                nota TEXT NOT NULL,
                readAt INTEGER
            )
        """
                )
                database.execSQL(
                    """
            INSERT INTO curiosity_new 
            SELECT id, NULL, title, body, category, emoji, isRead, isBookmarked, nota,
                   CASE WHEN readAt = 0 THEN NULL ELSE readAt END
            FROM curiosity
        """
                )
                database.execSQL("DROP TABLE curiosity")
                database.execSQL("ALTER TABLE curiosity_new RENAME TO curiosity")
                database.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS index_curiosity_externalId ON curiosity(externalId)"
                )
                database.execSQL(
                    "ALTER TABLE quiz_question ADD COLUMN category TEXT NOT NULL DEFAULT ''"
                )
            }
        }

        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE curiosity ADD COLUMN voto INTEGER")
                database.execSQL("ALTER TABLE curiosity ADD COLUMN isIgnorata INTEGER NOT NULL DEFAULT 0")
            }
        }

        private val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE curiosity ADD COLUMN approfondimentoAi TEXT")
            }
        }

        private val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS scoperte (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        firestoreId TEXT,
                        titolo TEXT NOT NULL,
                        descrizione TEXT NOT NULL,
                        categoria TEXT NOT NULL,
                        dataScoperta INTEGER NOT NULL
                    )
                """
                )
            }
        }
    }
}
