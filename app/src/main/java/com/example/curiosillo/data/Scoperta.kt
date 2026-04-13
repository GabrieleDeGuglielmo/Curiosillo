package com.example.curiosillo.data

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scoperte")
@Immutable
data class Scoperta(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val firestoreId: String? = null,
    val titolo: String,
    val descrizione: String,
    val categoria: String,
    val dataScoperta: Long = System.currentTimeMillis()
)
