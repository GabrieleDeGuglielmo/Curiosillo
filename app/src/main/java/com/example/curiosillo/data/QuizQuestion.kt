package com.example.curiosillo.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "quiz_question",
    foreignKeys = [ForeignKey(
        entity = Curiosity::class,
        parentColumns = ["id"],
        childColumns = ["curiosityId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("curiosityId")]
)
data class QuizQuestion(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val curiosityId: Int,
    val questionText: String,
    val correctAnswer: String,
    val wrongAnswer1: String,
    val wrongAnswer2: String,
    val wrongAnswer3: String,
    val explanation: String = ""
)
