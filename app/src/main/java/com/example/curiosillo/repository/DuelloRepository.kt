package com.example.curiosillo.repository

import com.example.curiosillo.data.DuelloDomanda
import com.example.curiosillo.data.DuelloGiocatore
import com.example.curiosillo.data.DuelloStato
import com.example.curiosillo.data.QuizQuestion
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class DuelloRepository {

    private val db     = FirebaseFirestore.getInstance()
    private val duelli = db.collection("duelli")
    private val coda   = db.collection("duello_coda")

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun generaCodice(): String =
        (1..6).map { ('A'..'Z').random() }.joinToString("")

    private fun domandaToMap(d: DuelloDomanda) = mapOf(
        "questionText"  to d.questionText,
        "correctAnswer" to d.correctAnswer,
        "wrongAnswer1"  to d.wrongAnswer1,
        "wrongAnswer2"  to d.wrongAnswer2,
        "wrongAnswer3"  to d.wrongAnswer3,
        "category"      to d.category
    )

    @Suppress("UNCHECKED_CAST")
    private fun mapToDomanda(m: Map<String, Any>): DuelloDomanda = DuelloDomanda(
        questionText  = m["questionText"]  as? String ?: "",
        correctAnswer = m["correctAnswer"] as? String ?: "",
        wrongAnswer1  = m["wrongAnswer1"]  as? String ?: "",
        wrongAnswer2  = m["wrongAnswer2"]  as? String ?: "",
        wrongAnswer3  = m["wrongAnswer3"]  as? String ?: "",
        category      = m["category"]      as? String ?: ""
    )

    @Suppress("UNCHECKED_CAST")
    private fun mapToGiocatore(m: Map<String, Any>): DuelloGiocatore = DuelloGiocatore(
        uid        = m["uid"]        as? String ?: "",
        nickname   = m["nickname"]   as? String ?: "",
        risposte   = (m["risposte"]  as? Map<String, String>) ?: emptyMap(),
        completato = m["completato"] as? Boolean ?: false
    )

    @Suppress("UNCHECKED_CAST")
    private fun docToStato(id: String, data: Map<String, Any>): DuelloStato {
        val domande   = (data["domande"] as? List<Map<String, Any>>)
            ?.map { mapToDomanda(it) } ?: emptyList()
        val giocatori = (data["giocatori"] as? Map<String, Map<String, Any>>)
            ?.mapValues { mapToGiocatore(it.value) } ?: emptyMap()
        return DuelloStato(
            id          = id,
            codice      = data["codice"]      as? String ?: "",
            stato       = data["stato"]       as? String ?: "attesa_avversario",
            domande     = domande,
            giocatori   = giocatori,
            creatoreUid = data["creatoreUid"] as? String ?: "",
            creatoAt    = data["creatoAt"]    as? Long ?: 0L
        )
    }

    /** Aggiorna solo se il documento esiste ancora — evita NOT_FOUND crash */
    private suspend fun safeUpdate(duelloId: String, dati: Map<String, Any>) {
        try {
            val exists = duelli.document(duelloId).get().await().exists()
            if (exists) duelli.document(duelloId).update(dati).await()
        } catch (_: Exception) {}
    }

    // ── Crea duello ───────────────────────────────────────────────────────────

    suspend fun creaDuello(
        uid: String,
        nickname: String,
        domande: List<QuizQuestion>
    ): String {
        val codice    = generaCodice()
        val docRef    = duelli.document()
        val giocatore = mapOf(
            "uid"        to uid,
            "nickname"   to nickname,
            "risposte"   to emptyMap<String, String>(),
            "completato" to false
        )
        docRef.set(mapOf(
            "codice"      to codice,
            "stato"       to "attesa_avversario",
            "creatoreUid" to uid,
            "creatoAt"    to System.currentTimeMillis(),
            "domande"     to domande.map { q ->
                domandaToMap(DuelloDomanda(
                    questionText  = q.questionText,
                    correctAnswer = q.correctAnswer,
                    wrongAnswer1  = q.wrongAnswer1,
                    wrongAnswer2  = q.wrongAnswer2,
                    wrongAnswer3  = q.wrongAnswer3,
                    category      = q.category
                ))
            },
            "giocatori" to mapOf(uid to giocatore)
        )).await()
        return docRef.id
    }

    // ── Unisciti con codice ───────────────────────────────────────────────────

    suspend fun uniscitiConCodice(
        codice: String,
        uid: String,
        nickname: String
    ): Result<String> {
        return try {
            val query = duelli
                .whereEqualTo("codice", codice.uppercase())
                .whereEqualTo("stato", "attesa_avversario")
                .limit(1)
                .get().await()

            when {
                query.isEmpty -> Result.failure(Exception("Stanza non trovata o già iniziata."))
                query.documents.first().getString("creatoreUid") == uid ->
                    Result.failure(Exception("Non puoi unirti alla tua stessa stanza."))
                else -> {
                    val doc = query.documents.first()
                    val giocatore = mapOf(
                        "uid"        to uid,
                        "nickname"   to nickname,
                        "risposte"   to emptyMap<String, String>(),
                        "completato" to false
                    )
                    doc.reference.update(mapOf(
                        "stato"          to "in_corso",
                        "giocatori.$uid" to giocatore
                    )).await()
                    Result.success(doc.id)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Matchmaking casuale ───────────────────────────────────────────────────
    //
    // FIX race condition:
    // - Il primo utente crea il duello e avvia il listener PRIMA di scrivere in coda
    //   così non può perdere l'evento "in_corso" per timing
    // - Il secondo utente trova il duelloId nella coda e aggiorna QUEL documento,
    //   non ne crea uno nuovo

    suspend fun cercaAvversarioCasuale(
        uid: String,
        nickname: String,
        domande: List<QuizQuestion>
    ): Flow<MatchmakingResult> = callbackFlow {

        // Cerca prima se c'è già qualcuno in coda
        val codaSnap = coda.whereNotEqualTo("uid", uid).limit(1).get().await()

        if (!codaSnap.isEmpty) {
            // ── Secondo utente: trova avversario ──────────────────────────────
            val avvDoc      = codaSnap.documents.first()
            val avvUid      = avvDoc.getString("uid")      ?: ""
            val avvNick     = avvDoc.getString("nickname") ?: "Anonimo"
            val avvDuelloId = avvDoc.getString("duelloId") ?: ""

            try {
                // Rimuovi avversario dalla coda
                avvDoc.reference.delete().await()

                // Aggiorna il duello già creato dal primo utente
                val nostroGiocatore = mapOf(
                    "uid"        to uid,
                    "nickname"   to nickname,
                    "risposte"   to emptyMap<String, String>(),
                    "completato" to false
                )
                duelli.document(avvDuelloId).update(mapOf(
                    "stato"          to "in_corso",
                    "giocatori.$uid" to nostroGiocatore
                )).await()

                trySend(MatchmakingResult.Trovato(avvDuelloId))
                close()

            } catch (_: Exception) {
                // Gara persa con un terzo utente — vai in coda
                val duelloId = creaDuello(uid, nickname, domande)
                mettitiInCoda(uid, nickname, duelloId)
                trySend(MatchmakingResult.InAttesa(duelloId))
                osservaInCorso(duelloId) { trySend(MatchmakingResult.Trovato(it)); close() }
            }

        } else {
            // ── Primo utente: crea duello, avvia listener, poi vai in coda ────
            val duelloId = creaDuello(uid, nickname, domande)

            // IMPORTANTE: listener attivo PRIMA di scrivere in coda
            var listenerRef: ListenerRegistration? = null
            listenerRef = duelli.document(duelloId).addSnapshotListener { snap, _ ->
                if (snap?.getString("stato") == "in_corso") {
                    trySend(MatchmakingResult.Trovato(duelloId))
                    listenerRef?.remove()
                    close()
                }
            }

            mettitiInCoda(uid, nickname, duelloId)
            trySend(MatchmakingResult.InAttesa(duelloId))

            awaitClose {
                listenerRef?.remove()
                coda.document(uid).delete()
            }
            return@callbackFlow
        }

        awaitClose { coda.document(uid).delete() }
    }

    private fun osservaInCorso(duelloId: String, onTrovato: (String) -> Unit) {
        var reg: ListenerRegistration? = null
        reg = duelli.document(duelloId).addSnapshotListener { snap, _ ->
            if (snap?.getString("stato") == "in_corso") {
                onTrovato(duelloId)
                reg?.remove()
            }
        }
    }

    private suspend fun mettitiInCoda(uid: String, nickname: String, duelloId: String) {
        coda.document(uid).set(mapOf(
            "uid"      to uid,
            "nickname" to nickname,
            "duelloId" to duelloId,
            "entratAt" to System.currentTimeMillis()
        )).await()
    }

    suspend fun annullaRicerca(uid: String, duelloId: String) {
        try { coda.document(uid).delete().await() } catch (_: Exception) {}
        try {
            if (duelli.document(duelloId).get().await().exists())
                duelli.document(duelloId).delete().await()
        } catch (_: Exception) {}
    }

    // ── Ascolta duello realtime ───────────────────────────────────────────────

    fun osservaDuello(duelloId: String): Flow<DuelloStato?> = callbackFlow {
        val listener: ListenerRegistration = duelli.document(duelloId)
            .addSnapshotListener { snap, _ ->
                if (snap == null || !snap.exists()) {
                    // null = documento eliminato (avversario ha abbandonato)
                    trySend(null)
                    return@addSnapshotListener
                }
                val data = snap.data ?: return@addSnapshotListener
                trySend(docToStato(snap.id, data))
            }
        awaitClose { listener.remove() }
    }

    // ── Invia risposta ────────────────────────────────────────────────────────

    suspend fun inviaRisposta(duelloId: String, uid: String, indice: Int, risposta: String) {
        safeUpdate(duelloId, mapOf("giocatori.$uid.risposte.$indice" to risposta))
    }

    suspend fun segnaCompletato(duelloId: String, uid: String) {
        safeUpdate(duelloId, mapOf("giocatori.$uid.completato" to true))
        try {
            val snap = duelli.document(duelloId).get().await()
            if (!snap.exists()) return
            @Suppress("UNCHECKED_CAST")
            val giocatori = snap.data?.get("giocatori") as? Map<String, Map<String, Any>> ?: return
            if (giocatori.values.all { it["completato"] as? Boolean == true }) {
                safeUpdate(duelloId, mapOf("stato" to "completato"))
            }
        } catch (_: Exception) {}
    }
}

sealed class MatchmakingResult {
    data class InAttesa(val duelloId: String) : MatchmakingResult()
    data class Trovato(val duelloId: String)  : MatchmakingResult()
}