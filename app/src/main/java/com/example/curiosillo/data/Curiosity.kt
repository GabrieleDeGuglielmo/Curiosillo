package com.example.curiosillo.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "curiosity")
data class Curiosity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title:        String,
    val body:         String,
    val category:     String,
    val emoji:        String  = "",
    val isRead:       Boolean = false,
    val isBookmarked: Boolean = false,
    val nota:         String  = "",      // ← nota personale
    val readAt:       Long    = 0L       // ← timestamp lettura (per ripasso)
)
