package com.example.curiosillo.firebase

import com.example.curiosillo.data.ContentPreferences
import com.example.curiosillo.data.Curiosity
import com.example.curiosillo.data.QuizQuestion
import com.example.curiosillo.repository.CuriosityRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

/**
 * Sincronizza le curiosità da Firestore → Room locale.
 * Versione OTTIMIZZATA: scarica tutto in un'unica chiamata (pillola + quiz integrato).
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
            val metaDoc      = db.collection("curiosita").document("_meta_").get().await()
            val versioneRemota = metaDoc.getLong("versione") ?: 1L
            val versioneLocale = contentPrefs.getContentVersion()

            val dbVuoto = repo.totaleCuriosità() == 0
            // Sincronizza anche se la versione e' uguale, per recuperare
            // quiz mancanti da sync precedenti incomplete (domanda = null)
            val quizMancanti = repo.quizNonRisposti() == 0 && !dbVuoto
            if (!dbVuoto && !quizMancanti && versioneRemota <= versioneLocale) {
                return SyncResult.NessunaModifica
            }

            val snapshot = db.collection("curiosita")
                .whereNotEqualTo("__name__", "_meta_")
                .get().await()

            var nuove      = 0
            var aggiornate = 0

            // Quiz nella subcollection quiz/domanda - lettura in parallelo
            data class DocConQuiz(
                val externalId: String,
                val titolo: String, val corpo: String,
                val categoria: String, val emoji: String,
                val domanda: String?, val rispostaCorretta: String?,
                val risposteErrate: List<String>?, val spiegazione: String?
            )

            val docsConQuiz = coroutineScope {
                snapshot.documents
                    .filter { it.id != "_meta_" }
                    .map { doc ->
                        async {
                            val titolo    = doc.getString("titolo")    ?: return@async null
                            val corpo     = doc.getString("corpo")     ?: return@async null
                            val categoria = doc.getString("categoria") ?: ""
                            val emoji     = doc.getString("emoji")     ?: ""
                            // Legge dalla subcollection - controlla exists() per evitare
                            // documenti vuoti che restituiscono null su getString()
                            val quizDoc = try {
                                val d = doc.reference.collection("quiz").document("domanda").get().await()
                                if (d.exists()) d else null
                            } catch (_: Exception) { null }
                            @Suppress("UNCHECKED_CAST")
                            DocConQuiz(
                                externalId       = doc.id,
                                titolo           = titolo,
                                corpo            = corpo,
                                categoria        = categoria,
                                emoji            = emoji,
                                domanda          = quizDoc?.getString("domanda"),
                                rispostaCorretta = quizDoc?.getString("rispostaCorretta"),
                                risposteErrate   = quizDoc?.get("risposteErrate") as? List<String>,
                                spiegazione      = quizDoc?.getString("spiegazione")
                            )
                        }
                    }.awaitAll().filterNotNull()
            }

            for (d in docsConQuiz) {
                val esistente = repo.getByExternalId(d.externalId)
                if (esistente == null) {
                    val curId = repo.insertCuriosita(Curiosity(
                        externalId = d.externalId, title = d.titolo, body = d.corpo,
                        category = d.categoria, emoji = d.emoji
                    ))
                    if (d.domanda != null && d.rispostaCorretta != null) {
                        repo.insertQuizQuestion(QuizQuestion(
                            curiosityId   = curId.toInt(),
                            questionText  = d.domanda,
                            correctAnswer = d.rispostaCorretta,
                            wrongAnswer1  = d.risposteErrate?.getOrElse(0) { "" } ?: "",
                            wrongAnswer2  = d.risposteErrate?.getOrElse(1) { "" } ?: "",
                            wrongAnswer3  = d.risposteErrate?.getOrElse(2) { "" } ?: "",
                            explanation   = d.spiegazione ?: "",
                            category      = d.categoria
                        ))
                    }
                    nuove++
                } else {
                    repo.updateCuriosita(esistente.copy(
                        title = d.titolo, body = d.corpo,
                        category = d.categoria, emoji = d.emoji
                    ))
                    if (d.domanda != null && d.rispostaCorretta != null) {
                        val qEsistente = repo.getQuizByCuriosityId(esistente.id)
                        val qNuovo = QuizQuestion(
                            id            = qEsistente?.id ?: 0,
                            curiosityId   = esistente.id,
                            questionText  = d.domanda,
                            correctAnswer = d.rispostaCorretta,
                            wrongAnswer1  = d.risposteErrate?.getOrElse(0) { "" } ?: "",
                            wrongAnswer2  = d.risposteErrate?.getOrElse(1) { "" } ?: "",
                            wrongAnswer3  = d.risposteErrate?.getOrElse(2) { "" } ?: "",
                            explanation   = d.spiegazione ?: "",
                            category      = d.categoria
                        )
                        // update se esiste, insert se mancante (es. sync precedente fallita)
                        if (qEsistente != null) repo.updateQuizQuestion(qNuovo)
                        else repo.insertQuizQuestion(qNuovo)
                    }
                    aggiornate++
                }
            }
            contentPrefs.setContentVersion(versioneRemota.toInt())
            SyncResult.Success(nuove, aggiornate)
        } catch (e: Exception) {
            SyncResult.Errore(e.message ?: "Errore sync")
        }
    }
}