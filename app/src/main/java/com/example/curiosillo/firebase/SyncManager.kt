package com.example.curiosillo.firebase

import android.util.Log
import com.example.curiosillo.data.BadgeCatalogo
import com.example.curiosillo.data.BadgeSbloccato
import com.example.curiosillo.data.ContentPreferences
import com.example.curiosillo.data.GamificationPreferences
import com.example.curiosillo.repository.CuriosityRepository
import kotlinx.coroutines.flow.first

/**
 * Al primo login sincronizza i progressi locali (XP, streak, badge, pillole lette)
 * su Firestore. Nelle sessioni successive mantiene i due in sync.
 */
class SyncManager(
    private val repo:         CuriosityRepository,
    private val gamifPrefs:   GamificationPreferences,
    private val contentPrefs: ContentPreferences
) {

    /**
     * Migrazione one-shot per utenti esistenti: carica tutto il locale su Firebase.
     * Viene eseguita una sola volta dopo l'aggiornamento dell'app.
     */
    suspend fun migraLocaleVersoCloudSeNecessario(uid: String) {
        if (contentPrefs.isCloudMigrazioneDone()) return

        // XP e streak
        val xp           = gamifPrefs.xpTotali.first()
        val streak       = gamifPrefs.streakCorrente.first()
        val streakMassima = gamifPrefs.streakMassima.first()
        if (xp > 0 || streak > 0) {
            FirebaseManager.aggiornaProfilo(uid, mapOf(
                "xp"             to xp,
                "streakCorrente" to streak,
                "streakMassima"  to streakMassima
            ))
        }

        // Badge
        val badgeLocali = repo.idBadgeSbloccati().toList()
        badgeLocali.forEach { FirebaseManager.aggiungiBadge(uid, it) }

        // Pillole lette
        val pilloleLette = repo.getPilloleLette().mapNotNull { it.externalId }
        pilloleLette.forEach { FirebaseManager.aggiungiPillolaLetta(uid, it) }

        // Bookmark
        val bookmark = repo.getBookmarked().mapNotNull { it.externalId }
        bookmark.forEach { FirebaseManager.aggiungiBookmark(uid, it) }

        // Pillole ignorate
        val ignorate = repo.getPilloleIgnorate().mapNotNull { it.externalId }
        ignorate.forEach { FirebaseManager.aggiungiIgnorata(uid, it) }

        // Note
        repo.getTutteLeCuriosita()
            .filter { !it.nota.isNullOrBlank() && it.externalId != null }
            .forEach { FirebaseManager.salvaNota(uid, it.externalId!!, it.nota!!) }

        contentPrefs.setCloudMigrazioneCompletata()
    }

    /**
     * Chiamato una volta sola al primo login.
     * Carica i dati locali su Firestore senza sovrascrivere dati cloud esistenti.
     */
    suspend fun migraLocaleVersoCloud(uid: String) {
        val profiloCloud = FirebaseManager.caricaProfilo(uid)

        // XP e streak: prendi il massimo tra locale e cloud
        val xpLocale      = gamifPrefs.xpTotali.first()
        val streakLocale   = gamifPrefs.streakCorrente.first()
        val streakMaxLocale = gamifPrefs.streakMassima.first()

        val xpCloud        = (profiloCloud?.get("xp") as? Long)?.toInt() ?: 0
        val streakCloud    = (profiloCloud?.get("streakCorrente") as? Long)?.toInt() ?: 0
        val streakMaxCloud = (profiloCloud?.get("streakMassima") as? Long)?.toInt() ?: 0

        FirebaseManager.aggiornaProfilo(uid, mapOf(
            "xp"             to maxOf(xpLocale, xpCloud),
            "streakCorrente" to maxOf(streakLocale, streakCloud),
            "streakMassima"  to maxOf(streakMaxLocale, streakMaxCloud)
        ))

        // Badge: unione di locali e cloud
        val badgeLocali = repo.idBadgeSbloccati().toList()
        val badgeCloud  = FirebaseManager.caricaBadge(uid)
        val badgeTutti  = (badgeLocali + badgeCloud).distinct()
        if (badgeTutti.isNotEmpty()) {
            badgeTutti.forEach { FirebaseManager.aggiungiBadge(uid, it) }
        }

        // Pillole lette: carica gli externalId letti localmente
        val pilloleCloud = FirebaseManager.caricaPilloleLette(uid)
        val pilloleLocali = repo.getPilloleLette()
            .mapNotNull { it.externalId }
            .filter { it !in pilloleCloud }
        pilloleLocali.forEach { FirebaseManager.aggiungiPillolaLetta(uid, it) }
    }

    /**
     * Sincronizza XP e streak su Firestore dopo ogni azione gamification.
     */
    suspend fun sincronizzaGamification(uid: String) {
        val xp           = gamifPrefs.xpTotali.first()
        val streak       = gamifPrefs.streakCorrente.first()
        val streakMassima = gamifPrefs.streakMassima.first()
        FirebaseManager.aggiornaProfilo(uid, mapOf(
            "xp"             to xp,
            "streakCorrente" to streak,
            "streakMassima"  to streakMassima
        ))
    }

    /**
     * Al login su un nuovo dispositivo, scarica i progressi dal cloud
     * e li applica in locale.
     */
    suspend fun ripristinaCloudVersoLocale(uid: String) {
        val profilo = FirebaseManager.caricaProfilo(uid) ?: return

        // 1. XP
        val xpCloud  = (profilo["xp"] as? Long)?.toInt() ?: 0
        val xpLocale = gamifPrefs.xpTotali.first()
        if (xpCloud > xpLocale) gamifPrefs.aggiungiXp(xpCloud - xpLocale)

        // 2. Streak
        val streakCloud     = (profilo["streakCorrente"] as? Long)?.toInt() ?: 0
        val streakMaxCloud  = (profilo["streakMassima"] as? Long)?.toInt() ?: 0
        val streakMaxLocale = gamifPrefs.streakMassima.first()
        if (streakMaxCloud > streakMaxLocale) {
            gamifPrefs.setStreakDaCloud(streakCloud, streakMaxCloud)
        }

        // 3. Badge
        val badgeCloud     = FirebaseManager.caricaBadge(uid)
        val badgeLocaliIds = repo.idBadgeSbloccati()
        badgeCloud.forEach { id ->
            if (id !in badgeLocaliIds) {
                val def = BadgeCatalogo.trovaPerId(id)
                if (def != null) repo.sbloccaBadge(BadgeSbloccato(
                    id = def.id, nome = def.nome,
                    descrizione = def.descrizione, icona = def.icona
                ))
            }
        }

        // 4. Pillole lette -> isRead in Room
        val tutteLocali    = repo.getTutteLeCuriosita()
        val mappaExternalId = tutteLocali.associateBy { it.externalId ?: "" }

        val pilloleLette = FirebaseManager.caricaPilloleLette(uid)
        pilloleLette.forEach { exId ->
            val c = mappaExternalId[exId]
            if (c != null && !c.isRead) repo.markAsReadSilent(c)
        }

        // 5. Bookmark
        val bookmarkCloud = FirebaseManager.caricaBookmark(uid)
        bookmarkCloud.forEach { exId ->
            val c = mappaExternalId[exId]
            if (c != null && !c.isBookmarked) repo.setBookmarkSilent(c, true)
        }

        // 6. Ignorate
        val ignorateCloud = FirebaseManager.caricaIgnorate(uid)
        ignorateCloud.forEach { exId ->
            val c = mappaExternalId[exId]
            if (c != null && !c.isIgnorata) repo.setIgnorataSilent(c, true)
        }

        // 7. Note
        val noteCloud = FirebaseManager.caricaNote(uid)
        noteCloud.forEach { (exId, nota) ->
            val c = mappaExternalId[exId]
            if (c != null && c.nota != nota) repo.setNotaSilent(c, nota)
        }

        // 8. Quiz risposti
        val quizRisposti = FirebaseManager.caricaQuizRisposti(uid)
        quizRisposti.forEach { idStr ->
            idStr.toIntOrNull()?.let { repo.salvaRispostaSilent(it) }
        }
    }
}
