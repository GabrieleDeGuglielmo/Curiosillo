package com.example.curiosillo.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Curiosity::class, QuizQuestion::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun curiosityDao(): CuriosityDao
    abstract fun quizQuestionDao(): QuizQuestionDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun getDatabase(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context.applicationContext,
                    AppDatabase::class.java, "curiosillo.db")
                    .build().also { INSTANCE = it }
            }
    }
}
