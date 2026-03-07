package com.example.curiosillo.network

import com.example.curiosillo.data.ContentPreferences
import com.example.curiosillo.data.Curiosity
import com.example.curiosillo.data.QuizQuestion
import com.example.curiosillo.repository.CuriosityRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

/**
 * Sincronizza le curiosità da Firestore → Room locale.
 *
 * Struttura Firestore:
 *   curiosita/{externalId}          → documento curiosità
 *     .titolo, .corpo, .categoria, .emoji, .versione (Long)
 *   curiosita/{externalId}/quiz/domanda → subdocument quiz (opzionale)
 *     .domanda, .rispostaCorretta, .risposteErrate (List<String>), .spiegazione
 *
 * Strategia MERGE: stessi principi del vecchio ContentSyncService.
 * La versione globale è salvata in un documento speciale "meta/versione".
 */
class FirestoreSyncService(
    private val repo:         CuriosityRepository,
    private val contentPrefs: ContentPreferences
) {
    private val db = FirebaseFirestore.getInstance()

    sealed class SyncResult {
        data class Success(val nuove: Int, val aggiornate: Int) : SyncResult()
        object NessunaModifica : SyncResult()
        data class Errore(val messaggio: String) : SyncResult()
    }

    suspend fun sync(): SyncResult {
        return try {
            // 1. Leggi versione remota
            val metaDoc      = db.collection("curiosita").document("_meta_").get().await()
            val versioneRemota = metaDoc.getLong("versione") ?: 1L
            val versioneLocale = contentPrefs.getContentVersion()

            val dbVuoto = repo.totaleCuriosità() == 0

            if (!dbVuoto && versioneRemota <= versioneLocale) {
                return SyncResult.NessunaModifica
            }

            // 2. Scarica tutte le curiosità
            val snapshot = db.collection("curiosita")
                .whereNotEqualTo("__name__", "_meta_")
                .get().await()

            var nuove      = 0
            var aggiornate = 0

            for (doc in snapshot.documents) {
                val externalId = doc.id
                if (externalId == "_meta_") continue

                val titolo    = doc.getString("titolo")    ?: continue
                val corpo     = doc.getString("corpo")     ?: continue
                val categoria = doc.getString("categoria") ?: ""
                val emoji     = doc.getString("emoji")     ?: ""

                val esistente = repo.getByExternalId(externalId)

                if (esistente == null) {
                    val nuovaCuriosita = Curiosity(
                        externalId   = externalId,
                        title        = titolo,
                        body         = corpo,
                        category     = categoria,
                        emoji        = emoji,
                        isRead       = false,
                        isBookmarked = false,
                        nota         = ""
                    )
                    val newId = repo.insertCuriosita(nuovaCuriosita)
                    sincronizzaQuiz(externalId, newId.toInt(), categoria)
                    nuove++
                } else {
                    repo.updateCuriosita(esistente.copy(
                        title    = titolo,
                        body     = corpo,
                        category = categoria,
                        emoji    = emoji
                    ))
                    aggiornaQuiz(externalId, esistente.id, categoria)
                    aggiornate++
                }
            }

            contentPrefs.setContentVersion(versioneRemota.toInt())
            SyncResult.Success(nuove, aggiornate)

        } catch (e: Exception) {
            SyncResult.Errore(e.message ?: "Errore sconosciuto")
        }
    }

    private suspend fun sincronizzaQuiz(externalId: String, curiosityId: Int, categoria: String) {
        try {
            val quizDoc = db.collection("curiosita").document(externalId)
                .collection("quiz").document("domanda").get().await()
            if (!quizDoc.exists()) return

            val domanda         = quizDoc.getString("domanda")         ?: return
            val rispostaCorretta = quizDoc.getString("rispostaCorretta") ?: return
            @Suppress("UNCHECKED_CAST")
            val risposteErrate  = quizDoc.get("risposteErrate") as? List<String> ?: emptyList()
            val spiegazione     = quizDoc.getString("spiegazione") ?: ""

            repo.insertQuizQuestion(QuizQuestion(
                curiosityId   = curiosityId,
                questionText  = domanda,
                correctAnswer = rispostaCorretta,
                wrongAnswer1  = risposteErrate.getOrElse(0) { "" },
                wrongAnswer2  = risposteErrate.getOrElse(1) { "" },
                wrongAnswer3  = risposteErrate.getOrElse(2) { "" },
                explanation   = spiegazione,
                category      = categoria
            ))
        } catch (_: Exception) {}
    }

    private suspend fun aggiornaQuiz(externalId: String, curiosityId: Int, categoria: String) {
        try {
            val quizDoc = db.collection("curiosita").document(externalId)
                .collection("quiz").document("domanda").get().await()
            if (!quizDoc.exists()) return

            val domanda         = quizDoc.getString("domanda")         ?: return
            val rispostaCorretta = quizDoc.getString("rispostaCorretta") ?: return
            @Suppress("UNCHECKED_CAST")
            val risposteErrate  = quizDoc.get("risposteErrate") as? List<String> ?: emptyList()
            val spiegazione     = quizDoc.getString("spiegazione") ?: ""

            val quizEsistente = repo.getQuizByCuriosityId(curiosityId)
            if (quizEsistente != null) {
                repo.updateQuizQuestion(quizEsistente.copy(
                    questionText  = domanda,
                    correctAnswer = rispostaCorretta,
                    wrongAnswer1  = risposteErrate.getOrElse(0) { "" },
                    wrongAnswer2  = risposteErrate.getOrElse(1) { "" },
                    wrongAnswer3  = risposteErrate.getOrElse(2) { "" },
                    explanation   = spiegazione
                ))
            } else {
                repo.insertQuizQuestion(QuizQuestion(
                    curiosityId   = curiosityId,
                    questionText  = domanda,
                    correctAnswer = rispostaCorretta,
                    wrongAnswer1  = risposteErrate.getOrElse(0) { "" },
                    wrongAnswer2  = risposteErrate.getOrElse(1) { "" },
                    wrongAnswer3  = risposteErrate.getOrElse(2) { "" },
                    explanation   = spiegazione,
                    category      = categoria
                ))
            }
        } catch (_: Exception) {}
    }
}