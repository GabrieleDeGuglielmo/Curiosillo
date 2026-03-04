package com.example.curiosillo.repository

import com.example.curiosillo.data.*

class CuriosityRepository(private val db: AppDatabase) {

    private val curDao = db.curiosityDao()
    private val quizDao = db.quizQuestionDao()
    private val quizAnswerDao = db.quizAnswerDao()
    private val bookmarkDao = db.bookmarkDao()


    // ── Seed ──────────────────────────────────────────────────────────────────

    suspend fun initializeSeedData() {
        if (curDao.totaleCuriosità() > 0) return

        val curiosities = listOf(
            Curiosity(
                title = "Il miele non scade mai",
                body = "Gli archeologi hanno trovato miele in tombe egizie vecchie di oltre 3.000 anni, " +
                        "ancora perfettamente commestibile! La sua composizione chimica — bassissima umidità " +
                        "e pH acido — crea un ambiente ostile per batteri e muffe. " +
                        "Purché rimanga sigillato, il miele può durare praticamente per sempre.",
                category = "Scienza", emoji = "Miele"
            ),
            Curiosity(
                title = "I polpi hanno tre cuori",
                body = "Il corpo di un polpo nasconde ben tre cuori: uno pompa sangue ossigenato verso " +
                        "il resto del corpo, mentre due cuori branchiali lo fanno circolare attraverso le " +
                        "branchie. Quando i polpi nuotano, il cuore principale si ferma — ecco perché " +
                        "preferiscono strisciare sul fondale!",
                category = "Animali", emoji = "Polpo"
            ),
            Curiosity(
                title = "Le banane sono leggermente radioattive",
                body = "Ogni banana contiene una piccola quantità di potassio-40, un isotopo naturalmente " +
                        "radioattivo. Nessun pericolo: la dose è infinitesimale e il corpo regola autonomamente " +
                        "i livelli di potassio. In ambito scientifico esiste persino un'unità informale: " +
                        "la BED (Banana Equivalent Dose)!",
                category = "Scienza", emoji = "Banana"
            ),
            Curiosity(
                title = "Gli elefanti non sanno saltare",
                body = "L'elefante è l'unico mammifero che non riesce a saltare. Le ossa troppo dense " +
                        "e il peso colossale (fino a 6 tonnellate) rendono impossibile staccarsi dal suolo. " +
                        "Nonostante questo compensano con resistenza straordinaria: riescono a camminare " +
                        "fino a 50 km al giorno.",
                category = "Animali", emoji = "Elefante"
            ),
            Curiosity(
                title = "Oxford è più antica dell'Impero Azteco",
                body = "L'Università di Oxford ha iniziato la sua attività didattica intorno al 1096, " +
                        "mentre l'Impero Azteco fu fondato solo nel 1427. Quando Hernán Cortés conquistò " +
                        "Tenochtitlan nel 1519, Oxford era già un centro di sapere da oltre quattro secoli!",
                category = "Storia", emoji = "Storia"
            )
        )
        val ids = curDao.insertAll(curiosities)

        val questions = listOf(
            QuizQuestion(
                curiosityId = ids[0].toInt(),
                questionText = "Per quanto tempo può conservarsi il miele in condizioni ottimali?",
                correctAnswer = "Non scade mai — dura millenni",
                wrongAnswer1 = "Circa 2 anni",
                wrongAnswer2 = "10-15 anni al massimo",
                wrongAnswer3 = "50 anni se ben sigillato",
                explanation = "Il miele trovato in tombe egizie di 3.000 anni fa era ancora commestibile!"
            ),
            QuizQuestion(
                curiosityId = ids[1].toInt(),
                questionText = "Quanti cuori ha un polpo?",
                correctAnswer = "3 cuori",
                wrongAnswer1 = "1 cuore",
                wrongAnswer2 = "2 cuori",
                wrongAnswer3 = "4 cuori",
                explanation = "Due cuori branchiali + uno principale. Quando nuota, quello principale si ferma!"
            ),
            QuizQuestion(
                curiosityId = ids[2].toInt(),
                questionText = "Quale sostanza rende le banane leggermente radioattive?",
                correctAnswer = "Il potassio-40, un isotopo naturale",
                wrongAnswer1 = "Tracce di uranio assorbite dal terreno",
                wrongAnswer2 = "Carbonio-14 prodotto dalla fotosintesi",
                wrongAnswer3 = "Radon proveniente dall'atmosfera",
                explanation = "Il potassio-40 è naturale e presente in molti alimenti. Assolutamente innocuo!"
            ),
            QuizQuestion(
                curiosityId = ids[3].toInt(),
                questionText = "Perché gli elefanti non riescono a saltare?",
                correctAnswer = "Ossa troppo dense e peso eccessivo",
                wrongAnswer1 = "Le zampe rigide non permettono la spinta",
                wrongAnswer2 = "Mancano dei muscoli adatti al salto",
                wrongAnswer3 = "Hanno paura di cadere",
                explanation = "Con 6 tonnellate di peso, staccarsi dal suolo è fisicamente impossibile!"
            ),
            QuizQuestion(
                curiosityId = ids[4].toInt(),
                questionText = "Quale tra questi è più antico dell'Impero Azteco?",
                correctAnswer = "L'Università di Oxford (fondata ~1096)",
                wrongAnswer1 = "La scoperta dell'America da parte di Colombo (1492)",
                wrongAnswer2 = "La caduta di Costantinopoli (1453)",
                wrongAnswer3 = "L'invenzione della stampa a caratteri mobili (1440 ca.)",
                explanation = "Oxford nacque intorno al 1096; gli Aztechi fondarono il loro impero nel 1427!"
            )
        )
        quizDao.insertAll(questions)
    }

    // helper privato
    private fun Set<String>.toFiltro() =
        Pair(if (isEmpty()) emptyList() else toList(), if (isEmpty()) 1 else 0)

    // ── Curiosità ─────────────────────────────────────────────────────────────

    suspend fun getNext(categorie: Set<String> = emptySet()): Curiosity? {
        val (lista, tutte) = categorie.toFiltro()
        return curDao.getNext(lista, tutte)
    }

    suspend fun markAsRead(c: Curiosity) = curDao.update(c.copy(isRead = true))
    suspend fun toggleBookmark(c: Curiosity) = curDao.update(c.copy(isBookmarked = !c.isBookmarked))
    suspend fun getBookmarked() = curDao.getBookmarked()
    suspend fun getCategorie() = curDao.getCategorie()
    suspend fun cercaBookmark(
        query: String = "",
        categorie: Set<String> = emptySet()
    ): List<Curiosity> {
        val (lista, tutte) = categorie.toFiltro()
        return bookmarkDao.cerca(query, lista, tutte)
    }

    suspend fun categorieBookmark() = bookmarkDao.categorieBookmark()

    suspend fun rimuoviBookmark(c: Curiosity) =
        bookmarkDao.update(c.copy(isBookmarked = false))

    // ── Quiz ──────────────────────────────────────────────────────────────────

    suspend fun getQuizQuestionsWithCategory(n: Int, categorie: Set<String> = emptySet()):
            List<QuizQuestionWithCategory> {
        val (lista, tutte) = categorie.toFiltro()
        return quizDao.getRandomWithCategory(n, lista, tutte)
    }

    suspend fun countAvailableQuestions(categorie: Set<String> = emptySet()): Int {
        val (lista, tutte) = categorie.toFiltro()
        return quizDao.countAvailable(lista, tutte)
    }

    suspend fun salvaRisposta(questionId: Int, isCorrect: Boolean) =
        quizAnswerDao.inserisci(QuizAnswer(questionId = questionId, isCorrect = isCorrect))

    // ── Statistiche profilo ───────────────────────────────────────────────────

    suspend fun totaleCuriosità() = curDao.totaleCuriosità()
    suspend fun curiositàImparate() = curDao.curiositàImparate()

    suspend fun quizNonRisposti(categorie: Set<String> = emptySet()): Int {
        val (lista, tutte) = categorie.toFiltro()
        return quizDao.quizNonRisposti(lista, tutte)
    }

    suspend fun totaleBookmark() = curDao.totaleBookmark()

    // ── Reset ─────────────────────────────────────────────────────────────────

    suspend fun resetProgressi() {
        db.userProgressDao().resetProgressi()
        quizAnswerDao.resetTutto()
    }
}