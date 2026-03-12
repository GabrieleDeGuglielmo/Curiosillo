package com.example.curiosillo.data

/**
 * Struttura di un duello su Firestore.
 * Path: duelli/{duelloId}
 *
 * domande: lista serializzata delle 10 domande (stessa per entrambi)
 * giocatori: mappa uid → DuelloGiocatore
 * stato: "attesa_avversario" | "in_corso" | "completato"
 */
data class DuelloGiocatore(
    val uid:        String = "",
    val username:   String = "",
    val risposte:   Map<String, String> = emptyMap(), // domandaIndex → risposta scelta
    val completato: Boolean = false
)

data class DuelloDomanda(
    val questionText:  String = "",
    val correctAnswer: String = "",
    val wrongAnswer1:  String = "",
    val wrongAnswer2:  String = "",
    val wrongAnswer3:  String = "",
    val category:      String = ""
) {
    fun risposteShuffled(seed: Long): List<String> =
        listOf(correctAnswer, wrongAnswer1, wrongAnswer2, wrongAnswer3)
            .shuffled(java.util.Random(seed))
}

data class DuelloStato(
    val id:            String                    = "",
    val codice:        String                    = "",   // 6 lettere uppercase per invito
    val stato:         String                    = "attesa_avversario",
    val domande:       List<DuelloDomanda>       = emptyList(),
    val giocatori:     Map<String, DuelloGiocatore> = emptyMap(),
    val creatoreUid:   String                    = "",
    val creatoAt:      Long                      = 0L
) {
    val isInCorso:     Boolean get() = stato == "in_corso"
    val isCompletato:  Boolean get() = stato == "completato"
    val isAttesa:      Boolean get() = stato == "attesa_avversario"

    fun punteggio(uid: String): Int {
        val g = giocatori[uid] ?: return 0
        return domande.mapIndexed { i, d ->
            if (g.risposte[i.toString()] == d.correctAnswer) 1 else 0
        }.sum()
    }

    fun avversarioUid(mioUid: String): String? =
        giocatori.keys.firstOrNull { it != mioUid }

    fun entrambiCompletato(): Boolean =
        giocatori.values.all { it.completato }
}
