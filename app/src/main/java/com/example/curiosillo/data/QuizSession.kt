package com.example.curiosillo.data

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quiz_session")
@Immutable
data class QuizSession(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val correctAnswers: Int,
    val totalAnswers:   Int,
    val categoria:      String = "",
    val playedAt:       Long   = System.currentTimeMillis()
)
