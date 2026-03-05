package com.example.curiosillo.repository

import com.example.curiosillo.data.*

class CuriosityRepository(private val db: AppDatabase) {

    private val curDao        = db.curiosityDao()
    private val quizDao       = db.quizQuestionDao()
    private val quizAnswerDao = db.quizAnswerDao()
    private val badgeDao      = db.badgeDao()
    private val sessionDao    = db.quizSessionDao()

    suspend fun initializeSeedData() { /* contenuti arrivano dal JSON remoto */ }

    // ── Curiosità ─────────────────────────────────────────────────────────────

    suspend fun getNext(categorie: Set<String>): Curiosity? =
        if (categorie.isEmpty()) curDao.getNext()
        else curDao.getNextFiltered(categorie.toList())

    suspend fun markAsRead(c: Curiosity) =
        curDao.update(c.copy(isRead = true, readAt = System.currentTimeMillis()))

    suspend fun toggleBookmark(c: Curiosity) =
        curDao.update(c.copy(isBookmarked = !c.isBookmarked))

    suspend fun rimuoviBookmark(c: Curiosity) =
        curDao.update(c.copy(isBookmarked = false))

    suspend fun getBookmarked(): List<Curiosity> = curDao.getBookmarked()

    suspend fun searchBookmarked(query: String, cats: Set<String>): List<Curiosity> =
        curDao.searchBookmarked(query, cats.toList(), cats.isEmpty())

    suspend fun categorieBookmark(): List<String> = curDao.getCategorie()

    suspend fun cercaBookmark(query: String, cats: Set<String>): List<Curiosity> =
        curDao.searchBookmarked(query, cats.toList(), cats.isEmpty())

    suspend fun getCategorie(): List<String> = curDao.getCategorie()

    suspend fun salvaNota(curiosity: Curiosity, nota: String) =
        curDao.update(curiosity.copy(nota = nota))

    suspend fun getTutteImparate(categorie: Set<String>): List<Curiosity> =
        if (categorie.isEmpty()) curDao.getTutteImparate()
        else curDao.getPerRipassoFiltered(Long.MAX_VALUE, categorie.toList())

    suspend fun getPerRipasso(giorniMinimi: Int, categorie: Set<String>): List<Curiosity> {
        val soglia = if (giorniMinimi == 0) Long.MAX_VALUE
        else System.currentTimeMillis() - giorniMinimi * 24L * 60 * 60 * 1000
        return if (categorie.isEmpty()) curDao.getPerRipasso(soglia)
        else curDao.getPerRipassoFiltered(soglia, categorie.toList())
    }

    /** Usato da SyncManager per migrare le pillole lette su Firebase */
    suspend fun getPilloleLette(): List<Curiosity> = curDao.getPilloleLette()

    // ── Sync remoto ───────────────────────────────────────────────────────────

    suspend fun getByExternalId(externalId: String): Curiosity? =
        curDao.getByExternalId(externalId)

    suspend fun insertCuriosita(c: Curiosity): Long = curDao.insert(c)

    suspend fun updateCuriosita(c: Curiosity) = curDao.update(c)

    suspend fun getQuizByCuriosityId(curiosityId: Int): QuizQuestion? =
        quizDao.getByCuriosityId(curiosityId)

    suspend fun insertQuizQuestion(q: QuizQuestion) = quizDao.insert(q)

    suspend fun updateQuizQuestion(q: QuizQuestion) = quizDao.update(q)

    // ── Quiz ──────────────────────────────────────────────────────────────────

    suspend fun getQuizQuestionsWithCategory(n: Int, categorie: Set<String>): List<QuizQuestion> =
        if (categorie.isEmpty()) quizDao.getRandomWithCategory(n)
        else quizDao.getRandomFiltered(n, categorie.toList())

    suspend fun countAvailableQuestions(categorie: Set<String>): Int =
        quizDao.countAvailable()

    suspend fun salvaRisposta(questionId: Int, isCorrect: Boolean) =
        quizAnswerDao.inserisci(QuizAnswer(questionId = questionId, isCorrect = isCorrect))

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

    // ── Statistiche ───────────────────────────────────────────────────────────

    suspend fun totaleCuriosità()   = curDao.totaleCuriosità()
    suspend fun curiositàImparate() = curDao.curiositàImparate()
    suspend fun quizNonRisposti()   = quizDao.quizNonRisposti()
    suspend fun totaleBookmark()    = curDao.totaleBookmark()

    // ── Reset ─────────────────────────────────────────────────────────────────

    suspend fun resetProgressi() {
        curDao.resetProgressi()
        quizAnswerDao.resetTutto()
    }
}
