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

    private val db = FirebaseFirestore.getInstance()
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
        val domande = (data["domande"] as? List<Map<String, Any>>)
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

    // ── Crea duello (creatore) ────────────────────────────────────────────────

    suspend fun creaDuello(
        uid: String,
        nickname: String,
        domande: List<QuizQuestion>
    ): String {
        val codice  = generaCodice()
        val docRef  = duelli.document()
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
            "giocatori"   to mapOf(uid to giocatore)
        )).await()
        return docRef.id
    }

    // ── Unisciti tramite codice ───────────────────────────────────────────────

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

            if (query.isEmpty) return Result.failure(Exception("Stanza non trovata o già iniziata."))

            val doc = query.documents.first()
            // Non permettere di unirsi alla propria stanza
            if (doc.getString("creatoreUid") == uid)
                return Result.failure(Exception("Non puoi unirti alla tua stessa stanza."))

            val giocatore = mapOf(
                "uid"        to uid,
                "nickname"   to nickname,
                "risposte"   to emptyMap<String, String>(),
                "completato" to false
            )
            doc.reference.update(mapOf(
                "stato"                to "in_corso",
                "giocatori.$uid"       to giocatore
            )).await()
            Result.success(doc.id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ── Matchmaking casuale ───────────────────────────────────────────────────

    suspend fun cercaAvversarioCasuale(
        uid: String,
        nickname: String,
        domande: List<QuizQuestion>
    ): Flow<MatchmakingResult> = callbackFlow {
        // Cerca se c'è qualcuno in coda
        val codaSnap = coda.whereNotEqualTo("uid", uid).limit(1).get().await()

        if (!codaSnap.isEmpty) {
            // Trovato avversario in coda
            val avversarioDoc = codaSnap.documents.first()
            val avversarioUid = avversarioDoc.getString("uid") ?: ""
            val avversarioNick = avversarioDoc.getString("nickname") ?: "Anonimo"

            // Rimuovi avversario dalla coda
            avversarioDoc.reference.delete().await()

            // Crea duello
            val duelloId = creaDuello(uid, nickname, domande)
            val giocatore2 = mapOf(
                "uid"        to avversarioUid,
                "nickname"   to avversarioNick,
                "risposte"   to emptyMap<String, String>(),
                "completato" to false
            )
            duelli.document(duelloId).update(mapOf(
                "stato"                         to "in_corso",
                "giocatori.$avversarioUid"      to giocatore2
            )).await()

            trySend(MatchmakingResult.Trovato(duelloId))
            close()
        } else {
            // Mettiti in coda e crea una stanza di attesa
            val duelloId = creaDuello(uid, nickname, domande)
            coda.document(uid).set(mapOf(
                "uid"      to uid,
                "nickname" to nickname,
                "duelloId" to duelloId,
                "entratAt" to System.currentTimeMillis()
            )).await()

            trySend(MatchmakingResult.InAttesa(duelloId))

            // Ascolta il duello finché non parte
            val listener = duelli.document(duelloId).addSnapshotListener { snap, _ ->
                if (snap == null || !snap.exists()) return@addSnapshotListener
                val stato = snap.getString("stato") ?: return@addSnapshotListener
                if (stato == "in_corso") {
                    trySend(MatchmakingResult.Trovato(duelloId))
                    close()
                }
            }

            awaitClose {
                listener.remove()
                // Se chiuso prima di trovare avversario, rimuovi dalla coda
                coda.document(uid).delete()
            }
        }
    }

    suspend fun annullaRicerca(uid: String, duelloId: String) {
        coda.document(uid).delete().await()
        duelli.document(duelloId).delete().await()
    }

    // ── Ascolta duello realtime ───────────────────────────────────────────────

    fun osservaDuello(duelloId: String): Flow<DuelloStato?> = callbackFlow {
        val listener: ListenerRegistration = duelli.document(duelloId)
            .addSnapshotListener { snap, _ ->
                if (snap == null || !snap.exists()) {
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
        duelli.document(duelloId).update(
            "giocatori.$uid.risposte.$indice", risposta
        ).await()
    }

    suspend fun segnaCompletato(duelloId: String, uid: String) {
        duelli.document(duelloId).update(
            "giocatori.$uid.completato", true
        ).await()

        // Se entrambi completato → aggiorna stato
        val snap = duelli.document(duelloId).get().await()
        val data = snap.data ?: return
        @Suppress("UNCHECKED_CAST")
        val giocatori = data["giocatori"] as? Map<String, Map<String, Any>> ?: return
        val tuttiCompletati = giocatori.values.all { it["completato"] as? Boolean == true }
        if (tuttiCompletati) {
            duelli.document(duelloId).update("stato", "completato").await()
        }
    }
}

sealed class MatchmakingResult {
    data class InAttesa(val duelloId: String) : MatchmakingResult()
    data class Trovato(val duelloId: String)  : MatchmakingResult()
}
