package com.example.curiosillo.domain

data class Livello(
    val numero: Int,
    val titolo: String,
    val xpInizio: Int,
    val xpFine: Int       // -1 = livello massimo
)

object LivelloHelper {

    private val livelli = listOf(
        Livello(1,  "Curioso",       0,     99),
        Livello(2,  "Esploratore",   100,   249),
        Livello(3,  "Studioso",      250,   499),
        Livello(4,  "Sapiente",      500,   999),
        Livello(5,  "Campione",      1000,  1749),
        Livello(6,  "Maestro",       1750,  2749),
        Livello(7,  "Gran Maestro",  2750,  4249),
        Livello(8,  "Leggenda",      4250,  6499),
        Livello(9,  "Mito",          6500,  9999),
        Livello(10, "Enciclopedia",  10000, -1)
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
