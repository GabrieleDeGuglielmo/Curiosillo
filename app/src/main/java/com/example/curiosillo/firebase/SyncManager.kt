package com.example.curiosillo.firebase

import android.util.Log
import com.example.curiosillo.data.BadgeCatalogo
import com.example.curiosillo.data.BadgeSbloccato
import com.example.curiosillo.data.ContentPreferences
import com.example.curiosillo.data.GamificationPreferences
import com.example.curiosillo.repository.CuriosityRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

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
    suspend fun migraLocaleVersoCloudSeNecessario(uid: String) = coroutineScope {
        if (contentPrefs.isCloudMigrazioneDone()) return@coroutineScope

        // Fetch locali in parallelo
        val xpDef            = async { gamifPrefs.xpTotali.first() }
        val streakDef        = async { gamifPrefs.streakCorrente.first() }
        val streakMassimaDef = async { gamifPrefs.streakMassima.first() }
        val recordSopravDef  = async { gamifPrefs.recordSopravvivenza.first() }
        val partiteSopravDef = async { gamifPrefs.partiteSopravvivenza.first() }
        val recordScalataDef = async { gamifPrefs.recordScalata.first() }
        val partiteScalataDef = async { gamifPrefs.partiteScalata.first() }

        val badgeLocaliDef   = async { repo.idBadgeSbloccati().toList() }
        val pilloleLetteDef  = async { repo.getPilloleLette().mapNotNull { it.externalId } }
        val bookmarkDef      = async { repo.getBookmarked().mapNotNull { it.externalId } }
        val ignorateDef      = async { repo.getPilloleIgnorate().mapNotNull { it.externalId } }
        val noteDef          = async { repo.getTutteLeCuriosita().filter { !it.nota.isNullOrBlank() && it.externalId != null } }

        val xp            = xpDef.await()
        val streak        = streakDef.await()
        val streakMassima = streakMassimaDef.await()
        val recordSoprav  = recordSopravDef.await()
        val partiteSoprav = partiteSopravDef.await()
        val recordScalata  = recordScalataDef.await()
        val partiteScalata = partiteScalataDef.await()

        launch {
            FirebaseManager.aggiornaProfilo(uid, mutableMapOf<String, Any>(
                "xp"             to xp,
                "streakCorrente" to streak,
                "streakMassima"  to streakMassima
            ).apply {
                if (recordSoprav > 0) {
                    put("hardcore_record", recordSoprav)
                    put("hardcore_partite", partiteSoprav)
                }
                if (recordScalata > 0) {
                    put("scalata_record", recordScalata)
                    put("scalata_partite", partiteScalata)
                }
            })
        }

        badgeLocaliDef.await().forEach { launch { FirebaseManager.aggiungiBadge(uid, it) } }
        pilloleLetteDef.await().forEach { launch { FirebaseManager.aggiungiPillolaLetta(uid, it) } }
        bookmarkDef.await().forEach { launch { FirebaseManager.aggiungiBookmark(uid, it) } }
        ignorateDef.await().forEach { launch { FirebaseManager.aggiungiIgnorata(uid, it) } }
        noteDef.await().forEach { launch { FirebaseManager.salvaNota(uid, it.externalId!!, it.nota!!) } }

        contentPrefs.setCloudMigrazioneCompletata()
    }

    /**
     * Chiamato una volta sola al primo login.
     * Carica i dati locali su Firestore senza sovrascrivere dati cloud esistenti.
     */
    suspend fun migraLocaleVersoCloud(uid: String) = coroutineScope {
        val profiloCloudDef = async { FirebaseManager.caricaProfilo(uid) }
        val fetchXpLocali   = async { gamifPrefs.xpTotali.first() }
        val fetchStreak     = async { gamifPrefs.streakCorrente.first() }
        val fetchStreakMax  = async { gamifPrefs.streakMassima.first() }
        
        val recordSopravDef  = async { gamifPrefs.recordSopravvivenza.first() }
        val partiteSopravDef = async { gamifPrefs.partiteSopravvivenza.first() }
        val recordScalataDef = async { gamifPrefs.recordScalata.first() }
        val partiteScalataDef = async { gamifPrefs.partiteScalata.first() }

        val badgeLocaliDef  = async { repo.idBadgeSbloccati().toList() }
        val badgeCloudDef   = async { FirebaseManager.caricaBadge(uid) }
        
        val pilloleCloudDef = async { FirebaseManager.caricaPilloleLette(uid) }
        val pilloleLocaliDef= async { repo.getPilloleLette().mapNotNull { it.externalId } }

        val profiloCloud    = profiloCloudDef.await()

        val xpLocale        = fetchXpLocali.await()
        val streakLocale    = fetchStreak.await()
        val streakMaxLocale = fetchStreakMax.await()
        val recSopravLoc    = recordSopravDef.await()
        val partSopravLoc   = partiteSopravDef.await()
        val recScalataLoc   = recordScalataDef.await()
        val partScalataLoc  = partiteScalataDef.await()

        val xpCloud        = (profiloCloud?.get("xp") as? Long)?.toInt() ?: 0
        val streakCloud    = (profiloCloud?.get("streakCorrente") as? Long)?.toInt() ?: 0
        val streakMaxCloud = (profiloCloud?.get("streakMassima") as? Long)?.toInt() ?: 0
        val recSopravCloud = (profiloCloud?.get("hardcore_record") as? Long)?.toInt() ?: 0
        val partSopravCloud = (profiloCloud?.get("hardcore_partite") as? Long)?.toInt() ?: 0
        val recScalataCloud = (profiloCloud?.get("scalata_record") as? Long)?.toInt() ?: 0
        val partScalataCloud = (profiloCloud?.get("scalata_partite") as? Long)?.toInt() ?: 0

        launch {
            FirebaseManager.aggiornaProfilo(uid, mapOf(
                "xp"             to maxOf(xpLocale, xpCloud),
                "streakCorrente" to maxOf(streakLocale, streakCloud),
                "streakMassima"  to maxOf(streakMaxLocale, streakMaxCloud),
                "hardcore_record" to maxOf(recSopravLoc, recSopravCloud),
                "hardcore_partite" to maxOf(partSopravLoc, partSopravCloud),
                "scalata_record" to maxOf(recScalataLoc, recScalataCloud),
                "scalata_partite" to maxOf(partScalataLoc, partScalataCloud)
            ))
        }

        val badgeTutti  = (badgeLocaliDef.await() + badgeCloudDef.await()).distinct()
        if (badgeTutti.isNotEmpty()) {
            badgeTutti.forEach { launch { FirebaseManager.aggiungiBadge(uid, it) } }
        }

        val pilloleCloud  = pilloleCloudDef.await()
        val pilloleLocali = pilloleLocaliDef.await().filter { it !in pilloleCloud }
        pilloleLocali.forEach { launch { FirebaseManager.aggiungiPillolaLetta(uid, it) } }
    }

    /**
     * Sincronizza XP e streak su Firestore dopo ogni azione gamification.
     */
    suspend fun sincronizzaGamification(uid: String) = coroutineScope {
        val xpDef            = async { gamifPrefs.xpTotali.first() }
        val streakDef        = async { gamifPrefs.streakCorrente.first() }
        val streakMassimaDef = async { gamifPrefs.streakMassima.first() }
        
        launch {
            FirebaseManager.aggiornaProfilo(uid, mapOf(
                "xp"             to xpDef.await(),
                "streakCorrente" to streakDef.await(),
                "streakMassima"  to streakMassimaDef.await()
            ))
        }
    }

    /**
     * Al login su un nuovo dispositivo, scarica i progressi dal cloud
     * e li applica in locale.
     */
    suspend fun ripristinaCloudVersoLocale(uid: String) = coroutineScope {
        val profiloDef       = async { FirebaseManager.caricaProfilo(uid) }
        val badgeCloudDef    = async { FirebaseManager.caricaBadge(uid) }
        val pilloleLetteDef  = async { FirebaseManager.caricaPilloleLette(uid) }
        val bookmarkCloudDef = async { FirebaseManager.caricaBookmark(uid) }
        val ignorateCloudDef = async { FirebaseManager.caricaIgnorate(uid) }
        val noteCloudDef     = async { FirebaseManager.caricaNote(uid) }
        val quizRispostiDef  = async { FirebaseManager.caricaQuizRisposti(uid) }
        
        val xpLocaleDef        = async { gamifPrefs.xpTotali.first() }
        val streakMaxLocaleDef = async { gamifPrefs.streakMassima.first() }
        val badgeLocaliIdsDef  = async { repo.idBadgeSbloccati() }
        val tutteLocaliDef     = async { repo.getTutteLeCuriosita() }

        val profilo = profiloDef.await() ?: return@coroutineScope

        val xpCloud  = (profilo["xp"] as? Long)?.toInt() ?: 0
        val xpLocale = xpLocaleDef.await()
        if (xpCloud > xpLocale) launch { gamifPrefs.aggiungiXp(xpCloud - xpLocale) }

        val streakCloud     = (profilo["streakCorrente"] as? Long)?.toInt() ?: 0
        val streakMaxCloud  = (profilo["streakMassima"] as? Long)?.toInt() ?: 0
        val streakMaxLocale = streakMaxLocaleDef.await()
        if (streakMaxCloud > streakMaxLocale) {
            launch { gamifPrefs.setStreakDaCloud(streakCloud, streakMaxCloud) }
        }

        // Ripristino Record Modalità
        val recSopravCloud  = (profilo["hardcore_record"] as? Long)?.toInt() ?: 0
        val partSopravCloud = (profilo["hardcore_partite"] as? Long)?.toInt() ?: 0
        if (recSopravCloud > 0 || partSopravCloud > 0) {
            launch { gamifPrefs.setSopravvivenzaDaCloud(recSopravCloud, partSopravCloud) }
        }

        val recScalataCloud  = (profilo["scalata_record"] as? Long)?.toInt() ?: 0
        val partScalataCloud = (profilo["scalata_partite"] as? Long)?.toInt() ?: 0
        if (recScalataCloud > 0 || partScalataCloud > 0) {
            launch { gamifPrefs.setScalataDaCloud(recScalataCloud, partScalataCloud) }
        }

        val badgeCloud     = badgeCloudDef.await()
        val badgeLocaliIds = badgeLocaliIdsDef.await()
        badgeCloud.forEach { id ->
            if (id !in badgeLocaliIds) {
                val def = BadgeCatalogo.trovaPerId(id)
                if (def != null) {
                    launch {
                        repo.sbloccaBadge(BadgeSbloccato(
                            id = def.id, nome = def.nome,
                            descrizione = def.descrizione, icona = def.icona
                        ))
                    }
                }
            }
        }

        val tutteLocali     = tutteLocaliDef.await()
        val mappaExternalId = tutteLocali.associateBy { it.externalId ?: "" }

        pilloleLetteDef.await().forEach { exId ->
            val c = mappaExternalId[exId]
            if (c != null && !c.isRead) launch { repo.markAsReadSilent(c) }
        }

        bookmarkCloudDef.await().forEach { exId ->
            val c = mappaExternalId[exId]
            if (c != null && !c.isBookmarked) launch { repo.setBookmarkSilent(c, true) }
        }

        ignorateCloudDef.await().forEach { exId ->
            val c = mappaExternalId[exId]
            if (c != null && !c.isIgnorata) launch { repo.setIgnorataSilent(c, true) }
        }

        noteCloudDef.await().forEach { (exId, nota) ->
            val c = mappaExternalId[exId]
            if (c != null && c.nota != nota) launch { repo.setNotaSilent(c, nota) }
        }

        quizRispostiDef.await().forEach { idStr ->
            idStr.toIntOrNull()?.let { launch { repo.salvaRispostaSilent(it) } }
        }
    }
}
