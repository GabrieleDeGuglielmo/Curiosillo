package com.example.curiosillo.firebase

import android.util.Log
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
            val versioneLocale = contentPrefs.getContentVersion().toLong()

            val dbVuoto = repo.totaleCuriosità() == 0
            
            // Forziamo la sync se la versione remota è superiore, o se mancano quiz.
            val quizMancanti = repo.quizNonRisposti() == 0 && !dbVuoto
            if (!dbVuoto && !quizMancanti && versioneRemota <= versioneLocale) {
                return SyncResult.NessunaModifica
            }

            val snapshot = db.collection("curiosita")
                .whereNotEqualTo("__name__", "_meta_")
                .get().await()

            var nuove      = 0
            var aggiornate = 0

            data class DocConQuiz(
                val externalId: String,
                val titolo: String, val corpo: String,
                val categoria: String, val emoji: String,
                val domanda: String?, val rispostaCorretta: String?,
                val risposteErrate: List<String>?, val spiegazione: String?
            )

            val docsConQuiz = snapshot.documents
                .filter { it.id != "_meta_" }
                .mapNotNull { doc ->
                    val titolo    = doc.getString("titolo")    ?: return@mapNotNull null
                    val corpo     = doc.getString("corpo")     ?: return@mapNotNull null
                    val categoria = doc.getString("categoria") ?: "Generale"
                    val emoji     = doc.getString("emoji")     ?: "✨"
                    
                    @Suppress("UNCHECKED_CAST")
                    DocConQuiz(
                        externalId       = doc.id,
                        titolo           = titolo,
                        corpo            = corpo,
                        categoria        = categoria,
                        emoji            = emoji,
                        // Utilizziamo i nomi campi corretti (CamelCase come in FirebaseManager)
                        domanda          = doc.getString("domanda"),
                        rispostaCorretta = doc.getString("risposta_corretta"),
                        risposteErrate   = doc.get("risposte_errate") as? List<String>,
                        spiegazione      = doc.getString("spiegazione")
                    )
                }

            // Manteniamo traccia degli ID presenti sul server per eventuale pulizia orfani
            val remoteIds = docsConQuiz.map { it.externalId }

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
                    // AGGIORNAMENTO: Qui aggiorniamo la categoria se è cambiata!
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
                        if (qEsistente != null) repo.updateQuizQuestion(qNuovo)
                        else repo.insertQuizQuestion(qNuovo)
                    }
                    aggiornate++
                }
            }

            // Pulizia orfani: se una pillola non è più sul server, la rimuoviamo in locale
            repo.deleteMissing(remoteIds)

            Log.d("SyncDebug", "Sincronizzazione completata: nuove=$nuove, aggiornate=$aggiornate")
            contentPrefs.setContentVersion(versioneRemota.toInt())
            SyncResult.Success(nuove, aggiornate)
        } catch (e: Exception) {
            Log.e("SyncDebug", "Errore durante la sync: ${e.message}")
            SyncResult.Errore(e.message ?: "Errore sync")
        }
    }
}
