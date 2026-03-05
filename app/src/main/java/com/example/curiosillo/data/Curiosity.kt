package com.example.curiosillo.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "curiosity",
    indices   = [Index(value = ["externalId"], unique = true)]
)
data class Curiosity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,

    // ID stabile proveniente dal JSON remoto (es. "c001").
    // NULL per le pillole create col vecchio seed hardcodato.
    val externalId: String? = null,

    val title:       String,
    val body:        String,
    val category:    String,
    val emoji:       String  = "",
    val isRead:      Boolean = false,
    val isBookmarked:Boolean = false,
    val nota:        String  = "",
    val readAt:      Long?   = null
)
