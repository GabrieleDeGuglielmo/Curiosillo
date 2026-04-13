package com.example.curiosillo.data

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "badge_sbloccato")
@Immutable
data class BadgeSbloccato(
    @PrimaryKey val id: String,
    val nome: String,
    val descrizione: String,
    val icona: String,
    val sbloccatoAt: Long = System.currentTimeMillis()
)

@Immutable
data class BadgeDefinizione(
    val id: String,
    val nome: String,
    val descrizione: String,
    val icona: String,
    val isLeggendario: Boolean = false
)

object BadgeCatalogo {
    val tutti = listOf(
        BadgeDefinizione("prima_pillola", "Prima pillola", "Hai letto la tua prima curiosità!", "🌱"),
        BadgeDefinizione("prima_risposta", "Mente sveglia", "Hai completato il tuo primo quiz!", "🧠"),
        BadgeDefinizione("streak_3", "In forma", "Streak di 3 giorni consecutivi!", "⚡"),
        BadgeDefinizione("streak_7", "Settimana di fuoco", "Streak di 7 giorni consecutivi!", "🔥"),
        BadgeDefinizione("streak_30", "Inarrestabile", "Streak di 30 giorni consecutivi!", "💎"),
        BadgeDefinizione("perfetto_5", "Perfetto", "5 risposte corrette di fila!", "💯"),
        BadgeDefinizione("pillole_10", "Lettore", "Hai letto 10 pillole!", "📖"),
        BadgeDefinizione("pillole_20", "Divoratore", "Hai letto 20 pillole!", "📚"),
        BadgeDefinizione("pillole_50", "Enciclopedia", "Hai letto 50 pillole!", "🗂️"),
        BadgeDefinizione("livello_3", "Studioso", "Hai raggiunto il livello 3!", "🎓"),
        BadgeDefinizione("livello_5", "Campione", "Hai raggiunto il livello 5!", "🏆"),
        BadgeDefinizione("preferiti_5", "Collezionista", "Hai salvato 5 pillole nei preferiti!", "🔖"),
        BadgeDefinizione("scoperta_1", "Esploratore", "Hai fatto la tua prima scoperta AR!", "🔍"),
        BadgeDefinizione("scoperte_10", "Ricercatore", "Hai fatto 10 scoperte AR!", "🔭"),
        BadgeDefinizione("scoperte_50", "Pioniere", "Hai fatto 50 scoperte AR!", "🗺️"),

        // --- Badge Leggendari AR ---
        BadgeDefinizione("leg_colosseo", "Il Gladiatore", "Hai scoperto il Colosseo!", "🏛️", true),
        BadgeDefinizione("leg_pantheon", "L'Occhio degli Dei", "Hai scoperto il Pantheon!", "🏛️", true),
        BadgeDefinizione("leg_pisa", "Equilibrista", "Hai scoperto la Torre di Pisa!", "🗼", true),
        BadgeDefinizione("leg_milano", "La Guglia", "Hai scoperto il Duomo di Milano!", "⛪", true),
        BadgeDefinizione("leg_pietro", "Il Cupolone", "Hai scoperto la Basilica di San Pietro!", "⛪", true),
        BadgeDefinizione("leg_fiore", "Rinascimentale", "Hai scoperto S. Maria del Fiore!", "⛪", true),
        BadgeDefinizione("leg_pompei", "L'Archeologo", "Hai scoperto gli Scavi di Pompei!", "🌋", true),
        BadgeDefinizione("leg_marco", "Il Veneziano", "Hai scoperto la Basilica di S. Marco!", "⛪", true),
        BadgeDefinizione("leg_caserta", "Il Cortigiano", "Hai scoperto la Reggia di Caserta!", "🏰", true)
    )

    fun trovaPerId(id: String): BadgeDefinizione? = tutti.find { it.id == id }
}
