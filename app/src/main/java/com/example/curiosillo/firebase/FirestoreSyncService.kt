package com.example.curiosillo.firebase

import android.util.Log
import com.example.curiosillo.data.AppDatabase
import com.example.curiosillo.data.ContentPreferences
import com.example.curiosillo.data.Curiosity
import com.example.curiosillo.data.QuizQuestion
import com.example.curiosillo.repository.CuriosityRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import androidx.room.withTransaction

/**
 * Sincronizza le curiosità da Firestore → Room locale.
 * Versione MASSIMAMENTE OTTIMIZZATA:
 * 1. Unica chiamata Firestore.
 * 2. Unica transazione Room per tutte le scritture.
 * 3. Riduzione query di controllo ridondanti tramite cache locale.
 * 4. Preservazione degli stati utente (isRead, bookmark, note, ecc).
 */
class FirestoreSyncService(
    private val repo:         CuriosityRepository,
    private val contentPrefs: ContentPreferences,
    private val database:     AppDatabase 
) {
    private val db = FirebaseFirestore.getInstance()

    sealed class SyncResult {
        data class Success(val nuove: Int, val aggiornate: Int) : SyncResult()
        object NessunaModifica : SyncResult()
        data class Errore(val messaggio: String) : SyncResult()
    }

    suspend fun sync(): SyncResult {
        return try {
            val metaDoc = db.collection("curiosita").document("_meta_").get().await()
            val versioneRemota = metaDoc.getLong("versione") ?: 1L
            val versioneLocale = contentPrefs.getContentVersion().toLong()

            val dbVuoto = repo.totaleCuriosità() == 0
            val quizMancanti = repo.quizNonRisposti() == 0 && !dbVuoto
            
            if (!dbVuoto && !quizMancanti && versioneRemota <= versioneLocale) {
                return SyncResult.NessunaModifica
            }

            val snapshot = db.collection("curiosita")
                .whereNotEqualTo("__name__", "_meta_")
                .get().await()

            if (snapshot.isEmpty) return SyncResult.NessunaModifica

            // 1. Mappatura dati remoti
            val itemsRemoti = snapshot.documents.mapNotNull { doc ->
                val titolo = doc.getString("titolo") ?: return@mapNotNull null
                RemoteData(
                    externalId = doc.id,
                    curiosity = Curiosity(
                        externalId = doc.id,
                        title = titolo,
                        body = doc.getString("corpo") ?: "",
                        category = doc.getString("categoria") ?: "Generale",
                        emoji = doc.getString("emoji") ?: "✨"
                    ),
                    quizData = doc.getString("domanda")?.let { domanda ->
                        @Suppress("UNCHECKED_CAST")
                        val risposte = (doc.get("risposteErrate") ?: doc.get("risposte_errate")) as? List<String>
                        QuizData(
                            domanda = domanda,
                            corretta = doc.getString("rispostaCorretta") ?: doc.getString("risposta_corretta") ?: "",
                            errate = risposte ?: emptyList(),
                            spiegazione = doc.getString("spiegazione") ?: ""
                        )
                    }
                )
            }

            var nuove = 0
            var aggiornate = 0

            // 2. Transazione Room atomica
            database.withTransaction {
                val locali = repo.getTutteLeCuriosita().associateBy { it.externalId }
                val remoteIds = itemsRemoti.map { it.externalId }

                for (remote in itemsRemoti) {
                    val locale = locali[remote.externalId]
                    val finalCuriosityId: Int

                    if (locale == null) {
                        finalCuriosityId = repo.insertCuriosita(remote.curiosity).toInt()
                        nuove++
                    } else {
                        finalCuriosityId = locale.id
                        // AGGIORNAMENTO: preserva i dati dell'utente (isRead, bookmark, note, ecc)
                        repo.updateCuriosita(remote.curiosity.copy(
                            id = locale.id,
                            isRead = locale.isRead,
                            isBookmarked = locale.isBookmarked,
                            isIgnorata = locale.isIgnorata,
                            readAt = locale.readAt,
                            nota = locale.nota,
                            voto = locale.voto,
                            approfondimentoAi = locale.approfondimentoAi
                        ))
                        aggiornate++
                    }

                    // Sincronizza Quiz
                    remote.quizData?.let { qd ->
                        val qEsistente = repo.getQuizByCuriosityId(finalCuriosityId)
                        val quiz = QuizQuestion(
                            id = qEsistente?.id ?: 0,
                            curiosityId = finalCuriosityId,
                            questionText = qd.domanda,
                            correctAnswer = qd.corretta,
                            wrongAnswer1 = qd.errate.getOrElse(0) { "" },
                            wrongAnswer2 = qd.errate.getOrElse(1) { "" },
                            wrongAnswer3 = qd.errate.getOrElse(2) { "" },
                            explanation = qd.spiegazione,
                            category = remote.curiosity.category
                        )
                        if (qEsistente != null) repo.updateQuizQuestion(quiz)
                        else repo.insertQuizQuestion(quiz)
                    }
                }

                // 3. Rimozione orfani
                repo.deleteMissing(remoteIds)
            }

            contentPrefs.setContentVersion(versioneRemota.toInt())
            SyncResult.Success(nuove, aggiornate)

        } catch (e: Exception) {
            Log.e("SyncOptimized", "Errore sync: ${e.message}")
            SyncResult.Errore(e.message ?: "Errore sconosciuto")
        }
    }

    private data class RemoteData(val externalId: String, val curiosity: Curiosity, val quizData: QuizData?)
    private data class QuizData(val domanda: String, val corretta: String, val errate: List<String>, val spiegazione: String)
}
