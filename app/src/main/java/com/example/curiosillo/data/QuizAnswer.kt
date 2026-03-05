package com.example.curiosillo.data

import androidx.room.*

@Entity(
    tableName   = "quiz_answer",
    foreignKeys = [ForeignKey(
        entity        = QuizQuestion::class,
        parentColumns = ["id"],
        childColumns  = ["questionId"],
        onDelete      = ForeignKey.CASCADE
    )],
    indices = [Index("questionId")]
)
data class QuizAnswer(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val questionId:  Int,
    val isCorrect:   Boolean,
    val answeredAt:  Long = System.currentTimeMillis()
)