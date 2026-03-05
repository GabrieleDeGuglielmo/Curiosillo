package com.example.curiosillo.network

import com.example.curiosillo.data.ContentPreferences
import com.example.curiosillo.data.Curiosity
import com.example.curiosillo.data.QuizQuestion
import com.example.curiosillo.repository.CuriosityRepository

/**
 * Scarica il JSON remoto e sincronizza il database locale con strategia MERGE:
 *  - Pillole nuove (ID non presente) → INSERT
 *  - Pillole esistenti (stesso ID)   → UPDATE titolo/corpo/categoria/emoji
 *    mantenendo isRead, isBookmarked, nota, readAt dell'utente
 *  - Quiz nuovi collegati a pillole esistenti → INSERT se non già presenti
 *  - Pillole rimosse dal JSON → rimangono nel DB ma non vengono più servite
 *    (gestito dalla query getNext che filtra per externalId non nullo se vuoi,
 *     oppure semplicemente rimangono visibili — scelta conservativa)
 */
class ContentSyncService(
    private val repo:         CuriosityRepository,
    private val contentPrefs: ContentPreferences
) {

    sealed class SyncResult {
        data class Success(val nuove: Int, val aggiornate: Int) : SyncResult()
        object NessunaModifica : SyncResult()
        data class Errore(val messaggio: String) : SyncResult()
    }

    suspend fun sync(): SyncResult {
        return try {
            // 1. Scarica JSON
            val json = HttpClient.get(RemoteConfig.CONTENT_URL)

            // 2. Parsing
            val remoteContent = parseRemoteContent(json)

            // 3. Controlla versione
            val versioneLocale = contentPrefs.getContentVersion()
            if (remoteContent.version <= versioneLocale) {
                return SyncResult.NessunaModifica
            }

            // 4. Merge
            var nuove = 0
            var aggiornate = 0

            for (rc in remoteContent.curiosita) {
                val esistente = repo.getByExternalId(rc.id)

                if (esistente == null) {
                    // INSERT nuova pillola
                    val nuovaCuriosita = Curiosity(
                        externalId   = rc.id,
                        title        = rc.titolo,
                        body         = rc.corpo,
                        category     = rc.categoria,
                        emoji        = rc.emoji,
                        isRead       = false,
                        isBookmarked = false,
                        nota         = ""
                    )
                    val newId = repo.insertCuriosita(nuovaCuriosita)

                    // INSERT quiz se presente
                    rc.quiz?.let { q ->
                        repo.insertQuizQuestion(
                            QuizQuestion(
                                curiosityId   = newId.toInt(),
                                questionText  = q.domanda,
                                correctAnswer = q.rispostaCorretta,
                                wrongAnswer1  = q.risposteErrate.getOrElse(0) { "" },
                                wrongAnswer2  = q.risposteErrate.getOrElse(1) { "" },
                                wrongAnswer3  = q.risposteErrate.getOrElse(2) { "" },
                                explanation   = q.spiegazione,
                                category      = rc.categoria
                            )
                        )
                    }
                    nuove++

                } else {
                    // UPDATE solo i campi editoriali — preserva i dati utente
                    repo.updateCuriosita(
                        esistente.copy(
                            title    = rc.titolo,
                            body     = rc.corpo,
                            category = rc.categoria,
                            emoji    = rc.emoji
                            // isRead, isBookmarked, nota, readAt → invariati
                        )
                    )

                    // UPDATE quiz se presente e già esiste, oppure INSERT se mancava
                    rc.quiz?.let { q ->
                        val quizEsistente = repo.getQuizByCuriosityId(esistente.id)
                        if (quizEsistente != null) {
                            repo.updateQuizQuestion(
                                quizEsistente.copy(
                                    questionText  = q.domanda,
                                    correctAnswer = q.rispostaCorretta,
                                    wrongAnswer1  = q.risposteErrate.getOrElse(0) { "" },
                                    wrongAnswer2  = q.risposteErrate.getOrElse(1) { "" },
                                    wrongAnswer3  = q.risposteErrate.getOrElse(2) { "" },
                                    explanation   = q.spiegazione
                                )
                            )
                        } else {
                            repo.insertQuizQuestion(
                                QuizQuestion(
                                    curiosityId   = esistente.id,
                                    questionText  = q.domanda,
                                    correctAnswer = q.rispostaCorretta,
                                    wrongAnswer1  = q.risposteErrate.getOrElse(0) { "" },
                                    wrongAnswer2  = q.risposteErrate.getOrElse(1) { "" },
                                    wrongAnswer3  = q.risposteErrate.getOrElse(2) { "" },
                                    explanation   = q.spiegazione,
                                    category      = rc.categoria
                                )
                            )
                        }
                    }
                    aggiornate++
                }
            }

            // 5. Salva nuova versione
            contentPrefs.setContentVersion(remoteContent.version)

            SyncResult.Success(nuove, aggiornate)

        } catch (e: Exception) {
            SyncResult.Errore(e.message ?: "Errore sconosciuto")
        }
    }
}
