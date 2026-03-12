package com.example.curiosillo.domain

data class Livello(
    val numero: Int,
    val titolo: String,
    val xpInizio: Int,
    val xpFine: Int       // -1 = livello massimo
)

object LivelloHelper {

    private val livelli = listOf(
        Livello(1,  "Curioso",       0,      249),
        Livello(2,  "Esploratore",   250,    599),
        Livello(3,  "Studioso",      600,    1199),
        Livello(4,  "Sapiente",      1200,   2299),
        Livello(5,  "Campione",      2300,   3999),
        Livello(6,  "Maestro",       4000,   6499),
        Livello(7,  "Gran Maestro",  6500,   9999),
        Livello(8,  "Leggenda",      10000,  14999),
        Livello(9,  "Mito",          15000,  22999),
        Livello(10, "Enciclopedia",  23000,  -1)
    )

    fun daXp(xp: Int): Livello =
        livelli.lastOrNull { xp >= it.xpInizio } ?: livelli.first()

    fun progressione(xp: Int): Float {
        val livello = daXp(xp)
        if (livello.xpFine == -1) return 1f
        val range = (livello.xpFine - livello.xpInizio + 1).toFloat()
        val fatto  = (xp - livello.xpInizio).toFloat()
        return (fatto / range).coerceIn(0f, 1f)
    }

    fun xpAlProssimo(xp: Int): Int {
        val livello = daXp(xp)
        if (livello.xpFine == -1) return 0
        return livello.xpFine - xp + 1
    }
}