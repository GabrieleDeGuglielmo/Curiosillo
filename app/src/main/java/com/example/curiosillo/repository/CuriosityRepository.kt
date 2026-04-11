package com.example.curiosillo.repository

import com.example.curiosillo.data.*
import com.example.curiosillo.firebase.FirebaseManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow

class CuriosityRepository(
    private val db: AppDatabase,
    private val appScope: CoroutineScope
) {

    private val curDao        = db.curiosityDao()
    private val quizDao       = db.quizQuestionDao()
    private val quizAnswerDao = db.quizAnswerDao()
    private val badgeDao      = db.badgeDao()
    private val sessionDao    = db.quizSessionDao()
    private val scopertaDao   = db.scopertaDao()

    // Scrive su Firebase in background usando lo scope applicazione:
    // non viene cancellato se il ViewModel viene distrutto
    private fun syncCloud(block: suspend () -> Unit) {
        val uid = FirebaseManager.uid ?: return
        appScope.launch { try { block() } catch (_: Exception) {} }
    }

    suspend fun initializeSeedData() { /* contenuti arrivano dal JSON remoto */ }

    // ── Curiosità ─────────────────────────────────────────────────────────────

    suspend fun getNext(categorie: Set<String>): Curiosity? =
        if (categorie.isEmpty()) curDao.getNext()
        else curDao.getNextFiltered(categorie.toList())

    suspend fun markAsRead(c: Curiosity) {
        curDao.update(c.copy(isRead = true, readAt = System.currentTimeMillis()))
        val uid = FirebaseManager.uid ?: return
        c.externalId?.let { syncCloud { FirebaseManager.aggiungiPillolaLetta(uid, it) } }
    }

    suspend fun toggleBookmark(c: Curiosity) {
        val nuovoStato = !c.isBookmarked
        curDao.update(c.copy(isBookmarked = nuovoStato))
        val uid = FirebaseManager.uid ?: return
        c.externalId?.let { exId ->
            syncCloud {
                if (nuovoStato) FirebaseManager.aggiungiBookmark(uid, exId)
                else FirebaseManager.rimuoviBookmark(uid, exId)
            }
        }
    }
    suspend fun countTotaliQuiz() = quizDao.countTotali()
    suspend fun rimuoviBookmark(c: Curiosity) {
        curDao.update(c.copy(isBookmarked = false))
        val uid = FirebaseManager.uid ?: return
        c.externalId?.let { syncCloud { FirebaseManager.rimuoviBookmark(uid, it) } }
    }

    suspend fun getBookmarked(): List<Curiosity> = curDao.getBookmarked()

    suspend fun searchBookmarked(query: String, cats: Set<String>): List<Curiosity> =
        curDao.searchBookmarked(query, cats.toList(), cats.isEmpty())

    suspend fun categorieBookmark(): List<String> = curDao.getCategorie()

    suspend fun cercaBookmark(query: String, cats: Set<String>): List<Curiosity> =
        curDao.searchBookmarked(query, cats.toList(), cats.isEmpty())

    suspend fun getCategorie(): List<String> = curDao.getCategorie()

    suspend fun salvaNota(curiosity: Curiosity, nota: String) {
        curDao.update(curiosity.copy(nota = nota))
        val uid = FirebaseManager.uid ?: return
        curiosity.externalId?.let { syncCloud { FirebaseManager.salvaNota(uid, it, nota) } }
    }

    suspend fun setVoto(curiosity: Curiosity, voto: Int?) =
        curDao.update(curiosity.copy(voto = voto))

    suspend fun toggleIgnora(curiosity: Curiosity) {
        val nuovoStato = !curiosity.isIgnorata
        curDao.update(curiosity.copy(isIgnorata = nuovoStato))
        val uid = FirebaseManager.uid ?: return
        curiosity.externalId?.let { exId ->
            syncCloud {
                if (nuovoStato) FirebaseManager.aggiungiIgnorata(uid, exId)
                else FirebaseManager.rimuoviIgnorata(uid, exId)
            }
        }
    }

    suspend fun salvaApprofondimentoAi(curiosity: Curiosity, testo: String) =
        curDao.update(curiosity.copy(approfondimentoAi = testo))

    suspend fun getTutteImparate(categorie: Set<String>): List<Curiosity> =
        if (categorie.isEmpty()) curDao.getTutteImparate()
        else curDao.getPerRipassoFiltered(Long.MAX_VALUE, categorie.toList())

    suspend fun getPerRipasso(giorniMinimi: Int, categorie: Set<String>): List<Curiosity> {
        val soglia = if (giorniMinimi == 0) Long.MAX_VALUE
        else System.currentTimeMillis() - giorniMinimi * 24L * 60 * 60 * 1000
        return if (categorie.isEmpty()) curDao.getPerRipasso(soglia)
        else curDao.getPerRipassoFiltered(soglia, categorie.toList())
    }

    fun getPerRipassoFlow(soglia: Long): Flow<List<Curiosity>> =
        curDao.getPerRipassoFlow(soglia)

    fun getPerRipassoFilteredFlow(soglia: Long, cats: List<String>): Flow<List<Curiosity>> =
        curDao.getPerRipassoFilteredFlow(soglia, cats)

    /** Usato da SyncManager per migrare le pillole lette su Firebase */
    suspend fun getPilloleLette(): List<Curiosity> = curDao.getPilloleLette()

    // ── Sync remoto ───────────────────────────────────────────────────────────

    suspend fun getByExternalId(externalId: String): Curiosity? =
        curDao.getByExternalId(externalId)

    suspend fun deleteMissing(remoteIds: List<String>) =
        curDao.deleteMissing(remoteIds)

    suspend fun insertCuriosita(c: Curiosity): Long = curDao.insert(c)

    suspend fun updateCuriosita(c: Curiosity) = curDao.update(c)

    /**
     * Aggiorna una pillola locale con i dati provenienti da Firebase (Admin).
     * Preserva lo stato dell'utente (isRead, bookmark, note, ecc).
     */
    suspend fun syncLocaleConRemoto(c: FirebaseManager.CuriositaRemota) {
        db.withTransaction {
            val locale = curDao.getByExternalId(c.externalId)
            val curId: Int = if (locale == null) {
                curDao.insert(Curiosity(
                    externalId = c.externalId,
                    title = c.titolo,
                    body = c.corpo,
                    category = c.categoria,
                    emoji = c.emoji
                )).toInt()
            } else {
                curDao.update(locale.copy(
                    title = c.titolo,
                    body = c.corpo,
                    category = c.categoria,
                    emoji = c.emoji
                ))
                locale.id
            }

            // Sincronizza anche il quiz se presente
            if (c.domanda != null) {
                val qEsistente = quizDao.getByCuriosityId(curId)
                val quiz = QuizQuestion(
                    id = qEsistente?.id ?: 0,
                    curiosityId = curId,
                    questionText = c.domanda,
                    correctAnswer = c.rispostaCorretta ?: "",
                    wrongAnswer1 = c.risposteErrate?.getOrNull(0) ?: "",
                    wrongAnswer2 = c.risposteErrate?.getOrNull(1) ?: "",
                    wrongAnswer3 = c.risposteErrate?.getOrNull(2) ?: "",
                    explanation = c.spiegazione ?: "",
                    category = c.categoria
                )
                if (qEsistente != null) quizDao.update(quiz)
                else quizDao.insert(quiz)
            }
        }
    }

    suspend fun getQuizByCuriosityId(curiosityId: Int): QuizQuestion? =
        quizDao.getByCuriosityId(curiosityId)

    suspend fun insertQuizQuestion(q: QuizQuestion) = quizDao.insert(q)

    suspend fun updateQuizQuestion(q: QuizQuestion) = quizDao.update(q)

    // ── Quiz ──────────────────────────────────────────────────────────────────

    suspend fun getQuizQuestionsWithCategory(n: Int, categorie: Set<String>): List<QuizQuestion> =
        if (categorie.isEmpty()) quizDao.getRandomWithCategory(n)
        else quizDao.getRandomFiltered(n, categorie.toList())

    /** Per il duello: domande da tutto il DB, non solo pillole lette */
    suspend fun getQuizQuestionsAll(n: Int): List<QuizQuestion> =
        quizDao.getRandomAll(n)

    suspend fun getQuizQuestionsTutteRandom(): List<QuizQuestion> =
        quizDao.getAllRandomly()

    suspend fun countAvailableQuestions(categorie: Set<String>): Int =
        quizDao.countAvailable()

    suspend fun salvaRisposta(questionId: Int, isCorrect: Boolean) {
        quizAnswerDao.inserisci(QuizAnswer(questionId = questionId, isCorrect = isCorrect))
        val uid = FirebaseManager.uid ?: return
        syncCloud { FirebaseManager.aggiungiQuizRisposto(uid, questionId) }
    }

    // ── Sessioni quiz ─────────────────────────────────────────────────────────

    suspend fun salvaSessioneQuiz(corrette: Int, totale: Int, categoria: String) =
        sessionDao.inserisci(QuizSession(correctAnswers = corrette, totalAnswers = totale, categoria = categoria))

    suspend fun statPerCategoria(): List<StatCategoria> = sessionDao.statPerCategoria()

    suspend fun ultime20Sessioni(): List<QuizSession> = sessionDao.ultime20()

    // ── Badge ─────────────────────────────────────────────────────────────────

    suspend fun badgeSbloccati(): List<BadgeSbloccato>  = badgeDao.getTutti()
    suspend fun idBadgeSbloccati(): Set<String>         = badgeDao.getTutti().map { it.id }.toSet()
    suspend fun sbloccaBadge(b: BadgeSbloccato)         = badgeDao.inserisci(b)
    suspend fun isBadgeSbloccato(id: String): Boolean  = badgeDao.esiste(id)

    // ── Scoperte ──────────────────────────────────────────────────────────────

    fun getScoperteFlow(): Flow<List<Scoperta>> = scopertaDao.getTutteFlow()

    suspend fun salvaScoperta(s: Scoperta): Int {
        val uid = FirebaseManager.uid
        if (uid != null) {
            val firestoreId = FirebaseManager.salvaScoperta(uid, s)
            scopertaDao.inserisci(s.copy(firestoreId = firestoreId))
        } else {
            scopertaDao.inserisci(s)
        }
        return scopertaDao.countTutte()
    }

    suspend fun syncScoperte() {
        val uid = FirebaseManager.uid ?: return
        val remote = FirebaseManager.getScoperte(uid)
        // Per semplicità facciamo un reset locale e ricarichiamo tutto
        // In una app reale si farebbe un merge più sofisticato
        scopertaDao.resetTutte()
        remote.forEach { scopertaDao.inserisci(it) }
    }

    // ── Statistiche ───────────────────────────────────────────────────────────

    suspend fun totaleCuriosità()   = curDao.totaleCuriosità()
    suspend fun curiositàImparate() = curDao.curiositàImparate()
    suspend fun quizNonRisposti()   = quizDao.quizNonRisposti()
    suspend fun totaleBookmark()    = curDao.totaleBookmark()
    suspend fun totaleIgnorate()    = curDao.totaleIgnorate()

    suspend fun getPilloleIgnorate(): List<Curiosity> = curDao.getPilloleIgnorate()

    suspend fun getTutteLeCuriosita(): List<Curiosity> = curDao.getTutte()

    suspend fun ripristinaIgnorata(c: Curiosity) {
        curDao.update(c.copy(isIgnorata = false))
        val uid = FirebaseManager.uid ?: return
        c.externalId?.let { syncCloud { FirebaseManager.rimuoviIgnorata(uid, it) } }
    }

    suspend fun resetBadge() = badgeDao.resetTutti()

    // ── Metodi silent (solo Room, no Firebase — usati dal ripristino cloud) ────

    suspend fun markAsReadSilent(c: Curiosity) =
        curDao.update(c.copy(isRead = true, readAt = System.currentTimeMillis()))

    suspend fun setBookmarkSilent(c: Curiosity, valore: Boolean) =
        curDao.update(c.copy(isBookmarked = valore))

    suspend fun setIgnorataSilent(c: Curiosity, valore: Boolean) =
        curDao.update(c.copy(isIgnorata = valore))

    suspend fun setNotaSilent(c: Curiosity, nota: String) =
        curDao.update(c.copy(nota = nota))

    suspend fun salvaRispostaSilent(questionId: Int) {
        // Inserisce solo se non gia' presente (IGNORE strategy)
        quizAnswerDao.inserisci(QuizAnswer(questionId = questionId, isCorrect = true))
    }

    // ── Reset ─────────────────────────────────────────────────────────────────

    suspend fun resetProgressi() {
        curDao.resetProgressi()
        quizAnswerDao.resetTutto()
        badgeDao.resetTutti()
        scopertaDao.resetTutte()
    }
}
