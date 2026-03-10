package com.example.curiosillo.network

import com.example.curiosillo.data.ContentPreferences
import com.example.curiosillo.data.Curiosity
import com.example.curiosillo.data.QuizQuestion
import com.example.curiosillo.repository.CuriosityRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

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
            if (!dbVuoto && versioneRemota <= versioneLocale) {
                return SyncResult.NessunaModifica
            }

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

                // Dati quiz integrati nel documento
                val domanda          = doc.getString("domanda")
                val rispostaCorretta = doc.getString("rispostaCorretta")
                @Suppress("UNCHECKED_CAST")
                val risposteErrate   = doc.get("risposteErrate") as? List<String>
                val spiegazione      = doc.getString("spiegazione")

                val esistente = repo.getByExternalId(externalId)

                if (esistente == null) {
                    val curId = repo.insertCuriosita(Curiosity(
                        externalId = externalId, title = titolo, body = corpo,
                        category = categoria, emoji = emoji
                    ))
                    
                    if (domanda != null && rispostaCorretta != null) {
                        repo.insertQuizQuestion(QuizQuestion(
                            curiosityId   = curId.toInt(),
                            questionText  = domanda,
                            correctAnswer = rispostaCorretta,
                            wrongAnswer1  = risposteErrate?.getOrElse(0) { "" } ?: "",
                            wrongAnswer2  = risposteErrate?.getOrElse(1) { "" } ?: "",
                            wrongAnswer3  = risposteErrate?.getOrElse(2) { "" } ?: "",
                            explanation   = spiegazione ?: "",
                            category      = categoria
                        ))
                    }
                    nuove++
                } else {
                    repo.updateCuriosita(esistente.copy(
                        title = titolo, body = corpo, category = categoria, emoji = emoji
                    ))
                    
                    if (domanda != null && rispostaCorretta != null) {
                        val qEsistente = repo.getQuizByCuriosityId(esistente.id)
                        val qNuovo = QuizQuestion(
                            id            = qEsistente?.id ?: 0,
                            curiosityId   = esistente.id,
                            questionText  = domanda,
                            correctAnswer = rispostaCorretta,
                            wrongAnswer1  = risposteErrate?.getOrElse(0) { "" } ?: "",
                            wrongAnswer2  = risposteErrate?.getOrElse(1) { "" } ?: "",
                            wrongAnswer3  = risposteErrate?.getOrElse(2) { "" } ?: "",
                            explanation   = spiegazione ?: "",
                            category      = categoria
                        )
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
